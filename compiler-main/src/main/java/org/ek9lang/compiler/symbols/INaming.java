package org.ek9lang.compiler.symbols;

/**
 * Used to assist with determining the naming of symbol names.
 */
public interface INaming {
  /**
   * Symbol names can be fully qualified (i.e. with the module name) or just a name.
   * This method returns the module name if present or "" if not.
   */
  static String getModuleNameIfPresent(final String symbolName) {

    if (isQualifiedName(symbolName)) {
      final var parts = symbolName.split("::");
      return parts[0];
    }

    return "";
  }

  /**
   * Just returns the actual symbol name in unqualified form (i.e. no module name).
   */
  static String getUnqualifiedName(final String symbolName) {

    if (isQualifiedName(symbolName)) {
      final var parts = symbolName.split("::");
      return parts[1];
    }

    return symbolName;
  }

  static boolean isQualifiedName(final String symbolName) {

    return symbolName.contains("::");
  }

  /**
   * Convert a scope name (module name) and a symbol name into a fully qualified symbol name.
   */
  static String makeFullyQualifiedName(final String scopeName, final String symbolName) {

    //In come cases (mainly testing) we may have an empty scope name.
    if (isQualifiedName(symbolName) || scopeName.isEmpty()) {
      return symbolName;
    }

    return scopeName + "::" + symbolName;
  }
}
