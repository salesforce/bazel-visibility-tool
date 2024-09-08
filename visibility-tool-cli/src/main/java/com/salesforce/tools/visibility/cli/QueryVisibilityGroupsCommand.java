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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Collections;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.visibility.definition.MavenDepsVisibilityInfo;
import com.salesforce.tools.visibility.definition.MavenDepsVisibilityInfoQueryTool;
import com.salesforce.tools.visibility.definition.VisibilityGroup;
import com.salesforce.tools.visibility.definition.VisibilityPackageInfo;
import com.salesforce.tools.visibility.definition.VisibilityPackageInfoQueryTool;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Scans the current workspace for visibility groups and visibility definitions.
 */
@Command(
        name = "query-visibility-groups",
        description = "Queries the workspace for visibility groups definitions.")
public class QueryVisibilityGroupsCommand extends BaseAnalyzeVisibilityCommand {

    @Option(
            names = { "--print-package-details" },
            description = "Indicates if group membership details about packages should be printed",
            required = false)
    private boolean printPackageDetails;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws IOException {
        var groups = getVisbilityGroups();

        out.info("");
        out.important("Number of visibility groups: " + groups.size());
        for (VisibilityGroup group : groups) {
            out.info(format(" - %s (%s)", group.getName(), group.getLabel()));
        }
        out.info("");

        var mavenDepsVisibilityInfoQueryTool =
                new MavenDepsVisibilityInfoQueryTool(visibilityPackage, workspaceRoot, executor);
        var mavenDepsVisibilityInfos = mavenDepsVisibilityInfoQueryTool.getMavenDepsVisibilityInfos().collect(toList());

        out.info("");
        out.important("Number of bazel_maven_deps visibility definitions: " + mavenDepsVisibilityInfos.size());
        for (MavenDepsVisibilityInfo info : mavenDepsVisibilityInfos) {
            out.info(
                format(" - %s: %s", info.getGroupName(), info.getIncludePatterns().stream().collect(joining(","))));
        }
        out.info("");

        if (printPackageDetails) {
            var packageInfoQueryTool = new VisibilityPackageInfoQueryTool(
                    visibilityPackage,
                    workspaceRoot,
                    executor,
                    Collections.emptyList());

            out.info("");
            out.important(
                "Number of packages with visibility definitions: "
                        + packageInfoQueryTool.getVisibilityPackageInfos().count());
            for (String groupName : packageInfoQueryTool.getGroupsNamesWithVisibilityPackageInfos()) {
                out.info(format(" - %s:", groupName));
                for (VisibilityPackageInfo info : packageInfoQueryTool.getVisibilityPackageInfos(groupName)) {
                    out.info(format("    - //%s", info.getPackagePath()));
                }
            }
            out.info("");
        }

        return 0;
    }
}
