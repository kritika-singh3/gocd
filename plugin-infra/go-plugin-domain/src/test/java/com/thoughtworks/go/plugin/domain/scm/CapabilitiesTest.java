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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class CapabilitiesTest {
    @Test
    void shouldReturnFalseIfEmpty() {
        Capabilities capabilities = new Capabilities();

        assertThat(capabilities.supportsWebhook("a", "e")).isFalse();
    }

    @Test
    void shouldReturnFalseIfTheProviderIsNotSupported() {
        Capabilities capabilities = new Capabilities().setSupportedWebhooks(singletonList(new WebhookSupport().setProvider("Github").setEvents(singletonList("push"))));

        assertThat(capabilities.supportsWebhook("a", "e")).isFalse();
    }

    @Test
    void shouldReturnFalseIfTheEventIsNotSupported() {
        Capabilities capabilities = new Capabilities().setSupportedWebhooks(singletonList(new WebhookSupport().setProvider("Github").setEvents(singletonList("push"))));

        assertThat(capabilities.supportsWebhook("Github", "e")).isFalse();
    }

    @Test
    void shouldReturnTrueIfTheEventIsSupportedByTheProvider() {
        Capabilities capabilities = new Capabilities().setSupportedWebhooks(new ArrayList<>());
        capabilities.getSupportedWebhooks().add(new WebhookSupport().setProvider("Github").setEvents(singletonList("push")));
        capabilities.getSupportedWebhooks().add(new WebhookSupport().setProvider("Gitlab").setEvents(singletonList("pull_request")));

        assertThat(capabilities.supportsWebhook("Github", "push")).isTrue();
    }
}
