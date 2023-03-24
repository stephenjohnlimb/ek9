package org.ek9lang.compiler.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ek9lang.compiler.symbol.support.ParameterizedGenericSymbolCreator;
import org.ek9lang.compiler.symbol.support.TypeSubstitution;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

class GenericParameterSubstitutionTest extends AbstractSymbolTestBase {

  private final ParameterizedGenericSymbolCreator creator = new ParameterizedGenericSymbolCreator();
  private final TypeSubstitution typeSubstitution = new TypeSubstitution();

  @Test
  void testFunctionSingleParameterSubstitution() {
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(stringType.isPresent());
    var durationType = symbolTable.resolve(new TypeSymbolSearch("Duration"));
    assertTrue(durationType.isPresent());

    var t = support.createGenericT("T", symbolTable);
    assertTrue(t.isConceptualTypeParameter());

    var aGenericFunction = new FunctionSymbol("SingleGenericFunction", symbolTable);
    aGenericFunction.setModuleScope(symbolTable);
    aGenericFunction.addTypeParameterOrArgument(t);

    //Add multiple parameters to the function
    var arg0 = new VariableSymbol("arg0", integerType);
    var arg1 = new VariableSymbol("arg1", t);
    var arg2 = new VariableSymbol("arg2", stringType);
    var rtn = new VariableSymbol("rtn", t);
    aGenericFunction.define(arg0);
    aGenericFunction.define(arg1);
    aGenericFunction.define(arg2);
    aGenericFunction.setReturningSymbol(rtn);

    var inTemplateForm = getMethodsAsString(aGenericFunction);
    var expectedForm = inTemplateForm.replace(" T", " String");
    var aParameterizedStringFunction = creator.apply(aGenericFunction, List.of(stringType.get()));
    //Check it has no parameters yet - we still need to populate them.
    assertEquals(0, aParameterizedStringFunction.getSymbolsForThisScope().size());

    var typeSubstitutedParameterisedFunction = typeSubstitution.apply(aParameterizedStringFunction);
    assertNotNull(typeSubstitutedParameterisedFunction);
    assertEquals(3, typeSubstitutedParameterisedFunction.getSymbolsForThisScope().size());

    var actualForm = getMethodsAsString(typeSubstitutedParameterisedFunction);

    System.out.println("Checking substitution from/to");
    System.out.println(inTemplateForm);
    System.out.println("=============================");
    System.out.println(expectedForm);
    System.out.println("=============================");
    assertEquals(expectedForm, actualForm);
  }

  @Test
  void testAggregateSingleParameterSubstitution() {
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(stringType.isPresent());
    var durationType = symbolTable.resolve(new TypeSymbolSearch("Duration"));
    assertTrue(durationType.isPresent());

    var t = support.createGenericT("T", symbolTable);
    assertTrue(t.isConceptualTypeParameter());

    //This will be our generic type that has one or more 'type parameters'
    var aGenericType = new AggregateSymbol("SingleGeneric", symbolTable);
    aGenericType.setModuleScope(symbolTable);
    aGenericType.addTypeParameterOrArgument(t);

    //So make a method accepts two parameters, one of which is a 'T' and needs substituting.
    //The return also needs substituting.
    var arg0 = new VariableSymbol("arg0", integerType);
    var arg1 = new VariableSymbol("arg1", t);
    var arg2 = new VariableSymbol("arg2", stringType);
    support.addConstructor(aGenericType);
    support.addConstructor(aGenericType, Optional.of(arg1));
    support.addPublicMethod(aGenericType, "methodOne", List.of(arg0, arg1), Optional.of(t));
    //Now add another method
    support.addPublicMethod(aGenericType, "methodTwo", List.of(arg0, arg1, arg2), Optional.of(t));
    //And another
    support.addPublicMethod(aGenericType, "methodThree", List.of(arg2, arg1, arg0), durationType);
    //Final one
    support.addPublicMethod(aGenericType, "methodFour", List.of(arg1, arg0), durationType);
    assertEquals(6, aGenericType.getSymbolsForThisScope().size());

    //We can check our symbol search and replace by just using this simple replace on the strings versions.
    var inTemplateForm = getMethodsAsString(aGenericType);

    //Now make a parameterised version with a String.
    var aParameterizedStringType = creator.apply(aGenericType, List.of(stringType.get()));

    //Check it has no methods yet - we still need to populate them.
    assertEquals(0, aParameterizedStringType.getSymbolsForThisScope().size());
    var constructorName = aGenericType.getName();
    var expectedConstructorName = aParameterizedStringType.getName();
    var expectedForm = inTemplateForm
        .replace(" T", " String")
        .replace(constructorName, expectedConstructorName);

    var typeSubstitutedParameterisedType = typeSubstitution.apply(aParameterizedStringType);
    assertNotNull(typeSubstitutedParameterisedType);
    assertEquals(6, typeSubstitutedParameterisedType.getSymbolsForThisScope().size());

    var actualForm = getMethodsAsString(typeSubstitutedParameterisedType);

    System.out.println("Checking substitution from/to");
    System.out.println(inTemplateForm);
    System.out.println("=============================");
    System.out.println(expectedForm);
    System.out.println("=============================");
    assertEquals(expectedForm, actualForm);
  }

