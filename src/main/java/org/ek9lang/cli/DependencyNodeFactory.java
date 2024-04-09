package org.ek9lang.cli;

import java.util.Map;
import java.util.Optional;
import org.ek9lang.compiler.common.Ek9SourceVisitor;
import org.ek9lang.compiler.common.Reporter;

/**
 * Just creates DependencyNodes by using a Source Visitor on some EK9 source that has a package
 * defined in it. Uses a package resolver to get the dependencies and that will unpack zips and
 * in the future pull them from remote servers can validate the contents.
 * There's a bit of recursion going on here.
 */
final class DependencyNodeFactory extends Reporter {
  private final CommandLineDetails commandLine;
  private final PackageResolver packageResolver;

  /**
   * Make a new Dependency Node Factory.
   */
  DependencyNodeFactory(final CommandLineDetails commandLine, final boolean muteReportedErrors) {

    super(commandLine.isVerbose(), muteReportedErrors);
    this.commandLine = commandLine;
    packageResolver = new PackageResolver(commandLine, muteReportedErrors);

  }

  @Override
  protected String messagePrefix() {

    return "Resolve : ";
  }

  Optional<DependencyNode> createFrom(final Ek9SourceVisitor visitor) {

    return createFrom(null, visitor);
  }

  private Optional<DependencyNode> createFrom(final DependencyNode parent, final Ek9SourceVisitor visitor) {

    final var details = visitor.getPackageDetails();
    if (details.isPresent()) {
      final var packageDetails = details.get();
      final var workingNode = new DependencyNode(packageDetails.moduleName(), packageDetails.version());

      log("Processing '" + workingNode + "'");

      if (parent != null) {
        parent.addDependency(workingNode);
        log("Added " + workingNode + " as dependency of " + parent);
        final var circulars = parent.reportCircularDependencies(true);
        if (circulars.isPresent()) {
          report("Circular dependency! '" + circulars.get() + "'");
          return Optional.empty();
        }
      }

      log("deps (" + packageDetails.deps().size() + ")");
      if (processDependencies(workingNode, packageDetails.deps())) {
        log("devDeps (" + packageDetails.devDeps().size() + ")");
        if (processDependencies(workingNode, packageDetails.devDeps())) {
          log("excludesDeps (" + packageDetails.excludeDeps().size() + ")");
          packageDetails.excludeDeps().forEach((key, value) -> {
            workingNode.addDependencyRejection(key, value);
            log("From '" + workingNode + "': excluding '" + key + "' when dependency of '"
                + value + "'");
          });
          return Optional.of(workingNode);
        }
      }
    }

    return Optional.empty();
  }

  private boolean processDependencies(final DependencyNode workingNode, final Map<String, String> deps) {

    for (final var entry : deps.entrySet()) {
      final var dependencyVector = commandLine.getFileHandling().makeDependencyVector(entry.getKey(), entry.getValue());
      log("Dependency '" + dependencyVector + "'");

      final var depVisitor = packageResolver.resolve(dependencyVector);
      if (depVisitor.isEmpty()) {
        return false;
      }

      //Build a recursive structure.
      if (createFrom(workingNode, depVisitor.get()).isEmpty()) {
        return false;
      }
    }

    return true;
  }
}
