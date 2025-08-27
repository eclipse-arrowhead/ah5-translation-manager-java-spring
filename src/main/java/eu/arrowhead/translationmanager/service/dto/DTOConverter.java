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
package eu.arrowhead.translationmanager.service.dto;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.TranslationBridgeCandidateDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TranslationDiscoveryResponseDTO convertDiscoveryModels(final UUID bridgeId, final List<TranslationDiscoveryModel> models) {
		logger.debug("convertDiscoveryModels started...");
		Assert.notNull(bridgeId, "bridgeId is null");
		Assert.isTrue(!Utilities.isEmpty(models), "models list is missing");
		Assert.isTrue(!Utilities.containsNull(models), "models list contains null element");

		return new TranslationDiscoveryResponseDTO(
				bridgeId.toString(),
				models
					.stream()
					.map(m -> new TranslationBridgeCandidateDTO(m.getTargetInstanceId(), m.getToInterfaceTemplate()))
					.toList());
	}
}