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

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target;
import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.salesforce.tools.visibility.query.BazelQueryForTargetProtoCommand;
import com.salesforce.tools.visibility.query.BazelRuleWithAttributes;
import com.salesforce.tools.visibility.util.bazel.BazelCommandExecutor;

/**
 * A loader for visibility package information (<code>visibility_package_info_definition</code>) from a workspace.
 */
public class VisibilityPackageInfoQueryTool extends BaseVisibilityQueryTool {

    private static final Comparator<VisibilityPackageInfo> VISIBILITY_INFO_BY_PACKAGE_RELATIVE_PATH_COMPARATOR =
            comparing(VisibilityPackageInfo::getPackagePath, comparing(WorkspacePath::relativePath));

    private final SortedMap<String, SortedSet<VisibilityPackageInfo>> visibilityPackageInfosByGroupName;
    private final Supplier<Map<WorkspacePath, String>> groupNameByPackageIndex;

    /**
     * Convenience constructor to read visibility information for a workspace.
     *
     * @param visibilityPackage
     *            value of the <code>--visibility-package</code> option (maybe <code>null</code> to use default)
     * @param workspaceRoot
     *            the workspace root
     * @param executor
     *            for executing queries
     */
    public VisibilityPackageInfoQueryTool(TargetExpression visibilityPackage, Path workspaceRoot,
            BazelCommandExecutor executor, Collection<TargetExpression> packagesToIgnore) throws IOException {
        super(visibilityPackage, workspaceRoot, executor);

        var visibilityPackageInfoQuery = new BazelQueryForTargetProtoCommand(
                workspaceRoot,
                format("kind( 'visibility_package_info_definition rule', %s )", createQueryScope(packagesToIgnore)),
                false,
                List.of("--noproto:rule_inputs_and_outputs", "--noproto:locations", "--noproto:default_values"),
                "Querying for visibility package information");

        Collection<Target> targets = executor.execute(visibilityPackageInfoQuery, Boolean.FALSE::booleanValue);

        visibilityPackageInfosByGroupName = new TreeMap<>();
        for (Target target : targets) {
            var rule = BazelRuleWithAttributes.forTarget(target);
            var packageInfo = new VisibilityPackageInfo(
                    new WorkspacePath(
                            requireNonNull(
                                rule.getString("package_name"),
                                () -> "missing attribute 'package_name' for " + rule.getLabel())),
                    Label.create(
                        requireNonNull(
                            rule.getString("group"),
                            () -> "missing attribute 'package_name' for " + rule.getLabel())));
            if (!visibilityPackageInfosByGroupName.containsKey(packageInfo.getGroupName())) {
                visibilityPackageInfosByGroupName.put(
                    packageInfo.getGroupName(),
                    new TreeSet<>(VISIBILITY_INFO_BY_PACKAGE_RELATIVE_PATH_COMPARATOR));
            }
            visibilityPackageInfosByGroupName.get(packageInfo.getGroupName()).add(packageInfo);
        }

        groupNameByPackageIndex = Suppliers.memoize(this::createGroupNameByPackageIndex);
    }

    private Map<WorkspacePath, String> createGroupNameByPackageIndex() {
        Map<WorkspacePath, String> groupByPackage = new HashMap<>();
        getVisibilityPackageInfos().forEach(info -> {
            var groupName = info.getGroupName();
            groupByPackage.put(info.getPackagePath(), groupName);
        });
        return groupByPackage;
    }

    private String createQueryScope(Collection<TargetExpression> packagesToIgnore) {
        var query = new StringBuilder();

        query.append("//...").append(" - ").append(getVisibilityPackageQueryScope().toString());

        for (TargetExpression packageToIgnore : packagesToIgnore) {
            query.append(" - ").append(packageToIgnore.toString());
        }

        return query.toString();
    }

    /**
     * Performs a reverse lookup of the group name a given package belongs to.
     *
     * @param packagePath
     *            the package path (must not be <code>null</code>)
     * @return the group name (maybe <code>null</code>)
     */
    public String getGroupName(WorkspacePath packagePath) {
        return groupNameByPackageIndex.get().get(requireNonNull(packagePath));
    }

    /**
     * @return a collection of group names (sorted) for which visibility package information is available
     */
    public Collection<String> getGroupsNamesWithVisibilityPackageInfos() {
        return visibilityPackageInfosByGroupName.keySet();
    }

    /**
     * @return a stream of all infos
     */
    public Stream<VisibilityPackageInfo> getVisibilityPackageInfos() {
        return visibilityPackageInfosByGroupName.values().stream().flatMap(Collection::stream);
    }

    /**
     * Returns the package info for a given group
     *
     * @param groupName
     *            group name
     * @return the collection of package infos (sorted by package path, maybe <code>null</code>)
     */
    public SortedSet<VisibilityPackageInfo> getVisibilityPackageInfos(String groupName) {
        return visibilityPackageInfosByGroupName.get(groupName);
    }
}
