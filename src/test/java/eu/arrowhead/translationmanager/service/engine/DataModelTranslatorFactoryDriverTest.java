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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DataModelTranslatorFactoryDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DataModelTranslatorFactoryDriver driver;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorPropertiesNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator(null, null, null));

		assertEquals("Factory interface properties is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorPropertiesEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator(Map.of(), null, null));

		assertEquals("Factory interface properties is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorFromNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator(Map.of("a", "b"), null, null));

		assertEquals("From data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorFromEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator(Map.of("a", "b"), "", null));

		assertEquals("From data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorToNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator(Map.of("a", "b"), "testXml", null));

		assertEquals("To data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorToEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator(Map.of("a", "b"), "testXml", ""));

		assertEquals("To data model identifier is missing", ex.getMessage());
	}
}