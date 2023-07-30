package org.ek9lang.compiler.main.phases.definition;

import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_BOOLEAN;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_CHARACTER;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_COMPARATOR;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_CONSUMER;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_DATE;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_DATETIME;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_DICTIONARY;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_DICTIONARY_ENTRY;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_DURATION;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_EXCEPTION;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_FLOAT;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_FUNCTION;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_HTTP_REQUEST;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_HTTP_RESPONSE;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_INTEGER;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_ITERATOR;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_JSON;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_LIST;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_MILLISECOND;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_OPTIONAL;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_PATH;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_PREDICATE;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_RESULT;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_STRING;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_SUPPLIER;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_TIME;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_UNARY_OPERATOR;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_VERSION;
import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_VOID;

import java.util.function.Function;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.search.TemplateFunctionSymbolSearch;
import org.ek9lang.compiler.symbols.search.TemplateTypeSymbolSearch;
import org.ek9lang.compiler.symbols.search.TypeSymbolSearch;

/**
 * Just resolves a number of built-in types so that internal compiler code can be more explicit.
 */
public class BuiltInTypeCacheResolver implements Function<IScope, Ek9Types> {
  @Override
  public Ek9Types apply(final IScope scope) {
    if ("org.ek9.lang".equals(scope.getScopeName())) {
      return new Ek9Types(
          resolveType(scope, EK9_VOID),
          resolveType(scope, EK9_BOOLEAN),
          resolveType(scope, EK9_INTEGER),
          resolveType(scope, EK9_FLOAT),
          resolveType(scope, EK9_STRING),
          resolveType(scope, EK9_CHARACTER),
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
          resolveTemplateFunction(scope, EK9_UNARY_OPERATOR),
          resolveTemplateFunction(scope, EK9_PREDICATE),
          resolveTemplateFunction(scope, EK9_COMPARATOR));
    }
    return null;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private ISymbol resolveType(final IScope scope, final String type) {
    return scope.resolve(new TypeSymbolSearch(type)).get();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private ISymbol resolveTemplateType(final IScope scope, final String type) {
    return scope.resolve(new TemplateTypeSymbolSearch(type)).get();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private ISymbol resolveTemplateFunction(final IScope scope, final String type) {
    return scope.resolve(new TemplateFunctionSymbolSearch(type)).get();
  }
}
