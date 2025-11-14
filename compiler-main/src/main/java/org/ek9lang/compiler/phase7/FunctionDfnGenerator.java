package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.FieldCreator;
import org.ek9lang.compiler.phase7.support.FieldsFromCapture;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
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
final class FunctionDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.FunctionDeclarationContext, IRConstruct> {

  /**
   * Constructor using only stack context - the single source of state.
   */
  FunctionDfnGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  @Override
  public IRConstruct apply(final EK9Parser.FunctionDeclarationContext ctx) {
    // Use stack context for function-level coordination
    var debugInfo = stackContext.createDebugInfo(ctx);
    stackContext.enterScope("function-generation", debugInfo, IRFrameType.FUNCTION);

    final var symbol = getParsedModule().getRecordedSymbol(ctx);

    if (symbol instanceof FunctionSymbol functionSymbol
        && (symbol.getGenus() == SymbolGenus.FUNCTION || symbol.getGenus() == SymbolGenus.FUNCTION_TRAIT)) {
      final var sourceFileName = getParsedModule().getSource().getRelativeFileName();
      final var construct = new IRConstruct(symbol, sourceFileName);

      // Create field declarations from symbol table
      createFieldDeclarations(construct, functionSymbol);

      // Create three-phase initialization operations
      createInitializationOperations(construct, functionSymbol);

      // Create OperationInstr for the function itself
      createConstructorOperation(construct, functionSymbol);
      createCallOperation(construct, functionSymbol, ctx);

      stackContext.exitScope();
      return construct;
    }
    throw new CompilerException("Cannot create Function - expect FunctionSymbol of FUNCTION Genus");
  }

  private void createFieldDeclarations(final IRConstruct construct, final FunctionSymbol functionSymbol) {

    final var debugInfoCreator = createDebugInfoCreator();
    final var fieldCreator = new FieldCreator(construct, debugInfoCreator);
    final var fieldsFromCapture = new FieldsFromCapture(fieldCreator);

    fieldsFromCapture.accept(functionSymbol);
  }

  private void createInitializationOperations(final IRConstruct construct, final FunctionSymbol functionSymbol) {

    // Create c_init operation for function/static initialization
    final ISymbol superType = functionSymbol.getSuperFunction().orElse(null);
    createInitOperation(construct, functionSymbol, superType);
    createInstanceInitOperation(construct, functionSymbol, null);
  }

  private void createConstructorOperation(final IRConstruct construct, final FunctionSymbol functionSymbol) {

    // Constructor method coordination
    final var constructorMethod = createSyntheticFunctionConstructorMethod(functionSymbol);
    var debugInfo = stackContext.createDebugInfo(constructorMethod.getSourceToken());
    stackContext.enterMethodScope("constructor", debugInfo, IRFrameType.METHOD);

    final IScopedSymbol superType = functionSymbol.getSuperFunction().orElse(null);
    processSyntheticConstructor(construct, constructorMethod, superType);
    stackContext.exitScope();
  }

  private void createCallOperation(final IRConstruct construct, final FunctionSymbol functionSymbol,
                                   final EK9Parser.FunctionDeclarationContext ctx) {

    //TODO IMPORTANT - need the actual capture ctx to get the naming.
    //Now for the synthetic constructor there is not context, in effect we need to call i_init
    //Then for Dynamic functions (we'd be using the capture details, because that has all we need).
    //In fact we MUST use the capture details, because it is possible to use a different name in the
    //capture the the actual variable being captured.

    // Create synthetic _call method following AggregateManipulator patterns
    final var callMethod = createSyntheticCallMethod(functionSymbol);
    final var callDebugInfo = stackContext.createDebugInfo(callMethod.getSourceToken());
    // Create OperationInstr for the synthetic _call method (not the function itself)
    final var callOperation = new OperationInstr(callMethod, callDebugInfo);

    // For abstract functions, only create the method signature without body
    if (!functionSymbol.isMarkedAbstract()) {
      // _call method coordination
      var debugInfo2 = stackContext.createDebugInfo(callMethod.getSourceToken());
      stackContext.enterMethodScope("_call", debugInfo2, IRFrameType.METHOD);

      // Process executable content using OperationDfnGenerator if operationDetails is present
      if (ctx.operationDetails() != null) {
        operationDfnGenerator.accept(callOperation, ctx.operationDetails());
      }

      stackContext.exitScope();
    }

    construct.add(callOperation);
  }


  private MethodSymbol createSyntheticFunctionConstructorMethod(final FunctionSymbol functionSymbol) {
    final var constructorMethod = new MethodSymbol(functionSymbol.getName(), functionSymbol);

    constructorMethod.setParsedModule(functionSymbol.getParsedModule());
    constructorMethod.setAccessModifier(IRConstants.PUBLIC);
    constructorMethod.setMarkedPure(functionSymbol.isMarkedPure());
    constructorMethod.setSynthetic(true);

    // Use function's source token for debugging - debugger will go to function definition
    constructorMethod.setSourceToken(functionSymbol.getSourceToken());
    constructorMethod.setInitialisedBy(functionSymbol.getSourceToken());

    //We don't copy over the parameters for the constructor, they are used for the _call.

    // Set return type from function's type
    functionSymbol.getType().ifPresent(constructorMethod::setType);

    return constructorMethod;
  }

  /**
   * Creates a synthetic _call method for the function following AggregateManipulator patterns.
   * The method has the function's signature and source token for proper debugging.
   */
  private MethodSymbol createSyntheticCallMethod(final FunctionSymbol functionSymbol) {
    // Create _call method with function as parent scope (making function act like a class)
    final var callMethod = new MethodSymbol(IRConstants.CALL_METHOD, functionSymbol);

    // Set up method properties following AggregateManipulator patterns
    callMethod.setParsedModule(functionSymbol.getParsedModule());
    callMethod.setAccessModifier(IRConstants.PUBLIC);
    callMethod.setMarkedPure(functionSymbol.isMarkedPure());
    callMethod.setMarkedAbstract(functionSymbol.isMarkedAbstract());
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