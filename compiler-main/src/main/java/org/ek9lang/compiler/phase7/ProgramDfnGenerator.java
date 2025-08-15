package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for an Aggregate of type 'program'.
 * TODO still needs the code for the actual 'main' entry point that deals with accepting
 * TODO and converting commandline arguments into correctly typed arguments to call _main.
 */
final class ProgramDfnGenerator implements Function<EK9Parser.MethodDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;
  private final OperationDfnGenerator operationDfnGenerator;

  ProgramDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    AssertValue.checkNotNull("CompilerFlags cannot be null", compilerFlags);

    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
    this.operationDfnGenerator = new OperationDfnGenerator(parsedModule, compilerFlags);
  }

  @Override
  public IRConstruct apply(final EK9Parser.MethodDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.PROGRAM) {

      final var construct = new IRConstruct(symbol);
      //Now for 'programs' we have used just a 'method' in the grammar, mangled it to an aggregate.
      //then created an artificial method on that aggregate - so there is no 'parse tree' for this.
      //We must now manually make the Operation and then we can jump back to the parse tree (ctx) and
      //process that.
      createOperation(construct, aggregateSymbol, ctx);
      return construct;
    }
    throw new CompilerException("Cannot create Program - expect AggregateSymbol of 'PROGRAM' Genus");
  }

  private void createOperation(final IRConstruct construct, final AggregateSymbol aggregateSymbol,
                               final EK9Parser.MethodDeclarationContext ctx) {

    System.out.println("Program: would create static main entry point deal with args and call to _main");

    AssertValue.checkTrue("Expecting only one method on program",
        aggregateSymbol.getAllMethods().size() == 1);

    final var context = new IRContext(parsedModule, compilerFlags);
    final var optionalMethod = aggregateSymbol.getAllMethods().stream().findFirst();
    optionalMethod.ifPresent(method -> {
      final var debugInfo = new DebugInfoCreator(context).apply(method);
      final var operation = new Operation(method, debugInfo);
      operationDfnGenerator.accept(operation, ctx.operationDetails());
      construct.add(operation);
    });
  }

}
