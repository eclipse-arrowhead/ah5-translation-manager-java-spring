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
package eu.arrowhead.translationmanager.service.validation;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryMgmtRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;

@Service
public class TranslationBridgeMgmtValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	@Autowired
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefinitionNameNormalizer;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	@Autowired
	private ServiceOperationNameNormalizer operationNormalizer;

	@Autowired
	private ServiceOperationNameValidator operationValidator;

	@Autowired
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Autowired
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Autowired
	private DataModelIdentifierNormalizer dataModelIdentifierNormalizer;

	@Autowired
	private DataModelIdentifierValidator dataModelIdentifierValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRequester(final String requester, final String origin) {
		logger.debug("validateAndNormalizeRequester started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateRequester(requester, origin);
		final String normalized = systemNameNormalizer.normalize(requester);

		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> validateAndNormalizeDiscoveryMgmtRequest(
			final String requester,
			final TranslationDiscoveryMgmtRequestDTO dto,
			final String origin) {
		logger.debug("validateAndNormalizeDiscoveryMgmtRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateDiscoveryMgmtRequest(dto, origin);
		final Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> normalized = normalizeDiscoveryMgmtRequest(requester, dto);
		validateNormalizedDiscoveryMgmtRequest(normalized.getFirst(), origin);

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateRequester(final String requester, final String origin) {
		logger.debug("validateRequester started...");

		if (Utilities.isEmpty(requester)) {
			throw new InvalidParameterException("Requester name is missing or empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateDiscoveryMgmtRequest(final TranslationDiscoveryMgmtRequestDTO dto, final String origin) {
		logger.debug("validateDiscoveryMgmtRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("request is missing", origin);
		}

		if (Utilities.isEmpty(dto.candidates())) {
			throw new InvalidParameterException("candidates list is missing", origin);
		}

		if (Utilities.containsNull(dto.candidates())) {
			throw new InvalidParameterException("candidates list contains null element", origin);
		}

		dto.candidates().forEach(c -> validateCandidate(c, origin));

		if (Utilities.isEmpty(dto.consumer())) {
			throw new InvalidParameterException("consumer is missing", origin);
		}

		if (Utilities.isEmpty(dto.operation())) {
			throw new InvalidParameterException("operation is missing", origin);
		}

		if (Utilities.isEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("interface template names list is missing", origin);
		}

		if (Utilities.containsNullOrEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("interface template names list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.flags())) {
			dto.flags()
					.keySet()
					.forEach(k -> {
						if (Utilities.isEmpty(k)) {
							throw new InvalidParameterException("flag name is missing", origin);
						}

						if (!Utilities.isEnumValue(k.trim().toUpperCase(), TranslationDiscoveryFlag.class)) {
							throw new InvalidParameterException("flag is invalid: " + k, origin);
						}
					});
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateCandidate(final ServiceInstanceResponseDTO candidate, final String origin) {
		logger.debug("validateCandidate started...");

		if (Utilities.isEmpty(candidate.instanceId())) {
			throw new InvalidParameterException("Service instance id is missing", origin);
		}

		if (candidate.provider() == null || Utilities.isEmpty(candidate.provider().name())) {
			throw new InvalidParameterException("Provider name is missing", origin);
		}

		if (candidate.serviceDefinition() == null || Utilities.isEmpty(candidate.serviceDefinition().name())) {
			throw new InvalidParameterException("Service definition name is missing", origin);
		}

		if (Utilities.isEmpty(candidate.interfaces())) {
			throw new InvalidParameterException("Interfaces list is missing", origin);
		}

		if (Utilities.containsNull(candidate.interfaces())) {
			throw new InvalidParameterException("Interfaces list contains null element", origin);
		}

		candidate.interfaces().forEach(intf -> validateServiceInstanceInterface(intf, origin));
	}

	//-------------------------------------------------------------------------------------------------
	private void validateServiceInstanceInterface(final ServiceInstanceInterfaceResponseDTO intf, final String origin) {
		logger.debug("validateCandidate started...");

		if (Utilities.isEmpty(intf.templateName())) {
			throw new InvalidParameterException("Template name is missing", origin);
		}

		if (Utilities.isEmpty(intf.policy())) {
			throw new InvalidParameterException("Policy is missing", origin);
		}

		if (!Utilities.isEnumValue(intf.policy().trim().toUpperCase(), ServiceInterfacePolicy.class)) {
			throw new InvalidParameterException("Policy is invalid: " + intf.policy(), origin);
		}

		if (Utilities.isEmpty(intf.properties())) {
			throw new InvalidParameterException("Interface properties are missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedDiscoveryMgmtRequest(final NormalizedTranslationDiscoveryRequestDTO normalized, final String origin) {
		logger.debug("validateNormalizedDiscoveryMgmtRequest started...");

		try {
			// normalized createdBy is already validated
			// discovery assumes that all candidate contains the same service definition
			final String serviceDefinition = normalized.candidates().get(0).serviceDefinition();
			normalized.candidates().forEach(c -> validateNormalizedCandidate(c, serviceDefinition));
			systemNameValidator.validateSystemName(normalized.consumer());
			operationValidator.validateServiceOperationName(normalized.operation());
			normalized.interfaceTemplateNames().forEach(iName -> interfaceTemplateNameValidator.validateInterfaceTemplateName(iName));
			if (normalized.inputDataModelId() != null) {
				dataModelIdentifierValidator.validateDataModelIdentifier(normalized.inputDataModelId());
			}

			if (normalized.outputDataModelId() != null) {
				dataModelIdentifierValidator.validateDataModelIdentifier(normalized.outputDataModelId());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedCandidate(final NormalizedServiceInstanceDTO candidate, final String serviceDefinition) {
		logger.debug("validateNormalizedCandidate started...");

		serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(candidate.instanceId());
		systemNameValidator.validateSystemName(candidate.provider());
		serviceDefinitionNameValidator.validateServiceDefinitionName(candidate.serviceDefinition());
		if (!candidate.serviceDefinition().equals(serviceDefinition)) {
			throw new InvalidParameterException("All candidates must contain the same service definition name");
		}

		candidate.interfaces().forEach(intf -> interfaceTemplateNameValidator.validateInterfaceTemplateName(intf.templateName()));
	}

	//-------------------------------------------------------------------------------------------------
	// NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	private Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> normalizeDiscoveryMgmtRequest(final String requester, final TranslationDiscoveryMgmtRequestDTO dto) {
		logger.debug("normalizeDiscoveryMgmtRequest started...");

		return Pair.of(
				new NormalizedTranslationDiscoveryRequestDTO(
						requester,
						dto.candidates().stream().map(c -> normalizeCandidate(c)).toList(),
						systemNameNormalizer.normalize(dto.consumer()),
						operationNormalizer.normalize(dto.operation()),
						dto.interfaceTemplateNames().stream().map(iName -> interfaceTemplateNameNormalizer.normalize(iName)).toList(),
						dataModelIdentifierNormalizer.normalize(dto.inputDataModelId()),
						dataModelIdentifierNormalizer.normalize(dto.outputDataModelId())),
				normalizeDiscoveryFlags(dto.flags()));
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedServiceInstanceDTO normalizeCandidate(final ServiceInstanceResponseDTO candidate) {
		logger.debug("normalizeCandidate started...");

		return new NormalizedServiceInstanceDTO(
				serviceInstanceIdentifierNormalizer.normalize(candidate.instanceId()),
				systemNameNormalizer.normalize(candidate.provider().name()),
				serviceDefinitionNameNormalizer.normalize(candidate.serviceDefinition().name()),
				candidate.interfaces().stream().map(intf -> normalizeInterface(intf)).toList());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceInterfaceResponseDTO normalizeInterface(final ServiceInstanceInterfaceResponseDTO intf) {
		logger.debug("normalizeInterface started...");

		return new ServiceInstanceInterfaceResponseDTO(
				interfaceTemplateNameNormalizer.normalize(intf.templateName()),
				Utilities.isEmpty(intf.protocol()) ? "" : intf.protocol().trim().toLowerCase(),
				intf.policy().trim().toUpperCase(),
				intf.properties());
	}

	//-------------------------------------------------------------------------------------------------
	private Map<TranslationDiscoveryFlag, Boolean> normalizeDiscoveryFlags(final Map<String, Boolean> flags) {
		logger.debug("normalizeDiscoveryFlags started...");

		if (Utilities.isEmpty(flags)) {
			return Map.of();
		}

		final Map<TranslationDiscoveryFlag, Boolean> result = new HashMap<>(flags.size());
		flags.forEach((k, v) -> {
			if (v != null) {
				result.put(TranslationDiscoveryFlag.valueOf(k.trim().toUpperCase()), v);
			}
		});

		return result;
	}
}