package org.ek9lang.compiler.symbol.support.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.SymbolTable;
import org.ek9lang.core.exception.AssertValue;

/**
 * To be used to search for symbols in a Symbol table.
 */
public class SymbolSearch {
  /**
   * The name of the symbol looking for.
   * Could be all sorts fully qualified type name
   * like com.abc:Something - or a function like com.abc:myFunction or a function
   * call com.abc:myFunction()
   * Or just a variable in a scope like 'a' or a method like transmutant()
   */
  private final String name;
  /**
   * Typically if searching for a method this will be zero or more parameters.
   * But note these are not just a list of types they are the parameters with the type
   * set into that symbol.
   */
  private final List<ISymbol> parameters = new ArrayList<>();
  /**
   * For variables - esp when checking for duplicates we want to allow
   * for class level variables of a name and allow a local variable to have the same name
   * But we want to stop any block within block level definition of variables with the same name.
   * So more like java and less like C++ where C and C++ allow variable hiding/shadowing in
   * blocks within blocks.
   */
  private boolean limitToBlocks = false;
  /**
   * Of this Type/Super type or the return type of the function/method.
   */
  private Optional<ISymbol> ofTypeOrReturn = Optional.empty();
  /**
   * What type of search is being triggered.
   */
  private ISymbol.SymbolCategory searchType = ISymbol.SymbolCategory.VARIABLE;

  /**
   * Clone from Symbol search into a new instance.
   */
  public SymbolSearch(SymbolSearch from) {
    AssertValue.checkNotNull("from cannot be null for search Symbol", from);
    this.name = from.name;
    from.cloneIntoSearchSymbol(this);
  }

  public SymbolSearch(String name) {
    AssertValue.checkNotEmpty("name cannot be null for search Symbol", name);
    this.name = name;
  }

  protected SymbolSearch(String name, Optional<ISymbol> ofTypeOrReturn) {
    this(name);
    AssertValue.checkNotEmpty("Type Or Return Type cannot be empty", ofTypeOrReturn);
    this.ofTypeOrReturn = ofTypeOrReturn;
  }

  protected SymbolSearch(String name, ISymbol ofTypeOrReturn) {
    this(name);
    AssertValue.checkNotNull("ofTypeOrReturn cannot be null for search Symbol", ofTypeOrReturn);
    this.ofTypeOrReturn = Optional.of(ofTypeOrReturn);
  }

  protected SymbolSearch cloneIntoSearchSymbol(SymbolSearch symbolSearch) {
    return symbolSearch.setOfTypeOrReturn(ofTypeOrReturn)
        .setParameters(parameters)
        .setLimitToBlocks(limitToBlocks)
        .setSearchType(searchType);
  }

  public boolean isLimitToBlocks() {
    return limitToBlocks;
  }

  public SymbolSearch setLimitToBlocks(boolean limitToBlocks) {
    this.limitToBlocks = limitToBlocks;
    return this;
  }

  public Optional<ISymbol> getOfTypeOrReturn() {
    return ofTypeOrReturn;
  }

  public SymbolSearch setOfTypeOrReturn(ISymbol ofTypeOrReturn) {
    return setOfTypeOrReturn(Optional.ofNullable(ofTypeOrReturn));
  }

  /**
   * What type of symbol is being searched for or its return type.
   */
  public SymbolSearch setOfTypeOrReturn(Optional<ISymbol> ofTypeOrReturn) {
    AssertValue.checkNotNull("ofTypeOrReturn cannot be null for search Symbol", ofTypeOrReturn);
    this.ofTypeOrReturn = ofTypeOrReturn;
    return this;
  }

  /**
   * Get the parameters being used in this search.
   */
  public List<ISymbol> getParameters() {
    return parameters;
  }

  /**
   * Set any parameters that might be needed as part of this search.
   */
  public SymbolSearch setParameters(List<ISymbol> parameters) {
    AssertValue.checkNotNull("parameters cannot be null for search Symbol", parameters);
    parameters.forEach(this::addParameter);
    return this;
  }

  /**
   * Get all the types of the parameters being used in this search.
   */
  public List<ISymbol> getParameterTypes() {
    return parameters
        .stream()
        .map(ISymbol::getType)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  /**
   * Add a parameter to the search parameters.
   */
  public SymbolSearch addParameter(ISymbol parameter) {
    AssertValue.checkNotNull("parameter cannot be null for search Symbol", parameter);
    this.parameters.add(parameter);
    return this;
  }

  public ISymbol.SymbolCategory getSearchType() {
    return searchType;
  }

  public SymbolSearch setSearchType(ISymbol.SymbolCategory searchType) {
    this.searchType = searchType;
    return this;
  }

  /**
   * Provide a symbol using the name of the search which may or may not be fully qualified.
   *
   * @return The Optional symbol.
   */
  public Optional<ISymbol> getNameAsSymbol() {
    AggregateSymbol sym = new AggregateSymbol(name, new SymbolTable());
    return Optional.of(sym);
  }

  /**
   * Creates a new Symbol from the name in the search and the module name.
   */
  public Optional<ISymbol> getNameAsSymbol(String fromModuleName) {
    AggregateSymbol sym;
    String theName = fromModuleName.equals(ISymbol.getModuleNameIfPresent(name))
        ? ISymbol.getUnqualifiedName(name) : name;

    sym = new AggregateSymbol(theName, new SymbolTable());

    return Optional.of(sym);
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();

    getOfTypeOrReturn().ifPresent(returnType -> buffer.append(returnType.getName()).append(" <- "));
    buffer.append(getName());

    if (this.searchType == ISymbol.SymbolCategory.METHOD) {
      buffer.append(CommonParameterisedTypeDetails.asCommaSeparated(parameters, true));
    }

    return buffer.toString();
  }
}
