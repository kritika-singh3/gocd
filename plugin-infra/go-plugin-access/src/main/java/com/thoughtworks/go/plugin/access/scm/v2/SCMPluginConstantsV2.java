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

public interface SCMPluginConstantsV2 {
    String REQUEST_SCM_CONFIGURATION = "scm-configuration";
    String REQUEST_SCM_VIEW = "scm-view";
    String REQUEST_VALIDATE_SCM_CONFIGURATION = "validate-scm-configuration";
    String REQUEST_CHECK_SCM_CONNECTION = "check-scm-connection";
    String REQUEST_LATEST_REVISION = "latest-revision";
    String REQUEST_LATEST_REVISIONS_SINCE = "latest-revisions-since";
    String REQUEST_CHECKOUT = "checkout";
    String GET_CAPABILITIES = "get-capabilities";
    String SHOULD_UPDATE = "should-update";
}
