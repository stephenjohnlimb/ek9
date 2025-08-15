package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for a standalone function.
 * Functions are generated as class-like constructs with synthetic _call methods,
 * following the pattern of built-in generic functions like BiPredicate and Function.
 * This allows functions to be passed as delegates and called uniformly.
 */
final class FunctionDfnGenerator implements Function<EK9Parser.FunctionDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;
  private final OperationDfnGenerator operationDfnGenerator;

  FunctionDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
    this.operationDfnGenerator = new OperationDfnGenerator(parsedModule, compilerFlags);
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

    final var context = new IRContext(parsedModule, compilerFlags);

    // Create synthetic _call method following AggregateManipulator patterns
    final var callMethod = createSyntheticCallMethod(functionSymbol);
    final var debugInfo = new DebugInfoCreator(context).apply(callMethod);
    // Create Operation for the synthetic _call method (not the function itself)
    final var operation = new Operation(callMethod, debugInfo);
    
    // Process executable content using OperationDfnGenerator if operationDetails is present
    if (ctx.operationDetails() != null) {
      operationDfnGenerator.accept(operation, ctx.operationDetails());
    }
    
    construct.add(operation);
  }
  
  /**
   * Creates a synthetic _call method for the function following AggregateManipulator patterns.
   * The method has the function's signature and source token for proper debugging.
   */
  private MethodSymbol createSyntheticCallMethod(final FunctionSymbol functionSymbol) {
    // Create _call method with function as parent scope (making function act like a class)
    final var callMethod = new MethodSymbol("_call", functionSymbol);
    
    // Set up method properties following AggregateManipulator patterns
    callMethod.setParsedModule(functionSymbol.getParsedModule());
    callMethod.setAccessModifier("public");
    callMethod.setMarkedPure(functionSymbol.isMarkedPure());
    callMethod.setSynthetic(true);
    
    // Use function's source token for debugging - debugger will go to function definition
    callMethod.setSourceToken(functionSymbol.getSourceToken());
    callMethod.setInitialisedBy(functionSymbol.getSourceToken());
    
    // Copy function's parameters to the _call method
    final var functionParams = functionSymbol.getSymbolsForThisScope();
    for (final var param : functionParams) {
      if (param instanceof VariableSymbol varSymbol) {
        final var methodParam = varSymbol.clone(callMethod);
        callMethod.define(methodParam);
      }
    }
    
    // Copy function's return type to the _call method
    if (functionSymbol.isReturningSymbolPresent()) {
      final var functionReturnSymbol = functionSymbol.getReturningSymbol();
      final var methodReturnSymbol = functionReturnSymbol.clone(callMethod);
      callMethod.setReturningSymbol(methodReturnSymbol);
    } else {
      // Set return type from function's type if no explicit returning symbol
      functionSymbol.getType().ifPresent(callMethod::setType);
    }
    
    return callMethod;
  }
}