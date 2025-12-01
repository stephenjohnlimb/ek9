package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ConstantSymbol;
import org.ek9lang.compiler.symbols.ControlSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.symbols.base.AbstractSymbolTestBase;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.junit.jupiter.api.Test;

/**
 * Tests for SymbolHoverFormatter to verify the markdown output
 * for different symbol types.
 */
final class SymbolHoverFormatterTest extends AbstractSymbolTestBase {

  private final SymbolHoverFormatter formatter = new SymbolHoverFormatter();

  @Test
  void testEmptyOptionalReturnsNull() {
    var result = formatter.apply(Optional.empty());
    assertNull(result, "Empty optional should return null");
  }

  @Test
  void testVariableWithType() {
    var variable = new VariableSymbol("counter", Optional.of(ek9Integer));
    variable.setSourceToken(new Ek9Token("test.ek9", 10));

    var hover = formatter.apply(Optional.of(variable));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();
    assertNotNull(content);

    // Verify the hover content
    assertTrue(content.contains("**counter**"), "Should contain variable name in bold");
    assertTrue(content.contains("`Integer`"), "Should contain type in code format");
    assertTrue(content.contains("*Kind*: Variable"), "Should indicate it's a variable");
    assertTrue(content.contains("Line 10"), "Should show source location");

    System.out.println("=== Variable Hover ===");
    System.out.println(content);
  }

  @Test
  void testConstantVariable() {
    // Use ConstantSymbol which properly represents immutable values
    var constant = new ConstantSymbol("MAX_VALUE", ek9Integer, false);
    constant.setNotMutable();
    constant.setSourceToken(new Ek9Token("constants.ek9", 5));

    var hover = formatter.apply(Optional.of(constant));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**MAX_VALUE**"));
    assertTrue(content.contains("*Modifier*: constant"), "Should indicate constant");

    System.out.println("=== Constant Hover ===");
    System.out.println(content);
  }

  @Test
  void testFunctionWithParametersAndReturn() {
    var function = new FunctionSymbol("calculateSum", symbolTable);
    function.setSourceToken(new Ek9Token("math.ek9", 15));

    // Add parameters
    var param1 = new VariableSymbol("a", Optional.of(ek9Integer));
    param1.setIncomingParameter(true);
    function.define(param1);

    var param2 = new VariableSymbol("b", Optional.of(ek9Integer));
    param2.setIncomingParameter(true);
    function.define(param2);

    // Add return
    var returnVar = new VariableSymbol("result", Optional.of(ek9Integer));
    returnVar.setReturningParameter(true);
    function.setReturningSymbol(returnVar);

    var hover = formatter.apply(Optional.of(function));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("calculateSum"), "Should contain function name");
    assertTrue(content.contains("*Parameters*:"), "Should show parameters");
    assertTrue(content.contains("a as Integer"), "Should show first parameter");
    assertTrue(content.contains("b as Integer"), "Should show second parameter");
    assertTrue(content.contains("*Returns*: `Integer`"), "Should show return type");
    assertTrue(content.contains("Line 15"), "Should show source location");

