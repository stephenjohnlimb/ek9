package org.ek9lang.compiler.phase7.helpers;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;

/**
 * Handles IR generation for method and function calls.
 *
 * <p>This helper consolidates call generation that was previously
 * handled by CallInstrGenerator and related classes. Uses stack-based
 * context to eliminate parameter threading.</p>
 */
public class CallIRHelper extends AbstractIRHelper {

  public CallIRHelper(IRInstructionBuilder instructionBuilder) {
    super(instructionBuilder);
  }

  /**
   * Generate IR for a call expression.
   */
  public void generateFor(EK9Parser.CallContext ctx) {
    //Will generate
  }
}