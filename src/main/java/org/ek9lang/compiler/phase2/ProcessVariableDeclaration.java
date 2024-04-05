package org.ek9lang.compiler.phase2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.CompilerException;

/**
 * Checks for inferred declarations of variables on 'properties' in aggregates.
 * If possible it will attempt to workout what the type is, if it cannot it will emit an error.
 * This is designed to aid in simple property declarations as much as possible, but property initialisation cannot
 * really trigger full expression processing - that is a potential long and circular trail - that must be done in phase
 * three. The main idea is to ensure that before phase three starts ALL properties on aggregates are 'typed'.
 * This aspect is essential for the phase three expression processing (as that requires types are known on interfaces
 * and aggregate properties to function correctly).
 * Note that it is possible to 'force' the EK9 developer to declare properties in the more long hand way as shown below.
 * <pre>
 *   shorthand <- 1
 *   longhand as Integer: 1
 * </pre>
 */
final class ProcessVariableDeclaration extends RuleSupport implements Consumer<EK9Parser.VariableDeclarationContext> {
  private final ParameterisedLocator parameterisedLocator;

  ProcessVariableDeclaration(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final SymbolFactory symbolFactory,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.parameterisedLocator = new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);
  }

  @Override
  public void accept(EK9Parser.VariableDeclarationContext ctx) {
    var variable = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (ctx.assignmentExpression().expression() != null) {
      checkIfInferredAggregateProperty(ctx, ctx.assignmentExpression().expression(), variable);
    }

  }

  private void checkIfInferredAggregateProperty(final EK9Parser.VariableDeclarationContext ctx,
                                                final EK9Parser.ExpressionContext exprCtx,
                                                final ISymbol property) {
    if (exprCtx.call() != null && exprCtx.call().identifierReference() != null) {
      processAsIdentifierReference(ctx, exprCtx.call().identifierReference(), property);
    } else if (exprCtx.list() != null) {
      processAsList(exprCtx.list(), property);
    } else if (exprCtx.dict() != null) {
      processAsDictionary(ctx, exprCtx.dict(), property);
    } else {
      emitMustBeSimpleError(ctx.start, "not expecting complex expression", property);
    }

  }

  private void processAsIdentifierReference(final EK9Parser.VariableDeclarationContext ctx,
                                            final EK9Parser.IdentifierReferenceContext identifierReferenceCtx,
                                            final ISymbol property) {

    var identifierReference = symbolAndScopeManagement.getRecordedSymbol(identifierReferenceCtx);

    if (identifierReference != null) {
      identifierReference.getType().ifPresent(type -> {
        property.setType(type);
        if (!type.isType()) {
          emitMustBeSimpleError(ctx.start, "expecting a valid type", property);
        }
      });
    } else {
      errorListener.semanticError(identifierReferenceCtx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    }
  }

  /**
   * Here we do a minimal cut down version of processing the List when declared on an aggregate property.
   * For full expressions there is a complete processing in phase 3. But when used on an aggregate as a property
   * they really have to be simple 'literals' and all same type, there is no trying to find a common super or anything
   * like implemented in phase 3. This is supposed to be simple and declarative on the aggregate.
   */
  private void processAsList(final EK9Parser.ListContext listCtx,
                             final ISymbol property) {
    if (allLiteralsInListOrError(listCtx, property)) {
      var typeOfList = getListType(listCtx.start, listCtx.expression(), property);
      if (typeOfList != null) {
        final var listType = symbolAndScopeManagement.getEk9Types().ek9List();
        final var typeData = new ParameterisedTypeData(new Ek9Token(listCtx.start), listType, List.of(typeOfList));
        final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);
        if (resolvedNewType.isEmpty()) {
          throw new CompilerException("Unable to create parameterised type");
        }
        property.setType(resolvedNewType);
      }
    }
  }

  private boolean allLiteralsInListOrError(final EK9Parser.ListContext list,
                                           final ISymbol property) {

    for (var exprCtx : list.expression()) {
      if (errorWhenExpressionIsNotLiteral(exprCtx, property)) {
        return false;
      }
    }
    return true;

  }

  private void processAsDictionary(final EK9Parser.VariableDeclarationContext ctx,
                                   final EK9Parser.DictContext dictCtx,
                                   final ISymbol property) {
    if (allLiteralsInDictOrError(dictCtx, property)) {
      var keyValueTypes = extractDictExpressionsAsLists(ctx.start, dictCtx, property);
      if (keyValueTypes.isEmpty()) {
        return;
      }
      final var dictType = symbolAndScopeManagement.getEk9Types().ek9Dict();
      final var typeData = new ParameterisedTypeData(new Ek9Token(dictCtx.start), dictType, keyValueTypes);
      final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);
      if (resolvedNewType.isEmpty()) {
        throw new CompilerException("Unable to create parameterised type");
      }
      property.setType(resolvedNewType);

    }
  }

  private List<ISymbol> extractDictExpressionsAsLists(final Token errorLocation,
                                                      final EK9Parser.DictContext dict,
                                                      final ISymbol property) {

    List<EK9Parser.ExpressionContext> keyExprList = new ArrayList<>();
    List<EK9Parser.ExpressionContext> valueExprList = new ArrayList<>();

    for (var valuePair : dict.initValuePair()) {
      keyExprList.add(valuePair.expression(0));
      valueExprList.add(valuePair.expression(1));
    }

    var keySymbolType = getListType(errorLocation, keyExprList, property);
    var valueSymbolType = getListType(errorLocation, valueExprList, property);

    if (keySymbolType == null || valueSymbolType == null) {
      return List.of();
    }

    return List.of(keySymbolType, valueSymbolType);
  }

  private ISymbol getListType(final Token errorLocation,
                              final List<EK9Parser.ExpressionContext> expressions,
                              final ISymbol property) {
    var distinctTypes = expressions
        .stream()
        .map(expr -> expr.primary().literal())
        .map(symbolAndScopeManagement::getRecordedSymbol)
        .map(ISymbol::getType)
        .flatMap(Optional::stream)
        .distinct()
        .toList();

    if (distinctTypes.size() != 1) {
      emitMustBeSimpleError(errorLocation, "not all types are the same", property);
      return null;
    }

    return distinctTypes.get(0);

  }

  private boolean allLiteralsInDictOrError(final EK9Parser.DictContext dict,
                                           final ISymbol property) {

    for (var valuePair : dict.initValuePair()) {
      if (errorWhenExpressionIsNotLiteral(valuePair.expression(0), property)
          || errorWhenExpressionIsNotLiteral(valuePair.expression(1), property)) {
        return false;
      }
    }

    return true;
  }

  private boolean errorWhenExpressionIsNotLiteral(final EK9Parser.ExpressionContext exprCtx,
                                                  final ISymbol property) {
    if (exprCtx.primary() == null || exprCtx.primary().literal() == null) {
      emitMustBeSimpleError(exprCtx.start, "expecting literals", property);
      return true;
    }
    return false;
  }

  private void emitMustBeSimpleError(final Token errorLocation,
                                     final String additionalErrorInformation,
                                     final ISymbol property) {
    var msg = "wrt '" + property.getName() + "' "
        + additionalErrorInformation + ":";

    errorListener.semanticError(errorLocation, msg,
        ErrorListener.SemanticClassification.TYPE_MUST_BE_SIMPLE);
  }
}