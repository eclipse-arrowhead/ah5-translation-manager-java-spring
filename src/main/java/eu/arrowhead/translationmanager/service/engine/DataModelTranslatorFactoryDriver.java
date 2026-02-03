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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.dto.DataModelTranslatorFactoryRequestDTO;
import eu.arrowhead.dto.TranslationDataModelTranslatorInitializationResponseDTO;

@Service
public class DataModelTranslatorFactoryDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private HttpService httpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public boolean isFactorySupportsTranslation(
			final String factoryName,
			final Map<String, Object> factoryInterfaceProperties,
			final String fromDataModelId,
			final String toDataModelId) {
		logger.debug("isFactorySupportsTranslation started...");
		Assert.isTrue(!Utilities.isEmpty(factoryInterfaceProperties), "Factory interface properties is missing");
		Assert.isTrue(!Utilities.isEmpty(fromDataModelId), "From data model identifier is missing");
		Assert.isTrue(!Utilities.isEmpty(toDataModelId), "To data model identifier is missing");

		HttpMethod method = HttpMethod.GET; // default method
		String operationPath = "/check"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final String host = ((List<String>) factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (factoryInterfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_INIT_BRIDGE)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_INIT_BRIDGE);
			try {
				final HttpOperationModel operationModel = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(operationModel.method());
				operationPath = operationModel.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for data model translator factory {}", factoryName);
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath);
		final DataModelTranslatorFactoryRequestDTO payload = calculateCheckPayload(fromDataModelId, toDataModelId);

		try {
			final Boolean response = httpService.sendRequest(
					uri,
					method,
					Boolean.class,
					payload);

			return response == null ? false : response;
		} catch (final ArrowheadException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);

			return false;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public TranslationDataModelTranslatorInitializationResponseDTO initializeDataModelTranslator(
			final Map<String, Object> factoryInterfaceProperties,
			final String fromDataModelId,
			final String toDataModelId) throws ExternalServerError {
		logger.debug("initializeDataModelTranslator started...");
		Assert.isTrue(!Utilities.isEmpty(factoryInterfaceProperties), "Factory interface properties is missing");
		Assert.isTrue(!Utilities.isEmpty(fromDataModelId), "From data model identifier is missing");
		Assert.isTrue(!Utilities.isEmpty(toDataModelId), "To data model identifier is missing");

		// TODO: implement to support data model translator factories

		throw new InternalServerError("Data model translator factories are currently not supported");
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DataModelTranslatorFactoryRequestDTO calculateCheckPayload(final String fromDataModelId, final String toDataModelId) {
		logger.debug("calculateCheckPayload started...");

		return new DataModelTranslatorFactoryRequestDTO(fromDataModelId, toDataModelId);
	}
}