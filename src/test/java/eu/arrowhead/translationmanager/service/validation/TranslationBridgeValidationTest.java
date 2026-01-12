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
package eu.arrowhead.translationmanager.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryRequestDTO;

@ExtendWith(MockitoExtension.class)
public class TranslationBridgeValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TranslationBridgeValidation validator;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	@Mock
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefinitionNameNormalizer;

	@Mock
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	@Mock
	private ServiceOperationNameNormalizer operationNormalizer;

	@Mock
	private ServiceOperationNameValidator operationValidator;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private DataModelIdentifierNormalizer dataModelIdentifierNormalizer;

	@Mock
	private DataModelIdentifierValidator dataModelIdentifierValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester("Consumer", null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester("Consumer", ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester(null, "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterEmpty() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("", "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterInvalid() {
		when(systemNameNormalizer.normalize("inv@lid")).thenReturn("Inv@lid");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("Inv@lid");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("inv@lid", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("inv@lid");
		verify(systemNameValidator).validateSystemName("Inv@lid");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterRequesterOk() {
		when(systemNameNormalizer.normalize("consumer")).thenReturn("Consumer");
		doNothing().when(systemNameValidator).validateSystemName("Consumer");

		final String result = validator.validateAndNormalizeRequester("consumer", "origin");

		assertEquals("Consumer", result);

		verify(systemNameNormalizer).normalize("consumer");
		verify(systemNameValidator).validateSystemName("Consumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestOriginEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestRequestNull() {
		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", null, "origin"));

		assertEquals("Request is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidatesNull() {
		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				null,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("candidates list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidatesListEmpty() {
		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("candidates list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidatesListContainsNull() {
		final List<ServiceInstanceResponseDTO> list = new ArrayList<>(1);
		list.add(null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				list,
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("candidates list contains null element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInstanceIdNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				null,
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
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInstanceIdEmpty() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"",
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
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateProviderNull() {
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
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateProviderNameNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						null,
						null,
						null,
						null,
						null,
						null,
						null),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateProviderNameEmpty() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"",
						null,
						null,
						null,
						null,
						null,
						null),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Provider name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateServiceDefinitionNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				null,
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateServiceDefinitionNameNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO(null, null, null),
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateServiceDefinitionNameEmpty() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("", null, null),
				null,
				null,
				null,
				null,
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Service definition name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceListNull() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				null,
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceListEmpty() {
		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceListContainsNull() {
		final List<ServiceInstanceInterfaceResponseDTO> intfList = new ArrayList<>(1);
		intfList.add(null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				intfList,
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface list contains null element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceTemplateNameNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				null,
				null,
				null,
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Template name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfaceTemplateNameEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"",
				null,
				null,
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Template name is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePolicyNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				null,
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Policy is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePolicyEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"",
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Policy is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePolicyInvalid() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"INVALID",
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Policy is invalid: INVALID", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePropertiesNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				null);

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface properties are missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestCandidateInterfacePropertiesEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface properties are missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestOperationNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				null,
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("operation is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestOperationEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"",
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("operation is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestInterfaceTempateNamesListNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				null,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestInterfaceTempateNamesListEmpty() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				List.of(),
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list is missing", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestInterfaceTempateNamesListContainsNull() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				list,
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeDiscoveryRequestInterfaceTempateNamesListContainsEmptyElement() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of("accessPort", 12345));

		final ServiceInstanceResponseDTO candidate = new ServiceInstanceResponseDTO(
				"TestProvider|testService|1.0.0",
				new SystemResponseDTO(
						"TestProvider",
						null,
						null,
						null,
						null,
						null,
						null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				null,
				null);

		final TranslationDiscoveryRequestDTO dto = new TranslationDiscoveryRequestDTO(
				List.of(candidate),
				"test-operation",
				List.of(""),
				null,
				null);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeDiscoveryRequest("Consumer", dto, "origin"));

		assertEquals("Interface template names list contains null or empty element", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}
}