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
package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.access.scm.v1.SCMExtensionV1;
import com.thoughtworks.go.plugin.access.scm.v2.SCMExtensionV2;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.scm.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.SCM_EXTENSION;
import static java.util.Arrays.asList;

@Component
public class SCMExtension extends AbstractExtension {
    public static final List<String> SCM_SUPPORTED_VERSIONS = asList(SCMExtensionV1.VERSION, SCMExtensionV2.VERSION);

    private Map<String, VersionedSCMExtension> scmExtensionMap = new HashMap<>();

    @Autowired
    public SCMExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry) {
        super(pluginManager, extensionsRegistry, new PluginRequestHelper(pluginManager, SCM_SUPPORTED_VERSIONS, SCM_EXTENSION), SCM_EXTENSION);
        scmExtensionMap.put(SCMExtensionV1.VERSION, new SCMExtensionV1(pluginRequestHelper));
        scmExtensionMap.put(SCMExtensionV2.VERSION, new SCMExtensionV2(pluginRequestHelper));

        registerHandler(SCMExtensionV1.VERSION, new PluginSettingsJsonMessageHandler1_0());
        registerHandler(SCMExtensionV2.VERSION, new PluginSettingsJsonMessageHandler1_0());
    }

    public SCMPropertyConfiguration getSCMConfiguration(String pluginId) {
        return getVersionedSCMExtension(pluginId).getSCMConfiguration(pluginId);
    }

    public SCMView getSCMView(String pluginId) {
        return getVersionedSCMExtension(pluginId).getSCMView(pluginId);
    }

    public ValidationResult isSCMConfigurationValid(String pluginId, final SCMPropertyConfiguration scmConfiguration) {
        return getVersionedSCMExtension(pluginId).isSCMConfigurationValid(pluginId, scmConfiguration);
    }

    public Result checkConnectionToSCM(String pluginId, final SCMPropertyConfiguration scmConfiguration) {
        return getVersionedSCMExtension(pluginId).checkConnectionToSCM(pluginId, scmConfiguration);
    }

    public MaterialPollResult getLatestRevision(String pluginId, final SCMPropertyConfiguration scmConfiguration, final Map<String, String> materialData, final String flyweightFolder) {
        return getVersionedSCMExtension(pluginId).getLatestRevision(pluginId, scmConfiguration, materialData, flyweightFolder);
    }

    public MaterialPollResult latestModificationSince(String pluginId, final SCMPropertyConfiguration scmConfiguration, final Map<String, String> materialData, final String flyweightFolder, final SCMRevision previouslyKnownRevision) {
        return getVersionedSCMExtension(pluginId).latestModificationSince(pluginId, scmConfiguration, materialData, flyweightFolder, previouslyKnownRevision);
    }

    public Result checkout(String pluginId, final SCMPropertyConfiguration scmConfiguration, final String destinationFolder, final SCMRevision revision) {
        return getVersionedSCMExtension(pluginId).checkout(pluginId, scmConfiguration, destinationFolder, revision);
    }

    public Capabilities getCapabilities(String pluginId) {
        return getVersionedSCMExtension(pluginId).getCapabilities(pluginId);
    }

    public List<SCMPropertyConfiguration> shouldUpdate(String pluginId, String provider, String eventType, String eventPayload, List<SCMPropertyConfiguration> materialsConfigured) {
        return getVersionedSCMExtension(pluginId).shouldUpdate(pluginId, provider, eventType, eventPayload, materialsConfigured);
    }

    @Override
    public List<String> goSupportedVersions() {
        return SCM_SUPPORTED_VERSIONS;
    }

    VersionedSCMExtension getVersionedSCMExtension(String pluginId) {
        final String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, SCM_EXTENSION, goSupportedVersions());
        return scmExtensionMap.get(resolvedExtensionVersion);
    }
}
