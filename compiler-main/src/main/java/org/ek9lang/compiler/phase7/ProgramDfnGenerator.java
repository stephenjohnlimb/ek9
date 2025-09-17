package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for an Aggregate of type 'program'.
 */
final class ProgramDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.MethodDeclarationContext, IRConstruct> {

  /**
   * Constructor using stack context - the single source of state.
   */
  ProgramDfnGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  @Override
  public IRConstruct apply(final EK9Parser.MethodDeclarationContext ctx) {
    final var symbol = getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.PROGRAM) {

      final var construct = new IRConstruct(symbol);
      //Now for 'programs' we have used just a 'method' in the grammar, mangled it to an aggregate.
      //then created an artificial method on that aggregate - so there is no 'parse tree' for this.
      //We must now manually make the OperationInstr and then we can jump back to the parse tree (ctx) and
      //process that.
      createOperation(construct, aggregateSymbol, ctx);
      return construct;
    }
    throw new CompilerException("Cannot create Program - expect AggregateSymbol of 'PROGRAM' Genus");
  }

  private void createOperation(final IRConstruct construct, final AggregateSymbol aggregateSymbol,
                               final EK9Parser.MethodDeclarationContext ctx) {

    AssertValue.checkTrue("Expecting only one method on program",
        aggregateSymbol.getAllMethods().size() == 1);

    final var context = newPerConstructContext();
    final var optionalMethod = aggregateSymbol.getAllMethods().stream().findFirst();
    optionalMethod.ifPresent(method -> {
      final var debugInfo = new DebugInfoCreator(context).apply(method.getSourceToken());
      final var operation = new OperationInstr(method, debugInfo);
      operationDfnGenerator.accept(operation, ctx.operationDetails());
      construct.add(operation);
    });
  }

}
