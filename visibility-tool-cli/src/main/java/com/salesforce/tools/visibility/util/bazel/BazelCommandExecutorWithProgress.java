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

import static java.nio.file.Files.createTempFile;

import java.io.IOException;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;

/**
 * An executor which reports progress using {@link MessagePrinter#progressMonitor(String)}
 */
public class BazelCommandExecutorWithProgress extends DefaultBazelCommandExecutor {

    private final MessagePrinter out;
    private final boolean verbose;

    public BazelCommandExecutorWithProgress(MessagePrinter out, BazelBinary bazelBinary, boolean verbose) {
        this.out = out;
        this.verbose = verbose;
        setBazelBinary(bazelBinary);
    }

    @Override
    protected ProcessStreamsProvider newProcessStreamProvider(
            BazelCommand<?> command,
            PreparedCommandLine commandLine) throws IOException {
        return new ProgressMonitorStreamsProvider(
                command,
                commandLine,
                out,
                createTempFile("bazel_", "_exec.log"),
                verbose);
    }

}
