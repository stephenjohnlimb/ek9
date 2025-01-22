package org.ek9lang.compiler.symbols;

/**
 * Typically used on symbols; so we know their broad use and type.
 * <p>
 * A Function is both a type and a method because we want to use it as a types variable
 * but also call it.
 * So we may need to alter these methods below to treat a function also as a type and a method.
 * A control is an example of a switch statement that can return a value - we may add others.
 * </p>
 * <p>
 *   Any is designed to be any of the others.
 * </p>
 */
public enum SymbolCategory {

  TYPE("type"),
  TEMPLATE_TYPE("generic type"), //This is a definition of the template once MyTemplate with type of T
  // is made read as a type MyTemplate of Integer it becomes a TYPE
  METHOD("method"),
  TEMPLATE_FUNCTION("generic function"), // As per the TEMPLATE_TYPE once made concrete becomes a FUNCTION
  // when used with concrete parameters - has unique name in the combination.
  FUNCTION("function"),
  CONTROL("control"),
  VARIABLE("variable"),
  ANY("any");

  private final String description;

  SymbolCategory(final String description) {
    this.description = description;
  }

  public String getDescription() {

    return description;
  }
}
