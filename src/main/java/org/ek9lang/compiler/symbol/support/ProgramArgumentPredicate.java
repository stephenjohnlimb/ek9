package org.ek9lang.compiler.symbol.support;

import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_BITS;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_BOOLEAN;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_CHARACTER;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_COLOUR;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_DATE;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_DATETIME;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_DIMENSION;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_DURATION;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_FLOAT;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_INTEGER;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_LIST;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_MILLISECOND;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_MONEY;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_REGEX;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_RESOLUTION;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_STRING;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_TIME;

import java.util.Set;
import java.util.function.Predicate;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ParameterisedSymbol;

/**
 * Check if this type being supplied to the test can be used as a program argument.
 * This is subset of the types EK9 supports.
 */
public class ProgramArgumentPredicate implements Predicate<ISymbol> {

  private static final Set<String> validTypes = Set.of(
      EK9_REGEX, EK9_MONEY, EK9_COLOUR, EK9_RESOLUTION, EK9_DIMENSION,
      EK9_MILLISECOND, EK9_DURATION, EK9_DATETIME, EK9_DATE, EK9_TIME,
      EK9_CHARACTER, EK9_STRING, EK9_INTEGER, EK9_FLOAT, EK9_BOOLEAN, EK9_BITS,
      EK9_LIST
  );

  @Override
  public boolean test(ISymbol typeSymbol) {

    if (typeSymbol != null) {
      if (typeSymbol.isParameterisedType() && typeSymbol instanceof ParameterisedSymbol parameterisedSymbol) {
        var theGenericTypeName = parameterisedSymbol.getParameterisableSymbol().getFullyQualifiedName();


        //Looks like could be OK, but if a generic List type then need to check that it is
        //only used with an EK9 String.
        if (EK9_LIST.equals(theGenericTypeName) && parameterisedSymbol.getParameterSymbols().size() == 1) {
          var parameterisedWith = parameterisedSymbol.getParameterSymbols().get(0).getFullyQualifiedName();
          return EK9_STRING.equals(parameterisedWith);
        }
      }
      return validTypes.contains(typeSymbol.getFullyQualifiedName());
    }
    return false;
  }
}
