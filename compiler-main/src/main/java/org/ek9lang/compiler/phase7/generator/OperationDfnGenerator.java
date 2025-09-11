package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.ir.CallMetaDataExtractor;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Deals with the generation of the IR for OperationDetails.
 * This is used quite widely by many of the constructs defined in the grammar.
 * methods, operators, etc. In various construct types.
 * <p>
 * But the context for this is only for methods/operators that do actually have processing defined.
 * Note, that in Ek9 it is possible to use the 'default' reserved word to get EK9 to generate the
 * appropriate functionality. For example '?' _isSet can be auto generated if the properties on the aggregate
 * have the '?' _isSet Operator.
 * </p>
 * <p>
 * This processing does NOT deal with that.
 * </p>
 */
public final class OperationDfnGenerator implements BiConsumer<Operation, EK9Parser.OperationDetailsContext> {

  private final IRGenerationContext stackContext;
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();

  public OperationDfnGenerator(final IRGenerationContext stackContext) {
    AssertValue.checkNotNull("Stack context cannot be null", stackContext);
    this.stackContext = stackContext;
  }

  @Override
  public void accept(final Operation operation, final EK9Parser.OperationDetailsContext ctx) {

    // Use stack context for block-level coordination (inherits method's IRContext)
    // Don't create a new scope here - let the instruction block create its own scope

    final var allInstructions = new ArrayList<IRInstr>();
    String returnScopeId = null;

    // Add constructor initialization logic if this is a constructor
    if (operation.getSymbol() instanceof MethodSymbol method && method.isConstructor()) {
      //TODO Need to check if the set of instructions already has a call to super()!
      //This may mean holding and checking the instruction block is separate instructions.
      //Then looking to see if a super(...) was actually called, if not then do this below.

      //If so we just need that by itself and not generate this.
      allInstructions.addAll(generateConstructorInitialization(method));
    }

    // Process in correct order: parameters -> returns -> body

    // 1. Process incoming parameters first (-> arg0 as String)
    if (ctx.argumentParam() != null) {
      allInstructions.addAll(processArgumentParam(ctx.argumentParam()));
    }

    // 2. Process return parameters second (<- rtn as String: String())  
    if (ctx.returningParam() != null) {
      final var returnResult = processReturningParamWithScope(ctx.returningParam());
      allInstructions.addAll(returnResult.instructions());
      returnScopeId = returnResult.scopeId();
    }

    // 3. Process instruction block last (function body)
    if (ctx.instructionBlock() != null) {
      allInstructions.addAll(processInstructionBlock(ctx.instructionBlock()));
    }

    // 4. Add return statement based on function signature
    allInstructions.addAll(generateReturnStatement(operation, returnScopeId));

    // Create BasicBlock with all instructions
    final var basicBlock = new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL));
    basicBlock.addInstructions(allInstructions);
    operation.setBody(basicBlock);

    // No exitScope() needed since we didn't enter a scope
  }

  /**
   * Process argument parameters (incoming parameters like -> arg0 as String).
   * These are variable-only declarations that get allocated for the caller to populate.
   */
  private List<IRInstr> processArgumentParam(final EK9Parser.ArgumentParamContext ctx) {
    final var instructions = new ArrayList<IRInstr>();

    // Use stack context infrastructure
    var instructionBuilder = new IRInstructionBuilder(stackContext);
    final var variableCreator = new VariableOnlyDeclInstrGenerator(instructionBuilder);
    final var scopeId = stackContext.generateScopeId(IRConstants.PARAM_SCOPE);

    for (final var varOnlyCtx : ctx.variableOnlyDeclaration()) {
      instructions.addAll(variableCreator.apply(varOnlyCtx, scopeId));
    }

    return instructions;
  }

  /**
   * Result record for return parameter processing including scope tracking.
   */
  private record ReturnParamResult(List<IRInstr> instructions, String scopeId) {
  }

  /**
   * Process returning parameters with scope tracking (<- rtn as String: String()).
   * These can be variable declarations (with initialization) or variable-only declarations.
   */
  private ReturnParamResult processReturningParamWithScope(final EK9Parser.ReturningParamContext ctx) {
    final var instructions = new ArrayList<IRInstr>();

    // Use stack context infrastructure
    var instructionBuilder = new IRInstructionBuilder(stackContext);

    // Use current stack-based IRContext for proper counter isolation
    final var variableCreator = new VariableDeclInstrGenerator(stackContext);
    final var variableOnlyCreator = new VariableOnlyDeclInstrGenerator(instructionBuilder);
    final var scopeId = stackContext.generateScopeId(IRConstants.RETURN_SCOPE);

    final var debugInfo = stackContext.createDebugInfo(ctx.start);
    // Enter scope for return parameter memory management
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Process return variable declarations
    if (ctx.variableDeclaration() != null) {
      // Return variable with initialization: <- rtn as String: String()
      instructions.addAll(variableCreator.apply(ctx.variableDeclaration(), scopeId));
    } else if (ctx.variableOnlyDeclaration() != null) {
      // Return variable without initialization: <- rtn as String
      instructions.addAll(variableOnlyCreator.apply(ctx.variableOnlyDeclaration(), scopeId));
    }

    // Note: SCOPE_EXIT for return scope happens at function end in generateReturnStatement
    // Return variables live for the entire function duration

    return new ReturnParamResult(instructions, scopeId);
  }

  /**
   * Process instruction block (function body).
   * Processes block statements directly without creating an intermediate BasicBlock.
   */
  private List<IRInstr> processInstructionBlock(final EK9Parser.InstructionBlockContext ctx) {
    final var instructions = new ArrayList<IRInstr>();
    // Use current stack-based IRContext for proper counter isolation
    final var blockStatementCreator = new BlockStmtInstrGenerator(stackContext);
    final var scopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    final var debugInfo = stackContext.createDebugInfo(ctx.start);

    // Enter scope for memory management
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Process all block statements using resolved symbols
    for (final var blockStmtCtx : ctx.blockStatement()) {
      instructions.addAll(blockStatementCreator.apply(blockStmtCtx, scopeId));
    }

    // Exit scope (automatic RELEASE of all registered objects)
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));

    return instructions;
  }

  /**
   * Generate return statement based on the operation's return type.
   * If the operation returns a value, return the return variable.
   * If the operation returns void, return void.
   */
  private List<IRInstr> generateReturnStatement(final Operation operation, final String returnScopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Check if the operation has a return type
    final var operationSymbol = operation.getSymbol();
    final var operationDebugInfo = stackContext.createDebugInfo(operationSymbol.getSourceToken());
    // Exit return scope if it exists (release all return variables)
    if (returnScopeId != null) {
      instructions.add(ScopeInstr.exit(returnScopeId, operationDebugInfo));
    }

    if (operationSymbol instanceof IMayReturnSymbol mayReturnSymbol && mayReturnSymbol.isReturningSymbolPresent()) {
      final var returnSymbol = mayReturnSymbol.getReturningSymbol();
      final var returnType = symbolTypeOrException.apply(returnSymbol);

      if (!stackContext.getParsedModule().getEk9Types().ek9Void().isExactSameType(returnType)) {
        // Function returns a value - return the return variable
        final var debugInfo = stackContext.createDebugInfo(returnSymbol.getSourceToken());
        instructions.add(BranchInstr.returnValue(returnSymbol.getName(), debugInfo));
      } else {
        // Function returns void or has no explicit return type
        instructions.add(BranchInstr.returnVoid(operationDebugInfo));
      }
    } else {
      // No return symbol - treat as void return
      instructions.add(BranchInstr.returnVoid(operationDebugInfo));
    }

    return instructions;
  }

  /**
   * Generate constructor initialization sequence:
   * 1. Call super constructor (if applicable)
   * 2. Call own class's i_init method
   * This ensures consistent initialization for both explicit and synthetic constructors.
   */
  private List<IRInstr> generateConstructorInitialization(final MethodSymbol constructorSymbol) {

    //TODO what if the ek9 developer has already included a call to the super?
    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(constructorSymbol.getSourceToken());
    final var aggregateSymbol = (AggregateSymbol) constructorSymbol.getParentScope();

    // 1. Call super constructor if this class explicitly extends another class
    final var superAggregateOpt = aggregateSymbol.getSuperAggregate();
    if (superAggregateOpt.isPresent()) {
      final var superSymbol = superAggregateOpt.get();

      // Only make super call if it's not the implicit base class (like Object)  
      if (isNotImplicitSuperClass(superSymbol)) {
        // Try to find constructor symbol in superclass for metadata
        final var metaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
        final var constructorSymbolOpt =
            superSymbol.resolve(new org.ek9lang.compiler.search.SymbolSearch(superSymbol.getName()));
        final var metaData = constructorSymbolOpt.isPresent() ? metaDataExtractor.apply(constructorSymbolOpt.get()) :
            CallMetaData.defaultMetaData();

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
    final var metaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
    final var iInitMethodOpt =
        aggregateSymbol.resolve(new org.ek9lang.compiler.search.SymbolSearch(IRConstants.I_INIT_METHOD));
    final var iInitMetaData = iInitMethodOpt.isPresent() ? metaDataExtractor.apply(iInitMethodOpt.get()) :
        CallMetaData.defaultMetaData();

    final var iInitCallDetails = new CallDetails(
        IRConstants.THIS, // Target this object
        aggregateSymbol.getFullyQualifiedName(),
        IRConstants.I_INIT_METHOD,
        java.util.List.of(), // No parameters
        "org.ek9.lang::Void", // Return type
        java.util.List.of(), // No arguments
        iInitMetaData
    );
    instructions.add(CallInstr.call(IRConstants.TEMP_I_INIT, debugInfo, iInitCallDetails));

    return instructions;
  }

  /**
   * Check if the super class is not an implicit base class.
   * This logic should match ClassDfnGenerator.isNotImplicitSuperClass()
   */
  private boolean isNotImplicitSuperClass(final IAggregateSymbol superSymbol) {
    //TODO refactor to reusable function.
    return !stackContext.getParsedModule().getEk9Types().ek9Any().isExactSameType(superSymbol);
  }
}
