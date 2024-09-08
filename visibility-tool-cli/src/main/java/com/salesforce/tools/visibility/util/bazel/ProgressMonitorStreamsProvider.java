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

import static java.util.stream.Collectors.joining;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;
import com.salesforce.tools.visibility.util.bazel.DefaultBazelCommandExecutor.PreparedCommandLine;

/**
 * A {@link ProcessStreamsProvider} reporting progress to e {@link ProgressMonitor} and redirecting any command output
 * to a file.
 */
public class ProgressMonitorStreamsProvider extends ProcessStreamsProvider {

    class BufferingAndProgressReportingOutputStream extends BufferedOutputStream {
        BufferingAndProgressReportingOutputStream(OutputStream os) {
            super(os);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            progressMonitor.progressBy(len);
        }

        @Override
        public synchronized void write(int b) throws IOException {
            super.write(b);
            progressMonitor.progressBy(1L);
        }
    }

    private final ProgressMonitor progressMonitor;
    private final BufferedOutputStream outputStream;
    private final Path outputFile;
    private final MessagePrinter out;
    private final PreparedCommandLine commandLine;
    private final boolean verbose;

    public ProgressMonitorStreamsProvider(BazelCommand<?> command, PreparedCommandLine commandLine, MessagePrinter out,
            Path outputFile, boolean verbose) throws IOException {
        this.commandLine = commandLine;
        this.out = out;
        this.verbose = verbose;
        this.progressMonitor = out.progressMonitor(command.getPurpose());
        this.outputFile = outputFile;
        outputStream = new BufferingAndProgressReportingOutputStream(
                Files.newOutputStream(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }

    @Override
    public void beginExecution() {
        progressMonitor.progressBy(1L);

        if (verbose) {
            out.notice("");
            out.notice(
                "> " + commandLine.commandLineForDisplayPurposes()
                        .stream()
                        .map(this::simpleQuoteForDisplayOnly)
                        .collect(joining(" ")));
        }
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
        try {
            progressMonitor.close();
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void commandResultGenerated(Object commandResult) {
        progressMonitor.progressBy(1L);
    }

    @Override
    public void executionCanceled() {
        out.warning("Command execution canceled!");
        progressMonitor.done();
    }

    @Override
    public void executionFailed(IOException cause) throws IOException {
        progressMonitor.done();
        out.error(cause.getMessage());
        out.error(commandLine.commandLineForDisplayPurposes().stream().collect(joining(" ")));
        outputStream.flush();
        Files.readAllLines(outputFile).forEach(System.err::println);
    }

    @Override
    public void executionFinished(int exitCode) {
        progressMonitor.done();
    }

    @Override
    public OutputStream getErrorStream() {
        return outputStream;
    }

    /**
     * @return the outputFile
     */
    public Path getOutputFile() {
        return outputFile;
    }

    @Override
    public OutputStream getOutStream() {
        progressMonitor.progressBy(1L);
        return outputStream;
    }

    private String simpleQuoteForDisplayOnly(String arg) {
        if (arg.indexOf(' ') > -1) {
            return (arg.indexOf('\'') > -1) ? '"' + arg + '"' : "'" + arg + "'";
        }
        return arg;
    }
}
