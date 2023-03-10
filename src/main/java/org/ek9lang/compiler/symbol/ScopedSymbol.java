package org.ek9lang.compiler.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * Represents a symbol that also has a scope. Typically, this means it can have variables,
 * methods and function declared within it. But also includes block scoped constructs like
 * for loops for example.
 */
public class ScopedSymbol extends Symbol implements IScopedSymbol {

  private final LocalScope actualScope;

  /**
   * If this AggregateSymbol/FunctionSymbol is a generic type then within its code area
   * either properties, methods or return types it may use another generic type with the same
   * parameters K and V for example.
   * So when we come to actually use this generic type class with K=String and V=Float we must
   * also look to replace these conceptual parameterised type symbols because they would be
   * Item of (K, V) and need to be Item of (String, Float).
   * A good example is Map of (K, V) and MapEntry of(K, V) - when you make a
   * Map of (String, Floa)> we replace K->String, V->Float but also
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
   * If we were generating a class of A then this would be empty
   * But if it were a generic type class like 'class B of T' then this would be set to
   * Symbol T.
   * Likewise function zule of(K,V>)would have K and V symbols in it.
   * But if class Jaguar of(S, T) then this would have S and T a symbols in it.
   * Note that you need to use ParameterisedTypeSymbol with this and a couple of concrete classes
   * To have something concrete.
   */
  private final List<AggregateSymbol> parameterisedTypes = new ArrayList<>();

  /**
   * Used for parameterised generic types/functions so that we can hang on to the context for
   * phase IR generation
   * We really do use the code as a template and so need to visit and generate Nodes multiple
   * times but alter the type of S and T for the concrete types provided.
   */
  private ParserRuleContext contextForParameterisedType;

  /**
   * For some scoped symbols - dynamic functions and classes it is important to keep a
   * reference to the outermost enclosing aggregate or function.
   */
  private Optional<IScopedSymbol> outerMostTypeOrFunction = Optional.empty();

  /**
   * If we encounter an exception within a scope we need to note the line number.
   */
  private Token encounteredExceptionToken = null;

  public ScopedSymbol(String name, IScope enclosingScope) {
    super(name);
    actualScope = new LocalScope(ScopeType.BLOCK, name, enclosingScope);
  }

  public ScopedSymbol(IScope.ScopeType scopeType, String scopeName, IScope enclosingScope) {
    super(scopeName);
    actualScope = new LocalScope(scopeType, scopeName, enclosingScope);
  }

  public ScopedSymbol(String name, Optional<ISymbol> type, IScope enclosingScope) {
    super(name, type);
    actualScope = new LocalScope(ScopeType.BLOCK, name, enclosingScope);
  }

  @Override
  public int hashCode() {

    return Objects.hash(super.hashCode(), actualScope.hashCode(),
        parameterisedTypeReferences.hashCode(), parameterisedFunctionReferences.hashCode(),
        parameterisedTypes.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    var rtn = super.equals(obj);
    if (rtn && obj instanceof ScopedSymbol scopedSymbol) {
      rtn = actualScope.equals(scopedSymbol.actualScope);
      rtn &= CommonParameterisedTypeDetails
          .doSymbolsMatch(Collections.unmodifiableList(parameterisedTypeReferences),
              Collections.unmodifiableList(scopedSymbol.parameterisedTypeReferences));
      rtn &= CommonParameterisedTypeDetails
          .doSymbolsMatch(Collections.unmodifiableList(parameterisedFunctionReferences),
              Collections.unmodifiableList(scopedSymbol.parameterisedFunctionReferences));
      rtn &= CommonParameterisedTypeDetails
          .doSymbolsMatch(Collections.unmodifiableList(parameterisedTypes),
              Collections.unmodifiableList(scopedSymbol.parameterisedTypes));
    }
    return rtn;
  }

  @Override
  public ScopedSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoScopeSymbol(new ScopedSymbol(this.getName(), withParentAsAppropriate));
  }

  protected ScopedSymbol cloneIntoScopeSymbol(ScopedSymbol newCopy) {
    cloneIntoSymbol(newCopy);
    actualScope.cloneIntoLocalScope(newCopy.actualScope);
    newCopy.parameterisedTypeReferences.addAll(parameterisedTypeReferences);
    newCopy.parameterisedFunctionReferences.addAll(parameterisedFunctionReferences);
    newCopy.parameterisedTypes.addAll(parameterisedTypes);
    newCopy.contextForParameterisedType = this.contextForParameterisedType;
    newCopy.encounteredExceptionToken = this.encounteredExceptionToken;
    getOuterMostTypeOrFunction().ifPresent(newCopy::setOuterMostTypeOrFunction);

    return newCopy;
  }

