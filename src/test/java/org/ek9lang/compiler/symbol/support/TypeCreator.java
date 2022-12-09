package org.ek9lang.compiler.symbol.support;

import java.util.Optional;
import java.util.function.BiFunction;
import org.ek9lang.compiler.internals.Module;
import org.ek9lang.compiler.internals.Source;
import org.ek9lang.compiler.symbol.AggregateSymbol;

/**
 * Just used in testing to add new named types to a symbol table.
 */
public class TypeCreator implements BiFunction<String, SymbolTable, AggregateSymbol> {
  @Override
  public AggregateSymbol apply(String typeName, SymbolTable inSymbolTable) {
    var newType = new AggregateSymbol(typeName, inSymbolTable);
    newType.setParsedModule(Optional.of(new Module() {
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
        return inSymbolTable.getScopeName();
      }
    }));
    inSymbolTable.define(newType);
    return newType;
  }
}
