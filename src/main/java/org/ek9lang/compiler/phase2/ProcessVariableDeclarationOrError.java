package org.ek9lang.compiler.phase2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.CompilerException;

/**
 * Checks for inferred declarations of variables in various contexts.
 * <p>
 * If possible it will attempt to work out what the type is, if it cannot, it will emit an error.
 * This is designed to aid in simple property declarations as much as possible, but property initialisation cannot
 * really trigger full expression processing - that is a potential long and circular trail - that must be done in phase
 * three.
 * </p>
 * <p>
 * The main idea is to ensure that before phase three starts ALL properties on aggregates are 'typed'.
 * This aspect is essential for the phase three expression processing (as that requires types are known on interfaces
 * and aggregate properties to function correctly).
 * Note that it is possible to 'force' the EK9 developer to declare properties in the more long hand way as shown below.
 * </p>
 * <pre>
 *   shorthand &lt;- 1
 *   longhand as Integer: 1
 * </pre>
 */
final class ProcessVariableDeclarationOrError extends RuleSupport
    implements Consumer<EK9Parser.VariableDeclarationContext> {
  private final ParameterisedLocator parameterisedLocator;

  ProcessVariableDeclarationOrError(final SymbolsAndScopes symbolsAndScopes,
                                    final SymbolFactory symbolFactory,
                                    final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.parameterisedLocator = new ParameterisedLocator(symbolsAndScopes, symbolFactory, errorListener, true);

  }

  @Override
  public void accept(final EK9Parser.VariableDeclarationContext ctx) {

    if (ctx.assignmentExpression().expression() != null) {
      final var variable = symbolsAndScopes.getRecordedSymbol(ctx);
      processInferredTypeOrError(ctx, ctx.assignmentExpression().expression(), variable);
    }

  }

  private void processInferredTypeOrError(final EK9Parser.VariableDeclarationContext ctx,
                                          final EK9Parser.ExpressionContext exprCtx,
                                          final ISymbol variable) {

    if (exprCtx.call() != null && exprCtx.call().identifierReference() != null) {
      processAsIdentifierReferenceOrError(ctx, exprCtx, variable);
    } else if (exprCtx.call() != null && exprCtx.call().parameterisedType() != null) {
      processAsParameterisedTypeOrError(ctx, exprCtx, variable);
    } else if (exprCtx.list() != null) {
      processAsList(exprCtx.list(), variable);
    } else if (exprCtx.dict() != null) {
      processAsDictionaryOrError(ctx, exprCtx.dict(), variable);
    } else {
      emitMustBeSimpleError(ctx.start, "not expecting complex expression", variable);
    }

  }

  private void processAsIdentifierReferenceOrError(final EK9Parser.VariableDeclarationContext ctx,
                                                   final EK9Parser.ExpressionContext exprCtx,
                                                   final ISymbol variable) {

    final var identifierReferenceCtx = exprCtx.call().identifierReference();
    final var identifierReference = symbolsAndScopes.getRecordedSymbol(identifierReferenceCtx);
    //Here this could be a function call, type construction or an inferred Generic Construction
    if (identifierReference != null
        && (identifierReference.isTemplateFunction() || identifierReference.isTemplateType())) {

      //At this point in the phases we have not started to try and resolve expressions, that's the next phase.
      //Consider a shortcut of this to attempt to resolve a inferred parameterised type early.
      emitMustBeSimpleError(ctx.start, "not expecting type inferred Generic", variable);
      return;
    }

    resolveTypeOrError(identifierReferenceCtx.start, ctx, identifierReferenceCtx, variable);

  }

  private void processAsParameterisedTypeOrError(final EK9Parser.VariableDeclarationContext ctx,
                                                 final EK9Parser.ExpressionContext exprCtx,
                                                 final ISymbol variable) {

    final var parameterisedTypeCtx = exprCtx.call().parameterisedType();
    resolveTypeOrError(parameterisedTypeCtx.start, ctx, parameterisedTypeCtx, variable);

  }

  private void resolveTypeOrError(final Token errorLocation,
                                  final EK9Parser.VariableDeclarationContext ctx,
                                  final ParseTree node,
                                  final ISymbol variable) {

    final var ref = symbolsAndScopes.getRecordedSymbol(node);
    if (ref != null) {
      ref.getType().ifPresent(type -> {
        variable.setType(type);
        if (!type.isType()) {
          emitMustBeSimpleError(ctx.start, "expecting a valid type", variable);
        }
      });
    } else {
      errorListener.semanticError(errorLocation, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    }

  }

  /**
   * Here we do a minimal cut down version of processing the List when declared on an aggregate variable.
   * For full expressions there is a complete processing in phase 3. But when used on an aggregate as a variable
   * they really have to be simple 'literals' and all same type, there is no trying to find a common super or anything
   * like implemented in phase 3. This is supposed to be simple and declarative on the aggregate.
   */
  private void processAsList(final EK9Parser.ListContext listCtx,
                             final ISymbol variable) {

    if (allLiteralsInListOrError(listCtx, variable)) {
      final var typeOfList = getListType(listCtx.start, listCtx.expression(), variable);
      if (typeOfList != null) {
        final var listType = symbolsAndScopes.getEk9Types().ek9List();
        final var typeData = new ParameterisedTypeData(new Ek9Token(listCtx.start), listType, List.of(typeOfList));
        final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);
        if (resolvedNewType.isEmpty()) {
          throw new CompilerException("Unable to create parameterised type");
        }
        variable.setType(resolvedNewType);
      }
    }

  }

  private boolean allLiteralsInListOrError(final EK9Parser.ListContext list,
                                           final ISymbol property) {

    for (var exprCtx : list.expression()) {
      if (emitErrorWhenExpressionIsNotLiteral(exprCtx, property)) {
        return false;
      }
    }

    return true;
  }

  private void processAsDictionaryOrError(final EK9Parser.VariableDeclarationContext ctx,
                                          final EK9Parser.DictContext dictCtx,
                                          final ISymbol property) {

    if (allLiteralsInDictOrError(dictCtx, property)) {
      final var keyValueTypes = extractDictExpressionsAsLists(ctx.start, dictCtx, property);
      if (keyValueTypes.isEmpty()) {
        return;
      }
      final var dictType = symbolsAndScopes.getEk9Types().ek9Dictionary();
      final var typeData = new ParameterisedTypeData(new Ek9Token(dictCtx.start), dictType, keyValueTypes);
      final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);
      if (resolvedNewType.isEmpty()) {
        //Something seriously wrong here.
        throw new CompilerException("Unable to create parameterised type");
      }
      property.setType(resolvedNewType);
    }

  }

  private List<ISymbol> extractDictExpressionsAsLists(final Token errorLocation,
                                                      final EK9Parser.DictContext dict,
                                                      final ISymbol property) {

    final List<EK9Parser.ExpressionContext> keyExprList = new ArrayList<>();
    final List<EK9Parser.ExpressionContext> valueExprList = new ArrayList<>();

    for (var valuePair : dict.initValuePair()) {
      keyExprList.add(valuePair.expression(0));
      valueExprList.add(valuePair.expression(1));
    }

    final var keySymbolType = getListType(errorLocation, keyExprList, property);
    final var valueSymbolType = getListType(errorLocation, valueExprList, property);

    if (keySymbolType == null || valueSymbolType == null) {
      return List.of();
    }

    return List.of(keySymbolType, valueSymbolType);
  }

  private ISymbol getListType(final Token errorLocation,
                              final List<EK9Parser.ExpressionContext> expressions,
                              final ISymbol property) {
    final var distinctTypes = expressions
        .stream()
        .map(expr -> expr.primary().literal())
        .map(symbolsAndScopes::getRecordedSymbol)
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
      if (emitErrorWhenExpressionIsNotLiteral(valuePair.expression(0), property)
          || emitErrorWhenExpressionIsNotLiteral(valuePair.expression(1), property)) {
        return false;
      }
    }

    return true;
  }

  private boolean emitErrorWhenExpressionIsNotLiteral(final EK9Parser.ExpressionContext exprCtx,
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

    final var msg = "wrt '" + property.getName() + "' "
        + additionalErrorInformation + ":";

    errorListener.semanticError(errorLocation, msg,
        ErrorListener.SemanticClassification.TYPE_MUST_BE_SIMPLE);

  }
}