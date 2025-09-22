package org.ek9lang.compiler.ir.data;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents details about an EK9 program for the PROGRAM_ENTRY_POINT_BLOCK.
 * Contains all information needed by backends to generate type-safe program selection
 * and argument conversion logic.
 */
public record ProgramDetails(String qualifiedName,
                             List<ParameterDetails> parameterSignature,
                             @Nullable String applicationName) {

  /**
   * Check if this program has an associated Application for dependency injection.
   */
  public boolean hasApplication() {
    return applicationName != null;
  }

  @Override
  @Nonnull
  public String toString() {
    final var sb = new StringBuilder();
    sb.append(qualifiedName);

    if (!parameterSignature.isEmpty()) {
      sb.append("\n[");
      for (ParameterDetails parameterDetails : parameterSignature) {
        sb.append("\n");
        sb.append(parameterDetails);
      }
      sb.append("\n]");
    }

    if (hasApplication()) {
      sb.append(" with application ").append(applicationName);
    }

    return sb.toString();
  }
}