    System.out.println("=== Function Hover ===");
    System.out.println(content);
  }

  @Test
  void testPureFunction() {
    var function = new FunctionSymbol("getValue", symbolTable);
    function.setMarkedPure(true);
    function.setSourceToken(new Ek9Token("pure.ek9", 20));

    var returnVar = new VariableSymbol("rtn", Optional.of(ek9String));
    returnVar.setReturningParameter(true);
    function.setReturningSymbol(returnVar);

    var hover = formatter.apply(Optional.of(function));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("*Modifier*: pure"), "Should indicate pure function");

    System.out.println("=== Pure Function Hover ===");
    System.out.println(content);
  }

  @Test
  void testMethodOnAggregate() {
    var aggregate = new AggregateSymbol("Customer", symbolTable);

    var method = new MethodSymbol("getName", aggregate);
    method.setSourceToken(new Ek9Token("customer.ek9", 25));

    var returnVar = new VariableSymbol("name", Optional.of(ek9String));
    returnVar.setReturningParameter(true);
    method.setReturningSymbol(returnVar);

    aggregate.define(method);

    var hover = formatter.apply(Optional.of(method));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**getName**"), "Should contain method name");
    assertTrue(content.contains("*On*: `Customer`"), "Should show parent aggregate");
    assertTrue(content.contains("*Returns*: `String`"), "Should show return type");

    System.out.println("=== Method Hover ===");
    System.out.println(content);
  }

  @Test
  void testMethodWithParameters() {
    var aggregate = new AggregateSymbol("Calculator", symbolTable);

    var method = new MethodSymbol("add", aggregate);
    method.setSourceToken(new Ek9Token("calc.ek9", 30));

    var param = new VariableSymbol("value", Optional.of(ek9Float));
    param.setIncomingParameter(true);
    method.define(param);

    var returnVar = new VariableSymbol("result", Optional.of(ek9Float));
    returnVar.setReturningParameter(true);
    method.setReturningSymbol(returnVar);

    aggregate.define(method);

    var hover = formatter.apply(Optional.of(method));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("*Parameters*: `(value as Float)`"), "Should show parameters");

    System.out.println("=== Method with Params Hover ===");
    System.out.println(content);
  }

  @Test
  void testAbstractMethod() {
    var aggregate = new AggregateSymbol("Shape", symbolTable);

    var method = new MethodSymbol("draw", aggregate);
    method.setMarkedAbstract(true);
    method.setSourceToken(new Ek9Token("shape.ek9", 35));

    aggregate.define(method);

    var hover = formatter.apply(Optional.of(method));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("*Modifier*: abstract"), "Should indicate abstract method");

    System.out.println("=== Abstract Method Hover ===");
    System.out.println(content);
  }

  @Test
  void testClassType() {
    var classType = new AggregateSymbol("Person", symbolTable);
    classType.setGenus(SymbolGenus.CLASS);
    classType.setSourceToken(new Ek9Token("person.ek9", 1));

    var hover = formatter.apply(Optional.of(classType));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**Person**"), "Should contain type name");
    assertTrue(content.contains("*Kind*:"), "Should show kind");
    assertTrue(content.contains("Line 1"), "Should show source location");

    System.out.println("=== Class Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testGenericType() {
    var genericType = new AggregateSymbol("List", symbolTable);
    genericType.setGenus(SymbolGenus.CLASS);
    genericType.setOpenForExtension(true);
    genericType.setSourceToken(new Ek9Token("list.ek9", 1));

    // Add a type parameter to make it generic
    var typeParam = aggregateManipulator.createGenericT("T", "List", symbolTable);
    genericType.addTypeParameterOrArgument(typeParam);

    var hover = formatter.apply(Optional.of(genericType));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("*Generic*: yes"), "Should indicate generic type");

    System.out.println("=== Generic Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testControlSymbol() {
    var control = new ControlSymbol("if", symbolTable);
    control.setSourceToken(new Ek9Token("control.ek9", 50));

    var hover = formatter.apply(Optional.of(control));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**if**"), "Should contain control name");
    assertTrue(content.contains("*Kind*: Control structure"), "Should indicate control structure");

    System.out.println("=== Control Symbol Hover ===");
    System.out.println(content);
  }

  @Test
  void testRecordType() {
    var recordType = new AggregateSymbol("Point", symbolTable);
    recordType.setGenus(SymbolGenus.RECORD);
    recordType.setSourceToken(new Ek9Token("geometry.ek9", 5));

    // Add fields
    recordType.define(new VariableSymbol("x", Optional.of(ek9Float)));
    recordType.define(new VariableSymbol("y", Optional.of(ek9Float)));

    var hover = formatter.apply(Optional.of(recordType));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**Point**"), "Should contain record name");

    System.out.println("=== Record Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testTraitType() {
    var traitType = new AggregateSymbol("Comparable", symbolTable);
    traitType.setGenus(SymbolGenus.CLASS_TRAIT);
    traitType.setSourceToken(new Ek9Token("traits.ek9", 1));

    var hover = formatter.apply(Optional.of(traitType));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**Comparable**"), "Should contain trait name");

    System.out.println("=== Trait Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testVariableWithUnknownType() {
    // Variable without type set - simulates typo in type name like "Strng" instead of "String"
    var variable = new VariableSymbol("untyped");
    variable.setSourceToken(new Ek9Token("test.ek9", 1));

    var hover = formatter.apply(Optional.of(variable));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**untyped**"), "Should contain variable name");
    assertTrue(content.contains("`unknown`"), "Should show unknown type");

    System.out.println("=== Untyped Variable Hover ===");
    System.out.println(content);
  }

  @Test
  void testFunctionWithUnresolvedParameterType() {
    // Function where parameter type couldn't be resolved (e.g., typo like "Integr")
    var function = new FunctionSymbol("processValue", symbolTable);
    function.setSourceToken(new Ek9Token("process.ek9", 10));

    // Parameter with no type - simulates unresolved type reference
    var param = new VariableSymbol("value");
    param.setIncomingParameter(true);
    function.define(param);

    // Return type is resolved
    var returnVar = new VariableSymbol("result", Optional.of(ek9Boolean));
    returnVar.setReturningParameter(true);
    function.setReturningSymbol(returnVar);

    var hover = formatter.apply(Optional.of(function));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("processValue"), "Should contain function name");
    assertTrue(content.contains("value as ?"), "Should show ? for unresolved param type");
    assertTrue(content.contains("*Returns*: `Boolean`"), "Should show resolved return type");

    System.out.println("=== Function with Unresolved Param Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testFunctionWithUnresolvedReturnType() {
    // Function where return type couldn't be resolved
    var function = new FunctionSymbol("getData", symbolTable);
    function.setSourceToken(new Ek9Token("data.ek9", 15));

    // Parameter is resolved
    var param = new VariableSymbol("id", Optional.of(ek9Integer));
    param.setIncomingParameter(true);
    function.define(param);

    // Return type is NOT resolved - simulates typo like "Strng" or missing import
    var returnVar = new VariableSymbol("data");
    returnVar.setReturningParameter(true);
    function.setReturningSymbol(returnVar);

    var hover = formatter.apply(Optional.of(function));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("getData"), "Should contain function name");
    assertTrue(content.contains("id as Integer"), "Should show resolved param type");
    // Return type should not appear or show as unknown since returnVar has no type

    System.out.println("=== Function with Unresolved Return Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testMethodWithAllUnresolvedTypes() {
    // Method where all types are unresolved - early development stage
    var aggregate = new AggregateSymbol("Draft", symbolTable);

    var method = new MethodSymbol("process", aggregate);
    method.setSourceToken(new Ek9Token("draft.ek9", 20));

    // Parameter with unresolved type
    var param1 = new VariableSymbol("input");
    param1.setIncomingParameter(true);
    method.define(param1);

    var param2 = new VariableSymbol("config");
    param2.setIncomingParameter(true);
    method.define(param2);

    // Return with unresolved type
    var returnVar = new VariableSymbol("output");
    returnVar.setReturningParameter(true);
    method.setReturningSymbol(returnVar);

    aggregate.define(method);

    var hover = formatter.apply(Optional.of(method));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**process**"), "Should contain method name");
    assertTrue(content.contains("*On*: `Draft`"), "Should show parent aggregate");
    assertTrue(content.contains("input as ?"), "Should show ? for unresolved types");
    assertTrue(content.contains("config as ?"), "Should show ? for unresolved types");

    System.out.println("=== Method with All Unresolved Types Hover ===");
    System.out.println(content);
  }

  @Test
  void testFunctionWithNoParameters() {
    // Function with no parameters but valid return
    var function = new FunctionSymbol("getTimestamp", symbolTable);
    function.setSourceToken(new Ek9Token("time.ek9", 5));

    var returnVar = new VariableSymbol("ts", Optional.of(ek9Date));
    returnVar.setReturningParameter(true);
    function.setReturningSymbol(returnVar);

    var hover = formatter.apply(Optional.of(function));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("getTimestamp"), "Should contain function name");
    assertTrue(content.contains("*Returns*: `Date`"), "Should show return type");
    // Should NOT contain Parameters line when there are none

    System.out.println("=== Function with No Parameters Hover ===");
    System.out.println(content);
  }

  @Test
  void testFunctionWithNoReturn() {
    // Procedure-style function with no return value
    var function = new FunctionSymbol("logMessage", symbolTable);
    function.setSourceToken(new Ek9Token("log.ek9", 8));

    var param = new VariableSymbol("message", Optional.of(ek9String));
    param.setIncomingParameter(true);
    function.define(param);

    // No return symbol set

    var hover = formatter.apply(Optional.of(function));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("logMessage"), "Should contain function name");
    assertTrue(content.contains("message as String"), "Should show parameter");
    // Should NOT contain Returns line when there is no return

    System.out.println("=== Function with No Return Hover ===");
    System.out.println(content);
  }

  @Test
  void testAnySymbol() {
    // Test with the Any type itself
    var hover = formatter.apply(Optional.of(ek9Any));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("Any"), "Should contain Any");

    System.out.println("=== Any Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testSymbolWithoutSourceToken() {
    // Symbol that has no source location (e.g., synthetic or built-in)
    var variable = new VariableSymbol("synthetic", Optional.of(ek9String));
    // No setSourceToken call - simulates built-in or generated symbol

    var hover = formatter.apply(Optional.of(variable));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**synthetic**"), "Should contain variable name");
    assertTrue(content.contains("`String`"), "Should contain type");
    // Should NOT contain "Defined at" since no source token
    assertFalse(content.contains("*Defined at*:"), "Should not show location for synthetic symbols");

    System.out.println("=== Symbol Without Source Token Hover ===");
    System.out.println(content);
  }

  @Test
  void testOperatorMethod() {
    // Test operator method like + or <>
    var aggregate = new AggregateSymbol("Money", symbolTable);

    var operator = new MethodSymbol("+", aggregate);
    operator.setOperator(true);
    operator.setSourceToken(new Ek9Token("money.ek9", 40));

    var param = new VariableSymbol("other", Optional.of(aggregate));
    param.setIncomingParameter(true);
    operator.define(param);

    var returnVar = new VariableSymbol("result", Optional.of(aggregate));
    returnVar.setReturningParameter(true);
    operator.setReturningSymbol(returnVar);

    aggregate.define(operator);

    var hover = formatter.apply(Optional.of(operator));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**+**"), "Should contain operator name");
    assertTrue(content.contains("*On*: `Money`"), "Should show parent aggregate");

    System.out.println("=== Operator Method Hover ===");
    System.out.println(content);
  }

  @Test
  void testEnumerationType() {
    var enumType = new AggregateSymbol("Color", symbolTable);
    enumType.setGenus(SymbolGenus.CLASS_ENUMERATION);
    enumType.setSourceToken(new Ek9Token("colors.ek9", 1));

    var hover = formatter.apply(Optional.of(enumType));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**Color**"), "Should contain enum name");
    assertTrue(content.contains("enumeration"), "Should indicate enumeration type");

    System.out.println("=== Enumeration Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testServiceType() {
    var serviceType = new AggregateSymbol("UserService", symbolTable);
    serviceType.setGenus(SymbolGenus.SERVICE);
    serviceType.setSourceToken(new Ek9Token("services.ek9", 10));

    var hover = formatter.apply(Optional.of(serviceType));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**UserService**"), "Should contain service name");

    System.out.println("=== Service Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testComponentType() {
    var componentType = new AggregateSymbol("DatabaseConnector", symbolTable);
    componentType.setGenus(SymbolGenus.COMPONENT);
    componentType.setSourceToken(new Ek9Token("components.ek9", 15));

    var hover = formatter.apply(Optional.of(componentType));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("**DatabaseConnector**"), "Should contain component name");

    System.out.println("=== Component Type Hover ===");
    System.out.println(content);
  }

  @Test
  void testPureAbstractMethod() {
    // Method that is both pure and abstract
    var aggregate = new AggregateSymbol("Formatter", symbolTable);

    var method = new MethodSymbol("format", aggregate);
    method.setMarkedPure(true);
    method.setMarkedAbstract(true);
    method.setSourceToken(new Ek9Token("formatter.ek9", 5));

    var param = new VariableSymbol("input", Optional.of(ek9String));
    param.setIncomingParameter(true);
    method.define(param);

    var returnVar = new VariableSymbol("output", Optional.of(ek9String));
    returnVar.setReturningParameter(true);
    method.setReturningSymbol(returnVar);

    aggregate.define(method);

    var hover = formatter.apply(Optional.of(method));

    assertNotNull(hover);
    var content = hover.getContents().getRight().getValue();

    assertTrue(content.contains("*Modifier*: pure"), "Should indicate pure");
    assertTrue(content.contains("*Modifier*: abstract"), "Should indicate abstract");

    System.out.println("=== Pure Abstract Method Hover ===");
    System.out.println(content);
  }
}
