package org.ek9lang.compiler.ir;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Used with CallInstr, holds all the necessary details to make a call.
 * Includes metadata for backend optimization.
 */
public record CallDetails(String targetObject,
                          String targetTypeName,
                          String methodName,
                          List<String> parameterTypes,
                          String returnTypeName,
                          List<String> arguments,
                          CallMetaData metaData) {

  @Override
  @Nonnull
  public String toString() {
    final var sb = new StringBuilder();
    sb.append("(")
        .append(targetTypeName())
        .append(")");
    if (targetObject() != null) {
      sb.append(targetObject());
    }

    sb.append(".").append(methodName());

    if (!arguments().isEmpty()) {
      sb.append("(");
      sb.append(String.join(", ", arguments()));
      sb.append(")");
    } else {
      sb.append("()");
    }

    // Add metadata information
    sb.append(" [pure=").append(metaData().isPure())
        .append(", complexity=").append(metaData().complexityScore());
    if (!metaData().sideEffects().isEmpty()) {
      sb.append(", effects=").append(String.join(",", metaData().sideEffects()));
    }
    sb.append("]");

    return sb.toString();
  }
}
