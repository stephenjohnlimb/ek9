package org.ek9lang.compiler.symbols.support;

import java.util.Optional;
import java.util.function.BiFunction;
import org.ek9lang.compiler.Module;
import org.ek9lang.compiler.Source;
import org.ek9lang.compiler.symbols.ISymbol;
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
