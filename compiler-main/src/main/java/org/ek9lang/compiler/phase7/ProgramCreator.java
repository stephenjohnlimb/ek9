package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for an Aggregate of type 'program'.
 */
final class ProgramCreator implements Function<EK9Parser.MethodDeclarationContext, Construct> {

  private final ParsedModule parsedModule;

  private final BlockCreator blockCreator;

  public ProgramCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.blockCreator = new BlockCreator(parsedModule);
  }

  @Override
  public Construct apply(final EK9Parser.MethodDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.PROGRAM) {

      final var construct = new Construct(symbol);
      //Now for 'programs' we have used just a 'method' in the grammar, mangled it to an aggregate.
      //then created an artificial method on that aggregate - so there is no 'parse tree' for this.
      //We must now manually make the Operation and then we can jump back to the parse tree (ctx) and
      //process that.
      createOperation(construct, aggregateSymbol, ctx);
      return construct;
    }
    throw new CompilerException("Cannot create Program - expect AggregateSymbol of PROGRAM Genus");
  }

  private void createOperation(final Construct construct, final AggregateSymbol aggregateSymbol,
                               final EK9Parser.MethodDeclarationContext ctx) {
    AssertValue.checkTrue("Expecting only one method on program",
        aggregateSymbol.getAllMethods().size() == 1);

    final var optionalMethod = aggregateSymbol.getAllMethods().stream().findFirst();
    optionalMethod.ifPresent(method -> {
      final var operation = new Operation(method);
      final var block = blockCreator.apply(ctx.operationDetails().instructionBlock());
      operation.setBody(block);
      construct.add(operation);

    });

  }

}
