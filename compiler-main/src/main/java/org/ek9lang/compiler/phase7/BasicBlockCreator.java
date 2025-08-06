package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.BasicBlock;
import org.ek9lang.compiler.ir.ScopeInstruction;
import org.ek9lang.core.AssertValue;

/**
 * Creates BasicBlock IR from EK9 instruction block contexts.
 * Follows the Creator pattern used throughout the EK9 compiler phase7 package.
 */
public final class BasicBlockCreator implements Function<EK9Parser.InstructionBlockContext, BasicBlock> {

  private final IRGenerationContext context;
  private final BlockStatementInstructionCreator blockStatementCreator;

  public BasicBlockCreator(final IRGenerationContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.blockStatementCreator = new BlockStatementInstructionCreator(context);
  }

  @Override
  public BasicBlock apply(final EK9Parser.InstructionBlockContext ctx) {
    AssertValue.checkNotNull("InstructionBlockContext cannot be null", ctx);

    final var blockLabel = context.generateBlockLabel("block");
    final var scopeId = context.generateScopeId("scope");
    final var block = new BasicBlock(blockLabel);

    // Enter scope for memory management
    block.addInstruction(ScopeInstruction.enter(scopeId));

    // Process all block statements using resolved symbols
    for (final var blockStmtCtx : ctx.blockStatement()) {
      final var instructions = blockStatementCreator.apply(blockStmtCtx, scopeId);
      block.addInstructions(instructions);
    }

    // Exit scope (automatic RELEASE of all registered objects)
    block.addInstruction(ScopeInstruction.exit(scopeId));

    return block;
  }
}