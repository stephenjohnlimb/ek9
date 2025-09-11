package org.ek9lang.compiler.phase7.support;

import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Locates the Operation details context for a method - or throws a compiler exception.
 */
public class OperationDetailContextOrError
    implements BiFunction<MethodSymbol, EK9Parser.AggregatePartsContext, EK9Parser.OperationDetailsContext> {

  private final ParsedModule parsedModule;

  public OperationDetailContextOrError(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
  }

  @Override
  public EK9Parser.OperationDetailsContext apply(final MethodSymbol method,
                                                 final EK9Parser.AggregatePartsContext ctx) {
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    // Search through methodDeclaration contexts
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var contextSymbol = parsedModule.getRecordedSymbol(methodCtx);
      if (contextSymbol == method) {
        return methodCtx.operationDetails();
      }
    }

    // Search through operatorDeclaration contexts
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var contextSymbol = parsedModule.getRecordedSymbol(operatorCtx);
      if (contextSymbol == method) {
        return operatorCtx.operationDetails();
      }
    }

    throw new CompilerException("Given synthetic method, but expecting concrete one");
  }
}
