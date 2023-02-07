package org.ek9lang.compiler.internals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ModuleScope;
import org.ek9lang.compiler.symbol.support.AggregateFactory;
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
  private final Map<String, ParsedModules> theParsedModules = new TreeMap<>();

  /**
   * It is important to maintain a quick mapping from source to parsed module.
   */
  private final Map<CompilableSource, ParsedModule> sourceToParsedModule = new HashMap<>();

  private final Function<String, List<ModuleScope>> getModuleScopes =
      moduleName -> Stream.ofNullable(theParsedModules.get(moduleName))
          .map(ParsedModules::getParsedModules)
          .flatMap(List::stream)
          .map(ParsedModule::getModuleScope)
          .toList();

  public CompilableProgram(List<ParsedModules> implicitBuiltInModules) {
    AssertValue.checkNotNull("ImplicitBuiltInModules cannot be null", implicitBuiltInModules);
    this.implicitBuiltInModules.addAll(implicitBuiltInModules);
  }

  public ParsedModule getParsedModuleForCompilableSource(CompilableSource source) {
    AssertValue.checkNotNull("Compilable source cannot be null", source);
    return this.sourceToParsedModule.get(source);
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

    sourceToParsedModule.put(parsedModule.getSource(), parsedModule);
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
      sourceToParsedModule.remove(parsedModule.getSource());
    }
  }

  /**
   * Provide read only access to the list of modules for a particular moduleName.
   */
  public List<ParsedModule> getParsedModules(String moduleName) {
    var modules = theParsedModules.get(moduleName);

    return modules != null ? modules.getParsedModules() : List.of();
  }

  public List<String> getParsedModuleNames() {
    return theParsedModules.keySet().stream().toList();
  }

  /**
   * Resolve some for of type via a fully qualified search.
   */
  public Optional<ISymbol> resolveByFullyQualifiedSearch(final SymbolSearch search) {
    var moduleName = ISymbol.getModuleNameIfPresent(search.getName());
    return resolveFromModule(moduleName, search);
  }

  /**
   * A package name (moduleName) can actually have multiple parsedModules
   * (i.e. a module can be made from multiple source files, each of which has its own parsed module).
   * But they are all part of the same namespace (package/module).
   * This method finds the appropriate set of parsed modules (if that module name exists) and
   * then checks each of the modules to see if the search can be resolved.
   * It returns the first resolution or Optional empty if it cannot be resolved.
   */
  public Optional<ISymbol> resolveFromModule(final String moduleName, final SymbolSearch search) {

    return getModuleScopes.apply(moduleName)
        .stream()
        .map(moduleScope -> moduleScope.resolveInThisScopeOnly(search))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }


  /**
   * Locates the token when the first reference was established.
   */
  public Optional<Token> getOriginalReferenceLocation(final String moduleName, final SymbolSearch search) {
    return getModuleScopes.apply(moduleName)
        .stream()
        .map(moduleScope -> moduleScope.getOriginalReferenceLocation(search))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }

  /**
   * Check the existing set of references in the moduleName.
   * So that means as there are multiple sources per module it is necessary to check in each.
   */
  public Optional<ISymbol> resolveReferenceFromModule(final String moduleName, final SymbolSearch search) {

    return getModuleScopes.apply(moduleName)
        .stream()
        .map(moduleScope -> moduleScope.resolveReferenceInThisModuleOnly(search))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }

  /**
   * Search in the implicit ek9 module namespaces of:
   * org.ek9.lang etc.
   */
  public Optional<ISymbol> resolveFromImplicitScopes(final SymbolSearch search) {
    //If the search name being presented is not fully qualified - which it probably won't be
    //We need to modify the search.
    var name = search.getName();
    if (!ISymbol.isQualifiedName(name)) {
      SymbolSearch newSearch =
          new SymbolSearch(ISymbol.makeFullyQualifiedName(AggregateFactory.EK9_LANG, name), search);
      var resolved = resolveFromModule(AggregateFactory.EK9_LANG, newSearch);
      if (resolved.isEmpty()) {
        newSearch = new SymbolSearch(ISymbol.makeFullyQualifiedName(AggregateFactory.EK9_MATH, name), search);
        resolved = resolveFromModule(AggregateFactory.EK9_MATH, newSearch);
      }
      return resolved;
    } else {
      return Stream.of(AggregateFactory.EK9_LANG, AggregateFactory.EK9_MATH)
          .map(moduleName -> this.resolveFromModule(moduleName, search))
          .filter(Optional::isPresent).flatMap(Optional::stream).findFirst();
    }
  }
}
