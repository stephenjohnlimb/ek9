package org.ek9lang.compiler.phase7;

import static org.ek9lang.compiler.symbols.SymbolGenus.CLASS_TRAIT;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Acts as a base for the main construct definitions.
 * As there are lots of common processing aspects across constructs and, they are
 * only really very focussed on IR generation (and not reusable elsewhere) hence defined in this base.
 */
abstract class AbstractDfnGenerator {

  protected final ParsedModule parsedModule;
  protected final CompilerFlags compilerFlags;
  protected final String voidStr;

  private final OperationDfnGenerator operationDfnGenerator;

  AbstractDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
    this.operationDfnGenerator = new OperationDfnGenerator(parsedModule, compilerFlags);
    this.voidStr = parsedModule.getEk9Types().ek9Void().getFullyQualifiedName();
  }

  protected void processAsMethodOrOperator(final IRConstruct construct,
                                           final ISymbol symbol,
                                           final EK9Parser.OperationDetailsContext ctx) {

    AssertValue.checkNotNull("IRConstruct cannot be null", construct);
    AssertValue.checkNotNull("ISymbol cannot be null", symbol);
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    if (symbol instanceof MethodSymbol method) {
      final var debugInfo = new DebugInfoCreator(new IRContext(parsedModule, compilerFlags)).apply(method);
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
  protected boolean isNotImplicitSuperClass(final ISymbol superSymbol) {
    if (CLASS_TRAIT == superSymbol.getGenus()) {
      return true;
    }
    final var fqn = superSymbol.getFullyQualifiedName();
    return !fqn.startsWith("org.ek9.lang::_") && !parsedModule.getEk9Types().ek9Any().isExactSameType(superSymbol);

  }

  protected Operation newSyntheticInitOperation(final IRContext context,
                                                final IScope scope,
                                                final String methodName) {

    final var method = newSyntheticInitMethodSymbol(scope, methodName);
    final var debugInfo = new DebugInfoCreator(context).apply(method);
    return new Operation(method, debugInfo);

  }

  private MethodSymbol newSyntheticInitMethodSymbol(final IScope scope,
                                                    final String methodName) {
    final var methodSymbol = new MethodSymbol(methodName, scope);
    final ISymbol returnType = parsedModule.getEk9Types().ek9Void();
    methodSymbol.setType(returnType);
    methodSymbol.setReturningSymbol(new VariableSymbol(IRConstants.RETURN_VARIABLE, returnType));
    methodSymbol.setMarkedPure(true); // Initialization methods are pure

    return methodSymbol;
  }
}
