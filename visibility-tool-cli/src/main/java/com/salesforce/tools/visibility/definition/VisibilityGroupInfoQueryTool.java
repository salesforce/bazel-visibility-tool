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
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.salesforce.tools.visibility.query.BazelQueryForTargetProtoCommand;
import com.salesforce.tools.visibility.query.BazelRuleWithAttributes;
import com.salesforce.tools.visibility.util.bazel.BazelCommandExecutor;

/**
 * A loader for visibility information from a workspace.
 */
public class VisibilityGroupInfoQueryTool extends BaseVisibilityQueryTool {

    private final SortedMap<String, VisibilityGroup> visibilityGroupsByName;

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
    public VisibilityGroupInfoQueryTool(TargetExpression visibilityPackage, Path workspaceRoot,
            BazelCommandExecutor executor) throws IOException {
        super(visibilityPackage, workspaceRoot, executor);

        var visibilityGroupsQuery = new BazelQueryForTargetProtoCommand(
                workspaceRoot,
                format("kind( 'visibility_group_definition rule', %s )", getVisibilityPackageQueryScope()),
                false,
                List.of("--noproto:rule_inputs_and_outputs", "--noproto:locations", "--noproto:default_values"),
                format("Querying '%s' for visibility groups", getVisibilityPackageQueryScope()));

        Collection<Target> targets = executor.execute(visibilityGroupsQuery, Boolean.FALSE::booleanValue);

        visibilityGroupsByName = new TreeMap<>();
        for (Target target : targets) {
            var rule = BazelRuleWithAttributes.forTarget(target);
            if (visibilityGroupsByName.containsKey(rule.getName())) {
                throw new IllegalStateException(
                        format(
                            "There are duplicate visibility group definitions sharing the same name '%s'. This is not supported. We want the names to be canonical for convenience.%nPlease change or delete one of the following:%n - %s%n - %s%n",
                            rule.getName(),
                            visibilityGroupsByName.get(rule.getName()).getLabel(),
                            rule.getLabel()));
            }

            visibilityGroupsByName.put(
                rule.getName(),
                new VisibilityGroup(
                        rule.getLabel(),
                        rule.getString("package_group"),
                        rule.getStringList("visible_to_groups"),
                        rule.getString("visibility_allow_list")));
        }
    }

    /**
     * {@return the {@link WorkspacePath} of the groups sub-package for persisting member information and
     * <code>package_group</code> definitions of the specified group}
     */
    public WorkspacePath getPackageGroupPackage(VisibilityGroup group) {
        return new WorkspacePath(getVisibilityPackage().relativePath() + "/groups/" + group.getName());
    }

    /**
     * Gets a visibility group with the specified name, performing check whether the group exists.
     *
     * @param name
     *            group name
     * @param messageSupplier
     *            a supplier to call when the group does not exists for populating the exception
     * @return the visibility group with the specified name
     * @throws NullPointerException
     *             when the group does not exists
     */
    public VisibilityGroup getVisibilityGroup(
            String name,
            Supplier<String> messageSupplier) throws NullPointerException {
        return requireNonNull(visibilityGroupsByName.get(name), messageSupplier);
    }

    /**
     * {@return the visibility groups (sorted by name)}
     */
    public Collection<VisibilityGroup> getVisibilityGroups() {
        return visibilityGroupsByName.values();
    }

    /**
     * Indicates if a group with the specified name is defined.
     *
     * @param name
     *            group name
     * @return <code>true</code> if the group is defined, <code>false</code> otherwise
     */
    public boolean hasGroup(String name) {
        return visibilityGroupsByName.containsKey(name);
    }
}
