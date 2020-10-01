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

import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.scm.SCMProperty;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.scm.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.plugin.access.scm.v1.SCMPluginConstantsV1.*;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.SCM_EXTENSION;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class SCMExtensionV2Test {
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String PLUGIN_ID = "cd.go.example.plugin";
    private SCMExtensionV2 scmExtension;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private GoPluginDescriptor descriptor;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @BeforeEach
    void setUp() {
        initMocks(this);
        final List<String> goSupportedVersions = singletonList(SCMExtensionV2.VERSION);

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        when(descriptor.id()).thenReturn(PLUGIN_ID);
        when(pluginManager.getPluginDescriptorFor(PLUGIN_ID)).thenReturn(descriptor);
        when(pluginManager.isPluginOfType(SCM_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SCM_EXTENSION, goSupportedVersions)).thenReturn(SCMExtensionV2.VERSION);

        final PluginRequestHelper pluginRequestHelper = new PluginRequestHelper(pluginManager, goSupportedVersions, SCM_EXTENSION);
        scmExtension = new SCMExtensionV2(pluginRequestHelper);
    }

    @Test
    void shouldTalkToPluginToGetSCMConfiguration() {
        String responseBody = "{\"key-one\":{}}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        SCMPropertyConfiguration response = scmExtension.getSCMConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), REQUEST_SCM_CONFIGURATION, null);
        assertThat(response.list()).isNotEmpty();
    }

    @Test
    void shouldTalkToPluginToGetSCMView() {
        String responseBody = "{\"displayValue\":\"MySCMPlugin\", \"template\":\"<html>junk</html>\"}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        SCMView response = scmExtension.getSCMView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), REQUEST_SCM_VIEW, null);
        assertThat(response).isNotNull();
    }

    @Test
    void shouldTalkToPluginToCheckIfSCMConfigurationIsValid() {
        String responseBody = "[]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));

        ValidationResult response = scmExtension.isSCMConfigurationValid(PLUGIN_ID, scmPropertyConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), REQUEST_VALIDATE_SCM_CONFIGURATION, "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"}}}");
        assertThat(response.isSuccessful()).isTrue();
    }

    @Test
    void shouldTalkToPluginToCheckSCMConnectionSuccessful() {
        String responseBody = "{\"status\":\"success\",messages=[]}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));

        Result response = scmExtension.checkConnectionToSCM(PLUGIN_ID, scmPropertyConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), REQUEST_CHECK_SCM_CONNECTION, "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"}}}");
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.getMessages()).isEmpty();
    }

    @Test
    void shouldTalkToPluginToGetLatestModification() {
        String revisionJSON = "{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\"," +
                "\"data\":{},\"modifiedFiles\":[]}";
        String responseBody = "{\"revision\": " + revisionJSON + "}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));

        MaterialPollResult response = scmExtension.getLatestRevision(PLUGIN_ID, scmPropertyConfiguration, emptyMap(), "some-flyweight");

        assertRequest(requestArgumentCaptor.getValue(), REQUEST_LATEST_REVISION, "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"}},\"scm-data\":{},\"flyweight-folder\":\"some-flyweight\"}");
        assertThat(response.getMaterialData()).isNullOrEmpty();
        assertThat(response.getRevisions()).isNotEmpty();
        assertThat(response.getLatestRevision()).isNotNull();
    }

    @Test
    void shouldTalkToPluginToGetLatestModificationSinceLastRevision() throws ParseException {
        String responseBody = "";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));

        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        SCMRevision previouslyKnownRevision = new SCMRevision("abc.rpm", timestamp, "someuser", "comment", emptyMap(), null);

        MaterialPollResult response = scmExtension.latestModificationSince(PLUGIN_ID, scmPropertyConfiguration, emptyMap(), "", previouslyKnownRevision);

        assertRequest(requestArgumentCaptor.getValue(), REQUEST_LATEST_REVISIONS_SINCE, "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"}},\"scm-data\":{},\"flyweight-folder\":\"\",\"previous-revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{}}}");
        assertThat(response.getMaterialData()).isNullOrEmpty();
        assertThat(response.getRevisions()).isNullOrEmpty();
        assertThat(response.getLatestRevision()).isNull();
    }

    @Test
    void shouldTalkToPluginToCheckout() throws ParseException {
        String responseBody = "{\"status\":\"success\",messages=[]}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));

        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        SCMRevision revision = new SCMRevision("abc.rpm", timestamp, "someuser", "comment", emptyMap(), null);

        Result response = scmExtension.checkout(PLUGIN_ID, scmPropertyConfiguration, "folder", revision);

        assertRequest(requestArgumentCaptor.getValue(), REQUEST_CHECKOUT, "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"}},\"destination-folder\":\"folder\",\"revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{}}}");
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.getMessages()).isEmpty();
    }

    @Test
    void shouldHandleExceptionDuringPluginInteraction() {
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenThrow(new RuntimeException("exception-from-plugin"));
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        try {
            scmExtension.checkConnectionToSCM(PLUGIN_ID, scmPropertyConfiguration);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("exception-from-plugin");
        }
    }

    @Test
    void shouldTalkToPluginForGetCapabilities() {
        String responseBody = "{\"supported_webhooks\": []}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        Capabilities capabilities = scmExtension.getCapabilities(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), SCMPluginConstantsV2.GET_CAPABILITIES, null);
        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedWebhooks()).isEmpty();
    }

    @Test
    void shouldTalkToPluginForShouldUpdate() {
        String responseBody = "[]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));

        List<SCMPropertyConfiguration> configurations = scmExtension.shouldUpdate(PLUGIN_ID, "Github", "pull", "some-payload", singletonList(scmPropertyConfiguration));
        assertRequest(requestArgumentCaptor.getValue(), SCMPluginConstantsV2.SHOULD_UPDATE, "{\"provider\":\"Github\",\"event_type\":\"pull\",\"event_payload\":\"some-payload\",\"materials\":[{\"key-one\":{\"value\":\"value-one\"}}]}");
        assertThat(configurations).isEmpty();
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension()).isEqualTo(SCM_EXTENSION);
        assertThat(goPluginApiRequest.extensionVersion()).isEqualTo(SCMExtensionV2.VERSION);
        assertThat(goPluginApiRequest.requestName()).isEqualTo(requestName);
        assertThat(goPluginApiRequest.requestBody()).isEqualTo(requestBody);
    }
}
