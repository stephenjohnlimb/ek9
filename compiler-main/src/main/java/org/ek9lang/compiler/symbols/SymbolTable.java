package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.SymbolMatcher;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * We need to support simple things like classes which are unique per symbol table
 * Or the fully qualified class names like 'com.some.A and com.other.A' so they won't collide.
 * But we don't want to define constants/variables like 'int A and float A' this is a duplicate
 * We do want method overloading like int A() and int A(String some param)
 * But not int A() and float A() as that is not good.
 * <pre>
 * So for the most part this is straight forward, but this issue is operators and type matching.
 * i.e support for
 * 1. int A()
 * 2. int A(String v1)
 * 3. int A(String v1, int someItem)
 * 4. int A(int item, String other)
 * 5. int A(List of String list1)
 * 6. int A(List of Integer list2)
 * 7. float A(List of Integer list3, String other)
 * 8. int A(List of Double list4, String other)
 * 9. int A(SomeClass sc)
 * 10. int A(ClassThatExtendsSomeClass supersc)
 * </pre>
 * When we define these we want them all to be defined and available in the appropriate scope.
 * The issue is doing the resolve! We can't just say hey I'm trying to resolve 'A' that's just
 * not enough
 * We need to know:
 * <pre>
 * 1. Are we looking for a class(type), function/method or variable/constant in this context.
 * 2. What is it's name - in this case 'A'
 * 3. What type (or return type) are we expecting - but here we should accept a super class match
 * with some weight match value
 * 4. What is the order and type of parameters - again we need to try and match with some weight
 * using super class matches.
 * </pre>
 * So what sort of algorithm can we use for this?
 * <pre>
 * The Symbol has a type already when we define it - we know if it is an AggregateSymbol,
 * MethodSymbol etc. We also know its name!
 * We know the Symbol we will use for it's return type or what it is i.e the Symbol of Integer as
 * the return type or what it is.
 * For methods (which are Scoped Symbols) we also have the parameters (in order).
 * So while parameters can be used when making the call this is just sugar - the order still has
 * to match.
 *
 * Well we could have separate internal tables for methods/functions, classes/types and variables.
 * So we can quickly and simply resolve obvious ones like variables and types.
 * Then use more expensive operations for methods.
 *
 * Now for methods we can store in a hash map of method names - so in a simple case with just one
 * method of name A we're done!
 * But in that hashmap we store and List of all methods of that name - now we have the costly
 * activity of going through each and getting the weight of each to find a match. in local scopes
 * which ever is the best match we return - but clearly it is possible there is no match then we
 * resort to moving back up the scope tree (as we do now).
 * </pre>
 */
public class SymbolTable implements IScope {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * We now store the symbols in separate areas for quick access.
   * It might seem strange but for some symbols like methods we have a single name
   * but a list of actual symbols. i.e. method overloading.
   */
  private final Map<SymbolCategory, Map<String, List<ISymbol>>> splitSymbols =
      new EnumMap<>(SymbolCategory.class);

  /**
   * But also keep an ordered list - useful for ordered parameters.
   */
  private final List<ISymbol> orderedSymbols = new ArrayList<>();
  private final SymbolMatcher matcher = new SymbolMatcher();

  private String scopeName = "global";

  /**
   * Is this scope marked as pure, so that mutations cannot be undertaken.
   */
  private boolean markedPure = false;

  /**
   * If we encounter an exception within a scope we need to note the line number.
   */
  private IToken encounteredExceptionToken = null;

  public SymbolTable(String scopeName) {
    this.scopeName = scopeName;
  }

  public SymbolTable() {
  }

  @Override
  public IScope getEnclosingScope() {
    return null;
  }

  @Override
  public String getFriendlyScopeName() {
    return getScopeName();
  }

  @Override
  public String getScopeName() {
    return scopeName;
  }

  @Override
  public ScopeType getScopeType() {
    return ScopeType.BLOCK;
  }

  @Override
  public boolean isTerminatedNormally() {
    return getEncounteredExceptionToken() == null;
  }

  @Override
  public IToken getEncounteredExceptionToken() {
    return encounteredExceptionToken;
  }

  @Override
  public void setEncounteredExceptionToken(final IToken encounteredExceptionToken) {
    this.encounteredExceptionToken = encounteredExceptionToken;
  }

