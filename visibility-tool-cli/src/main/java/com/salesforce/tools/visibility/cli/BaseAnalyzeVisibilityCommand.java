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

import static com.salesforce.tools.visibility.definition.BaseVisibilityQueryTool.TOOLS_BUILD_VISIBILITY;
import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.google.common.graph.ImmutableGraph;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.salesforce.tools.bazel.cli.BaseCommandWithWorkspaceRoot;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.visibility.cli.picocli.TargetExpressionConverter;
import com.salesforce.tools.visibility.definition.VisibilityGroup;
import com.salesforce.tools.visibility.definition.VisibilityGroupInfoQueryTool;
import com.salesforce.tools.visibility.query.BazelQueryForPackagesCommand;
import com.salesforce.tools.visibility.util.bazel.BazelBinaryFinder;
import com.salesforce.tools.visibility.util.bazel.BazelCommandExecutorWithProgress;

import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * Base class for commands analyzing visibility information.
 */
public abstract class BaseAnalyzeVisibilityCommand extends BaseCommandWithWorkspaceRoot {

    @Option(
            names = { "--visibility-package" },
            description = "Bazel package with the visibility definitions in the current workspace. (default is //tools/build/visibility)",
            defaultValue = TOOLS_BUILD_VISIBILITY,
            paramLabel = "VISIBILITY_PACKAGE",
            scope = ScopeType.INHERIT,
            converter = TargetExpressionConverter.class,
            required = false)
    protected TargetExpression visibilityPackage;

    @Option(
            names = { BazelBinaryFinder.BAZEL_BINARY_COMMAND_OPTION_NAME },
            description = "Bazel binary to use for executing queries (default is 'tools/bazel' wrapper script if it exists otherwise 'bazel' binary in PATH)",
            paramLabel = "BAZEL",
            scope = ScopeType.INHERIT,
            required = false)
    private Path bazelBinaryPath;

    protected BazelCommandExecutorWithProgress executor;
    protected VisibilityGroupInfoQueryTool visibilityGroupInfoLoader;
    private final Supplier<ImmutableGraph<VisibilityGroup>> groupGraphSupplier =
            Suppliers.memoize(() -> new VisibilityGraphBuilder(getVisbilityGroups()).getGraph());

    @Override
    protected void afterExecuteCommand(int returnCode, MessagePrinter out) {
        printScmActivity(out);

        if (printFeedbackNotice && !batchMode && (returnCode == 0)) {
            out.notice(
                format(
                    "%n%nLike what you are seeing?%nPlease leave feedback at https://github.com/salesforce/bazel-visibility-tool."));
        }
    }

    protected abstract int doExecuteCommand(MessagePrinter out) throws IOException;

    @Override
    protected final int executeCommand(MessagePrinter out) throws Exception {
        if (verbose) {
            out.notice("Using visibility package " + visibilityPackage);
        }
        var bazelBinary = new BazelBinaryFinder().getBazelBinary(bazelBinaryPath, workspaceRoot);
        executor = new BazelCommandExecutorWithProgress(out, bazelBinary, verbose);

        visibilityGroupInfoLoader = new VisibilityGroupInfoQueryTool(visibilityPackage, workspaceRoot, executor);

        return doExecuteCommand(out);
    }

    protected ImmutableGraph<VisibilityGroup> getGraph() {
        return groupGraphSupplier.get();
    }

    /**
     * Returns a group for a name.
     *
     * @param name
     *            the group name
     * @return the {@link VisibilityGroup} (never <code>null</code>)
     * @throws IllegalArgumentException
     *             if the group is not defined
     */
    protected VisibilityGroup getVisbilityGroup(String name) throws IllegalArgumentException {
        return getVisbilityGroup(name, () -> format("Visibility group '%s' is not defined!", name));
    }

    /**
     * Returns a group for a name.
     *
     * @param name
     *            the group name
     * @param errorMessageSupplier
     *            supplier for an error message is the group is missing
     * @return the {@link VisibilityGroup} (never <code>null</code>)
     * @throws IllegalArgumentException
     *             if the group is not defined
     */
    protected VisibilityGroup getVisbilityGroup(
            String name,
            Supplier<String> errorMessageSupplier) throws IllegalArgumentException {
        var result = visibilityGroupInfoLoader.getVisibilityGroups()
                .stream()
                .filter(g -> g.getName().equals(name))
                .findAny();
        if (result.isPresent()) {
            return result.get();
        }

        throw new IllegalArgumentException(errorMessageSupplier.get());
    }

    protected Collection<VisibilityGroup> getVisbilityGroups() {
        return visibilityGroupInfoLoader.getVisibilityGroups();
    }

    protected Path getVisibilityPackagePath() {
        return visibilityGroupInfoLoader.getVisibilityPackageDir();
    }

    /**
     * @return a list of all packages in the workspace *excluding* the visibility package
     * @throws IOException
     */
    protected Collection<String> queryForAllPackages() throws IOException {
        return queryForPackages(
            format("//... - %s/...", visibilityPackage),
            format("Discover packages in '%s'", workspaceRoot.getFileName()));
    }

    protected Collection<String> queryForPackages(String query, String purpose) throws IOException {
        var queryForPackagesCommand = new BazelQueryForPackagesCommand(workspaceRoot, query, false, purpose);

        return executor.execute(queryForPackagesCommand, Boolean.FALSE::booleanValue);
    }

    protected Path resolvePathInVisibilityPackage(Path visibilityPackageRelativePath) {
        if (visibilityPackageRelativePath.isAbsolute()) {
            throw new IllegalArgumentException("path must not be absolute: " + visibilityPackageRelativePath);
        }

        return getVisibilityPackagePath().resolve(visibilityPackageRelativePath);
    }

}
