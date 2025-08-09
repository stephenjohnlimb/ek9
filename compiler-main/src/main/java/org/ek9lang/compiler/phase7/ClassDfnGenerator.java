package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for a class declaration.
 */
final class ClassDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.ClassDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;

  ClassDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    super(parsedModule, compilerFlags);
    this.parsedModule = parsedModule;
  }

  @Override
  public IRConstruct apply(final EK9Parser.ClassDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.CLASS) {
      final var construct = new IRConstruct(symbol);

      // Process aggregateParts if present (methods, operators, properties)
      if (ctx.aggregateParts() != null) {
        createOperationsForAggregateParts(construct, aggregateSymbol, ctx.aggregateParts());
      }

      return construct;
    }
    throw new CompilerException("Cannot create Class - expect AggregateSymbol of CLASS Genus");
  }

  private void createOperationsForAggregateParts(final IRConstruct construct,
                                                 final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.AggregatePartsContext ctx) {
    // Create Operation nodes for each method in the class
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var symbol = parsedModule.getRecordedSymbol(methodCtx);
      processAsMethodOrOperator(construct, symbol, methodCtx.operationDetails());
    }

    // Create Operation nodes for each operator in the class
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var symbol = parsedModule.getRecordedSymbol(operatorCtx);
      processAsMethodOrOperator(construct, symbol, operatorCtx.operationDetails());
    }

    // Note: Properties are handled differently - they're data declarations, not operations
    // They would be processed for initialization expressions but don't create Operation nodes
  }
}
