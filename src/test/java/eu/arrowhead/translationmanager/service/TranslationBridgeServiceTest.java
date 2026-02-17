/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationBridgeCandidateDTO;
import eu.arrowhead.dto.TranslationDiscoveryRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.engine.TranslatorBridgeEngine;
import eu.arrowhead.translationmanager.service.validation.TranslationBridgeValidation;

@ExtendWith(MockitoExtension.class)
public class TranslationBridgeServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslationBridgeService service;

	@Mock
	private TranslationBridgeValidation validator;

	@Mock
	private TranslationManagerSystemInfo sysInfo;

	@Mock
	private TranslatorBridgeEngine engine;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDiscoveryOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.discoveryOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDiscoveryOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.discoveryOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDiscoveryOperationOk() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");
		final NormalizedServiceInstanceDTO normalizedCandidate = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				null,
				null,
				null);
		final NormalizedTranslationDiscoveryRequestDTO normalized = new NormalizedTranslationDiscoveryRequestDTO(
				"TestConsumer",
				List.of(normalizedCandidate),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testXml",
				"testJson");
		final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);
		final TranslationDiscoveryResponseDTO response = new TranslationDiscoveryResponseDTO(
				"bridgeId",
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));

		when(validator.validateAndNormalizeRequester("TestConsumer", "origin")).thenReturn("TestConsumer");
		when(validator.validateAndNormalizeDiscoveryRequest("TestConsumer", dto, "origin")).thenReturn(normalized);
		when(sysInfo.isBlacklistEnabled()).thenReturn(false);
		when(sysInfo.isAuthorizationEnabled()).thenReturn(false);
		when(engine.doDiscovery(normalized, discoveryFlags, "origin")).thenReturn(response);

		final TranslationDiscoveryResponseDTO result = service.discoveryOperation("TestConsumer", dto, "origin");

		assertNotNull(result);
		assertEquals("bridgeId", result.bridgeId());
		assertNotNull(result.candidates());
		assertEquals(1, result.candidates().size());
		assertEquals("TestProvider|testService|1.0.0", result.candidates().get(0).serviceInstanceId());
		assertEquals("generic_http", result.candidates().get(0).interfaceTemplateName());

		verify(validator).validateAndNormalizeRequester("TestConsumer", "origin");
		verify(validator).validateAndNormalizeDiscoveryRequest("TestConsumer", dto, "origin");
		verify(sysInfo, times(2)).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(engine).doDiscovery(normalized, discoveryFlags, "origin");
	}
}