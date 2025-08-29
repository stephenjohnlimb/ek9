package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BITS;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BOOLEAN;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_CHARACTER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_COLOUR;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DATE;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DATETIME;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DIMENSION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DURATION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_FLOAT;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_INTEGER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_LIST;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_LIST_OF_STRING;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_MILLISECOND;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_MONEY;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_REGEX;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_RESOLUTION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_STRING;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_TIME;

import java.util.Set;
import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Check if this type being supplied to the test can be used as a program argument.
 * This is subset of the types EK9 supports.
 * Only a finite set of values of EK9 types can easily be identified and converted to actual typed values.
 */
public class ProgramArgumentPredicate implements Predicate<ISymbol> {

  private static final Set<String> validTypes = Set.of(
      EK9_REGEX, EK9_MONEY, EK9_COLOUR, EK9_RESOLUTION, EK9_DIMENSION,
      EK9_MILLISECOND, EK9_DURATION, EK9_DATETIME, EK9_DATE, EK9_TIME,
      EK9_CHARACTER, EK9_STRING, EK9_INTEGER, EK9_FLOAT, EK9_BOOLEAN, EK9_BITS,
      EK9_LIST_OF_STRING
  );

  @Override
  public boolean test(final ISymbol typeSymbol) {

    if (typeSymbol != null) {
      if (typeSymbol.isParameterisedType() && typeSymbol instanceof PossibleGenericSymbol parameterisedSymbol
          && parameterisedSymbol.getGenericType().isPresent()) {

        final var theGenericTypeName = parameterisedSymbol.getGenericType().get().getFullyQualifiedName();

        //Looks like could be OK, but if a generic List type then need to check that it is
        //only used with an EK9 String.
        if (EK9_LIST.equals(theGenericTypeName) && parameterisedSymbol.getTypeParameterOrArguments().size() == 1) {
          final var parameterisedWith =
              parameterisedSymbol.getTypeParameterOrArguments().getFirst().getFullyQualifiedName();
          return EK9_STRING.equals(parameterisedWith);
        }

      }

      return validTypes.contains(typeSymbol.getFullyQualifiedName());
    }

    return false;
  }
}
