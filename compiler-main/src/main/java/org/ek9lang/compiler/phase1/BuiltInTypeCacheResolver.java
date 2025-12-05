package org.ek9lang.compiler.phase1;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_ANY;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BITS;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BOOLEAN;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_CHARACTER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_COMPARATOR;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_CONSUMER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DATE;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DATETIME;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DICTIONARY;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DICTIONARY_ENTRY;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DURATION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_EXCEPTION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_FLOAT;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_FUNCTION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_HTTP_REQUEST;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_HTTP_RESPONSE;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_INTEGER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_IO;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_ITERATOR;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_JSON;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_LIST;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_MILLISECOND;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_OPTIONAL;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_PATH;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_PREDICATE;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_RESULT;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_ROUTINE;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_STRING;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_SUPPLIER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_TIME;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_UNARY_OPERATOR;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_VERSION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_VOID;

import java.util.function.Function;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.search.TemplateFunctionSymbolSearch;
import org.ek9lang.compiler.search.TemplateTypeSymbolSearch;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.AnyTypeSymbol;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;

/**
 * Just resolves a number of built-in types so that internal compiler code can be more explicit.
 */
final class BuiltInTypeCacheResolver implements Function<IScope, Ek9Types> {

  BuiltInTypeCacheResolver() {

  }

  @Override
  public Ek9Types apply(final IScope scope) {

    if ("org.ek9.lang".equals(scope.getScopeName())) {
      return new Ek9Types(
          resolveAny(scope),
          resolveType(scope, EK9_VOID),
          resolveType(scope, EK9_BOOLEAN),
          resolveType(scope, EK9_INTEGER),
          resolveType(scope, EK9_FLOAT),
          resolveType(scope, EK9_STRING),
          resolveType(scope, EK9_CHARACTER),
          resolveType(scope, EK9_BITS),
          resolveType(scope, EK9_DATE),
          resolveType(scope, EK9_DATETIME),
          resolveType(scope, EK9_TIME),
          resolveType(scope, EK9_DURATION),
          resolveType(scope, EK9_JSON),
          resolveType(scope, EK9_MILLISECOND),
          resolveType(scope, EK9_PATH),
          resolveType(scope, EK9_VERSION),
          resolveType(scope, EK9_HTTP_REQUEST),
          resolveType(scope, EK9_HTTP_RESPONSE),
          resolveType(scope, EK9_EXCEPTION),
          resolveTemplateType(scope, EK9_LIST),
          resolveTemplateType(scope, EK9_ITERATOR),
          resolveTemplateType(scope, EK9_OPTIONAL),
          resolveTemplateType(scope, EK9_DICTIONARY),
          resolveTemplateType(scope, EK9_DICTIONARY_ENTRY),
          resolveTemplateType(scope, EK9_RESULT),
          resolveTemplateFunction(scope, EK9_SUPPLIER),
          resolveTemplateFunction(scope, EK9_CONSUMER),
          resolveTemplateFunction(scope, EK9_FUNCTION),
          resolveTemplateFunction(scope, EK9_ROUTINE),
          resolveTemplateFunction(scope, EK9_UNARY_OPERATOR),
          resolveTemplateFunction(scope, EK9_PREDICATE),
          resolveTemplateFunction(scope, EK9_COMPARATOR),
          resolveType(scope, EK9_IO));
    }

    return null;

  }

  private AnyTypeSymbol resolveAny(final IScope scope) {
    final var ek9AnySearch = new SymbolSearch(EK9_ANY).setSearchType(SymbolCategory.ANY);
    final var resolved = scope.resolve(ek9AnySearch);
    if (resolved.isPresent()) {
      return (AnyTypeSymbol) resolved.get();
    }
    throw new RuntimeException("Unable to resolve type " + EK9_ANY);
  }

  private ISymbol resolveType(final IScope scope, final String type) {
    return scope.resolve(new TypeSymbolSearch(type))
        .orElseThrow(() -> new RuntimeException("Unable to resolve type " + type));
  }

  private ISymbol resolveTemplateType(final IScope scope, final String type) {
    return scope.resolve(new TemplateTypeSymbolSearch(type))
        .orElseThrow(() -> new RuntimeException("Unable to resolve template type " + type));
  }

  private ISymbol resolveTemplateFunction(final IScope scope, final String type) {
    return scope.resolve(new TemplateFunctionSymbolSearch(type))
        .orElseThrow(() -> new RuntimeException("Unable to resolve template function " + type));
  }
}
