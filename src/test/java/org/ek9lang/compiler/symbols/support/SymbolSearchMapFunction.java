package org.ek9lang.compiler.symbols.support;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.main.resolvedefine.SymbolSearchConfiguration;

public class SymbolSearchMapFunction implements Function<List<String>, List<SymbolSearchConfiguration>> {
  @Override
  public List<SymbolSearchConfiguration> apply(List<String> symbolNames) {
    return symbolNames.stream().map(SymbolSearchConfiguration::new).toList();
  }
}
