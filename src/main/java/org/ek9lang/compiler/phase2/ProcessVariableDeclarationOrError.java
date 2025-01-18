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
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks for inferred declarations of variables in various contexts. Even though this is phase2 (explicit phase).
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
                                          final ISymbol returnVariable) {

    if (exprCtx.call() != null && exprCtx.call().identifierReference() != null) {
      final var resolvedType = processAsIdentifierReferenceOrError(ctx.start, exprCtx, returnVariable);
      resolvedType.ifPresent(returnVariable::setType);
    } else if (exprCtx.call() != null && exprCtx.call().parameterisedType() != null) {
      final var resolvedType = processAsParameterisedTypeOrError(exprCtx);
      resolvedType.ifPresent(returnVariable::setType);
    } else if (exprCtx.list() != null) {
      final var resolvedType = processAsList(exprCtx.list(), returnVariable);
      resolvedType.ifPresent(returnVariable::setType);
    } else if (exprCtx.dict() != null) {
      final var resolvedType = processAsDictionaryOrError(ctx, exprCtx.dict(), returnVariable);
      resolvedType.ifPresent(returnVariable::setType);
    } else {
      emitMustBeSimpleError(ctx.start, "not expecting complex expression", returnVariable);
    }

  }

  private Optional<ISymbol> processAsIdentifierReferenceOrError(final Token errorLocation,
                                                                final EK9Parser.ExpressionContext exprCtx,
                                                                final ISymbol returnVariable) {

    final var identifierReferenceCtx = exprCtx.call().identifierReference();
    final var identifierReference = symbolsAndScopes.getRecordedSymbol(identifierReferenceCtx);
    //Here this could be a function call, type construction or an inferred Generic Construction
    if (identifierReference != null
        && (identifierReference.isTemplateFunction() || identifierReference.isTemplateType())) {

      return attemptToEvaluateType(errorLocation, identifierReference, exprCtx, returnVariable);
    }

    return resolveTypeOrError(identifierReferenceCtx.start, identifierReferenceCtx, returnVariable);
  }

  /**
   * The resolution of expression types is fully done in later stages.
   * But here it is really nice to be able to look ahead to process types that are used in
   * properties and return variable (if possible).
   */
  private Optional<ISymbol> attemptToEvaluateType(final Token errorLocation,
                                                  final ISymbol identifierReference,
                                                  final EK9Parser.ExpressionContext ctx,
                                                  final ISymbol returnVariable) {

    if (identifierReference.isTemplateType()
        && ctx.call().paramExpression() != null
        && ctx.call().paramExpression().expressionParam() != null
        && !ctx.call().paramExpression().expressionParam().isEmpty()) {

      //Then we know it will need some arguments to parameterize it.
      //We must have/resolve those so that we can ensure we parameterize the genetic type to
      //get its actual type. i.e. List of type T -> List of T or String.
      //Now this could be in the form of literals or Type constructors, i.e. "" or String().
      final var parameterizingTypes = attemptToGetTypes(errorLocation,
          ctx.call().paramExpression().expressionParam(), returnVariable);

      if (!parameterizingTypes.isEmpty()) {
        final var typeData =
            new ParameterisedTypeData(new Ek9Token(errorLocation), identifierReference, parameterizingTypes);
        return parameterisedLocator.resolveOrDefine(typeData);
      }
    }
    emitMustBeSimpleError(errorLocation, "not expecting type inferred Generic", returnVariable);
    return Optional.empty();
  }

  private List<ISymbol> attemptToGetTypes(final Token errorLocation,
                                          final List<EK9Parser.ExpressionParamContext> ctxList,
                                          final ISymbol returnVariable) {

    final var returnList = new ArrayList<ISymbol>();
    for (var ctx : ctxList) {
      //Now try and get the symbol (if present in this phase)
      if (ctx.expression().primary() != null && ctx.expression().primary().literal() != null) {
        //Just a literal so can guarantee locating that and also know its type
        final var literal = symbolsAndScopes.getRecordedSymbol(ctx.expression().primary().literal());
        literal.getType().ifPresent(returnList::add);
      } else if (ctx.expression().primary() != null && ctx.expression().primary().identifierReference() != null) {
        //A bit trickier as we cannot be sure the identifier can be located or has a type at this stage.
        final var identifierReferenceCtx = ctx.expression().primary().identifierReference();
        final var resolved = symbolsAndScopes.getTopScope().resolve(new SymbolSearch(identifierReferenceCtx.getText()));
        if (resolved.isEmpty()) {
          errorListener.semanticError(identifierReferenceCtx.start, "",
              ErrorListener.SemanticClassification.NOT_RESOLVED);
        }
        resolved.flatMap(ISymbol::getType).ifPresent(returnList::add);
      } else if (ctx.expression().call() != null
          && ctx.expression().call().identifierReference() != null
          && ctx.expression().call().paramExpression() != null) {
        final var resolved = processAsIdentifierReferenceOrError(errorLocation, ctx.expression(), returnVariable);
        //Unlike the above this method will return the actual type
        resolved.ifPresent(returnList::add);
      }
    }
    //Only if we could actually locate all the types required do we return the list
    //If the list is empty on return then errors will be emitted.
    if (returnList.size() == ctxList.size()) {
      return returnList;
    }
    return List.of();

  }

  /**
   * <pre>
   *   typeDef
   *     : identifierReference
   *     | parameterisedType
   *     ;
   *
   * //Added optional paramExpression here, to simplify grammar and processing
   * //But now needs coding check to ensure only used in the correct context.
   * parameterisedType
   *     : identifierReference paramExpression? OF LPAREN parameterisedArgs RPAREN
   *     | identifierReference paramExpression? OF typeDef
   *     ;
   * parameterisedArgs
   *     : typeDef (COMMA typeDef)*
   *     ;
   * </pre>
   */
  private Optional<ISymbol> processAsParameterisedTypeOrError(final EK9Parser.ExpressionContext exprCtx) {

    final var parameterisedTypeCtx = exprCtx.call().parameterisedType();
    return Optional.ofNullable(symbolsAndScopes.getRecordedSymbol(parameterisedTypeCtx));
  }

  private Optional<ISymbol> resolveTypeOrError(final Token errorLocation,
                                               final ParseTree node,
                                               final ISymbol returnVariable) {

    final var ref = symbolsAndScopes.getRecordedSymbol(node);
    if (ref != null && ref.getType().isPresent()) {
      final var type = ref.getType().get();
      if (!type.isType()) {
        emitMustBeSimpleError(errorLocation, "expecting a valid type", returnVariable);
      }
      return ref.getType();
    } else {
      errorListener.semanticError(errorLocation, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    }
    return Optional.empty();
  }

  /**
   * Here we do a minimal cut down version of processing the List when declared on an aggregate variable.
   * For full expressions there is a complete processing in phase 3. But when used on an aggregate as a variable
   * they really have to be simple 'literals' and all same type, there is no trying to find a common super or anything
   * like implemented in phase 3. This is supposed to be simple and declarative on the aggregate.
   */
  private Optional<ISymbol> processAsList(final EK9Parser.ListContext listCtx,
                                          final ISymbol returnVariable) {

    if (allLiteralsInListOrError(listCtx, returnVariable)) {
      final var typeOfList = getListType(listCtx.start, listCtx.expression(), returnVariable);
      if (typeOfList != null) {
        final var listType = symbolsAndScopes.getEk9Types().ek9List();
        final var typeData = new ParameterisedTypeData(new Ek9Token(listCtx.start), listType, List.of(typeOfList));
        return parameterisedLocator.resolveOrDefine(typeData);
      }
    }
    return Optional.empty();
  }

  private boolean allLiteralsInListOrError(final EK9Parser.ListContext list,
                                           final ISymbol returnVariable) {

    for (var exprCtx : list.expression()) {
      if (emitErrorWhenExpressionIsNotLiteral(exprCtx, returnVariable)) {
        return false;
      }
    }

    return true;
  }

  private Optional<ISymbol> processAsDictionaryOrError(final EK9Parser.VariableDeclarationContext ctx,
                                                       final EK9Parser.DictContext dictCtx,
                                                       final ISymbol returnVariable) {

    if (allLiteralsInDictOrError(dictCtx, returnVariable)) {
      final var keyValueTypes = extractDictExpressionsAsLists(ctx.start, dictCtx, returnVariable);
      if (keyValueTypes.isEmpty()) {
        return Optional.empty();
      }
      final var dictType = symbolsAndScopes.getEk9Types().ek9Dictionary();
      final var typeData = new ParameterisedTypeData(new Ek9Token(dictCtx.start), dictType, keyValueTypes);
      return parameterisedLocator.resolveOrDefine(typeData);
    }
    return Optional.empty();
  }

  private List<ISymbol> extractDictExpressionsAsLists(final Token errorLocation,
                                                      final EK9Parser.DictContext dict,
                                                      final ISymbol returnVariable) {

    final List<EK9Parser.ExpressionContext> keyExprList = new ArrayList<>();
    final List<EK9Parser.ExpressionContext> valueExprList = new ArrayList<>();

    for (var valuePair : dict.initValuePair()) {
      keyExprList.add(valuePair.expression(0));
      valueExprList.add(valuePair.expression(1));
    }

    final var keySymbolType = getListType(errorLocation, keyExprList, returnVariable);
    final var valueSymbolType = getListType(errorLocation, valueExprList, returnVariable);

    if (keySymbolType == null || valueSymbolType == null) {
      return List.of();
    }

    return List.of(keySymbolType, valueSymbolType);
  }

  private ISymbol getListType(final Token errorLocation,
                              final List<EK9Parser.ExpressionContext> expressions,
                              final ISymbol returnVariable) {

    final var distinctTypes = expressions
        .stream()
        .map(expr -> expr.primary().literal())
        .map(symbolsAndScopes::getRecordedSymbol)
        .map(ISymbol::getType)
        .flatMap(Optional::stream)
        .distinct()
        .toList();

    if (distinctTypes.size() != 1) {
      emitMustBeSimpleError(errorLocation, "not all types are the same", returnVariable);
      return null;
    }

    return distinctTypes.get(0);

  }

  private boolean allLiteralsInDictOrError(final EK9Parser.DictContext dict,
                                           final ISymbol returnVariable) {

    for (var valuePair : dict.initValuePair()) {
      if (emitErrorWhenExpressionIsNotLiteral(valuePair.expression(0), returnVariable)
          || emitErrorWhenExpressionIsNotLiteral(valuePair.expression(1), returnVariable)) {
        return false;
      }
    }

    return true;
  }

  private boolean emitErrorWhenExpressionIsNotLiteral(final EK9Parser.ExpressionContext exprCtx,
                                                      final ISymbol returnVariable) {
    if (exprCtx.primary() == null || exprCtx.primary().literal() == null) {
      emitMustBeSimpleError(exprCtx.start, "expecting literals", returnVariable);
      return true;
    }

    return false;
  }

  private void emitMustBeSimpleError(final Token errorLocation,
                                     final String additionalErrorInformation,
                                     final ISymbol returnVariable) {

    final var msg = "wrt '" + returnVariable.getName() + "' "
        + additionalErrorInformation + ":";

    errorListener.semanticError(errorLocation, msg,
        ErrorListener.SemanticClassification.TYPE_MUST_BE_SIMPLE);

  }
}