package org.ek9lang.cli;

/**
 * Holds the command line help text only. There are quite a few options, so this encapsulates descriptions.
 */
public record CommandLineHelp(String helpText) {

  public CommandLineHelp() {
    this("""
        where possible options include:
        \t-V The version of the compiler/runtime
        \t-ls Run compiler as Language Server
        \t-lsh Provide EK9 Language Help/Hover
        \t-h Help message
        \t-c Incremental compile; but don't run
        \t-ch Incremental compile; but don't link to final executable
        \t-cg Incremental compile but with debugging information; but don't run
        \t-cd Incremental compile include dev code and debugging information; but don't run
        \t-cdh Incremental compile; include dev code, but don't link to final executable
        \t-Cp [phase] Full recompilation; compile up to a specific phase
        \t-Cdp [phase] Full recompilation (include dev code); compile up to a specific phase
        \t-C Force full recompilation; but don't run
        \t-Ch Force full recompilation; but don't link to final executable
        \t-Cg Force full recompilation with debugging information; but don't run
        \t-Cd Force full recompilation include dev code and debugging information; but don't run
        \t-Cdh Force full recompilation; include dev code, but don't link to final executable
        \t-Cl Clean all, including dependencies
        \t-Dp Resolve all dependencies, this triggers -Cl clean all first
        \t-P Package ready for deployment to artefact server/library.
        \t-I Install packaged software to your local library.
        \t-Gk Generate signing keys for deploying packages to an artefact server
        \t-D Deploy packaged software, this triggers -P packaging first and -Gk if necessary
        \t-IV [major|minor|patch|build] - increment part of the release vector
        \t-SV major.minor.patch - setting to a value i.e 6.8.1-0 (zeros build)
        \t-SF major.minor.patch-feature - setting to a value i.e 6.8.0-specials-0 (zeros build)
        \t-PV print out the current version i.e 6.8.1-0 or 6.8.0-specials-0 for a feature.
        \t-Up Check for newer version of ek9 and upgrade if available.
        \t-v Verbose mode
        \t-dv Debug mode - produces internal debug information during compilation
        \t-t Runs all unit tests that have been found, this triggers -Cd full compile first
        \t-d port Run in debug mode (requires debugging information - on a port)
        \t-e <name>=<value> set environment variable i.e. user=Steve or user='Steve Limb' for spaces
        \t-T target architecture - defaults to 'jvm' if not specified.
        \t-O0 No optimization (fast compile, maximum debuggability)
        \t-O2 Minimal optimization (balanced - default for normal builds)
        \t-O3 Full optimization (maximum performance - default for packaging)
        \tfilename.ek9 - the main file to work with
        \t-r Program to run (if EK9 file as more than one)

        Environment Variables:
        \tEK9_HOME - Directory containing EK9 compiler JAR (for version switching)
        \tEK9_TARGET - Alternative to -T flag for target architecture
        \tEK9_COMPILER_MEMORY - JVM memory for compiler (default: -Xmx512m)
        \tEK9_APPLICATION_MEMORY - JVM memory for running compiled applications (default: -Xmx512m)
        """);
  }
}
