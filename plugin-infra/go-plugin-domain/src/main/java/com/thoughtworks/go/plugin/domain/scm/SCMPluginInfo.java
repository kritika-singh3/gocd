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
package com.thoughtworks.go.plugin.domain.scm;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.Objects;

public class SCMPluginInfo extends PluginInfo {

    private final String displayName;
    private final PluggableInstanceSettings scmSettings;
    private final Capabilities capabilities;

    public SCMPluginInfo(PluginDescriptor descriptor, String displayName, PluggableInstanceSettings scmSettings, PluggableInstanceSettings pluginSettings, Capabilities capabilities) {
        super(descriptor, PluginConstants.SCM_EXTENSION, pluginSettings, null);
        this.displayName = displayName;
        this.scmSettings = scmSettings;
        this.capabilities = capabilities;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PluggableInstanceSettings getScmSettings() {
        return scmSettings;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SCMPluginInfo that = (SCMPluginInfo) o;
        return Objects.equals(displayName, that.displayName) &&
                Objects.equals(scmSettings, that.scmSettings) &&
                Objects.equals(capabilities, that.capabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), displayName, scmSettings, capabilities);
    }
}
