package org.ek9lang.compiler.internals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;

/**
 * With EK9 it is possible to have multiple source files that are all in the same 'module'.
 * This object represents that concept. So the module name is always the same but there can and
 * will be multiple parsed modules.
 */
public class ParsedModules {
  /**
   * The name of the module.
   */
  private final String moduleName;

  /**
   * The parsedModules that make up the whole module namespace.
   */
  private final List<ParsedModule> parsedModulesInModule = new ArrayList<>();

  public ParsedModules(String moduleName) {
    AssertValue.checkNotNull("ModuleName cannot be null", moduleName);
    this.moduleName = moduleName;
  }

  /**
   * Add a parsed module to the set all under the same moduleName.
   */
  public void add(ParsedModule parsedModule) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);

    if (!parsedModulesInModule.contains(parsedModule)) {
      parsedModulesInModule.add(parsedModule);
    } else {
      throw new CompilerException("Parsed module already in list for " + moduleName);
    }
  }

  /**
   * Remove an existing parsed module for the set of modules recorded against a specific module name.
   */
  public void remove(ParsedModule parsedModule) {
    parsedModulesInModule.remove(parsedModule);
  }

  /**
   * Provides an unmodifiable list of parsed modules.
   */
  public List<ParsedModule> getParsedModules() {
    return Collections.unmodifiableList(parsedModulesInModule);
  }
}

