package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Acts as a base for the main construct definitions.
 */
abstract class AbstractDfnGenerator {

  private final OperationDfnGenerator operationDfnGenerator;

  AbstractDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.operationDfnGenerator = new OperationDfnGenerator(parsedModule, compilerFlags);
  }

  protected void processAsMethodOrOperator(final IRConstruct construct,
                                           final ISymbol symbol,
                                           final EK9Parser.OperationDetailsContext ctx) {

    if (symbol instanceof MethodSymbol method) {
      final var operation = new Operation(method);
      operationDfnGenerator.accept(operation, ctx);
      construct.add(operation);
    } else {
      throw new CompilerException("Expecting a Method Symbol");
    }
  }
}
