package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for a trait declaration.
 * Follows the same pattern as ProgramCreator and ClassCreator.
 */
final class TraitDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.TraitDeclarationContext, IRConstruct> {


  /**
   * Constructor using stack context - the single source of state.
   */
  TraitDfnGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  @Override
  public IRConstruct apply(final EK9Parser.TraitDeclarationContext ctx) {
    final var symbol = getParsedModule().getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.CLASS_TRAIT) {
      final var construct = new IRConstruct(symbol);

      // Process aggregateParts if present (default method implementations, operators, properties)
      if (ctx.aggregateParts() != null) {
        createOperationsForAggregateParts(construct, aggregateSymbol, ctx.aggregateParts());
      }

      return construct;
    }
    throw new CompilerException("Cannot create Trait - expect AggregateSymbol of CLASS_TRAIT Genus");
  }

  private void createOperationsForAggregateParts(final IRConstruct construct,
                                                 final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.AggregatePartsContext ctx) {
    // Create Operation nodes for each method in the trait
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var symbol = getParsedModule().getRecordedSymbol(methodCtx);
      processAsMethodOrOperator(construct, symbol, methodCtx.operationDetails());
    }

    // Create Operation nodes for each operator in the trait
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var symbol = getParsedModule().getRecordedSymbol(operatorCtx);
      processAsMethodOrOperator(construct, symbol, operatorCtx.operationDetails());
    }

    // Note: Trait properties are handled differently - they're typically abstract declarations
    // Only concrete default implementations create Operation nodes
  }
}