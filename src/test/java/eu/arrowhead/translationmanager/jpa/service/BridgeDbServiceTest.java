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
package eu.arrowhead.translationmanager.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDetailsRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDiscoveryRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeHeaderRepository;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationReportRequestDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@ExtendWith(MockitoExtension.class)
public class BridgeDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private BridgeDbService dbService;

	@Mock
	private BridgeHeaderRepository headerRepository;

	@Mock
	private BridgeDetailsRepository detailsRepository;

	@Mock
	private BridgeDiscoveryRepository discoveryRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.handleTranslationReport(null));

		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportBridgeIdNull() {
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO("Requester", null, null, null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportTimestampNull() {
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString("42ab0775-26cc-49aa-87a4-2313f9d9975b"),
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("timestamp is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportNoBridge1() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.empty());

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Invalid bridge id: 42ab0775-26cc-49aa-87a4-2313f9d9975b", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportNoBridge2() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.empty());

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Invalid bridge id: 42ab0775-26cc-49aa-87a4-2313f9d9975b", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportWrongRequester() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"Requester",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Requester has no permission to report about the specified translation bridge", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportNotActiveBridge() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"InterfaceTranslator",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.USED,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.CLOSED);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Invalid reporting case", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportInvalidTransition() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"InterfaceTranslator",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				null,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.USED);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Invalid reporting case", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"InterfaceTranslator",
				UUID.fromString(bridgeId),
				ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC)),
				TranslationBridgeEventState.INTERNAL_ERROR,
				"Error");

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.USED);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));
		when(headerRepository.saveAndFlush(header)).thenThrow(RuntimeException.class);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> dbService.handleTranslationReport(dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHandleTranslationReportOk() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final ZonedDateTime timestamp = ZonedDateTime.of(2026, 2, 20, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
		final NormalizedTranslationReportRequestDTO dto = new NormalizedTranslationReportRequestDTO(
				"InterfaceTranslator",
				UUID.fromString(bridgeId),
				timestamp,
				TranslationBridgeEventState.USED,
				null);

		final BridgeHeader header = new BridgeHeader(UUID.fromString(bridgeId), "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.INITIALIZED);
		header.setUsageReportCount(0);

		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);
		details.setInterfaceTranslator("InterfaceTranslator");

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(detailsRepository.findByHeader(header)).thenReturn(Optional.of(details));
		when(headerRepository.saveAndFlush(header)).thenReturn(header);

		assertDoesNotThrow(() -> dbService.handleTranslationReport(dto));

		assertEquals(TranslationBridgeStatus.USED, header.getStatus());
		assertEquals(timestamp, header.getAlivesAt());
		assertEquals(1, header.getUsageReportCount());

		verify(headerRepository).findByUuid(bridgeId);
		verify(detailsRepository).findByHeader(header);
		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(null, null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesCreatedByNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), null, null));

		assertEquals("createdBy is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesCreatedByEmpty() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "", null));

		assertEquals("createdBy is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesModelsNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "Creator", null));

		assertEquals("models list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesModelsEmpty() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "Creator", List.of()));

		assertEquals("models list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesModelsContainsNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final List<TranslationDiscoveryModel> list = new ArrayList<>(1);
		list.add(null);
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "Creator", list));

		assertEquals("models list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");

		when(headerRepository.saveAndFlush(any(BridgeHeader.class))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.storeBridgeDiscoveries(UUID.fromString(bridgeId), "Creator", List.of(model)));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).saveAndFlush(any(BridgeHeader.class));
	}
}