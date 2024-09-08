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
 * A <code>maven_deps_visibility_info</code> definition.
 */
public class MavenDepsVisibilityInfo {

    private final Label group;
    private final String groupName;
    private final SortedSet<String> includePatterns;
    private final SortedSet<String> excludePatterns;
    private final Label definingTargetLabel;

    /**
     * Creates a new <code>package_visibility_info</code> instance.
     *
     * @param definingTargetLabel
     *            label of the target defining the info (must not be <code>null</code>)
     * @param group
     *            the visibility group (must not be <code>null</code>)
     * @param includePatterns
     *            the list of package inclusion patterns (optional, may be <code>null</code>)
     * @param excludePatterns
     *            the list of package exclusion patterns (optional, may be <code>null</code>)
     */
    public MavenDepsVisibilityInfo(Label definingTargetLabel, Label group, Collection<String> includePatterns,
            Collection<String> excludePatterns) {
        this.definingTargetLabel = requireNonNull(definingTargetLabel);
        this.group = requireNonNull(group);
        this.groupName = group.targetName().toString();
        this.includePatterns = includePatterns != null ? new TreeSet<>(includePatterns) : new TreeSet<>();
        this.excludePatterns = excludePatterns != null ? new TreeSet<>(excludePatterns) : new TreeSet<>();
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
        var other = (MavenDepsVisibilityInfo) obj;
        return Objects.equals(this.definingTargetLabel, other.definingTargetLabel)
                && Objects.equals(this.group, other.group)
                && Objects.equals(this.includePatterns, other.includePatterns)
                && Objects.equals(this.excludePatterns, other.excludePatterns);
    }

    /**
     * @return the definingTargetLabel
     */
    public Label getDefiningTargetLabel() {
        return definingTargetLabel;
    }

    /**
     * {@return the list of package exclusion patterns (never <code>null</code>)}
     */
    public SortedSet<String> getExcludePatterns() {
        return excludePatterns;
    }

    /**
     * @return the visibility group label
     */
    public Label getGroup() {
        return group;
    }

    /**
     * @return the visibility group name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * {@return the list of package inclusion patterns (never <code>null</code>)}
     */
    public SortedSet<String> getIncludePatterns() {
        return includePatterns;
    }

    @Override
    public int hashCode() {
        return Objects.hash(definingTargetLabel, group, includePatterns, excludePatterns);
    }

    @Override
    public String toString() {
        return "VisibilityMavenDepsInfo [" + this.definingTargetLabel + " (" + this.group + ")]";
    }
}
