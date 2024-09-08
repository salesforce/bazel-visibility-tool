package com.salesforce.tools.visibility.util.bazel;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

/**
 * Record of a Bazel binary to use.
 */
public class BazelBinary {

    private final Path executable;
    private final BazelVersion bazelVersion;

    public BazelBinary(Path executable, BazelVersion bazelVersion) {
        this.executable = requireNonNull(executable, "executable must not be null");
        this.bazelVersion = requireNonNull(bazelVersion, "bazelVersion must not be null");
    }

    public BazelVersion bazelVersion() {
        return bazelVersion;
    }

    public Path executable() {
        return executable;
    }
}
