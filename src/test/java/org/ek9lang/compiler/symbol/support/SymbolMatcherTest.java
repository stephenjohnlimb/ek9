package org.ek9lang.compiler.symbol.support;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.CompilableProgramSuitable;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SymbolMatcherTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();
  private final SymbolMatcher underTest = new SymbolMatcher();
  private Optional<ISymbol> integerType;
  private Optional<ISymbol> floatType;

  private Optional<ISymbol> stringType;

  @BeforeAll
  void setupCompilerAndGetTypes() {
    //So crank up the compiler, parse all the built-in source and get real ek9 types.
    //then we can check coercion of what we will actually be using.
    Consumer<CompilableProgram> compilerAccess = compiler -> {
      integerType = compiler.resolveByFullyQualifiedSearch(new TypeSymbolSearch("org.ek9.lang::Integer"));
      floatType = compiler.resolveByFullyQualifiedSearch(new TypeSymbolSearch("org.ek9.lang::Float"));
      stringType = compiler.resolveByFullyQualifiedSearch(new TypeSymbolSearch("org.ek9.lang::String"));
    };

    sharedContext.get().accept(compilerAccess);
  }

  @Test
  void testEmptyParameters() {
    var matchWeight = underTest.getWeightOfParameterMatch(List.of(), List.of());
    Assertions.assertFalse(matchWeight < 0.0 || matchWeight > 0.0);
  }

  @Test
  void testPartialEmptyParameters1() {
    var from = getTypeSymbols(List.of(integerType));
    var to = getTypeSymbols(List.of());
    var matchWeight = underTest.getWeightOfParameterMatch(from, to);
    Assertions.assertTrue(matchWeight < 0.0);
  }

  @Test
  void testPartialEmptyParameters2() {
    var from = getTypeSymbols(List.of());
    var to = getTypeSymbols(List.of(integerType));
    var matchWeight = underTest.getWeightOfParameterMatch(from, to);
    Assertions.assertTrue(matchWeight < 0.0);
  }

  @Test
  void testBasicCoercion() {
    var integerToFloatCoercionWeight = underTest.getWeightOfMatch(integerType, floatType);
    Assertions.assertTrue(integerToFloatCoercionWeight > 0.0);
    var floatToIntegerCoercionWeight = underTest.getWeightOfMatch(floatType, integerType);
    Assertions.assertTrue(floatToIntegerCoercionWeight < 0.0);
  }

  @Test
  void testValidParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType));
    var to = getTypeSymbols(List.of(integerType));
    var matchWeight = underTest.getWeightOfParameterMatch(from, to);
    Assertions.assertFalse(matchWeight < 0.0 || matchWeight > 0.0);
  }

  @Test
  void testMultipleValidParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType, floatType, stringType));
    var to = getTypeSymbols(List.of(integerType, floatType, stringType));
    var matchWeight = underTest.getWeightOfParameterMatch(from, to);
    Assertions.assertFalse(matchWeight < 0.0 || matchWeight > 0.0);
  }

  @Test
  void testMultipleInValidParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType, floatType, stringType));
    var to = getTypeSymbols(List.of(stringType, floatType, stringType));
    var matchWeight = underTest.getWeightOfParameterMatch(from, to);
    Assertions.assertTrue(matchWeight < 0.0);
  }

  @Test
  void testValidCoercibleParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType));
    var to = getTypeSymbols(List.of(floatType));
    var matchWeight = underTest.getWeightOfParameterMatch(from, to);
    Assertions.assertTrue(matchWeight > 0.0);
  }

  @Test
  void testInvalidCoercibleParameterTypeMatches() {
    var from = getTypeSymbols(List.of(floatType));
    var to = getTypeSymbols(List.of(integerType));
    var matchWeight = underTest.getWeightOfParameterMatch(from, to);
    Assertions.assertTrue(matchWeight < 0.0);
  }

  @Test
  void testMultipleValidCoercibleParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType, floatType, stringType, integerType));
    var mostSpecificTo = getTypeSymbols(List.of(integerType, floatType, stringType, integerType));
    var nextSpecificTo1 = getTypeSymbols(List.of(floatType, floatType, stringType, integerType));
    var nextSpecificTo2 = getTypeSymbols(List.of(integerType, floatType, stringType, floatType));
    var leastSpecificTo = getTypeSymbols(List.of(floatType, floatType, stringType, floatType));

    var mostSpecificMatchWeight = underTest.getWeightOfParameterMatch(from, mostSpecificTo);
    Assertions.assertFalse(mostSpecificMatchWeight < 0.0 || mostSpecificMatchWeight > 0.0);

    var nextMatchWeight1 = underTest.getWeightOfParameterMatch(from, nextSpecificTo1);
    Assertions.assertTrue(nextMatchWeight1 >= 0.5 && nextMatchWeight1 < 0.6);

    var nextMatchWeight2 = underTest.getWeightOfParameterMatch(from, nextSpecificTo2);
    Assertions.assertTrue(nextMatchWeight2 >= 0.5 && nextMatchWeight2 < 0.6);

    var leastSpecificMatchWeight = underTest.getWeightOfParameterMatch(from, leastSpecificTo);
    Assertions.assertTrue(leastSpecificMatchWeight >= 1.0 && leastSpecificMatchWeight < 1.1);

  }

  private List<ISymbol> getTypeSymbols(List<Optional<ISymbol>> types) {
    return types.stream()
        .filter(Optional::isPresent)
        .flatMap(Optional::stream)
        .toList();
  }
}
