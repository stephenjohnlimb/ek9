package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for a standalone function.
 * Follows the same pattern as ProgramCreator.
 */
final class FunctionCreator implements Function<EK9Parser.FunctionDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;

  public FunctionCreator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }

  @Override
  public IRConstruct apply(final EK9Parser.FunctionDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);

    if (symbol instanceof FunctionSymbol functionSymbol && symbol.getGenus() == SymbolGenus.FUNCTION) {
      final var construct = new IRConstruct(symbol);
      
      // Create Operation for the function itself
      createOperation(construct, functionSymbol, ctx);
      
      return construct;
    }
    throw new CompilerException("Cannot create Function - expect FunctionSymbol of FUNCTION Genus");
  }

  private void createOperation(final IRConstruct construct, final FunctionSymbol functionSymbol,
                               final EK9Parser.FunctionDeclarationContext ctx) {
    final var operation = new Operation(functionSymbol);
    
    // Process executable content if operationDetails is present
    if (ctx.operationDetails() != null) {
      // Create IRGenerationContext for this function's executable code
      final var context = new IRGenerationContext(parsedModule, compilerFlags);
      final var basicBlockCreator = new BasicBlockCreator(context);
      
      // Generate BasicBlock IR for the function
      if (ctx.operationDetails().instructionBlock() != null) {
        final var basicBlock = basicBlockCreator.apply(ctx.operationDetails().instructionBlock());
        operation.setBody(basicBlock);
      }
    }
    
    construct.add(operation);
  }
}