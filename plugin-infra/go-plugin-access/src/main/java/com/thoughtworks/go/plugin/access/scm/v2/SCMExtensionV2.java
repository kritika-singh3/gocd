/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.plugin.access.scm.v2;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import com.thoughtworks.go.plugin.access.scm.VersionedSCMExtension;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.scm.Capabilities;

import java.util.List;
import java.util.Map;

public class SCMExtensionV2 implements VersionedSCMExtension {
    public static final String VERSION = "2.0";
    private final PluginRequestHelper pluginRequestHelper;
    private final SCMExtensionConverterV2 scmExtensionConverterV2;

    public SCMExtensionV2(PluginRequestHelper pluginRequestHelper) {
        this.pluginRequestHelper = pluginRequestHelper;
        this.scmExtensionConverterV2 = new SCMExtensionConverterV2();
    }

    public SCMPropertyConfiguration getSCMConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.REQUEST_SCM_CONFIGURATION, new DefaultPluginInteractionCallback<>() {

            @Override
            public SCMPropertyConfiguration onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForSCMConfiguration(responseBody);
            }
        });
    }

    public SCMView getSCMView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.REQUEST_SCM_VIEW, new DefaultPluginInteractionCallback<>() {

            @Override
            public SCMView onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForSCMView(responseBody);
            }
        });
    }

    public ValidationResult isSCMConfigurationValid(String pluginId, final SCMPropertyConfiguration scmConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.REQUEST_VALIDATE_SCM_CONFIGURATION, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV2.requestMessageForIsSCMConfigurationValid(scmConfiguration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForIsSCMConfigurationValid(responseBody);
            }
        });
    }

    public Result checkConnectionToSCM(String pluginId, final SCMPropertyConfiguration scmConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.REQUEST_CHECK_SCM_CONNECTION, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV2.requestMessageForCheckConnectionToSCM(scmConfiguration);
            }

            @Override
            public Result onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForCheckConnectionToSCM(responseBody);
            }
        });
    }

    public MaterialPollResult getLatestRevision(String pluginId, final SCMPropertyConfiguration scmConfiguration, final Map<String, String> materialData, final String flyweightFolder) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.REQUEST_LATEST_REVISION, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV2.requestMessageForLatestRevision(scmConfiguration, materialData, flyweightFolder);
            }

            @Override
            public MaterialPollResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForLatestRevision(responseBody);
            }
        });
    }

    public MaterialPollResult latestModificationSince(String pluginId, final SCMPropertyConfiguration scmConfiguration, final Map<String, String> materialData, final String flyweightFolder, final SCMRevision previouslyKnownRevision) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.REQUEST_LATEST_REVISIONS_SINCE, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV2.requestMessageForLatestRevisionsSince(scmConfiguration, materialData, flyweightFolder, previouslyKnownRevision);
            }

            @Override
            public MaterialPollResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForLatestRevisionsSince(responseBody);
            }
        });
    }

    public Result checkout(String pluginId, final SCMPropertyConfiguration scmConfiguration, final String destinationFolder, final SCMRevision revision) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.REQUEST_CHECKOUT, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV2.requestMessageForCheckout(scmConfiguration, destinationFolder, revision);
            }

            @Override
            public Result onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForCheckout(responseBody);
            }
        });
    }

    @Override
    public Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.GET_CAPABILITIES, new DefaultPluginInteractionCallback<>() {
            @Override
            public Capabilities onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForGetCapabilities(responseBody);
            }
        });
    }

    @Override
    public List<SCMPropertyConfiguration> shouldUpdate(String pluginId, String provider, String eventType, String eventPayload, List<SCMPropertyConfiguration> materialsConfigured) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV2.SHOULD_UPDATE, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV2.requestMessageForShouldUpdate(provider, eventType, eventPayload, materialsConfigured);
            }

            @Override
            public List<SCMPropertyConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV2.responseMessageForShouldUpdate(responseBody);
            }
        });
    }

}
