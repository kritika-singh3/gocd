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
import com.thoughtworks.go.plugin.domain.scm.WebhookSupport;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class CapabilitiesConverterV2 {
    public Capabilities fromDTO(CapabilitiesDTO capabilitiesDTO) {
        List<WebhookSupport> webhookSupports = capabilitiesDTO.getSupportedWebhooks()
                .stream()
                .map((webhookSupportDTO) -> new WebhookSupport().setProvider(webhookSupportDTO.getProvider()).setEvents(webhookSupportDTO.getEvents()))
                .collect(toList());
        return new Capabilities().setSupportedWebhooks(webhookSupports);
    }
}
