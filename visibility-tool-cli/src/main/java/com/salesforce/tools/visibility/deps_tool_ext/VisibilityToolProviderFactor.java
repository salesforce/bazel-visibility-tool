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
package com.salesforce.tools.visibility.deps_tool_ext;

import static java.lang.String.format;

import java.nio.file.Path;

import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.visibility.VisibilityProvider;
import com.salesforce.tools.bazel.mavendependencies.visibility.VisibilityProviderFactory;
import com.salesforce.tools.visibility.definition.BaseVisibilityQueryTool;
import com.salesforce.tools.visibility.util.bazel.BazelBinaryFinder;
import com.salesforce.tools.visibility.util.bazel.BazelCommandExecutorWithProgress;

import picocli.CommandLine.Model.CommandSpec;

public class VisibilityToolProviderFactor implements VisibilityProviderFactory {

    @Override
    public VisibilityProvider create(MessagePrinter out, CommandSpec spec, boolean verbose, Path workspaceRoot) {
        var visibilityPackageOption = spec.optionsMap().get("--visibility-package");
        var visibilityPackage =
                visibilityPackageOption != null ? (TargetExpression) visibilityPackageOption.getValue() : null;

        if (verbose) {
            out.notice(
                format(
                    "Initializing visibility provider for bazel_maven_deps (%s, %s)",
                    visibilityPackage == null ? BaseVisibilityQueryTool.TOOLS_BUILD_VISIBILITY : visibilityPackage,
                    workspaceRoot));
        }

        var bazelBinary = new BazelBinaryFinder().getBazelBinary(spec, workspaceRoot);
        var executor = new BazelCommandExecutorWithProgress(out, bazelBinary, verbose);
        return new VisibilityToolProvider(out, verbose, workspaceRoot, visibilityPackage, executor);
    }

}
