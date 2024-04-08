package org.ek9lang.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.ek9lang.compiler.directives.Directive;
import org.ek9lang.compiler.directives.DirectiveType;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Used to hold just the transient data that will be used by listeners and visitors and
 * SymbolAndScopeManagement.
 */
public class ParsedModuleTransientData {

  /**
   * Now we also need to hold a set of scopes for each context.
   * This is where we store the scopes in a map with the contexts
   * We use this for the multiple passes we need to do.
   * But remember there will be other parsed modules with their scopes
   * and important some with the same module name! so need to go to compilable program to access.
   * As the 'listener' goes through the ek9 source we register these scopes here.
   */
  private final ParseTreeProperty<IScope> scopes = new ParseTreeProperty<>();

  /**
   * These are the directives that can be added to EK9 source.
   * These can be used for testing the compiler, but also enabled instrumentation and reification.
   */
  private final List<Directive> directives = new ArrayList<>();

  /**
   * On first pass through the code we don't know the types or other items relating to a symbol
   * So we keep the map of the context and the symbol, so we can augment information on the symbol
   * Then when we are ready we can attempt to add the symbols to the correct scopes during a visit
   * But we may find there are duplicate and the like so that is semantic analysis.
   * So it is probable that something like a class will be both recorded as a symbol and also a scope.
   */
  private final ParseTreeProperty<ISymbol> symbols = new ParseTreeProperty<>();

  /**
   * When processing EK9 source code the developer now has some ability to use
   * '@directives'. These are aimed at code compilation, instrumentation and error checking.
   * This means that for compiler development, we can reduce the number of Java unit tests and
   * specific coding (to some extent), by adding in our expectations just before we write
   * so erroneous code (to check the compiler).
   * This means that the test and the deliberated defective code as co-located.
   */
  public void recordDirective(final Directive directive) {

    AssertValue.checkNotNull("Directive cannot be null", directive);
    directives.add(directive);

  }

  /**
   * Provide access to any directives recorded of a specific type and compilation phase.
   */
  public List<Directive> getDirectives(final DirectiveType type, final CompilationPhase phase) {

    return directives.stream().filter(directive -> directive.type() == type)
        .filter(directive -> directive.isForPhase(phase)).toList();
  }

  /**
   * Provide access to any directives recorded.
   */
  public List<Directive> getDirectives(final DirectiveType type) {

    return directives.stream().filter(directive -> directive.type() == type).toList();
  }

  /**
   * Record a particular node context during listen/visit of a context with a particular scope.
   */
  public void recordScope(final ParseTree node, final IScope withScope) {

    AssertValue.checkNotNull("WithScope cannot be null", withScope);
    scopes.put(node, withScope);

  }

  /**
   * Locate and return a recorded scope against part of the parse tree,
   * this may return null if nothing has been recorded.
   */
  public IScope getRecordedScope(final ParseTree node) {

    return scopes.get(node);
  }

  /**
   * Record a particular node context with a particular symbol.
   */
  public void recordSymbol(final ParseTree node, final ISymbol symbol, final Module module) {

    AssertValue.checkNotNull("Node cannot be null", node);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    AssertValue.checkNotNull("Module cannot be null", module);

    //Let the symbol know where it is defined.
    //But it can only be defined in one place - in case of references we record in other locations
    //We only want its actual module recorded the first time it is encountered.
    symbol.setParsedModule(Optional.of(module));
    symbols.put(node, symbol);

  }

  /**
   * Locate and return a recorded symbol against part of the parse tree,
   * this may return null if nothing has been recorded.
   */
  public ISymbol getRecordedSymbol(final ParseTree node) {

    return symbols.get(node);
  }

}
