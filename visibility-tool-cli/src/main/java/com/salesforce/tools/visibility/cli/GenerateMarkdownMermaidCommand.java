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
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import com.google.common.graph.EndpointPair;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.visibility.definition.VisibilityGroup;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Creates a marmaid snippet for embedding within Markdown.
 *
 * @see https://github.blog/2022-02-14-include-diagrams-markdown-files-mermaid/
 */
@Command(
        name = "generate-markdown-mermaid",
        description = "Generate a Markdown Mermaid code snippet for pasting a graph into GitHub Markdown files.")
public class GenerateMarkdownMermaidCommand extends BaseAnalyzeVisibilityCommand {

    private static final Pattern NOT_ALLOWED_ID_CHARS = Pattern.compile("[^_0-9A-Za-z\200-\377]");

    public static String toId(String value) {
        return NOT_ALLOWED_ID_CHARS.matcher(value).replaceAll("_");
    }

    @Option(
            names = { "--file" },
            description = "the file to write to (defaults to STDOUT if nothing is provided)",
            required = false,
            paramLabel = "FILE")
    private Path markdownFile;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws IOException {
        var groupsGraph = getGraph();

        if (verbose && (markdownFile != null)) {
            out.notice(format("Writing %s", markdownFile));
        }

        try (var writer = markdownFile != null ? Files.newBufferedWriter(markdownFile, CREATE, TRUNCATE_EXISTING)
                : new StringWriter()) {
            writer.append("```mermaid").append(System.lineSeparator());
            writer.append("graph TD").append(System.lineSeparator());

            for (EndpointPair<VisibilityGroup> edge : groupsGraph.edges()) {
                writer.write(format("    %s --> %s%n", edge.nodeU().getName(), edge.nodeV().getName()));
            }
            writer.append("```").append(System.lineSeparator());

            if (markdownFile == null) {
                ((StringWriter) writer).flush();
                System.out.println(((StringWriter) writer).toString());
            }
        }

        if (markdownFile != null) {
            out.important(format("Wrote %s", markdownFile));
        }

        return 0;
    }
}
