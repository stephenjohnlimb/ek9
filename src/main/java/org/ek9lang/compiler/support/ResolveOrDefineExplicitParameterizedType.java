package org.ek9lang.compiler.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * The bulk of the processing is in the abstract base.
 * This just defines the entry point for typedef context.
 */
public class ResolveOrDefineExplicitParameterizedType extends ResolveOrDefineTypes
    implements Function<EK9Parser.ParameterisedTypeContext, Optional<ISymbol>> {

  /**
   * A bit of a complex function constructor - for a function.
   */
  public ResolveOrDefineExplicitParameterizedType(final SymbolAndScopeManagement symbolAndScopeManagement,
                                                  final SymbolFactory symbolFactory,
                                                  final ErrorListener errorListener,
                                                  final boolean errorIfNotDefinedOrResolved) {

    super(symbolAndScopeManagement, symbolFactory, errorListener, errorIfNotDefinedOrResolved);

  }

  @Override
  public Optional<ISymbol> apply(final EK9Parser.ParameterisedTypeContext ctx) {

    if (ctx == null) {
      return Optional.empty();
    }

    return resolveTypeByParameterizedType(new Ek9Token(ctx.start), ctx);
  }
}
