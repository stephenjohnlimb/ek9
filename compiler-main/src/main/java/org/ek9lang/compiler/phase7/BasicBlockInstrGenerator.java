package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.tokenizer.Ek9Token;
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

  BasicBlockInstrGenerator(final IRContext context) {
    super(context);
    this.blockStatementCreator = new BlockStmtInstrGenerator(context);
  }

  @Override
  public BasicBlockInstr apply(final EK9Parser.InstructionBlockContext ctx) {
    AssertValue.checkNotNull("InstructionBlockContext cannot be null", ctx);

    final var debugInfoCreator = new DebugInfoCreator(context);
    final var blockLabel = context.generateBlockLabel("block");
    final var scopeId = context.generateScopeId(IRConstants.GENERAL_SCOPE);
    final var block = new BasicBlockInstr(blockLabel);
    final var debugInfo = debugInfoCreator.apply(new Ek9Token(ctx.start));

    // Enter scope for memory management
    block.addInstruction(ScopeInstr.enter(scopeId, debugInfo));

    // Process all block statements using resolved symbols
    for (final var blockStmtCtx : ctx.blockStatement()) {
      final var instructions = blockStatementCreator.apply(blockStmtCtx, scopeId);
      block.addInstructions(instructions);
    }

    // Exit scope (automatic RELEASE of all registered objects)
    block.addInstruction(ScopeInstr.exit(scopeId, debugInfo));

    return block;
  }
}