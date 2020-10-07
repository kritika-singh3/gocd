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
package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.MetadataWithPartOfIdentity;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginView;
import com.thoughtworks.go.plugin.domain.scm.Capabilities;
import com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo;
import com.thoughtworks.go.plugin.domain.scm.WebhookSupport;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecretParamResolver;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
public class PluggableScmServiceTest {
    private static final String pluginId = "abc.def";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private SCMExtension scmExtension;
    @Mock
    private SCMPreference preference;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private EntityHashingService entityHashingService;
    @Mock
    private SecretParamResolver secretParamResolver;

    private PluggableScmService pluggableScmService;
    private SCMConfigurations scmConfigurations;
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @BeforeEach
    void setUp() throws Exception {
        initMocks(this);

        pluggableScmService = new PluggableScmService(scmExtension, goConfigService, entityHashingService, secretParamResolver);

        SCMPropertyConfiguration scmConfig = new SCMPropertyConfiguration();
        scmConfig.add(new SCMProperty("KEY1").with(Property.REQUIRED, true));
        scmConfigurations = new SCMConfigurations(scmConfig);

        when(preference.getScmConfigurations()).thenReturn(scmConfigurations);
        SCMMetadataStore.getInstance().setPreferenceFor(pluginId, preference);
    }

    @Test
    void shouldValidateSCM() {
        SCMConfiguration scmConfig = new SCMConfiguration(new SCMProperty("KEY2").with(Property.REQUIRED, false));
        scmConfigurations.add(scmConfig);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("KEY1", "error message"));
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        assertThat(modifiedSCM.getConfiguration().getProperty("KEY1").errors().isEmpty()).isFalse();
        assertThat(modifiedSCM.getConfiguration().getProperty("KEY1").errors().firstError()).isEqualTo("error message");
        verify(scmExtension).isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class));
    }

    @Test
    void shouldHandleIncorrectKeyForValidateSCM() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        configuration.getProperty("KEY1").setConfigurationValue(new ConfigurationValue("junk"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("NON-EXISTENT-KEY", "error message"));
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        assertThat(modifiedSCM.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldValidateMandatoryFieldsForSCM() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        final List<ValidationError> validationErrors = validationResult.getErrors();
        assertThat(validationErrors.isEmpty()).isFalse();
        final ValidationError validationError = getValidationErrorFor(validationErrors, "KEY1");
        assertThat(validationError).isNotNull();
        assertThat(validationError.getMessage()).isEqualTo("This field is required");
    }

    @Test
    void shouldValidateMandatoryAndSecureFieldsForSCM() {
        SCMConfiguration scmConfig = new SCMConfiguration(new SCMProperty("KEY2").with(Property.REQUIRED, true).with(Property.SECURE, true));
        scmConfigurations.add(scmConfig);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("KEY2", true, ""));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        final List<ValidationError> validationErrors = validationResult.getErrors();
        assertThat(validationErrors.isEmpty()).isFalse();
        final ValidationError validationErrorForKey1 = getValidationErrorFor(validationErrors, "KEY1");
        assertThat(validationErrorForKey1).isNotNull();
        assertThat(validationErrorForKey1.getMessage()).isEqualTo("This field is required");
        final ValidationError validationErrorForKey2 = getValidationErrorFor(validationErrors, "KEY2");
        assertThat(validationErrorForKey2).isNotNull();
        assertThat(validationErrorForKey2.getMessage()).isEqualTo("This field is required");
    }

    @Test
    void shouldPassValidationIfAllRequiredFieldsHaveValuesForSCM() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        configuration.getProperty("KEY1").setConfigurationValue(new ConfigurationValue("junk"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        assertThat(validationResult.getErrors().isEmpty()).isTrue();
    }

    @Test
    void shouldCallPluginToCheckConnectionForTheGivenSCMConfiguration() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        Result resultFromPlugin = new Result();
        resultFromPlugin.withSuccessMessages(singletonList("message"));

        when(scmExtension.checkConnectionToSCM(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(resultFromPlugin);

        HttpLocalizedOperationResult result = pluggableScmService.checkConnection(modifiedSCM);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Connection OK. message");
    }

    @Test
    void checkConnectionResultShouldFailForFailureResponseFromPlugin() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        Result resultFromPlugin = new Result();
        resultFromPlugin.withErrorMessages("connection failed");

        when(scmExtension.checkConnectionToSCM(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(resultFromPlugin);

        HttpLocalizedOperationResult result = pluggableScmService.checkConnection(modifiedSCM);

        assertThat(result.httpCode()).isEqualTo(422);
        assertThat(result.message()).isEqualTo("Check connection failed. Reason(s): connection failed");
    }

    @Test
    void shouldReturnAListOfAllScmsInTheConfig() {
        SCMs list = new SCMs();
        list.add(new SCM());
        when(goConfigService.getSCMs()).thenReturn(list);

        SCMs scms = pluggableScmService.listAllScms();

        assertThat(scms).isEqualTo(list);
    }

    @Test
    void shouldReturnAPluggableScmMaterialIfItExists() {
        SCM scm = new SCM("1", null, null);
        scm.setName("foo");

        SCMs list = new SCMs();
        list.add(scm);
        when(goConfigService.getSCMs()).thenReturn(list);

        assertThat(pluggableScmService.findPluggableScmMaterial("foo")).isEqualTo(scm);
    }

    @Test
    void shouldReturnNullIfPluggableScmMaterialDoesNotExist() {
        SCMs scms = new SCMs();
        when(goConfigService.getSCMs()).thenReturn(scms);

        assertThat(pluggableScmService.findPluggableScmMaterial("bar")).isNull();
    }

    @Test
    void shouldDeleteSCMConfigIfValid() {
        doNothing().when(goConfigService).updateConfig(any(), any());
        SCM scm = new SCM("id", "name");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pluggableScmService.deletePluggableSCM(new Username("admin"), scm, result);

        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void isValidShouldSkipValidationAgainstPluginIfPluginIsNonExistent() {
        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(false);

        thrown.expect(RuntimeException.class);
        pluggableScmService.isValid(scmConfig);

        verifyZeroInteractions(scmExtension);
    }

    @Test
    void isValidShouldMapPluginValidationErrorsToPluggableSCMConfigurations() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("url", false, "url"));
        configuration.add(ConfigurationPropertyMother.create("username", false, "admin"));

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("url", "invalid"));
        validationResult.addError(new ValidationError("username", "invalid"));

        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(true);
        when(scmConfig.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(scmConfig.getConfiguration()).thenReturn(configuration);
        when(scmExtension.isSCMConfigurationValid(any(String.class), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        assertThat(pluggableScmService.isValid(scmConfig)).isFalse();
        assertThat(configuration.getProperty("url").errors().get("url").get(0)).isEqualTo("invalid");
        assertThat(configuration.getProperty("username").errors().get("username").get(0)).isEqualTo("invalid");
    }

    @Test
    void isValidShouldMapPluginValidationErrorsToPluggableSCMForMissingConfigurations() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("url", "URL is a required field"));

        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(true);
        when(scmConfig.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(scmConfig.getConfiguration()).thenReturn(new Configuration());
        when(scmExtension.isSCMConfigurationValid(any(String.class), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        assertThat(pluggableScmService.isValid(scmConfig)).isFalse();
        verify(scmConfig).addError("url", "URL is a required field");
    }

    @Test
    void shouldSendResolvedValueToPluginDuringValidateSCM() {
        SCMConfiguration scmConfig = new SCMConfiguration(new SCMProperty("KEY2").with(Property.REQUIRED, false));
        scmConfigurations.add(scmConfig);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1", "{{SECRET:[secret_config_id][lookup_username]}}"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("KEY1", "error message"));
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);
        doAnswer(invocation -> {
            SCM config = invocation.getArgument(0);
            config.getSecretParams().get(0).setValue("resolved-value");
            return config;
        }).when(secretParamResolver).resolve(modifiedSCM);

        pluggableScmService.validate(modifiedSCM);

        verify(secretParamResolver).resolve(modifiedSCM);
        assertThat(modifiedSCM.getConfiguration().getProperty("KEY1").errors().isEmpty()).isFalse();
        assertThat(modifiedSCM.getConfiguration().getProperty("KEY1").errors().firstError()).isEqualTo("error message");
        ArgumentCaptor<SCMPropertyConfiguration> captor = ArgumentCaptor.forClass(SCMPropertyConfiguration.class);
        verify(scmExtension).isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), captor.capture());
        assertThat(captor.getValue().list().get(0).getValue()).isEqualTo("resolved-value");
    }

    @Test
    void shouldSendResolvedValueToPluginDuringIsValidCall() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("url", false, "url"));
        configuration.add(ConfigurationPropertyMother.create("username", false, "{{SECRET:[secret_config_id][username]}}"));

        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(true);
        when(scmConfig.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(scmConfig.getConfiguration()).thenReturn(configuration);
        when(scmExtension.isSCMConfigurationValid(any(String.class), any(SCMPropertyConfiguration.class))).thenReturn(new ValidationResult());
        doAnswer(invocation -> {
            configuration.get(1).getSecretParams().get(0).setValue("resolved-value");
            return scmConfig;
        }).when(secretParamResolver).resolve(any(SCM.class));

        assertThat(pluggableScmService.isValid(scmConfig)).isTrue();

        ArgumentCaptor<SCMPropertyConfiguration> captor = ArgumentCaptor.forClass(SCMPropertyConfiguration.class);
        verify(scmExtension).isSCMConfigurationValid(anyString(), captor.capture());
        assertThat(captor.getValue().list().get(1).getValue()).isEqualTo("resolved-value");
    }

    @Test
    void shouldCallPluginAndSendResolvedValuesToCheckConnectionForTheGivenSCMConfiguration() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1", "{{SECRET:[secret_config_id][value]}}"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        Result resultFromPlugin = new Result();
        resultFromPlugin.withSuccessMessages(singletonList("message"));

        ArgumentCaptor<SCMPropertyConfiguration> captor = ArgumentCaptor.forClass(SCMPropertyConfiguration.class);
        when(scmExtension.checkConnectionToSCM(eq(modifiedSCM.getPluginConfiguration().getId()), captor.capture())).thenReturn(resultFromPlugin);
        doAnswer(invocation -> {
            configuration.get(0).getSecretParams().get(0).setValue("resolved-value");
            return modifiedSCM;
        }).when(secretParamResolver).resolve(any(SCM.class));

        HttpLocalizedOperationResult result = pluggableScmService.checkConnection(modifiedSCM);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Connection OK. message");
        assertThat(captor.getValue().list().get(0).getValue()).isEqualTo("resolved-value");
    }

    @Nested
    class GetMaterialsToUpdate {
        @Test
        void shouldReturnEmptySetIfPluginIsNotFound() {
            PluggableSCMMaterialConfig scmMaterialConfig = MaterialConfigsMother.pluggableSCMMaterialConfig();
            Set<MaterialConfig> materialsToUpdate = pluggableScmService.getMaterialsToUpdate("some-plugin-id", "Github", "pull", "some-payload", singleton(scmMaterialConfig));

            assertThat(materialsToUpdate).isEmpty();
            verifyNoInteractions(scmExtension);
        }

        @Test
        void shouldReturnEmptySetIfPluginDoesNotSupportTheProvider() {
            NewSCMMetadataStore.instance().setPluginInfo(createSCMPluginInfo());
            PluggableSCMMaterialConfig scmMaterialConfig = MaterialConfigsMother.pluggableSCMMaterialConfig();
            Set<MaterialConfig> materialsToUpdate = pluggableScmService.getMaterialsToUpdate(pluginId, "Github", "pull", "some-payload", singleton(scmMaterialConfig));

            assertThat(materialsToUpdate).isEmpty();
            verifyNoInteractions(scmExtension);
        }

        @Test
        void shouldReturnEmptySetIfPluginDoesNotSupportTheEvent() {
            SCMPluginInfo scmPluginInfo = createSCMPluginInfo();
            scmPluginInfo.getCapabilities().setSupportedWebhooks(singletonList(new WebhookSupport().setProvider("Github").setEvents(singletonList("push"))));
            NewSCMMetadataStore.instance().setPluginInfo(scmPluginInfo);
            PluggableSCMMaterialConfig scmMaterialConfig = MaterialConfigsMother.pluggableSCMMaterialConfig();
            Set<MaterialConfig> materialsToUpdate = pluggableScmService.getMaterialsToUpdate(pluginId, "Github", "pull", "some-payload", singleton(scmMaterialConfig));

            assertThat(materialsToUpdate).isEmpty();
            verifyNoInteractions(scmExtension);
        }

        @Test
        void shouldReturnTheMaterialsToUpdate() throws CryptoException {
            SCMPluginInfo scmPluginInfo = createSCMPluginInfo();
            scmPluginInfo.getCapabilities().setSupportedWebhooks(singletonList(new WebhookSupport().setProvider("Github").setEvents(singletonList("pull"))));
            NewSCMMetadataStore.instance().setPluginInfo(scmPluginInfo);
            PluggableSCMMaterialConfig materialConfig1 = MaterialConfigsMother.pluggableSCMMaterialConfig();
            materialConfig1.getSCMConfig().getConfiguration().addNewConfigurationWithValue("key1", new GoCipher().encrypt("value1"), true);
            PluggableSCMMaterialConfig materialConfig2 = MaterialConfigsMother.pluggableSCMMaterialConfig();
            materialConfig2.getSCMConfig().getConfiguration().addNewConfigurationWithValue("key2", new GoCipher().encrypt("value2"), true);

            SCMPropertyConfiguration configuration = new SCMPropertyConfiguration();
            configuration.add(new SCMProperty("key1", "value1"));
            ArgumentCaptor<List<SCMPropertyConfiguration>> captor = ArgumentCaptor.forClass(List.class);

            when(scmExtension.shouldUpdate(anyString(), anyString(), anyString(), anyString(), anyList())).thenReturn(singletonList(configuration));

            Set<MaterialConfig> materialsToUpdate = pluggableScmService.getMaterialsToUpdate(pluginId, "Github", "pull", "some-payload", new HashSet<>(asList(materialConfig1, materialConfig2)));

            assertThat(materialsToUpdate).hasSize(1);
            assertThat(materialsToUpdate).containsExactly(materialConfig1);
            verify(scmExtension).shouldUpdate(eq(pluginId), eq("Github"), eq("pull"), eq("some-payload"), captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        private SCMPluginInfo createSCMPluginInfo() {
            ArrayList<com.thoughtworks.go.plugin.domain.common.PluginConfiguration> configurations = new ArrayList<>();
            com.thoughtworks.go.plugin.domain.common.PluginConfiguration key1 = new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("key1", new MetadataWithPartOfIdentity(true, false, true));
            configurations.add(key1);

            GoPluginDescriptor.About about = GoPluginDescriptor.About.builder()
                    .name("some-name")
                    .version("v1")
                    .targetGoVersion("goVersion1")
                    .description("go plugin")
                    .vendor(new GoPluginDescriptor.Vendor("go", "goUrl"))
                    .targetOperatingSystems(emptyList())
                    .build();

            GoPluginDescriptor descriptor = GoPluginDescriptor.builder()
                    .id(pluginId)
                    .version("1")
                    .about(about)
                    .pluginJarFileLocation("file")
                    .isBundledPlugin(false)
                    .build();

            descriptor.setBundleDescriptor(new GoPluginBundleDescriptor(descriptor));

            return new SCMPluginInfo(descriptor, "SCM", new PluggableInstanceSettings(configurations, new PluginView("Template")), null, new Capabilities().setSupportedWebhooks(emptyList()));
        }
    }

    private ValidationError getValidationErrorFor(List<ValidationError> validationErrors, final String key) {
        return validationErrors.stream().filter(new Predicate<ValidationError>() {
            @Override
            public boolean test(ValidationError item) {
                return item.getKey().equals(key);
            }
        }).findFirst().orElse(null);
    }
}
