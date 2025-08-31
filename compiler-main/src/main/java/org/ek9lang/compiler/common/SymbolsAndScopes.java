package org.ek9lang.compiler.common;

import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.support.SymbolChecker;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.ModuleScope;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.symbols.ScopedSymbol;
import org.ek9lang.compiler.symbols.StackConsistencyScope;
import org.ek9lang.core.AssertValue;

/**
 * Used as external helper to record symbols and scopes across the parser.
 * They are held in the parsedModule against specific nodes in the parseTree.
 * This enables later phases to look up the symbols and scopes when it encounters
 * the same node in the parseTree. In this way the symbols get 'fleshed out'
 * with more details in each of the compiler phases.
 * This does push scopes on to the scope stack - that the listener uses.
 * IMPORTANT concepts here:
 * 1. The ParsedModule keeps a record of a Symbol against a ParseTree node
 * 2. The ParsedModule keeps a record of a Scope against a ParseTree node
 * 3. The scopeStack keeps a dynamic position of the current scope stuff is being added to.
 * But the scopeStack is ephemeral - it grows and shrinks based on the language constructs the
 * listener encounters, as stuff is added in - that scope become richer. But when the end of the
 * scope is encountered - it is popped off the scopeStack.
 * All would be lost! - But for the fact that the symbols/scopes were registered against a ParseTree node in the
 * ParsedModule. Hence, it lives on - with all the sub scopes and symbols.
 * This information in the ParsedModule will be enriched further in additional and different passes.
 * <br/>
 * Now also uses a transient CodeFlowAnalyzer to assess whether variables in instruction blocks have been initialised.
 */
public class SymbolsAndScopes {
  private final ParsedModule parsedModule;
  private final ScopeStack scopeStack;
  private final CodeFlowAnalyzer uninitialisedVariableAnalyzer = new UninitialisedVariableAnalyzer();
  private final CodeFlowAnalyzer unSafePropertyAccessAnalyzer = new UnSafePropertyAccessAnalyzer();
  private final CodeFlowAnalyzer unSafeOkResultAccessAnalyzer = new UnSafeGenericAccessAnalyzer();
  private final CodeFlowAnalyzer unSafeErrorResultAccessAnalyzer = new UnSafeGenericAccessAnalyzer();
  private final CodeFlowAnalyzer unSafeGetOptionalAccessAnalyzer = new UnSafeGenericAccessAnalyzer();
  private final CodeFlowAnalyzer unSafeNextIteratorAccessAnalyzer = new UnSafeGenericAccessAnalyzer();

  /**
   * Create a new instance for symbol and scope management.
   * If these types are not resolved then OK with a big exception and stop processing (so suppress get warning).
   */
  public SymbolsAndScopes(final ParsedModule parsedModule, final ScopeStack scopeStack) {

    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    AssertValue.checkNotNull("ScopeStack cannot be null", scopeStack);

    this.parsedModule = parsedModule;
    this.scopeStack = scopeStack;

  }

  public boolean isExternallyImplemented() {
    return parsedModule.isExternallyImplemented();
  }

  public ModuleScope getModuleScope() {

    return parsedModule.getModuleScope();
  }

  public Ek9Types getEk9Types() {

    return parsedModule.getEk9Types();
  }

  /**
   * Provide access to the set of code flow analyzers being used for flow analysis.
   */
  public List<CodeFlowAnalyzer> getCodeFlowAnalyzers() {

    return List.of(uninitialisedVariableAnalyzer, unSafePropertyAccessAnalyzer);
  }

  public List<ISymbol> getUninitialisedVariables(final IScope inScope) {

    return uninitialisedVariableAnalyzer.getSymbolsNotMeetingAcceptableCriteria(inScope);
  }

  public boolean isVariableInitialised(final ISymbol identifierSymbol, final IScope inScope) {

    return uninitialisedVariableAnalyzer.doesSymbolMeetAcceptableCriteria(identifierSymbol, inScope);
  }

  public boolean isVariableInitialised(final ISymbol identifierSymbol) {

    return isVariableInitialised(identifierSymbol, this.getTopScope());
  }

