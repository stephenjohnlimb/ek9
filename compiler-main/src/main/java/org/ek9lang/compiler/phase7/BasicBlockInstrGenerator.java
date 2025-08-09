package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.core.AssertValue;

/**
 * Creates BasicBlock IR from EK9 instruction block contexts.
 * Follows the Creator pattern used throughout the EK9 compiler phase7 package.
 */
final class BasicBlockInstrGenerator implements Function<EK9Parser.InstructionBlockContext, BasicBlockInstr> {

  private final IRContext context;
  private final BlockStatementInstrGenerator blockStatementCreator;

  BasicBlockInstrGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.blockStatementCreator = new BlockStatementInstrGenerator(context);
  }

  @Override
  public BasicBlockInstr apply(final EK9Parser.InstructionBlockContext ctx) {
    AssertValue.checkNotNull("InstructionBlockContext cannot be null", ctx);

    final var blockLabel = context.generateBlockLabel("block");
    final var scopeId = context.generateScopeId("scope");
    final var block = new BasicBlockInstr(blockLabel);

    // Enter scope for memory management
    block.addInstruction(ScopeInstr.enter(scopeId));

    // Process all block statements using resolved symbols
    for (final var blockStmtCtx : ctx.blockStatement()) {
      final var instructions = blockStatementCreator.apply(blockStmtCtx, scopeId);
      block.addInstructions(instructions);
    }

    // Exit scope (automatic RELEASE of all registered objects)
    block.addInstruction(ScopeInstr.exit(scopeId));

    return block;
  }
}