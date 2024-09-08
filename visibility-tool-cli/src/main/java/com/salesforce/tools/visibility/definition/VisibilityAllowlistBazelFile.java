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
import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.idea.blaze.base.model.primitives.Label;
import com.salesforce.tools.bazel.mavendependencies.starlark.ParseException;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkFileParser;

import net.starlark.java.syntax.Argument;
import net.starlark.java.syntax.Argument.Keyword;
import net.starlark.java.syntax.CallExpression;
import net.starlark.java.syntax.Expression.Kind;
import net.starlark.java.syntax.ExpressionStatement;
import net.starlark.java.syntax.Identifier;

public class VisibilityAllowlistBazelFile {

    static class Reader extends StarlarkFileParser<VisibilityAllowlistBazelFile> {

        public Reader(Path visibilityAllowlistBazelFile) throws IOException {
            super(visibilityAllowlistBazelFile);

            var fileName = visibilityAllowlistBazelFile.getFileName().toString();
            if (!fileName.equals("BUILD.bazel") && !fileName.equals("BUILD")) {
                throw new IllegalArgumentException(
                        format(
                            "Invalid visibility allowlist file: file '%s' should be named BUILD.bazel or BUILD!",
                            fileName));
            }
        }

        private boolean isPackageGroupCall(CallExpression statement) {
            return (statement.getFunction().kind() == Kind.IDENTIFIER)
                    && ((Identifier) statement.getFunction()).getName().equals("package_group");
        }

        @Override
        public VisibilityAllowlistBazelFile read() throws ParseException {
            SortedSet<PackageGroup> packageGroups = new TreeSet<>(Comparator.comparing(PackageGroup::getName));

            var statements = starlarkFile.getStatements()
                    .stream()
                    .filter(ExpressionStatement.class::isInstance)
                    .map(ExpressionStatement.class::cast)
                    .map(ExpressionStatement::getExpression)
                    .filter(CallExpression.class::isInstance)
                    .map(CallExpression.class::cast)
                    .collect(toList());
            for (CallExpression statement : statements) {
                if (!isPackageGroupCall(statement)) {
                    continue;
                }

                List<Argument> arguments = statement.getArguments();
                if (!arguments.stream().allMatch(Keyword.class::isInstance)) {
                    throw new ParseException(
                            "invalid visibility_group call: only keyword arguments allowed",
                            statement);
                }

                var keywordArguments = keywordArgumentsAsMap(arguments);
                var name = parseStringArgument(keywordArguments.get("name"));

                var packages = parseOptionalStringListArgument(keywordArguments.get("packages"));
                var includes = parseOptionalStringListArgument(keywordArguments.get("includes"));

                packageGroups.add(new PackageGroup(name, packages, includes));
            }

            return new VisibilityAllowlistBazelFile(packageGroups);
        }

    }

    public static VisibilityAllowlistBazelFile read(Label allowlistLabel, Path workspaceRoot) throws IOException {
        var directory = workspaceRoot.resolve(allowlistLabel.blazePackage().asPath());
        for (String candidateFile : List.of("BUILD.bazel", "BUILD")) {
            var file = directory.resolve(candidateFile);
            if (isRegularFile(file)) {
                return read(file);
            }
        }
        return null;
    }

    public static VisibilityAllowlistBazelFile read(Path existingFile) throws IOException {
        return new Reader(existingFile).read();
    }

    private final SortedSet<PackageGroup> packageGroups;

    public VisibilityAllowlistBazelFile(SortedSet<PackageGroup> packageGroups) {
        this.packageGroups = packageGroups;
    }

    public SortedSet<PackageGroup> getPackageGroups() {
        return this.packageGroups;
    }
}
