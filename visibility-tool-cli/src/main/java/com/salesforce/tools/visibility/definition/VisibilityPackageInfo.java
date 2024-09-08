/*-
 * Copyright (c) 2024 Salesforce.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.salesforce.tools.visibility.definition;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;

/**
 * A <code>package_visibility_info</code> definition.
 */
public class VisibilityPackageInfo {

    private final WorkspacePath packagePath;
    private final Label group;
    private final String groupName;

    /**
     * Creates a new <code>package_visibility_info</code> instance.
     *
     * @param packagePath
     *            the package path (must not be <code>null</code>)
     * @param visibilityGroup
     *            the visibility group (must not be <code>null</code>)
     */
    public VisibilityPackageInfo(WorkspacePath packagePath, Label group) {
        this.packagePath = requireNonNull(packagePath);
        this.group = requireNonNull(group);
        this.groupName = group.targetName().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        var other = (VisibilityPackageInfo) obj;
        return Objects.equals(this.packagePath, other.packagePath) && Objects.equals(this.group, other.group);
    }

    /**
     * @return the group
     */
    public Label getGroup() {
        return group;
    }

    /**
     * @return the groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @return the packagePath
     */
    public WorkspacePath getPackagePath() {
        return packagePath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(packagePath, group);
    }

    @Override
    public String toString() {
        return "VisibilityPackageInfo [" + this.packagePath + "]";
    }
}
