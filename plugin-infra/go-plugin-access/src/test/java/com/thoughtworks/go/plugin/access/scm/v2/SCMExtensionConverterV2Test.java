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

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.scm.SCMProperty;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedAction;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedFile;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.scm.Capabilities;
import com.thoughtworks.go.plugin.domain.scm.WebhookSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class SCMExtensionConverterV2Test {
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private SCMExtensionConverterV2 scmExtensionConverterV2;
    private SCMPropertyConfiguration scmPropertyConfiguration;
    private Map<String, String> materialData;

    @BeforeEach
    void setUp() throws Exception {
        scmExtensionConverterV2 = new SCMExtensionConverterV2();
        scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));
        scmPropertyConfiguration.add(new SCMProperty("key-two", "value-two"));
        materialData = new HashMap<>();
        materialData.put("key-one", "value-one");
    }

    @Test
    void shouldBuildSCMConfigurationFromResponseBody() throws Exception {
        String responseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";
        SCMPropertyConfiguration scmConfiguration = scmExtensionConverterV2.responseMessageForSCMConfiguration(responseBody);

        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-one"), "key-one", "", true, true, false, "", 0);
        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    void shouldBuildSCMViewFromResponse() {
        String jsonResponse = "{\"displayValue\":\"MySCMPlugin\", \"template\":\"<html>junk</html>\"}";

        SCMView view = scmExtensionConverterV2.responseMessageForSCMView(jsonResponse);

        assertThat(view.displayValue()).isEqualTo("MySCMPlugin");
        assertThat(view.template()).isEqualTo("<html>junk</html>");
    }

    @Test
    void shouldBuildRequestBodyForCheckSCMConfigurationValidRequest() throws Exception {
        String requestMessage = scmExtensionConverterV2.requestMessageForIsSCMConfigurationValid(scmPropertyConfiguration);

        assertThat(requestMessage).isEqualTo("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}");
    }

    @Test
    void shouldBuildValidationResultFromCheckSCMConfigurationValidResponse() throws Exception {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";
        ValidationResult validationResult = scmExtensionConverterV2.responseMessageForIsSCMConfigurationValid(responseBody);

        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    void shouldBuildSuccessValidationResultFromCheckSCMConfigurationValidResponse() throws Exception {
        assertThat(scmExtensionConverterV2.responseMessageForIsSCMConfigurationValid("").isSuccessful()).isTrue();
        assertThat(scmExtensionConverterV2.responseMessageForIsSCMConfigurationValid(null).isSuccessful()).isTrue();
    }

    @Test
    void shouldBuildRequestBodyForCheckSCMConnectionRequest() throws Exception {
        String requestMessage = scmExtensionConverterV2.requestMessageForCheckConnectionToSCM(scmPropertyConfiguration);

        assertThat(requestMessage).isEqualTo("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}");
    }

    @Test
    void shouldBuildSuccessResultFromCheckSCMConnectionResponse() throws Exception {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = scmExtensionConverterV2.responseMessageForCheckConnectionToSCM(responseBody);

        assertSuccessResult(result, asList("message-one", "message-two"));
    }

    @Test
    void shouldBuildFailureResultFromCheckSCMConnectionResponse() throws Exception {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = scmExtensionConverterV2.responseMessageForCheckConnectionToSCM(responseBody);

        assertFailureResult(result, asList("message-one", "message-two"));
    }

    @Test
    void shouldHandleNullMessagesForCheckSCMConnectionResponse() throws Exception {
        assertSuccessResult(scmExtensionConverterV2.responseMessageForCheckConnectionToSCM("{\"status\":\"success\"}"), new ArrayList<>());
        assertFailureResult(scmExtensionConverterV2.responseMessageForCheckConnectionToSCM("{\"status\":\"failure\"}"), new ArrayList<>());
    }

    @Test
    void shouldBuildRequestBodyForLatestRevisionRequest() throws Exception {
        String requestBody = scmExtensionConverterV2.requestMessageForLatestRevision(scmPropertyConfiguration, materialData, "flyweight");

        assertThat(requestBody).isEqualTo("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"scm-data\":{\"key-one\":\"value-one\"},\"flyweight-folder\":\"flyweight\"}");
    }

    @Test
    void shouldBuildSCMRevisionFromLatestRevisionResponse() throws Exception {
        String revisionJSON = "{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"},{\"fileName\":\"f2\",\"action\":\"modified\"},{\"fileName\":\"f3\",\"action\":\"deleted\"}]}";
        String responseBody = "{\"revision\": " + revisionJSON + "}";
        MaterialPollResult pollResult = scmExtensionConverterV2.responseMessageForLatestRevision(responseBody);

        assertThat(pollResult.getMaterialData()).isNull();
        assertSCMRevision(pollResult.getLatestRevision(), "r1", "some-user", "2011-07-14T19:43:37.100Z", "comment", asList(new ModifiedFile("f1", ModifiedAction.added), new ModifiedFile("f2", ModifiedAction.modified), new ModifiedFile("f3", ModifiedAction.deleted)));
    }

    @Test
    void shouldBuildSCMDataFromLatestRevisionResponse() throws Exception {
        String responseBodyWithSCMData = "{\"revision\":{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\"},\"scm-data\":{\"key-one\":\"value-one\"}}";
        MaterialPollResult pollResult = scmExtensionConverterV2.responseMessageForLatestRevision(responseBodyWithSCMData);

        Map<String, String> scmData = new HashMap<>();
        scmData.put("key-one", "value-one");
        assertThat(pollResult.getMaterialData()).isEqualTo(scmData);
        assertThat(pollResult.getRevisions().get(0).getRevision()).isEqualTo("r1");
    }

    @Test
    void shouldBuildRequestBodyForLatestRevisionsSinceRequest() throws Exception {
        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map data = new LinkedHashMap();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        SCMRevision previouslyKnownRevision = new SCMRevision("abc.rpm", timestamp, "someuser", "comment", data, null);
        String requestBody = scmExtensionConverterV2.requestMessageForLatestRevisionsSince(scmPropertyConfiguration, materialData, "flyweight", previouslyKnownRevision);

        String expectedValue = "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"scm-data\":{\"key-one\":\"value-one\"},\"flyweight-folder\":\"flyweight\"," +
                "\"previous-revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";
        assertThat(requestBody).isEqualTo(expectedValue);
    }

    @Test
    void shouldBuildSCMRevisionsFromLatestRevisionsSinceResponse() throws Exception {
        String r1 = "{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"},{\"fileName\":\"f2\",\"action\":\"modified\"},{\"fileName\":\"f3\",\"action\":\"deleted\"}]}";
        String r2 = "{\"revision\":\"r2\",\"timestamp\":\"2011-07-14T19:43:37.101Z\",\"user\":\"new-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"}]}";
        String responseBody = "{\"revisions\":[" + r1 + "," + r2 + "]}";
        MaterialPollResult pollResult = scmExtensionConverterV2.responseMessageForLatestRevisionsSince(responseBody);

        assertThat(pollResult.getMaterialData()).isNull();
        List<SCMRevision> scmRevisions = pollResult.getRevisions();
        assertThat(scmRevisions.size()).isEqualTo(2);
        assertSCMRevision(scmRevisions.get(0), "r1", "some-user", "2011-07-14T19:43:37.100Z", "comment", asList(new ModifiedFile("f1", ModifiedAction.added), new ModifiedFile("f2", ModifiedAction.modified), new ModifiedFile("f3", ModifiedAction.deleted)));
        assertSCMRevision(scmRevisions.get(1), "r2", "new-user", "2011-07-14T19:43:37.101Z", "comment", asList(new ModifiedFile("f1", ModifiedAction.added)));
    }

    @Test
    void shouldBuildSCMDataFromLatestRevisionsSinceResponse() throws Exception {
        String responseBodyWithSCMData = "{\"revisions\":[],\"scm-data\":{\"key-one\":\"value-one\"}}";
        MaterialPollResult pollResult = scmExtensionConverterV2.responseMessageForLatestRevisionsSince(responseBodyWithSCMData);

        Map<String, String> scmData = new HashMap<>();
        scmData.put("key-one", "value-one");
        assertThat(pollResult.getMaterialData()).isEqualTo(scmData);
        assertThat(pollResult.getRevisions().isEmpty()).isTrue();
    }

    @Test
    void shouldBuildNullSCMRevisionFromLatestRevisionsSinceWhenEmptyResponse() throws Exception {
        MaterialPollResult pollResult = scmExtensionConverterV2.responseMessageForLatestRevisionsSince("");
        assertThat(pollResult.getRevisions()).isNull();
        assertThat(pollResult.getMaterialData()).isNull();
        pollResult = scmExtensionConverterV2.responseMessageForLatestRevisionsSince(null);
        assertThat(pollResult.getRevisions()).isNull();
        assertThat(pollResult.getMaterialData()).isNull();
    }

    @Test
    void shouldBuildRequestBodyForCheckoutRequest() throws Exception {
        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map data = new LinkedHashMap();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        SCMRevision revision = new SCMRevision("abc.rpm", timestamp, "someuser", "comment", data, null);
        String requestBody = scmExtensionConverterV2.requestMessageForCheckout(scmPropertyConfiguration, "destination", revision);

        String expectedValue = "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"destination-folder\":\"destination\"," +
                "\"revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";
        assertThat(requestBody).isEqualTo(expectedValue);
    }

    @Test
    void shouldBuildResultFromCheckoutResponse() throws Exception {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = scmExtensionConverterV2.responseMessageForCheckout(responseBody);

        assertFailureResult(result, asList("message-one", "message-two"));
    }

    @Test
    void shouldValidateIncorrectJsonResponseForSCMConfiguration() {
        assertThat(errorMessageForSCMConfiguration("")).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForSCMConfiguration(null)).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForSCMConfiguration("[{\"key-one\":\"value\"},{\"key-two\":\"value\"}]")).isEqualTo("Unable to de-serialize json response. SCM configuration should be returned as a map");
        assertThat(errorMessageForSCMConfiguration("{\"\":{}}")).isEqualTo("Unable to de-serialize json response. SCM configuration key cannot be empty");
        assertThat(errorMessageForSCMConfiguration("{\"key\":[{}]}")).isEqualTo("Unable to de-serialize json response. SCM configuration properties for key 'key' should be represented as a Map");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":100}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":100}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":100}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-name\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-name\":100}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":10.0}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
    }

    @Test
    void shouldValidateIncorrectJsonResponseForSCMView() {
        assertThat(errorMessageForSCMView("{\"template\":\"<html>junk</html>\"}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'displayValue' is a required field.");
        assertThat(errorMessageForSCMView("{\"displayValue\":\"MySCMPlugin\"}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'template' is a required field.");
        assertThat(errorMessageForSCMView("{\"displayValue\":null, \"template\":\"<html>junk</html>\"}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'displayValue' is a required field.");
        assertThat(errorMessageForSCMView("{\"displayValue\":true, \"template\":null}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'displayValue' should be of type string.");
        assertThat(errorMessageForSCMView("{\"displayValue\":\"MySCMPlugin\", \"template\":true}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'template' should be of type string.");
    }

    @Test
    void shouldValidateIncorrectJsonForSCMRevisions() {
        assertThat(errorMessageForSCMRevisions("{\"revisions\":{}}")).isEqualTo("Unable to de-serialize json response. 'revisions' should be of type list of map");
        assertThat(errorMessageForSCMRevisions("{\"revisions\":[\"crap\"]}")).isEqualTo("Unable to de-serialize json response. SCM revision should be of type map");
    }

    @Test
    void shouldValidateIncorrectJsonForSCMRevision() {
        assertThat(errorMessageForSCMRevision("")).isEqualTo("Unable to de-serialize json response. SCM revision cannot be empty");
        assertThat(errorMessageForSCMRevision("{\"revision\":[]}")).isEqualTo("Unable to de-serialize json response. SCM revision should be of type map");
        assertThat(errorMessageForSCMRevision("{\"crap\":{}}")).isEqualTo("Unable to de-serialize json response. SCM revision cannot be empty");
    }

    @Test
    void shouldValidateIncorrectJsonForEachRevision() {
        assertThat(errorMessageForEachRevision("{\"revision\":{}}")).isEqualTo("SCM revision should be of type string");
        assertThat(errorMessageForEachRevision("{\"revision\":\"\"}")).isEqualTo("SCM revision's 'revision' is a required field");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":{}}")).isEqualTo("SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty");
        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"\"}")).isEqualTo("SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty");
        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"12-01-2014\"}")).isEqualTo("SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"revisionComment\":{}}")).isEqualTo("SCM revision comment should be of type string");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":{}}")).isEqualTo("SCM revision user should be of type string");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":{}}")).isEqualTo("SCM revision 'modifiedFiles' should be of type list of map");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[\"crap\"]}")).isEqualTo("SCM revision 'modified file' should be of type map");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":{}}]}")).isEqualTo("modified file 'fileName' should be of type string");
        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"\"}]}")).isEqualTo("modified file 'fileName' is a required field");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":{}}]}")).isEqualTo("modified file 'action' should be of type string");
        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"crap\"}]}")).isEqualTo("modified file 'action' can only be added, modified, deleted");
    }

    @Test
    void shouldValidateIncorrectJsonForSCMData() {
        assertThat(errorMessageForSCMData("{\"scm-data\":[]}")).isEqualTo("Unable to de-serialize json response. SCM data should be of type map");
    }

    @Test
    void shouldBuildCapabilities() {
        String responseBody = "{" +
                "    \"supported_webhooks\": [" +
                "        {" +
                "            \"provider\": \"GitHub\"," +
                "            \"events\": [\"pull\"]" +
                "        }" +
                "    ]" +
                "}";
        Capabilities capabilities = scmExtensionConverterV2.responseMessageForGetCapabilities(responseBody);

        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedWebhooks()).isNotEmpty();
        WebhookSupport webhookSupport = capabilities.getSupportedWebhooks().get(0);
        assertThat(webhookSupport.getProvider()).isEqualTo("GitHub");
        assertThat(webhookSupport.getEvents()).containsExactly("pull");
    }

    @Test
    void shouldBuildRequestMessageForShouldUpdate() {
        String requestBody = scmExtensionConverterV2.requestMessageForShouldUpdate("Github", "pull", "{\"action\":\"synchronize\",\"number\":1}", singletonList(scmPropertyConfiguration));

        String expected = "{" +
                "\"provider\":\"Github\"," +
                "\"event_type\":\"pull\"," +
                "\"event_payload\":\"{\\\"action\\\":\\\"synchronize\\\",\\\"number\\\":1}\"," +
                "\"scm-configurations\":[" +
                "{" +
                "\"key-one\":{\"value\":\"value-one\"}," +
                "\"key-two\":{\"value\":\"value-two\"}" +
                "}]}";
        assertThat(requestBody).isEqualTo(expected);
    }

    @Test
    void shouldBuildSCMConfigurationFromResponseBodyForShouldUpdate() throws Exception {
        String responseBody = "[{" +
                "\"key-one\":{\"value\":\"value-one\"}," +
                "\"key-two\":{\"value\":\"value-two\"}" +
                "}]";
        List<SCMPropertyConfiguration> propertyConfigurations = scmExtensionConverterV2.responseMessageForShouldUpdate(responseBody);

        assertThat(propertyConfigurations).isNotEmpty();
        List<? extends Property> properties = propertyConfigurations.get(0).list();
        assertThat(properties).hasSize(2);
    }

    @Test
    void shouldValidateIncorrectJsonResponseForShouldUpdate() {
        assertThat(errorMessageForShouldUpdate("")).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForShouldUpdate(null)).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForShouldUpdate("{\"\":{}}")).isEqualTo("Unable to de-serialize json response. The result of shouldUpdate should be returned as a list of map of scm configurations");
        assertThat(errorMessageForShouldUpdate("[{\"\":{}}]")).isEqualTo("Unable to de-serialize json response. SCM configuration key cannot be empty");
        assertThat(errorMessageForShouldUpdate("[{\"key\":[{}]}]")).isEqualTo("Unable to de-serialize json response. SCM configuration properties for key 'key' should be represented as a Map");

        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"part-of-identity\":\"true\"}}]")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"part-of-identity\":100}}]")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"part-of-identity\":\"\"}}]")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");

        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"secure\":\"true\"}}]")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"secure\":100}}]")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"secure\":\"\"}}]")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");

        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"required\":\"true\"}}]")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"required\":100}}]")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"required\":\"\"}}]")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");

        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"display-name\":true}}]")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"display-name\":100}}]")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");

        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"display-order\":true}}]")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"display-order\":10.0}}]")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForShouldUpdate("[{\"key\":{\"display-order\":\"\"}}]")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
    }

    private void assertSCMRevision(SCMRevision scmRevision, String revision, String user, String timestamp, String comment, List<ModifiedFile> modifiedFiles) throws ParseException {
        assertThat(scmRevision.getRevision()).isEqualTo(revision);
        assertThat(scmRevision.getUser()).isEqualTo(user);
        assertThat(scmRevision.getTimestamp()).isEqualTo(new SimpleDateFormat(DATE_FORMAT).parse(timestamp));
        assertThat(scmRevision.getRevisionComment()).isEqualTo(comment);
        assertThat(scmRevision.getData().size()).isEqualTo(2);
        assertThat(scmRevision.getDataFor("dataKeyOne")).isEqualTo("data-value-one");
        assertThat(scmRevision.getDataFor("dataKeyTwo")).isEqualTo("data-value-two");
        assertThat(scmRevision.getModifiedFiles()).isEqualTo(modifiedFiles);
    }

    private void assertSuccessResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getMessages()).isEqualTo(messages);
    }

    private void assertFailureResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getMessages()).isEqualTo(messages);
    }

    private void assertValidationError(ValidationError validationError, String expectedKey, String expectedMessage) {
        assertThat(validationError.getKey()).isEqualTo(expectedKey);
        assertThat(validationError.getMessage()).isEqualTo(expectedMessage);
    }

    private void assertPropertyConfiguration(SCMProperty property, String key, String value, boolean partOfIdentity, boolean required, boolean secure, String displayName, int displayOrder) {
        assertThat(property.getKey()).isEqualTo(key);
        assertThat(property.getValue()).isEqualTo(value);
        assertThat(property.getOption(Property.PART_OF_IDENTITY)).isEqualTo(partOfIdentity);
        assertThat(property.getOption(Property.REQUIRED)).isEqualTo(required);
        assertThat(property.getOption(Property.SECURE)).isEqualTo(secure);
        assertThat(property.getOption(Property.DISPLAY_NAME)).isEqualTo(displayName);
        assertThat(property.getOption(Property.DISPLAY_ORDER)).isEqualTo(displayOrder);
    }

    private String errorMessageForSCMConfiguration(String message) {
        try {
            scmExtensionConverterV2.responseMessageForSCMConfiguration(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForShouldUpdate(String responseBody) {
        try {
            scmExtensionConverterV2.responseMessageForShouldUpdate(responseBody);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMView(String message) {
        try {
            scmExtensionConverterV2.responseMessageForSCMView(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMRevisions(String message) {
        try {
            Map revisionsMap = (Map) new GsonBuilder().create().fromJson(message, Object.class);
            scmExtensionConverterV2.toSCMRevisions(revisionsMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMRevision(String message) {
        try {
            Map revisionMap = (Map) new GsonBuilder().create().fromJson(message, Object.class);
            scmExtensionConverterV2.toSCMRevision(revisionMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForEachRevision(String message) {
        try {
            Map revisionMap = (Map) new GsonBuilder().create().fromJson(message, Object.class);
            scmExtensionConverterV2.getScmRevisionFromMap(revisionMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMData(String message) {
        try {
            Map dataMap = (Map) new GsonBuilder().create().fromJson(message, Object.class);
            scmExtensionConverterV2.toMaterialDataMap(dataMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }
}