  @Test
  void testAggregateMultipleParameterSubstitutions() {
    //But this also includes some concrete types!
    var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
    assertTrue(integerType.isPresent());
    var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
    assertTrue(stringType.isPresent());
    var durationType = symbolTable.resolve(new TypeSymbolSearch("Duration"));
    assertTrue(durationType.isPresent());
    var booleanType = symbolTable.resolve(new TypeSymbolSearch("Boolean"));
    assertTrue(booleanType.isPresent());
    var floatType = symbolTable.resolve(new TypeSymbolSearch("Float"));
    assertTrue(floatType.isPresent());

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
    aGenericType.addTypeParameterOrArgument(integerType.get());
    aGenericType.addTypeParameterOrArgument(q);
    aGenericType.addTypeParameterOrArgument(booleanType.get());
    aGenericType.addTypeParameterOrArgument(booleanType.get());
    aGenericType.addTypeParameterOrArgument(r);
    aGenericType.addTypeParameterOrArgument(s);

    var arg0 = new VariableSymbol("arg0", integerType);
    var arg1 = new VariableSymbol("arg1", p);
    var arg2 = new VariableSymbol("arg2", booleanType);
    var arg3 = new VariableSymbol("arg3", q);
    var arg4 = new VariableSymbol("arg4", r);
    var arg5 = new VariableSymbol("arg5", durationType);
    support.addPublicMethod(aGenericType, "methodOne", List.of(arg0, arg1, arg2, arg3, arg4, arg5), Optional.of(s));

    assertEquals(1, aGenericType.getSymbolsForThisScope().size());

    //We can check our symbol search and replace by just using this simple replace on the strings versions.
    var inTemplateForm = getMethodsAsString(aGenericType);
    //A bit of crappy way to do this - but quick and easy - and conceptually easy to understand whats going on.
    var expectedForm = inTemplateForm
        .replace(" S", " Duration")
        .replace(" P", " String")
        .replace(" Q", " Float")
        .replace(" R", " Boolean");

    var aParameterizedStringType =
        creator.apply(aGenericType,
            List.of(stringType.get(), floatType.get(), booleanType.get(), durationType.get()));

    //Check it has no methods yet - we still need to populate them.
    assertEquals(0, aParameterizedStringType.getSymbolsForThisScope().size());

    var typeSubstitutedParameterisedType = typeSubstitution.apply(aParameterizedStringType);
    assertNotNull(typeSubstitutedParameterisedType);
    assertEquals(1, typeSubstitutedParameterisedType.getSymbolsForThisScope().size());

    var actualForm = getMethodsAsString(typeSubstitutedParameterisedType);
    System.out.println("Checking substitution from/to");
    System.out.println(inTemplateForm);
    System.out.println("=============================");
    System.out.println(expectedForm);
    System.out.println("=============================");

    assertEquals(expectedForm, actualForm);
  }

  private String getMethodsAsString(final IScopedSymbol scopedSymbol) {
    return scopedSymbol.getSymbolsForThisScope().stream().map(ISymbol::getFriendlyName)
        .collect(Collectors.joining("\n"));
  }

}
