package org.ek9lang.compiler.internals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * Represents the whole program(s) then developer is attempting to create.
 * This is the main route in and holds global symbol tables and references to the compilable source code
 * and the resulting parse trees.
 * So access to this object needs to be tightly controlled. This should be done via the SharedThreadContext object.
 * This also acts as a scope to enable global symbols to be defined and resolved (across parsed modules if needs be).
 * No longer using a global symbol table, but will use 'implicit' modules a bit like java does.
 * So, will use module name 'org.ek9.lang' as the main implicit module to define build in types/symbols.
 */
public class CompilableProgram {

  /**
   * This will be populated with all the standard built-in EK9 symbols and types.
   * But we accept a list of modules as we may wish to add more in the future but in other namespaces.
   * All developer created types will be in their own modules/symbol tables.
   */
  private final List<ParsedModules> implicitBuiltInModules = new ArrayList<>();

  /**
   * For developer defined modules. Quick access via module name into the parsedModules that are recorded.
   */
  private final HashMap<String, ParsedModules> theParsedModules = new HashMap<>();

  public CompilableProgram(List<ParsedModules> implicitBuiltInModules) {
    AssertValue.checkNotNull("ImplicitBuiltInModules cannot be null", implicitBuiltInModules);
    this.implicitBuiltInModules.addAll(implicitBuiltInModules);
  }

  /**
   * Add in a newly parsed module to the list of parsed modules.
   * Classify in the same set under the same module name.
   *
   * @param parsedModule The new parsed module to add in.
   */
  public void add(ParsedModule parsedModule) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    AssertValue.checkNotNull("ParsedModule getModuleName cannot be null", parsedModule.getModuleName());

    ParsedModules list = theParsedModules.get(parsedModule.getModuleName());

    //check and then need to add a new one
    if (list == null) {
      list = new ParsedModules(parsedModule.getModuleName());
      theParsedModules.put(parsedModule.getModuleName(), list);
    }

    //Will be checked for duplication.
    list.add(parsedModule);
  }

  /**
   * If some source has been altered, then its corresponding parsedModule must be removed.
   * Once it has then been re-parsed it can be added back in.
   *
   * @param parsedModule The existing parsed module to be removed.
   */
  public void remove(ParsedModule parsedModule) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    ParsedModules list = theParsedModules.get(parsedModule.getModuleName());
    if (list != null) {
      list.remove(parsedModule);
    }
  }

  /**
   * Provide read only access to the list of modules for a particular moduleName.
   */
  public List<ParsedModule> getParsedModules(String moduleName) {
    var modules =  theParsedModules.get(moduleName);

    return modules != null ? modules.getParsedModules() : List.of();
  }

  /**
   * TODO write java doc on what this does and why.
   */
  public Optional<ISymbol> resolveFromModule(String moduleName, SymbolSearch search) {
    //TODO implement
    return Optional.empty();
  }

  /**
   * Attempt to resolve the symbol for the search.
   * If this is a type search then first check is in concrete types. If not found then
   * The main set of implicit scopes are searched.
   *
   * @param search The search to try and locate the symbol for.
   * @return An empty option or the resolved symbol.
   */
  public Optional<ISymbol> resolveFromImplicitScopes(SymbolSearch search) {
    //TODO implement
    return Optional.empty();
  }
}
