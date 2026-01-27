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
package eu.arrowhead.translationmanager.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.TranslationBridgeCandidateDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService;
import eu.arrowhead.translationmanager.service.dto.DTOConverter;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;
import eu.arrowhead.translationmanager.service.matchmaking.DataModelTranslatorMatchmaker;
import eu.arrowhead.translationmanager.service.matchmaking.InterfaceTranslatorMatchmaker;

@ExtendWith(MockitoExtension.class)
public class TranslatorBridgeEngineTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslatorBridgeEngine engine;

	@Mock
	private CoreSystemsDriver csDriver;

	@Mock
	private InterfaceTranslatorDriver itDriver;

	@Mock
	private DataModelTranslatorFactoryDriver dmfDriver;

	@Mock
	private DataModelIdentifierNormalizer dataModelIdentifierNormalizer;

	@Mock
	private DataModelIdentifierValidator dataModelIdentifierValidator;

	@Mock
	private InterfaceTranslatorMatchmaker interfaceTranslatorMatchmaker;

	@Mock
	private DataModelTranslatorMatchmaker dataModelTranslatorMatchmaker;

	@Mock
	private BridgeDbService dbService;

	@Mock
	private DTOConverter converter;

	@Mock
	private TranslationManagerSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryDTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doDiscovery(null, null, null));

		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryFlagsNull() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doDiscovery(dto, null, null));

		assertEquals("discoveryFlags is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryOriginNull() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doDiscovery(dto, Map.of(), null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryOriginEmpty() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> engine.doDiscovery(dto, Map.of(), ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryConsumerBlacklisted() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, true);

		when(csDriver.isBlacklisted("TestConsumer")).thenReturn(true);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("TestConsumer system is blacklisted", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver).isBlacklisted("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryNoNeedForTranslation() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, true);

		when(csDriver.isBlacklisted("TestConsumer")).thenReturn(false);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver).isBlacklisted("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryTranslationNotPossible1() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", "string"));

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				"testJson",
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryTranslationNotPossible2() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of()));

		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf))),
				"TestConsumer",
				"test-operation",
				List.of("generic_http"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryInvalidDataModelId() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "test+ml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		when(dataModelIdentifierNormalizer.normalize("test+ml")).thenReturn("test+ml");
		doThrow(InvalidParameterException.class).when(dataModelIdentifierValidator).validateDataModelIdentifier("test+ml");

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("test+ml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("test+ml");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryTranslationNotPossible3() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryTranslationNotPossible4() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", "testXml");
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryBlacklistedCandidate() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", "string"), "a", "b"));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true);

		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemAuthException() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new AuthException("test"));

		final ArrowheadException ex = assertThrows(
				AuthException.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemForbiddenException() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new ForbiddenException("test"));

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemInvalidParameterException() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemExternalServerError() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new ExternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryAuthorizationProblemRuntimeException() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenThrow(new RuntimeException("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> engine.doDiscovery(dto, flags, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryUnauthorizedCandidate() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutBlacklistedSystems(List.of("TestProvider"))).thenReturn(List.of("TestProvider"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryNoAppropriateInterfaceTranslator() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", "testXml");
		opMap.put("output", null);
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, true);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation")).thenReturn(List.of("TestProvider"));
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryBlacklistedInterfaceTranslator() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("InterfaceTranslator"))).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryUnauthorizedInterfaceTranslator() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("InterfaceTranslator"))).thenReturn(List.of("InterfaceTranslator"));
		when(sysInfo.getSystemName()).thenReturn("TranslationManager");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null)).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(sysInfo).getSystemName();
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryIncompatibleInterfaceTranslatorForTarget() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http")),
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(sysInfo.getSystemName()).thenReturn("TranslationManager");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null)).thenReturn(List.of("InterfaceTranslator"));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(sysInfo).getSystemName();
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryEmptyModels() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http")),
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(null);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoDiscoveryEmptyModels2() {
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", Map.of("input", "testXml", "output", "testXml"))));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("some_problem"))),
				List.of(iTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(dataModelIdentifierNormalizer, times(2)).normalize("testXml");
		verify(dataModelIdentifierValidator, times(2)).validateDataModelIdentifier("testXml");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryOkNoDMTranslators() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", null);
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				null);
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));

		final UUID bridgeId = UUID.fromString("9ef06aec-7865-48c0-b456-9f6faab47c22");
		final TranslationDiscoveryResponseDTO expected = new TranslationDiscoveryResponseDTO(
				bridgeId.toString(),
				List.of(new TranslationBridgeCandidateDTO("TestProvider|testService|1.0.0", "generic_http")));

		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);

		try (MockedStatic<UUID> mockedUUID = Mockito.mockStatic(UUID.class)) {
			mockedUUID.when(() -> UUID.randomUUID()).thenReturn(bridgeId);
			when(dbService.storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model))).thenReturn(null);
			when(converter.convertDiscoveryModels(bridgeId, List.of(model))).thenReturn(expected);

			final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

			assertNotNull(result);
			assertEquals(expected, result);

			verify(csDriver, never()).isBlacklisted("TestConsumer");
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
			verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
			verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
			verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
			verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
			verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
			verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
			mockedUUID.verify(() -> UUID.randomUUID());
			verify(dbService).storeBridgeDiscoveries(bridgeId, "TestCreator", List.of(model));
			verify(converter).convertDiscoveryModels(bridgeId, List.of(model));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryDMTranslatorNotFound() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", "testXml");
		opMap.put("output", null);
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				"testJson",
				null);

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testJson",
				null);
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetInputDataModelId("testXml");

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
		verify(csDriver).collectDataModelTranslatorCandidates(List.of(model));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryDMTranslatorBlacklisted() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", "testXml");
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				"testJson");
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetOutputDataModelId("testXml");

		final ServiceInstanceInterfaceResponseDTO dmTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12347));

		final ServiceInstanceResponseDTO dataModelTranslator = new ServiceInstanceResponseDTO(
				"DataModelTranslator|dataModelTranslation|1.0.0",
				new SystemResponseDTO("DataModelTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("dataModelTranslation", null, null),
				"1.0.0",
				null,
				Map.of("dataModelIds", List.of(List.of("testXml", "testJson"))),
				List.of(dmTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("InterfaceTranslator"))).thenReturn(List.of("InterfaceTranslator"));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of(dataModelTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("DataModelTranslator"))).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
		verify(csDriver).collectDataModelTranslatorCandidates(List.of(model));
		verify(csDriver).filterOutBlacklistedSystems(List.of("DataModelTranslator"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoDiscoveryDMTranslatorUnauthorizedToUse() {
		final Map<String, String> opMap = new HashMap<>(1);
		opMap.put("input", null);
		opMap.put("output", "testXml");
		final ServiceInstanceInterfaceResponseDTO targetIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("dataModels", Map.of("test-operation", opMap)));

		final List<NormalizedServiceInstanceDTO> candidates = List.of(new NormalizedServiceInstanceDTO("TestProvider|testService|1.0.0", "TestProvider", "testService", List.of(targetIntf)));
		final NormalizedTranslationDiscoveryRequestDTO dto = new NormalizedTranslationDiscoveryRequestDTO(
				"TestCreator",
				candidates,
				"TestConsumer",
				"test-operation",
				List.of("generic_mqtt"),
				null,
				"testJson");

		final Map<TranslationDiscoveryFlag, Boolean> flags = Map.of(
				TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false,
				TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false,
				TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, true,
				TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, true);

		final ServiceInstanceInterfaceResponseDTO iTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO interfaceTranslator = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				Map.of("interfaceBridge", Map.of("to", "generic_http", "from", List.of("generic_mqtt"))),
				List.of(iTranslatorIntf),
				null,
				null);

		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				"testJson");
		model.setFromInterfaceTemplate("generic_mqtt");
		model.setToInterfaceTemplate("generic_http");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInterfaceTranslatorPolicy("NONE");
		model.setInterfaceTranslatorProperties(Map.of("accessPort", 12345));
		model.setTargetPolicy("NONE");
		model.setTargetProperties(Map.of("dataModels", Map.of("test-operation", opMap)));
		model.setTargetOutputDataModelId("testXml");

		final ServiceInstanceInterfaceResponseDTO dmTranslatorIntf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12347));

		final ServiceInstanceResponseDTO dataModelTranslator = new ServiceInstanceResponseDTO(
				"DataModelTranslator|dataModelTranslation|1.0.0",
				new SystemResponseDTO("DataModelTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("dataModelTranslation", null, null),
				"1.0.0",
				null,
				Map.of("dataModelIds", List.of(List.of("testXml", "testJson"))),
				List.of(dmTranslatorIntf),
				null,
				null);

		when(dataModelIdentifierNormalizer.normalize("testXml")).thenReturn("testXml");
		doNothing().when(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		when(csDriver.collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates)).thenReturn(List.of(interfaceTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("InterfaceTranslator"))).thenReturn(List.of("InterfaceTranslator"));
		when(sysInfo.getSystemName()).thenReturn("TranslationManager");
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null)).thenReturn(List.of("InterfaceTranslator"));
		when(csDriver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator))).thenReturn(Map.of());
		when(itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates)).thenReturn(candidates);
		when(interfaceTranslatorMatchmaker.doMatchmaking(List.of(interfaceTranslator), Map.of())).thenReturn(interfaceTranslator);
		when(csDriver.collectDataModelTranslatorCandidates(List.of(model))).thenReturn(List.of(dataModelTranslator));
		when(csDriver.filterOutBlacklistedSystems(List.of("DataModelTranslator"))).thenReturn(List.of("DataModelTranslator"));
		when(csDriver.filterOutProvidersBecauseOfUnauthorization(List.of("DataModelTranslator"), "InterfaceTranslator", "dataModelTranslation", null)).thenReturn(List.of());

		final TranslationDiscoveryResponseDTO result = engine.doDiscovery(dto, flags, "origin");

		assertNotNull(result);
		assertNull(result.bridgeId());
		assertTrue(result.candidates().isEmpty());

		verify(csDriver, never()).isBlacklisted("TestConsumer");
		verify(csDriver, never()).filterOutBlacklistedSystems(List.of("TestProvider"));
		verify(csDriver, never()).filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");
		verify(dataModelIdentifierNormalizer).normalize("testXml");
		verify(dataModelIdentifierValidator).validateDataModelIdentifier("testXml");
		verify(csDriver).collectInterfaceTranslatorCandidates(List.of("generic_mqtt"), candidates);
		verify(csDriver).filterOutBlacklistedSystems(List.of("InterfaceTranslator"));
		verify(sysInfo).getSystemName();
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("InterfaceTranslator"), "TranslationManager", "interfaceBridgeManagement", null);
		verify(csDriver).generateTokenForManagerToInterfaceBridgeManagementService(List.of(interfaceTranslator));
		verify(itDriver).filterOutNotAppropriateTargetsForInterfaceTranslator(interfaceTranslator, null, "test-operation", candidates);
		verify(interfaceTranslatorMatchmaker).doMatchmaking(List.of(interfaceTranslator), Map.of());
		verify(csDriver).collectDataModelTranslatorCandidates(List.of(model));
		verify(csDriver).filterOutBlacklistedSystems(List.of("DataModelTranslator"));
		verify(csDriver).filterOutProvidersBecauseOfUnauthorization(List.of("DataModelTranslator"), "InterfaceTranslator", "dataModelTranslation", null);
	}
}