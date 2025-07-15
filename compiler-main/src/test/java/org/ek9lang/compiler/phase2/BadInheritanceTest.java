package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SimpleResolverForTesting;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IFunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.Symbol;
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

    final var ek9Any = program.getEk9Types().ek9Any();
    assertNotNull(ek9Any);

    assertRecords(ek9Any, program);
    assertTraits(ek9Any, program);
    assertComponents(ek9Any, program);
    assertFunctions(ek9Any, program);
  }

  private void assertFunctions(final ISymbol ek9Any, final CompilableProgram program) {
    var modules = program.getParsedModules("bad.inherited.functions");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.getFirst().getModuleScope(), true);
    assertAnyTypeHierarchy(ek9Any, "Function1", resolver);
    assertAnyTypeHierarchy(ek9Any, "Function2", resolver);

    //3 and 4 extend
    assertHierarchy(ek9Any, resolver, "Function3", "Function1");
    assertHierarchy(ek9Any, resolver, "Function4", "Function2");

    assertNoTypeHierarchy("Function5", resolver);
    assertNoTypeHierarchy("Function6", resolver);
    assertNoTypeHierarchy("Function7", resolver);
  }

  private void assertComponents(final ISymbol ek9Any, final CompilableProgram program) {
    var modules = program.getParsedModules("bad.inherited.components");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.getFirst().getModuleScope(), true);

    assertAnyTypeHierarchy(ek9Any, "Component1", resolver);
    assertAnyTypeHierarchy(ek9Any,"Component2", resolver);

    assertHierarchy(ek9Any, resolver, "Component3", "Component1");
    assertHierarchy(ek9Any, resolver, "Component4", "Component2");

    assertNoTypeHierarchy("Component5", resolver);
    assertNoTypeHierarchy("Component6", resolver);
    assertNoTypeHierarchy("Component7", resolver);
    assertNoTypeHierarchy("Component8", resolver);
    assertNoTypeHierarchy("Component9", resolver);
  }

  private void assertTraits(final ISymbol ek9Any, final CompilableProgram program) {
    var modules = program.getParsedModules("bad.inherited.traits");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.getFirst().getModuleScope(), true);

    assertAnyTypeHierarchy(ek9Any,"Trait1", resolver);
    assertAnyTypeHierarchy(ek9Any,"Trait2", resolver);

    //When Traits extend Traits they 'have a trait of' not a 'is a' type relationship.
    assertAnyTypeHierarchy(ek9Any,"Trait3", resolver);
    assertAnyTypeHierarchy(ek9Any,"Trait4", resolver);
    assertAnyTypeHierarchy(ek9Any,"Trait5", resolver);
    assertAnyTypeHierarchy(ek9Any,"Trait6", resolver);

    assertTraits(resolver, "Trait3", "Trait1", "Trait2");
    assertTraits(resolver, "Trait4", "Trait2", "Trait1");
    assertTraits(resolver, "Trait5", "Trait3", "Trait1");
    assertTraits(resolver, "Trait6", "Trait4", "Trait2");

    assertAnyTypeHierarchy(ek9Any,"Trait7", resolver);
    assertAnyTypeHierarchy(ek9Any,"Trait8", resolver);
    assertAnyTypeHierarchy(ek9Any,"Trait9", resolver);
    assertAnyTypeHierarchy(ek9Any,"Trait10", resolver);
  }

  private void assertRecords(final ISymbol ek9Any, final CompilableProgram program) {
    var modules = program.getParsedModules("bad.inherited.records");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.getFirst().getModuleScope(), true);

    assertRecord3Hierarchy(ek9Any, resolver);
    assertNoTypeHierarchy("Record4", resolver);
    assertNoTypeHierarchy("Record5", resolver);
  }

  private void assertRecord3Hierarchy(final ISymbol ek9Any, final SimpleResolverForTesting resolver) {
    var record3 = resolver.apply("Record3");
    assertTrue(record3.isPresent());
    record3.ifPresent(symbol -> {
      if (symbol instanceof IAggregateSymbol aggregate) {
        var record2 = assertAndGetAggregateSuper(Optional.of(aggregate), "Record3");
        var record1 = assertAndGetAggregateSuper(record2, "Record2");
        assertAnyTypeAggregateHierarchy(ek9Any, record1, "Record1");
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

  private void assertHierarchy(final ISymbol ek9Any, final SimpleResolverForTesting resolver, final String theName,
                               final String theSuperName) {

    var mainType = resolver.apply(theName);
    assertTrue(mainType.isPresent());
    mainType.ifPresent(symbol -> {
      if (symbol instanceof IAggregateSymbol aggregate) {
        var superType = assertAndGetAggregateSuper(Optional.of(aggregate), theName);
        assertAnyTypeAggregateHierarchy(ek9Any, superType, theSuperName);
      } else if (symbol instanceof FunctionSymbol function) {
        var superType = assertAndGetFunctionSuper(Optional.of(function), theName);
        assertAnyFunctionHierarchy(ek9Any, superType, theSuperName);
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
    return type.get().getSuperAggregate();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<IFunctionSymbol> assertAndGetFunctionSuper(final Optional<FunctionSymbol> type,
                                                              final String expectName) {
    assertTrue(type.isPresent());
    assertEquals(expectName, type.get().getName());
    return type.get().getSuperFunction();
  }

  private void assertAnyTypeHierarchy(final ISymbol ek9Any, final String typeName, final SimpleResolverForTesting resolver) {
    assertAnyTypeHierarchy(ek9Any, resolver.apply(typeName));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertAnyTypeHierarchy(final ISymbol ek9Any, final Optional<ISymbol> type) {
    assertTrue(type.isPresent());
    if (type.get() instanceof AggregateSymbol aggregate) {
      assertTrue(aggregate.getSuperAggregate().isPresent());
      aggregate.getSuperAggregate().ifPresent(superType -> assertTrue(superType.isExactSameType(ek9Any)));
    } else if (type.get() instanceof FunctionSymbol function) {
      assertTrue(function.getSuperFunction().isPresent());
      function.getSuperFunction().ifPresent(superFunction -> assertTrue(superFunction.isExactSameType(ek9Any)));
    }
  }
  private void assertNoTypeHierarchy(final String typeName, final SimpleResolverForTesting resolver) {
    assertNoTypeHierarchy(resolver.apply(typeName));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertNoTypeHierarchy(final Optional<ISymbol> type) {
    assertTrue(type.isPresent());
    if (type.get() instanceof AggregateSymbol aggregate) {
      assertFalse(aggregate.getSuperAggregate().isPresent());
    } else if (type.get() instanceof FunctionSymbol function) {
      assertFalse(function.getSuperFunction().isPresent());
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertAnyTypeAggregateHierarchy(final ISymbol ek9Any, final Optional<IAggregateSymbol> type, final String expectName) {
    assertTrue(type.isPresent());
    assertEquals(expectName, type.get().getName());
    assertTrue(type.get().getSuperAggregate().isPresent());
    type.get().getSuperAggregate().ifPresent(superType -> assertTrue(superType.isExactSameType(ek9Any)));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertAnyFunctionHierarchy(final ISymbol ek9Any, final Optional<IFunctionSymbol> type, final String expectName) {
    assertTrue(type.isPresent());
    assertEquals(expectName, type.get().getName());
    assertTrue(type.get().getSuperFunction().isPresent());
    type.get().getSuperFunction().ifPresent(superFunctionType -> assertTrue(superFunctionType.isExactSameType(ek9Any)));
  }
}
