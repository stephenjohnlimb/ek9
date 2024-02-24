package org.ek9lang.cli;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.ek9lang.core.Logger;

/**
 * Check and pull all dependencies in.
 * Now this does a full clean so that all existing artefacts/generated sources and
 * targets are removed.
 */
final class Edp extends E {

  Edp(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Resolve : ";
  }

  @Override
  protected boolean doRun() {
    log("- Clean");

    boolean rtn = new Ecl(compilationContext).run();
    if (rtn) {
      if (compilationContext.commandLine().noPackageIsPresent()) {
        log("No Dependencies defined");
      }
      Optional<DependencyNode> rootNode =
          new DependencyNodeFactory(compilationContext.commandLine(),
              compilationContext.muteReportedErrors()).createFrom(
              compilationContext.commandLine().getSourceVisitor());
      rtn = rootNode.isPresent() && processDependencies(new DependencyManager(rootNode.get()));
    }
    return rtn;
  }

  private boolean processDependencies(DependencyManager dependencyManager) {
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

  private boolean hasCircularDependencies(DependencyManager dependencyManager) {
    List<String> circulars = dependencyManager.reportCircularDependencies(true);

    log("Circular Dependencies (" + circulars.size() + ")");

    if (circulars.isEmpty()) {
      return false;
    }

    circulars.forEach(this::report);
    return true;
  }

  private void processExclusions(DependencyManager dependencyManager) {
    //They will all be accepted at the moment, we need to get all their exclusions and apply them
    List<DependencyNode> allDependencies = dependencyManager.reportAcceptedDependencies();
    log("Exclusions (" + allDependencies.size() + ")");
    allDependencies.forEach(dep -> {
      Map<String, String> rejections = dep.getDependencyRejections();
      rejections.keySet().forEach(moduleName -> {
        String whenDependencyOf = rejections.get(moduleName);
        log("Exclusion '" + moduleName + "' <- '" + whenDependencyOf + "'");
        dependencyManager.reject(moduleName, whenDependencyOf);
      });
    });
  }

  private void optimise(DependencyManager dependencyManager) {
    log("Optimising");

    int iterations = 0;
    boolean optimised;
    do {
      optimised = dependencyManager.optimise(iterations++);
      log("Optimisation (" + iterations + ")");
    } while (optimised);
  }

  private boolean hasSemanticVersionBreaches(DependencyManager dependencyManager) {
    List<DependencyNode> breaches = dependencyManager.reportStrictSemanticVersionBreaches();
    log("Version breaches (" + breaches.size() + ")");

    if (breaches.isEmpty()) {
      return false;
    }

    report("Semantic version breaches:");
    breaches.forEach(this::report);
    report("You must review/refine your dependencies");
    return true;
  }

  private void showAnalysisInformation(DependencyManager dependencyManager) {
    showAnalysisDetails("Not Applied:", dependencyManager.reportRejectedDependencies());
    showAnalysisDetails("Applied:", dependencyManager.reportAcceptedDependencies());
  }

  private void showAnalysisDetails(String application, List<DependencyNode> list) {
    if (list.isEmpty()) {
      return;
    }

    var builder = new StringBuilder(messagePrefix()).append(application);
    list.stream().map(node -> " '" + node + "'").forEach(builder::append);
    builder.append(".");
    Logger.error(builder);
  }
}
