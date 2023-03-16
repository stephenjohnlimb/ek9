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
 * generic in nature and also be parameterised types.
 * This is quite complex, as in ek9 both classes and functions can be:
 * A 'GenericType'
 * Have 'Type Parameters'
 * Can be 'Parameterised Types' through the use of 'Type Arguments'
 * This can result in 'Parameterised Types' actually still being a 'GenericType'.
 * It's hard to get your (mine) head around all this.
 */
public class PossibleGenericSymbol extends ScopedSymbol implements ICanCaptureVariables, ICanBeGeneric {

  //This is the module this function has been defined in.
  private IScope moduleScope;

  /**
   * Was it marked abstract in the source code.
   */
  private boolean markedAbstract = false;

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
   * YOu could also imagine a situation where you have to replace
   * Something of (Integer, V) with Something of (Integer, Float)!
   */
  private final List<ParameterisedTypeSymbol> parameterisedTypeReferences = new ArrayList<>();

  /**
   * Also set up the same but for generic functions.
   */
  private final List<ParameterisedFunctionSymbol> parameterisedFunctionReferences =
      new ArrayList<>();

  /**
   * So this is the list of generic parameters the class/function can accept.
   * If we were generating a class 'A' then this would be empty
   * But if it were a generic type class like 'class B of type T' then this would be set to
   * Symbol T.
   * Likewise function zule of type (K,V>)would have K and V symbols in it.
   * But if class Jaguar of type (S, T) then this would have S and T a symbols in it.
   * Note that you need to use ParameterisedTypeSymbol with this and a couple of concrete classes
   * To have something concrete.
   */
  private final List<AggregateSymbol> parameterTypes = new ArrayList<>();

  /**
   * Used for parameterised generic types/functions so that we can hang on to the context for
   * phase IR generation
   * We really do use the code as a template and so need to visit and generate Nodes multiple
   * times but alter the type of S and T for the concrete types provided.
   */
  private ParserRuleContext contextForParameterisedType;


