package org.ek9lang.compiler.symbol.support;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.ScopedSymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;

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
  /**
   * We now store the symbols in separate areas for quick access.
   * It might seem strange but for some symbols like methods we have a single name
   * but a list of actual symbols. i.e. method overloading.
   */
  private final Map<ISymbol.SymbolCategory, Map<String, List<ISymbol>>> splitSymbols =
      new EnumMap<>(ISymbol.SymbolCategory.class);
  /**
   * But also keep an ordered list - useful for ordered parameters.
   */
  private final List<ISymbol> orderedSymbols = new ArrayList<>();
  private final SymbolMatcher matcher = new SymbolMatcher();
  /**
   * Function to work with MethodSymbols here rather than just ISymbols.
   */
  private final Function<List<ISymbol>, List<MethodSymbol>> methodSymbolCast = list -> list
      .stream()
      .filter(MethodSymbol.class::isInstance)
      .map(MethodSymbol.class::cast).toList();
  /**
   * Function to check there is one and only one item in a symbol list and then to return that item.
   */
  private final Function<List<ISymbol>, Optional<ISymbol>> checkAndSelectFirstItem = symbols -> {
    AssertValue.checkRange("Expecting a Single result in the symbol table", symbols.size(), 1, 1);
    return Optional.of(symbols.get(0));
  };
  /**
   * Simple BI Function to see if two ISymbols contained within Optionals are assignable.
   * i.e. is the 'from' assignable to the 'to'?
   */
  private final BiPredicate<Optional<ISymbol>, Optional<ISymbol>> isAssignable =
      (to, from) -> from
          .stream()
          .map(toSet -> toSet.isAssignableTo(to))
          .findFirst().orElse(false);

  private String scopeName = "global";

  /**
   * Is this scope marked as pure, so that mutations cannot be undertaken.
   */
  private boolean markedPure = false;
  /**
   * If we encounter an exception within a scope we need to note the line number.
   */
  private Token encounteredExceptionToken = null;

  public SymbolTable(String scopeName) {
    this.scopeName = scopeName;
  }

  public SymbolTable() {
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
  public Token getEncounteredExceptionToken() {
    return encounteredExceptionToken;
  }

  @Override
  public void setEncounteredExceptionToken(final Token encounteredExceptionToken) {
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
  public int hashCode() {
    return getFriendlyScopeName().hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof IScope scope) {
      return getFriendlyScopeName().equals((scope).getFriendlyScopeName());
    }
    return false;
  }

  /**
   * So we have a split map for each category
   * each of those has a map with a list of symbols of the same name.
   * So this means we can handle multiple methods with same name but different signatures.
   */
  private void addToSplitSymbols(final ISymbol symbol) {
    Map<String, List<ISymbol>> table =
        splitSymbols.computeIfAbsent(symbol.getCategory(), k -> new HashMap<>());
    List<ISymbol> list = table.computeIfAbsent(symbol.getName(), k -> new ArrayList<>());
    if (!symbol.getCategory().equals(ISymbol.SymbolCategory.METHOD) && list.contains(symbol)) {
      throw new CompilerException("Compiler Coding Error - Duplicate symbol [" + symbol + "]");
    }
    list.add(symbol);
  }

  /**
   * Find symbols in a specific category.
   */
  public List<ISymbol> getSymbolsForThisScopeOfCategory(final ISymbol.SymbolCategory category) {
    return orderedSymbols.stream().filter(symbol -> category.equals(symbol.getCategory())).toList();
  }

  /**
   * Get all the symbols in this table.
   */
  @Override
  public List<ISymbol> getSymbolsForThisScope() {
    return Collections.unmodifiableList(orderedSymbols);
  }

  /**
   * Search and resolve from a symbol search.
   */
  public Optional<ISymbol> resolve(final SymbolSearch search) {
    var rtn = resolveInThisScopeOnly(search);
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
        resolveForAllMatchingMethodsInEnclosingScope(search, new MethodSymbolSearchResult()));

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
    var optTable = Optional.ofNullable(getSplitSymbolTable(search.getSearchType()));
    optTable
        .stream()
        .map(table -> getSymbolByName(table, search.getName()))
        .map(methodSymbolCast)
        .forEach(methodList -> matcher.addMatchesToResult(result, search, methodList));

    return result;
  }

  /**
   * There are no supers so this will not add any methods.
   */
  protected MethodSymbolSearchResult resolveForAllMatchingMethodsInEnclosingScope(
      final MethodSymbolSearch search, MethodSymbolSearchResult result) {
    AssertValue.checkNotNull("Search must not be null", search);

    //nothing needed here.
    return result;
  }

  private Optional<ISymbol> resolveFromSplitSymbolTable(final String symbolName,
                                                        final SymbolSearch search) {
    return resolveInThisScopeOnly(getSplitSymbolTable(search.getSearchType()), symbolName, search);
  }

  /**
   * If search is unqualified (i.e. just a name then yes we look in this scope).
   * If the search is a fully qualified name then the scope name in the search has to match
   * this scope.
   */
  private boolean isSearchInThisScope(final SymbolSearch search) {
    String symbolName = search.getName();
    if (ISymbol.isQualifiedName(symbolName)) {
      return getScopeName().equals(ISymbol.getModuleNameIfPresent(symbolName));
    }
    return true;
  }

  /**
   * Resolve a symbol in this symbol table only.
   */
  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(final SymbolSearch search) {
    AssertValue.checkNotNull("Search cannot be null", search);

    if (!isSearchInThisScope(search)) {
      return Optional.empty();
    }

    String searchName = ISymbol.getUnqualifiedName(search.getName());

    if (search.getSearchType() != null) {
      return resolveFromSplitSymbolTable(searchName, search);
    }

    //So if search type is not set then that means search all categories!
    return Arrays.stream(ISymbol.SymbolCategory.values())
        .map(category -> resolveFromSplitSymbolTable(searchName,
            search.clone().setSearchType(category)))
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

    final Predicate<Optional<ISymbol>> canBeAssigned = toCheck -> {
      final Optional<ISymbol> searchSymbol = search.getNameAsSymbol(getScopeName());
      return isAssignable.test(toCheck, searchSymbol);
    };

    final Predicate<Optional<ISymbol>> canVariableTypeBeAssigned = foundType -> {
      final Optional<ISymbol> toReceive = search.getOfTypeOrReturn();
      //We do consider this acceptable as the search has not indicated a specific type required.
      return toReceive.isEmpty() || isAssignable.test(toReceive, foundType);
    };

    // Finds a method (if present) in a list of ISymbols, by casting to methodSymbols and runs
    // the matcher to add to results.
    final Supplier<MethodSymbolSearchResult> findMethod = () -> {
      MethodSymbolSearchResult result = new MethodSymbolSearchResult();
      Optional.of(symbolList).stream().parallel()
          .map(methodSymbolCast)
          .forEach(methodList -> matcher.addMatchesToResult(result, search, methodList));
      return result;
    };

    final var searchType = search.getSearchType();

    return switch (searchType) {
      case METHOD:
        var result = findMethod.get();

        //This is ambiguous - i.e. we found more than one method that would match.
        //so report failed to find, calling code then needs
        //to do a fuzzy search and report on ambiguities to make developer choose.
        yield result.isSingleBestMatchPresent() ? result.getSingleBestMatchSymbol() :
            Optional.empty();
      case FUNCTION, TEMPLATE_TYPE, TEMPLATE_FUNCTION:
        yield checkAndSelectFirstItem.apply(symbolList);
      case TYPE:
        var checkType = checkAndSelectFirstItem.apply(symbolList);
        yield canBeAssigned.test(checkType) ? checkType : Optional.empty();
      case VARIABLE:
        var checkVariable = checkAndSelectFirstItem.apply(symbolList);
        var foundType =
            checkVariable.stream().map(ISymbol::getType).findFirst().orElse(Optional.empty());
        yield canVariableTypeBeAssigned.test(foundType) ? checkVariable : Optional.empty();
      case CONTROL:
        //You can never locate controls. Well not yet; and I don't think we will need to.
        yield Optional.empty();
    };
  }

  /**
   * This class is the root and so there is no enclosing scope.
   * sub-classes will override to provide the scope that encloses them
   */
  protected Optional<ISymbol> resolveWithEnclosingScope(final SymbolSearch search) {
    AssertValue.checkNotNull("Search must not be null", search);
    return Optional.empty();
  }

  @Override
  public boolean isScopeAMatchForEnclosingScope(IScope toCheck) {
    return false;
  }

  @Override
  public Optional<ScopedSymbol> findNearestAggregateScopeInEnclosingScopes() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return scopeName;
  }

  /**
   * Just a wrapper to make null safe.
   */
  private Map<String, List<ISymbol>> getSplitSymbolTable(final ISymbol.SymbolCategory category) {
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