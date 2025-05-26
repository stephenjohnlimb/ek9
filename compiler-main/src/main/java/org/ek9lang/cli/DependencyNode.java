package org.ek9lang.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.ek9lang.core.SemanticVersion;

/**
 * Represents a single dependency by moduleName and version.
 */
final class DependencyNode {

  private final List<DependencyNode> dependencies = new ArrayList<>();
  //When loading up nodes these are the defined rejections that need to be applied.
  private final Map<String, String> dependencyRejections = new HashMap<>();
  private final String moduleName;
  private final SemanticVersion version;
  //Navigational graph structure.
  //Will remain null if this is the top node.
  private DependencyNode parent = null;
  private boolean rejected = false;
  private RejectionReason reason;

  DependencyNode(final String moduleName, final String version) {

    this.moduleName = moduleName;
    this.version = SemanticVersion.of(version);

  }

  /**
   * Create a node from a vector.
   *
   * @param dependencyVector - example a.b.c-3.8.3-feature32-90
   * @return A new dependency node.
   */
  static DependencyNode of(final String dependencyVector) {

    final var versionStart = dependencyVector.indexOf('-');
    final var moduleName = dependencyVector.substring(0, versionStart);
    final var versionPart = dependencyVector.substring(versionStart + 1);

    return new DependencyNode(moduleName, versionPart);
  }

  void addDependencyRejection(final String moduleName, final String whenDependencyOf) {

    this.dependencyRejections.put(moduleName, whenDependencyOf);

  }

  Map<String, String> getDependencyRejections() {

    return dependencyRejections;
  }

  void addDependency(final DependencyNode node) {

    node.setParent(this);
    dependencies.add(node);

  }

  /**
   * Check if a dependency.
   */
  boolean isDependencyOf(final String whenDependencyOf) {

    var d = parent;
    while (d != null) {
      if (d.getModuleName().equals(whenDependencyOf)) {
        return true;
      }
      d = d.getParent();
    }

    return false;
  }

  /**
   * Get a list of all the dependencies.
   */
  List<String> reportAllDependencies() {

    final List<String> rtn = new ArrayList<>();
    rtn.add(getModuleName());
    dependencies.forEach(dep -> rtn.addAll(dep.reportAllDependencies()));

    return rtn;
  }

  /**
   * Find the first circular dependency.
   */
  Optional<String> reportCircularDependencies(final boolean includeVersion) {

    var d = parent;
    while (d != null) {
      if (d.isSameModule(this)) {
        //Found a circular dependency
        return Optional.of(showPathToDependency(includeVersion));
      }
      d = d.getParent();
    }

    return Optional.empty();
  }

  /**
   * Show the path through other dependencies to this dependency.
   */
  String showPathToDependency(final boolean includeVersion) {

    final var backTrace = new StringBuilder(toString(includeVersion));

    var d = parent;
    while (d != null) {
      backTrace.insert(0, " ~> ");
      backTrace.insert(0, d.toString(includeVersion));
      d = d.getParent();
    }

    return backTrace.toString();
  }

  boolean isParentRejected() {

    return parent != null && parent.isRejected();
  }

  boolean isSameModule(final DependencyNode node) {

    return this.moduleName.equals(node.moduleName);
  }

  boolean isRejected() {

    return rejected;
  }

  /**
   * Reject this dependency and optionally reject any dependencies it pulled in.
   */
  void setRejected(final RejectionReason reason,
                   final boolean rejected,
                   final boolean alsoRejectDependencies) {

    this.rejected = rejected;
    this.reason = reason;
    if (alsoRejectDependencies) {
      dependencies.forEach(dep -> dep.setRejected(reason, rejected, true));
    }

  }

  DependencyNode getParent() {

    return parent;
  }

  void setParent(DependencyNode parent) {

    this.parent = parent;
  }

  List<DependencyNode> getDependencies() {

    return dependencies;
  }

  String getModuleName() {

    return moduleName;
  }

  SemanticVersion getVersion() {

    return version;
  }

  @Override
  public int hashCode() {

    return getModuleAndVersion().hashCode();
  }

  @Override
  public boolean equals(final Object obj) {

    var rtn = false;
    if (obj instanceof DependencyNode dep) {
      rtn = getModuleAndVersion().equals(dep.getModuleAndVersion());
    }

    return rtn;
  }

  @Override
  public String toString() {

    final var builder = new StringBuilder(getModuleAndVersion());
    if (rejected) {
      builder.append(getReason());
    }

    return builder.toString();
  }

  private String toString(final boolean includeVersion) {

    if (includeVersion) {
      return toString();
    }

    return rejected ? moduleName + getReason() : moduleName;
  }

  private String getModuleAndVersion() {

    return String.format("%s-%s", moduleName, version);
  }

  private String getReason() {

    return String.format("(%s)", reason);
  }

  /**
   * Why was a dependency rejected.
   */
  public enum RejectionReason {

    /**
     * The developer configured this dependency to be rejected in the package directive.
     */
    MANUAL,

    /**
     * When the Dependency manager resolved all the dependencies did it find a later version
     * And resolve this version of the dependency away.
     */
    RATIONALISATION,

    /**
     * If we find the same module and also the same version then we have it.
     */
    SAME_VERSION,

    /**
     * After rationalisation did the Dependency manager then workout that actually this
     * dependency is not needed at any version number, this can happen if lower version
     * numbered dependencies pull in other dependencies, but then the lower version
     * numbered dependency gets rationalised away, leaving a trail of stuff it pulled in
     * that is now no longer needed. You can just reject the dependencies and all it's
     * dependencies directly as those dependencies might be used elsewhere.
     */
    OPTIMISED

    //But note is something that was rationalised/optimised out did bring in a dependency
    //that it at a higher level than one that is still needed elsewhere then we will use that
    //in preference.
  }
}
