package org.ek9lang.compiler.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ek9lang.compiler.ParametricResolveOrDefine;
import org.ek9lang.compiler.support.ParameterizedSymbolCreator;
import org.ek9lang.compiler.support.TypeSubstitution;
import org.junit.jupiter.api.Test;

/**
 * Testing and checking generic type substitutions.
 * Checks Function and Aggregate with one and several type parameters and type arguments.
 * Simple cases, see MoreCompleteGenericSubstitutionTest for more.
 */
class GenericParameterSubstitutionTest extends AbstractSymbolTestBase {

  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator();

  @Test
  void testFunctionSingleParameterSubstitution() {
    var parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);
    var typeSubstitution = new TypeSubstitution(parametricResolveOrDefine);

    var t = support.createGenericT("T", symbolTable);

    var aGenericFunction = new FunctionSymbol("SingleGenericFunction", symbolTable);
    aGenericFunction.setModuleScope(symbolTable);
    aGenericFunction.addTypeParameterOrArgument(t);

    //Add multiple parameters to the function
    var arg0 = new VariableSymbol("arg0", ek9Integer);
    var arg1 = new VariableSymbol("arg1", t);
    var arg2 = new VariableSymbol("arg2", ek9String);
    var rtn = new VariableSymbol("rtn", t);
    aGenericFunction.define(arg0);
    aGenericFunction.define(arg1);
    aGenericFunction.define(arg2);
    aGenericFunction.setReturningSymbol(rtn);

    //Here take 'genericType of 'SingleGenericFunction' with 'type parameter' of 'T' and
    //parameterize it with 'type argument' 'Duration'.
    var aParameterizedStringFunction = creator.apply(aGenericFunction, List.of(ek9Duration));

    //Check the function itself has no parameters yet - we still need to populate them.
    assertEquals(0, aParameterizedStringFunction.getSymbolsForThisScope().size());

    //This is now what we'd expect when T -> Duration.
    var expecting = """
        arg0 as Integer
        arg1 as Duration
        arg2 as String""";

    //This type substitution is where the 'magic' happens - it's much more complex than you'd expect.
    var typeSubstitutedParameterisedFunction = typeSubstitution.apply(aParameterizedStringFunction);

    assertNotNull(typeSubstitutedParameterisedFunction);
    assertEquals(3, typeSubstitutedParameterisedFunction.getSymbolsForThisScope().size());

    var actualForm = getScopeSymbolsAsString(typeSubstitutedParameterisedFunction);

