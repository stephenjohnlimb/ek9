package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.EnumMap;
import java.util.Optional;
import java.util.regex.Pattern;
import org.ek9lang.compiler.Module;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.support.TypeCoercions;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * A bit of a beast.
 * The 'Symbol' can be a type, a function, a variable, a control statement,
 * i.e. it can be used in a very wide range of contexts.
 * Is extended widely for particular types of symbol.
 */
public class Symbol implements ISymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * This is the 'bit bucket' where during the parsing and IR phases
   * information about the symbol can be augmented.
   */
  private final EnumMap<CommonValues, String> squirrelledAway = new EnumMap<>(CommonValues.class);

  /**
   * This tells you what nature this symbol has.
   */
  private SymbolCategory category = SymbolCategory.VARIABLE;

  /**
   * What sort of ek9 'construct' is this.
   */
  private SymbolGenus genus = SymbolGenus.CLASS;

  /**
   * The actual name of the symbol, but can be 'mangled' or conceptual.
   */
  private String name;

  /**
   * So for some symbols (some controls) this makes little sense, they cannot have a 'type'.
   * But a variable call 'x' will have a type - it could be an 'Integer' for example.
   */
  private ISymbol type;

  /**
   * Is this mutable or not.
   */
  private boolean notMutable = false;

  /**
   * The token where this symbol was defined.
   */
  private IToken sourceToken = null;

  /**
   * This is the token that initialised the symbol.
   * Typically, ony valid for Variables and expressions.
   * But if we're talking components, they will get injected with '!'.
   * So this may just be set when the variable is declared as having an 'injected' assignment.
   */
  private IToken initialisedBy = null;

  /**
   * Was this symbol referenced.
   * Typically, for variables, rules may check this - because if not referenced
   * what's the point. But with IOC and injection this is sometimes more complex to establish.
   */
  private boolean referenced = false;

  /**
   * The idea is to assume that symbol cannot be null by default.
   * i.e a variable cannot null or a function/method cannot return a null value
   * If the developer wants to support then ? has to be used to make that explicit.
   * In an ideal world we would have eradicated 'null', but to support specific concepts like
   * generics and abstract types - a null value has to be available in someway.
   * because how to you initialise 'var as T' - where the 'T' is an abstract type that cannot be
   * instantiated?
   */
  private boolean nullAllowed = false;

  /**
   * For components, we need to know if injection is expected.
   */
  private boolean injectionExpected = false;

  /**
   * Used to mark a symbol as ek9 core or not.
   */
  private boolean ek9Core = false;

  /**
   * Is this symbol marked as being pure.
   * The concept of 'pure' is focussed on immutability.
   * In EK9 there is no 'const', 'final' or anything like that.
   * The concept is based around 'processing' i.e. a function/method etc
   * can be marked as 'pure'. Within that 'pure block' there are limits on
   * variable reassignments.
   */
  private boolean markedPure = false;
  private boolean produceFullyQualifiedName = false;

  /**
   * Where this symbol come from - not always set for every symbol.
   * Try to remove this, squirrel data where possible and move
   * Also try using source file name from tokenSource.
   */
  private Module parsedModule;

  public Symbol(final String name) {

    this.setName(name);

  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public Symbol(final String name, final Optional<ISymbol> type) {

    this(name);
    this.setType(type);

  }

  @Override
  public Symbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoSymbol(new Symbol(this.getName(), this.getType()));
  }

  protected Symbol cloneIntoSymbol(final Symbol newCopy) {

    return copySymbolProperties(newCopy);
  }

  /**
   * Just copies the properties over.
   */
  public Symbol copySymbolProperties(final Symbol newCopy) {

    newCopy.setInitialisedBy(this.getInitialisedBy());
    newCopy.setCategory(this.getCategory());
    newCopy.setGenus(this.getGenus());
    newCopy.setName(this.getName());
    newCopy.notMutable = this.notMutable;
    newCopy.setNullAllowed(this.isNullAllowed());
    newCopy.setInjectionExpected(this.isInjectionExpected());
    newCopy.setEk9Core(this.isEk9Core());
    newCopy.setMarkedPure(this.isMarkedPure());
    newCopy.setProduceFullyQualifiedName(this.getProduceFullyQualifiedName());
    newCopy.doSetModule(parsedModule);
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

  public void setMarkedPure(final boolean markedPure) {

    this.markedPure = markedPure;

  }

  public boolean isReferenced() {

    return referenced;
  }

  public void setReferenced(final boolean referenced) {

    this.referenced = referenced;

  }

  /**
   * Sometimes it's important to 'squirrel' away information during different phases of the
   * compilation. This mechanism on a symbol enables arbitrary data to be recorded against
   * a symbol. This can then be retrieved in later stages of compilation.
   */
  public void putSquirrelledData(final CommonValues key, final String value) {

    if (value.startsWith("\"") && value.endsWith("\"")) {
      //We only store the value that is in the Quotes not the quotes themselves
      final var p = Pattern.compile("\"(.*)\""); //just the contents in the quotes
      final var m = p.matcher(value);
      if (m.find()) {
        squirrelledAway.put(key, m.group(1));
        return;
      }
    }

    squirrelledAway.put(key, value);

  }

  public String getSquirrelledData(final CommonValues key) {

    return squirrelledAway.get(key);
  }

  @Override
  public boolean isMutable() {

    return !notMutable;
  }

  @Override
  public void setNotMutable() {

    notMutable = true;

  }

  public boolean isNullAllowed() {

    return nullAllowed;
  }

  public void setNullAllowed(final boolean nullAllowed) {

    this.nullAllowed = nullAllowed;

  }

  public boolean isInjectionExpected() {

    return injectionExpected;

  }

  public void setInjectionExpected(final boolean injectionExpected) {

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
  public void setEk9Core(final boolean ek9Core) {

    this.ek9Core = ek9Core;

  }

  public boolean isDevSource() {

    //Consider squirreling dev at construction.
    return getParsedModule().map(m -> m.getSource().isDev()).orElse(false);
  }

  public boolean isLibSource() {

    //Consider squirreling scopeName at construction.
    return getParsedModule().map(m -> m.getSource().isLib()).orElse(false);
  }

  public boolean getProduceFullyQualifiedName() {

    return produceFullyQualifiedName;
  }

  public void setProduceFullyQualifiedName(final boolean produceFullyQualifiedName) {

    this.produceFullyQualifiedName = produceFullyQualifiedName;

  }

  public IToken getSourceToken() {

    return sourceToken;
  }

  public void setSourceToken(final IToken sourceToken) {

    this.sourceToken = sourceToken;
  }

  @Override
  public IToken getInitialisedBy() {

    return initialisedBy;
  }

  @Override
  public void setInitialisedBy(final IToken initialisedBy) {

    this.initialisedBy = initialisedBy;

  }

  @Override
  public void clearInitialisedBy() {

    this.initialisedBy = null;

  }

  public Optional<Module> getParsedModule() {

    return Optional.ofNullable(parsedModule);
  }

  public void setParsedModule(final Optional<Module> module) {

    module.ifPresent(this::doSetModule);

  }

  private void doSetModule(final Module module) {

    if (parsedModule == null) {
      parsedModule = module;
    }

    if (parsedModule != null) {
      this.setEk9Core(parsedModule.isEk9Core());
    }

  }

  @Override
  public String getFullyQualifiedName() {

    final var rtn = getName();
    final var prefix = getSquirrelledData(CommonValues.GENERIC_PARENT);

    //This 'isConceptualTypeParameter' not just for 'T' but also conceptual types
    if (this.isConceptualTypeParameter() && prefix != null) {

      return INaming.makeFullyQualifiedName(prefix, rtn);
    }

    //Consider squirreling scopeName at construction.
    return getParsedModule().map(m -> INaming.makeFullyQualifiedName(m.getScopeName(), rtn)).orElse(rtn);
  }

  /**
   * Checks if the type match exactly.
   */
  public boolean isExactSameType(final ISymbol symbolType) {

    final var thisFullyQualified = getFullyQualifiedName();
    final var thatFullyQualified = symbolType.getFullyQualifiedName();

    return sameCategory(this, symbolType) && thisFullyQualified.equals(thatFullyQualified);
  }

  private boolean sameCategory(final ISymbol c1, final ISymbol c2) {

    return c1.getCategory().equals(c2.getCategory());
  }

  @Override
  public boolean isAssignableTo(final ISymbol s) {

    return getAssignableWeightTo(s) >= 0.0;
  }

  @Override
  public boolean isAssignableTo(final Optional<ISymbol> s) {

    return getAssignableWeightTo(s) >= 0.0;
  }

  @Override
  public double getAssignableWeightTo(final Optional<ISymbol> s) {

    return s.map(this::getAssignableWeightTo).orElse(NOT_ASSIGNABLE);
  }

  @Override
  public double getAssignableWeightTo(final ISymbol s) {

    final var canAssign = getUnCoercedAssignableWeightTo(s);

    //Well if not the same symbol can we coerce/promote?
    if (canAssign < 0.0 && TypeCoercions.get().isCoercible(this, s)) {
      return 0.5;
    }

    return canAssign;
  }

  @Override
  public boolean isPromotionSupported(final ISymbol s) {

    //Check if any need to promote might be same type
    if (!isExactSameType(s)) {
      return TypeCoercions.get().isCoercible(this, s);
    }

    return false;
  }

  @Override
  public double getUnCoercedAssignableWeightTo(final ISymbol s) {

    AssertValue.checkNotNull("Symbol cannot be null", s);
    if (isExactSameType(s)) {
      return 0.0;
    }

    return NOT_ASSIGNABLE;
  }

  public SymbolCategory getCategory() {

    return category;
  }

  public void setCategory(final SymbolCategory category) {

    this.category = category;

  }

  public SymbolGenus getGenus() {

    return genus;
  }

  public void setGenus(final SymbolGenus genus) {

    this.genus = genus;

  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }

    return (o instanceof Symbol symbol)
        && notMutable == symbol.notMutable
        && getCategory() == symbol.getCategory()
        && getGenus() == symbol.getGenus()
        && getName().equals(symbol.getName());
  }

  @Override
  public int hashCode() {

    int result = getCategory().hashCode();
    result = 31 * result + getGenus().hashCode();
    result = 31 * result + getName().hashCode();
    result = 31 * result + (notMutable ? 1 : 0);
    result = 31 * result + (getSourceToken() != null ? getSourceToken().hashCode() : 0);
    result = 31 * result + (isMarkedPure() ? 1 : 0);

    return result;
  }

  @Override
  public String getFriendlyName() {

    final var theType = getSymbolTypeAsString(this.getType());
    if (!theType.isEmpty()) {
      return getName() + " as " + theType;
    }

    return getName();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  protected String getSymbolTypeAsString(final Optional<ISymbol> type) {

    final var self = this;

    //The problem of using theType.getFriendlyName is that aggregate has method -> back to same aggregate
    //So not a direct type is the same type - but aggregate -> method -> uses aggregate -> method  ... stack overflow
    return type.map(theType -> self != theType ? theType.getFriendlyName() : "").orElse("Unknown");
  }

  @Override
  public String toString() {

    return getFriendlyName();
  }

  public String getName() {

    return name;
  }

  @Override
  public void setName(final String name) {

    AssertValue.checkNotNull("Name cannot be null", name);
    this.name = name;

  }

  @Override
  public Optional<ISymbol> getType() {

    return Optional.ofNullable(type);
  }

  @Override
  public ISymbol setType(final Optional<ISymbol> type) {

    AssertValue.checkNotNull("SymbolType cannot be null", type);
    type.ifPresentOrElse(newType -> this.type = newType, () -> this.type = null);

    return this;
  }

}
