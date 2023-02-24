package org.ek9lang.compiler.symbol.support;

import java.util.List;
import java.util.function.Function;

public class SymbolSearchMapFunction implements Function<List<String>, List<SymbolSearchForTest>> {
  @Override
  public List<SymbolSearchForTest> apply(List<String> symbolNames) {
    return symbolNames.stream().map(SymbolSearchForTest::new).toList();
  }
}
