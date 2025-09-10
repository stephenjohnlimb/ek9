package org.ek9lang.compiler.phase7;

import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.ir.CallMetaDataExtractor;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.IRInstructionBuilder;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Acts as base for most generators as they all require the context and in most cases
 * need to generate debug information.
 * <p>
 * ENHANCED: Now provides access to stack-based IRInstructionBuilder for migration support.
 * </p>
 */
abstract class AbstractGenerator {
  protected final IRContext context;
  protected final DebugInfoCreator debugInfoCreator;
  protected final IRInstructionBuilder instructionBuilder;

  AbstractGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);

    // Create IRInstructionBuilder for stack-based IR generation support
    // Pass IRContext directly to preserve state and avoid unnecessary object creation
    var generationContext = new org.ek9lang.compiler.phase7.support.IRGenerationContext(context);
    this.instructionBuilder = new org.ek9lang.compiler.phase7.support.IRInstructionBuilder(generationContext);
  }

  protected ISymbol getRecordedSymbolOrException(ParseTree node) {
    final var rtn = context.getParsedModule().getRecordedSymbol(node);
    AssertValue.checkNotNull("Symbol should be resolved by phases 1-6",
        rtn);
    AssertValue.checkTrue("Symbol must have been given a type by phase 7",
        rtn.getType().isPresent());
    return rtn;
  }

  /**
   * Extract call metadata from a symbol using the common pattern.
   * This consolidates the frequent pattern of creating CallMetaDataExtractor and applying it.
   */
  protected CallMetaData extractCallMetaData(final ISymbol symbol) {
    final var metaDataExtractor = new CallMetaDataExtractor(context.getParsedModule().getEk9Types());
    return symbol != null ? metaDataExtractor.apply(symbol) : CallMetaData.defaultMetaData();
  }

}
