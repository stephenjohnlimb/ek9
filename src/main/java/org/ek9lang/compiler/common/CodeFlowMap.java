package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.support.SymbolFactory.UNINITIALISED_AT_DECLARATION;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Designed to be used in a transient manner when interacting with variables.
 * This is typically through the flow of variable use within a bounded scope.
 * For example within a function or a method.
 * It is transient in the sense that it does not live beyond the parsing od that specific source file.
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

  private Map<IScope, Map<VariableSymbol, SymbolAccess>> accessMap = new HashMap<>();

  /**
   * Check if a variable was initialised at this point in the ek9 code structure.
   */
  private boolean isVariableInitialisedBeforeUse(final VariableSymbol identifierSymbol, final IScope inScope) {
    var possibleSymbolAccess = findSymbolAccess(identifierSymbol, inScope);
    if (possibleSymbolAccess.isPresent()) {
      return possibleSymbolAccess.get().metaData.contains("INITIALISED");
    }
    return false;
  }

  /**
   * Records a symbol against the specific scope if it was not initialised at declaration.
   */
  public void recordSymbol(final IToken location, final VariableSymbol identifierSymbol, final IScope inScope) {
    var access = getOrCreateSymbolAccess(location, identifierSymbol, inScope);
    if ("TRUE".equals(identifierSymbol.getSquirrelledData(UNINITIALISED_AT_DECLARATION))) {
      access.get(identifierSymbol).metaData().add("UNINITIALISED");
    }
  }

  /**
   * Notes that a symbol was assigned in a particular scope.
   */
  public void recordSymbolAssignment(final IToken location, final VariableSymbol identifierSymbol,
                                     final IScope inScope) {
    var access = getOrCreateSymbolAccess(location, identifierSymbol, inScope);
    access.get(identifierSymbol).metaData().add("INITIALISED");
  }

  /**
   * Gets or Creates a Symbol Access for the variable within the scope.
   */
  private Map<VariableSymbol, SymbolAccess> getOrCreateSymbolAccess(final IToken location,
                                                                    final VariableSymbol identifierSymbol,
                                                                    final IScope inScope) {
    var access = accessMap.getOrDefault(inScope, new HashMap<>());
    var identifierEntry = access.getOrDefault(identifierSymbol, new SymbolAccess(location, new HashSet<>()));

    //Put either way even if present.
    access.put(identifierSymbol, identifierEntry);
    accessMap.put(inScope, access);
    return access;
  }

  private Optional<SymbolAccess> findSymbolAccess(final VariableSymbol identifierSymbol, final IScope fromScope) {

    var toCheck = fromScope;
    //Means we are still within some sort of computational block
    while (!(toCheck instanceof FunctionSymbol) && !(toCheck instanceof MethodSymbol)) {
      //Then not only do we have a scope recorded but in the scope the variable was recorded.
      if (accessMap.containsKey(toCheck) && accessMap.get(toCheck).containsKey(identifierSymbol)) {
        return Optional.of(accessMap.get(toCheck).get(identifierSymbol));
      } else {
        //Otherwise move back up the ek9 scopes to the enclosing and try that.
        toCheck = toCheck.getEnclosingScope();
      }
    }
    //If we get right back to the top of the method/function then nothing was found.
    return Optional.empty();
  }

  private record SymbolAccess(IToken location, Set<String> metaData) {

  }
}
