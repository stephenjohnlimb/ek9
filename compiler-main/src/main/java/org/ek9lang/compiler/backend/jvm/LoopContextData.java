package org.ek9lang.compiler.backend.jvm;

import org.objectweb.asm.Label;

/**
 * Loop-specific context data for bytecode generation.
 * Stored in BytecodeStackFrame when frameType is LOOP.
 * <p>
 * EK9 loops have no break/continue statements - they execute to completion.
 * This context provides labels for:
 * </p>
 * <ul>
 *   <li>Next iteration (continueLabel) - where nested control flow jumps after completion</li>
 *   <li>Natural loop exit (exitLabel) - where loop exits when condition fails</li>
 * </ul>
 */
public record LoopContextData(
    Label continueLabel,  // Where to jump for next iteration (loop increment/condition)
    Label exitLabel       // Where to jump when loop completes naturally
) {

  public LoopContextData {
    if (continueLabel == null) {
      throw new IllegalArgumentException("continueLabel cannot be null");
    }
    if (exitLabel == null) {
      throw new IllegalArgumentException("exitLabel cannot be null");
    }
  }

}
