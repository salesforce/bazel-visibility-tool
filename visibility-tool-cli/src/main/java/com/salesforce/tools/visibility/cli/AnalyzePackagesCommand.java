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

import static java.lang.String.format;
import static java.nio.file.Files.write;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.GsonBuilder;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WildcardTargetPattern;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;
import com.salesforce.tools.visibility.cli.picocli.TargetExpressionConverter;
import com.salesforce.tools.visibility.definition.VisibilityPackageInfo;
import com.salesforce.tools.visibility.definition.VisibilityPackageInfoQueryTool;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Analyzes the current workspace for visibility violations of packages.
 */
@Command(
        name = "analyze-packages",
        description = "Analyzes for package visibility violations")
public class AnalyzePackagesCommand extends BaseAnalyzeVisibilityCommand {

    public enum Output {
        formatted, json, buildozer
    }

    @Parameters(
            arity = "1",
            description = "The package to analyze",
            paramLabel = "PACKAGE",
            defaultValue = "//...",
            converter = TargetExpressionConverter.class)
    private TargetExpression packageToAnalyzeExpression;

    @Option(
            names = { "--ignore-package" },
            description = "A package to ignore when querying for package visibiliy infos (eg., useful for tests of macros)",
            paramLabel = "PACKAGE",
            converter = TargetExpressionConverter.class,
            required = false)
    private final SortedSet<TargetExpression> packagesToIgnore = new TreeSet<>();

    @Option(
            names = { "--only-group" },
            description = "Limit analysis to packages of the specified group (eg., useful for focusing on groups with limited visibility)",
            paramLabel = "GROUP_NAME",
            required = false)
    private final SortedSet<String> onlyGroups = new TreeSet<>();

    @Option(
            names = { "--output" },
            description = "The output of violations (default is ${DEFAULT-VALUE}, possible values: ${COMPLETION-CANDIDATES})",
            defaultValue = "formatted",
            required = false)
    private Output output;

    @Option(
            names = { "--recommend-group-threshold" },
            description = "The threshold when to start recommending an entire group instead of packages only for additional visibility (default is 3)",
            defaultValue = "3",
            required = false)
    private int recommendPackageGroupThreshold;

    @Option(
            names = { "--buildozer-commands-file" },
            description = "Path to file to write the list of buildozer commands to (only with '--output=buildozer', defaults to buildozer_commands.txt)",
            defaultValue = "buildozer_commands.txt",
            required = false)
    private Path buildozerCommandsFile;

    @Option(
            names = { "--buildozer-target-name" },
            description = "The target name selection to add to the package path label for buildozer (see https://github.com/bazelbuild/buildtools/blob/master/buildozer/README.md#targets) (only with '--output=buildozer', defaults to no target suffix)",
            required = false)
    private String buildozerOverrideTargetName;