  /**
   * For dynamic functions/types we can capture variables from the enclosing scope(s) and pull them in.
   * We can then hold and access them in the dynamic function/type even when the function has moved
   * out of the original scope. i.e. a sort of closure over variables.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<LocalScope> capturedVariables = Optional.empty();


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
    super.cloneIntoScopeSymbol(newCopy);
    newCopy.moduleScope = moduleScope;
    newCopy.markedAbstract = markedAbstract;
    newCopy.parameterisedTypeReferences.addAll(parameterisedTypeReferences);
    newCopy.parameterisedFunctionReferences.addAll(parameterisedFunctionReferences);
    newCopy.parameterTypes.addAll(parameterTypes);
    newCopy.contextForParameterisedType = this.contextForParameterisedType;

    if (capturedVariables.isPresent()) {
      LocalScope newCaptureScope = new LocalScope("CaptureScope", getEnclosingScope());
      capturedVariables.get().cloneIntoLocalScope(newCaptureScope);
      newCopy.setCapturedVariables(newCaptureScope);
    }

    return newCopy;
  }

  public IScope getModuleScope() {
    return moduleScope;
  }

  public void setModuleScope(IScope moduleScope) {
    this.moduleScope = moduleScope;
  }

  @Override
  public boolean isMarkedAbstract() {
    return markedAbstract;
  }

  public void setMarkedAbstract(boolean markedAbstract) {
    this.markedAbstract = markedAbstract;
  }

  public Optional<LocalScope> getCapturedVariables() {
    return capturedVariables;
  }

  /**
   * It is possible to capture variables in the current scope and pull them into the
   * function, so they can be used.
   */
  public void setCapturedVariables(LocalScope capturedVariables) {
    setCapturedVariables(Optional.ofNullable(capturedVariables));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setCapturedVariables(Optional<LocalScope> capturedVariables) {
    this.capturedVariables = capturedVariables;
  }

  /**
   * The variables that have been captured can be given public access if needed.
   */
  public void setCapturedVariablesVisibility(final boolean isPublic) {
    capturedVariables.ifPresent(
        localScope -> localScope.getSymbolsForThisScope().forEach(symbol -> {
          if (symbol instanceof VariableSymbol s) {
            s.setPrivate(!isPublic);
          }
        }));
  }

  protected String getPrivateVariablesForDisplay() {
    return capturedVariables
        .map(scope -> CommonParameterisedTypeDetails.asCommaSeparated(scope.getSymbolsForThisScope(), true))
        .orElse("");
  }

  @Override
  public boolean isGenericInNature() {
    return !parameterTypes.isEmpty();
  }

  @Override
  public List<ParameterisedFunctionSymbol> getParameterisedFunctionReferences() {
    return Collections.unmodifiableList(parameterisedFunctionReferences);
  }

  /**
   * Add a reference to a parameterised function.
   */
  public void addParameterisedFunctionReference(
      ParameterisedFunctionSymbol parameterisedFunctionReference) {
    if (!parameterisedFunctionReferences.contains(parameterisedFunctionReference)) {
      parameterisedFunctionReferences.add(parameterisedFunctionReference);
    }
  }

  @Override
  public List<ParameterisedTypeSymbol> getParameterisedTypeReferences() {
    return Collections.unmodifiableList(parameterisedTypeReferences);
  }

  /**
   * Add a reference to a parameterised function.
   */
  public void addParameterisedTypeReference(ParameterisedTypeSymbol parameterisedTypeReference) {
    //only need to add once but source might have many references to the type.
    if (!parameterisedTypeReferences.contains(parameterisedTypeReference)) {
      parameterisedTypeReferences.add(parameterisedTypeReference);
    }
  }

  /**
   * Add a parameter type aggregate to this scope.
   */
  public void addParameterType(AggregateSymbol parameterType) {
    AssertValue.checkNotNull("ParameterType cannot be null", parameterType);
    this.parameterTypes.add(parameterType);
  }

  protected String getAnyGenericParamsAsFriendlyNames() {
    StringBuilder buffer = new StringBuilder();
    if (isGenericInNature()) {
      buffer.append(" of type ");
      var params = getParameterTypes();
      buffer.append(CommonParameterisedTypeDetails.asCommaSeparated(params, params.size() > 1));
    }
    return buffer.toString();
  }

  /**
   * If this scope has been parameterised, then go through those parameters and
   * return a list of all those that are generic 'T' in nature and not actually concrete types.
   */
  public List<ISymbol> getAnyGenericParameters() {
    return parameterTypes
        .stream()
        .filter(AggregateSymbol::isGenericTypeParameter)
        .map(ISymbol.class::cast)
        .toList();
  }

  public List<ISymbol> getParameterTypes() {
    return Collections.unmodifiableList(parameterTypes);
  }

  /**
   * Used when the type/function is one that is generic/template.
   * It will have a number of parameterTypes, these are 'T' and the like.
   * The conceptual types.
   */
  public Optional<ISymbol> resolveFromParameterTypes(final SymbolSearch search) {
    Optional<ISymbol> rtn = Optional.empty();
    if (isGenericInNature() && (search.getSearchType() == null || SymbolCategory.TYPE.equals(search.getSearchType()))) {
      for (ISymbol parameterType : getParameterTypes()) {
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
  public Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search) {
    return super.resolveInThisScopeOnly(search);
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
    if (rtn.isEmpty() && capturedVariables.isPresent()) {
      rtn = capturedVariables.get().resolveInThisScopeOnly(search);
    }
    return rtn;
  }

  @Override
  public int hashCode() {

    int captureHash = capturedVariables.map(LocalScope::hashCode).orElse(0);

    return Objects.hash(super.hashCode(), captureHash, Boolean.hashCode(markedAbstract),
        parameterisedTypeReferences.hashCode(), parameterisedFunctionReferences.hashCode(),
        parameterTypes.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    var rtn = super.equals(obj);
    if (rtn && obj instanceof PossibleGenericSymbol symbol) {
      rtn = CommonParameterisedTypeDetails
          .doSymbolsMatch(Collections.unmodifiableList(parameterisedTypeReferences),
              Collections.unmodifiableList(symbol.parameterisedTypeReferences));
      rtn &= CommonParameterisedTypeDetails
          .doSymbolsMatch(Collections.unmodifiableList(parameterisedFunctionReferences),
              Collections.unmodifiableList(symbol.parameterisedFunctionReferences));
      rtn &= CommonParameterisedTypeDetails
          .doSymbolsMatch(Collections.unmodifiableList(parameterTypes),
              Collections.unmodifiableList(symbol.parameterTypes));

      if (capturedVariables.isPresent() && symbol.capturedVariables.isPresent()) {
        rtn &= capturedVariables.get().equals(symbol.capturedVariables.get());
      } else {
        rtn &= capturedVariables.isEmpty() && symbol.capturedVariables.isEmpty();
      }
    }
    return rtn;
  }
}
