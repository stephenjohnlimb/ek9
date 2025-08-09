package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;

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
    final var basicBlock = new BasicBlockInstr(context.generateBlockLabel("entry"));
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
    final var scopeId = context.generateScopeId("param");

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
    final var scopeId = context.generateScopeId("return");

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
    final var blockStatementCreator = new BlockStatementInstrGenerator(context);
    final var scopeId = context.generateScopeId("scope");

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
}
