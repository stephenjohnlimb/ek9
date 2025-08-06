package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.compiler.ir.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

class DebugInfoCreator implements Function<ISymbol, DebugInfo> {

  private final IRGenerationContext context;

  public DebugInfoCreator(final IRGenerationContext context) {
    AssertValue.checkNotNull("Context cannot be Null", context);
    this.context = context;
  }

  @Override
  public DebugInfo apply(final ISymbol iSymbol) {
    return context.getCompilerFlags().isDebuggingInstrumentation()
        ? DebugInfo.from(context.getParsedModule().getSource(), iSymbol) : null;
  }
}
