package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.compiler.support.SimpleResolverForTesting;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad inheritance usage.
 */
class BadInheritanceTest extends PhasesTest {

  public BadInheritanceTest() {
    super("/examples/parseButFailCompile/badInheritance");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertRecords(program);
    assertTraits(program);
    assertComponents(program);
    assertFunctions(program);
  }

  private void assertFunctions(final CompilableProgram program) {
    var modules = program.getParsedModules("bad.inherited.functions");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.get(0).getModuleScope(), true);
    assertNoTypeHierarchy("Function1", resolver);
    assertNoTypeHierarchy("Function2", resolver);

    //3 and 4 extend
    assertHierarchy(resolver, "Function3", "Function1");
    assertHierarchy(resolver, "Function4", "Function2");

    assertNoTypeHierarchy("Function5", resolver);
    assertNoTypeHierarchy("Function6", resolver);
    assertNoTypeHierarchy("Function7", resolver);
  }

  private void assertComponents(final CompilableProgram program) {
    var modules = program.getParsedModules("bad.inherited.components");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.get(0).getModuleScope(), true);

    assertNoTypeHierarchy("Component1", resolver);
    assertNoTypeHierarchy("Component2", resolver);

    assertHierarchy(resolver, "Component3", "Component1");
    assertHierarchy(resolver, "Component4", "Component2");

    assertNoTypeHierarchy("Component5", resolver);
    assertNoTypeHierarchy("Component6", resolver);
    assertNoTypeHierarchy("Component7", resolver);
  }

  private void assertTraits(final CompilableProgram program) {
    var modules = program.getParsedModules("bad.inherited.traits");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.get(0).getModuleScope(), true);

    assertNoTypeHierarchy("Trait1", resolver);
    assertNoTypeHierarchy("Trait2", resolver);

    //When Traits extend Traits they 'have a trait of' not a 'is a' type relationship.
    assertNoTypeHierarchy("Trait3", resolver);
    assertNoTypeHierarchy("Trait4", resolver);
    assertNoTypeHierarchy("Trait5", resolver);
    assertNoTypeHierarchy("Trait6", resolver);

    assertTraits(resolver, "Trait3", "Trait1", "Trait2");
    assertTraits(resolver, "Trait4", "Trait2", "Trait1");
    assertTraits(resolver, "Trait5", "Trait3", "Trait1");
    assertTraits(resolver, "Trait6", "Trait4", "Trait2");

    assertNoTypeHierarchy("Trait7", resolver);
    assertNoTypeHierarchy("Trait8", resolver);
    assertNoTypeHierarchy("Trait9", resolver);
  }

  private void assertRecords(final CompilableProgram program) {
    var modules = program.getParsedModules("bad.inherited.records");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.get(0).getModuleScope(), true);

    assertRecord3Hierarchy(resolver);
    assertNoTypeHierarchy("Record4", resolver);
    assertNoTypeHierarchy("Record5", resolver);
  }

  private void assertRecord3Hierarchy(final SimpleResolverForTesting resolver) {
    var record3 = resolver.apply("Record3");
    assertTrue(record3.isPresent());
    record3.ifPresent(symbol -> {
      if (symbol instanceof IAggregateSymbol aggregate) {
        var record2 = assertAndGetAggregateSuper(Optional.of(aggregate), "Record3");
        var record1 = assertAndGetAggregateSuper(record2, "Record2");
        assertNoAggregateHierarchy(record1, "Record1");
      } else {
        fail("Expecting an aggregate");
      }
    });
  }

  private void assertTraits(final SimpleResolverForTesting resolver,
                            final String theTraitName, final String traitA, final String traitB) {
    var mainTrait = resolver.apply(theTraitName);
    assertTrue(mainTrait.isPresent());
    mainTrait.ifPresent(symbol -> {
      if (symbol instanceof AggregateWithTraitsSymbol aggregate) {
        var traits = assertAndGetTraits(Optional.of(aggregate), theTraitName);
        assertTrue(traits.contains(traitA));
        assertTrue(traits.contains(traitB));
      } else {
        fail("Expecting an aggregate");
      }
    });
  }

  private void assertHierarchy(final SimpleResolverForTesting resolver, final String theName,
                               final String theSuperName) {
    var mainType = resolver.apply(theName);
    assertTrue(mainType.isPresent());
    mainType.ifPresent(symbol -> {
      if (symbol instanceof IAggregateSymbol aggregate) {
        var superType = assertAndGetAggregateSuper(Optional.of(aggregate), theName);
        assertNoAggregateHierarchy(superType, theSuperName);
      } else if (symbol instanceof FunctionSymbol function) {
        var superType = assertAndGetFunctionSuper(Optional.of(function), theName);
        assertNoFunctionHierarchy(superType, theSuperName);
      } else {
        fail("Expecting an aggregate or function");
      }
    });
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private List<String> assertAndGetTraits(Optional<AggregateWithTraitsSymbol> type, String expectName) {
    assertTrue(type.isPresent());
    assertEquals(expectName, type.get().getName());

    var rtn = type.get().getAllTraits().stream().map(Symbol::getName).toList();
    assertFalse(rtn.isEmpty());
    return rtn;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<IAggregateSymbol> assertAndGetAggregateSuper(final Optional<IAggregateSymbol> type,
                                                                final String expectName) {
    assertTrue(type.isPresent());
    assertEquals(expectName, type.get().getName());
    return type.get().getSuperAggregateSymbol();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<FunctionSymbol> assertAndGetFunctionSuper(final Optional<FunctionSymbol> type,
                                                             final String expectName) {
    assertTrue(type.isPresent());
    assertEquals(expectName, type.get().getName());
    return type.get().getSuperFunctionSymbol();
  }

  private void assertNoTypeHierarchy(final String typeName, final SimpleResolverForTesting resolver) {
    assertNoTypeHierarchy(resolver.apply(typeName));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertNoTypeHierarchy(final Optional<ISymbol> type) {
    assertTrue(type.isPresent());
    if (type.get() instanceof AggregateSymbol aggregate) {
      assertFalse(aggregate.getSuperAggregateSymbol().isPresent());
    } else if (type.get() instanceof FunctionSymbol function) {
      assertFalse(function.getSuperFunctionSymbol().isPresent());
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertNoAggregateHierarchy(final Optional<IAggregateSymbol> type, final String expectName) {
    assertTrue(type.isPresent());
    assertEquals(expectName, type.get().getName());
    assertFalse(type.get().getSuperAggregateSymbol().isPresent());
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertNoFunctionHierarchy(final Optional<FunctionSymbol> type, final String expectName) {
    assertTrue(type.isPresent());
    assertEquals(expectName, type.get().getName());
    assertFalse(type.get().getSuperFunctionSymbol().isPresent());
  }
}
