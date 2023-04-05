package org.ek9lang.compiler.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.junit.jupiter.api.Test;

final class AggregateWithTraitsSymbolTest {

  @Test
  void testEmptyAggregateWithTraits() {
    SymbolTable symbolTable = new SymbolTable();

    AggregateWithTraitsSymbol underTest = new AggregateWithTraitsSymbol("UnderTest", symbolTable);
    assertEquals("UnderTest", underTest.getAggregateDescription());

    var clone = underTest.clone(symbolTable);
    assertEquals(underTest.hashCode(), clone.hashCode());
    assertEquals(underTest, clone);
  }

  @Test
  void testWithTraits() {
    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol booleanType = new AggregateSymbol("Boolean", symbolTable);
    symbolTable.define(booleanType);

    //used later for checking extendability.
    var anyAggregate = new AggregateSymbol("AnySort", symbolTable);

    var costAssessment = makeTraitWithLowCostMethod(symbolTable, "CostAssessment");
    var moniterable = makeTraitWithLowCostMethod(symbolTable, "Moniterable");

    assertNotEquals(costAssessment, moniterable);

    assertTrue(costAssessment.isAssignableTo(costAssessment));
    assertFalse(costAssessment.isAssignableTo(moniterable));
    assertFalse(moniterable.isAssignableTo(costAssessment));

    //now a trait that has these two traits!
    var processor = makeTraitWithLowCostMethod(symbolTable, "Processor");
    processor.addTrait(costAssessment);
    processor.addTrait(moniterable);

    //Check traits are taken into account
    assertTrue(processor.isAssignableTo(processor));

    assertEquals(2, processor.getAllTraits().size());
    assertFalse(processor.isExtensionConstrained());
    assertTrue(processor.hasImmediateTrait(moniterable));

    assertTrue(processor.isAllowingExtensionBy(anyAggregate));

    //So while this limited process does not 'extend' any other traits.
    //It will limit anything that implements/or extends it to only allow the cost assessment.
    //This is a bit like Java sealed classes.
    var limitedProcessor = makeTraitWithLowCostMethod(symbolTable, "LimitedProcessor");
    limitedProcessor.addAllowedExtender(costAssessment);
    assertEquals(0, limitedProcessor.getAllTraits().size());
    assertTrue(limitedProcessor.isExtensionConstrained());
    assertEquals(1, limitedProcessor.getAllowedExtenders().size());
    assertFalse(limitedProcessor.isAllowingExtensionBy(anyAggregate));
    assertFalse(limitedProcessor.isAllowingExtensionBy(moniterable));
    assertTrue(limitedProcessor.isAllowingExtensionBy(costAssessment));

    var limitedExtender = new AggregateWithTraitsSymbol("LimitedExtender", symbolTable);
    limitedExtender.addTrait(limitedProcessor);

    assertEquals(1, limitedExtender.getAllExtensionConstrainedTraits().size());
    assertEquals(limitedProcessor, limitedExtender.getAllExtensionConstrainedTraits().get(0));

    //Now make an aggregate with trait of processor -> moniterable and costAssessment
    var extender = new AggregateWithTraitsSymbol("Extender", symbolTable);
    extender.addTrait(processor);
    //So this should give us processor, moniterable and costAssessment
    assertEquals(3, extender.getAllTraits().size());
    assertTrue(extender.getAllAbstractMethods().isEmpty());

    //Check assignment with compatible trait is possible.
    assertTrue(extender.isAssignableTo(processor));
    assertTrue(extender.isAssignableTo(costAssessment));
    assertTrue(extender.isAssignableTo(moniterable));
    //But not the converse
    assertFalse(processor.isAssignableTo(extender));
    assertFalse(costAssessment.isAssignableTo(extender));
    assertFalse(moniterable.isAssignableTo(extender));

    //So get all those low cost methods! From each of the traits.
    List<MethodSymbol> nonAbstracts = extender.getAllNonAbstractMethods();
    assertEquals(3, nonAbstracts.size());

    var lowCostMethodToBeUsed = extender.resolve(new MethodSymbolSearch("lowCost"));
    assertTrue(lowCostMethodToBeUsed.isPresent());

    //Now a plain aggregate with the extender as a super.
    var plainAggregate = new AggregateSymbol("PlainAggregate", symbolTable);
    plainAggregate.setSuperAggregateSymbol(extender);
    //While this plain aggregate does not apply the traits, its super 'extender' does.
    assertEquals(3, plainAggregate.getAllTraits().size());

    var results =
        plainAggregate.resolveMatchingMethods(new MethodSymbolSearch("lowCost"), new MethodSymbolSearchResult());
    assertTrue(results.getSingleBestMatchSymbol().isPresent());
    assertEquals(lowCostMethodToBeUsed.get(), results.getSingleBestMatchSymbol().get());

    assertTrue(plainAggregate.getAllAbstractMethods().isEmpty());

    //Even though indirect - it is implemented.
    assertTrue(plainAggregate.isImplementingInSomeWay(costAssessment));

    var assignableWeight = plainAggregate.getAssignableWeightTo(Optional.of(plainAggregate));
    assertTrue(assignableWeight < 0.001 && assignableWeight > -0.0001);
  }

  private AggregateWithTraitsSymbol makeTraitWithLowCostMethod(final SymbolTable symbolTable, final String traitName) {
    var booleanType = symbolTable.resolve(new SymbolSearch("Boolean"));

    AggregateWithTraitsSymbol trait = new AggregateWithTraitsSymbol(traitName, symbolTable);
    trait.setGenus(ISymbol.SymbolGenus.CLASS_TRAIT);
    var lowCostMethod = new MethodSymbol("lowCost", trait);
    lowCostMethod.setReturningSymbol(new VariableSymbol("rtn", booleanType));
    trait.define(lowCostMethod);

    return trait;
  }
}
