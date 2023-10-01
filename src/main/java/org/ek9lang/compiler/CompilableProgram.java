package org.ek9lang.compiler;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.ModuleScope;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Represents the whole program(s) that the developer is attempting to create.
 * This is the main route in and holds module symbol tables and references to the compilable source code
 * and the resulting parse trees.
 * So access to this object needs to be tightly controlled. This should be done via the SharedThreadContext object.
 * This also acts as a scope to enable symbols to be defined and resolved (across parsed modules if needs be).
 * No longer using a global symbol table, but will use 'implicit' modules a bit like java does.
 * So, will use module name 'org.ek9.lang' as the main implicit module to define build in types/symbols.
 */
public class CompilableProgram implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * For developer defined modules. Quick access via module name into the parsedModules that are recorded.
   */
  private final Map<String, ParsedModules> theParsedModules = new TreeMap<>();

  /**
   * It is important to maintain a quick mapping from source to parsed module.
   */
  private final Map<CompilableSource, ParsedModule> sourceToParsedModule = new HashMap<>();

  /**
   * Provides the list of scopes in a single module for a module name.
   */
  private transient Function<String, List<ModuleScope>> getModuleScopes;

  /**
   * When the built-in ek9 bootstrap module is parsed and processed, it will be added here.
   * This is so that basic built in types (which are immutable) can then be used within the compiler.
   * This provides quick and programmatic access - while it would be possible to resolve within the
   * normal scope hierarchy, this is quicker and more obvious within the compiler.
   */
  private Ek9Types ek9Types;

  /**
   * For a specific source the ParsedModule is returned.
   */
  public ParsedModule getParsedModuleForCompilableSource(CompilableSource source) {
    AssertValue.checkNotNull("Compilable source cannot be null", source);
    return this.sourceToParsedModule.get(source);
  }

  public Ek9Types getEk9Types() {
    return ek9Types;
  }

  public void setEk9Types(Ek9Types ek9Types) {
    this.ek9Types = ek9Types;
  }

  private Function<String, List<ModuleScope>> getModuleScopesFunction() {
    if (getModuleScopes == null) {
      getModuleScopes = moduleName -> Stream.ofNullable(theParsedModules.get(moduleName))
          .map(ParsedModules::getParsedModules)
          .flatMap(List::stream)
          .map(ParsedModule::getModuleScope)
          .toList();
    }
    return getModuleScopes;
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
   * Once it has, then been parsed it can be added back in.
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
   * Returns an empty list of the module name cannot be located.
   */
  public List<ParsedModule> getParsedModules(String moduleName) {
    var modules = theParsedModules.get(moduleName);

    return modules != null ? modules.getParsedModules() : List.of();
  }

  /**
   * Provides a list of all the module names in the program.
   * This can be very long.
   */
  public List<String> getParsedModuleNames() {
    return theParsedModules.keySet().stream().toList();
  }

  /**
   * Resolve some symbol via a fully qualified search.
   */
  public Optional<ISymbol> resolveByFullyQualifiedSearch(final SymbolSearch search) {
    var moduleName = ISymbol.getModuleNameIfPresent(search.getName());
    return resolveFromModule(moduleName, search);
  }

  /**
   * When using generic types, the parameterised type goes into the same module namespace as the
   * Generic Type. i.e. org.ek9.lang::List of some.mod.area::Thing would be given a fully qualified name
   * like org.ek9.lang::_List_SOMEDIGEST_OF_FULLY_QULIFIED_NAMES.
   * So once a List of String has been defined it can be used everywhere as it will be in the
   * org.ek9.lang module.
   */
  public ResolvedOrDefineResult resolveOrDefine(final PossibleGenericSymbol possibleGenericSymbol) {
    var moduleName = ISymbol.getModuleNameIfPresent(possibleGenericSymbol.getFullyQualifiedName());
    var search = new SymbolSearch(possibleGenericSymbol);

    var resolved = resolveFromModule(moduleName, search);
    if (resolved.isEmpty()) {
      //need to define it and return it.
      var module = getModuleScopesFunction().apply(moduleName).get(0);
      module.define(possibleGenericSymbol);

      return new ResolvedOrDefineResult(Optional.of(possibleGenericSymbol), true);
    }
    return new ResolvedOrDefineResult(Optional.of((PossibleGenericSymbol) resolved.get()), false);
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

    var moduleScopes = getModuleScopesFunction().apply(moduleName);
    return moduleScopes
        .stream()
        .map(moduleScope -> moduleScope.resolveInThisScopeOnly(search))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }


  /**
   * Locates the token when the first reference was established.
   */
  public Optional<IToken> getOriginalReferenceLocation(final String moduleName, final SymbolSearch search) {
    return getModuleScopesFunction().apply(moduleName)
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

    return getModuleScopesFunction().apply(moduleName)
        .stream()
        .map(moduleScope -> moduleScope.resolveReferenceInThisScopeOnly(search))
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
