package org.ek9lang.compiler.symbols.generics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.ParametricResolveOrDefine;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.support.InternalNameFor;
import org.ek9lang.compiler.support.ParameterizedSymbolCreator;
import org.ek9lang.compiler.support.TypeSubstitution;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.symbols.base.AbstractSymbolTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A more complete test. With nesting of Generics.
 * But focussed on a single type parameter all the way through.
 * <pre>
 *   D1 of type X
 *    methodOnD1()
 *      -> arg0 as X
 *      <- rtn as Boolean
 *    ...
 *   G1 of type P
 *    methodOnG1()
 *      ->
 *        arg0 as D1 of D1 of P
 *        arg1 as Integer
 *        arg2 as D1 of P
 *      <-
 *        rtn as P
 *    ...
 *   G2 of type Q
 *    ...
 *   SingleGeneric of type T
 *    methodOne()
 *      ->
 *        arg0 as T
 *        arg1 as G1 of T
 *        arg2 as G2 of T
 *        arg3 as G1 of G2 of T
 *      <-
 *        rtn as G2 of G1 of T
 *  ...
 *  //Now make a SingleGeneric of String
 *  concreteType as SingleGeneric of String
 * </pre>
 * What should we 'end up with'?
 * In abstract terms (because this is what we defined):
 * <pre>
 *   D1 of type X
 *   G1 of type P
 *   G2 of type Q
 *   SingleGeneric of type T
 * </pre>
 * But in concrete terms we should get this:
 * <pre>
 *   SingleGeneric of type T of type String
 *   D1 of type X of type String
 *   D1 of type X of type D1 of type X of type String
 *   G2 of type Q of type String
 *   G1 of type P of type String
 *   G1 of type P of type G2 of type Q of type String
 *   D1 of type X of type G2 of type Q of type String
 *   D1 of type X of type D1 of type X of type G2 of type Q of type String
 *   G2 of type Q of type G1 of type P of type String
 * </pre>
 */
class MoreCompleteGenericSubstitutionTest extends AbstractSymbolTestBase {

  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator(new InternalNameFor());

  private final ErrorListener errorListener = new ErrorListener("test");

  /**
   * This is the main test in this file, but there are some minor checks as well.
   */
  @Test
  void testAggregateSingleParameterButWithMultipleGenerics() {

    var parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);
    var typeSubstitution = new TypeSubstitution(parametricResolveOrDefine, errorListener);

    //'D1 of type X'
    var d1OfTypeX = createD1OfTypeX();

    //'G1 of type P'
    var g1OfTypeP = createG1OfTypeP(d1OfTypeX);

    //'G2 of type Q'
    var g2OfTypeQ = createG2OfTypeQ();

    //'SingleGeneric of type T'
    var singleGenericOfTypeT = createSingleGenericOfTypeT(g1OfTypeP, g2OfTypeQ);

    //'concreteType as SingleGeneric of String'
    var concreteType = creator.apply(singleGenericOfTypeT, List.of(ek9String));
    assertNotNull(concreteType);

    //But is should be concrete so cannot be parameterised and not used as a conceptual parameter.
    assertFalse(concreteType.isGenericInNature());
    assertFalse(concreteType.isConceptualTypeParameter());

    //On creating it will not have the methods/fields.
    assertEquals(0, concreteType.getSymbolsForThisScope().size());

    //Now complete the creation, via type substitution - but this will also check in the symbol table to see if one
    //already exists - in this case it won't.

    var concreteTypeWithTypeSubstitution = typeSubstitution.apply(concreteType);
    //This should have a single method on it.
    assertEquals(1, concreteTypeWithTypeSubstitution.getSymbolsForThisScope().size());

    //So lets have a look at the types involved on that method.
    var method = (MethodSymbol) concreteTypeWithTypeSubstitution.getSymbolsForThisScope().get(0);
    assertNotNull(method);

    var methodReturnType = method.getType();
    assertTrue(methodReturnType.isPresent());

