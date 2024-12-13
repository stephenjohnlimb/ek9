package org.ek9lang.compiler.symbols;

/**
 * Typically, used on aggregates because we might use a AggregateSymbol
 * But when coming to process it we need to ensure other aggregate symbols that extend
 * it are of compatible genus i.e a class can only extend a base class not a style
 * but one style could extend another style.
 * We can also use this information when parsing the structure and then doing the semantic
 * analysis before the IR node generation. We can modify output by using this information.
 */
public enum SymbolGenus {
  GENERAL_APPLICATION("application"),
  SERVICE_APPLICATION("service application"),
  COMPONENT("component"),
  VALUE("value"),
  CLASS("class"),
  CLASS_TRAIT("trait"),
  CLASS_CONSTRAINED("constrained class"),
  CLASS_ENUMERATION("enumeration"),
  RECORD("record"),
  TYPE("type"),
  FUNCTION("function"),
  FUNCTION_TRAIT("abstract function"),
  TEXT_BASE("text base"),
  TEXT("text"),
  SERVICE("service"),
  PROGRAM("program"),
  META_DATA("meta-data");

  private final String description;

  SymbolGenus(final String description) {
    this.description = description;
  }

  public String getDescription() {

    return description;
  }

}
