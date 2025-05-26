package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.Block;
import org.ek9lang.compiler.ir.Marker;
import org.ek9lang.core.AssertValue;

/**
 * Accepts an instruction block parse tree context and creates an IR version of a Block.
 */
final class BlockCreator implements Function<EK9Parser.InstructionBlockContext, Block> {
  private final ParsedModule parsedModule;
  private final BlockStatementCreator blockStatementCreator;

  public BlockCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.blockStatementCreator = new BlockStatementCreator(parsedModule);
  }

  @Override
  public Block apply(final EK9Parser.InstructionBlockContext ctx) {
    final var scope = parsedModule.getRecordedScope(ctx);
    AssertValue.checkNotNull("Scope cannot be null", scope);
    AssertValue.checkNotNull("Start cannot be null", ctx.start);
    final var start = ctx.start;
    final var stop = ctx.stop;
    var block = new Block(scope,
        getStart(start.getLine(), start.getStartIndex()),
        getEnd(stop.getLine(), stop.getStopIndex()));

    ctx.blockStatement().stream().map(blockStatementCreator).forEach(block::add);

    return block;
  }

  private Marker getStart(final int lineNo, final int start) {
    return new Marker("B-" + lineNo + "-" + start);
  }

  private Marker getEnd(final int lineNo, final int end) {
    return new Marker("B-" + lineNo + "-" + end);

  }
}