  public Optional<IScopedSymbol> getOuterMostTypeOrFunction() {
    return outerMostTypeOrFunction;
  }

  public void setOuterMostTypeOrFunction(IScopedSymbol outerMostTypeOrFunction) {
    this.outerMostTypeOrFunction = Optional.of(outerMostTypeOrFunction);
  }

  public LocalScope getActualScope() {
    return actualScope;
  }

  @Override
  public IScope.ScopeType getScopeType() {
    return actualScope.getScopeType();
  }

  public void setScopeType(final IScope.ScopeType scopeType) {
    actualScope.setScopeType(scopeType);
  }

  @Override
  public boolean isGenericInNature() {
    return !parameterisedTypes.isEmpty();
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
   * Add a parameterised type aggregate to this scope.
   */
  public void addParameterisedType(AggregateSymbol parameterisedType) {
    AssertValue.checkNotNull("ParameterisedType cannot be null", parameterisedType);
    this.parameterisedTypes.add(parameterisedType);
  }

  protected String getAnyGenericParamsAsFriendlyNames() {
    StringBuilder buffer = new StringBuilder();
    if (isGenericInNature()) {
      buffer.append(" of type ");
      var params = getParameterisedTypes();
      buffer.append(CommonParameterisedTypeDetails.asCommaSeparated(params, params.size() > 1));
    }
    return buffer.toString();
  }

  /**
   * If this scope has been parameterised, then go through those parameters and
   * return a list of all those that are generic 'T' in nature and not actually concrete types.
   */
  public List<ISymbol> getAnyGenericParameters() {
    return parameterisedTypes
        .stream()
        .filter(AggregateSymbol::isGenericTypeParameter)
        .map(ISymbol.class::cast)
        .toList();
  }

  public List<ISymbol> getParameterisedTypes() {
    return Collections.unmodifiableList(parameterisedTypes);
  }

  public ParserRuleContext getContextForParameterisedType() {
    return contextForParameterisedType;
  }

  public void setContextForParameterisedType(ParserRuleContext ctx) {
    this.contextForParameterisedType = ctx;
  }

  @Override
  public boolean isMarkedPure() {
    return actualScope.isMarkedPure();
  }

  @Override
  public void setMarkedPure(boolean markedPure) {
    actualScope.setMarkedPure(markedPure);
  }

  @Override
  public boolean isTerminatedNormally() {
    return getEncounteredExceptionToken() == null;
  }

  @Override
  public Token getEncounteredExceptionToken() {
    return encounteredExceptionToken;
  }

  @Override
  public void setEncounteredExceptionToken(Token encounteredExceptionToken) {
    this.encounteredExceptionToken = encounteredExceptionToken;
  }

  @Override
  public String getScopeName() {
    return actualScope.getScopeName();
  }

  @Override
  public String getFriendlyScopeName() {
    return this.getFriendlyName();
  }

  @Override
  public void define(ISymbol symbol) {
    actualScope.define(symbol);
  }

  protected IScope getEnclosingScope() {
    return actualScope.getEnclosingScope();
  }

  @Override
  public List<ISymbol> getSymbolsForThisScope() {
    return actualScope.getSymbolsForThisScope();
  }

  @Override
  public MethodSymbolSearchResult resolveMatchingMethods(MethodSymbolSearch search,
                                                         MethodSymbolSearchResult result) {
    return actualScope.resolveMatchingMethods(search, result);
  }

  @Override
  public MethodSymbolSearchResult resolveMatchingMethodsInThisScopeOnly(
      MethodSymbolSearch search, MethodSymbolSearchResult result) {
    return actualScope.resolveMatchingMethodsInThisScopeOnly(search, result);
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    return actualScope.resolveInThisScopeOnly(search);
  }

  @Override
  public Optional<ISymbol> resolve(SymbolSearch search) {
    return actualScope.resolve(search);
  }

  @Override
  public Optional<ScopedSymbol> findNearestNonBlockScopeInEnclosingScopes() {
    if (getScopeType().equals(ScopeType.NON_BLOCK)) {
      return Optional.of(this);
    }
    return actualScope.findNearestNonBlockScopeInEnclosingScopes();
  }

  @Override
  public Optional<ScopedSymbol> findNearestDynamicBlockScopeInEnclosingScopes() {
    if (getScopeType().equals(ScopeType.DYNAMIC_BLOCK)) {
      return Optional.of(this);
    }

    return actualScope.findNearestDynamicBlockScopeInEnclosingScopes();
  }

  @Override
  public boolean isScopeAMatchForEnclosingScope(IScope toCheck) {
    return actualScope.isScopeAMatchForEnclosingScope(toCheck);
  }
}
