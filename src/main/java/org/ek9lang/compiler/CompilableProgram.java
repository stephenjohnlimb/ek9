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
import org.ek9lang.compiler.support.AggregateManipulator;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.INaming;
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

  private transient CompilationData compilationData;

  /**
   * When the built-in ek9 bootstrap module is parsed and processed, it will be added here.
   * This is so that basic built in types (which are immutable) can then be used within the compiler.
   * This provides quick and programmatic access - while it would be possible to resolve within the
   * normal scope hierarchy, this is quicker and more obvious within the compiler.
   */
  private Ek9Types ek9Types;

  /**
   * This is baked in at a code level for super of all things.
   */
  private IAggregateSymbol ek9Any;

  /**
   * For a specific source the ParsedModule is returned.
   */
  public ParsedModule getParsedModuleForCompilableSource(final CompilableSource source) {

    AssertValue.checkNotNull("Compilable source cannot be null", source);

    return this.sourceToParsedModule.get(source);
  }

  public Ek9Types getEk9Types() {

    return ek9Types;
  }

  public void setEk9Types(final Ek9Types ek9Types) {

    this.ek9Types = ek9Types;

  }

  public IAggregateSymbol getEk9Any() {

    return ek9Any;

  }

  public void setEk9Any(IAggregateSymbol ek9Any) {

    this.ek9Any = ek9Any;

  }

  public CompilationData getCompilationData() {

    return compilationData;
  }

  public void setCompilationData(final CompilationData compilationData) {

    this.compilationData = compilationData;

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
  public void add(final ParsedModule parsedModule) {

    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    AssertValue.checkNotNull("ParsedModule getModuleName cannot be null", parsedModule.getModuleName());

    final var parsedModules = getOrCreateParsedModules(parsedModule.getModuleName());
    parsedModules.add(parsedModule);
    sourceToParsedModule.put(parsedModule.getSource(), parsedModule);

  }

  private ParsedModules getOrCreateParsedModules(final String parsedModuleName) {

    return theParsedModules.computeIfAbsent(parsedModuleName, ParsedModules::new);
  }

  /**
   * If some source has been altered, then its corresponding parsedModule must be removed.
   * Once it has, then been parsed it can be added back in.
   *
   * @param parsedModule The existing parsed module to be removed.
   */
  public void remove(final ParsedModule parsedModule) {

    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);

    final var parsedModules = getOrCreateParsedModules(parsedModule.getModuleName());
    parsedModules.remove(parsedModule);
    sourceToParsedModule.remove(parsedModule.getSource());

  }

  /**
   * Provide read only access to the list of modules for a particular moduleName.
   * Returns an empty list of the module name cannot be located.
   */
  public List<ParsedModule> getParsedModules(String moduleName) {

    final var parsedModules = getOrCreateParsedModules(moduleName);

    return parsedModules.getParsedModules();
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

    final var moduleName = INaming.getModuleNameIfPresent(search.getName());

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

    final var moduleName = INaming.getModuleNameIfPresent(possibleGenericSymbol.getFullyQualifiedName());
    final var search = new SymbolSearch(possibleGenericSymbol);
    final var resolved = resolveFromModule(moduleName, search);

    if (resolved.isEmpty()) {
      //need to define it and return it.
      final var modules = getModuleScopesFunction().apply(moduleName);
      AssertValue.checkTrue("Modules cannot be empty", !modules.isEmpty());

      final var module = modules.get(0);
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

    final var moduleScopes = getModuleScopesFunction().apply(moduleName);

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
    final var name = search.getName();

    if (!INaming.isQualifiedName(name)) {
      final var resolved = resolveFromBuiltInModule(AggregateManipulator.EK9_LANG, search);
      if (resolved.isPresent()) {
        return resolved;
      }
      return resolveFromBuiltInModule(AggregateManipulator.EK9_MATH, search);
    } else {
      return Stream.of(AggregateManipulator.EK9_LANG, AggregateManipulator.EK9_MATH)
          .map(moduleName -> this.resolveFromModule(moduleName, search))
          .filter(Optional::isPresent).flatMap(Optional::stream).findFirst();
    }
  }

  private Optional<ISymbol> resolveFromBuiltInModule(final String moduleName, final SymbolSearch search) {

    final var name = search.getName();
    SymbolSearch newSearch = new SymbolSearch(INaming.makeFullyQualifiedName(moduleName, name), search);

    return resolveFromModule(moduleName, newSearch);
  }
}
