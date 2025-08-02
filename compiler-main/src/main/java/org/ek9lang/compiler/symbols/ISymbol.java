package org.ek9lang.compiler.symbols;

import java.io.Serializable;
import java.util.Optional;
import org.ek9lang.compiler.Module;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Represents the concept of what functionality a Symbol should have.
 */
public interface ISymbol extends ISymbolNature, ITokenReference, Serializable {

  double NOT_ASSIGNABLE = -1000000.0;

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
  void putSquirrelledData(final CommonValues key, final String value);

  String getSquirrelledData(final CommonValues key);

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

  void clearInitialisedBy();

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

  /**
   * Only use on symbols, to see if they are directly defined as a constant.
   */
  default boolean isDeclaredAsConstant() {

    final var thisType = this.getType();
    if (this instanceof ConstantSymbol && thisType.isPresent()) {
      final var thisGenus = thisType.get().getGenus();
      //We allow enumerations because there is no :=: operator to modify them
      //We also allow function delegates, but they are also constants.
      if ((thisGenus == SymbolGenus.CLASS_ENUMERATION)
          || (thisGenus == SymbolGenus.FUNCTION)
          || (thisGenus == SymbolGenus.FUNCTION_TRAIT)) {
        return true;
      }
    }

    //We consider loop variables as constants - EK9 deals with changing the value
    return this.isLoopVariable() || this.isFromLiteral();
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
  double getAssignableCostTo(final Optional<ISymbol> s);

  double getAssignableCostTo(final ISymbol s);

  double getUnCoercedAssignableCostTo(final ISymbol s);

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

}
