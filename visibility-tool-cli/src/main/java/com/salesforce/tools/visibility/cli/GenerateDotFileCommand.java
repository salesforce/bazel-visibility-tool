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
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.google.common.graph.EndpointPair;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.visibility.definition.VisibilityGroup;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Creates a dot file of the visibility information
 */
@Command(
        name = "generate-dot",
        description = "Generate a GraphViz dot file of all visibility groups.")
public class GenerateDotFileCommand extends BaseAnalyzeVisibilityCommand {

    private static final Pattern NOT_ALLOWED_ID_CHARS = Pattern.compile("[^_0-9A-Za-z\200-\377]");

    public static String toId(String value) {
        return NOT_ALLOWED_ID_CHARS.matcher(value).replaceAll("_");
    }

    @Option(
            names = { "--file" },
            description = "the file to write to (defaults to visibility.dot in current working directory)",
            defaultValue = "visibility.dot",
            required = false,
            paramLabel = "FILE")
    private Path dotFile;

    @Option(
            names = { "--subgraph-prefix" },
            description = "Defines subgraphs based on group prefixes (use comma to define multiple prefixes for one subgraph")
    private final SortedSet<String> subGraphPrefixes = new TreeSet<>();

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws IOException {
        var groupsGraph = getGraph();

        if (verbose) {
            out.notice(format("Writing %s", dotFile));
        }

        try (var writer = newBufferedWriter(dotFile, CREATE, TRUNCATE_EXISTING)) {
            writer.append("strict digraph visibility {").append(System.lineSeparator());
            writer.append("\tlabel=\"Visibility Graph\"").append(System.lineSeparator());
            writer.append(System.lineSeparator());

            if (!subGraphPrefixes.isEmpty()) {
                for (String prefix : subGraphPrefixes) {
                    List<String> prefixes = Arrays.asList(prefix.split(","));
                    writer.append("subgraph cluster_").append(toId(prefix)).append(" {").append(System.lineSeparator());
                    for (VisibilityGroup group : groupsGraph.nodes()) {
                        if (!prefixes.stream().anyMatch(p -> group.getName().startsWith(p))) {
                            continue;
                        }
                        var color = group.getVisibleToGroups().isEmpty() ? " color=\"lightgrey\" style=\"filled\"" : "";
                        writer.write(format("\t%s [label=\"%s\"%s]%n", toId(group.getName()), group.getName(), color));
                    }
                    writer.append("}").append(System.lineSeparator());
                    writer.append(System.lineSeparator());
                }
            } else {
                for (VisibilityGroup group : groupsGraph.nodes()) {
                    var color = group.getVisibleToGroups().isEmpty() ? " color=\"lightgrey\" style=\"filled\"" : "";
                    writer.write(format("\t%s [label=\"%s\"%s]%n", toId(group.getName()), group.getName(), color));
                }
                writer.append(System.lineSeparator());
            }

            for (EndpointPair<VisibilityGroup> edge : groupsGraph.edges()) {
                writer.write(format("\t%s -> %s%n", toId(edge.nodeU().getName()), toId(edge.nodeV().getName())));
            }
            writer.append(System.lineSeparator());

            writer.append("}").append(System.lineSeparator());
        }

        out.important(format("Wrote %s", dotFile));

        return 0;
    }
}
