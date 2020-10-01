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
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.thoughtworks.go.plugin.access.scm.SCMExtension.SCM_SUPPORTED_VERSIONS;
import static com.thoughtworks.go.plugin.access.scm.v1.SCMPluginConstantsV1.REQUEST_SCM_CONFIGURATION;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.SCM_EXTENSION;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
public class SCMExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";

    @Mock
    private PluginManager pluginManager;
    @Mock
    private ExtensionsRegistry extensionsRegistry;
    @Mock
    private SCMExtension scmExtension;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeEach
    void setUp() {
        initMocks(this);
        scmExtension = new SCMExtension(pluginManager, extensionsRegistry);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SCM_EXTENSION, singletonList("1.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(SCM_EXTENSION, PLUGIN_ID)).thenReturn(true);
    }

    @Test
    void shouldHaveVersionedSCMExtensionForAllSupportedVersions() {
        for (String supportedVersion : SCM_SUPPORTED_VERSIONS) {
            final String message = String.format("Must define versioned extension class for %s extension with version %s", SCM_EXTENSION, supportedVersion);

            when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SCM_EXTENSION, SCM_SUPPORTED_VERSIONS)).thenReturn(supportedVersion);

            final VersionedSCMExtension extension = this.scmExtension.getVersionedSCMExtension(PLUGIN_ID);

            assertThat(extension).as(message).isNotNull();

            assertThat(ReflectionUtil.getField(extension, "VERSION")).isEqualTo(supportedVersion);
        }
    }

    @Test
    void shouldExtendAbstractExtension() {
        assertThat(scmExtension).isInstanceOf(AbstractExtension.class);
    }

    @Test
    void shouldCallTheVersionedExtensionBasedOnResolvedVersion() {
        ArgumentCaptor<GoPluginApiRequest> argumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), argumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success("{\"key-one\":{}}"));

        scmExtension.getSCMConfiguration(PLUGIN_ID);

        final GoPluginApiRequest request = argumentCaptor.getValue();
        assertThat(request.requestName()).isEqualTo(REQUEST_SCM_CONFIGURATION);
        assertThat(request.extensionVersion()).isEqualTo("1.0");
        assertThat(request.extension()).isEqualTo(SCM_EXTENSION);
        assertThatJson(request.requestBody()).isEqualTo(null);
    }
}
