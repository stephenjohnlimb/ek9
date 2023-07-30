package org.ek9lang.compiler.symbols.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.Module;
import org.ek9lang.compiler.Source;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.ek9lang.compiler.symbols.support.ToCommaSeparated;
import org.ek9lang.core.AssertValue;

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
   * Typically if searching for a method this will be zero or more type parameters.
   * But note these are not just a list of types they are the parameters with the type
   * set into that symbol.
   */
  private final List<ISymbol> typeParameters = new ArrayList<>();
  /**
   * These categories are considered viable 'types' to be searched for.
   */
  private final List<ISymbol.SymbolCategory> justTypes = List.of(
      ISymbol.SymbolCategory.TYPE,
      ISymbol.SymbolCategory.TEMPLATE_TYPE,
      ISymbol.SymbolCategory.TEMPLATE_FUNCTION,
      ISymbol.SymbolCategory.FUNCTION);
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
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<ISymbol> ofTypeOrReturn = Optional.empty();
  /**
   * What type of search is being triggered.
   */
  private ISymbol.SymbolCategory searchType = ISymbol.SymbolCategory.VARIABLE;
  private List<ISymbol.SymbolCategory> vetoSearchTypes = new ArrayList<>();
  /**
   * In some cases it's useful to have a symbol as an example to search for.
   */
  private ISymbol exampleSymbolToMatch;

  /**
   * Clone from Symbol search into a new instance.
   */
  @SuppressWarnings("CopyConstructorMissesField")
  public SymbolSearch(SymbolSearch from) {
    AssertValue.checkNotNull("from cannot be null for search Symbol", from);
    this.name = from.name;
    from.cloneIntoSearchSymbol(this);
  }

  /**
   * Use a symbol to populate the search.
   */
  public SymbolSearch(final ISymbol exampleSymbolToMatch) {
    AssertValue.checkNotNull("exampleSymbolToMatch cannot be null for search Symbol", exampleSymbolToMatch);
    this.name = exampleSymbolToMatch.getFullyQualifiedName();
    this.searchType = exampleSymbolToMatch.getCategory();
    this.exampleSymbolToMatch = exampleSymbolToMatch;
  }

  /**
   * Create a new search from an existing search with a new search name.
   */
  public SymbolSearch(String newName, SymbolSearch from) {
    AssertValue.checkNotNull("from cannot be null for search Symbol", from);
    AssertValue.checkNotNull("newName cannot be null for search Symbol", newName);
    this.name = newName;
    from.cloneIntoSearchSymbol(this);
  }

  public SymbolSearch(String name) {
    AssertValue.checkNotEmpty("name cannot be null for search Symbol", name);
    this.name = name;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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

  protected void cloneIntoSearchSymbol(SymbolSearch symbolSearch) {
    symbolSearch.setOfTypeOrReturn(ofTypeOrReturn)
        .setTypeParameters(typeParameters)
        .setLimitToBlocks(limitToBlocks)
        .setSearchType(searchType)
        .setExampleSymbolToMatch(exampleSymbolToMatch)
        .setVetoSearchTypes(vetoSearchTypes);
  }

  /**
   * Uses the search type and any vetos to see if the category passed in is a match on the search.
   */
  public boolean isCategoryAcceptable(final ISymbol.SymbolCategory category) {
    //If a single allowed search type must match
    if (searchType != null) {
      return searchType == category;
    }
    //Else could be one of the valid searches.
    return getValidSearchTypes().contains(category);
  }

  /**
   * Is this sort of type search, TYPE, FUNCTION or TEMPLATE versions of those.
   */
  public boolean isAnyValidTypeSearch() {
    if (searchType == null) {
      return justTypes.equals(getValidSearchTypes());
    }
    return justTypes.contains(searchType);
  }

  /**
   * Provide a list of valid search types if multiple supported.
   */
  public List<ISymbol.SymbolCategory> getValidSearchTypes() {
    if (searchType != null) {
      return List.of(searchType);
    }
    return Arrays.stream(ISymbol.SymbolCategory.values())
        .filter(category -> !vetoSearchTypes.contains(category))
        .toList();
  }

  @SuppressWarnings("UnusedReturnValue")
  public SymbolSearch setVetoSearchTypes(List<ISymbol.SymbolCategory> vetoSearchTypes) {
    this.vetoSearchTypes = vetoSearchTypes;
    return this;
  }

  public boolean isLimitToBlocks() {
    return limitToBlocks;
  }

  public SymbolSearch setLimitToBlocks(boolean limitToBlocks) {
    this.limitToBlocks = limitToBlocks;
    return this;
  }

  public SymbolSearch setExampleSymbolToMatch(ISymbol exampleSymbolToMatch) {
    this.exampleSymbolToMatch = exampleSymbolToMatch;
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
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public SymbolSearch setOfTypeOrReturn(Optional<ISymbol> ofTypeOrReturn) {
    AssertValue.checkNotNull("ofTypeOrReturn cannot be null for search Symbol", ofTypeOrReturn);
    this.ofTypeOrReturn = ofTypeOrReturn;
    return this;
  }

  /**
   * Get the parameters being used in this search.
   */
  public List<ISymbol> getTypeParameters() {
    return typeParameters;
  }

  /**
   * Set any parameters that might be needed as part of this search.
   */
  public SymbolSearch setTypeParameters(List<ISymbol> typeParameters) {
    AssertValue.checkNotNull("parameters cannot be null for search Symbol", typeParameters);
    typeParameters.forEach(this::addTypeParameter);
    return this;
  }

  /**
   * Add a parameter to the search parameters.
   * This is the 'type' symbol - i.e. Integer or whatever.
   * It is not a variable that 'has' a type.
   */
  public SymbolSearch addTypeParameter(ISymbol parameter) {
    AssertValue.checkNotNull("parameter cannot be null for search Symbol", parameter);
    this.typeParameters.add(parameter);
    return this;
  }

  /**
   * Add a type parameter to the search.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public SymbolSearch addTypeParameter(Optional<ISymbol> parameter) {
    AssertValue.checkNotNull("parameter cannot be null for search Symbol", parameter);
    AssertValue.checkTrue("parameter cannot be empty for search Symbol", parameter.isPresent());
    this.typeParameters.add(parameter.get());
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
   * Creates a new Symbol from the name in the search and the module name.
   */
  public Optional<ISymbol> getAsSymbol() {

    if (exampleSymbolToMatch != null) {
      return Optional.of(exampleSymbolToMatch);
    }

    final var newSymbolModuleName = ISymbol.getModuleNameIfPresent(name);
    final String newSymbolName = ISymbol.getUnqualifiedName(name);

    //Make a new symbol (synthetic) and ensure it has the correct module name.
    //Also ensure it has a valid unqualified name in that scope.
    final var symbol = new AggregateSymbol(newSymbolName, new SymbolTable(newSymbolModuleName));
    symbol.setParsedModule(Optional.of(new Module() {
      @Override
      public Source getSource() {
        return () -> "syntheticSource.ek9";
      }

      @Override
      public String getScopeName() {
        return newSymbolModuleName;
      }
    }));

    return Optional.of(symbol);
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    var toCommaSeparated = new ToCommaSeparated(true);
    StringBuilder buffer = new StringBuilder();

    getOfTypeOrReturn().ifPresent(returnType -> buffer.append(returnType.getName()).append(" <- "));
    buffer.append(getName());

    if (this.searchType == ISymbol.SymbolCategory.METHOD) {
      buffer.append(toCommaSeparated.apply(typeParameters));
    }

    return buffer.toString();
  }
}
