package org.ek9lang.compiler.phase7;

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
}
