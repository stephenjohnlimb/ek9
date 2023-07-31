package org.ek9lang.cli;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Just increments a version number.
 */
final class Eiv extends Eve {
  /**
   * Rather than use a switch use a map of version vector name to a method call.
   */
  private final Map<String, Consumer<Version>> operation = Map.of(
      "major", Version::incrementMajor,
      "minor", Version::incrementMinor,
      "patch", Version::incrementPatch,
      "build", Version::incrementBuildNumber
  );

  Eiv(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Version+: ";
  }

  @Override
  protected boolean doRun() {
    //Need to get from command line.
    String partToIncrement = compilationContext.commandLine().getOptionParameter("-IV");
    Version versionNumber = Version.withBuildNumber(compilationContext.commandLine().getVersion());
    log("Processing increment " + versionNumber);
    //i.e. Find the key or produce a no-op, this is in place of a switch.
    operation.getOrDefault(partToIncrement, v -> {
    }).accept(versionNumber);

    return setVersionNewNumber(versionNumber);
  }
}
