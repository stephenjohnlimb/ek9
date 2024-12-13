package org.ek9lang.compiler.bootstrap;

import static org.ek9lang.compiler.support.AggregateFactory.EK9_LANG;
import static org.ek9lang.core.AssertValue.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.common.TypeDefResolver;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.TemplateTypeSymbolSearch;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.junit.jupiter.api.Test;

/**
 * Design just to check that it is possible with just one generic Type and one standard type
 * to be able to create a polymorphic parameterized type.
 * I've struggled with this quite a bit, and now I'm starting to see what's wrong (I think).
 * So this test is designed to ensure that given a Generic Type all the operations on that type
 * are correctly generated on the new type when it is parameterized and importantly that they can be resolved.
 */
class MinimalGenericBootStrapTest {
  //Note need to use this name so that ek9 types can be used in compiler.
  //As not actually loading the basics of EK9 source.
  final Supplier<List<CompilableSource>> sourceSupplier =
      () -> List.of(new CompilableSource(Objects.requireNonNull(getClass().getResource(
          "/examples/bootstrap/org-ek9-lang.ek9")).getPath()));
  private final CompilerReporter reporter = new CompilerReporter(false, false);
  private final Supplier<CompilationPhaseListener> listener
      = () -> compilationEvent -> {
    var source = compilationEvent.source();
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(reporter::report);
    }
  };

  @Test
  void basicMinimalBootStrap() {
    final var underTest = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), reporter);
    final var sharedContext = underTest.get();

    //Precondition before we even start to try and really test anything.
    sharedContext.accept(this::assertMinimalEk9);

    //Now resolve the polymorphic type and attributes
    sharedContext.accept(this::assertListOfStringAttributes);

    //Now check the methods available on this List of String type.
    sharedContext.accept(this::assertListOfStringMethods);

    sharedContext.accept(this::assertListOfStringConstructors);

  }

  private void assertListOfStringConstructors(final CompilableProgram program) {
    var concreteListType = resolveListOfString(program);
    var ek9String = assertCanResolveTypesViaAggregate(concreteListType, "String");

    var nameToResolve = "_List_5A526831B9118422B1770505E4E6E766FB887E59BF3A6211393E89E45ED18237";
    assertEquals(nameToResolve, concreteListType.getName());

    //So we would expect to resolve constructors with this name, with no argument and also with same type.
    var resolvedNoArgConstructor = concreteListType.resolveInThisScopeOnly(new MethodSymbolSearch(nameToResolve));
    assertTrue(resolvedNoArgConstructor.isPresent());
    assertTrue(resolvedNoArgConstructor.get().isMethod());

    var resolvedOneArgConstructor = concreteListType
        .resolveInThisScopeOnly(new MethodSymbolSearch(nameToResolve).addTypeParameter(ek9String));
    assertTrue(resolvedOneArgConstructor.isPresent());
    assertTrue(resolvedOneArgConstructor.get().isMethod());

  }

  private void assertListOfStringAttributes(final CompilableProgram program) {

    var concreteListType = resolveListOfString(program);

    assertTrue(concreteListType.isEk9Core());
    assertFalse(concreteListType.isDevSource());
    assertFalse(concreteListType.isLibSource());

    assertTrue(concreteListType.isPublic());
    assertFalse(concreteListType.isProtected());
    assertFalse(concreteListType.isPrivate());

    //Now do some basic checks on that type

    assertTrue(concreteListType.isInitialised());
    assertNotNull(concreteListType.getSourceToken());
    assertNotNull(concreteListType.getInitialisedBy());

    assertFalse(concreteListType.isReturningParameter());
    assertFalse(concreteListType.isIncomingParameter());

    assertEquals(SymbolGenus.CLASS, concreteListType.getGenus());
    assertEquals(SymbolCategory.TYPE, concreteListType.getCategory());

    assertTrue(concreteListType.isType());
    assertTrue(concreteListType.isParameterisedType());
    assertTrue(concreteListType.getGenericType().isPresent());
    assertEquals(1, concreteListType.getTypeParameterOrArguments().size());
    assertFalse(concreteListType.isPrimitiveType());
    assertFalse(concreteListType.isConceptualTypeParameter());
    assertFalse(concreteListType.isGenericInNature());

    assertFalse(concreteListType.isApplication());
    assertFalse(concreteListType.isFunction());
    assertFalse(concreteListType.isVariable());
    assertFalse(concreteListType.isConstant());
    assertFalse(concreteListType.isDeclaredAsConstant());
    assertFalse(concreteListType.isTemplateType());
    assertFalse(concreteListType.isTemplateFunction());
    assertFalse(concreteListType.isMethod());

    assertFalse(concreteListType.isFromLiteral());

    assertFalse(concreteListType.isPropertyField());
    assertFalse(concreteListType.isLoopVariable());
    assertFalse(concreteListType.isControl());
    assertFalse(concreteListType.isFunction());
    assertFalse(concreteListType.isMarkedAbstract());
    assertFalse(concreteListType.isMutable());
    assertFalse(concreteListType.isInjectable());
    assertFalse(concreteListType.isExtensionOfInjectable());
    assertFalse(concreteListType.isInjectionExpected());
    assertFalse(concreteListType.isMarkedPure());

    //A bit strange I know - but this is how EK9 creates a new specific type from a generic and it is deterministic
    //So no type erasure.
    assertEquals("_List_5A526831B9118422B1770505E4E6E766FB887E59BF3A6211393E89E45ED18237",
        concreteListType.getName());
    assertEquals("List of type T of type String", concreteListType.getFriendlyName());
    assertEquals("org.ek9.lang::_List_5A526831B9118422B1770505E4E6E766FB887E59BF3A6211393E89E45ED18237",
        concreteListType.getFullyQualifiedName());

  }

  private void assertListOfStringMethods(final CompilableProgram program) {
    var concreteListType = resolveListOfString(program);

    //These will be needed later for checking methods.
    var ek9String = assertCanResolveTypesViaAggregate(concreteListType, "String");
    var ek9Boolean = assertCanResolveTypesViaAggregate(concreteListType, "Boolean");
    var ek9Integer = assertCanResolveTypesViaAggregate(concreteListType, "Integer");

    //Now check each of the methods.

    //First the simple no argument methods/operators and the appropriate expected return type
    assertNoArgMethodReturningType("first", concreteListType, ek9String);
    assertNoArgMethodReturningType("last", concreteListType, ek9String);
    assertNoArgMethodReturningType("reverse", concreteListType, concreteListType);
    //TODO fix defect
    //assertNoArgMethodReturningType("~", concreteListType, concreteListType);

    assertNoArgMethodReturningType("#<", concreteListType, ek9String);
    assertNoArgMethodReturningType("#>", concreteListType, ek9String);
    assertNoArgMethodReturningType("$", concreteListType, ek9String);

    assertNoArgMethodReturningType("?", concreteListType, ek9Boolean);
    assertNoArgMethodReturningType("empty", concreteListType, ek9Boolean);

    assertNoArgMethodReturningType("#?", concreteListType, ek9Integer);
    assertNoArgMethodReturningType("length", concreteListType, ek9Integer);

    //Now the single argument methods/operators and their return types
    assertOneArgMethodReturningType("get", concreteListType, ek9Integer, ek9String);

    assertOneArgMethodReturningType("==", concreteListType, concreteListType, ek9Boolean);
    assertOneArgMethodReturningType("<>", concreteListType, concreteListType, ek9Boolean);

    assertOneArgMethodReturningType("+", concreteListType, concreteListType, concreteListType);
    assertOneArgMethodReturningType("+", concreteListType, ek9String, concreteListType);
    assertOneArgMethodReturningType("-", concreteListType, concreteListType, concreteListType);
    assertOneArgMethodReturningType("-", concreteListType, ek9String, concreteListType);

    assertOneArgMethodReturningType("contains", concreteListType, ek9String, ek9Boolean);

    assertOneArgMethodReturningVoid(":~:", concreteListType, concreteListType);
    assertOneArgMethodReturningVoid(":^:", concreteListType, concreteListType);
    assertOneArgMethodReturningVoid(":=:", concreteListType, concreteListType);
    assertOneArgMethodReturningVoid("+=", concreteListType, concreteListType);
    assertOneArgMethodReturningVoid("-=", concreteListType, concreteListType);

    assertOneArgMethodReturningVoid("|", concreteListType, ek9String);
    assertOneArgMethodReturningVoid("+=", concreteListType, ek9String);
    assertOneArgMethodReturningVoid("-=", concreteListType, ek9String);
  }


  private ISymbol assertCanResolveTypesViaAggregate(final IAggregateSymbol aggregate, final String typeName) {

    var resolved = aggregate.resolve(new TypeSymbolSearch(typeName));
    assertTrue(resolved.isPresent());
    var ek9Type = resolved.get();
    assertEquals(typeName, ek9Type.getName());
    return ek9Type;

  }

  private void assertNoArgMethodReturningType(final String methodName,
                                              final AggregateSymbol concreteListType,
                                              final ISymbol ek9ReturnType) {

    var resolved = concreteListType.resolve(new MethodSymbolSearch(methodName));
    assertTrue(resolved.isPresent());
    var resolvedMethod = resolved.get();

    assertMethodCorrect(resolvedMethod, ek9ReturnType);
  }

  private void assertOneArgMethodReturningType(final String methodName,
                                               final AggregateSymbol concreteListType,
                                               final ISymbol ek9ArgType,
                                               final ISymbol ek9ReturnType) {


    var resolved = concreteListType.resolve(new MethodSymbolSearch(methodName).addTypeParameter(ek9ArgType));
    assertTrue(resolved.isPresent());
    var resolvedMethod = resolved.get();

    var method = assertMethodCorrect(resolvedMethod, ek9ReturnType);
    if (method != null) {
      assertTrue(method.isMarkedPure());
    }
  }

  private void assertOneArgMethodReturningVoid(final String methodName,
                                               final AggregateSymbol concreteListType,
                                               final ISymbol ek9ArgType) {
    var resolved = concreteListType.resolve(new MethodSymbolSearch(methodName).addTypeParameter(ek9ArgType));
    assertTrue(resolved.isPresent());

    var ek9Void = assertCanResolveTypesViaAggregate(concreteListType, "Void");
    var resolvedMethod = resolved.get();
    var method = assertMethodCorrect(resolvedMethod, ek9Void);
    if (method != null) {
      assertFalse(method.isMarkedPure());
    }

  }

  private MethodSymbol assertMethodCorrect(final ISymbol resolvedMethod, final ISymbol ek9ReturnType) {
    //We expect it to be a method
    assertTrue(resolvedMethod.isMethod());
    if (resolvedMethod instanceof MethodSymbol method) {
      //Now we can check it is has return value and what type that is.
      if (!method.isReturningSymbolPresent()) {
        System.out.println("Check - the generic to see if that had a returning symbol");
      }
      assertTrue(method.isReturningSymbolPresent());
      assertTrue(method.getReturningSymbol().getType().isPresent());
      assertTrue(ek9ReturnType.isExactSameType(method.getReturningSymbol().getType().get()));
      return method;
    }
    fail("Expecting a method symbol");
    return null;
  }

  private AggregateSymbol resolveListOfString(final CompilableProgram program) {
    var scope = program.getParsedModules(EK9_LANG).get(0).getModuleScope();
    var resolver = new TypeDefResolver(scope);
    var listOfString = resolver.typeDefToSymbol("List of (String)");
    assertTrue(listOfString.isPresent());
    return (AggregateSymbol) listOfString.get();
  }

  /**
   * Asserts that the types and template types defined in the minimal source have been
   * compiled and exist inside the minimal EK9 module scope.
   */
  private void assertMinimalEk9(final CompilableProgram program) {

    var scope = program.getParsedModules(EK9_LANG).get(0).getModuleScope();
    assertNotNull(scope);

    assertTrue(scope.resolve(new TypeSymbolSearch("String")).isPresent());
    assertTrue(scope.resolve(new TypeSymbolSearch("Integer")).isPresent());
    assertTrue(scope.resolve(new TypeSymbolSearch("Boolean")).isPresent());

    //Ensure not just a 'type' but known to be a template (generic) type.
    assertFalse(scope.resolve(new TypeSymbolSearch("List")).isPresent());
    assertTrue(scope.resolve(new TemplateTypeSymbolSearch("List")).isPresent());

  }


}
