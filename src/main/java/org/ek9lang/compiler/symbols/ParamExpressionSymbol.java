package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ek9lang.compiler.support.ToCommaSeparated;

/**
 * While we don't add these in the scoped structures when compiling.
 * We do use these to augment the parse tree for the appropriate context.
 * We do this so that we can work out what type of result will be returned from an expression.
 * For this call we need the order list of parameters specifically the types of those parameters.
 */
public class ParamExpressionSymbol extends Symbol {

  @Serial
  private static final long serialVersionUID = 1L;

  private final List<ISymbol> params = new ArrayList<>();

  public ParamExpressionSymbol(String name) {
    super(name);
  }

  public void addParameter(ISymbol symbol) {
    params.add(symbol);
  }

  @Override
  public ParamExpressionSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoStreamPipeLineSymbol(new ParamExpressionSymbol(getName()));
  }

  protected ParamExpressionSymbol cloneIntoStreamPipeLineSymbol(final ParamExpressionSymbol newCopy) {

    super.cloneIntoSymbol(newCopy);
    getParameters().forEach(newCopy::addParameter);

    return newCopy;
  }

  public List<ISymbol> getParameters() {

    return Collections.unmodifiableList(params);
  }

  @Override
  public String getFriendlyName() {

    return new ToCommaSeparated(true).apply(params);
  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }

    return (o instanceof ParamExpressionSymbol that)
        && super.equals(o)
        && params.equals(that.params);
  }

  @Override
  public int hashCode() {

    int result = super.hashCode();
    result = 31 * result + params.hashCode();

    return result;
  }

}
