package org.ek9lang.compiler.ir.data;

import javax.annotation.Nonnull;

/**
 * Represents details about a program parameter for the PROGRAM_ENTRY_POINT_BLOCK.
 * Used by backends to generate type-safe command-line argument conversion and validation.
 */
public record ParameterDetails(String name,
                               String type,
                               int position) {

  @Override
  @Nonnull
  public String toString() {
    return position + ":" + name + ":" + type;
  }
}