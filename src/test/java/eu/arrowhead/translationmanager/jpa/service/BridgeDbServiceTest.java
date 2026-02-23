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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.data.util.Pair;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDiscovery;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDetailsRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDiscoveryRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeHeaderRepository;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService.AbortResult;
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

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeDiscoveriesOk() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		final String modelJson = Utilities.toJson(model);
		final BridgeDiscovery discovery = new BridgeDiscovery(header, modelJson);

		when(headerRepository.saveAndFlush(header)).thenReturn(header);
		when(discoveryRepository.saveAllAndFlush(List.of(discovery))).thenReturn(List.of(discovery));

		final Pair<BridgeHeader, List<BridgeDiscovery>> result = dbService.storeBridgeDiscoveries(uuid, "Creator", List.of(model));

		assertEquals(header, result.getFirst());
		assertEquals(TranslationBridgeStatus.DISCOVERED, result.getFirst().getStatus());
		assertEquals(1, result.getSecond().size());
		assertEquals(discovery, result.getSecond().get(0));

		verify(headerRepository, times(2)).saveAndFlush(header);
		verify(discoveryRepository).saveAllAndFlush(List.of(discovery));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.selectBridgeFromDiscoveries(null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesInstanceIdNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, null));

		assertEquals("instanceId is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesInstanceIdEmpty() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, ""));

		assertEquals("instanceId is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesBridgeNotFound() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0"));

		assertEquals("Invalid bridge identifier: " + bridgeId, ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesNotAppropriateStatus() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.NEW);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0"));

		assertEquals("Invalid bridge identifier: " + bridgeId, ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesNotAppropriateModel() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.DISCOVERED);
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("OtherProvider|testService|1.0.0");
		final String modelJson = Utilities.toJson(model);
		final BridgeDiscovery discovery = new BridgeDiscovery(header, modelJson);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(discoveryRepository.findByHeader(header)).thenReturn(List.of(discovery));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0"));

		assertEquals("Invalid bridge identifier: " + bridgeId, ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(discoveryRepository).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.DISCOVERED);
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		final String modelJson = Utilities.toJson(model);
		final BridgeDiscovery discovery = new BridgeDiscovery(header, modelJson);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(discoveryRepository.findByHeader(header)).thenReturn(List.of(discovery));
		when(headerRepository.saveAndFlush(header)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0"));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(discoveryRepository).findByHeader(header);
		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSelectBridgeFromDiscoveriesOk() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setStatus(TranslationBridgeStatus.DISCOVERED);
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel();
		model.setTargetInstanceId("TestProvider|testService|1.0.0");
		model.setConsumer("TestConsumer");
		model.setProvider("TestProvider");
		model.setServiceDefinition("testService");
		model.setOperation("test-operation");
		model.setInterfaceTranslator("InterfaceTranslator");
		model.setInputDataModelTranslator("DataModelTranslator1");
		model.setOutputDataModelTranslator("DataModelTranslator2");
		final String modelJson = Utilities.toJson(model);
		final BridgeDiscovery discovery = new BridgeDiscovery(header, modelJson);
		final BridgeDetails details = new BridgeDetails(
				header,
				model.getConsumer(),
				model.getProvider(),
				model.getServiceDefinition(),
				model.getOperation(),
				model.getInterfaceTranslator(),
				"", // will be set later
				model.getInputDataModelTranslator(),
				null, // will be set later if necessary
				model.getOutputDataModelTranslator(),
				null); // will be set later if necessary

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(discoveryRepository.findByHeader(header)).thenReturn(List.of(discovery));
		when(headerRepository.saveAndFlush(header)).thenReturn(header);
		doNothing().when(discoveryRepository).deleteByHeader(header);
		doNothing().when(discoveryRepository).flush();
		when(detailsRepository.saveAndFlush(details)).thenReturn(details);

		final Pair<TranslationDiscoveryModel, BridgeDetails> result = dbService.selectBridgeFromDiscoveries(uuid, "TestProvider|testService|1.0.0");

		assertEquals(model, result.getFirst());
		assertEquals(details, result.getSecond());

		verify(headerRepository).findByUuid(bridgeId);
		verify(discoveryRepository).findByHeader(header);
		verify(headerRepository).saveAndFlush(header);
		verify(discoveryRepository).deleteByHeader(header);
		verify(discoveryRepository).flush();
		verify(detailsRepository).saveAndFlush(details);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.updateDetailsRecord(null));

		assertEquals("record is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordDbException() {
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);

		when(detailsRepository.findById(1L)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.updateDetailsRecord(details));

		assertEquals("Database operation error", ex.getMessage());

		verify(detailsRepository).findById(1L);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordNotFound() {
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);

		when(detailsRepository.findById(1L)).thenReturn(Optional.empty());

		final boolean result = dbService.updateDetailsRecord(details);

		assertFalse(result);

		verify(detailsRepository).findById(1L);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordEndState() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.CLOSED);
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);

		when(detailsRepository.findById(1L)).thenReturn(Optional.of(details));

		final boolean result = dbService.updateDetailsRecord(details);

		assertTrue(result);

		verify(detailsRepository).findById(1L);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDetailsRecordOk() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.DISCOVERED);
		final BridgeDetails details = new BridgeDetails();
		details.setId(1L);
		details.setHeader(header);

		when(detailsRepository.findById(1L)).thenReturn(Optional.of(details));
		when(detailsRepository.saveAndFlush(details)).thenReturn(details);

		final boolean result = dbService.updateDetailsRecord(details);

		assertFalse(result);

		verify(detailsRepository).findById(1L);
		verify(detailsRepository).saveAndFlush(details);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.bridgeInitialized(null));

		assertEquals("header is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedDbException() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.PENDING);

		when(headerRepository.saveAndFlush(header)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.bridgeInitialized(header));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedOk() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.PENDING);

		when(headerRepository.saveAndFlush(header)).thenReturn(header);

		final boolean result = dbService.bridgeInitialized(header);

		assertFalse(result);
		assertEquals(TranslationBridgeStatus.INITIALIZED, header.getStatus());

		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedEndState() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.ABORTED);

		final boolean result = dbService.bridgeInitialized(header);

		assertTrue(result);

		verify(headerRepository, never()).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBridgeInitializedOtherState() {
		final BridgeHeader header = new BridgeHeader();
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.INITIALIZED);

		final boolean result = dbService.bridgeInitialized(header);

		assertFalse(result);

		verify(headerRepository, never()).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeProblem(null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorMessageNull() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeProblem(uuid, null));

		assertEquals("errorMessage is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorMessageEmpty() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.storeBridgeProblem(uuid, ""));

		assertEquals("errorMessage is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenThrow(RuntimeException.class);

		assertDoesNotThrow(() -> dbService.storeBridgeProblem(uuid, "error"));

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorBridgeNotFound() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> dbService.storeBridgeProblem(uuid, "error"));

		verify(headerRepository).findByUuid(bridgeId);
		verify(headerRepository, never()).saveAndFlush(any(BridgeHeader.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorEndStateBridge() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.CLOSED);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));

		assertDoesNotThrow(() -> dbService.storeBridgeProblem(uuid, "error"));

		verify(headerRepository).findByUuid(bridgeId);
		verify(headerRepository, never()).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStoreBridgeProblemErrorOk() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.INITIALIZED);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));
		when(headerRepository.saveAndFlush(header)).thenReturn(header);

		assertDoesNotThrow(() -> dbService.storeBridgeProblem(uuid, "error"));
		assertEquals(TranslationBridgeStatus.ERROR, header.getStatus());
		assertEquals("error", header.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
		verify(headerRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeBridgeIdNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.abortBridge(null, null));

		assertEquals("bridgeId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeDbException() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.abortBridge(uuid, null));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeNotFound() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.empty());

		final AbortResult result = dbService.abortBridge(uuid, null);

		assertNotNull(result);
		assertFalse(result.abortHappened());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeNoPermission() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));

		final Throwable ex = assertThrows(
				ForbiddenException.class,
				() -> dbService.abortBridge(uuid, "Other"));

		assertEquals("No permission to abort bridge: " + bridgeId, ex.getMessage());

		verify(headerRepository).findByUuid(bridgeId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAbortBridgeEndStateBridge() {
		final String bridgeId = "42ab0775-26cc-49aa-87a4-2313f9d9975b";
		final UUID uuid = UUID.fromString(bridgeId);
		final BridgeHeader header = new BridgeHeader(uuid, "Creator");
		header.setId(1L);
		header.setStatus(TranslationBridgeStatus.CLOSED);

		when(headerRepository.findByUuid(bridgeId)).thenReturn(Optional.of(header));

		final AbortResult result = dbService.abortBridge(uuid, "Creator");

		assertNotNull(result);
		assertFalse(result.abortHappened());
		assertEquals(TranslationBridgeStatus.CLOSED, result.fromStatus());

		verify(headerRepository).findByUuid(bridgeId);
	}
}