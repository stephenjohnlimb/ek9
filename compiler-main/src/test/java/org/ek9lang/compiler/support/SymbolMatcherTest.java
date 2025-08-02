package org.ek9lang.compiler.support;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.common.CompilableProgramSupplier;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SymbolMatcherTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSupplier();
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
    var matchCost = underTest.getCostOfParameterMatch(List.of(), List.of());
    Assertions.assertFalse(matchCost < 0.0 || matchCost > 0.0);
  }

  @Test
  void testPartialEmptyParameters1() {
    var from = getTypeSymbols(List.of(integerType));
    var to = getTypeSymbols(List.of());
    var matchCost = underTest.getCostOfParameterMatch(from, to);
    Assertions.assertTrue(matchCost < 0.0);
  }

  @Test
  void testPartialEmptyParameters2() {
    var from = getTypeSymbols(List.of());
    var to = getTypeSymbols(List.of(integerType));
    var matchCost = underTest.getCostOfParameterMatch(from, to);
    Assertions.assertTrue(matchCost < 0.0);
  }

  @Test
  void testBasicCoercion() {
    var integerToFloatCoercionCost = underTest.getCostOfMatch(integerType, floatType);
    Assertions.assertTrue(integerToFloatCoercionCost > 0.0);
    var floatToIntegerCoercionCost = underTest.getCostOfMatch(floatType, integerType);
    Assertions.assertTrue(floatToIntegerCoercionCost < 0.0);
  }

  @Test
  void testValidParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType));
    var to = getTypeSymbols(List.of(integerType));
    var matchCost = underTest.getCostOfParameterMatch(from, to);
    Assertions.assertFalse(matchCost < 0.0 || matchCost > 0.0);
  }

  @Test
  void testMultipleValidParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType, floatType, stringType));
    var to = getTypeSymbols(List.of(integerType, floatType, stringType));
    var matchCost = underTest.getCostOfParameterMatch(from, to);
    Assertions.assertFalse(matchCost < 0.0 || matchCost > 0.0);
  }

  @Test
  void testMultipleInValidParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType, floatType, stringType));
    var to = getTypeSymbols(List.of(stringType, floatType, stringType));
    var matchCost = underTest.getCostOfParameterMatch(from, to);
    Assertions.assertTrue(matchCost < 0.0);
  }

  @Test
  void testValidCoercibleParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType));
    var to = getTypeSymbols(List.of(floatType));
    var matchCost = underTest.getCostOfParameterMatch(from, to);
    Assertions.assertTrue(matchCost > 0.0);
  }

  @Test
  void testInvalidCoercibleParameterTypeMatches() {
    var from = getTypeSymbols(List.of(floatType));
    var to = getTypeSymbols(List.of(integerType));
    var matchCost = underTest.getCostOfParameterMatch(from, to);
    Assertions.assertTrue(matchCost < 0.0);
  }

  @Test
  void testMultipleValidCoercibleParameterTypeMatches() {
    var from = getTypeSymbols(List.of(integerType, floatType, stringType, integerType));
    var mostSpecificTo = getTypeSymbols(List.of(integerType, floatType, stringType, integerType));
    var nextSpecificTo1 = getTypeSymbols(List.of(floatType, floatType, stringType, integerType));
    var nextSpecificTo2 = getTypeSymbols(List.of(integerType, floatType, stringType, floatType));
    var leastSpecificTo = getTypeSymbols(List.of(floatType, floatType, stringType, floatType));

    var mostSpecificmatchCost = underTest.getCostOfParameterMatch(from, mostSpecificTo);
    Assertions.assertFalse(mostSpecificmatchCost < 0.0 || mostSpecificmatchCost > 0.0);

    var nextmatchCost1 = underTest.getCostOfParameterMatch(from, nextSpecificTo1);
    Assertions.assertTrue(nextmatchCost1 >= 0.5 && nextmatchCost1 < 0.6);

    var nextmatchCost2 = underTest.getCostOfParameterMatch(from, nextSpecificTo2);
    Assertions.assertTrue(nextmatchCost2 >= 0.5 && nextmatchCost2 < 0.6);

    var leastSpecificmatchCost = underTest.getCostOfParameterMatch(from, leastSpecificTo);
    Assertions.assertTrue(leastSpecificmatchCost >= 1.0 && leastSpecificmatchCost < 1.1);

  }

  private List<ISymbol> getTypeSymbols(List<Optional<ISymbol>> types) {
    return types.stream()
        .filter(Optional::isPresent)
        .flatMap(Optional::stream)
        .toList();
  }
}
