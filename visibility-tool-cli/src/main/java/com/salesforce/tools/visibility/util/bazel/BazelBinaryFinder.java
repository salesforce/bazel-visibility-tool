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
package com.salesforce.tools.visibility.util.bazel;

import static java.nio.file.Files.isExecutable;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.salesforce.tools.bazel.cli.helper.UnifiedLogger;

import picocli.CommandLine.Model.CommandSpec;

/**
 * A helper for finding the BazelBinary to user.
 *
 */
public class BazelBinaryFinder {

    /** hard code version to 7 because query does not has significant syntax dependencies on Bazel version */
    private static final BazelVersion BAZEL_VERSION = new BazelVersion(7, 0, 0);

    /**
     * A command option with value type {@link Path}, which defines the Bazel binary to use.
     */
    public static final String BAZEL_BINARY_COMMAND_OPTION_NAME = "--bazel-binary";

    private static final Logger LOG = UnifiedLogger.getLogger();

    /**
     * Returns a Bazel binary to use
     *
     * @param spec
     *            the command spec to use for extracting an optional Bazel binary path to use
     * @param workspaceRoot
     *            the workspace root to check for a <code>tools/bazel</code> wrapper
     * @return the {@link BazelBinary} to use
     */
    public BazelBinary getBazelBinary(CommandSpec spec, Path workspaceRoot) {
        // prefer command override
        var bazelBinaryOption = spec.optionsMap().get(BAZEL_BINARY_COMMAND_OPTION_NAME);
        if (bazelBinaryOption != null) {
            var path = (Path) bazelBinaryOption.getValue();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found '--bazel-binary' command option with value '{}", path);
            }
            return getBazelBinary(path, workspaceRoot);
        }

        return getBazelBinary(workspaceRoot);
    }

    /**
     * Returns a Bazel binary to use from the workspace
     *
     * @param workspaceRoot
     *            the workspace root to check for a <code>tools/bazel</code> wrapper
     * @return the {@link BazelBinary} to use
     */
    private BazelBinary getBazelBinary(Path workspaceRoot) {
        // check for 'tools/bazel' wrapper script
        var toolsBazelWrapper = workspaceRoot.resolve("tools/bazel");
        if (isExecutable(toolsBazelWrapper)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found 'tools/bazel' wrapper script at '{}", toolsBazelWrapper);
            }
            return new BazelBinary(toolsBazelWrapper, BAZEL_VERSION);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Using default 'bazel' command. Please ensure PATH environment is correct.");
        }
        return new BazelBinary(Path.of("bazel"), BAZEL_VERSION);
    }

    /**
     * Returns a Bazel binary to use
     *
     * @param bazelBinaryPath
     *            an optional Bazel binary path to use (Eg., from a an explicit specified command option)
     * @param workspaceRoot
     *            the workspace root to check for a <code>tools/bazel</code> wrapper
     * @return the {@link BazelBinary} to use
     */
    public BazelBinary getBazelBinary(Path bazelBinaryPath, Path workspaceRoot) {
        // prefer command override
        if (bazelBinaryPath != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using specified Bazel binary '{}'", bazelBinaryPath);
            }
            return new BazelBinary(bazelBinaryPath, BAZEL_VERSION);
        }

        return getBazelBinary(workspaceRoot);
    }

}
