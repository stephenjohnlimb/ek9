package org.ek9lang.compiler.symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.internals.Module;
import org.ek9lang.compiler.symbol.support.TypeCoercions;
import org.ek9lang.core.exception.AssertValue;

/**
 * A bit of a beast.
 * Is extended widely for particular types of symbol.
 */
public class Symbol implements ISymbol {
  //Sometimes it is necessary to pick data and hold it in the symbol for later.
  private final Map<String, String> squirrelledAway = new HashMap<>();
  //This also covers constants - change mutability for those.
  private SymbolCategory category = SymbolCategory.VARIABLE;
  private SymbolGenus genus = SymbolGenus.CLASS;
  private String name;
  private Optional<ISymbol> type = Optional.empty();
  private boolean hasBeenSet = false;
  /**
   * Not always set if ek9 core.
   * But is the token where this symbol was defined.
   */
  private Token sourceToken = null;
  /**
   * This is the token that initialised the symbol.
   * Typically, ony valid for Variables and expressions.
   */
  private Token initialisedBy = null;
  /**
   * Was this symbol referenced.
   */
  private boolean referenced = false;
  /**
   * The idea is to assume that symbol cannot be null by default.
   * i.e a variable cannot null or a function/method cannot return a null value
   * If the developer wants to support then ? has to be used to make that explicit.
   */
  private boolean nullAllowed = false;
  //For components, we need to know if injection is expected.
  private boolean injectionExpected = false;
  /**
   * Used to mark a symbol as ek9 core or not.
   * Normally used to drive the generation of fully qualified names.
   */
  private boolean ek9Core = false;
  /**
   * Is this symbol marked as being pure.
   */
  private boolean markedPure = false;
  private boolean produceFullyQualifiedName = false;
  /**
   * Where this symbol come from - not always set for every symbol.
   */
  private Optional<Module> parsedModule = Optional.empty();

  public Symbol(String name) {
    this.setName(name);
  }

  public Symbol(String name, Optional<ISymbol> type) {
    this(name);
    this.setType(type);
  }

  @Override
  public Symbol clone(IScope withParentAsAppropriate) {
    return cloneIntoSymbol(new Symbol(this.getName(), this.getType()));
  }

  protected Symbol cloneIntoSymbol(Symbol newCopy) {
    newCopy.setInitialisedBy(this.getInitialisedBy());
    newCopy.setCategory(this.getCategory());
    newCopy.setGenus(this.getGenus());
    newCopy.setName(this.getName());
    newCopy.hasBeenSet = this.hasBeenSet;
    newCopy.setNullAllowed(this.isNullAllowed());
    newCopy.setInjectionExpected(this.isInjectionExpected());
    newCopy.setEk9Core(this.isEk9Core());
    newCopy.setMarkedPure(this.isMarkedPure());
    newCopy.setProduceFullyQualifiedName(this.getProduceFullyQualifiedName());
    parsedModule.ifPresent(newCopy::doSetModule);
    newCopy.setSourceToken(this.getSourceToken());
    newCopy.squirrelledAway.putAll(this.squirrelledAway);
    newCopy.setReferenced(this.isReferenced());

    if (!(this instanceof AggregateSymbol)) {
      getType().ifPresent(newCopy::setType);
    }
    return newCopy;
  }

  public boolean isMarkedPure() {
    return markedPure;
  }

  public void setMarkedPure(boolean markedPure) {
    this.markedPure = markedPure;
  }

  public Token getInitialisedBy() {
    return initialisedBy;
  }

  public void setInitialisedBy(Token initialisedBy) {
    this.initialisedBy = initialisedBy;
  }

  public boolean isReferenced() {
    return referenced;
  }

  public void setReferenced(boolean referenced) {
    this.referenced = referenced;
  }

  /**
   * Sometimes it's important to 'squirrel' away information during different phases of the
   * compilation. This mechanism on a symbol enables arbitrary data to be recorded against
   * a symbol. This can then be retrieved in later stages of compilation.
   */
  public void putSquirrelledData(String key, String value) {
    if (value.startsWith("\"") && value.endsWith("\"")) {
      //We only store the value that is in the Quotes not the quotes themselves
      Pattern p = Pattern.compile("\"(.*)\""); //just the contents in the quotes
      Matcher m = p.matcher(value);
      if (m.find()) {
        value = m.group(1);
      }
    }
    squirrelledAway.put(key, value);
  }

  public String getSquirrelledData(String key) {
    return squirrelledAway.get(key);
  }

  @Override
  public boolean isMutable() {
    return !hasBeenSet;
  }

  @Override
  public void setNotMutable() {
    hasBeenSet = true;
  }

  @Override
  public boolean isLoopVariable() {
    return false;
  }

  @Override
  public boolean isIncomingParameter() {
    return false;
  }

  @Override
  public boolean isReturningParameter() {
    return false;
  }

  @Override
  public boolean isAggregatePropertyField() {
    return false;
  }

  public boolean isNullAllowed() {
    return nullAllowed;
  }

  public void setNullAllowed(boolean nullAllowed) {
    this.nullAllowed = nullAllowed;
  }

  public boolean isInjectionExpected() {
    return injectionExpected;
  }

  public void setInjectionExpected(boolean injectionExpected) {
    this.injectionExpected = injectionExpected;
  }

