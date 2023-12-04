package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.support.SymbolFactory.UNINITIALISED_AT_DECLARATION;

import org.ek9lang.compiler.symbols.VariableSymbol;

final class UnSafePropertyAccessAnalyzer extends CodeFlowMap {
  private static final String SAFE_ACCESS = "SAFE_ACCESS";

  UnSafePropertyAccessAnalyzer() {
    super(symbol -> ("TRUE".equals(symbol.getSquirrelledData(UNINITIALISED_AT_DECLARATION))
            && symbol instanceof VariableSymbol && symbol.isPropertyField()),
        access -> access.metaData().contains(SAFE_ACCESS),
        access -> access.metaData().add(SAFE_ACCESS));
  }
}
