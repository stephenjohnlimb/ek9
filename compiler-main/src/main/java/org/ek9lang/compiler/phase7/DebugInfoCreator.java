package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.compiler.ir.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

final class DebugInfoCreator implements Function<ISymbol, DebugInfo> {

  private final IRContext context;

  DebugInfoCreator(final IRContext context) {
    AssertValue.checkNotNull("Context cannot be Null", context);
    this.context = context;
  }

  @Override
  public DebugInfo apply(final ISymbol symbol) {
    return context.getCompilerFlags().isDebuggingInstrumentation()
        ? DebugInfo.from(context.getParsedModule().getSource(), symbol) : null;
  }
}
