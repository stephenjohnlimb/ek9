package org.ek9lang.compiler.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.search.TemplateTypeSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Going to be very hard to get your head around this as generics and
 * parameterised polymorphism is hard.
 * Especially templates that use templates.
 * This set of tests are aimed at checking the simpler sides of generic types.
 * There will be other tests the check generic Functions.
 * Plus dynamic Function use within a generic function/type.
 * But at a higher level where will be pure ek9 source tests with @directives in.
 * These tests just use a single global symbol table and made up types.
 */
final class GenericTypeTest extends AbstractSymbolTestBase {

  @Test
  void testTemplateTypeCreation() {
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));

    assertTrue(stringType.isPresent());
    assertTrue(integerType.isPresent());

    //The type T we are going to use as part of the template type definition
    var t = support.createGenericT("Tee", symbolTable);

    //Now the actual template type that can be parameterised.
    var z = support.createTemplateGenericType("Zee", symbolTable, t);
    symbolTable.define(z);

    //Now make sure it is possible to find the correct index of 'Tee'
    //This will be needed when we come to process parameterised types.
    Assertions.assertEquals(0, CommonParameterisedTypeDetails.getIndexOfType(z, Optional.of(t)));

    //Check we CANNOT find this as a type, but only as a template type.
    assertTrue(symbolTable.resolve(new TypeSymbolSearch("Zee")).isEmpty());
    assertTrue(symbolTable.resolve(new TypeSymbolSearch("Tee")).isEmpty());

    //Now check we can find it as a TEMPLATE_TYPE
    assertTrue(z.isGenericInNature());
    assertTrue(symbolTable.resolve(new TemplateTypeSymbolSearch("Zee")).isPresent());
    assertEquals("Zee of type Tee", z.getFriendlyName());

    //Now lets use that generic Z with a String and then with an Integer
    //In java it's like saying:
    //1. var stringZeeType = new Zee<String>();
    //2. var integerZeeType = new Zee<Integer>();

    var stringZeeType = new ParameterisedTypeSymbol(z, stringType, symbolTable);
    symbolTable.define(stringZeeType);
    assertEquals("Zee of String", stringZeeType.getFriendlyName());

    //It is now a real concrete type and not generic in nature.
    assertFalse(stringZeeType.isGenericInNature());
    //But it is actually a parameterised type
    assertTrue(stringZeeType.isParameterisedType());

    var integerZeeType = new ParameterisedTypeSymbol(z, integerType, symbolTable);
    symbolTable.define(integerZeeType);
    assertEquals("Zee of Integer", integerZeeType.getFriendlyName());
  }

  @Test
  void testMatchingListsOfTypes() {
    var dateType = symbolTable.resolve(new TypeSymbolSearch("Date"));
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(dateType.isPresent());
    assertTrue(stringType.isPresent());

    assertFalse(CommonParameterisedTypeDetails
        .doSymbolsMatch(
            List.of(dateType.get()), null
        )
    );
    assertFalse(CommonParameterisedTypeDetails
        .doSymbolsMatch(
            null, List.of(dateType.get())
        )
    );

    assertFalse(CommonParameterisedTypeDetails
        .doSymbolsMatch(
            List.of(dateType.get()), List.of(stringType.get())
        )
    );

    assertFalse(CommonParameterisedTypeDetails
        .doSymbolsMatch(
            List.of(dateType.get(), stringType.get()), List.of(stringType.get())
        )
    );

    assertFalse(CommonParameterisedTypeDetails
        .doSymbolsMatch(
            List.of(dateType.get(), stringType.get()), List.of(stringType.get(), dateType.get())
        )
    );

    assertTrue(CommonParameterisedTypeDetails
        .doSymbolsMatch(
            List.of(dateType.get()), List.of(dateType.get())
        )
    );

    assertTrue(CommonParameterisedTypeDetails
        .doSymbolsMatch(
            List.of(stringType.get(), dateType.get()), List.of(stringType.get(), dateType.get())
        )
    );
  }

  /**
   * A quick check of some type that can be parameterised with multiple types.
   * We've got multiple uses and reuses of generic types in here.
   */
  @Test
  void testMultipleT() {
    var dateType = symbolTable.resolve(new TypeSymbolSearch("Date"));
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    var booleanType = symbolTable.resolve(new TypeSymbolSearch("Boolean"));

    assertTrue(dateType.isPresent());
    assertTrue(stringType.isPresent());
    assertTrue(integerType.isPresent());
    assertTrue(booleanType.isPresent());

    var p = support.createGenericT("Pee", symbolTable);
    var q = support.createGenericT("Que", symbolTable);
    var r = support.createGenericT("Arr", symbolTable);

    //In Java speak public class Ex<Pee,Que,Arr>
    var x = support.createTemplateGenericType("Ex", symbolTable, List.of(p, q, r));
    symbolTable.define(x);

    //In Java speak public class Why<Pee,Arr>
    var y = support.createTemplateGenericType("Why", symbolTable, List.of(p, r));
    symbolTable.define(y);

    //Check we can find the types that have been used and cannot find the ones that haven't.
    //So Why does not have a Que.
    assertEquals(0, CommonParameterisedTypeDetails.getIndexOfType(y, Optional.of(p)));
    assertEquals(1, CommonParameterisedTypeDetails.getIndexOfType(y, Optional.of(r)));
    assertEquals(-1, CommonParameterisedTypeDetails.getIndexOfType(y, Optional.of(q)));

    //In Java speak public class Zee<Arr,Que,Pee>
    var z = support.createTemplateGenericType("Zee", symbolTable, List.of(r, q, p));
    symbolTable.define(z);

    //OK is your mind blown now?
    //We have a mix of real fixed types and 'T' types and also templates defined in terms of templates.

    //Expect this to fail as it needs 3 parameters. This will be checked by the compiler earlier, but check.
    Assertions.assertThrows(RuntimeException.class, () -> new ParameterisedTypeSymbol(z, stringType, symbolTable));

    //Now lets try using some of those templates to make concrete types
    //but also parameterised types, where some parameters are still generic
    //var testItem = new Zee<String, Integer, Boolean>();
    var theZType = new ParameterisedTypeSymbol(z,
        List.of(stringType.get(), integerType.get(), booleanType.get()), symbolTable);
    //z -> r, q, p so ZType -> String, Integer, Boolean: Hence r -> String, p -> Boolean, q -> Integer
    symbolTable.define(theZType);

    {
      //Make a parameterised type but with generic T's as if it is created within theZType - so it's still not concrete.
      var yDash = new ParameterisedTypeSymbol(y, List.of(p, r), symbolTable);

      //Now we need to see if we could work out what actual types should really be used with yDash
      var types = support.getSuitableParameters(theZType, yDash);
      //So what do we expect here?
      //whereas y -> p, r: When defined in the context of Z: we expect p -> Boolean, r -> String
      //Logger.log("types " + types);
      assertEquals("Boolean", types.get(0).getName());
      assertEquals("String", types.get(1).getName());
      //Now we would be able to create a real concrete type!
      //This in effect what our compiler will have to do, when processing generic type definitions
      //that actually use other generics within them. When the outer definition is made 'manifest'
      //the compiler has to make manifest all the other uses of other generic types that use both real
      //and conceptual generic 'T' parameters.
      var yDashDash = new ParameterisedTypeSymbol(y, types, symbolTable);
      assertEquals("Why of (Boolean, String)", yDashDash.getFriendlyName());
    }

    //Now lets try and 'X' but mix real Types and conceptual 'T'
    {
      var xDash = new ParameterisedTypeSymbol(x, List.of(r, r, dateType.get()), symbolTable);
      var types = support.getSuitableParameters(theZType, xDash);
      //So what do we expect here?
      //whereas x -> p, q, r: But xDash: p -> r, q -> r, r -> "Date"
      // When defined in the context of Z: we expect p -> String, q -> String - but r is fixed on "Date"
      //Logger.log("types " + types);
      assertEquals("String", types.get(0).getName());
      assertEquals("String", types.get(1).getName());
      assertEquals("Date", types.get(2).getName());
      var xDashDash = new ParameterisedTypeSymbol(x, types, symbolTable);
      assertEquals("Ex of (String, String, Date)", xDashDash.getFriendlyName());
    }

  }

  /**
   * Designed to test out if we can work out the concrete types
   * when using parameterised types within a parameterised type.
   * i.e. G of (S, T) for example, then inside type G we use S or T in another template type.
   * i.e. lets say with G we use a List of S or a Function of T. This means when we come to use
   * actual types like String or Integer for S and T, we must put those in place in G.
   * But in some cases within G of (S, T) we might use a List of Date i.e. we don;t need to replace!
   * <p>
   * HARD, very HARD (well for me anyway).
   */
  @Test
  void testCommonT() {
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));

    assertTrue(stringType.isPresent());
    assertTrue(integerType.isPresent());

    var t = support.createGenericT("Tee", symbolTable);
    var z = support.createTemplateGenericType("Zee", symbolTable, t);
    symbolTable.define(z);

    //Now a new type but also refers to the same 'Tee' i.e. feels like it is defined within Zee
    //So the conceptual 'Tee' we're talking about here is the same 'Tee'
    var p = support.createTemplateGenericType("Pee", symbolTable, t);
    symbolTable.define(p);

    //Now make a parameterised type but not with a concrete type but with Tee again.
    var pDash = new ParameterisedTypeSymbol(p, Optional.of(t), symbolTable);

    var stringZeeType = new ParameterisedTypeSymbol(z, stringType, symbolTable);
    symbolTable.define(stringZeeType);

    var integerZeeType = new ParameterisedTypeSymbol(z, integerType, symbolTable);
    symbolTable.define(integerZeeType);

    var types = support.getSuitableParameters(stringZeeType, pDash);

    assertFalse(types.isEmpty());
    assertTrue(types.get(0).isExactSameType(stringType.get()));

    var concreteTypes = support.getSuitableParameters(stringZeeType, integerZeeType);

    assertFalse(concreteTypes.isEmpty());
    assertTrue(concreteTypes.get(0).isExactSameType(integerType.get()));
  }
}
