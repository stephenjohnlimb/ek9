package org.ek9lang.compiler.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * Added as a common symbol type for both Functions and Aggregates that could possibly be
 * generic in nature and also be a parameterised type.
 * This is quite (very) complex, as in ek9 both classes and functions can be:
 * A 'GenericType'
 * Have 'Type Parameters'
 * Can be 'Parameterised Types' through the use of 'Type Arguments'
 * This can result in 'Parameterised Types' actually still being a 'GenericType'.
 * It's hard to get your (mine) head around all this.
 */
public class PossibleGenericSymbol extends CaptureScopedSymbol implements ICanBeGeneric {

  /**
   * This is the optional reference to a 'generic type' (could be a class or a function).
   * That 'generic type' will have its own 'type parameters'. i.e. the 'K', "V' or 'T' things.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<PossibleGenericSymbol> genericType = Optional.empty();

  /**
   * If this were the 'generic type':
   * This is the list of generic parameters the class/function can accept.
   * If we were generating a 'non-generic' class 'A' then this would be empty
   * But if it were a 'generic type' class like 'class B of type T' then this would be set to
   * Symbol T.
   * Likewise 'generic' function zule of type (K,V) would have K and V symbols in it.
   * OK, so far so good, but this class can also now be used as a 'parameterized type', this means that
   * this same class can have a reference in the 'genericType' above.
   * Imagine 'List of type T', The 'List' is an AggregateSymbol and the 'T' goes into parameterTypesOrArguments below.
   * Now imagine: var as List of Integer, this is a 'parameterized type', it too is an AggregateSymbol.
   * BUT: it references the List AggregateSymbol in 'genericType' AND Integer in 'parameterTypesOrArguments'
   * So when a 'GenericType' is being parameterized these are the arguments.
   * So this has dual use:
   * 1. For a 'GenericType' it is the 'type parameters'
   * 2. For a 'Parameterized Type' i.e. uses a 'GenericType' - that has its own 'type parameters' - this is then used
   * as the 'type arguments'.
   */
  private final List<ISymbol> parameterTypesOrArguments = new ArrayList<>();

  /**
   * Used for parameterised generic types/functions so that we can hang on to the context for
   * phase IR generation
   * We really do use the code as a template and so need to visit and generate Nodes multiple
   * times but alter the type of S and T for the concrete types provided.
   */
  private ParserRuleContext contextForParameterisedType;

  /**
   * If this AggregateSymbol/FunctionSymbol is a generic type then within its code area
   * either properties, methods or return types it may use another generic type with the same
   * parameters K and V for example.
   * So when we come to actually use this generic type class with K=String and V=Float we must
   * also look to replace these conceptual parameterised type symbols because they would be
   * Item of (K, V) and need to be Item of (String, Float).
   * A good example is Map of (K, V) and MapEntry of(K, V) - when you make a
   * Map of (String, Float)> we replace K->String, V->Float but also
   * need to replace MapEntry of(<, V) with MapEntry of(String, Float).
   * You could also imagine a situation where you have to replace
   * Something of (Integer, V) with Something of (Integer, Float)!
   */
  private final List<PossibleGenericSymbol> parameterisedTypeReferences = new ArrayList<>();

  public PossibleGenericSymbol(String name, IScope enclosingScope) {
    super(name, enclosingScope);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public PossibleGenericSymbol(String name, Optional<ISymbol> type, IScope enclosingScope) {
    super(name, type, enclosingScope);
  }

  @Override
  public ScopedSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoPossibleGenericSymbol(new PossibleGenericSymbol(this.getName(), withParentAsAppropriate));
  }

  protected PossibleGenericSymbol cloneIntoPossibleGenericSymbol(PossibleGenericSymbol newCopy) {
    super.cloneIntoCaptureScopedSymbol(newCopy);
    newCopy.parameterisedTypeReferences.addAll(parameterisedTypeReferences);
    newCopy.parameterTypesOrArguments.addAll(parameterTypesOrArguments);
    newCopy.contextForParameterisedType = this.contextForParameterisedType;
    this.genericType.ifPresent(theType -> newCopy.setGenericType(Optional.of(theType)));

    return newCopy;
  }

