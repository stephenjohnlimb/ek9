package org.ek9lang.compiler.main.phases.definition;

import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.IScopedSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.ScopeStack;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * Used as external helper to record symbols and scopes across the parser.
 * They are held in the parsedModule against specific nodes in the parseTree.
 * This enabled later phases to look up the symbols and scopes when it encounters
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
 * All would be lost! - But for the fact that the scope was registered against a ParseTree node in the
 * ParsedModule. Hence, it lives on - with all the sub scopes and symbols.
 * This information in the ParsedModule will be enriched further in additional and different passes.
 */
public class SymbolAndScopeManagement {
  private final ParsedModule parsedModule;
  private final ScopeStack scopeStack;

  public SymbolAndScopeManagement(ParsedModule parsedModule, ScopeStack scopeStack) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    AssertValue.checkNotNull("ScopeStack cannot be null", scopeStack);

    this.parsedModule = parsedModule;
    this.scopeStack = scopeStack;
  }

  /**
   * To be used to ensure that a scope has been pushed on to the scopeStack.
   */
  public void enterScope(IScope scope) {
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

  /**
   * Just uses the current scope on the top of the stack to resolve for the search.
   * This may not be limited to just that scope - the search could go all the way back up
   * to the main CompilableProgram and seach in other modules.
   */
  public Optional<ISymbol> resolve(SymbolSearch search) {
    return scopeStack.peek().resolve(search);
  }

  /**
   * For dynamic functions, classes and records we need the aggregate to be defined at the module level.
   * So the type is visible throughout the module.
   * Hence, the symbol is defined at the module level (not the current scope in the scopeStack).
   * It is also recorded as a symbol and a scope against the node in the parsedModule.
   * Finally, the scope is pushed on to the scope stack.
   * Yes, it's a bit strange - but this is how Dynamic classes and Functions can be defined like
   * Tuples, Functions deep with scope block or for loops - and yet appear as a new module level Type.
   */
  public void enterNewDynamicScopedSymbol(IScopedSymbol symbol, ParseTree node) {
    parsedModule.getModuleScope().define(symbol);
    enterNewScopedSymbol(symbol, node);
  }

  /**
   * A new symbol has been encountered and is defined within the current scope in the scope stack
   * and recorded in the parsedModule against the appropriate node.
   */
  public void enterNewSymbol(ISymbol symbol, ParseTree node) {
    scopeStack.peek().define(symbol);
    parsedModule.recordSymbol(node, symbol);
  }

  /**
   * For literals we only record in the parsedModule.
   */
  public void enterNewLiteral(ISymbol symbol, ParseTree node) {
    parsedModule.recordSymbol(node, symbol);
  }


  public void defineScopedSymbol(IScopedSymbol symbol, ParseTree node) {
    System.out.println("Defining [" + symbol.getName() + "] in " + scopeStack.peek());
    scopeStack.peek().define(symbol);
    enterNewScopedSymbol(symbol, node);
  }

  /**
   * Enter a new scoped symbol and record in parsed module as a symbol and as a scope.
   * Then push the scope on to the working scope stack.
   */
  public void enterNewScopedSymbol(IScopedSymbol symbol, ParseTree node) {
    parsedModule.recordSymbol(node, symbol);
    enterNewScope(symbol, node);
  }

  /**
   * Enter a new scope and record in both the parsed module and push on to the working scope stack.
   */
  public void enterNewScope(IScope scope, ParseTree node) {
    recordScopeForStackConsistency(scope, node);
  }

  /**
   * There are times in parsing/listening when symbols are already defined (due to a developer error).
   * During this situation we cannot define a new node, but we can detect the duplicate (display errors).
   * But so the scope stack does not get corrupted - it is important to still push the existing scope on
   * to the stack. In this way when the stack is popped everything works out.
   */
  public void recordScopeForStackConsistency(IScope scope, ParseTree node) {
    parsedModule.recordScope(node, scope);
    enterScope(scope);
  }
}
