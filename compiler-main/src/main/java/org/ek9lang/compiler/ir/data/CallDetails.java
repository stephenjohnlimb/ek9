package org.ek9lang.compiler.ir.data;

import java.util.List;
import java.util.stream.Collectors;
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
                          CallMetaDataDetails metaData,
                          boolean isTraitCall) {

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
      // Sort side effects for deterministic test output
      final var sortedEffects = metaData().sideEffects().stream()
          .sorted()
          .collect(Collectors.joining(","));
      sb.append(", effects=").append(sortedEffects);
    }
    if (isTraitCall()) {
      sb.append(", trait=true");
    }
    sb.append("]");

    return sb.toString();
  }
}
