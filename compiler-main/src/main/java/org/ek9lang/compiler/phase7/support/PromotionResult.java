package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.ir.IRInstr;

/**
 * Result of parameter promotion processing.
 * Contains the promoted argument variables and any IR instructions needed to perform the promotions.
 */
public record PromotionResult(
    List<String> promotedArguments,
    List<IRInstr> promotionInstructions
) {

  /**
   * Check if any promotions were needed.
   */
  public boolean hasPromotions() {
    return !promotionInstructions.isEmpty();
  }

  /**
   * Get the total number of parameters that required promotion.
   */
  public int getPromotionCount() {
    return promotionInstructions.size();
  }
}