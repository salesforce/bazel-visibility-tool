package com.salesforce.tools.visibility.cli;

import java.util.concurrent.Callable;

import com.salesforce.tools.bazel.cli.BaseCommand;
import com.salesforce.tools.visibility.cli.mavendeps.GenerateMavenDepsAllowList;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * Uber command for the Visibility Tool
 */
@Command(
        name = "visibility-tool",
        synopsisSubcommandLabel = "COMMAND",
        subcommands = { //@formatter:off

        QueryVisibilityGroupsCommand.class,
        AnalyzePackagesCommand.class,

        GenerateGroupVisibilityMatrixCommand.class,

        GenerateMembershipFilesCommand.class,
        GenerateMavenDepsAllowList.class,

        GenerateDotFileCommand.class,
        GenerateMarkdownMermaidCommand.class

}) //@formatter:on

public class VisibilityToolCli implements Callable<Integer> {

    public static void main(final String... args) {
        BaseCommand.execute(new VisibilityToolCli(), args);
    }

    @Option(
            names = { "-h", "--help" },
            usageHelp = true,
            description = "Prints this help text")
    boolean helpRequested;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        throw new ParameterException(spec.commandLine(), "Something went wrong. Did you specify a subcommand?");
    }

}
