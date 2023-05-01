package org.ek9lang.compiler.symbol;

import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Concept of a scope where functions, methods and variables can be declared.
 */
public interface IScope {

  IScope clone(IScope withParentAsAppropriate);

  ScopeType getScopeType();

  String getScopeName();

  /**
   * Useful for printing out errors and information.
   * The scope name might be a complex generated name used internally a bit like symbol names are.
   * So some items are both scopes and symbols - so ideally we'd want to use a friendly name
   * where possible.
   *
   * @return The friendly name to be used for the developer.
   */
  String getFriendlyScopeName();

  /**
   * Typically used with functions.
   * Something that is pure cannot have 'side effects'.
   * To enforce this a bit of logic can have no references to other variables, methods and functions
   * else how can no side effects be guaranteed. Hence, a function that just tests a value
   * or calculates a result is deemed pure. Anything else is questionable.
   *
   * @return true if marked as pure, false otherwise.
   */
  boolean isMarkedPure();

  /**
   * Define a Symbol in this scope.
   */
  void define(ISymbol symbol);

  /**
   * Provide a list of all the parameters held in this scope and only this scope.
   */
  List<ISymbol> getSymbolsForThisScope();

  /**
   * Find the nearest symbol of that name up the scope tree.
   */
  Optional<ISymbol> resolve(SymbolSearch search);

  /**
   * Looks in scope and parent scopes.
   */
  MethodSymbolSearchResult resolveMatchingMethods(MethodSymbolSearch search,
                                                  MethodSymbolSearchResult result);

  /**
   * Look in own scope just for methods and return all those that could match.
   * ideally there would be one in the case of ambiguities there will be more.
   */
  MethodSymbolSearchResult resolveMatchingMethodsInThisScopeOnly(MethodSymbolSearch search,
                                                                 MethodSymbolSearchResult result);

  /**
   * Just look in own scope.
   */
  Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search);

  boolean isScopeAMatchForEnclosingScope(IScope toCheck);

  Optional<ScopedSymbol> findNearestNonBlockScopeInEnclosingScopes();

  Optional<ScopedSymbol> findNearestDynamicBlockScopeInEnclosingScopes();

  /**
   * Typically in a scoped block we can encounter situations (like exceptions) that cause the block
   * to end (terminate) early.
   */
  boolean isTerminatedNormally();

  Token getEncounteredExceptionToken();

  void setEncounteredExceptionToken(Token encounteredExceptionToken);

  /**
   * The main type of scope in use a block is just like a set of instruction inside an if block
   * or a while block whereas a non-block is as the whole class/function/component level.
   * A dynamic-block is to de-mark a dynamic class or a dynamic function.
   * For some operations (especially around generic/template processing) it is important to find the top-most
   * block all the code has been defined in.
   * So for variable definition it follows that same sort of logic as java not C/C++.
   * You can have fields as variables with a name say 'v1' and parameters and block declarations
   * of something as 'v1'.
   * But once in a block scope then you cannot redefine 'v1'.
   */
  enum ScopeType {
    NON_BLOCK,
    DYNAMIC_BLOCK,
    CAPTURE_BLOCK,
    BLOCK
  }
}
