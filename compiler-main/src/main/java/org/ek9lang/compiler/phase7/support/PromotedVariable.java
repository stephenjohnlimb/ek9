package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.ir.IRInstr;

/**
 * Represents a variable that has been promoted using the #^ operator.
 * Contains both the promoted variable name and the IR instructions needed to perform the promotion.
 */
public record PromotedVariable(
    String variable,
    List<IRInstr> instructions
) {

  /**
   * Create a promoted variable result.
   */
  public static PromotedVariable of(String variable, List<IRInstr> instructions) {
    return new PromotedVariable(variable, instructions);
  }

  /**
   * Create a non-promoted variable (direct use, no promotion needed).
   */
  public static PromotedVariable direct(String variable) {
    return new PromotedVariable(variable, List.of());
  }

}