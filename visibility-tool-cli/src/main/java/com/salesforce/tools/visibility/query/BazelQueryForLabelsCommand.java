package com.salesforce.tools.visibility.query;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.readAllLines;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.salesforce.tools.visibility.util.bazel.BazelVersion;

/**
 * <code>bazel query --output label</code>
 */
public class BazelQueryForLabelsCommand extends BazelQueryCommand<Collection<String>> {

    public BazelQueryForLabelsCommand(Path workspaceRoot, String query, boolean keepGoing, String purpose) {
        super(workspaceRoot, query, keepGoing, purpose);
        setCommandArgs("--output", "label");
    }

    @Override
    protected Collection<String> doGenerateResult() throws IOException {
        return readAllLines(getStdOutFile());
    }

    @Override
    public List<String> prepareCommandLine(BazelVersion bazelVersion) throws IOException {
        // redirect output to file for parsing
        var stdoutFile = createTempFile("bazel_query_stdout_", ".bin");
        setRedirectStdOutToFile(stdoutFile);

        // prepare regular query command line
        return super.prepareCommandLine(bazelVersion);
    }
}
