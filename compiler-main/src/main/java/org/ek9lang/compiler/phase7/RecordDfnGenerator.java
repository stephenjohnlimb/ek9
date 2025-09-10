package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for a record declaration.
 * Follows the same pattern as ProgramCreator and ClassCreator.
 */
final class RecordDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.RecordDeclarationContext, IRConstruct> {

  /**
   * Constructor using stack context - the single source of state.
   */
  RecordDfnGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  @Override
  public IRConstruct apply(final EK9Parser.RecordDeclarationContext ctx) {
    final var symbol = getParsedModule().getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.RECORD) {
      final var construct = new IRConstruct(symbol);

      // Process aggregateParts if present (methods, operators, properties)
      if (ctx.aggregateParts() != null) {
        createOperationsForAggregateParts(construct, aggregateSymbol, ctx.aggregateParts());
      }

      return construct;
    }
    throw new CompilerException("Cannot create Record - expect AggregateSymbol of RECORD Genus");
  }

  private void createOperationsForAggregateParts(final IRConstruct construct,
                                                 final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.AggregatePartsContext ctx) {
    // Create Operation nodes for each method in the record
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var symbol = getParsedModule().getRecordedSymbol(methodCtx);
      processAsMethodOrOperator(construct, symbol, methodCtx.operationDetails());
    }

    // Create Operation nodes for each operator in the record
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var symbol = getParsedModule().getRecordedSymbol(operatorCtx);
      processAsMethodOrOperator(construct, symbol, operatorCtx.operationDetails());
    }

    // Note: Properties are handled differently - they're data declarations, not operations
    // They would be processed for initialization expressions but don't create Operation nodes
  }
}