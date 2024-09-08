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
package com.salesforce.tools.visibility.cli.mavendeps;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isRegularFile;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelDependenciesCatalog;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;
import com.salesforce.tools.visibility.cli.BaseAnalyzeVisibilityCommand;
import com.salesforce.tools.visibility.definition.MavenDepsVisibilityInfoQueryTool;
import com.salesforce.tools.visibility.definition.PackageGroup;
import com.salesforce.tools.visibility.definition.VisibilityGroup;
import com.salesforce.tools.visibility.deps_tool_ext.MavenDepsVisibilityAnalyzer;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Generate allowlist for bazel_maven_deps visibility group caputring all violations
 */
@Command(
        name = "generate-maven-deps-allowlist",
        description = "Generates/updates an allowlist for a visibility group based on the bazel_maven_deps information")
public class GenerateMavenDepsAllowList extends BaseAnalyzeVisibilityCommand {

    @Option(
            names = { "--group", "-g" },
            description = "The group to analyze",
            paramLabel = "GROUP_NAME",
            required = true,
            arity = "1..*")
    private final SortedSet<String> groupsToAnalyze = new TreeSet<>();

    private MavenDepsVisibilityInfoQueryTool mavenDepsVisibilityInfoQueryTool;
    private MavenDepsVisibilityAnalyzer mavenDepsVisibilityAnalyzer;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws IOException {
        if (verbose) {
            out.notice("Analyzing group(s): " + groupsToAnalyze.stream().collect(joining(", ")));
        }

        mavenDepsVisibilityInfoQueryTool =
                new MavenDepsVisibilityInfoQueryTool(visibilityPackage, workspaceRoot, executor);
        mavenDepsVisibilityAnalyzer =
                new MavenDepsVisibilityAnalyzer(mavenDepsVisibilityInfoQueryTool.getMavenDepsVisibilityInfos());

        // collect all repos we are interested in
        SortedMap<VisibilityGroup, SortedSet<String>> reposOfInterestByGroup =
                new TreeMap<>(Comparator.comparing(VisibilityGroup::getName));

        if (verbose) {
            out.notice("Loading pinned dependency catalog");
        }
        var dependenciesCatalog = BazelDependenciesCatalog.load(workspaceRoot);
        SortedSet<String> allRepoNames = dependenciesCatalog.getAllImports()
                .map(BazelJavaDependencyImport::getName)
                .collect(Collectors.toCollection(TreeSet::new));
        for (String repo : allRepoNames) {
            var info = mavenDepsVisibilityAnalyzer.findGroupForExternalRepository(repo);
            if ((info != null) && groupsToAnalyze.contains(info.getGroupName())) {
                LOG.debug("Adding repo '{}' for group '{}'", repo, info.getGroupName());
                var group = getVisbilityGroup(info.getGroupName());
                if (!reposOfInterestByGroup.containsKey(group)) {
                    reposOfInterestByGroup.put(group, new TreeSet<>());
                }

                reposOfInterestByGroup.get(group).add(repo);
            }
        }

        // sanity check that we have results for each group
        // otherwise there is an error with the command line
        if (reposOfInterestByGroup.size() != groupsToAnalyze.size()) {
            SortedSet<String> foundGroupNames = reposOfInterestByGroup.keySet()
                    .stream()
                    .map(VisibilityGroup::getName)
                    .collect(toCollection(TreeSet::new));
            throw new IllegalStateException(
                    format(
                        "Unable to locate bazel_maven_deps information for the following groups. Please check they exist!%n - %s",
                        groupsToAnalyze.stream().filter(not(foundGroupNames::contains)).collect(joining("\n - "))));
        }

        for (Entry<VisibilityGroup, SortedSet<String>> e : reposOfInterestByGroup.entrySet()) {
            var group = e.getKey();
            var repos = e.getValue();

            if (verbose) {
                out.notice("Analyzing group " + group.getName());
            }

            var packages = queryForPackages(
                format("rdeps( //..., %s, 1)", repos.stream().collect(joining(" + @", "@", ""))),
                format("Discover rdeps of group '%s'", group.getName()));

            // remove external references and turn into labels
            packages = packages.stream().filter(p -> !p.startsWith("@")).map(p -> "//" + p).collect(toList());

            var visibilityAllowList = group.getVisibilityAllowList();
            if (visibilityAllowList == null) {
                visibilityAllowList = visibilityPackage.toString() + "/allowlists/" + group.getName() + "-exceptions";
                out.warning(
                    format(
                        "Group '%s' is missing an allow list. Don't forget to add it using:%n%n  > buildozer 'set visibility_allow_list \"%s\"' %s",
                        group.getName(),
                        visibilityAllowList,
                        group.getLabel()));
            }
            if (visibilityAllowList.indexOf(':') == -1) {
                visibilityAllowList = visibilityAllowList +=
                        ":" + new WorkspacePath(visibilityAllowList.substring(2)).asPath().getFileName().toString();
            }

            writeAllowList(out, packages, Label.create(visibilityAllowList), group);
        }

        return 0;
    }

    private void writeAllowList(
            MessagePrinter out,
            Collection<String> packages,
            Label allowList,
            VisibilityGroup group) throws IOException {
        var buildFile = workspaceRoot.resolve(allowList.blazePackage().asPath()).resolve("BUILD.bazel");

        if (packages.isEmpty()) {
            if (isRegularFile(buildFile)) {
                if (verbose) {
                    out.notice("Removing no longer needed allow list: " + buildFile);
                }
                if (getScmTool().removeFile(buildFile)) {
                    out.important(
                        format(
                            "Group '%s' no longer needs an allow list. Don't forget to unset it using:%n%n  > buildozer 'remove visibility_allow_list' %s",
                            group.getName(),
                            group.getLabel()));
                }
            }
            return;
        }

        createDirectories(buildFile.getParent());

        var output = new StarlarkStringBuilder(4);
        var packageGroup = new PackageGroup(allowList.targetName().toString(), packages, null);
        packageGroup.appendTo(output);

        getScmTool().writeFile(buildFile, output.toString(), UTF_8);
        if (verbose) {
            out.info("Generated " + buildFile);
        }
    }

}
