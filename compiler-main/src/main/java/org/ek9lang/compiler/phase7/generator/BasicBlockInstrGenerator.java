package org.ek9lang.compiler.phase7.generator;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.core.AssertValue;

/**
 * Creates BasicBlock IR from EK9 instruction block contexts.
 * Follows the Creator pattern used throughout the EK9 compiler phase7 package.
 * <p>
 * For the following ANTLR grammar.
 * </p>
 * <pre>
 *   instructionBlock
 *     : (directive? blockStatement NL+)+
 *     ;
 * </pre>
 */
final class BasicBlockInstrGenerator extends AbstractGenerator
    implements Function<EK9Parser.InstructionBlockContext, BasicBlockInstr> {

  private final BlockStmtInstrGenerator blockStatementCreator;

  BasicBlockInstrGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
    this.blockStatementCreator = new BlockStmtInstrGenerator(stackContext);
  }

  @Override
  public BasicBlockInstr apply(final EK9Parser.InstructionBlockContext ctx) {
    AssertValue.checkNotNull("InstructionBlockContext cannot be null", ctx);

    final var blockLabel = stackContext.generateBlockLabel("block");
    final var scopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    final var block = new BasicBlockInstr(blockLabel);
    final var debugInfo = stackContext.createDebugInfo(ctx.start);

    // STACK-BASED: Push scope onto stack context for child generators to access
    stackContext.enterScope(scopeId, debugInfo, IRFrameType.BLOCK);

    // Enter scope for memory management
    block.addInstruction(ScopeInstr.enter(scopeId, debugInfo));

    // Process all block statements - they can now use stackContext.currentScopeId()
    for (final var blockStmtCtx : ctx.blockStatement()) {
      final var instructions = blockStatementCreator.apply(blockStmtCtx);
      block.addInstructions(instructions);
    }

    // Exit scope (automatic RELEASE of all registered objects)
    block.addInstruction(ScopeInstr.exit(scopeId, debugInfo));

    // STACK-BASED: Pop scope from stack context
    stackContext.exitScope();

    return block;
  }
}