package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.io.Serializable;

/**
 * Used to cache built in types as they are so frequently used.
 */
public record Ek9Types(AnyTypeSymbol ek9Any,
                       ISymbol ek9Void,
                       ISymbol ek9Boolean,
                       ISymbol ek9Integer,
                       ISymbol ek9Float,
                       ISymbol ek9String,
                       ISymbol ek9Character,
                       ISymbol ek9Bits,
                       ISymbol ek9Date,
                       ISymbol ek9DateTime,
                       ISymbol ek9Time,
                       ISymbol ek9Duration,
                       ISymbol ek9Json,
                       ISymbol ek9Millisecond,
                       ISymbol ek9Path,
                       ISymbol ek9Version,
                       ISymbol ek9HttpRequest,
                       ISymbol ek9HttpResponse,
                       ISymbol ek9Exception,
                       ISymbol ek9List,
                       ISymbol ek9Iterator,
                       ISymbol ek9Optional,
                       ISymbol ek9Dictionary,
                       ISymbol ek9DictionaryEntry,
                       ISymbol ek9Result,
                       ISymbol ek9Supplier,
                       ISymbol ek9Consumer,
                       ISymbol ek9Function,
                       ISymbol ek9Routine,
                       ISymbol ek9UnaryOperator,
                       ISymbol ek9Predicate,
                       ISymbol ek9Comparator,
                       ISymbol ek9IO) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
}
