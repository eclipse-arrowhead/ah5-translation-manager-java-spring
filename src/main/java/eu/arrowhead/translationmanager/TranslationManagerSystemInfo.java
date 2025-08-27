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
package eu.arrowhead.translationmanager;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class TranslationManagerSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	@Value(Constants.$ENABLE_BLACKLIST_FILTER_WD)
	private boolean blacklistEnabled;

	@Value(Constants.$FORCE_BLACKLIST_FILTER_WD)
	private boolean forceBlackist;

	@Value(TranslationManagerConstants.$ENABLE_AUTHORIZATION_WD)
	private boolean authorizationEnabled;

	@Value(TranslationManagerConstants.$ENABLE_CUSTOM_CONFIGURATION_WD)
	private boolean customConfigurationEnabled;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return Constants.SYS_NAME_TRANSLATIONMANAGER;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION);

			if (AuthenticationPolicy.CERTIFICATE == this.getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		// TODO implement
		return List.of();
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public boolean isBlacklistEnabled() {
		return blacklistEnabled;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isBlacklistForced() {
		return forceBlackist;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAuthorizationEnabled() {
		return authorizationEnabled;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isCustomConfigurationEnabled() {
		return customConfigurationEnabled;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.MQTT_API_ENABLED,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.ENABLE_BLACKLIST_FILTER,
						Constants.FORCE_BLACKLIST_FILTER,
						Constants.MAX_PAGE_SIZE,
						Constants.NORMALIZATION_MODE,
						TranslationManagerConstants.ENABLE_AUTHORIZATION,
						TranslationManagerConstants.TRANSLATOR_SERVICE_MIN_AVAILABILITY,
						TranslationManagerConstants.ENABLE_CUSTOM_CONFIGURATION),
				TranslationManagerDefaults.class);
	}
}