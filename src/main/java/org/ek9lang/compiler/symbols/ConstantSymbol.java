package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.Optional;

/**
 * A very simple type that holds a single distinct value.
 * For example PI = 3.242 would be a constant.
 */
public class ConstantSymbol extends Symbol {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Is this constant defined as a literal.
   */
  private boolean literal;

  public ConstantSymbol(final String name, final boolean fromLiteral) {

    this(name, Optional.empty(), fromLiteral);

  }

  public ConstantSymbol(final String name) {

    this(name, Optional.empty(), false);

  }

  public ConstantSymbol(final String name, final ISymbol type, final boolean fromLiteral) {

    this(name, Optional.ofNullable(type), fromLiteral);

  }

  public ConstantSymbol(final String name, final ISymbol type) {

    this(name, Optional.ofNullable(type), false);

  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public ConstantSymbol(final String name, final Optional<ISymbol> type) {

    this(name, type, false);

  }

  /**
   * Construction of a Constant.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public ConstantSymbol(final String name, final Optional<ISymbol> type, final boolean fromLiteral) {

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
  public ConstantSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoConstant(new ConstantSymbol(this.getName(), this.getType()));
  }

  protected ConstantSymbol cloneIntoConstant(final ConstantSymbol newCopy) {

    cloneIntoSymbol(newCopy);
    newCopy.literal = this.literal;

    return newCopy;
  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }
    if (!(o instanceof ConstantSymbol that)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    return literal == that.literal;
  }

  @Override
  public int hashCode() {

    int result = super.hashCode();
    result = 31 * result + (getSourceToken() != null ? getSourceToken().hashCode() : 0);
    result = 31 * result + (literal ? 1 : 0);

    return result;
  }

  @Override
  public String toString() {

    return "const " + getFriendlyName();
  }
}