    assertEquals(expecting, actualForm);
  }

  @Test
  void testAggregateSingleParameterSubstitution() {

    var parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);
    var typeSubstitution = new TypeSubstitution(parametricResolveOrDefine);

    var t = support.createGenericT("T", symbolTable);
    assertTrue(t.isConceptualTypeParameter());

    //This will be our generic type that has one or more 'type parameters'
    var aGenericType = new AggregateSymbol("SingleGeneric", symbolTable);
    aGenericType.setModuleScope(symbolTable);
    aGenericType.addTypeParameterOrArgument(t);

    //So make a method accepts two parameters, one of which is a 'T' and needs substituting.
    //The return also needs substituting.
    var arg0 = new VariableSymbol("arg0", ek9Integer);
    var arg1 = new VariableSymbol("arg1", t);
    var arg2 = new VariableSymbol("arg2", ek9String);
    support.addConstructor(aGenericType);
    support.addConstructor(aGenericType, Optional.of(arg1));
    support.addPublicMethod(aGenericType, "methodOne", List.of(arg0, arg1), Optional.of(t));
    //Now add another method
    support.addPublicMethod(aGenericType, "methodTwo", List.of(arg0, arg1, arg2), Optional.of(t));
    //And another
    support.addPublicMethod(aGenericType, "methodThree", List.of(arg2, arg1, arg0), Optional.of(ek9Duration));
    //Final one
    support.addPublicMethod(aGenericType, "methodFour", List.of(arg1, arg0), Optional.of(ek9Duration));
    assertEquals(6, aGenericType.getSymbolsForThisScope().size());

    //Now make a parameterised version with a String.
    var aParameterizedStringType = creator.apply(aGenericType, List.of(ek9String));

    //Check it has no methods yet - we still need to populate them.
    assertEquals(0, aParameterizedStringType.getSymbolsForThisScope().size());

    var expecting = """
        public SingleGeneric of type T of type String <- _SingleGeneric_C7B334A5597C3F213CB74D5A0DB330083ECDFEBEF4E2FAA7E85B115448FC645F()
        public SingleGeneric of type T of type String <- _SingleGeneric_C7B334A5597C3F213CB74D5A0DB330083ECDFEBEF4E2FAA7E85B115448FC645F(arg1 as String)
        public String <- methodOne(arg0 as Integer, arg1 as String)
        public String <- methodTwo(arg0 as Integer, arg1 as String, arg2 as String)
        public Duration <- methodThree(arg2 as String, arg1 as String, arg0 as Integer)
        public Duration <- methodFour(arg1 as String, arg0 as Integer)""";

    var typeSubstitutedParameterisedType = typeSubstitution.apply(aParameterizedStringType);
    assertNotNull(typeSubstitutedParameterisedType);
    assertEquals(6, typeSubstitutedParameterisedType.getSymbolsForThisScope().size());
    var actualForm = getScopeSymbolsAsString(typeSubstitutedParameterisedType);

    assertEquals(expecting, actualForm);
  }

  @Test
  void testAggregateMultipleParameterSubstitutions() {

    var parametricResolveOrDefine = new ParametricResolveOrDefine(symbolTable);
    var typeSubstitution = new TypeSubstitution(parametricResolveOrDefine);

    var p = support.createGenericT("P", symbolTable);
    assertTrue(p.isConceptualTypeParameter());

    var q = support.createGenericT("Q", symbolTable);
    assertTrue(q.isConceptualTypeParameter());

    var r = support.createGenericT("R", symbolTable);
    assertTrue(r.isConceptualTypeParameter());

    var s = support.createGenericT("S", symbolTable);
    assertTrue(s.isConceptualTypeParameter());

    //This will be our generic type that has one or more 'type parameters'
    //So a horrible mix of conceptual and concrete parameters.
    //But only 4 that need replacing.
    var aGenericType = new AggregateSymbol("MultiGeneric", symbolTable);
    aGenericType.setModuleScope(symbolTable);
    aGenericType.addTypeParameterOrArgument(p);
    aGenericType.addTypeParameterOrArgument(ek9Integer);
    aGenericType.addTypeParameterOrArgument(q);
    aGenericType.addTypeParameterOrArgument(ek9Boolean);
    aGenericType.addTypeParameterOrArgument(ek9Boolean);
    aGenericType.addTypeParameterOrArgument(r);
    aGenericType.addTypeParameterOrArgument(s);

    var arg0 = new VariableSymbol("arg0", ek9Integer);
    var arg1 = new VariableSymbol("arg1", p);
    var arg2 = new VariableSymbol("arg2", ek9Boolean);
    var arg3 = new VariableSymbol("arg3", q);
    var arg4 = new VariableSymbol("arg4", r);
    var arg5 = new VariableSymbol("arg5", ek9Time);
    support.addPublicMethod(aGenericType, "methodOne", List.of(arg0, arg1, arg2, arg3, arg4, arg5), Optional.of(s));

    assertEquals(1, aGenericType.getSymbolsForThisScope().size());

    var aParameterizedStringType = creator.apply(aGenericType, List.of(ek9String, ek9Float, ek9Boolean, ek9Duration));

    //Check it has no methods yet - we still need to populate them.
    assertEquals(0, aParameterizedStringType.getSymbolsForThisScope().size());

    var expecting = """
        public Duration <- methodOne(arg0 as Integer, arg1 as String, arg2 as Boolean, arg3 as Float, arg4 as Boolean, arg5 as Time)""";

    var typeSubstitutedParameterisedType = typeSubstitution.apply(aParameterizedStringType);
    assertNotNull(typeSubstitutedParameterisedType);
    assertEquals(1, typeSubstitutedParameterisedType.getSymbolsForThisScope().size());

    var actualForm = getScopeSymbolsAsString(typeSubstitutedParameterisedType);

    assertEquals(expecting, actualForm);
  }

  private String getScopeSymbolsAsString(final IScopedSymbol scopedSymbol) {
    return scopedSymbol.getSymbolsForThisScope().stream().map(ISymbol::getFriendlyName)
        .collect(Collectors.joining("\n"));
  }
}