    //Conceptual
    assertResolution("D1", SymbolCategory.TEMPLATE_TYPE);
    assertResolution("G1", SymbolCategory.TEMPLATE_TYPE);
    assertResolution("G2", SymbolCategory.TEMPLATE_TYPE);
    assertResolution("SingleGeneric", SymbolCategory.TEMPLATE_TYPE);

    //Concrete
    assertResolution("D1 of (String)", SymbolCategory.TYPE);
    assertResolution("D1 of (D1 of (String))", SymbolCategory.TYPE);
    assertResolution("G2 of (String)", SymbolCategory.TYPE);
    assertResolution("G1 of (String)", SymbolCategory.TYPE);
    assertResolution("G1 of (G2 of (String))", SymbolCategory.TYPE);
    assertResolution("D1 of (G2 of (String))", SymbolCategory.TYPE);
    assertResolution("D1 of (D1 of (G2 of (String)))", SymbolCategory.TYPE);
    assertResolution("G2 of (G1 of (String))", SymbolCategory.TYPE);
  }

  /**
   * This is just a basic check with concrete parameters.
   */
  @Test
  void testD1OfTypeX() {
    var parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);
    var typeSubstitution = new TypeSubstitution(parametricResolveOrDefine, errorListener);
    var d1OfTypeX = createD1OfTypeX();
    Assertions.assertEquals(1, d1OfTypeX.getSymbolsForThisScope().size());
    //Now make a parameterized version
    var d1OfString = creator.apply(d1OfTypeX, List.of(ek9String));

    assertEquals(0, d1OfString.getSymbolsForThisScope().size());
    //OK so made - now do type substitution - this should also record/define as appropriate
    var d1OfStringDash = typeSubstitution.apply(d1OfString);
    assertNotNull(d1OfStringDash);
    assertEquals(1, d1OfStringDash.getSymbolsForThisScope().size());

    //But now what if we did a 'D1 of D1 of String'?
    var d1OfD1OfString = creator.apply(d1OfTypeX, List.of(d1OfString));
    var d1OfD1OfStringDash = typeSubstitution.apply(d1OfD1OfString);
    assertNotNull(d1OfD1OfStringDash);
    assertEquals(1, d1OfD1OfStringDash.getSymbolsForThisScope().size());

    //Check the friendly naming. Can you call it 'friendly'?
    Assertions.assertEquals("D1 of type X", d1OfTypeX.getFriendlyName());
    assertEquals("D1 of type X of type String", d1OfStringDash.getFriendlyName());
    assertEquals("D1 of type X of type D1 of type X of type String", d1OfD1OfStringDash.getFriendlyName());

    var methodOnD1 = d1OfD1OfStringDash.getSymbolsForThisScope().get(0);
    assertEquals("public Boolean <- methodOnD1(arg0 as D1 of type X of type String)", methodOnD1.getFriendlyName());
  }

  private PossibleGenericSymbol createD1OfTypeX() {
    var d1 = new AggregateSymbol("D1", symbolTable);
    var x = aggregateManipulator.createGenericT("X", d1.getFullyQualifiedName(), symbolTable);

    d1.setModuleScope(symbolTable);
    d1.addTypeParameterOrArgument(x);

    var arg0 = new VariableSymbol("arg0", x);
    aggregateManipulator.addPublicMethod(d1, "methodOnD1", List.of(arg0), Optional.of(ek9Boolean));
    symbolTable.define(d1);
    return d1;
  }

  /**
   * This is more sophisticated in the sense that the 'String' is passed in via 'G1' as the 'P',
   * but must get routed through to the 'X' on 'D1'.
   */
  @Test
  void testG1OfTypeP() {
    var parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);
    var typeSubstitution = new TypeSubstitution(parametricResolveOrDefine, errorListener);

    var d1OfTypeX = createD1OfTypeX();
    assertTrue(d1OfTypeX.isGenericInNature());
    assertTrue(d1OfTypeX.isConceptualTypeParameter());
    assertEquals(SymbolCategory.TEMPLATE_TYPE, d1OfTypeX.getCategory());

    var g1OfTypeP = createG1OfTypeP(d1OfTypeX);
    assertTrue(g1OfTypeP.isGenericInNature());
    assertTrue(g1OfTypeP.isConceptualTypeParameter());
    assertEquals(SymbolCategory.TEMPLATE_TYPE, g1OfTypeP.getCategory());

    //Now make a parameterized version
    var g1OfString = creator.apply(g1OfTypeP, List.of(ek9String));
    //OK so made - now do type substitution - this should also record/define as appropriate
    var g1OfStringDash = typeSubstitution.apply(g1OfString);

    assertNotNull(g1OfStringDash);
    assertFalse(g1OfStringDash.isGenericInNature());
    assertFalse(g1OfStringDash.isConceptualTypeParameter());
    assertEquals(SymbolCategory.TYPE, g1OfStringDash.getCategory());

    assertEquals(1, g1OfStringDash.getSymbolsForThisScope().size());

    assertEquals("G1 of type P of type String", g1OfStringDash.getFriendlyName());

    var methodOnG1 = g1OfStringDash.getSymbolsForThisScope().get(0);
    assertEquals(
        "public String <- methodOnG1(arg0 as D1 of type X of type D1 of type X of type String, arg1 as Integer, arg2 as D1 of type X of type String)",
        methodOnG1.getFriendlyName());
  }

  private PossibleGenericSymbol createG1OfTypeP(final PossibleGenericSymbol d1) {
    var g1 = new AggregateSymbol("G1", symbolTable);
    var p = aggregateManipulator.createGenericT("P", g1.getFullyQualifiedName(), symbolTable);

    g1.setModuleScope(symbolTable);
    g1.addTypeParameterOrArgument(p);

    var d1OfP = creator.apply(d1, List.of(p));

    var d1OfD1OfP = creator.apply(d1, List.of(d1OfP));

    var arg0 = new VariableSymbol("arg0", d1OfD1OfP);
    var arg1 = new VariableSymbol("arg1", ek9Integer);
    var arg2 = new VariableSymbol("arg2", d1OfP);
    aggregateManipulator.addPublicMethod(g1, "methodOnG1", List.of(arg0, arg1, arg2), Optional.of(p));

    symbolTable.define(g1);
    return g1;
  }

  private PossibleGenericSymbol createG2OfTypeQ() {
    var g2 = new AggregateSymbol("G2", symbolTable);
    var q = aggregateManipulator.createGenericT("Q", g2.getFullyQualifiedName(), symbolTable);

    g2.setModuleScope(symbolTable);
    g2.addTypeParameterOrArgument(q);
    symbolTable.define(g2);

    return g2;
  }

  private PossibleGenericSymbol createSingleGenericOfTypeT(final PossibleGenericSymbol g1,
                                                           final PossibleGenericSymbol g2) {
    var singleGeneric = new AggregateSymbol("SingleGeneric", symbolTable);
    var t = aggregateManipulator.createGenericT("T", singleGeneric.getFullyQualifiedName(), symbolTable);

    singleGeneric.setModuleScope(symbolTable);
    singleGeneric.addTypeParameterOrArgument(t);

    var g1OfT = creator.apply(g1, List.of(t));
    var g2OfT = creator.apply(g2, List.of(t));

    var g1OfG2OfT = creator.apply(g1, List.of(g2OfT));
    var g2OfG1OfT = creator.apply(g2, List.of(g1OfT));

    var arg0 = new VariableSymbol("arg0", t);
    var arg1 = new VariableSymbol("arg1", g1OfT);
    var arg2 = new VariableSymbol("arg2", g2OfT);
    var arg3 = new VariableSymbol("arg3", g1OfG2OfT);

    aggregateManipulator.addPublicMethod(singleGeneric, "methodOne", List.of(arg0, arg1, arg2, arg3),
        Optional.of(g2OfG1OfT));

    symbolTable.define(singleGeneric);
    return singleGeneric;
  }
}
