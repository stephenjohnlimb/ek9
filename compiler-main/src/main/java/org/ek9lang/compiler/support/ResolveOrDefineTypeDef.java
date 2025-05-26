package org.ek9lang.compiler.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * The bulk of the processing is in the abstract base.
 * This just defines the entry point for typedef context.
 * So this is the main 'way in' from a bit of EK9 code that triggered a typeDef resolution.
 * This seems obvious. But the nature of a typeDef in terms of generics is recursive.
 * So while this is the main way in, it can and does trigger multiple recursive calls to resolveTypeByTypeDef.
 */
public class ResolveOrDefineTypeDef extends ResolveOrDefineTypes
    implements Function<EK9Parser.TypeDefContext, Optional<ISymbol>> {

  /**
   * A bit of a complex constructor - for a function.
   */
  public ResolveOrDefineTypeDef(final SymbolsAndScopes symbolsAndScopes,
                                final SymbolFactory symbolFactory,
                                final ErrorListener errorListener,
                                final boolean errorIfNotDefinedOrResolved) {

    super(symbolsAndScopes, symbolFactory, errorListener, errorIfNotDefinedOrResolved);

  }

  @Override
  public Optional<ISymbol> apply(EK9Parser.TypeDefContext ctx) {

    if (ctx == null) {
      return Optional.empty();
    }

    return resolveTypeByTypeDef(new Ek9Token(ctx.start), ctx);
  }
}
