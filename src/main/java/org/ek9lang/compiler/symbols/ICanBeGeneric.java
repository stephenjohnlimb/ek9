package org.ek9lang.compiler.symbols;

import java.util.List;
import java.util.Optional;

/**
 * Really a Marker interface for generic classes and generic functions.
 */
public interface ICanBeGeneric extends ISymbol {

  /**
   * Not only can a class/function that implements this interface be generic in nature,
   * they can also be a 'parameterised type'.
   */
  Optional<PossibleGenericSymbol> getGenericType();

  /**
   * Used to keep track of any generic types/functions used in a generic type that use some or
   * all of the generic type parameters or arguments.
   * For example a 'List of type T' might use or return an 'Iterator of type T'. It is the 'Iterator' that
   * would be a generic symbol reference.
   */
  List<PossibleGenericSymbol> getGenericSymbolReferences();

  /**
   * Provides a mechanism to record 'dependent generic types'.
   * See above wrt 'List of T' and 'Iterator of T'.
   * The reason for keeping these references is to that when the main generic type is 'paramterized'
   * we can 'cascade' that parameterization through to all dependent types.
   * This means when 'List of type T' is parameterized with 'T' -> 'Integer', we can also do that for
   * 'Iterator of type T', meaning we get a 'Iterator of Integer' even though we only asked for a 'List of Integer'.
   * You can see where this is taking us! Gigantic cascades of parameterized types. This means the EK9 developer
   * can get loads of 'types for free' i.e. without needing to be explicit.
   */
  void addGenericSymbolReference(PossibleGenericSymbol genericSymbolReference);

  void addTypeParameterOrArgument(ISymbol typeParameterOrArgument);

  List<ISymbol> getAnyConceptualTypeParameters();

  List<ISymbol> getTypeParameterOrArguments();

  /**
   * Is the symbol open for extension or not.
   * Typically this means a function is open/abstract and therefore can/must be extended.
   * The same with classes and components.
   */
  boolean isOpenForExtension();
}
