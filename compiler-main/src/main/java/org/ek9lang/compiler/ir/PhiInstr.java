package org.ek9lang.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.ek9lang.core.AssertValue;

/**
 * Specialized IR instruction for PHI nodes in SSA form.
 * <p>
 * PHI instructions merge values from different control flow paths,
 * essential for proper LLVM IR generation and SSA compatibility.
 * </p>
 * <p>
 * Format: PHI result = [value1, block1], [value2, block2], ...
 * </p>
 */
public final class PhiInstr extends IRInstr {

  /**
   * Represents a single value-block pair in a PHI node.
   */
  public record PhiPair(String value, String block) {
    public PhiPair {
      AssertValue.checkNotNull("Value cannot be null", value);
      AssertValue.checkNotNull("Block cannot be null", block);
    }
    
    @Override
    @Nonnull
    public String toString() {
      return "[" + value + ", " + block + "]";
    }
  }

  private final List<PhiPair> phiPairs = new ArrayList<>();

  /**
   * Create PHI instruction with result variable.
   */
  public static PhiInstr phi(final String result) {
    return new PhiInstr(result, null);
  }

  /**
   * Create PHI instruction with result variable and debug info.
   */
  public static PhiInstr phi(final String result, final DebugInfo debugInfo) {
    return new PhiInstr(result, debugInfo);
  }

  private PhiInstr(final String result, final DebugInfo debugInfo) {
    super(IROpcode.PHI, result, debugInfo);
  }

  /**
   * Add a value-block pair to this PHI node.
   */
  public PhiInstr addPhiPair(final String value, final String block) {
    AssertValue.checkNotNull("Value cannot be null", value);
    AssertValue.checkNotNull("Block cannot be null", block);
    
    phiPairs.add(new PhiPair(value, block));
    // Also add to operands for base class toString() functionality
    addOperand("[" + value + ", " + block + "]");
    return this;
  }

  /**
   * Add multiple value-block pairs to this PHI node.
   */
  public PhiInstr addPhiPairs(final PhiPair... pairs) {
    for (PhiPair pair : pairs) {
      addPhiPair(pair.value(), pair.block());
    }
    return this;
  }

  /**
   * Get all PHI pairs for this instruction.
   */
  public List<PhiPair> getPhiPairs() {
    return List.copyOf(phiPairs);
  }

  /**
   * Check if this PHI node has any pairs.
   */
  public boolean hasPairs() {
    return !phiPairs.isEmpty();
  }

  /**
   * Get the number of incoming values for this PHI node.
   */
  public int getIncomingCount() {
    return phiPairs.size();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    
    if (getResult() != null) {
      sb.append(getResult()).append(" = ");
    }
    
    sb.append("PHI");
    
    if (!phiPairs.isEmpty()) {
      sb.append(" ");
      sb.append(phiPairs.stream()
          .map(PhiPair::toString)
          .reduce((a, b) -> a + ", " + b)
          .orElse(""));
    }
    
    // Add debug information as comment if available
    if (getDebugInfo().isPresent() && getDebugInfo().get().isValidLocation()) {
      sb.append("  ").append(getDebugInfo().get());
    }
    
    return sb.toString();
  }
}