package org.ek9lang.cli;

import java.util.List;
import org.ek9lang.core.Logger;

/**
 * Check and pull all dependencies in.
 * Now this does a full clean so that all existing artefacts/generated sources and
 * targets are removed.
 */
final class Edp extends E {

  Edp(final CompilationContext compilationContext) {

    super(compilationContext);

  }

  @Override
  protected String messagePrefix() {

    return "Resolve : ";
  }

  @Override
  protected boolean doRun() {

    log("- Clean");

    final boolean compileResult = new Ecl(compilationContext).run();
    if (compileResult) {
      if (compilationContext.commandLine().noPackageIsPresent()) {
        log("No Dependencies defined");
      }
      final var rootNode = new DependencyNodeFactory(compilationContext.commandLine(),
          compilationContext.muteReportedErrors()).createFrom(
          compilationContext.commandLine().getSourceVisitor());
      return rootNode.isPresent() && processDependencies(new DependencyManager(rootNode.get()));
    }

    return false;
  }

  private boolean processDependencies(final DependencyManager dependencyManager) {

    log("Analysing Dependencies");

    if (hasCircularDependencies(dependencyManager)) {
      return false;
    }

    processExclusions(dependencyManager);

    log("Version Promotions");
    dependencyManager.rationalise();

    optimise(dependencyManager);

    if (hasSemanticVersionBreaches(dependencyManager)) {
      return false;
    }

    if (compilationContext.commandLine().isVerbose()) {
      showAnalysisInformation(dependencyManager);
    }

    return true;
  }

  private boolean hasCircularDependencies(final DependencyManager dependencyManager) {

    final var circulars = dependencyManager.reportCircularDependencies(true);
    log("Circular Dependencies (" + circulars.size() + ")");

    if (circulars.isEmpty()) {
      return false;
    }
    circulars.forEach(this::report);

    return true;
  }

  private void processExclusions(final DependencyManager dependencyManager) {

    //They will all be accepted at the moment, we need to get all their exclusions and apply them
    final var allDependencies = dependencyManager.reportAcceptedDependencies();
    log("Exclusions (" + allDependencies.size() + ")");

    allDependencies.forEach(dep -> {

      final var rejections = dep.getDependencyRejections();
      rejections.keySet().forEach(moduleName -> {

        final var whenDependencyOf = rejections.get(moduleName);
        log("Exclusion '" + moduleName + "' <- '" + whenDependencyOf + "'");
        dependencyManager.reject(moduleName, whenDependencyOf);

      });

    });

  }

  private void optimise(final DependencyManager dependencyManager) {

    log("Optimising");

    int iterations = 0;
    boolean optimised;
    do {
      optimised = dependencyManager.optimise(iterations++);
      log("Optimisation (" + iterations + ")");
    } while (optimised);

  }

  private boolean hasSemanticVersionBreaches(final DependencyManager dependencyManager) {

    final var breaches = dependencyManager.reportStrictSemanticVersionBreaches();
    log("Version breaches (" + breaches.size() + ")");

    if (breaches.isEmpty()) {
      return false;
    }

    report("Semantic version breaches:");
    breaches.forEach(this::report);
    report("You must review/refine your dependencies");

    return true;
  }

  private void showAnalysisInformation(final DependencyManager dependencyManager) {

    showAnalysisDetails("Not Applied:", dependencyManager.reportRejectedDependencies());
    showAnalysisDetails("Applied:", dependencyManager.reportAcceptedDependencies());

  }

  private void showAnalysisDetails(final String application, final List<DependencyNode> list) {

    if (list.isEmpty()) {
      return;
    }

    final var builder = new StringBuilder(messagePrefix()).append(application);
    list.stream().map(node -> " '" + node + "'").forEach(builder::append);
    builder.append(".");
    Logger.error(builder);

  }
}
