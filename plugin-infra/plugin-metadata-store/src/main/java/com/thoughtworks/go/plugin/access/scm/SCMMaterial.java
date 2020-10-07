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

import java.util.Objects;

public class SCMMaterial {
    private String id;
    private SCMPropertyConfiguration propertyConfiguration;

    public SCMMaterial(String id, SCMPropertyConfiguration propertyConfiguration) {
        this.id = id;
        this.propertyConfiguration = propertyConfiguration;
    }

    public String getId() {
        return id;
    }

    public SCMPropertyConfiguration getPropertyConfiguration() {
        return propertyConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SCMMaterial that = (SCMMaterial) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(propertyConfiguration, that.propertyConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, propertyConfiguration);
    }
}
