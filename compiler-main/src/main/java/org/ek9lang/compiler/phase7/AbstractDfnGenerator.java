package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Acts as a base for the main construct definitions.
 */
abstract class AbstractDfnGenerator {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;
  private final OperationDfnGenerator operationDfnGenerator;

  AbstractDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
    this.operationDfnGenerator = new OperationDfnGenerator(parsedModule, compilerFlags);
  }

  protected void processAsMethodOrOperator(final IRConstruct construct,
                                           final ISymbol symbol,
                                           final EK9Parser.OperationDetailsContext ctx) {

    if (symbol instanceof MethodSymbol method) {
      final var context = new IRContext(parsedModule, compilerFlags);
      final var debugInfo = new DebugInfoCreator(context).apply(method);
      final var operation = new Operation(method, debugInfo);
      operationDfnGenerator.accept(operation, ctx);
      construct.add(operation);
    } else {
      throw new CompilerException("Expecting a Method Symbol");
    }
  }

  /**
   * Check if the given super class is an implicit base class that should be ignored for initialization calls.
   * In EK9, classes might implicitly extend base types that don't need explicit super initialization.
   */
  protected boolean isNotImplicitSuperClass(final IAggregateSymbol superSymbol) {
    return !parsedModule.getEk9Types().ek9Any().isExactSameType(superSymbol);
  }

  /**
   * Create a synthetic method symbol for initialization methods (c_init, i_init).
   * These are basic very early methods, that can be triggered during construction of the construct.
   */
  protected MethodSymbol newSyntheticInitMethodSymbol(final AggregateSymbol aggregateSymbol,
                                                      final String methodName) {
    final var methodSymbol = new MethodSymbol(methodName, aggregateSymbol);
    final ISymbol returnType = parsedModule.getEk9Types().ek9Void();
    methodSymbol.setType(returnType);
    methodSymbol.setReturningSymbol(new VariableSymbol("_rtn", returnType));
    methodSymbol.setMarkedPure(true); // Initialization methods are pure

    return methodSymbol;
  }
}
