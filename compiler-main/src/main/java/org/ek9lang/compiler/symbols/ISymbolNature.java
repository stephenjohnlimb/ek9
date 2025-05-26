package org.ek9lang.compiler.symbols;

/**
 * Defines helpful and require methods to assist in the definition of the nature of a symbol.
 * This includes the category, genus, but also whether it is generic or a template.
 * Symbol is really a chameleon, it can be a variable a type, etc.
 */
public interface ISymbolNature {
  SymbolGenus getGenus();

  void setGenus(final SymbolGenus genus);

  SymbolCategory getCategory();

  default boolean isTemplateType() {

    return getCategory().equals(SymbolCategory.TEMPLATE_TYPE);
  }

  default boolean isTemplateFunction() {

    return getCategory().equals(SymbolCategory.TEMPLATE_FUNCTION);
  }

  default boolean isType() {

    return getCategory().equals(SymbolCategory.TYPE);
  }

  default boolean isVariable() {

    return getCategory().equals(SymbolCategory.VARIABLE);
  }

  /**
   * Is this a core thing from EK9.
   */
  boolean isEk9Core();

  /**
   * So just to add to the confusion.
   * A type that has been used with a type that is generic in nature with some parameters.
   * So this would normally result in a type that is now concrete. i.e. List (generic in nature)
   * and String (concrete) so this resulting type 'List of String' isAParameterisedType=true
   * and isGenericInNature=false.
   * But if List were parameterised with another generic parameter it would still be of a
   * generic nature.
   * For example, consider List T and Iterator I when we defined List T and use the Iterator
   * with it; we parameterise Iterator with 'T'. But they are
   * still both 'isGenericTypeParameter' it only when we use List (and by implication Iterator)
   * with String do they both stop being generic in nature.
   *
   * @return true if a parameterised type (note parameterised not just of a generic nature).
   */
  default boolean isParameterisedType() {

    return false;
  }

  /**
   * Is this symbol a type that is generic in nature i.e. can it be parameterised with types.
   * Aggregate Symbols can be defined to accept one or more parameters ie S and T.
   */
  default boolean isGenericInNature() {

    return false;
  }

  /**
   * Some symbols are simulated as generic type parameters like T and S and U for example
   * When they are constructed as AggregateTypes this will be set to true in those classes.
   * In general this is useful when working with Generic classes and needing types like S and T
   * But they can never really be generated.
   */
  default boolean isConceptualTypeParameter() {

    return false;
  }


  /**
   * Is the symbol a core primitive type.
   */
  default boolean isPrimitiveType() {

    return isType() && isEk9Core() && !isParameterisedType();
  }

  /**
   * Is the symbol an application of some sort.
   */
  default boolean isApplication() {

    return switch (getGenus()) {
      case GENERAL_APPLICATION, SERVICE_APPLICATION -> true;
      default -> false;
    };
  }

  default boolean isMethod() {

    return getCategory().equals(SymbolCategory.METHOD);
  }

  default boolean isFunction() {

    return getCategory().equals(SymbolCategory.FUNCTION);
  }

  default boolean isControl() {

    return getCategory().equals(SymbolCategory.CONTROL);
  }

  default boolean isConstant() {

    return false;
  }

}
