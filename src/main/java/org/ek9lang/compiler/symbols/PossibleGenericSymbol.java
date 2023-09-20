package org.ek9lang.compiler.symbols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.ToCommaSeparated;
import org.ek9lang.core.AssertValue;

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
  static final long serialVersionUID = 1L;

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
  private final List<ISymbol> typeParameterOrArguments = new ArrayList<>();
  /**
   * If this AggregateSymbol/FunctionSymbol is a generic type then within its code area
   * either properties, methods or return types it may use another generic type with the same
   * parameters K and V for example.
   * So when we come to actually use this generic type class with K=String and V=Float we must
   * also look to replace these conceptual parameterised type symbols because they would be
   * Item of (K, V) and need to be Item of (String, Float).
   * A good example is Map of (K, V) and MapEntry of(K, V) - when you make a
   * Map of (String, Float) we replace K&rarr;String, V&rarr;Float but also
   * need to replace MapEntry of(K, V) with MapEntry of(String, Float).
   * You could also imagine a situation where you have to replace
   * Something of (Integer, V) with Something of (Integer, Float)!
   */
  private final List<PossibleGenericSymbol> parameterisedTypeReferences = new ArrayList<>();
  /**
   * This is the optional reference to a 'generic type' (could be a class or a function).
   * That 'generic type' will have its own 'type parameters'. i.e. the 'K', "V' or 'T' things.
   */
  private PossibleGenericSymbol genericType;

  /**
   * This is actually a 'T' itself - we will need to know this.
   * So we use an aggregate symbol to model the 'S' and 'T' conceptual types.
   * These are 'synthetic' in nature but do have 'synthetic' operators, so that
   * generic/template classes/functions can 'assume' certain operators and operations.
   * Only when the generic type is actually parameterised with a 'concrete' type does the
   * compiler check that the generic class/function that used the operators can be used with
   * the actual concrete type.
   */
  private boolean conceptualTypeParameter;

  /**
   * Is this aggregate/function open to be extended.
   * i.e. is it closed so that it cannot be extended.
   */
  private boolean openForExtension = false;

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
    newCopy.parameterisedTypeReferences.addAll(getGenericSymbolReferences());
    newCopy.typeParameterOrArguments.addAll(typeParameterOrArguments);
    newCopy.conceptualTypeParameter = conceptualTypeParameter;
    newCopy.openForExtension = openForExtension;
    this.getGenericType().ifPresent(theType -> newCopy.setGenericType(Optional.of(theType)));

    return newCopy;
  }

  @Override
  public boolean isParameterisedType() {
    return getGenericType().isPresent();
  }

  @Override
  public Optional<PossibleGenericSymbol> getGenericType() {
    return Optional.ofNullable(genericType);
  }

  /**
   * Used parameterizing a generic type with type arguments.
   */
  @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
  public void setGenericType(Optional<PossibleGenericSymbol> genericType) {
    genericType.ifPresentOrElse(theGenericType -> {
          this.genericType = theGenericType;
          //Use the same scope, module and source token for this as the generic type.
          setModuleScope(theGenericType.getModuleScope());
          setParsedModule(theGenericType.getParsedModule());
          setSourceToken(theGenericType.getSourceToken());
        },
        () -> this.genericType = null);
  }

  public void setGenericType(PossibleGenericSymbol genericType) {
    setGenericType(Optional.ofNullable(genericType));
  }

  @Override
  public boolean isGenericInNature() {
    return typeParameterOrArguments.stream().anyMatch(ISymbol::isConceptualTypeParameter);
  }

  public boolean isOpenForExtension() {
    return this.openForExtension;
  }

  public void setOpenForExtension(boolean open) {
    this.openForExtension = open;
  }

  /**
   * Is this itself a 'conceptual' type.
   */
  @Override
  public boolean isConceptualTypeParameter() {
    return conceptualTypeParameter;
  }

  /**
   * Set this type symbol to be conceptual in nature.
   */
  public void setConceptualTypeParameter(boolean conceptualTypeParameter) {
    this.conceptualTypeParameter = conceptualTypeParameter;
  }

  @Override
  public List<PossibleGenericSymbol> getGenericSymbolReferences() {
    return Collections.unmodifiableList(parameterisedTypeReferences);
  }

  /**
   * Add a reference to a parameterised function.
   * I'm still in two minds about adding these references.
   * Leave as is at the moment, and then see what I think later on.
   */
  public void addGenericSymbolReference(PossibleGenericSymbol genericSymbolReference) {
    //only need to add once but source might have many references to the type.
    if (!parameterisedTypeReferences.contains(genericSymbolReference)) {
      parameterisedTypeReferences.add(genericSymbolReference);
    }
  }

  /**
   * Add a parameter type to this scope.
   */
  @Override
  public void addTypeParameterOrArgument(ISymbol typeParameterOrArgument) {
    AssertValue.checkNotNull("TypeParameterOrArgument cannot be null", typeParameterOrArgument);
    typeParameterOrArguments.add(typeParameterOrArgument);
    if (isGenericInNature()) {
      //because this is now generic in nature, it will itself become a conceptual type
      this.setConceptualTypeParameter(true);
      if (this.getCategory() == SymbolCategory.FUNCTION) {
        this.setCategory(SymbolCategory.TEMPLATE_FUNCTION);
      } else if (this.getCategory() == SymbolCategory.TYPE) {
        this.setCategory(SymbolCategory.TEMPLATE_TYPE);
      }
    }
  }

  @Override
  public String getFriendlyName() {
    if (getGenericType().isPresent()) {
      return getGenericType().get().getFriendlyName();
    }
    return super.getFriendlyName();
  }

  protected String getAnyGenericParamsAsFriendlyNames() {
    StringBuilder buffer = new StringBuilder();
    if (!getTypeParameterOrArguments().isEmpty()) {
      buffer.append(" of type ");
      var params = getTypeParameterOrArguments();
      var toCommaSeparated = new ToCommaSeparated(this, params.size() > 1);
      buffer.append(toCommaSeparated.apply(params));
    }
    return buffer.toString();
  }

  /**
   * If this scope has been parameterised, then go through those parameters and
   * return a list of all those that are generic 'T' in nature and not actually concrete types.
   */
  public List<ISymbol> getAnyConceptualTypeParameters() {
    return typeParameterOrArguments
        .stream()
        .filter(ISymbol::isConceptualTypeParameter)
        .toList();
  }

  public List<ISymbol> getTypeParameterOrArguments() {
    return Collections.unmodifiableList(typeParameterOrArguments);
  }

  /**
   * Used when the type/function is one that is generic/template.
   * It will have a number of parameterTypes, these are 'T' and the like.
   * The conceptual or concrete types.
   */
  public Optional<ISymbol> resolveFromParameterTypes(final SymbolSearch search) {
    Optional<ISymbol> rtn = Optional.empty();
    if (isGenericInNature() && (search.isAnyValidTypeSearch())) {
      for (ISymbol parameterType : getTypeParameterOrArguments()) {
        if (parameterType.isAssignableTo(search.getAsSymbol())) {
          rtn = Optional.of(parameterType);
        }
      }
    }
    return rtn;
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    Optional<ISymbol> rtn = resolveFromParameterTypes(search);

    if (rtn.isEmpty()) {
      rtn = super.resolveInThisScopeOnly(search);
    }

    return rtn;
  }

  /**
   * So this is where we alter the mechanism of normal symbol resolution.
   */
  @Override
  public Optional<ISymbol> resolve(SymbolSearch search) {
    Optional<ISymbol> rtn = resolveInThisScopeOnly(search);

    //This is the scenario where we have a dynamic class or function being used within
    // a generic class or function and we need to resolve the types used in that outer type.
    //i.e. the K, V and T sort of conceptual types.
    if (rtn.isEmpty() && getScopeType().equals(ScopeType.DYNAMIC_BLOCK)
        && getOuterMostTypeOrFunction().isPresent() && search.isAnyValidTypeSearch()) {
      rtn = getOuterMostTypeOrFunction().get().resolve(search);
    }

    if (rtn.isEmpty()) {
      rtn = resolveWithParentScope(search);
    }
    return rtn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    return (o instanceof PossibleGenericSymbol that)
        && super.equals(o)
        && isConceptualTypeParameter() == that.isConceptualTypeParameter()
        && getGenericType().equals(that.getGenericType())
        && getTypeParameterOrArguments().equals(that.getTypeParameterOrArguments())
        && parameterisedTypeReferences.equals(that.parameterisedTypeReferences);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + getGenericType().hashCode();
    result = 31 * result + getTypeParameterOrArguments().hashCode();
    result = 31 * result + parameterisedTypeReferences.hashCode();
    result = 31 * result + (isConceptualTypeParameter() ? 1 : 0);
    return result;
  }
}
