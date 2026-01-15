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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyResponseDTO;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;

@ExtendWith(MockitoExtension.class)
public class CoreSystemsDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CoreSystemsDriver driver;

	@Mock
	private TranslationManagerSystemInfo sysInfo;

	@Mock
	private ArrowheadHttpService ahHttpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsBlacklistedNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.isBlacklisted(null));

		assertEquals("systemName is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsBlacklistedEmptyInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.isBlacklisted(""));

		assertEquals("systemName is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsBlacklistedOk() {
		// checking that filterOutBlacklistedSystems() is called (that sysInfo.getBlacklistCheckExcludeList() is called actually)

		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of("TestConsumer"));

		final boolean result = driver.isBlacklisted("TestConsumer");

		assertFalse(result);

		verify(sysInfo).getBlacklistCheckExcludeList();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsListNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutBlacklistedSystems(null));

		assertEquals("systemNames is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsListEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutBlacklistedSystems(List.of()));

		assertEquals("systemNames is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutBlacklistedSystems(list));

		assertEquals("systemNames list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsListContainsEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutBlacklistedSystems(List.of("")));

		assertEquals("systemNames list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsForbiddenException() {
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenThrow(new ForbiddenException("test", "origin"));

		final ArrowheadException ex = assertThrows(ForbiddenException.class,
				() -> driver.filterOutBlacklistedSystems(List.of("TestConsumer")));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsAuthException() {
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenThrow(new AuthException("test", "origin"));

		final ArrowheadException ex = assertThrows(AuthException.class,
				() -> driver.filterOutBlacklistedSystems(List.of("TestConsumer")));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsAnyOtherArrowheadExceptionForcedBlacklist() {
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenThrow(ExternalServerError.class);
		when(sysInfo.isBlacklistForced()).thenReturn(true);

		final List<String> result = driver.filterOutBlacklistedSystems(List.of("TestConsumer"));

		assertTrue(result.isEmpty());

		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
		verify(sysInfo).isBlacklistForced();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsAnyOtherArrowheadExceptionNotForcedBlacklist() {
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenThrow(ExternalServerError.class);
		when(sysInfo.isBlacklistForced()).thenReturn(false);

		final List<String> result = driver.filterOutBlacklistedSystems(List.of("TestConsumer"));

		assertEquals(List.of("TestConsumer"), result);

		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
		verify(sysInfo).isBlacklistForced();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsOk() {
		final BlacklistEntryDTO entry1 = new BlacklistEntryDTO("BadSystem", null, null, null, null, null, null, true);
		final BlacklistEntryDTO entry2 = new BlacklistEntryDTO("BadSystem", null, null, null, null, null, null, true);
		final BlacklistEntryDTO entry3 = new BlacklistEntryDTO("BadSystem", null, null, null, null, null, null, true);

		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenReturn(
						new BlacklistEntryListResponseDTO(List.of(entry1, entry2), 3),
						new BlacklistEntryListResponseDTO(List.of(entry3), 3));

		final List<String> result = driver.filterOutBlacklistedSystems(List.of("TestConsumer", "BadSystem"));

		assertEquals(List.of("TestConsumer"), result);

		verify(sysInfo, times(2)).getBlacklistCheckExcludeList();
		verify(ahHttpService, times(2)).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationCandidateListNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(null, null, null, null));

		assertEquals("candidates list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationCandidateListEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of(), null, null, null));

		assertEquals("candidates list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationCandidateListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(list, null, null, null));

		assertEquals("candidates list contains null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationCandidateListContainsEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of(""), null, null, null));

		assertEquals("candidates list contains null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationConsumerNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), null, null, null));

		assertEquals("consumer is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationConsumerEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "", null, null));

		assertEquals("consumer is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationServiceDefinitionNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", null, null));

		assertEquals("serviceDefinition is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationServiceDefinitionEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "", null));

		assertEquals("serviceDefinition is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationOk1() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"TestProvider",
				"TestConsumer",
				null,
				"SERVICE_DEF",
				"testService",
				"test-operation");
		final AuthorizationVerifyListRequestDTO requestList = new AuthorizationVerifyListRequestDTO(List.of(request));

		when(ahHttpService.consumeService("authorizationManagement", "check-policies", "ConsumerAuthorization", AuthorizationVerifyListResponseDTO.class, requestList))
				.thenReturn(new AuthorizationVerifyListResponseDTO(List.of(), 0));

		final List<String> result = driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");

		assertTrue(result.isEmpty());

		verify(ahHttpService).consumeService("authorizationManagement", "check-policies", "ConsumerAuthorization", AuthorizationVerifyListResponseDTO.class, requestList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationOk2() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"TestProvider",
				"TestConsumer",
				null,
				"SERVICE_DEF",
				"testService",
				"test-operation");
		final AuthorizationVerifyRequestDTO request2 = new AuthorizationVerifyRequestDTO(
				"TestProvider2",
				"TestConsumer",
				null,
				"SERVICE_DEF",
				"testService",
				"test-operation");
		final AuthorizationVerifyListRequestDTO requestList = new AuthorizationVerifyListRequestDTO(List.of(request, request2));
		final AuthorizationVerifyResponseDTO response1 = new AuthorizationVerifyResponseDTO(
				"TestProvider",
				"TestConsumer",
				null,
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"test-operation",
				true);
		final AuthorizationVerifyResponseDTO response2 = new AuthorizationVerifyResponseDTO(
				"TestProvider2",
				"TestConsumer",
				null,
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"test-operation",
				false);

		when(ahHttpService.consumeService("authorizationManagement", "check-policies", "ConsumerAuthorization", AuthorizationVerifyListResponseDTO.class, requestList))
				.thenReturn(new AuthorizationVerifyListResponseDTO(List.of(response1, response2), 2));

		final List<String> result = driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider", "TestProvider2"), "TestConsumer", "testService", "test-operation");

		assertEquals(List.of("TestProvider"), result);

		verify(ahHttpService).consumeService("authorizationManagement", "check-policies", "ConsumerAuthorization", AuthorizationVerifyListResponseDTO.class, requestList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceInputListNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.generateTokenForManagerToInterfaceBridgeManagementService(null));

		assertEquals("interfaceTranslators list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceInputListEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.generateTokenForManagerToInterfaceBridgeManagementService(List.of()));

		assertEquals("interfaceTranslators list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceInputListContainsNull() {
		final List<ServiceInstanceResponseDTO> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.generateTokenForManagerToInterfaceBridgeManagementService(list));

		assertEquals("interfaceTranslators list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceNoTokensNeeded() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO dto = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(intf),
				null,
				null);

		final Map<String, String> result = driver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(dto));

		assertTrue(result.isEmpty());
	}
}