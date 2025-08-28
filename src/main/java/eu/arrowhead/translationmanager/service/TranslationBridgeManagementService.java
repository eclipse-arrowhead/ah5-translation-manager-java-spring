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
package eu.arrowhead.translationmanager.service;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.TranslationDiscoveryMgmtRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationMgmtRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.engine.TranslatorBridgeEngine;
import eu.arrowhead.translationmanager.service.validation.TranslationBridgeMgmtValidation;

@Service
public class TranslationBridgeManagementService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private TranslationBridgeMgmtValidation validator;

	@Autowired
	private TranslationManagerSystemInfo sysInfo;

	@Autowired
	private TranslatorBridgeEngine engine;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TranslationDiscoveryResponseDTO discoveryOperation(final String requester, final TranslationDiscoveryMgmtRequestDTO dto, final String origin) {
		logger.debug("discoveryOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requester, origin);
		final Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> normalized = validator.validateAndNormalizeDiscoveryMgmtRequest(normalizedRequester, dto, origin);
		final Map<TranslationDiscoveryFlag, Boolean> actualFlags = mergeUserFlagsWithSettings(normalized.getSecond());

		return engine.doDiscovery(normalized.getFirst(), actualFlags, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public TranslationNegotiationResponseDTO negotiationOperation(final TranslationNegotiationMgmtRequestDTO dto, final String origin) {
		logger.debug("negotiationOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// TODO: continue

		return null;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Map<TranslationDiscoveryFlag, Boolean> mergeUserFlagsWithSettings(final Map<TranslationDiscoveryFlag, Boolean> userFlags) {
		logger.debug("mergeUserFlagsWithSettings started...");

		return Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, userFlags.getOrDefault(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false) && sysInfo.isBlacklistEnabled(),
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, userFlags.getOrDefault(TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false) && sysInfo.isBlacklistEnabled(),
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, userFlags.getOrDefault(TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false) && sysInfo.isAuthorizationEnabled(),
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, userFlags.getOrDefault(TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false) && sysInfo.isBlacklistEnabled(),
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, userFlags.getOrDefault(TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false) && sysInfo.isAuthorizationEnabled());
	}
}