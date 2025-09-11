package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.FieldCreator;
import org.ek9lang.compiler.phase7.support.FieldsFromCapture;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.NotImplicitSuper;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.FunctionSymbol;
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

  private final NotImplicitSuper notImplicitSuper = new NotImplicitSuper();

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

    if (symbol instanceof FunctionSymbol functionSymbol && symbol.getGenus() == SymbolGenus.FUNCTION) {
      final var construct = new IRConstruct(symbol);

      // Create field declarations from symbol table
      createFieldDeclarations(construct, functionSymbol);

      // Create three-phase initialization operations
      createInitializationOperations(construct, functionSymbol, ctx);

      // Create OperationInstr for the function itself
      createOperation(construct, functionSymbol, ctx);

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

  private void createInitializationOperations(final IRConstruct construct, final FunctionSymbol functionSymbol,
                                              final EK9Parser.FunctionDeclarationContext ctx) {

    // Create c_init operation for function/static initialization
    final ISymbol superType = functionSymbol.getSuperFunction().orElse(null);
    createInitOperation(construct, functionSymbol, superType);
    createInstanceInitOperation(construct, functionSymbol);
  }

  private void createInitOperation(final IRConstruct construct,
                                   final FunctionSymbol functionSymbol,
                                   ISymbol superType) {
    // Create a synthetic method symbol for c_init is when the class/construct definition is actually loaded.
    final var cInitOperation = newSyntheticInitOperation(functionSymbol, IRConstants.C_INIT_METHOD);

    stackContext.enterMethodScope("c_init", cInitOperation.getDebugInfo(), IRFrameType.METHOD);

    // Generate c_init body
    final var allInstructions = new java.util.ArrayList<IRInstr>();

    // Call super class c_init if this class explicitly extends another class
    if (superType != null && notImplicitSuper.test(superType)) {

      final var metaData = CallMetaDataDetails.defaultMetaData();
      final var callDetails = new CallDetails(
          null, // No target object for static call
          superType.getFullyQualifiedName(),
          IRConstants.C_INIT_METHOD,
          java.util.List.of(), // No parameters
          voidStr, // Return type
          java.util.List.of(), // No arguments
          metaData
      );
      allInstructions.add(CallInstr.callStatic(IRConstants.TEMP_C_INIT, null, callDetails));
    }


    allInstructions.add(BranchInstr.returnVoid());

    // Create BasicBlock with all instructions - use stack context for consistent labeling
    final var basicBlock = new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL));
    basicBlock.addInstructions(allInstructions);
    cInitOperation.setBody(basicBlock);

    construct.add(cInitOperation);
    stackContext.exitScope();
  }

  private void createInstanceInitOperation(final IRConstruct construct,
                                           final FunctionSymbol functionSymbol) {
    final var iInitOperation = newSyntheticInitOperation(functionSymbol, IRConstants.I_INIT_METHOD);

    // Use stack context for method-level coordination with fresh IRContext
    var debugInfo = stackContext.createDebugInfo(functionSymbol.getSourceToken());
    stackContext.enterMethodScope("i_init", debugInfo, IRFrameType.METHOD);

    // Generate i_init body
    final var allInstructions = new java.util.ArrayList<IRInstr>();

    //TODO Now it is possible that there are captured variables.
    //The fields will have been declared, but now they need to be initialised.
    //TODO this we really need the dynamic function ctx. Leave this for now and restructure the code to
    // functions.

    allInstructions.add(BranchInstr.returnVoid());
    // Create BasicBlock with all instructions - use stack context for consistent labeling
    final var basicBlock = new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL));
    basicBlock.addInstructions(allInstructions);
    iInitOperation.setBody(basicBlock);

    construct.add(iInitOperation);
    stackContext.exitScope();
  }

  private void createOperation(final IRConstruct construct, final FunctionSymbol functionSymbol,
                               final EK9Parser.FunctionDeclarationContext ctx) {

    // Constructor method coordination
    final var constructorMethod = createSyntheticFunctionConstructorMethod(functionSymbol);
    var debugInfo = stackContext.createDebugInfo(constructorMethod.getSourceToken());
    stackContext.enterMethodScope("constructor", debugInfo, IRFrameType.METHOD);

    processSyntheticConstructor(construct, constructorMethod);
    stackContext.exitScope();

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

    // _call method coordination 
    var debugInfo2 = stackContext.createDebugInfo(callMethod.getSourceToken());
    stackContext.enterMethodScope("_call", debugInfo2, IRFrameType.METHOD);

    // Process executable content using OperationDfnGenerator if operationDetails is present
    if (ctx.operationDetails() != null) {
      operationDfnGenerator.accept(callOperation, ctx.operationDetails());
    }

    construct.add(callOperation);
    stackContext.exitScope();
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

  /**
   * Process a synthetic constructor for the function, now mapped to an aggregate form in the IR.
   * 1. Call super constructor (if not implicit base class)
   * 2. Call own class's i_init method
   * 3. Return this
   */
  private void processSyntheticConstructor(final IRConstruct construct, final MethodSymbol constructorSymbol) {
    final var debugInfo = stackContext.createDebugInfo(constructorSymbol.getSourceToken());
    final var operation = new OperationInstr(constructorSymbol, debugInfo);

    final var instructions = new java.util.ArrayList<IRInstr>();
    final var aggregateSymbol = (FunctionSymbol) constructorSymbol.getParentScope();

    // 1. Call super constructor if this class explicitly extends another class
    final var superFunctionOpt = aggregateSymbol.getSuperFunction();
    if (superFunctionOpt.isPresent()) {
      final var superSymbol = superFunctionOpt.get();

      // Only make super call if it's not the implicit base class (like Object)
      if (notImplicitSuper.test(superSymbol)) {
        // Try to find constructor symbol in super function for metadata
        final var metaDataExtractor = createCallMetaDataExtractor();
        final var constructorSymbolOpt =
            superSymbol.resolve(new SymbolSearch(superSymbol.getName()));
        final var metaData = constructorSymbolOpt.isPresent() ? metaDataExtractor.apply(constructorSymbolOpt.get()) :
            CallMetaDataDetails.defaultMetaData();

        final var callDetails = new CallDetails(
            IRConstants.SUPER, // Target super object
            superSymbol.getFullyQualifiedName(),
            superSymbol.getName(), // Constructor name matches class name
            java.util.List.of(), // No parameters for default constructor
            superSymbol.getFullyQualifiedName(), // Return type is the super class
            java.util.List.of(), // No arguments
            metaData
        );
        instructions.add(CallInstr.call(IRConstants.TEMP_SUPER_INIT, debugInfo, callDetails));
      }
    }

    // 2. Call own class's i_init method to initialize this class's fields
    // Try to find i_init method symbol for metadata
    final var metaDataExtractor = createCallMetaDataExtractor();
    final var iInitMethodOpt =
        aggregateSymbol.resolve(new SymbolSearch(IRConstants.I_INIT_METHOD));
    final var iInitMetaData = iInitMethodOpt.isPresent() ? metaDataExtractor.apply(iInitMethodOpt.get()) :
        CallMetaDataDetails.defaultMetaData();

    final var iInitCallDetails = new CallDetails(
        IRConstants.THIS, // Target this object
        aggregateSymbol.getFullyQualifiedName(),
        IRConstants.I_INIT_METHOD,
        java.util.List.of(), // No parameters
        voidStr, // Return type
        java.util.List.of(), // No arguments
        iInitMetaData
    );
    instructions.add(CallInstr.call(IRConstants.TEMP_I_INIT, debugInfo, iInitCallDetails));

    // 3. Return this
    instructions.add(BranchInstr.returnValue(IRConstants.THIS, debugInfo));

    final var basicBlock = new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL));
    basicBlock.addInstructions(instructions);
    operation.setBody(basicBlock);

    construct.add(operation);
  }
}