    private VisibilityPackageInfoQueryTool visibilityPackageInfoQueryTool;
    private WildcardTargetPattern packageFilter;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws IOException {
        visibilityPackageInfoQueryTool =
                new VisibilityPackageInfoQueryTool(visibilityPackage, workspaceRoot, executor, packagesToIgnore);
        packageFilter = WildcardTargetPattern.fromExpression(packageToAnalyzeExpression);

        // collect the list of packages to analyze:

        SortedSet<WorkspacePath> packagesToAnalyze = visibilityPackageInfoQueryTool.getVisibilityPackageInfos()
                .map(VisibilityPackageInfo::getPackagePath)
                .filter(
                    p -> shouldAnalyze(p)// ignore packages which are outside of packageToAnalyzeExpression
                            && (onlyGroups.isEmpty()
                                    || !shouldIgnoreGroup(visibilityPackageInfoQueryTool.getGroupName(p)))) // ignore packages which group is not of interest (onlyGroups not empty)
                .collect(toCollection(() -> new TreeSet<>(Comparator.comparing(WorkspacePath::relativePath))));

        if (packagesToAnalyze.isEmpty()) {
            out.error("No packages to analyze!");
            return 1;
        }
        if (packagesToAnalyze.size() == 1) {
            out.notice("Analyzing 1 package...");
        } else {
            out.notice(format("Analyzing %d packages...", packagesToAnalyze.size()));
        }

        // violations are recorded by: package -> group of violating rdep -> violating rdeps
        Map<WorkspacePath, SortedMap<String, SortedSet<String>>> violationsByPackage =
                new TreeMap<>(Comparator.comparing(WorkspacePath::relativePath));

        for (WorkspacePath packagePath : packagesToAnalyze) {
            var groupName = visibilityPackageInfoQueryTool.getGroupName(packagePath);
            if ((groupName == null) || shouldIgnoreGroup(groupName)) {
                if (verbose) {
                    out.notice(
                        format(
                            "Ignoring package '//%s' (%s)",
                            packagePath,
                            groupName != null ? groupName : "no group"));
                }
                continue;
            }

            var group = requireNonNull(
                getVisbilityGroup(groupName),
                () -> format("Invalid group '%s'. No group information available in workspace.", groupName));

            var rdpes = queryForRDepsOfPackage(packagePath);
            for (String directReverseDependency : rdpes) {
                var rdepsPackagePath = WorkspacePath.createIfValid(directReverseDependency);
                if ((rdepsPackagePath == null) || shouldIgnoreRdep(rdepsPackagePath)
                        || rdepsPackagePath.asPath().startsWith(packagePath.asPath()) // ignore rdeps in subpackages
                        || packagePath.asPath().startsWith(rdepsPackagePath.asPath()) // also ignore rdeps being parents
                ) {
                    if (verbose) {
                        out.notice(format("Ignoring rdep '%s'", directReverseDependency));
                    }
                    continue;
                }

                var groupNameOfRdep = visibilityPackageInfoQueryTool.getGroupName(rdepsPackagePath);
                if ((groupNameOfRdep == null) || !group.getVisibleToGroups().contains(groupNameOfRdep)) {
                    // record violation
                    // the group is either not allowed or the package is outside a group
                    violationsByPackage.putIfAbsent(packagePath, new TreeMap<>());
                    var violatingPackagesByGroupName = violationsByPackage.get(packagePath);
                    violatingPackagesByGroupName.putIfAbsent(groupNameOfRdep, new TreeSet<>());
                    violatingPackagesByGroupName.get(groupNameOfRdep).add("//" + directReverseDependency);
                    out.notice(
                        format(
                            "Violation: %s (%s) <<(rdep)<< //%s (%s)",
                            packagePath,
                            groupName,
                            directReverseDependency,
                            groupNameOfRdep != null ? groupNameOfRdep : "no group"));
                } else if (verbose) {
                    out.notice(format("//%s is ok", directReverseDependency));
                }
            }
        }

        if (output == Output.json) {
            var gson = new GsonBuilder().setPrettyPrinting().create();
            out.info(gson.toJson(violationsByPackage));
        } else {
            List<String> buildozerCommands = new ArrayList<>();

            out.info("");
            out.info(
                format(
                    "Note, package groups will be recommended when there are %d or more violations from the same group.",
                    recommendPackageGroupThreshold));

            for (Entry<WorkspacePath, SortedMap<String, SortedSet<String>>> e : violationsByPackage.entrySet()) {
                var packagePath = e.getKey();

                if (output == Output.formatted) {
                    out.info("");
                    out.info("");
                    out.info("-----------------------------------------");
                    out.important(packagePath.toString());
                    out.info("");
                }

                SortedSet<String> recommendedAdditionalVisibility = new TreeSet<>();

                var violatingPackagesByGroupName = e.getValue();
                for (Entry<String, SortedSet<String>> groupNameAndPackages : violatingPackagesByGroupName.entrySet()) {
                    var packageGroup = getVisbilityGroup(groupNameAndPackages.getKey()).getPackageGroup();
                    if ((packageGroup != null)
                            && (groupNameAndPackages.getValue().size() >= recommendPackageGroupThreshold)) {
                        recommendedAdditionalVisibility.add(packageGroup);
                    } else {
                        recommendedAdditionalVisibility.addAll(groupNameAndPackages.getValue());
                    }
                }

                if (output == Output.formatted) {
                    var violationList = new StarlarkStringBuilder(4);
                    violationList.increaseIndention();
                    violationList.append("additional_visibility = ");
                    violationList.appendListQuotedWithWrappingWhenNecessary(recommendedAdditionalVisibility);
                    violationList.appendNewline();
                    out.info(violationList.toString());

                    out.info("-----------------------------------------");
                }

                for (String additionalVisibility : recommendedAdditionalVisibility) {
                    buildozerCommands.add(
                        format(
                            "add additional_visibility %s|//%s",
                            additionalVisibility,
                            ((buildozerOverrideTargetName == null) || buildozerOverrideTargetName.isBlank())
                                    ? packagePath : (packagePath + ":" + buildozerOverrideTargetName)));
                }
            }

            if (output == Output.buildozer) {
                var path = buildozerCommandsFile.isAbsolute() ? buildozerCommandsFile
                        : buildozerCommandsFile.toAbsolutePath();
                write(path, buildozerCommands); // don't use SCM tool (might be outside Git)
                out.info(format("Wrote buildozer commands to '%s'.", path));
                out.important("> buildozer -f " + path);
            }
        }

        return 0;
    }

    private Collection<String> queryForRDepsOfPackage(WorkspacePath packagePath) throws IOException {
        return queryForPackages(
            format("rdeps( //..., //%s, 1)", packagePath),
            format("Discover rdeps of '%s'", packagePath));
    }

    private boolean shouldAnalyze(WorkspacePath packagePath) {
        if (packageFilter != null) {
            return packageFilter.coversPackage(packagePath);
        }

        return requireNonNull(packageToAnalyzeExpression).toString().equals("//" + packagePath);
    }

    private boolean shouldIgnoreGroup(String groupName) {
        return !onlyGroups.isEmpty() && !onlyGroups.contains(requireNonNull(groupName));
    }

    private boolean shouldIgnoreRdep(WorkspacePath packagePath) {
        for (TargetExpression packageToIgnore : packagesToIgnore) {
            var filter = WildcardTargetPattern.fromExpression(packageToIgnore);
            if ((filter != null) && filter.coversPackage(packagePath)) {
                return true;
            }
            if (packageToIgnore.toString().equals("//" + packagePath.relativePath())) {
                return true;
            }
        }

        return false;
    }
}
