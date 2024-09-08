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
package com.salesforce.tools.visibility.deps_tool_ext;

import static com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions.simplifyLabel;
import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.visibility.ReverseDependenciesProvider;
import com.salesforce.tools.bazel.mavendependencies.visibility.VisibilityProvider;
import com.salesforce.tools.visibility.definition.MavenDepsVisibilityInfoQueryTool;
import com.salesforce.tools.visibility.definition.VisibilityGroupInfoQueryTool;
import com.salesforce.tools.visibility.util.bazel.BazelCommandExecutor;

/**
 * A visibility provider using the visibility information from the tool
 */
public class VisibilityToolProvider implements VisibilityProvider {

    private final MavenDepsVisibilityAnalyzer analyzer;
    private VisibilityGroupInfoQueryTool groupsQueryTool;

    public VisibilityToolProvider(MessagePrinter out, boolean verbose, Path workspaceRoot,
            TargetExpression visibilityPackage, BazelCommandExecutor executor) {
        try {
            var queryTool = new MavenDepsVisibilityInfoQueryTool(visibilityPackage, workspaceRoot, executor);
            analyzer = new MavenDepsVisibilityAnalyzer(queryTool.getMavenDepsVisibilityInfos());

            groupsQueryTool = new VisibilityGroupInfoQueryTool(visibilityPackage, workspaceRoot, executor);
        } catch (IOException e) {
            throw new IllegalStateException(
                    format(
                        "Unable to load visibility information for workspace '%s': %s",
                        workspaceRoot,
                        e.getMessage()),
                    e);
        }
    }

    protected MavenDepsVisibilityAnalyzer getAnalyzer() {
        return analyzer;
    }

    @Override
    public Collection<String> getVisibility(
            String name,
            MavenArtifact mavenArtifact,
            SortedSet<String> tags,
            ReverseDependenciesProvider rdepsProvider) {
        var info = getAnalyzer().findGroupForExternalRepository(name);
        if (info == null) {
            return Collections.emptySet();
        }

        var group = groupsQueryTool.getVisibilityGroup(
            info.getGroupName(),
            () -> format(
                "Invalid reference in '%s': group '%s' is not defined!",
                info.getDefiningTargetLabel(),
                info.getGroupName()));

        SortedSet<String> visibilityLabels = new TreeSet<>();

        var visibleToGroups = group.getVisibleToGroups();
        for (String visibleToGroup : visibleToGroups) {
            var target = groupsQueryTool.getVisibilityGroup(
                info.getGroupName(),
                () -> format(
                    "Invalid reference in visible_to_groups attribute in group '%s': group '%s' is not defined!",
                    group.getName(),
                    visibleToGroup));

            // only add if the group has a package_group (skip non-existing or empty groups)
            if (target.getPackageGroup() != null) {
                // we blindly assume the label is in the main repo, hence we add '@' prefix
                visibilityLabels.add("@" + simplifyLabel(target.getPackageGroup()));
            }
        }

        if (group.getVisibilityAllowList() != null) {
            // add allow list
            visibilityLabels.add("@" + simplifyLabel(group.getVisibilityAllowList()));

            // also allow rdeps within the catalog
            var directReverseDependencies = rdepsProvider.getDirectReverseDependencies(name);
            if (!directReverseDependencies.isEmpty()) {
                for (String rdep : directReverseDependencies) {
                    visibilityLabels.add("@" + rdep + "//:__subpackages__"); // visible to all sub packages
                }
            }
        } else if(group.getVisibilityAllowList() == null || visibilityLabels.isEmpty())  {
            // no allow list means must not use --> make the library private
            // also also make it private when it's empty, i.e. not groups
            return Set.of("//visibility:private");
        }

        return visibilityLabels;
    }

}
