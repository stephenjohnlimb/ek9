package org.ek9lang.compiler.symbol;

import java.util.Objects;
import java.util.Optional;

/**
 * A very simple type that holds a single distinct value.
 * For example PI = 3.242 would be a constant.
 */
public class ConstantSymbol extends Symbol {

  /**
   * Is this constant defined as a literal.
   */
  private boolean literal;

  public ConstantSymbol(String name, boolean fromLiteral) {
    this(name, Optional.empty(), fromLiteral);
  }

  public ConstantSymbol(String name) {
    this(name, Optional.empty(), false);
  }

  public ConstantSymbol(String name, ISymbol type, boolean fromLiteral) {
    this(name, Optional.ofNullable(type), fromLiteral);
  }

  public ConstantSymbol(String name, ISymbol type) {
    this(name, Optional.ofNullable(type), false);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public ConstantSymbol(String name, Optional<ISymbol> type) {
    this(name, type, false);
  }

  /**
   * Construction of a Constant.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public ConstantSymbol(String name, Optional<ISymbol> type, boolean fromLiteral) {
    super(name, type);
    super.setGenus(SymbolGenus.VALUE);
    this.literal = fromLiteral;
    super.setProduceFullyQualifiedName(!fromLiteral);
  }

  @Override
  public boolean isFromLiteral() {
    return literal;
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  @Override
  public ConstantSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoConstant(new ConstantSymbol(this.getName(), this.getType()));
  }

  protected ConstantSymbol cloneIntoConstant(ConstantSymbol newCopy) {
    cloneIntoSymbol(newCopy);
    newCopy.literal = this.literal;
    return newCopy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.literal);
  }

  @Override
  public boolean equals(Object obj) {
    var rtn = super.equals(obj);
    if (rtn && obj instanceof ConstantSymbol constant) {
      rtn = constant.literal == this.literal;
    }
    return rtn;
  }

  @Override
  public String toString() {
    return "const " + getFriendlyName();
  }
}