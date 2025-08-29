package org.ek9lang.compiler.phase7;

import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Acts as base for most generators as they all require the context and in most cases
 * need to generate debug information.
 */
abstract class AbstractGenerator {
  protected final IRContext context;
  protected final DebugInfoCreator debugInfoCreator;

  AbstractGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  protected ISymbol getRecordedSymbolOrException(ParseTree node) {
    final var rtn = context.getParsedModule().getRecordedSymbol(node);
    AssertValue.checkNotNull("Symbol should be resolved by phases 1-6",
        rtn);
    AssertValue.checkTrue("Symbol must have been given a type by phase 7",
        rtn.getType().isPresent());
    return rtn;
  }

}
