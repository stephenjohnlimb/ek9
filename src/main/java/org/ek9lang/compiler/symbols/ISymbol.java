package org.ek9lang.compiler.symbols;

import java.io.Serializable;
import java.util.Optional;
import org.ek9lang.compiler.Module;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Represents the concept of what functionality a Symbol should have.
 */
public interface ISymbol extends ITokenReference, Serializable {

  double NOT_ASSIGNABLE = -1000000.0;

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

  //Used for declarations of variables/params where they can be null.
  boolean isNullAllowed();

  void setNullAllowed(final boolean nullAllowed);

  boolean isInjectionExpected();

  void setInjectionExpected(final boolean injectionExpected);

  //Has the symbol been referenced
  boolean isReferenced();

  //mark the symbol as referenced.
  void setReferenced(final boolean referenced);

  //Bits of information we might need to put away for later processing
  void putSquirrelledData(final String key, final String value);

  String getSquirrelledData(final String key);

  Optional<Module> getParsedModule();

  /**
   * For some symbols you may wish to specify the parsed module they were defined in.
   *
   * @param parsedModule The parsedModule the symbol was defined in.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  void setParsedModule(final Optional<Module> parsedModule);

  boolean isDevSource();

  boolean isLibSource();

  /**
   * Clone the symbol and re-parent if this symbol like a method should have a parent.
   * Other symbols like VariableSymbols are un-parented
   */
  ISymbol clone(final IScope withParentAsAppropriate);

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
   * This symbol itself can be marked as pure - i.e. an operator with no side effects.
   *
   * @return true if pure, false otherwise. By default, false - lets assume the worst.
   */
  boolean isMarkedPure();

  /**
   * Even constants can be mutable until set. then they change to being none mutable.
   * Likewise in 'pure' scopes a variable can be mutable until it is first set then none mutable.
   *
   * @return If this symbol is mutable or not.
   */
  boolean isMutable();

  void setNotMutable();

  IToken getSourceToken();

  IToken getInitialisedBy();

  void setInitialisedBy(final IToken initialisedBy);

  default boolean isInitialised() {

    return getInitialisedBy() != null;
  }

  default boolean isPrivate() {

    return false;
  }

  default boolean isProtected() {

    return false;
  }

  default boolean isPublic() {

    return true;
  }

  default boolean isLoopVariable() {

    return false;
  }

  default boolean isIncomingParameter() {

    return false;
  }

  default boolean isReturningParameter() {

    return false;
  }

  //Special type of variable one that is a property on an aggregate/or capture.
  default boolean isPropertyField() {

    return false;
  }

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

  /**
   * Only use on symbols, to see if they are directly defined as a constant.
   */
  default boolean isDeclaredAsConstant() {

    final var thisType = this.getType();
    if (this instanceof ConstantSymbol && thisType.isPresent()) {
      final var thisGenus = thisType.get().getGenus();
      //We allow enumerations because there is no :=: operator to modify them
      //We also allow function delegates, but they are also constants.
      if ((thisGenus == ISymbol.SymbolGenus.CLASS_ENUMERATION)
          || (thisGenus == ISymbol.SymbolGenus.FUNCTION)
          || (thisGenus == ISymbol.SymbolGenus.FUNCTION_TRAIT)) {
        return true;
      }
    }

    //We consider loop variables as constants - EK9 deals with changing the value
    return this.isLoopVariable() || this.isFromLiteral();
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

  default boolean isFromLiteral() {

    return false;
  }

  default boolean isMarkedAbstract() {

    return false;
  }

  /**
   * Some classes generated can be injected and others not.
   *
   * @return true if this can be injected, false if not.
   */
  default boolean isInjectable() {

    return false;
  }

  /**
   * While this aggregate itself might not able been marked as injectable
   * Does this extend an aggregate that is marked as injectable.
   *
   * @return true if this is injectable or any of its supers area
   */
  default boolean isExtensionOfInjectable() {

    return false;
  }

  /**
   * If is the symbol is an exact match.
   */
  boolean isExactSameType(final ISymbol symbolType);

  /**
   * Is this a core thing from EK9.
   */
  boolean isEk9Core();

  /**
   * For some symbols we might support the _promote method via coercion.
   * i.e. Long -> Float for example
   * But with class structures and traits/interfaces there is a time when
   * objects are assignable because of base and super classes/types but don't need coercion.
   * The isAssignable deals with mingling both coercion and super/trait type compatibility.
   * But we need to know when it comes to IR generation whether to include a PromoteNode
   * or whether the code generated will just work because of class inheritance/interface
   * implementation.
   */
  boolean isPromotionSupported(final ISymbol s);

  boolean isAssignableTo(final ISymbol s);

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  boolean isAssignableTo(final Optional<ISymbol> s);

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  double getAssignableWeightTo(final Optional<ISymbol> s);

  double getAssignableWeightTo(final ISymbol s);

  double getUnCoercedAssignableWeightTo(final ISymbol s);

  SymbolGenus getGenus();

  void setGenus(final SymbolGenus genus);

  SymbolCategory getCategory();


  /**
   * Provide the internal fully qualified name of this symbol.
   * Defaults to just the name unless overridden.
   * -Useful for generating output where you want to ensure fully qualified names are used.
   */
  String getFullyQualifiedName();

  /**
   * Provide the name an end user would need to see on the screen. Normally this is just
   * 'getName' but in the case of Templates We use a very nasty internal naming for List
   * of SomeClass - which will probably be something like _List_hashed_version_of_ComeClass
   * and the end user needs to see 'List of SomeClass' for it to be meaningful.
   *
   * @return a user presentable of the symbol name.
   */
  String getFriendlyName();

  /**
   * Provide the internal name of this symbol - not fully qualified in terms of the module it is in.
   *
   * @return The basic internal name we'd use to resolve the symbol
   */
  String getName();

  void setName(final String name);

  Optional<ISymbol> getType();

  default ISymbol setType(final ISymbol type) {

    return setType(Optional.ofNullable(type));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  ISymbol setType(final Optional<ISymbol> type);

  /**
   * Typically, used on aggregates because we might use a AggregateSymbol
   * But when coming to process it we need to ensure other aggregate symbols that extend
   * it are of compatible genus i.e a class can only extend a base class not a style
   * but one style could extend another style.
   * We can also use this information when parsing the structure and then doing the semantic
   * analysis before the IR node generation. We can modify output by using this information.
   */
  enum SymbolGenus {
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

  /**
   * Typically used on symbols; so we know their broad use and type.
   * A Function is both a type and a method because we want to use it as a types variable
   * but also call it.
   * So we may need to alter these methods below to treat a function also as a type and a method.
   * A control is an example of a switch statement that can return a value - we may add others.
   */
  enum SymbolCategory {
    TYPE("type"),
    TEMPLATE_TYPE("generic type"), //This is a definition of the template once MyTemplate with type of T
    // is made read as a type MyTemplate of Integer it becomes a TYPE
    METHOD("method"),
    TEMPLATE_FUNCTION("generic function"), // As per the TEMPLATE_TYPE once made concrete becomes a FUNCTION
    // when used with concrete parameters - has unique name in the combination.
    FUNCTION("function"),
    CONTROL("control"),
    VARIABLE("variable");

    private final String description;

    SymbolCategory(final String description) {
      this.description = description;
    }

    public String getDescription() {
      
      return description;
    }
  }
}
