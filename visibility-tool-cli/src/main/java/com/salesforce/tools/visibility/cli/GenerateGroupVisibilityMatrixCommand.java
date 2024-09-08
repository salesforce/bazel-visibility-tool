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
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isDirectory;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import com.google.idea.blaze.base.model.primitives.Label;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;
import com.salesforce.tools.visibility.definition.VisibilityGroup;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Generates a matrix of group names and which packages groups they are visible to.
 */
@Command(
        name = "generate-group-visibility-matrix",
        description = "Generate the group visibility matrix information for supporting macros in the Bazel workspace")
public class GenerateGroupVisibilityMatrixCommand extends BaseAnalyzeVisibilityCommand {

    @Option(
            names = { "--ignore-group" },
            description = "A group to ignore when writing package visibiliy infos (eg., useful for empty or placeholder groups)",
            paramLabel = "GROUP_NAME",
            required = false)
    private final SortedSet<String> groupNamesToSkip = new TreeSet<>();

    @Option(
            names = { "--file-name" },
            description = "Path to a bzl file (either absolute or relativ to the visibility package) for writing the visibility matrix to (default is 'visibility-matrix.bzl')",
            paramLabel = "FILE",
            required = false)
    private final Path matrixFile = Path.of("visibility-matrix.bzl");

    private void appendGroupVisibilityValue(StarlarkStringBuilder output, VisibilityGroup visibilityGroup) {
        SortedSet<Label> visibility = new TreeSet<>();

        var visibleToGroups = visibilityGroup.getVisibleToGroups();
        for (String groupName : visibleToGroups) {
            var target = getVisbilityGroup(
                groupName,
                () -> format(
                    "Invalid reference in visible_to_groups attribute of group '%s': group '%s' is not defined!",
                    visibilityGroup.getName(),
                    groupName));

            // only add a group if it is not ignored
            if (!shouldIgnore(target) && (target.getPackageGroup() != null)) {
                visibility.add(Label.create(target.getPackageGroup()));
            }
        }

        if (visibilityGroup.getVisibilityAllowList() != null) {
            visibility.add(Label.create(visibilityGroup.getVisibilityAllowList()));
        }

        output.append("# visibility for group ").append(visibilityGroup.getName()).appendNewline();
        output.append(visibilityGroupVisibilityVariable(visibilityGroup))
                .append(" = ")
                .appendListQuotedWithWrappingWhenNecessary(visibility.stream().map(Label::toString).collect(toList()))
                .appendNewline();
    }

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws IOException {
        var targetFile = !matrixFile.isAbsolute() ? getVisibilityPackagePath().resolve(matrixFile) : matrixFile;
        if (!isDirectory(targetFile.getParent())) {
            createDirectories(targetFile.getParent());
        }

        writeGroupsVisibilityFile(targetFile);

        return 0;
    }

    private List<VisibilityGroup> getRelevantGroups() {
        return getVisbilityGroups().stream().filter(Predicate.not(this::shouldIgnore)).collect(toList());
    }

    private boolean shouldIgnore(VisibilityGroup group) {
        return groupNamesToSkip.contains(group.getName());
    }

    private String visibilityGroupVisibilityVariable(VisibilityGroup visibilityGroup) {
        return toStarlarkIdentifier(visibilityGroup.getName()) + "_visibility";
    }

    private void writeGroupsVisibilityFile(Path targetFile) throws IOException {
        var output = new StarlarkStringBuilder(4);
        output.append("\"\"\"Generated group visibility matrix for supporting macros.\"\"\"").appendNewline();
        output.append("visibility(\"public\")").appendNewline();
        output.appendNewline();

        // only groups not ignored
        var visbilityGroups = getRelevantGroups();

        // define individual variables with visibility per group
        for (VisibilityGroup visibilityGroup : visbilityGroups) {
            appendGroupVisibilityValue(output, visibilityGroup);
            output.appendNewline();
        }

        // define dict with all groups as key and reference to the variables
        output.append("# dictionary of visibility groups and their visibility").appendNewline();
        output.append("visibility_groups_default_visibility").append(" = {").appendNewline().increaseIndention();
        for (VisibilityGroup visibilityGroup : visbilityGroups) {
            output.appendQuoted(visibilityGroup.getName())
                    .append(": ")
                    .append(visibilityGroupVisibilityVariable(visibilityGroup))
                    .appendCommaFollowedByNewline();
        }
        output.decreaseIndention().append("}").appendNewline();

        getScmTool().writeFile(targetFile, output.toString(), UTF_8);
    }
}