  /**
   * Records a variable against the appropriate scope.
   */
  public void recordSymbolDeclaration(final ISymbol identifierSymbol) {

    if (identifierSymbol.isReturningParameter()) {
      //Record against the outer scope, so it can be located.
      recordSymbolDeclaration(identifierSymbol, this.getTopScope().getEnclosingScope());
    } else {
      recordSymbolDeclaration(identifierSymbol, this.getTopScope());
    }

  }

  /**
   * Record variable declaration against a specific scope.
   */
  public void recordSymbolDeclaration(final ISymbol identifierSymbol, final IScope inScope) {
    uninitialisedVariableAnalyzer.recordSymbol(identifierSymbol, inScope);
  }

  public void recordDeclarationOfVariableUsingResult(final ISymbol identifierSymbol) {
    if (identifierSymbol.getSquirrelledData(CommonValues.OK_ACCESS_REQUIRES_SAFE_ACCESS) == null) {
      //Given the logic required to detect whether an identifier needs a check - just record a flag.
      identifierSymbol.putSquirrelledData(CommonValues.OK_ACCESS_REQUIRES_SAFE_ACCESS, "TRUE");
      identifierSymbol.putSquirrelledData(CommonValues.ERROR_ACCESS_REQUIRES_SAFE_ACCESS, "TRUE");
      unSafeOkResultAccessAnalyzer.recordSymbol(identifierSymbol, this.getTopScope());
      unSafeErrorResultAccessAnalyzer.recordSymbol(identifierSymbol, this.getTopScope());
    }
  }

  public void recordDeclarationOfVariableUsingOptional(final ISymbol identifierSymbol) {
    //Given the logic required to detect whether an identifier needs a check - just record a flag.
    if (identifierSymbol.getSquirrelledData(CommonValues.GET_ACCESS_REQUIRES_SAFE_ACCESS) == null) {
      identifierSymbol.putSquirrelledData(CommonValues.GET_ACCESS_REQUIRES_SAFE_ACCESS, "TRUE");
      unSafeGetOptionalAccessAnalyzer.recordSymbol(identifierSymbol, this.getTopScope());
    }
  }

  public void recordDeclarationOfVariableUsingIterator(final ISymbol identifierSymbol) {
    //Given the logic required to detect whether an identifier needs a check - just record a flag.
    if (identifierSymbol.getSquirrelledData(CommonValues.NEXT_ACCESS_REQUIRES_SAFE_ACCESS) == null) {
      identifierSymbol.putSquirrelledData(CommonValues.NEXT_ACCESS_REQUIRES_SAFE_ACCESS, "TRUE");
      unSafeNextIteratorAccessAnalyzer.recordSymbol(identifierSymbol, this.getTopScope());
    }
  }

  /**
   * Record an identifier was assigned and therefore initialised.
   */
  public void recordSymbolAssignment(final ISymbol identifierSymbol) {

    final var scope = this.getTopScope();
    markSymbolAsInitialised(identifierSymbol, scope);

    //Now we have to deal with the do/while loop so that do part gets marked as initialised
    final var parentScope = this.getParentOfTopScope();
    if (parentScope instanceof ScopedSymbol scopedSymbol
        && CommonValues.DO.toString().equals(scopedSymbol.getSquirrelledData(CommonValues.LOOP))) {
      //So image a do while loop - where a previously uninitialised variable is now initialised within the block
      //In such cases the variable is now initialised for the 'while' part - this only applies to do/while loops.
      markSymbolAsInitialised(identifierSymbol, parentScope);

    }

  }

  /**
   * Record an identifier was initialised.
   */
  public void markSymbolAsInitialised(final ISymbol identifierSymbol, final IScope inScope) {

    uninitialisedVariableAnalyzer.markSymbolAsMeetingAcceptableCriteria(identifierSymbol, inScope);

  }

  /**
   * Record an identifier as being safe to access.
   */
  public void markSymbolAccessSafe(final ISymbol identifierSymbol, final IScope inScope) {

    unSafePropertyAccessAnalyzer.markSymbolAsMeetingAcceptableCriteria(identifierSymbol, inScope);

  }

