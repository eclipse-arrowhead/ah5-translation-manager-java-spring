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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.TranslationDataModelTranslatorInitializationResponseDTO;

@Service
public class DataModelTranslatorFactoryDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TranslationDataModelTranslatorInitializationResponseDTO initializeDataModelTranslator(
			final Map<String, Object> factoryInterfaceProperties,
			final String fromDataModelId,
			final String toDataModelId) {
		logger.debug("initializeDataModelTranslator started...");
		Assert.isTrue(!Utilities.isEmpty(factoryInterfaceProperties), "Factory interface properties is missing");
		Assert.isTrue(!Utilities.isEmpty(fromDataModelId), "From data model identifier is missing");
		Assert.isTrue(!Utilities.isEmpty(toDataModelId), "To data model identifier is missing");

		// TODO: implement to support data model translator factories

		throw new InternalServerError("Data model translator factories are currently not supported");
	}
}