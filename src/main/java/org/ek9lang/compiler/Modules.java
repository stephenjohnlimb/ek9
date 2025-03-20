package org.ek9lang.compiler;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * With EK9 it is possible to have multiple source files that are all in the same 'module'.
 * This object represents that concept. So the module name is always the same but there can and
 * will be multiple modules.
 */
public class Modules<T extends Module> implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The name of the module.
   */
  private final String moduleName;

  /**
   * The modules that make up the whole module namespace.
   */
  private final List<T> modulesInModule = new ArrayList<>();

  public Modules(final String moduleName) {

    AssertValue.checkNotNull("ModuleName cannot be null", moduleName);
    this.moduleName = moduleName;

  }

  /**
   * Add a module to the set all under the same moduleName.
   */
  public void add(final T module) {

    AssertValue.checkNotNull("Module cannot be null", module);

    if (!modulesInModule.contains(module)) {
      modulesInModule.add(module);
    } else {
      throw new CompilerException("Module already in list for " + moduleName);
    }

  }

  /**
   * Remove an existing  module for the set of modules recorded against a specific module name.
   */
  public void remove(final T module) {

    modulesInModule.remove(module);

  }

  /**
   * Provides an unmodifiable list of parsed modules.
   */
  public List<T> getModules() {

    return Collections.unmodifiableList(modulesInModule);
  }
}