  /**
   * true if identifier is safe to access.
   */
  public boolean isSymbolAccessSafe(final ISymbol identifierSymbol) {

    return unSafePropertyAccessAnalyzer.doesSymbolMeetAcceptableCriteria(identifierSymbol, getTopScope());
  }

  public void markOkResultAccessSafe(final ISymbol identifierSymbol, final IScope inScope) {

    unSafeOkResultAccessAnalyzer.markSymbolAsMeetingAcceptableCriteria(identifierSymbol, inScope);

  }

  public boolean isOkResultAccessSafe(final ISymbol identifierSymbol) {

    return unSafeOkResultAccessAnalyzer.doesSymbolMeetAcceptableCriteria(identifierSymbol, getTopScope());
  }

  public void markErrorResultAccessSafe(final ISymbol identifierSymbol, final IScope inScope) {

    unSafeErrorResultAccessAnalyzer.markSymbolAsMeetingAcceptableCriteria(identifierSymbol, inScope);

  }

  public boolean isErrorResultAccessSafe(final ISymbol identifierSymbol) {

    return unSafeErrorResultAccessAnalyzer.doesSymbolMeetAcceptableCriteria(identifierSymbol, getTopScope());
  }

  public void markGetOptionalAccessSafe(final ISymbol identifierSymbol, final IScope inScope) {

    unSafeGetOptionalAccessAnalyzer.markSymbolAsMeetingAcceptableCriteria(identifierSymbol, inScope);

  }

  public boolean isGetOptionalAccessSafe(final ISymbol identifierSymbol) {

    return unSafeGetOptionalAccessAnalyzer.doesSymbolMeetAcceptableCriteria(identifierSymbol, getTopScope());
  }

  public void markNextIteratorAccessSafe(final ISymbol identifierSymbol, final IScope inScope) {

    unSafeNextIteratorAccessAnalyzer.markSymbolAsMeetingAcceptableCriteria(identifierSymbol, inScope);

  }

  public boolean isNextIteratorAccessSafe(final ISymbol identifierSymbol) {

    return unSafeNextIteratorAccessAnalyzer.doesSymbolMeetAcceptableCriteria(identifierSymbol, getTopScope());
  }

  /**
   * Ensure that the correct scope is pushed on to the stack.
   * This method takes a context, retrieves the scope, checks for null and then pushes it.
   *
   * @param ctx The contact that must have a scope recorded.
   */
  public IScope enterScope(final ParseTree ctx) {
    final var scope = getRecordedScope(ctx);
    AssertValue.checkNotNull("Scope should have been defined", scope);
    enterScope(scope);
    return scope;
  }

  /**
   * To be used to ensure that a scope has been pushed on to the scopeStack.
   */
  public void enterScope(final IScope scope) {

    scopeStack.push(scope);

  }

  /**
   * Normally called at the point where the parser listener exits a scope.
   * The scope will be popped off the scopeStack to reveal the parent scope.
   */
  public void exitScope() {

    scopeStack.pop();

  }

  /**
   * Provides access to the top of the scope stack.
   */
  public IScope getTopScope() {

    return scopeStack.peek();

  }

  public IScope getParentOfTopScope() {
    final var top = scopeStack.pop();
    final var parentScope = scopeStack.peek();
    scopeStack.push(top);
    return parentScope;
  }

  /**
   * Navigates back up the scope stack to find the first match of the scope type passed in.
   */
  public Optional<IScope> traverseBackUpStack(final IScope.ScopeType scopeType) {

    return scopeStack.traverseBackUpStack(scopeType);
  }

  /**
   * Traversed back up the scope stack to try and locate the enclosing method (if there is one).
   * Clearly the scope might not be within a method and so Optional.empty will result.
   *
   * @return The method and aggregate information if a method was located.
   */
  public Optional<MethodAndAggregateData> traverseBackUpStackToEnclosingMethod() {

    final var maybeMethod = scopeStack.traverseBackUpStackToEnclosingMethod();
    if (maybeMethod.isPresent()) {
      final var method = maybeMethod.get();
      final var aggregate = (IAggregateSymbol) method.getParentScope();

      return Optional.of(new MethodAndAggregateData(method, aggregate));
    }

    return Optional.empty();
  }

