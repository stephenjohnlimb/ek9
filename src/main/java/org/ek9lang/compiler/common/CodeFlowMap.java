package org.ek9lang.compiler.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Designed to be used in a transient manner when interacting with variables.
 * This is typically through the flow of variable use within a bounded scope.
 * For example within a function or a method.
 * It is transient in the sense that it does not live beyond the parsing of a specific source file.
 * So it is not attached to any scopes or symbols. Instead, those scopes and symbols are used as keys to be
 * able to access specific meta-data that is recorded/accessed during flow of the ek9 code.
 * <br/>
 * This approach has been taken to ensure that memory is not excessively used and the scope/symbol concepts
 * are not mixed with what is in effect a sort of interpreter assessment.
 * <br/>
 * While the initial reason for this was assessment of variables being initialised or not within branching
 * structures. It will probably also be useful for the checking of Optional (is-set) and Result ('ok'/'error').
 * It may even be extendable to assess if conditionals are always true/false and for assessing cyclo metric complexity.
 */
public class CodeFlowMap {

  private static final String INITIALISED = "INITIALISED";

  private final Map<IScope, Map<ISymbol, SymbolAccess>> accessMap = new HashMap<>();

  private final UninitialisedVariableToBeChecked uninitialisedVariableToBeChecked =
      new UninitialisedVariableToBeChecked();

  /**
   * Just provide a list of variables that have not been marked as initialised in the scope.
   */
  public List<ISymbol> getUninitialisedVariables(IScope inScope) {

    var access = accessMap.getOrDefault(inScope, new HashMap<>());
    return access.keySet().stream()
        .filter(variable -> !access.get(variable).metaData.contains(INITIALISED))
        .toList();
  }

  /**
   * Check if a variable was initialised at this point in the ek9 code structure.
   */
  public boolean isVariableInitialised(final ISymbol identifierSymbol, final IScope inScope) {
    if (uninitialisedVariableToBeChecked.test(identifierSymbol)) {
      var symbolAccess = getSymbolAccessForVariable(identifierSymbol, inScope);
      return symbolAccess.metaData.contains(INITIALISED);
    }
    return false;
  }

  /**
   * Records a symbol against the specific scope if it was not initialised at declaration.
   */
  public void recordSymbol(final ISymbol identifierSymbol, final IScope inScope) {

    if (uninitialisedVariableToBeChecked.test(identifierSymbol)) {
      getOrCreateSymbolAccess(identifierSymbol, inScope);
    }

  }

  /**
   * Notes that a symbol was assigned in a particular scope.
   */
  public void recordSymbolAssignment(final ISymbol identifierSymbol,
                                     final IScope inScope) {

    markSymbolAsInitialised(identifierSymbol, inScope);

  }

  /**
   * Ensures that an identifier symbol is now marked initialised within a specific scope.
   */
  public void markSymbolAsInitialised(final ISymbol identifierSymbol, final IScope inScope) {

    if (uninitialisedVariableToBeChecked.test(identifierSymbol)) {
      var access = getOrCreateSymbolAccess(identifierSymbol, inScope);
      access.get(identifierSymbol).metaData().add(INITIALISED);
    }

  }

  private void ensureEnclosingScopeHasVariable(final ISymbol identifierSymbol,
                                               final IScope fromScope) {
    var enclosingScope = fromScope.getEnclosingScope();
    if (enclosingScope == null) {
      return;
    }
    if (accessMap.containsKey(enclosingScope) && accessMap.get(enclosingScope).containsKey(identifierSymbol)) {
      return;
    }
    //Need to ensure that the variable is actually available in this scope before adding to the map.
    if (enclosingScope.resolve(new SymbolSearch(identifierSymbol.getName())).isEmpty()) {
      return;
    }

    //This is broken variables being pushed up to far - must stay within method/function.
    getSymbolAccessForVariable(identifierSymbol, enclosingScope);
  }

  private SymbolAccess getSymbolAccessForVariable(final ISymbol identifierSymbol,
                                                  final IScope fromScope) {

    //This is necessary because there can be multiple nested scopes
    //So if a variable exists in a very outer scope, and also in very deep scopes
    //It is necessary to pull the variable and its values into the middle scopes
    //even though they may not be directly referenced. This is because when looking at
    //checking if all paths are set those values are needed in the appropriate scope.
    //See ConditionalAssignment10 as an example of this.
    ensureEnclosingScopeHasVariable(identifierSymbol, fromScope);

    if (accessMap.containsKey(fromScope) && accessMap.get(fromScope).containsKey(identifierSymbol)) {
      return accessMap.get(fromScope).get(identifierSymbol);
    }

    var enclosingScope = fromScope.getEnclosingScope();
    if (enclosingScope == null) {
      return new SymbolAccess(new HashSet<>());
    }

    //Recursive call to get same but from enclosing scope.
    var toReturn = getSymbolAccessForVariable(identifierSymbol, enclosingScope);

    //Now as this was not present for the 'fromScope' we need to add it in and populate with the values from toReturn
    //In this way we always ensure that whatever the scope path (even when variables were not accessed in intermediate
    //scopes, we actually have the variable and the symbol access

    //So we have to make the entry if not present for scope or variable and then copy of the values from the
    //enclosing scope - this scope may then mutate them or add to them - but we don't want to affect the enclosing scope
    var map = getOrCreateSymbolAccess(identifierSymbol, fromScope);
    map.get(identifierSymbol).metaData.addAll(toReturn.metaData);
    return toReturn;

  }

  /**
   * Gets or Creates a Symbol Access for the variable within the scope.
   */
  private Map<ISymbol, SymbolAccess> getOrCreateSymbolAccess(final ISymbol identifierSymbol,
                                                             final IScope inScope) {

    var access = accessMap.getOrDefault(inScope, new HashMap<>());
    var identifierEntry = access.getOrDefault(identifierSymbol, new SymbolAccess(new HashSet<>()));

    //Put either way even if present.
    access.put(identifierSymbol, identifierEntry);

    accessMap.put(inScope, access);
    return access;

  }

  private record SymbolAccess(Set<String> metaData) {

  }
}
