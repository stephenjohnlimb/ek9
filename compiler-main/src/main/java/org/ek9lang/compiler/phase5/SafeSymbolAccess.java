package org.ek9lang.compiler.phase5;

import java.util.Map;
import java.util.function.Predicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.ISymbol;

abstract class SafeSymbolAccess extends TypedSymbolAccess {

  protected final SafeGenericAccessMarker safeGenericAccessMarker;
  protected final SafeGenericAccessPredicate safeGenericAccessPredicate;

  //Used to map a specific method name to a squirrelled data element.
  //If present then that method call has to be preceded by a check call so as not to trigger
  //a possible exception. We emit compiler errors to try and prevent this occurring.
  private final Map<String, CommonValues> methodNameLookup =
      Map.of("ok", CommonValues.OK_ACCESS_REQUIRES_SAFE_ACCESS,
          "error", CommonValues.ERROR_ACCESS_REQUIRES_SAFE_ACCESS,
          "get", CommonValues.GET_ACCESS_REQUIRES_SAFE_ACCESS,
          "next", CommonValues.NEXT_ACCESS_REQUIRES_SAFE_ACCESS);

  private final Map<CommonValues, Predicate<ISymbol>> methodLookup;

  SafeSymbolAccess(final SymbolsAndScopes symbolsAndScopes,
                   final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.safeGenericAccessMarker = new SafeGenericAccessMarker(symbolsAndScopes);
    this.safeGenericAccessPredicate = new SafeGenericAccessPredicate(symbolsAndScopes);

    //Now create the map of methods to call for specific checks.
    methodLookup = Map.of(CommonValues.OK_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::isOkResultAccessSafe,
        CommonValues.ERROR_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::isErrorResultAccessSafe,
        CommonValues.GET_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::isGetOptionalAccessSafe,
        CommonValues.NEXT_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::isNextIteratorAccessSafe);
  }

  protected boolean isAccessUnSafe(final ISymbol calledFromSymbol, final String methodName) {

    final var squirrelledLookupValue = methodNameLookup.get(methodName);
    if (squirrelledLookupValue != null && calledFromSymbol.getSquirrelledData(squirrelledLookupValue) != null) {
      final var toCall = methodLookup.get(squirrelledLookupValue);
      return !toCall.test(calledFromSymbol);
    }

    return false;
  }
}
