package org.ek9lang.compiler.symbol.support;

import java.util.Optional;
import java.util.function.BiFunction;
import org.ek9lang.compiler.internals.Module;
import org.ek9lang.compiler.internals.Source;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.tokenizer.SyntheticToken;

public class SyntheticSymbolCompletion implements BiFunction<ISymbol, String, ISymbol> {
  @Override
  public ISymbol apply(final ISymbol iSymbol, final String scopeName) {
    iSymbol.setSourceToken(new SyntheticToken());
    iSymbol.setParsedModule(Optional.of(new Module() {
      @Override
      public Source getSource() {
        return new Source() {

          @Override
          public String getFileName() {
            return "syntheticSource.ek9";
          }

          @Override
          public boolean isDev() {
            return false;
          }

          @Override
          public boolean isLib() {
            return false;
          }
        };
      }

      @Override
      public String getScopeName() {
        return scopeName;
      }
    }));
    return iSymbol;
  }
}
