package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

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
final class OperationDfnGenerator implements BiConsumer<Operation, EK9Parser.OperationDetailsContext> {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;

  OperationDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }

  @Override
  public void accept(final Operation operation, final EK9Parser.OperationDetailsContext ctx) {

    final var context = new IRContext(parsedModule, compilerFlags);
    final var allInstructions = new ArrayList<IRInstr>();
    String returnScopeId = null;

    // Add constructor initialization logic if this is a constructor
    if (operation.getSymbol() instanceof MethodSymbol method && method.isConstructor()) {
      //TODO Need to check if the set of instructions already has a call to super()!
      //If so we just need that by itself and not generate this.
      allInstructions.addAll(generateConstructorInitialization(method, context));
    }

    // Process in correct order: parameters -> returns -> body

    // 1. Process incoming parameters first (-> arg0 as String)
    if (ctx.argumentParam() != null) {
      allInstructions.addAll(processArgumentParam(ctx.argumentParam(), context));
    }

    // 2. Process return parameters second (<- rtn as String: String())  
    if (ctx.returningParam() != null) {
      final var returnResult = processReturningParamWithScope(ctx.returningParam(), context);
      allInstructions.addAll(returnResult.instructions());
      returnScopeId = returnResult.scopeId();
    }

    // 3. Process instruction block last (function body)
    if (ctx.instructionBlock() != null) {
      allInstructions.addAll(processInstructionBlock(ctx.instructionBlock(), context));
    }

    // 4. Add return statement based on function signature
    allInstructions.addAll(generateReturnStatement(operation, context, returnScopeId));

    // Create BasicBlock with all instructions
    final var basicBlock = new BasicBlockInstr(context.generateBlockLabel(IRConstants.ENTRY_LABEL));
    basicBlock.addInstructions(allInstructions);
    operation.setBody(basicBlock);
  }

  /**
   * Process argument parameters (incoming parameters like -> arg0 as String).
   * These are variable-only declarations that get allocated for the caller to populate.
   */
  private List<IRInstr> processArgumentParam(final EK9Parser.ArgumentParamContext ctx, final IRContext context) {
    final var instructions = new ArrayList<IRInstr>();
    final var variableCreator = new VariableOnlyDeclInstrGenerator(context);
    final var scopeId = context.generateScopeId(IRConstants.PARAM_SCOPE);

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
  private ReturnParamResult processReturningParamWithScope(final EK9Parser.ReturningParamContext ctx,
                                                           final IRContext context) {
    final var instructions = new ArrayList<IRInstr>();
    final var variableCreator = new VariableDeclInstrGenerator(context);
    final var variableOnlyCreator = new VariableOnlyDeclInstrGenerator(context);
    final var scopeId = context.generateScopeId(IRConstants.RETURN_SCOPE);

    // Enter scope for return parameter memory management
    instructions.add(ScopeInstr.enter(scopeId));

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
  private List<IRInstr> processInstructionBlock(final EK9Parser.InstructionBlockContext ctx, final IRContext context) {
    final var instructions = new ArrayList<IRInstr>();
    final var blockStatementCreator = new BlockStmtInstrGenerator(context);
    final var scopeId = context.generateScopeId(IRConstants.GENERAL_SCOPE);

    // Enter scope for memory management
    instructions.add(ScopeInstr.enter(scopeId));

    // Process all block statements using resolved symbols
    for (final var blockStmtCtx : ctx.blockStatement()) {
      instructions.addAll(blockStatementCreator.apply(blockStmtCtx, scopeId));
    }

    // Exit scope (automatic RELEASE of all registered objects)
    instructions.add(ScopeInstr.exit(scopeId));

    return instructions;
  }

  /**
   * Generate return statement based on the operation's return type.
   * If the operation returns a value, return the return variable.
   * If the operation returns void, return void.
   */
  private List<IRInstr> generateReturnStatement(final Operation operation, final IRContext context,
                                                final String returnScopeId) {
    final var instructions = new ArrayList<IRInstr>();
    final var debugInfoCreator = new DebugInfoCreator(context);

    // Exit return scope if it exists (release all return variables)
    if (returnScopeId != null) {
      instructions.add(ScopeInstr.exit(returnScopeId));
    }

    // Check if the operation has a return type
    final var operationSymbol = operation.getSymbol();
    if (operationSymbol instanceof IMayReturnSymbol mayReturnSymbol && mayReturnSymbol.isReturningSymbolPresent()) {
      final var returnSymbol = mayReturnSymbol.getReturningSymbol();
      final var returnType = returnSymbol.getType().orElse(null);

      if (returnType != null && !context.getParsedModule().getEk9Types().ek9Void().isExactSameType(returnType)) {
        // Function returns a value - return the return variable
        final var debugInfo = debugInfoCreator.apply(returnSymbol);
        instructions.add(BranchInstr.returnValue(returnSymbol.getName(), debugInfo));
      } else {
        // Function returns void or has no explicit return type
        instructions.add(BranchInstr.returnVoid());
      }
    } else {
      // No return symbol - treat as void return
      instructions.add(BranchInstr.returnVoid());
    }

    return instructions;
  }

  /**
   * Generate constructor initialization sequence:
   * 1. Call super constructor (if applicable)
   * 2. Call own class's i_init method
   * This ensures consistent initialization for both explicit and synthetic constructors.
   */
  private List<IRInstr> generateConstructorInitialization(final MethodSymbol constructorSymbol,
                                                          final IRContext context) {

    //TODO what if the ek9 developer has already included a call to the super?
    final var instructions = new ArrayList<IRInstr>();
    final var debugInfoCreator = new DebugInfoCreator(context);
    final var debugInfo = debugInfoCreator.apply(constructorSymbol);
    final var aggregateSymbol = (AggregateSymbol) constructorSymbol.getParentScope();

    // 1. Call super constructor if this class explicitly extends another class
    final var superAggregateOpt = aggregateSymbol.getSuperAggregate();
    if (superAggregateOpt.isPresent()) {
      final var superSymbol = superAggregateOpt.get();

      // Only make super call if it's not the implicit base class (like Object)  
      if (isNotImplicitSuperClass(superSymbol)) {
        final var callDetails = new CallDetails(
            IRConstants.SUPER, // Target super object
            superSymbol.getFullyQualifiedName(),
            superSymbol.getName(), // Constructor name matches class name
            java.util.List.of(), // No parameters for default constructor
            superSymbol.getFullyQualifiedName(), // Return type is the super class
            java.util.List.of() // No arguments
        );
        instructions.add(CallInstr.call(IRConstants.TEMP_SUPER_INIT, debugInfo, callDetails));
      }
    }

    // 2. Call own class's i_init method to initialize this class's fields
    final var iInitCallDetails = new CallDetails(
        IRConstants.THIS, // Target this object
        aggregateSymbol.getFullyQualifiedName(),
        IRConstants.I_INIT_METHOD,
        java.util.List.of(), // No parameters
        "org.ek9.lang::Void", // Return type
        java.util.List.of() // No arguments
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
    return !parsedModule.getEk9Types().ek9Any().isExactSameType(superSymbol);
  }
}
