package org.ek9lang.compiler.phase7.support;

import java.util.function.Function;
import org.ek9lang.compiler.ir.DebugInfo;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Creates the debug information for the symbol provided. Filename, line number and position.
 * This helps when translating the IR into a real back end to debug information can be incorporated
 * in to the final output.
 */
public final class DebugInfoCreator implements Function<IToken, DebugInfo> {

  private final IRContext context;

  public DebugInfoCreator(final IRContext context) {
    AssertValue.checkNotNull("Context cannot be Null", context);
    this.context = context;
  }

  @Override
  public DebugInfo apply(final IToken token) {
    return context.getCompilerFlags().isDebuggingInstrumentation()
        ? DebugInfo.from(context.getParsedModule().getSource(), token) : null;
  }
}
