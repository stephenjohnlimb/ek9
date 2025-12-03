package org.ek9lang.compiler.support;

import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Utility to determine if an ISymbol represents a reference-counted type
 * that should be included in the traceableFields list during initial IR generation.
 */
public final class ReferenceTypeChecker {

  private ReferenceTypeChecker() {
  }

  /**
   * Determines if the given ISymbol represents a reference-counted type.
   * In EK9, all objects are conceptually reference types. This check is primarily to
   * exclude non-object types like Void from being considered traceable.
   *
   * @param type     The ISymbol representing the type to check.
   * @param ek9Types A cache of the core EK9 types.
   * @return true if the type is a reference type, false otherwise.
   */
  public static boolean isReferenceType(final ISymbol type, final Ek9Types ek9Types) {
    if (type == null) {
      return false;
    }
    // Void is a special case that does not represent a memory-holding reference.
    return !type.isExactSameType(ek9Types.ek9Void());
  }
}