  public boolean isEk9Core() {
    return ek9Core;
  }

  /**
   * set this symbol as an EK9 core symbol.
   * This means it is designed and built right into the language
   * or is part of the standard EK9 library.
   */
  public void setEk9Core(boolean ek9Core) {
    this.ek9Core = ek9Core;
  }

  public boolean isDevSource() {
    return parsedModule.map(m -> m.getSource().isDev()).orElse(false);
  }

  public boolean isLibSource() {
    return parsedModule.map(m -> m.getSource().isLib()).orElse(false);
  }

  public boolean getProduceFullyQualifiedName() {
    return produceFullyQualifiedName;
  }

  public void setProduceFullyQualifiedName(boolean produceFullyQualifiedName) {
    this.produceFullyQualifiedName = produceFullyQualifiedName;
  }

  public Token getSourceToken() {
    return sourceToken;
  }

  public void setSourceToken(Token sourceToken) {
    this.sourceToken = sourceToken;
  }

  public Optional<Module> getParsedModule() {
    return parsedModule;
  }

  public void setParsedModule(Optional<Module> module) {
    module.ifPresent(this::doSetModule);
  }

  private void doSetModule(Module module) {
    if (parsedModule.isEmpty()) {
      parsedModule = Optional.of(module);
    }
    this.setEk9Core(module.isEk9Core());
  }

  @Override
  public String getFullyQualifiedName() {
    String rtn = getName();
    return parsedModule.map(m -> ISymbol.makeFullyQualifiedName(m.getScopeName(), rtn)).orElse(rtn);
  }

  /**
   * Checks if the type match exactly.
   */
  public boolean isExactSameType(ISymbol symbolType) {
    //If this is a T or U or whatever then just use the name as is.
    if (this.isGenericTypeParameter()) {
      return getName().equals(symbolType.getName());
    }
    return sameCategory(this, symbolType)
        && getFullyQualifiedName().equals(symbolType.getFullyQualifiedName());
  }

  private boolean sameCategory(ISymbol c1, ISymbol c2) {
    return c1.getCategory().equals(c2.getCategory());
  }

  @Override
  public boolean isAssignableTo(ISymbol s) {
    return getAssignableWeightTo(s) >= 0.0;
  }

  @Override
  public boolean isAssignableTo(Optional<ISymbol> s) {
    return getAssignableWeightTo(s) >= 0.0;
  }

  @Override
  public double getAssignableWeightTo(Optional<ISymbol> s) {
    return s.map(this::getAssignableWeightTo).orElse(-1000000.0);
  }

  @Override
  public double getAssignableWeightTo(ISymbol s) {
    double canAssign = getUnCoercedAssignableWeightTo(s);
    //Well if not the same symbol can we coerce/promote?
    if (canAssign < 0.0 && TypeCoercions.get().isCoercible(this, s)) {
      canAssign = 0.5;
    }
    return canAssign;
  }

  @Override
  public boolean isPromotionSupported(ISymbol s) {
    //Check if any need to promote might be same type
    var rtn = false;
    if (!isExactSameType(s)) {
      rtn = TypeCoercions.get().isCoercible(this, s);
    }
    return rtn;
  }


  @Override
  public double getUnCoercedAssignableWeightTo(ISymbol s) {
    double canAssign = -1000000.0;
    AssertValue.checkNotNull("Symbol cannot be null", s);

    if (isExactSameType(s)) {
      canAssign = 0.0;
    }
    return canAssign;
  }

  public SymbolCategory getCategory() {
    return category;
  }

  protected void setCategory(SymbolCategory category) {
    this.category = category;
  }

  public SymbolGenus getGenus() {
    return genus;
  }

  public void setGenus(SymbolGenus genus) {
    this.genus = genus;
  }

  @Override
  public int hashCode() {
    return getFriendlyName().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    var rtn = false;
    if (obj instanceof ISymbol symbol) {
      rtn = getFriendlyName().equals(symbol.getFriendlyName());
    }
    return rtn;
  }

  @Override
  public String getFriendlyName() {
    String theType = getSymbolTypeAsString(this.getType());
    if (theType.length() > 0) {
      return getName() + " as " + theType;
    }
    return getName();
  }

  protected String getSymbolTypeAsString(Optional<ISymbol> type) {
    final var self = this;
    //The problem of using theType.getFriendlyName is that aggregate has method -> back to same aggregate
    //So not a direct type is the same type - but aggregate -> method -> uses aggregate -> method  ... stack overflow
    return type.map(theType -> self != theType ? theType.getName() : "").orElse("Unknown");
  }

  @Override
  public String toString() {
    return getFriendlyName();
  }

  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    AssertValue.checkNotNull("Name cannot be null", name);
    this.name = name;
  }

  @Override
  public Optional<ISymbol> getType() {
    return type;
  }

  @Override
  public ISymbol setType(Optional<ISymbol> type) {
    AssertValue.checkNotNull("SymbolType cannot be null", type);
    this.type = type;
    return this;
  }

  @Override
  public boolean isParameterisedType() {
    return false;
  }

  @Override
  public boolean isGenericInNature() {
    return false;
  }

  @Override
  public boolean isGenericTypeParameter() {
    return false;
  }

}
