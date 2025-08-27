/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.translationmanager.service.engine;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.service.validation.meta.MetaOps;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementTokenizer;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.translationmanager.TranslationManagerConstants;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@Service
public class CoreSystemsDriver {

	//=================================================================================================
	// members

	@Value(TranslationManagerConstants.$TRANSLATOR_SERVICE_MIN_AVAILABILITY_WD)
	private int translatorServiceMinAvailability;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private TranslationManagerSystemInfo sysInfo;

	@Autowired
	private ArrowheadHttpService ahHttpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean isBlacklisted(final String systemName) {
		logger.debug("isBlacklisted started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is missing");

		return filterOutBlacklistedSystems(List.of(systemName)).isEmpty();
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> filterOutBlacklistedSystems(final List<String> systemNames) {
		logger.debug("filterOutBlacklistedSystems started...");
		Assert.isTrue(!Utilities.isEmpty(systemNames), "systemNames is missing");
		Assert.isTrue(!Utilities.containsNullOrEmpty(systemNames), "systemNames list contains null or empty element");

		final List<String> candidates = systemNames.stream().filter(sysName -> !sysInfo.getBlacklistCheckExcludeList().contains(sysName)).toList();

		try {
			boolean hasMorePage = false;
			int pageNumber = 0;
			Integer pageSize = null;

			final List<BlacklistEntryDTO> blacklistEntries = new ArrayList<BlacklistEntryDTO>();
			do {
				final BlacklistEntryListResponseDTO response = ahHttpService.consumeService(
						Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT,
						Constants.SERVICE_OP_BLACKLIST_QUERY,
						Constants.SYS_NAME_BLACKLIST,
						BlacklistEntryListResponseDTO.class,
						new BlacklistQueryRequestDTO(
								new PageDTO(pageNumber == 0 ? null : pageNumber, pageSize, null, null),
								candidates,
								null,
								null,
								null,
								null,
								Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow())));
				blacklistEntries.addAll(response.entries());
				hasMorePage = blacklistEntries.size() < response.count();
				pageNumber = hasMorePage ? pageNumber + 1 : pageNumber;
				pageSize = pageSize == null ? response.entries().size() : pageSize;
			} while (hasMorePage);

			final List<String> result = systemNames
					.stream()
					.filter(name -> {
						boolean isBlacklisted = false;
						for (final BlacklistEntryDTO blDTO : blacklistEntries) {
							if (name.equals(blDTO.systemName())) {
								isBlacklisted = true;
								break;
							}
						}

						return !isBlacklisted;
					}).toList();

			return result;
		} catch (final ForbiddenException | AuthException ex) {
			throw ex;
		} catch (final ArrowheadException ex) {
			logger.error("Blacklist server is not available during the translation bridge process");
			if (sysInfo.isBlacklistForced()) {
				logger.error("All the provider candidate has been filtered out, because blacklist is forced");
				return List.of();
			} else {
				logger.error("All the provider candidate has been passed, because blacklist filter is not forced");
				return systemNames;
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> filterOutProvidersBecauseOfUnauthorization(final List<String> candidates, final String consumer, final String serviceDefinition, final String operation) {
		logger.debug("filterOutProvidersBecauseOfUnauthorization started...");
		Assert.isTrue(!Utilities.isEmpty(candidates), "candidates list is missing");
		Assert.isTrue(!Utilities.isEmpty(consumer), "consumer is missing");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is missing");

		final AuthorizationVerifyListRequestDTO payload = calculateVerifyPayload(candidates, consumer, serviceDefinition, operation);
		final AuthorizationVerifyListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT,
				Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES,
				Constants.SYS_NAME_CONSUMER_AUTHORIZATION,
				AuthorizationVerifyListResponseDTO.class,
				payload);

		if (response.entries().isEmpty()) {
			return List.of();
		}

		final List<String> result = new ArrayList<>();
		candidates.forEach(c -> {
			final boolean denied = response
					.entries()
					.stream()
					.anyMatch(e -> e.provider().equals(c) && !e.granted());

			if (!denied) {
				// has access the specified operation  (or all operations if nothing is specified)
				result.add(c);
			}
		});

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public String generateTokenForInterfaceTranslator(
			final String policy,
			final String interfaceTranslator,
			final String targetProvider,
			final String serviceDefinition,
			final String operation) {
		logger.debug("generateTokenForInterfaceTranslator started...");
		Assert.isTrue(!Utilities.isEmpty(policy), "policy is missing");
		Assert.isTrue(!Utilities.isEmpty(interfaceTranslator), "interfaceTranslator is missing");
		Assert.isTrue(!Utilities.isEmpty(targetProvider), "targetProvider is missing");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is missing");
		Assert.isTrue(!Utilities.isEmpty(operation), "operation is missing");

		final AuthorizationTokenGenerationMgmtListRequestDTO payload = calculateTokenGenerationPayload(policy, interfaceTranslator, targetProvider, serviceDefinition, operation);

		final AuthorizationTokenMgmtListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT,
				Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS,
				Constants.SYS_NAME_CONSUMER_AUTHORIZATION,
				AuthorizationTokenMgmtListResponseDTO.class,
				payload,
				new LinkedMultiValueMap<>(Map.of(Constants.UNBOUND, List.of(Boolean.TRUE.toString()))));

		return response
				.entries()
				.get(0)
				.token();
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceResponseDTO> collectInterfaceTranslatorCandidates(final List<String> inputInterfaceRequirements, final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("collectInterfaceTranslatorCandidates started...");
		Assert.isTrue(!Utilities.isEmpty(inputInterfaceRequirements), "inputInterfaceRequirements list is missing");
		Assert.isTrue(!Utilities.isEmpty(targets), "targets list is missing");

		final ServiceInstanceLookupRequestDTO payload = calculateInterfaceTranslatorLookupPayload(inputInterfaceRequirements, targets);
		final ServiceInstanceListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				Constants.SERVICE_OP_LOOKUP,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				ServiceInstanceListResponseDTO.class,
				payload);

		return response.entries();
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceResponseDTO> collectDataModelTranslatorCandidates(final List<TranslationDiscoveryModel> models) {
		logger.debug("collectDataModelTranslatorCandidates started...");
		Assert.isTrue(!Utilities.isEmpty(models), "models list is missing");

		final ServiceInstanceLookupRequestDTO payload = calculateDataModelTranslatorLookupPayload(models);
		final ServiceInstanceListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				Constants.SERVICE_OP_LOOKUP,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				ServiceInstanceListResponseDTO.class,
				payload);

		return response.entries();
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> getConfigurationForSystem(final String systemName) {
		logger.debug("getConfigurationForSystem started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "system name is missing");

		// TODO: implement after configuration support system is available

		return Map.of();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationVerifyListRequestDTO calculateVerifyPayload(final List<String> candidates, final String consumer, final String serviceDefinition, final String operation) {
		logger.debug("calculateVerifyPayload started...");

		final List<AuthorizationVerifyRequestDTO> list = new ArrayList<>(candidates.size());
		candidates.forEach(c -> {
			list.add(new AuthorizationVerifyRequestDTO(
					c,
					consumer,
					null, // local cloud
					AuthorizationTargetType.SERVICE_DEF.name(),
					serviceDefinition,
					operation));
		});

		return new AuthorizationVerifyListRequestDTO(list);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationTokenGenerationMgmtListRequestDTO calculateTokenGenerationPayload(
			final String policy,
			final String interfaceTranslator,
			final String targetProvider,
			final String serviceDefinition,
			final String operation) {
		logger.debug("calculateTokenGenerationPayload started...");

		return new AuthorizationTokenGenerationMgmtListRequestDTO(List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						policy,
						AuthorizationTargetType.SERVICE_DEF.name(),
						Defaults.DEFAULT_CLOUD,
						interfaceTranslator,
						targetProvider,
						serviceDefinition,
						operation,
						null,
						null)));
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceLookupRequestDTO calculateInterfaceTranslatorLookupPayload(final List<String> inputInterfaceRequirements, final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("calculateInterfaceTranslatorLookupPayload started...");

		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final List<String> policies = AuthenticationPolicy.CERTIFICATE == sysInfo.getAuthenticationPolicy()
				? List.of(ServiceInterfacePolicy.CERT_AUTH.name(), ServiceInterfacePolicy.NONE.name())
				: List.of(ServiceInterfacePolicy.NONE.name());

		ServiceInstanceLookupRequestDTO.Builder builder = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(Constants.SERVICE_DEF_INTERFACE_BRIDGE_MANAGEMENT)
				.interfaceTemplateName(templateName)
				.policies(policies)
				.metadataRequirementsList(calculateInterfaceTranslatorMetadataRequirements(inputInterfaceRequirements, targets));

		if (translatorServiceMinAvailability > 0) {
			final ZonedDateTime alivesAt = Utilities.utcNow().plusMinutes(translatorServiceMinAvailability);
			builder = builder.alivesAt(Utilities.convertZonedDateTimeToUTCString(alivesAt));
		}

		return builder.build();
	}

	//-------------------------------------------------------------------------------------------------
	private List<MetadataRequirementDTO> calculateInterfaceTranslatorMetadataRequirements(final List<String> inputInterfaceRequirements, final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("calculateInterfaceTranslatorMetadataRequirements started...");

		final Set<MetadataRequirementDTO> resultSet = new HashSet<>();

		targets.forEach(t -> {
			final MetadataRequirementDTO req = new MetadataRequirementDTO();
			req.put(String.join(Constants.DOT, Constants.METADATA_KEY_INTERFACE_BRIDGE, Constants.METADATA_KEY_FROM), Map.of(
					MetadataRequirementTokenizer.OP, MetaOps.CONTAINS_ANY.name(),
					MetadataRequirementTokenizer.VALUE, inputInterfaceRequirements));
			req.put(String.join(Constants.DOT, Constants.METADATA_KEY_INTERFACE_BRIDGE, Constants.METADATA_KEY_TO), Map.of(
					MetadataRequirementTokenizer.OP, MetaOps.IN.name(),
					MetadataRequirementTokenizer.VALUE, t.interfaces()
							.stream()
							.map(intf -> intf.templateName())
							.toList()));
			resultSet.add(req);
		});

		return new ArrayList<>(resultSet);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceLookupRequestDTO calculateDataModelTranslatorLookupPayload(final List<TranslationDiscoveryModel> models) {
		logger.debug("calculateDataModelTranslatorLookupPayload started...");

		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final List<String> policies = AuthenticationPolicy.CERTIFICATE == sysInfo.getAuthenticationPolicy()
				? List.of(ServiceInterfacePolicy.CERT_AUTH.name(), ServiceInterfacePolicy.NONE.name())
				: List.of(ServiceInterfacePolicy.NONE.name());

		ServiceInstanceLookupRequestDTO.Builder builder = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(Constants.SERVICE_DEF_DATA_MODEL_TRANSLATION)
				.interfaceTemplateName(templateName)
				.policies(policies)
				.metadataRequirementsList(calculateDataModelTranslatorMetadataRequirements(models));

		if (translatorServiceMinAvailability > 0) {
			final ZonedDateTime alivesAt = Utilities.utcNow().plusMinutes(translatorServiceMinAvailability);
			builder = builder.alivesAt(Utilities.convertZonedDateTimeToUTCString(alivesAt));
		}

		return builder.build();
	}

	//-------------------------------------------------------------------------------------------------
	private List<MetadataRequirementDTO> calculateDataModelTranslatorMetadataRequirements(final List<TranslationDiscoveryModel> models) {
		logger.debug("calculateDataModelTranslatorInterfacePropertyRequirements started...");

		final Set<MetadataRequirementDTO> resultSet = new HashSet<>();

		models.forEach(m -> {
			if (m.getInputDataModelIdRequirement() != null) {
				final MetadataRequirementDTO req = new MetadataRequirementDTO();
				final List<String> translationDirection = List.of(m.getInputDataModelIdRequirement(), m.getTargetInputDataModelId());
				req.put(Constants.METADATA_KEY_DATA_MODEL_IDS, Map.of(
						MetadataRequirementTokenizer.OP, MetaOps.CONTAINS.name(),
						MetadataRequirementTokenizer.VALUE, translationDirection));
				resultSet.add(req);
			}

			if (m.getOutputDataModelIdRequirement() != null) {
				final MetadataRequirementDTO req = new MetadataRequirementDTO();
				final List<String> translationDirection = List.of(m.getTargetOutputDataModelId(), m.getOutputDataModelIdRequirement());
				req.put(Constants.METADATA_KEY_DATA_MODEL_IDS, Map.of(
						MetadataRequirementTokenizer.OP, MetaOps.CONTAINS.name(),
						MetadataRequirementTokenizer.VALUE, translationDirection));
				resultSet.add(req);
			}
		});

		return new ArrayList<>(resultSet);
	}
}