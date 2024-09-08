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
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target;
import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.salesforce.tools.visibility.query.BazelQueryForTargetProtoCommand;
import com.salesforce.tools.visibility.query.BazelRuleWithAttributes;
import com.salesforce.tools.visibility.util.bazel.BazelCommandExecutor;

/**
 * A loader for visibility package information from a workspace.
 */
public class MavenDepsVisibilityInfoQueryTool extends BaseVisibilityQueryTool {

    private final SortedMap<String, List<MavenDepsVisibilityInfo>> mavenDepsVisibilityInfosByGroup;

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
    public MavenDepsVisibilityInfoQueryTool(TargetExpression visibilityPackage, Path workspaceRoot,
            BazelCommandExecutor executor) throws IOException {
        super(visibilityPackage, workspaceRoot, executor);

        var mavenDepsInfoQuery = new BazelQueryForTargetProtoCommand(
                workspaceRoot,
                format(
                    "kind( 'visibility_maven_deps_definition rule', %s + //third_party/dependencies/...:all )",
                    getVisibilityPackageQueryScope()),
                false,
                List.of("--noproto:rule_inputs_and_outputs", "--noproto:locations", "--noproto:default_values"),
                "Querying for bazel_maven_deps visibility information");

        mavenDepsVisibilityInfosByGroup = new TreeMap<>();

        Collection<Target> targets = executor.execute(mavenDepsInfoQuery, Boolean.FALSE::booleanValue);
        for (Target target : targets) {
            var rule = BazelRuleWithAttributes.forTarget(target);
            var info = new MavenDepsVisibilityInfo(
                    rule.getLabel(),
                    Label.create(
                        requireNonNull(
                            rule.getString("group"),
                            () -> "missing attribute 'group' for " + rule.getLabel())),
                    rule.getStringList("include_patterns"),
                    rule.getStringList("exclude_patterns"));

            if (!mavenDepsVisibilityInfosByGroup.containsKey(info.getGroupName())) {
                mavenDepsVisibilityInfosByGroup.put(info.getGroupName(), new ArrayList<>());
            }

            mavenDepsVisibilityInfosByGroup.get(info.getGroupName()).add(info);
        }
    }

    /**
     * @return a collection of group names (sorted) for which visibility information is available
     */
    public Collection<String> getGroupsNamesWithMavenDepsVisibilityInfos() {
        return mavenDepsVisibilityInfosByGroup.keySet();
    }

    /**
     * @return stream of all MavenDepsVisibilityInfo
     */
    public Stream<MavenDepsVisibilityInfo> getMavenDepsVisibilityInfos() {
        return mavenDepsVisibilityInfosByGroup.values().stream().flatMap(Collection::stream);
    }

    /**
     * Returns the info for a given group
     *
     * @param groupName
     *            group name
     * @return the collection of infos (as returned by bazel query, maybe <code>null</code>)
     */
    public Collection<MavenDepsVisibilityInfo> getMavenDepsVisibilityInfos(String groupName) {
        return mavenDepsVisibilityInfosByGroup.get(groupName);
    }
}
