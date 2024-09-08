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
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.salesforce.tools.bazel.mavendependencies.starlark.ParseException;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkFileParser;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;

import net.starlark.java.syntax.AssignmentStatement;
import net.starlark.java.syntax.Expression.Kind;
import net.starlark.java.syntax.Identifier;
import net.starlark.java.syntax.ListExpression;

/**
 * A Starlark <code>.bzl</code> file with a single (public) variable assignment of type string list.
 */
public class StringListVariablesBazelFile {

    static class Reader extends StarlarkFileParser<StringListVariablesBazelFile> {

        public Reader(Path visibilityAllowlistBazelFile) throws IOException {
            super(visibilityAllowlistBazelFile);

            var fileName = visibilityAllowlistBazelFile.getFileName().toString();
            if (!fileName.endsWith(".bzl")) {
                throw new IllegalArgumentException(format("Invalid file: file '%s' must end with .bzl!", fileName));
            }
        }

        @Override
        public StringListVariablesBazelFile read() throws ParseException {
            Map<String, SortedSet<String>> assignments = new TreeMap<>();

            var statements = starlarkFile.getStatements()
                    .stream()
                    .filter(AssignmentStatement.class::isInstance)
                    .map(AssignmentStatement.class::cast)
                    .collect(toList());

            for (AssignmentStatement assignment : statements) {
                if (assignment.getLHS().kind() != Kind.IDENTIFIER) {
                    throw new ParseException("invalid assignment: left hand side must be an identifier", assignment);
                }

                var variableName = ((Identifier) assignment.getLHS()).getName();
                if (variableName.startsWith("_")) {
                    continue;
                }

                if (assignment.getRHS().kind() != Kind.LIST_EXPR) {
                    throw new ParseException("invalid assignment: right hand side must be list expression", assignment);
                }

                var values = parseStringListExpression((ListExpression) assignment.getRHS());
                assignments.put(variableName, new TreeSet<>(values));
            }

            return new StringListVariablesBazelFile(assignments);
        }

    }

    public static StringListVariablesBazelFile read(Path existingFile) throws IOException {
        return new Reader(existingFile).read();
    }

    private final SortedMap<String, SortedSet<String>> assignments;

    public StringListVariablesBazelFile() {
        this(new TreeMap<>());
    }

    public StringListVariablesBazelFile(Map<String, SortedSet<String>> assignments) {
        this.assignments = new TreeMap<>(assignments);
    }

    public SortedSet<String> getAssignment(String variableName) {
        return assignments.get(variableName);
    }

    public CharSequence prettyPrint(String preamble) {
        var output = new StarlarkStringBuilder(4);

        if ((preamble != null) && !preamble.isBlank()) {
            output.append(preamble);
            output.appendNewline();
        }

        for (Entry<String, SortedSet<String>> variables : assignments.entrySet()) {
            output.append(variables.getKey())
                    .append(" = ")
                    .appendListQuotedWithWrappingWhenNecessary(variables.getValue())
                    .appendNewline();
        }

        return output.toString();
    }

    public SortedSet<String> setAssignment(String variableName, Collection<String> values) {
        return assignments.put(variableName, new TreeSet<>(values));
    }
}
