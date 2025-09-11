package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for a record declaration.
 * Follows the same pattern as ProgramCreator and ClassCreator.
 */
final class RecordDfnGenerator extends AggregateDfnGenerator
    implements Function<EK9Parser.RecordDeclarationContext, IRConstruct> {

  /**
   * Constructor using stack context - the single source of state.
   */
  RecordDfnGenerator(final IRGenerationContext stackContext) {
    super(stackContext, SymbolGenus.RECORD);
  }

  @Override
  public IRConstruct apply(final EK9Parser.RecordDeclarationContext ctx) {
    final var symbol = getParsedModule().getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol) {
      return processAggregate(aggregateSymbol, ctx.aggregateParts());
    }
    throw new CompilerException("Cannot create Record - expect AggregateSymbol of RECORD Genus");
  }

}