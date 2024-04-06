package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_ITERATE_METHOD;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.CommonTypeDeterminationDetails;
import org.ek9lang.compiler.support.CommonTypeOrError;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * Deals with working out what the type being 'catted' is.
 * A bit more complex than you might think. This is because there can be multiple
 * expressions as part of the 'cat'. So it is essential to establish a common type.
 */
final class ProcessStreamCat extends TypedSymbolAccess implements Consumer<EK9Parser.StreamCatContext> {

  private final GetIteratorType getIteratorType;
  private final CommonTypeOrError commonTypeOrError;


  ProcessStreamCat(final SymbolAndScopeManagement symbolAndScopeManagement,
                   final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.getIteratorType = new GetIteratorType(symbolAndScopeManagement, errorListener);
    this.commonTypeOrError = new CommonTypeOrError(errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamCatContext ctx) {

    final var streamCat = (StreamCallSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (streamCat != null) {
      final var commonTypeToProduce = calculatedCommonTypeOrError(streamCat, ctx);
      commonTypeToProduce.ifPresent(type -> {
        streamCat.setProducesSymbolType(type);
        streamCat.setType(type);
      });
    }

  }

  private Optional<ISymbol> calculatedCommonTypeOrError(final StreamCallSymbol streamCat,
                                                        final EK9Parser.StreamCatContext ctx) {

    final var expressions = ctx.expression().stream()
        .map(super::getRecordedAndTypedSymbol)
        .filter(Objects::nonNull)
        .toList();

    final var expressionTypes = expressions.stream()
        .map(ISymbol::getType)
        .filter(Optional::isPresent)
        .flatMap(Optional::stream)
        .map(IAggregateSymbol.class::cast)
        .toList();

    final var listOfIterableTypes = getIterableTypesOrError(expressions, expressionTypes);

    if (!expressions.isEmpty() && expressions.size() == listOfIterableTypes.size()) {
      final var details =
          new CommonTypeDeterminationDetails(streamCat.getSourceToken(), expressions, listOfIterableTypes);
      return commonTypeOrError.apply(details);
    }

    return Optional.empty();
  }

  private List<ISymbol> getIterableTypesOrError(final List<ISymbol> expressions,
                                                final List<IAggregateSymbol> expressionTypes) {

    final List<ISymbol> rtn = new ArrayList<>();

    if (expressions.size() == expressionTypes.size()) {
      for (int i = 0; i < expressions.size(); i++) {
        final var possibleType = getIterableTypeOrError(expressions.get(i), expressionTypes.get(i));
        possibleType.ifPresent(rtn::add);
      }
    }

    return rtn;
  }

  private Optional<ISymbol> getIterableTypeOrError(final ISymbol expression, final IAggregateSymbol expressionType) {

    final var resolvedType = getIteratorType.apply(expressionType);
    if (resolvedType.isEmpty()) {
      errorListener.semanticError(expression.getSourceToken(),
          "iteration over '" + expressionType.getFriendlyName() + "' type is not possible:",
          MISSING_ITERATE_METHOD);
    }

    return resolvedType;
  }
}
