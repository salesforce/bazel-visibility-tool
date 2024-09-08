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

import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.idea.blaze.base.model.primitives.Label;

/**
 * A <code>visibility_group</code> definition.
 */
public class VisibilityGroup {

    private final String name;
    private final Label label;
    private final String packageGroup;
    private final SortedSet<String> visibleToGroups;
    private final String visibilityAllowList;

    /**
     * Creates a new <code>visibility_group</code> instance.
     *
     * @param name
     *            the name of the group (must not be <code>null</code>)
     * @param packageGroup
     *            the package group label representing the visibility group (optional, may be <code>null</code>)
     * @param visibleToGroups
     *            the label list of groups this group shall be visible to (optional, may be <code>null</code>)
     * @param visibilityAllowList
     *            the visibility_allow_list label (optional, may be <code>null</code>)
     */
    public VisibilityGroup(Label label, String packageGroup, Collection<String> visibleToGroups,
            String visibilityAllowList) {
        this.label = requireNonNull(label);
        this.name = label.targetName().toString();
        this.packageGroup = packageGroup;
        this.visibleToGroups = visibleToGroups != null ? new TreeSet<>(visibleToGroups) : new TreeSet<>();
        this.visibilityAllowList = visibilityAllowList;
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
        var other = (VisibilityGroup) obj;
        return Objects.equals(this.label, other.label) && Objects.equals(this.packageGroup, other.packageGroup)
                && Objects.equals(this.visibilityAllowList, other.visibilityAllowList)
                && Objects.equals(this.visibleToGroups, other.visibleToGroups);
    }

    /**
     * @return the label
     */
    public Label getLabel() {
        return label;
    }

    /**
     * @return the name of the group (never <code>null</code>)
     */
    public String getName() {
        return name;
    }

    /**
     * @return the package_group representing the visibility group (can be <code>null</code>)
     */
    public String getPackageGroup() {
        return packageGroup;
    }

    /**
     * @return the visibility_allow_list label (may be <code>null</code>)
     */
    public String getVisibilityAllowList() {
        return visibilityAllowList;
    }

    /**
     * @return the label list of groups this group shall be visible to (never <code>null</code>)
     */
    public SortedSet<String> getVisibleToGroups() {
        return visibleToGroups;
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, packageGroup, visibilityAllowList, visibleToGroups);
    }

    @Override
    public String toString() {
        return "VisibilityGroup [" + this.label + "]";
    }
}
