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
 * Creates the appropriate IR Construct for a component declaration.
 * Follows the same pattern as ProgramCreator and ClassCreator.
 */
final class ComponentDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.ComponentDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;

  ComponentDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    super(parsedModule, compilerFlags);
    this.parsedModule = parsedModule;
  }

  @Override
  public IRConstruct apply(final EK9Parser.ComponentDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.COMPONENT) {
      final var construct = new IRConstruct(symbol);

      // Process aggregateParts if present (methods, operators, properties)
      if (ctx.aggregateParts() != null) {
        createOperationsForAggregateParts(construct, aggregateSymbol, ctx.aggregateParts());
      }

      return construct;
    }
    throw new CompilerException("Cannot create Component - expect AggregateSymbol of COMPONENT Genus");
  }

  private void createOperationsForAggregateParts(final IRConstruct construct,
                                                 final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.AggregatePartsContext ctx) {
    // Create Operation nodes for each method in the component
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var symbol = parsedModule.getRecordedSymbol(methodCtx);
      processAsMethodOrOperator(construct, symbol, methodCtx.operationDetails());
    }

    // Create Operation nodes for each operator in the component
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var symbol = parsedModule.getRecordedSymbol(operatorCtx);
      processAsMethodOrOperator(construct, symbol, operatorCtx.operationDetails());
    }

    // Note: Component properties are like static fields - handled differently than operations
    // They would be processed for initialization expressions but don't create Operation nodes
  }
}