  @Override
  public Optional<PossibleGenericSymbol> getGenericType() {
    return genericType;
  }

  @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
  public void setGenericType(Optional<PossibleGenericSymbol> genericType) {
    this.genericType = genericType;
  }

  @Override
  public boolean isGenericInNature() {
    return !parameterTypesOrArguments.isEmpty();
  }

  @Override
  public List<PossibleGenericSymbol> getParameterisedTypeReferences() {
    return Collections.unmodifiableList(parameterisedTypeReferences);
  }

  /**
   * Add a reference to a parameterised function.
   */
  public void addParameterisedTypeReference(PossibleGenericSymbol parameterisedTypeReference) {
    //only need to add once but source might have many references to the type.
    if (!parameterisedTypeReferences.contains(parameterisedTypeReference)) {
      parameterisedTypeReferences.add(parameterisedTypeReference);
    }
  }

  /**
   * Add a parameter type to this scope.
   */
  @Override
  public void addParameterType(ISymbol parameterType) {
    AssertValue.checkNotNull("ParameterType cannot be null", parameterType);
    this.parameterTypesOrArguments.add(parameterType);
  }

  protected String getAnyGenericParamsAsFriendlyNames() {
    StringBuilder buffer = new StringBuilder();
    if (isGenericInNature()) {
      buffer.append(" of type ");
      var params = getParameterTypesOrArguments();
      buffer.append(CommonParameterisedTypeDetails.asCommaSeparated(params, params.size() > 1));
    }
    return buffer.toString();
  }

  /**
   * If this scope has been parameterised, then go through those parameters and
   * return a list of all those that are generic 'T' in nature and not actually concrete types.
   */
  public List<ISymbol> getAnyGenericParameters() {
    return parameterTypesOrArguments
        .stream()
        .filter(ISymbol::isGenericTypeParameter)
        .map(ISymbol.class::cast)
        .toList();
  }

  public List<ISymbol> getParameterTypesOrArguments() {
    return Collections.unmodifiableList(parameterTypesOrArguments);
  }

  /**
   * Used when the type/function is one that is generic/template.
   * It will have a number of parameterTypes, these are 'T' and the like.
   * The conceptual types.
   */
  public Optional<ISymbol> resolveFromParameterTypes(final SymbolSearch search) {
    Optional<ISymbol> rtn = Optional.empty();
    if (isGenericInNature() && (search.getSearchType() == null || SymbolCategory.TYPE.equals(search.getSearchType()))) {
      for (ISymbol parameterType : getParameterTypesOrArguments()) {
        if (parameterType.isAssignableTo(search.getAsSymbol())) {
          rtn = Optional.of(parameterType);
        }
      }
    }
    return rtn;
  }

  public ParserRuleContext getContextForParameterisedType() {
    return contextForParameterisedType;
  }

  public void setContextForParameterisedType(ParserRuleContext ctx) {
    this.contextForParameterisedType = ctx;
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
    //TODO think about resolving in generic bits.
    return rtn;
  }

  @Override
  public int hashCode() {
    int genericTypeHash = genericType.map(PossibleGenericSymbol::hashCode).orElse(0);
    return Objects.hash(super.hashCode(), genericTypeHash,
        parameterisedTypeReferences.hashCode(), parameterTypesOrArguments.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    var rtn = super.equals(obj);
    if (rtn && obj instanceof PossibleGenericSymbol symbol) {
      rtn = CommonParameterisedTypeDetails
          .doSymbolsMatch(Collections.unmodifiableList(parameterisedTypeReferences),
              Collections.unmodifiableList(symbol.parameterisedTypeReferences));
      rtn &= CommonParameterisedTypeDetails
          .doSymbolsMatch(Collections.unmodifiableList(parameterTypesOrArguments),
              Collections.unmodifiableList(symbol.parameterTypesOrArguments));

      if (genericType.isPresent() && symbol.genericType.isPresent()) {
        rtn &= genericType.get().equals(symbol.genericType.get());
      } else {
        rtn &= genericType.isEmpty() && symbol.genericType.isEmpty();
      }
    } else {
      rtn = false;
    }
    return rtn;
  }
}
