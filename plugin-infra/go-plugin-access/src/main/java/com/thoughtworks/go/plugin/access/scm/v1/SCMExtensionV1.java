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

package com.thoughtworks.go.plugin.access.scm.v1;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.scm.SCMMaterial;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import com.thoughtworks.go.plugin.access.scm.VersionedSCMExtension;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.scm.Capabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class SCMExtensionV1 implements VersionedSCMExtension {
    private static final Logger LOG = LoggerFactory.getLogger(SCMExtensionV1.class);
    public static final String VERSION = "1.0";
    private final PluginRequestHelper pluginRequestHelper;
    private final SCMExtensionConverterV1 scmExtensionConverterV1;

    public SCMExtensionV1(PluginRequestHelper pluginRequestHelper) {
        this.pluginRequestHelper = pluginRequestHelper;
        this.scmExtensionConverterV1 = new SCMExtensionConverterV1();
    }

    public SCMPropertyConfiguration getSCMConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV1.REQUEST_SCM_CONFIGURATION, new DefaultPluginInteractionCallback<>() {

            @Override
            public SCMPropertyConfiguration onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV1.responseMessageForSCMConfiguration(responseBody);
            }
        });
    }

    public SCMView getSCMView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV1.REQUEST_SCM_VIEW, new DefaultPluginInteractionCallback<>() {

            @Override
            public SCMView onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV1.responseMessageForSCMView(responseBody);
            }
        });
    }

    public ValidationResult isSCMConfigurationValid(String pluginId, final SCMPropertyConfiguration scmConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV1.REQUEST_VALIDATE_SCM_CONFIGURATION, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV1.requestMessageForIsSCMConfigurationValid(scmConfiguration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV1.responseMessageForIsSCMConfigurationValid(responseBody);
            }
        });
    }

    public Result checkConnectionToSCM(String pluginId, final SCMPropertyConfiguration scmConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV1.REQUEST_CHECK_SCM_CONNECTION, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV1.requestMessageForCheckConnectionToSCM(scmConfiguration);
            }

            @Override
            public Result onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV1.responseMessageForCheckConnectionToSCM(responseBody);
            }
        });
    }

    public MaterialPollResult getLatestRevision(String pluginId, final SCMPropertyConfiguration scmConfiguration, final Map<String, String> materialData, final String flyweightFolder) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV1.REQUEST_LATEST_REVISION, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV1.requestMessageForLatestRevision(scmConfiguration, materialData, flyweightFolder);
            }

            @Override
            public MaterialPollResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV1.responseMessageForLatestRevision(responseBody);
            }
        });
    }

    public MaterialPollResult latestModificationSince(String pluginId, final SCMPropertyConfiguration scmConfiguration, final Map<String, String> materialData, final String flyweightFolder, final SCMRevision previouslyKnownRevision) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV1.REQUEST_LATEST_REVISIONS_SINCE, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV1.requestMessageForLatestRevisionsSince(scmConfiguration, materialData, flyweightFolder, previouslyKnownRevision);
            }

            @Override
            public MaterialPollResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV1.responseMessageForLatestRevisionsSince(responseBody);
            }
        });
    }

    public Result checkout(String pluginId, final SCMPropertyConfiguration scmConfiguration, final String destinationFolder, final SCMRevision revision) {
        return pluginRequestHelper.submitRequest(pluginId, SCMPluginConstantsV1.REQUEST_CHECKOUT, new DefaultPluginInteractionCallback<>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return scmExtensionConverterV1.requestMessageForCheckout(scmConfiguration, destinationFolder, revision);
            }

            @Override
            public Result onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return scmExtensionConverterV1.responseMessageForCheckout(responseBody);
            }
        });
    }

    @Override
    public Capabilities getCapabilities(String pluginId) {
        LOG.warn("Plugin: '{}' uses scm extension v1 and getCapabilities calls are not supported by scm V1", pluginId);
        return null;
    }

    @Override
    public List<SCMMaterial> shouldUpdate(String pluginId, String provider, String eventType, String eventPayload, List<SCMMaterial> materialsConfigured) {
        throw new UnsupportedOperationException(format("Plugin: '%s' uses scm extension v1 and shouldUpdate calls are not supported by scm V1", pluginId));
    }
}