  public Optional<IScopedSymbol> traverseBackUpStackToMethodOrFunction() {

    return scopeStack.traverseBackUpStackToMethodOrFunction();
  }

  public Optional<ISymbol> resolveOrDefine(final PossibleGenericSymbol parameterisedSymbol,
                                           final ErrorListener errorListener) {

    return parsedModule.getModuleScope().resolveOrDefine(parameterisedSymbol, errorListener);
  }

  /**
   * A new symbol has been encountered and is defined within the current scope in the scope stack
   * and recorded in the parsedModule against the appropriate node.
   */
  public void enterNewSymbol(final ISymbol symbol, final ParseTree node) {

    scopeStack.peek().define(symbol);
    recordSymbol(symbol, node);

  }

  public ISymbol getRecordedSymbol(final ParseTree node) {

    return parsedModule.getRecordedSymbol(node);
  }

  public IScope getRecordedScope(final ParseTree node) {

    return parsedModule.getRecordedScope(node);
  }

  /**
   * Record a symbol against a specific node (so it can be looked up later).
   */
  public void recordSymbol(final ISymbol symbol, final ParseTree node) {

    parsedModule.recordSymbol(node, symbol);

  }

  /**
   * For literals, we only record in the parsedModule.
   */
  public void enterNewLiteral(final ISymbol symbol, final ParseTree node) {

    recordSymbol(symbol, node);

  }

  /**
   * Create a new constant as declared in the constants section and records the symbol.
   */
  public void enterNewConstant(final ISymbol symbol, final ParseTree node, final SymbolChecker symbolChecker) {

    final var moduleScope = parsedModule.getModuleScope();
    if (moduleScope.defineOrError(symbol, symbolChecker)) {
      recordSymbol(symbol, node);
    }

  }

  /**
   * To be used when defining a high level symbol at module scope.
   */
  public void enterModuleScopedSymbol(final IScopedSymbol symbol,
                                      final ParseTree node,
                                      final SymbolChecker symbolChecker) {

    final var scopeType = symbol.getScopeType();
    final var moduleScope = parsedModule.getModuleScope();

    if (moduleScope.defineOrError(symbol, symbolChecker)) {
      enterNewScopedSymbol(symbol, node);
    } else {
      recordScopeForStackConsistency(new StackConsistencyScope(moduleScope, scopeType), node);
    }

  }

  /**
   * Record a new scoped symbol in the current scope on the stack.
   * Also records the symbol as both a scope and a symbol in the parsedModule.
   */
  public void defineScopedSymbol(final IScopedSymbol symbol, final ParseTree node) {

    AssertValue.checkNotNull("Looks like your enter/exits are not balanced", scopeStack.peek());

    scopeStack.peek().define(symbol);
    enterNewScopedSymbol(symbol, node);

  }

  /**
   * Enter a new scoped symbol and record in parsed module as a symbol and as a scope.
   * Then push the scope on to the working scope stack.
   */
  public void enterNewScopedSymbol(final IScopedSymbol symbol, final ParseTree node) {

    recordSymbol(symbol, node);
    enterNewScope(symbol, node);

  }

  /**
   * Enter a new scope and record in both the parsed module and push on to the working scope stack.
   */
  public void enterNewScope(final IScope scope, final ParseTree node) {

    recordScopeForStackConsistency(scope, node);

  }

  /**
   * There are times in parsing/listening when symbols are already defined (due to a developer error).
   * During this situation we cannot define a new node, but we can detect the duplicate (display errors).
   * But so the scope stack does not get corrupted - it is important to still push the existing scope on
   * to the stack. In this way when the stack is popped everything works out.
   */
  public void recordScopeForStackConsistency(final IScope scope, final ParseTree node) {

    parsedModule.recordScope(node, scope);
    enterScope(scope);

  }
}
