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

import com.thoughtworks.go.plugin.domain.scm.Capabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapabilitiesConverterV2Test {
    private CapabilitiesConverterV2 capabilitiesConverterV2;

    @BeforeEach
    void setUp() {
        capabilitiesConverterV2 = new CapabilitiesConverterV2();
    }

    @Test
    void shouldConvertToCapabilitiesFromDTO() {
        WebhookSupportDTO webhookSupportDTO = mock(WebhookSupportDTO.class);
        CapabilitiesDTO capabilitiesDTO = mock(CapabilitiesDTO.class);

        when(webhookSupportDTO.getProvider()).thenReturn("Github");
        when(webhookSupportDTO.getEvents()).thenReturn(singletonList("pull"));
        when(capabilitiesDTO.getSupportedWebhooks()).thenReturn(singletonList(webhookSupportDTO));

        Capabilities capabilities = capabilitiesConverterV2.fromDTO(capabilitiesDTO);
        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedWebhooks()).hasSize(1);
        assertThat(capabilities.getSupportedWebhooks().get(0).getProvider()).isEqualTo("Github");
        assertThat(capabilities.getSupportedWebhooks().get(0).getEvents()).containsExactly("pull");
    }
}
