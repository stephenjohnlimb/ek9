package org.ek9lang.compiler.phase7.generator;

import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.ir.CallMetaDataExtractor;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Acts as base for most generators as they all require the context and in most cases
 * need to generate debug information.
 * <p>
 * Uses stack-based IRGenerationContext for state management, eliminating parameter threading.
 * </p>
 */
abstract class AbstractGenerator {
  protected final IRGenerationContext stackContext;
  protected final DebugInfoCreator debugInfoCreator;
  protected final IRInstructionBuilder instructionBuilder;

  /**
   * Constructor accepting only IRGenerationContext - the single source of state.
   */
  AbstractGenerator(final IRGenerationContext stackContext) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", stackContext);
    this.stackContext = stackContext;
    this.debugInfoCreator = new DebugInfoCreator(stackContext.getCurrentIRContext());
    this.instructionBuilder = new IRInstructionBuilder(stackContext);
  }


  protected ISymbol getRecordedSymbolOrException(ParseTree node) {
    final var rtn = stackContext.getParsedModule().getRecordedSymbol(node);
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
    final var metaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
    return symbol != null ? metaDataExtractor.apply(symbol) : CallMetaData.defaultMetaData();
  }

}
