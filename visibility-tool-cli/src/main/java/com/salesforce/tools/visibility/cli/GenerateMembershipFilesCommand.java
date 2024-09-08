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
package com.salesforce.tools.visibility.cli;

import static com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions.toStarlarkIdentifier;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;
import com.salesforce.tools.visibility.cli.picocli.TargetExpressionConverter;
import com.salesforce.tools.visibility.definition.VisibilityGroup;
import com.salesforce.tools.visibility.definition.VisibilityPackageInfo;
import com.salesforce.tools.visibility.definition.VisibilityPackageInfoQueryTool;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * Generate membership information into variables for consumption by macros.
 */
@Command(
        name = "generate-group-membership-file",
        description = "Generate group membership file based on querying Bazel for visibility_package_info_definition rule information")
public class GenerateMembershipFilesCommand extends BaseAnalyzeVisibilityCommand {

    @Option(
            names = { "--ignore-package" },
            description = "A package to ignore when querying for package visibiliy infos (eg., useful for tests of macros)",
            paramLabel = "PACKAGE",
            converter = TargetExpressionConverter.class,
            required = false,
            scope = ScopeType.INHERIT)
    private final SortedSet<TargetExpression> packagesToIgnore = new TreeSet<>();

    @Option(
            names = { "--group", "-g" },
            description = "The group to analyze",
            paramLabel = "GROUP_NAME",
            required = true,
            arity = "1..*",
            scope = ScopeType.INHERIT)
    private final SortedSet<String> groupsToAnalyze = new TreeSet<>();

    @Option(
            names = { "--file", "-f" },
            description = "Path to a bzl file (either absolute or relative to the workspace root) for writing the membership information",
            paramLabel = "FILE",
            required = true,
            scope = ScopeType.INHERIT)
    private Path membersFile;

    @Option(
            names = { "--preamble", },
            description = "Optional preamble to add to the top of the file (instead of a default generated comment)",
            paramLabel = "TEXT",
            required = false,
            scope = ScopeType.INHERIT)
    private String preamble;

    private VisibilityPackageInfoQueryTool visibilityPackageInfoQueryTool;
    private Map<String, SortedSet<WorkspacePath>> packagesByGroup;

    private Map<String, SortedSet<WorkspacePath>> createPackagesByGroupIndex() {
        Map<String, SortedSet<WorkspacePath>> packagesByGroup = new HashMap<>();
        for (String groupName : visibilityPackageInfoQueryTool.getGroupsNamesWithVisibilityPackageInfos()) {
            if (!packagesByGroup.containsKey(groupName)) {
                packagesByGroup.put(groupName, new TreeSet<>(Comparator.comparing(WorkspacePath::relativePath)));
            }

            var packages = packagesByGroup.get(groupName);
            for (VisibilityPackageInfo info : visibilityPackageInfoQueryTool.getVisibilityPackageInfos(groupName)) {
                packages.add(info.getPackagePath());
            }
        }
        return packagesByGroup;
    }

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws IOException {
        if (verbose) {
            out.notice("Analyzing group(s): " + groupsToAnalyze.stream().collect(joining(", ")));
        }

        visibilityPackageInfoQueryTool =
                new VisibilityPackageInfoQueryTool(visibilityPackage, workspaceRoot, executor, packagesToIgnore);
        packagesByGroup = createPackagesByGroupIndex();

        var output = new StarlarkStringBuilder(4);
        if (preamble == null) {
            output.append("# This file contains membership information for the following groups: ").appendNewline();
            for (String groupName : groupsToAnalyze) {
                output.append("#  - ").append(groupName).appendNewline();
            }
        } else {
            output.append(preamble);
            if (!preamble.endsWith(System.lineSeparator())) {
                output.appendNewline();
            }
        }
        output.append("visibility(\"public\")").appendNewline();
        output.appendNewline();

        // only groups not ignored
        for (String groupName : groupsToAnalyze) {
            var group = getVisbilityGroup(groupName);
            output.append("# list of members of group ").append(group.getName()).appendNewline();
            if (!hasPackages(group)) {
                output.append(toStarlarkIdentifier(group.getName())).append(" = []").appendNewline();
            } else {
                var groupPackages = getPackages(group).stream().map(p -> "//" + p).collect(toCollection(TreeSet::new));
                output.append(toStarlarkIdentifier(group.getName()))
                        .append(" = ")
                        .appendListQuotedMultiLine(groupPackages.stream())
                        .appendNewline();
            }
            output.appendNewline();
        }

        getScmTool().writeFile(
            membersFile.isAbsolute() ? membersFile : workspaceRoot.resolve(membersFile),
            output.toString(),
            UTF_8);

        return 0;
    }

    private SortedSet<WorkspacePath> getPackages(VisibilityGroup group) {
        return requireNonNull(packagesByGroup.get(group.getName()));
    }

    private boolean hasPackages(VisibilityGroup group) {
        var foundPackagesForGroup = packagesByGroup.get(group.getName());
        return (foundPackagesForGroup != null) && !foundPackagesForGroup.isEmpty();
    }
}