  @Override
  public SymbolTable clone(final IScope withParentAsAppropriate) {
    return cloneIntoSymbolTable(new SymbolTable(this.getScopeName()), withParentAsAppropriate);
  }

  protected SymbolTable cloneIntoSymbolTable(final SymbolTable rtn,
                                             final IScope withParentAsAppropriate) {
    rtn.scopeName = this.scopeName;
    rtn.markedPure = this.markedPure;
    orderedSymbols.forEach(symbol -> rtn.define(symbol.clone(withParentAsAppropriate)));

    return rtn;
  }

  public boolean isMarkedPure() {
    return markedPure;
  }

  public void setMarkedPure(final boolean markedPure) {
    this.markedPure = markedPure;
  }

  @Override
  public void define(final ISymbol symbol) {
    //Add in the split symbols and also the ordered symbols.
    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    addToSplitSymbols(symbol);
    orderedSymbols.add(symbol);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof SymbolTable that)
        && isMarkedPure() == that.isMarkedPure()
        && orderedSymbols.equals(that.orderedSymbols)
        && getScopeName().equals(that.getScopeName());
  }

  @Override
  public int hashCode() {
    int result = orderedSymbols.hashCode();
    result = 31 * result + scopeName.hashCode();
    result = 31 * result + (isMarkedPure() ? 1 : 0);
    return result;
  }

  /**
   * So we have a split map for each category
   * each of those has a map with a list of symbols of the same name.
   * So this means we can handle multiple methods with same name but different signatures.
   */
  @SuppressWarnings("checkstyle:LambdaParameterName")
  private void addToSplitSymbols(final ISymbol symbol) {
    Map<String, List<ISymbol>> table =
        splitSymbols.computeIfAbsent(symbol.getCategory(), _ -> new HashMap<>());
    List<ISymbol> list = table.computeIfAbsent(symbol.getName(), _ -> new ArrayList<>());
    if (!symbol.getCategory().equals(SymbolCategory.METHOD) && list.contains(symbol)) {
      throw new CompilerException(
          "Compiler Coding Error - Duplicate symbol [" + symbol + "] try to add to [" + this.scopeName + "]");
    }
    list.add(symbol);
  }

  /**
   * Find symbols in a specific category.
   */
  public List<ISymbol> getSymbolsForThisScopeOfCategory(final SymbolCategory category) {
    return orderedSymbols.stream().filter(symbol -> category.equals(symbol.getCategory())).toList();
  }

  /**
   * Get all the symbols in this table.
   */
  @Override
  public List<ISymbol> getSymbolsForThisScope() {
    return Collections.unmodifiableList(orderedSymbols);
  }

  @Override
  public List<ISymbol> getAllSymbolsMatchingName(final String symbolName) {
    return orderedSymbols.stream().filter(symbol -> symbol.getName().equals(symbolName)).toList();
  }

  /**
   * Search and resolve from a symbol search.
   */
  public Optional<ISymbol> resolve(final SymbolSearch search) {
    final var rtn = resolveInThisScopeOnly(search);
    return rtn.isPresent() ? rtn : resolveWithEnclosingScope(search);
  }

  /**
   * Add all matching methods for a method search.
   */
  @Override
  public MethodSymbolSearchResult resolveMatchingMethods(final MethodSymbolSearch search,
                                                         MethodSymbolSearchResult result) {
    MethodSymbolSearchResult buildResult = new MethodSymbolSearchResult(result);

    //Do our enclosing scope first then this scope.
    buildResult = buildResult.mergePeerToNewResult(
        resolveMatchingMethodsInEnclosingScope(search, new MethodSymbolSearchResult()));

    //So override results with anything from this scope
    buildResult = buildResult.overrideToNewResult(
        resolveMatchingMethodsInThisScopeOnly(search, new MethodSymbolSearchResult()));

    return buildResult;
  }

  /**
   * Add all matching methods for a method search but only in this scope.
   */
  @Override
  public MethodSymbolSearchResult resolveMatchingMethodsInThisScopeOnly(
      final MethodSymbolSearch search, MethodSymbolSearchResult result) {

    final Function<List<ISymbol>, List<MethodSymbol>> methodSymbolCast = list -> list
        .stream()
        .filter(MethodSymbol.class::isInstance)
        .map(MethodSymbol.class::cast).toList();

    final var methods = Optional.of(getSplitSymbolTable(SymbolCategory.METHOD))
        .stream()
        .map(table -> getSymbolByName(table, search.getName()))
        .map(methodSymbolCast)
        .toList();

    //Left like this to aid debugging.
    methods.forEach(methodList -> matcher.addMatchesToResult(result, search, methodList));

    return result;
  }

  /**
   * There are no supers so this will not add any methods.
   */
  protected MethodSymbolSearchResult resolveMatchingMethodsInEnclosingScope(
      final MethodSymbolSearch search, MethodSymbolSearchResult result) {

    //nothing needed here.
    return result;
  }

  @Override
  public Optional<ISymbol> resolveMember(SymbolSearch search) {
    return this.resolveInThisScopeOnly(search);
  }

  private Optional<ISymbol> resolveFromSplitSymbolTable(final String symbolName,
                                                        final SymbolSearch search) {
    final var category = search.getSearchType();
    AssertValue.checkNotNull("Search type must be explicit", category);
    return resolveInThisScopeOnly(getSplitSymbolTable(category), symbolName, search);
  }

  /**
   * If search is unqualified (i.e. just a name then yes we look in this scope).
   * If the search is a fully qualified name then the scope name in the search has to match
   * this scope.
   */
  protected boolean searchIsNotInThisScope(final SymbolSearch search) {
    String symbolName = search.getName();
    if (INaming.isQualifiedName(symbolName)) {
      return !getScopeName().equals(INaming.getModuleNameIfPresent(symbolName));
    }
    return false;
  }

  /**
   * Resolve a symbol in this symbol table only.
   */
  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(final SymbolSearch search) {
    AssertValue.checkNotNull("Search cannot be null", search);

    if (searchIsNotInThisScope(search)) {
      return Optional.empty();
    }

    String searchName = INaming.getUnqualifiedName(search.getName());

    //So search the valid categories.
    final var searchTypes = search.getValidSearchTypes();
    return searchTypes.stream()
        .map(category -> resolveFromSplitSymbolTable(searchName,
            new SymbolSearch(search).setSearchType(category)))
        .filter(Optional::isPresent)
        .findFirst().orElse(Optional.empty());
  }

  private Optional<ISymbol> resolveInThisScopeOnly(final Map<String, List<ISymbol>> table,
                                                   final String shortSymbolName,
                                                   final SymbolSearch search) {
    return Optional.of(table)
        .stream()
        .map(t -> getSymbolByName(t, shortSymbolName))
        .map(symbolList -> resolveInThisScopeOnly(symbolList, search))
        .filter(Optional::isPresent)
        .findAny()
        .orElse(Optional.empty());
  }

  /**
   * This is really the backbone of the symbol table and pretty much the compiler.
   * Resolving a symbol of a specific type using the symbol search criteria.
   */
  private Optional<ISymbol> resolveInThisScopeOnly(final List<ISymbol> symbolList,
                                                   final SymbolSearch search) {
    if (symbolList.isEmpty()) {
      return Optional.empty();
    }

    // I've pulled out common code to in-method functions, so I can capture incoming parameters
    // and re-use the code.

    final var searchType = search.getSearchType();

    return switch (searchType) {
      case METHOD -> byMethod(symbolList, search);
      case FUNCTION, TEMPLATE_TYPE, TEMPLATE_FUNCTION, ANY -> byFirstInList(symbolList);
      case TYPE -> byType(symbolList, search);
      case VARIABLE -> byVariable(symbolList, search);
      case CONTROL -> Optional.empty();
    };
  }

  private Optional<ISymbol> byFirstInList(final List<ISymbol> symbolList) {

    return getCheckAndSelectFirstItem().apply(symbolList);
  }

  private Optional<ISymbol> byVariable(final List<ISymbol> symbolList,
                                       final SymbolSearch search) {

    final var canVariableTypeBeAssigned = getCanVariableTypeBeAssigned(search, getIsAssignable());

    var checkVariable = getCheckAndSelectFirstItem().apply(symbolList);
    var foundType = checkVariable.stream().map(ISymbol::getType).findFirst().orElse(Optional.empty());
    return canVariableTypeBeAssigned.test(foundType) ? checkVariable : Optional.empty();
  }

  private Optional<ISymbol> byMethod(final List<ISymbol> symbolList,
                                     final SymbolSearch search) {
    final Supplier<MethodSymbolSearchResult> findMethod = getFindMethod(symbolList, search);
    var result = findMethod.get();
    if (result.getSingleBestMatchSymbol().isPresent()) {
      return Optional.of(result.getSingleBestMatchSymbol().get());
    }
    return Optional.empty();
  }

  private Optional<ISymbol> byType(final List<ISymbol> symbolList,
                                   final SymbolSearch search) {

    final var canBeAssigned = getCanBeAssigned(search, getIsAssignable());

    var checkType = getCheckAndSelectFirstItem().apply(symbolList);
    return canBeAssigned.test(checkType) ? checkType : Optional.empty();
  }


  private Function<List<ISymbol>, Optional<ISymbol>> getCheckAndSelectFirstItem() {
    return symbols -> {
      AssertValue.checkRange("Expecting a Single result in the symbol table", symbols.size(), 1, 1);
      return Optional.of(symbols.getFirst());
    };
  }

  private BiPredicate<Optional<ISymbol>, Optional<ISymbol>> getIsAssignable() {
    return (to, from) -> from
        .stream()
        .map(toSet -> toSet.isAssignableTo(to))
        .findFirst().orElse(false);
  }

  private Predicate<Optional<ISymbol>> getCanBeAssigned(final SymbolSearch search,
                                                        final BiPredicate<Optional<ISymbol>,
                                                            Optional<ISymbol>> isAssignable) {
    return toCheck -> {
      final Optional<ISymbol> searchSymbol = search.getAsSymbol();
      return isAssignable.test(toCheck, searchSymbol);
    };
  }

  private Predicate<Optional<ISymbol>> getCanVariableTypeBeAssigned(final SymbolSearch search,
                                                                    final BiPredicate<Optional<ISymbol>,
                                                                        Optional<ISymbol>> isAssignable) {
    return foundType -> {
      final Optional<ISymbol> toReceive = search.getOfTypeOrReturn();
      //We do consider this acceptable as the search has not indicated a specific type required.
      return toReceive.isEmpty() || isAssignable.test(toReceive, foundType);
    };
  }

  private Supplier<MethodSymbolSearchResult> getFindMethod(final List<ISymbol> symbolList, final SymbolSearch search) {
    return () -> {
      MethodSymbolSearchResult result = new MethodSymbolSearchResult();
      Optional.of(symbolList).stream().parallel()
          .map(getSymbolMethodCast())
          .forEach(methodList -> matcher.addMatchesToResult(result, search, methodList));
      return result;
    };
  }

  private Function<List<ISymbol>, List<MethodSymbol>> getSymbolMethodCast() {
    return list -> list
        .stream()
        .filter(MethodSymbol.class::isInstance)
        .map(MethodSymbol.class::cast).toList();
  }

  /**
   * This class is the root and so there is no enclosing scope.
   * subclasses will override to provide the scope that encloses them
   */
  public Optional<ISymbol> resolveWithEnclosingScope(final SymbolSearch search) {
    AssertValue.checkNotNull("Search must not be null", search);
    return Optional.empty();
  }

  @Override
  public boolean isScopeAMatchForEnclosingScope(IScope toCheck) {
    return false;
  }

  @Override
  public Optional<ScopedSymbol> findNearestNonBlockScopeInEnclosingScopes() {
    return Optional.empty();
  }

  @Override
  public Optional<ScopedSymbol> findNearestDynamicBlockScopeInEnclosingScopes() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return scopeName;
  }

  /**
   * Just a wrapper to make null safe.
   */
  private Map<String, List<ISymbol>> getSplitSymbolTable(final SymbolCategory category) {
    var table = splitSymbols.get(category);
    return table != null ? table : Map.of();
  }

  /**
   * Just a wrapper to make null safe.
   */
  private List<ISymbol> getSymbolByName(final Map<String, List<ISymbol>> table, final String name) {
    var list = table.get(name);
    return list != null ? list : List.of();
  }
}