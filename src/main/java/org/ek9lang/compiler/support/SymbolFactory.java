package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DEFAULT_AND_TRAIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.directives.Directive;
import org.ek9lang.compiler.directives.DirectiveSpecExtractor;
import org.ek9lang.compiler.directives.DirectiveType;
import org.ek9lang.compiler.directives.DirectivesCompilationPhase;
import org.ek9lang.compiler.directives.DirectivesNextLineNumber;
import org.ek9lang.compiler.directives.ErrorDirective;
import org.ek9lang.compiler.directives.GenusDirective;
import org.ek9lang.compiler.directives.ImplementsDirective;
import org.ek9lang.compiler.directives.NotResolvedDirective;
import org.ek9lang.compiler.directives.ResolvedDirective;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.ConstantSymbol;
import org.ek9lang.compiler.symbols.ExpressionSymbol;
import org.ek9lang.compiler.symbols.ForSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ICanCaptureVariables;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;
import org.ek9lang.compiler.symbols.StreamCallSymbol;
import org.ek9lang.compiler.symbols.StreamPipeLineSymbol;
import org.ek9lang.compiler.symbols.SwitchSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.compiler.symbols.TrySymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.symbols.WhileSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.UniqueIdGenerator;

/**
 * Just a factory for all types of EK9 symbol.
 * Ensures that newly created symbols are initialised correctly.
 */
public class SymbolFactory {

  public static final String UNINITIALISED_AT_DECLARATION = "UNINITIALISED";
  public static final String NO_REFERENCED_RESET = "NO_REFERENCED_RESET";
  public static final String SUBSTITUTED = "SUBSTITUTED";
  public static final String ACCESSED = "ACCESSED";
  public static final String DEFAULTED = "DEFAULTED";
  public static final String EXTERN = "EXTERN";
  public static final String URI_PROTO = "URIPROTO";
  public static final String HTTP_REQUEST = "REQUEST";
  public static final String HTTP_PATH = "PATH";
  public static final String HTTP_HEADER = "HEADER";
  public static final String HTTP_QUERY = "QUERY";
  public static final String HTTP_VERB = "HTTPVERB";
  public static final String HTTP_ACCESS = "HTTPACCESS";
  public static final String HTTP_SOURCE = "HTTPSOURCE";

  //Note that uniq and sort might be able to consume anything if they aren't given a function.
  //But there are limits, there would be to be hashCode, comparators, plus in some cases types being consumed
  //must be functions (call and async).
  private static final Set<String> streamPartCanConsumeAnything = Set.of("flatten",
      "call", "async", "skipping", "head", "tail");

  private static final Set<String> streamPartProducerAndConsumerTypeSame = Set.of(
      "skipping", "head", "tail", "filter", "select", "sort", "group",
      "join", "uniq", "tee");

  private static final Map<String, String> operatorToHttpVerbMap = Map.of(
      "+", "POST",
      "+=", "POST",
      "-", "DELETE",
      "-=", "DELETE:",
      ":^:", "PUT",
      ":~:", "PATCH",
      "?", "HEAD"
  );

  private static final Predicate<String> canConsumeAnything = streamPartCanConsumeAnything::contains;

  private static final Predicate<String> isProducerAndConsumerSameType =
      streamPartProducerAndConsumerTypeSame::contains;

  private static final Predicate<String> isProducerDerivedFromConsumerType = "flatten"::equals;

  private static final Predicate<String> isASinkInNature = "tee"::equals;

  private static final Predicate<String> isFunctionRequired
      = operation -> "call".equals(operation) || "async".equals(operation);

  private final ParsedModule parsedModule;

  private final DirectivesNextLineNumber directivesNextLineNumber = new DirectivesNextLineNumber();

  private final DirectivesCompilationPhase directivesCompilationPhase = new DirectivesCompilationPhase();

  /**
   * To be used to create parameterised versions of generic types and functions.
   * Will replace parameterizedTypeCreator and parameterizedFunctionCreator.
   */
  private final ParameterizedSymbolCreator parameterizedSymbolCreator =
      new ParameterizedSymbolCreator(new InternalNameFor());

  /**
   * Used for low level additions of methods to aggregates.
   */
  private final AggregateFactory aggregateFactory;

  private final Consumer<Object> checkContextNotNull = ctx -> AssertValue.checkNotNull("CTX cannot be null", ctx);

  private final CheckForInvalidServiceDefinition checkForInvalidServiceDefinition;

  private final CheckForInvalidServiceOperator checkForInvalidServiceOperator;

  private final CheckAppropriateWebVariable checkAppropriateWebVariable;

  private final DirectiveSpecExtractor directiveSpecExtractor = new DirectiveSpecExtractor();

  private final CheckAndPopulateOperator checkAndPopulateOperator;

  /**
   * Create a new symbol factory for use with the parsedModule.
   */
  public SymbolFactory(final ParsedModule parsedModule) {

    AssertValue.checkNotNull("Parsed Module cannot be null", parsedModule);

    this.parsedModule = parsedModule;
    this.aggregateFactory = new AggregateFactory(parsedModule.getEk9Types());
    this.checkForInvalidServiceDefinition =
        new CheckForInvalidServiceDefinition(parsedModule.getSource().getErrorListener());
    this.checkForInvalidServiceOperator =
        new CheckForInvalidServiceOperator(parsedModule.getSource().getErrorListener());
    this.checkAppropriateWebVariable = new CheckAppropriateWebVariable(parsedModule.getSource().getErrorListener());
    this.checkAndPopulateOperator =
        new CheckAndPopulateOperator(aggregateFactory, parsedModule.getSource().getErrorListener());

  }

  public AggregateFactory getAggregateFactory() {

    return aggregateFactory;
  }

  /**
   * Create a new directive to be used inside the compiler.
   */
  public Directive newDirective(final EK9Parser.DirectiveContext ctx) {

    final var nameOfDirective = ctx.identifier().getText();
    try {
      final var typeOfDirective = DirectiveType.valueOf(nameOfDirective);
      return switch (typeOfDirective) {
        case Error -> newErrorDirective(ctx);
        case Resolved -> newResolutionDirective(ctx, true);
        case Implements -> newImplementsDirective(ctx);
        case NotResolved -> newResolutionDirective(ctx, false);
        case Genus -> newGenusDirective(ctx);
        case Symbols, Compiler, Instrument ->
            throw new IllegalArgumentException("Unsupported '@" + nameOfDirective + "':");
      };
    } catch (IllegalArgumentException ex) {
      var errorListener = parsedModule.getSource().getErrorListener();
      errorListener.semanticError(ctx.start, ex.getMessage()
          + ",", ErrorListener.SemanticClassification.UNKNOWN_DIRECTIVE);
    }

    return null;
  }

  private Directive newErrorDirective(final EK9Parser.DirectiveContext ctx) {

    if (ctx.directivePart().size() != 2) {
      throw new IllegalArgumentException("Expecting, compilerPhase: errorClassification");
    }

    try {
      final var applyToLine = directivesNextLineNumber.apply(ctx);
      final var compilerPhase = directivesCompilationPhase.apply(ctx);
      final var errorClassification = ErrorListener.SemanticClassification.valueOf(ctx.directivePart(1).getText());

      return new ErrorDirective(new Ek9Token(ctx.start), compilerPhase, errorClassification, applyToLine);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Expecting one of: " + Arrays.toString(
          ErrorListener.SemanticClassification.values()));
    }

  }

  private Directive newImplementsDirective(final EK9Parser.DirectiveContext ctx) {

    final var spec = directiveSpecExtractor.apply(ctx);

    return new ImplementsDirective(spec);
  }

  private Directive newResolutionDirective(final EK9Parser.DirectiveContext ctx, final boolean resolve) {

    final var spec = directiveSpecExtractor.apply(ctx);

    if (resolve) {
      return new ResolvedDirective(spec);
    }

    return new NotResolvedDirective(spec);
  }

  private Directive newGenusDirective(final EK9Parser.DirectiveContext ctx) {

    final var spec = directiveSpecExtractor.apply(ctx);

    return new GenusDirective(spec);
  }

  /**
   * Create a new EK9 package aggregate.
   */
  public AggregateSymbol newPackage(final EK9Parser.PackageBlockContext ctx) {

    checkContextNotNull.accept(ctx);
    final var pack = new AggregateSymbol("Package", parsedModule.getModuleScope());

    configureAggregate(pack, new Ek9Token(ctx.start));
    //Also add in a default constructor.
    pack.setGenus(ISymbol.SymbolGenus.META_DATA);
    aggregateFactory.addConstructor(pack);

    return pack;
  }

  /**
   * Create a new aggregate that represents an EK9 program.
   */
  public AggregateSymbol newProgram(final EK9Parser.MethodDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate program name", ctx.identifier());

    final var programName = ctx.identifier().getText();
    final var program = new AggregateSymbol(programName, parsedModule.getModuleScope());

    configureAggregate(program, new Ek9Token(ctx.identifier().start));

    //It is not necessary to wire in an application configuration to be used.
    //But if present then it will be named here. In the resolve phase we will check it.
    //For now just record it.
    if (ctx.APPLICATION() != null) {
      program.putSquirrelledData("APPLICATION", ctx.identifierReference().getText());
    }

    program.setGenus(ISymbol.SymbolGenus.PROGRAM);

    return program;
  }

  /**
   * Create a new aggregate that represents an EK9 class.
   * A bit tricky when it comes to parameterised (generic/template classes).
   */
  public AggregateWithTraitsSymbol newClass(final EK9Parser.ClassDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate class name", ctx.Identifier());

    final var moduleScope = parsedModule.getModuleScope();
    final var className = ctx.Identifier().getText();
    final var newClass = newAggregateWithTraitsSymbol(className, new Ek9Token(ctx.start));

    newClass.setOpenForExtension(ctx.ABSTRACT() != null || ctx.OPEN() != null);
    newClass.setGenus(ISymbol.SymbolGenus.CLASS);
    newClass.setMarkedAbstract(ctx.ABSTRACT() != null);

    final var parameterisedSymbols = createAndRegisterParameterisedSymbols(ctx.parameterisedParams(), moduleScope);

    if (!parameterisedSymbols.isEmpty()) {
      //Now need to register against the class we are creating
      parameterisedSymbols.forEach(newClass::addTypeParameterOrArgument);
    }

    return newClass;
  }

  /**
   * Create a new aggregate that represents an EK9 component.
   */
  public AggregateWithTraitsSymbol newComponent(final EK9Parser.ComponentDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate component name", ctx.Identifier());

    final var componentName = ctx.Identifier().getText();
    final var component = newAggregateWithTraitsSymbol(componentName, new Ek9Token(ctx.start));

    component.setOpenForExtension(ctx.ABSTRACT() != null || ctx.OPEN() != null);
    component.setGenus(ISymbol.SymbolGenus.COMPONENT);
    component.setMarkedAbstract(ctx.ABSTRACT() != null);

    return component;
  }

  /**
   * Create a new aggregate that represents an EK9 trait.
   */
  public AggregateWithTraitsSymbol newTrait(final EK9Parser.TraitDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate trait name", ctx.Identifier());

    final var traitName = ctx.Identifier().getText();
    final var trait = newAggregateWithTraitsSymbol(traitName, new Ek9Token(ctx.start));

    configureAggregate(trait, new Ek9Token(ctx.start));
    trait.setGenus(ISymbol.SymbolGenus.CLASS_TRAIT);

    //All traits are designed to be open to extending and use/override.
    //Even though trait can have abstract and open - this is just for syntax consistency
    //So a developer can write it in the same way as a class or component - but they are all open and abstract.
    trait.setOpenForExtension(true);
    trait.setMarkedAbstract(true);

    return trait;
  }

  /**
   * Create a new aggregate that represents an EK9 record.
   */
  public AggregateSymbol newRecord(final EK9Parser.RecordDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate record name", ctx.Identifier());

    final var recordName = ctx.Identifier().getText();
    final var newRecord = new AggregateSymbol(recordName, parsedModule.getModuleScope());

    configureAggregate(newRecord, new Ek9Token(ctx.start));
    newRecord.setOpenForExtension(ctx.ABSTRACT() != null || ctx.OPEN() != null);
    newRecord.setGenus(ISymbol.SymbolGenus.RECORD);
    newRecord.setMarkedAbstract(ctx.ABSTRACT() != null);

    return newRecord;
  }

  /**
   * Create a new function symbol that represents an EK9 function.
   */
  public FunctionSymbol newFunction(final EK9Parser.FunctionDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate function name", ctx.Identifier());

    final var moduleScope = parsedModule.getModuleScope();
    final var functionName = ctx.Identifier().getText();
    final var newFunction = new FunctionSymbol(functionName, moduleScope);

    newFunction.setModuleScope(parsedModule.getModuleScope());
    newFunction.setOpenForExtension(ctx.ABSTRACT() != null || ctx.OPEN() != null);
    newFunction.setMarkedAbstract(ctx.ABSTRACT() != null);
    //More like a library - so we mark as referenced.
    newFunction.setReferenced(true);

    final var start = new Ek9Token(ctx.start);
    newFunction.setInitialisedBy(start);
    configureSymbol(newFunction, start);

    //A function can be both pure and abstract - in this case it is establishing a 'contract' that the
    //implementation must also be pure!
    //This is so that uses of abstract concepts can be used to ensure that there are no side effects.
    if (ctx.PURE() != null) {
      newFunction.setMarkedPure(true);
    }

    //While it maybe a function we need to know if it is abstract or not
    if (ctx.ABSTRACT() == null) {
      newFunction.setGenus(ISymbol.SymbolGenus.FUNCTION);
    } else {
      newFunction.setGenus(ISymbol.SymbolGenus.FUNCTION_TRAIT);
    }

    final var parameterisedSymbols = createAndRegisterParameterisedSymbols(ctx.parameterisedParams(), moduleScope);
    if (!parameterisedSymbols.isEmpty()) {
      //Now need to register against the class we are creating
      parameterisedSymbols.forEach(newFunction::addTypeParameterOrArgument);
    }

    return newFunction;
  }

  /**
   * Create a new aggregate that represents EK9 text construct.
   * Note that this also creates a common base for all text with the same name.
   * It ensures that all methods are added to that common base.
   * Then we can check if all those methods on the common base are also defined in each and every concrete language
   * text aggregate.
   */
  public AggregateSymbol newText(final EK9Parser.TextDeclarationContext ctx, final String forLanguage) {

    //an error will have been created for the developer as language does not conform.
    if (forLanguage != null) {
      var baseName = ctx.Identifier().getText();
      Optional<ISymbol> base = parsedModule.getModuleScope().resolve(new TypeSymbolSearch(baseName));

      if (base.isEmpty()) {

        final var textBase = new AggregateSymbol(baseName, parsedModule.getModuleScope());
        configureAggregate(textBase, new Ek9Token(ctx.start));
        textBase.setGenus(ISymbol.SymbolGenus.TEXT);

        parsedModule.getModuleScope().define(textBase);

        final var constructor = aggregateFactory
            .addConstructor(textBase, aggregateFactory.resolveString(parsedModule.getModuleScope()));
        //Text can never mutate anything as construction.
        constructor.setMarkedPure(true);
        base = Optional.of(textBase);
      }

      final var textName = baseName + "_" + forLanguage;
      final var text = new AggregateSymbol(textName, parsedModule.getModuleScope());

      configureAggregate(text, new Ek9Token(ctx.start));
      text.setGenus(ISymbol.SymbolGenus.TEXT);
      //Now ensure it is set up as the 'super'.
      text.setSuperAggregate((IAggregateSymbol) base.get());
      //Store both the language this is for and the base name, this will be useful later
      text.putSquirrelledData("LANG", forLanguage);
      text.putSquirrelledData("BASE_NAME", baseName);
      return text;
    }

    //return a placeholder so error handling can continue
    return new AggregateSymbol(ctx.Identifier().getText(), parsedModule.getModuleScope());
  }

  /**
   * Create a new aggregate that represents an EK9 text body - this is represented by a method.
   */
  public MethodSymbol newTextBody(final EK9Parser.TextBodyDeclarationContext ctx, final IScope scope) {

    final var methodName = ctx.Identifier().getText();

    return makeTextBodyMethod(methodName, new Ek9Token(ctx.start), scope);
  }

  /**
   * Ensures that any method in a specific text block is always added to the base text for that component.
   */
  public void ensureTextBodyIsInSuper(final MethodSymbol textMethodSymbol) {

    final var scope = textMethodSymbol.getParentScope();

    if (scope instanceof IAggregateSymbol textDeclaration
        && textDeclaration.getSuperAggregate().isPresent()) {

      final var textBase = textDeclaration.getSuperAggregate().get();
      final var results = textBase.resolveMatchingMethodsInThisScopeOnly(new MethodSymbolSearch(textMethodSymbol),
          new MethodSymbolSearchResult());

      if (results.isEmpty()) {
        final var textBaseMethod = textMethodSymbol.clone(textBase);
        textBaseMethod.setOverride(false);
        textBase.define(textMethodSymbol);
      }
    } else {
      throw new CompilerException("Only expecting text body to be using within a textDeclaration scope with a super");
    }

  }

  private MethodSymbol makeTextBodyMethod(final String methodName,
                                          final IToken token,
                                          final IScope scope) {

    final var method = new MethodSymbol(methodName, scope);

    configureSymbol(method, token);
    method.setOverride(true);
    method.setMarkedAbstract(false);
    method.setMarkedPure(true);
    method.setOperator(false);

    //Always returns a String - no need or ability to declare it.
    method.setType(aggregateFactory.resolveString(scope));

    return method;
  }

  /**
   * Create a new aggregate that represents an EK9 service.
   */
  public AggregateSymbol newService(final EK9Parser.ServiceDeclarationContext ctx) {

    final var serviceName = ctx.Identifier().getText();
    final var service = new AggregateSymbol(serviceName, parsedModule.getModuleScope());
    final var uri = ctx.Uriproto().getText();

    configureAggregate(service, new Ek9Token(ctx.start));
    service.setGenus(ISymbol.SymbolGenus.SERVICE);
    service.putSquirrelledData("HTTPURI", uri);
    checkForInvalidServiceDefinition.accept(service);

    return service;
  }

  /**
   * Create a new aggregate that represents an EK9 service operation - a specialised method.
   */
  public ServiceOperationSymbol newServiceOperation(final EK9Parser.ServiceOperationDeclarationContext ctx,
                                                    final IScope scope) {

    final var operator = ctx.operator() != null;
    final var methodName = operator ? ctx.operator().getText() : ctx.identifier().getText();
    final var httpVerb = ctx.httpVerb() != null ? ctx.httpVerb().getText() : "GET";
    final var serviceOperation = new ServiceOperationSymbol(methodName, scope);

    configureSymbol(serviceOperation, new Ek9Token(ctx.start));
    serviceOperation.setOverride(false);
    serviceOperation.setMarkedAbstract(false);
    serviceOperation.setMarkedPure(false);
    serviceOperation.setOperator(operator);
    serviceOperation.putSquirrelledData(URI_PROTO, ctx.Uriproto().getText());
    serviceOperation.putSquirrelledData(HTTP_VERB, httpVerb);

    if (operator) {
      //Check operator used is valid.
      checkForInvalidServiceOperator.accept(serviceOperation);
      serviceOperation.putSquirrelledData(HTTP_VERB,
          operatorToHttpVerbMap.getOrDefault(serviceOperation.getName(), ""));
    }

    return serviceOperation;
  }

  /**
   * Create a new aggregate that represents an EK9 application.
   */
  public AggregateSymbol newApplication(final EK9Parser.ApplicationDeclarationContext ctx) {

    final var applicationName = ctx.Identifier().getText();
    final var application = new AggregateSymbol(applicationName, parsedModule.getModuleScope());

    configureAggregate(application, new Ek9Token(ctx.start));
    application.setGenus(ISymbol.SymbolGenus.GENERAL_APPLICATION);

    return application;
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic class.
   */
  public AggregateWithTraitsSymbol newDynamicClass(final IScopedSymbol enclosingMainTypeOrFunction,
                                                   final EK9Parser.DynamicClassDeclarationContext ctx) {

    //Name is optional - if not present then generate a dynamic value.
    final var dynamicClassName = ctx.Identifier() != null
        ? ctx.Identifier().getText()
        : "_Class_" + UniqueIdGenerator.getNewUniqueId();

    final var rtn = newAggregateWithTraitsSymbol(dynamicClassName, new Ek9Token(ctx.start));
    rtn.setOuterMostTypeOrFunction(enclosingMainTypeOrFunction);
    rtn.setScopeType(IScope.ScopeType.DYNAMIC_BLOCK);
    rtn.setReferenced(true);

    return rtn;
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic function.
   */
  public FunctionSymbol newDynamicFunction(final IScopedSymbol enclosingMainTypeOrFunction,
                                           final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    //As above need to consider how S, T etc would be resolved in later phases.
    final var functionName = "_Function_" + UniqueIdGenerator.getNewUniqueId();
    final var newFunction = new FunctionSymbol(functionName, parsedModule.getModuleScope());

    newFunction.setOuterMostTypeOrFunction(enclosingMainTypeOrFunction);
    configureSymbol(newFunction, new Ek9Token(ctx.start));
    newFunction.setModuleScope(parsedModule.getModuleScope());
    newFunction.setGenus(ISymbol.SymbolGenus.FUNCTION);
    newFunction.setScopeType(IScope.ScopeType.DYNAMIC_BLOCK);
    newFunction.setMarkedPure(ctx.PURE() != null);
    newFunction.setReferenced(true);

    return newFunction;
  }

  public PossibleGenericSymbol newParameterisedSymbol(final PossibleGenericSymbol genericType,
                                                      final List<ISymbol> typeArguments) {

    return parameterizedSymbolCreator.apply(genericType, typeArguments);
  }

  /**
   * Create a new local scope just for variables to be defined/captured in for dynamic classes/functions.
   */
  public CaptureScope newDynamicVariableCapture(final ICanCaptureVariables scope, final IScope enclosingBlockScope) {

    //Now we have to jump back the scope stack to find the main enclosing block.
    //The capture scope will then use this conditionally to resolve items to capture on enter.
    final var captureScope = new CaptureScope(enclosingBlockScope);
    //Now the dynamic class/function needs to know about this additional scope it may have to resolve variables in.
    scope.setCapturedVariables(captureScope);

    //Need to check if the naming is valid.
    return captureScope;
  }

  /**
   * Create a new aggregate that represents an EK9 operator, uses a method for this.
   */
  public MethodSymbol newOperator(final EK9Parser.OperatorDeclarationContext ctx, final IAggregateSymbol aggregate) {

    final var operator = checkAndPopulateOperator.apply(ctx, aggregate);
    //Might be null if not a valid use of operator.
    if (operator != null) {
      final var startToken = new Ek9Token(ctx.operator().start);
      final var methodInitializer = getDefaultOperatorSymbolInitializer(getDefaultOperatorInitializer(startToken));
      methodInitializer.accept(operator);
    }

    return operator;
  }

  /**
   * A bit of a beast this method.
   * Check if operator is not present if so add one in.
   * But as it is used in very early phase of compilation not all types will be known.
   * So here this method just uses the raw name of the method.
   */
  public boolean addMissingDefaultOperators(final EK9Parser.DefaultOperatorContext ctx,
                                            final IAggregateSymbol aggregate) {

    final var startToken = new Ek9Token(ctx.DEFAULT().getSymbol());

    if (aggregate.getGenus().equals(ISymbol.SymbolGenus.CLASS_TRAIT)) {
      final var msg = "wrt to type: '" + aggregate.getFriendlyName() + "':";
      final var errorListener = parsedModule.getSource().getErrorListener();
      errorListener.semanticError(startToken, msg, DEFAULT_AND_TRAIT);

      return false;
    }

    final var existingMethodNames = aggregate.getAllNonAbstractMethods()
        .stream()
        .map(ISymbol::getName)
        .toList();

    final var methodInitializer = getDefaultOperatorSymbolInitializer(getDefaultOperatorInitializer(startToken));

    for (MethodSymbol operator : aggregateFactory.getAllPossibleDefaultOperators(aggregate)) {
      if (!existingMethodNames.contains(operator.getName())) {
        methodInitializer.accept(operator);
        //Now we can add that operator in.
        aggregate.define(operator);
      }
    }

    return true;
  }

  /**
   * Create a new method that represents an EK9 class/component method.
   */
  public MethodSymbol newMethod(final EK9Parser.MethodDeclarationContext ctx, final IScopedSymbol scopedSymbol) {

    //So now we should have an aggregate we are adding this method into.
    final var methodName = ctx.identifier().getText();

    return newMethod(ctx, methodName, scopedSymbol);
  }

  /**
   * Create a new method with a specific name.
   */
  public MethodSymbol newMethod(final EK9Parser.MethodDeclarationContext ctx,
                                final String methodName,
                                final IScopedSymbol scopedSymbol) {

    final var method = new MethodSymbol(methodName, scopedSymbol);

    configureSymbol(method, new Ek9Token(ctx.identifier().start));
    method.setOverride(ctx.OVERRIDE() != null);
    method.setMarkedAbstract(ctx.ABSTRACT() != null);
    method.setMarkedPure(ctx.PURE() != null);
    method.setMarkedAsDispatcher(ctx.DISPATCHER() != null);

    if (ctx.DEFAULT() != null) {
      method.putSquirrelledData(DEFAULTED, "TRUE");
    }

    //General context free logic goes here.
    //but anything that depends on if this method is used in as a program, trait method, class method
    //etc. goes in the 'exit{Construct} iterate over the methods operators and do the validation in that
    //context - i.e. where it is relevant.
    if (ctx.accessModifier() != null) {
      method.setAccessModifier(ctx.accessModifier().getText());
    }

    if (method.getName().equals(scopedSymbol.getScopeName())) {
      //looks like a constructor
      method.setConstructor(true);
      method.setType(scopedSymbol);
    } else {
      //Set this as default unless we have a returning section
      method.setType(aggregateFactory.resolveVoid(scopedSymbol));
    }

    return method;
  }

  /**
   * Create a new aggregate that represents an EK9 type, constrained or enumeration.
   */
  public AggregateSymbol newType(final EK9Parser.TypeDeclarationContext ctx) {

    final var newTypeName = ctx.Identifier().getText();
    final var aggregateSymbol = new AggregateSymbol(newTypeName, parsedModule.getModuleScope());

    configureAggregate(aggregateSymbol, new Ek9Token(ctx.start));

    if (ctx.typeDef() != null) {
      aggregateSymbol.setGenus(ISymbol.SymbolGenus.CLASS_CONSTRAINED);
    } else if (ctx.enumerationDeclaration() != null) {
      aggregateSymbol.setGenus(ISymbol.SymbolGenus.CLASS_ENUMERATION);
      aggregateFactory.addEnumerationMethods(aggregateSymbol);
    }

    return aggregateSymbol;
  }

  /**
   * Populates the enumeration with each of the values supplied in the identifiers.
   * This does check for duplicates and will raise errors if there are any.
   */
  public void populateEnumeration(final AggregateSymbol enumerationSymbol, final List<TerminalNode> identifiers) {

    //Quick way to ensure uniqueness, could have used enumerationSymbol to check.
    //But I want to search irrespective of case and '_' and error on stuff that is similar.
    final var checkValues = new HashMap<String, Token>();

    identifiers.forEach(identifier -> {
      //For checking duplicates we uppercase and remove '_', developer may use mixed case in enumerations.
      //But same/similar word in different cases is highly likely to cause issues or be an error (opinion).
      final var enumValue = identifier.getText().toUpperCase().replace("_", "");
      final var existing = checkValues.get(enumValue);

      if (existing == null) {
        checkValues.put(enumValue, identifier.getSymbol());
        addEnumeratedValue(identifier, enumerationSymbol);
      } else {
        new InvalidEnumeratedValue(parsedModule.getSource().getErrorListener())
            .accept(new Ek9Token(identifier.getSymbol()), existing);
      }
    });

  }

  private void addEnumeratedValue(final TerminalNode ctx, final AggregateSymbol inEnumeration) {

    final var symbol = new ConstantSymbol(ctx.getText());
    final var startToken = new Ek9Token(ctx.getSymbol());

    configureSymbol(symbol, startToken);
    symbol.setNotMutable();

    //The nature of an enumeration is to define possible values
    //These do not have to be referenced to be valuable. So mark referenced.
    symbol.setNullAllowed(false);
    symbol.setInitialisedBy(startToken);
    symbol.setReferenced(true);
    symbol.setType(inEnumeration);
    inEnumeration.define(symbol);

  }

  /**
   * Create a new symbol that represents an EK9 concept of a stream pipeline.
   */
  public StreamPipeLineSymbol newStream(final ParserRuleContext ctx) {

    final var pipeLine = new StreamPipeLineSymbol("stream");

    configureSymbol(pipeLine, new Ek9Token(ctx.start));
    pipeLine.setReferenced(true);
    pipeLine.setNotMutable();

    return pipeLine;
  }

  /**
   * Create a new symbol that represents an EK9 'cat' part of a stream pipeline.
   */
  public StreamCallSymbol newStreamCat(final EK9Parser.StreamCatContext ctx, final IScope scope) {

    final var call = new StreamCallSymbol("cat", scope);
    configureStreamCallSymbol(call, new Ek9Token(ctx.start));

    return call;
  }

  /**
   * Create a new symbol that represents an EK9 'for' part of a stream pipeline.
   */
  public StreamCallSymbol newStreamFor(final EK9Parser.StreamForContext ctx, final IScope scope) {

    final var call = new StreamCallSymbol("for", scope);
    configureStreamCallSymbol(call, new Ek9Token(ctx.start));

    return call;
  }

  /**
   * Create a new symbol that represents an EK9 stream function part of a stream pipeline.
   */
  public StreamCallSymbol newStreamPart(final EK9Parser.StreamPartContext ctx, final IScope scope) {

    final var operation = ctx.op.getText();
    final var call = new StreamCallSymbol(operation, scope);

    configureStreamCallSymbol(call, new Ek9Token(ctx.start));

    //It is necessary to correctly configure the stream part for later processing.
    //This enables type inference and also other logic checks.
    //May need to revisit this once addressing later phases.
    call.setCapableOfConsumingAnything(canConsumeAnything.test(operation));
    call.setProducerSymbolTypeSameAsConsumerSymbolType(isProducerAndConsumerSameType.test(operation));
    call.setSinkInNature(isASinkInNature.test(operation));
    call.setProducesTypeMustBeAFunction(isFunctionRequired.test(operation));
    call.setDerivesProducesTypeFromConsumesType(isProducerDerivedFromConsumerType.test(operation));

    return call;
  }

  /**
   * Create a new symbol that represents an EK9 terminal part of a stream pipeline.
   */
  public StreamCallSymbol newStreamTermination(final ParserRuleContext ctx,
                                               final String operation,
                                               final IScope scope) {

    final var call = new StreamCallSymbol(operation, scope);
    configureStreamCallSymbol(call, new Ek9Token(ctx.start));
    call.setSinkInNature(true);

    return call;
  }

  private void configureStreamCallSymbol(final StreamCallSymbol call, final IToken token) {

    configureSymbol(call, token);
    call.setReferenced(true);
    call.setNotMutable();

  }

  /**
   * Just for general symbols, like references.
   */
  public Symbol newGeneralSymbol(final IToken token, final String name) {

    final var symbol = new Symbol(name);
    configureSymbol(symbol, token);

    return symbol;
  }

  /**
   * Create a new expression symbol place-holder.
   * Really just enables the line of code to be captured and the type that the expression returns.
   */
  public ExpressionSymbol newExpressionSymbol(final IToken token, final String name) {

    return newExpressionSymbol(token, name, Optional.empty());
  }

  /**
   * Create a new expression symbol place-holder.
   * Really just enables the line of code to be captured and the type that the expression returns.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public ExpressionSymbol newExpressionSymbol(final IToken token, final String name, final Optional<ISymbol> ofType) {

    final var expressionSymbol = new ExpressionSymbol(name);
    configureSymbol(expressionSymbol, token);
    expressionSymbol.setType(ofType);

    return expressionSymbol;
  }

  /**
   * Create a new expression around a symbol.
   */
  public ExpressionSymbol newExpressionSymbol(final ISymbol fromSymbol) {

    return new ExpressionSymbol(fromSymbol);
  }

  /**
   * Symbol to model some type of call to a function, dynamic function, constructor, this, super etc.
   */
  public CallSymbol newCall(final EK9Parser.CallContext ctx, final IScope scope) {

    final var callSymbol = new CallSymbol(ctx.getText(), scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(callSymbol, startToken);
    callSymbol.setInitialisedBy(startToken);

    return callSymbol;
  }

  /**
   * Create a new EK9 List (a generic type).
   */
  public CallSymbol newList(final EK9Parser.ListContext ctx, final IScope scope) {

    final var callSymbol = new CallSymbol("List", scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(callSymbol, startToken);
    callSymbol.setInitialisedBy(startToken);

    return callSymbol;
  }

  /**
   * Create a new EK9 Dictionary - like a Map.
   */
  public CallSymbol newDict(final EK9Parser.DictContext ctx, final IScope scope) {

    final var callSymbol = new CallSymbol("Dict", scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(callSymbol, startToken);
    callSymbol.setInitialisedBy(startToken);

    return callSymbol;
  }

  /**
   * Create a new entry for the Dictionary, basically a tuple.
   */
  public CallSymbol newDictEntry(final EK9Parser.InitValuePairContext ctx, final IScope scope) {

    final var callSymbol = new CallSymbol("DictEntry", scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(callSymbol, startToken);
    callSymbol.setInitialisedBy(startToken);

    return callSymbol;
  }

  /**
   * New call but modelled as an operator if marked as such.
   */
  public CallSymbol newOperationCall(final EK9Parser.OperationCallContext ctx, final IScope scope) {

    final var callName = ctx.operator() != null ? ctx.operator().getText() : ctx.identifier().getText();
    final var callSymbol = new CallSymbol(callName, scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(callSymbol, startToken);
    callSymbol.setOperator(ctx.operator() != null);
    callSymbol.setInitialisedBy(startToken);

    return callSymbol;
  }

  /**
   * Create a new symbol that represents an EK9 'switch' block.
   */
  public SwitchSymbol newSwitch(final EK9Parser.SwitchStatementExpressionContext ctx, final IScope scope) {

    final var switchSymbol = new SwitchSymbol(scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(switchSymbol, startToken);

    return switchSymbol;
  }

  /**
   * Create a new symbol that represents an EK9 'try' block.
   */
  public TrySymbol newTry(final EK9Parser.TryStatementExpressionContext ctx, final IScope scope) {

    final var trySymbol = new TrySymbol(scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(trySymbol, startToken);

    return trySymbol;
  }

  /**
   * Create a new symbol that represents an EK9 'for' loop.
   */
  public ForSymbol newForLoop(final ParserRuleContext ctx, final IScope scope) {

    final var forSymbol = new ForSymbol(scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(forSymbol, startToken);

    return forSymbol;
  }

  /**
   * Create a new while or do/while scoped symbol.
   */
  public WhileSymbol newWhileLoop(final EK9Parser.WhileStatementExpressionContext ctx, final IScope scope) {

    final var whileSymbol = new WhileSymbol(scope, ctx.DO() != null);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(whileSymbol, startToken);

    return whileSymbol;
  }

  /**
   * Create a new aggregate that represents an EK9 loop variable.
   */
  public VariableSymbol newLoopVariable(final EK9Parser.ForLoopContext ctx) {

    checkContextNotNull.accept(ctx);

    return newLoopVariable(ctx.identifier());
  }

  /**
   * Create a new aggregate that represents an EK9 loop variable for a range.
   */
  public VariableSymbol newLoopVariable(final EK9Parser.ForRangeContext ctx) {

    checkContextNotNull.accept(ctx);

    //The second identifier (which may not be present) is the loop step either literal or another identifier.
    return newLoopVariable(ctx.identifier(0));
  }

  private VariableSymbol newLoopVariable(final EK9Parser.IdentifierContext identifier) {

    checkContextNotNull.accept(identifier);

    final var variable = newVariable(identifier, false, false);
    //Make note is a loop variable and EK9 will initialise it, developer cannot mutate it.
    variable.setLoopVariable(true);
    variable.setInitialisedBy(new Ek9Token(identifier.start));
    variable.setNotMutable();

    return variable;
  }

  /**
   * Create a new constant that represents the fixed text part of an interpolated String.
   */
  public ConstantSymbol newInterpolatedStringPart(final EK9Parser.StringPartContext ctx, final IScope scope) {

    //if interpolated string had a " in now needs to be escaped because we will wrapping ""
    //But also if we had and escaped $ i.e. \$ we can turn that back into a dollar now.
    final var literalText = ctx.getChild(0).getText().replace("\"", "\\\"").replace("\\$", "$").replace("\\`", "`");
    final var literal = newLiteral(new Ek9Token(ctx.start), "\"" + literalText + "\"");

    literal.setType(aggregateFactory.resolveString(scope));

    return literal;
  }

  /**
   * Create a new expression that represents the expression part of and interpolated String.
   */
  public ExpressionSymbol newInterpolatedExpressionPart(final EK9Parser.StringPartContext ctx) {

    final var expressionSymbol = new ExpressionSymbol(ctx.getText());

    configureSymbol(expressionSymbol, new Ek9Token(ctx.start));

    return expressionSymbol;
  }

  /**
   * Create a new constant as declared in the constants section.
   */
  public ConstantSymbol newConstant(final EK9Parser.ConstantDeclarationContext ctx) {

    final var start = new Ek9Token(ctx.start);
    final var constant = new ConstantSymbol(ctx.Identifier().getText(), false);

    configureSymbol(constant, start);
    constant.setInitialisedBy(start);
    constant.setNotMutable();

    return constant;
  }

  /**
   * Just a declaration of a variable by itself - i.e. without an assignment.
   */
  public VariableSymbol newVariable(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    //Now this is also used in web services and additional ctx.webVariableCorrelation()
    //Makes sense but only for services.
    checkAppropriateWebVariable.accept(ctx);
    final var variable = newVariable(ctx.identifier(), ctx.QUESTION() != null, ctx.BANG() != null);

    if (ctx.webVariableCorrelation() != null) {
      //We need to squirrel away the information, so it can be both checked and used elsewhere
      variable.putSquirrelledData(HTTP_ACCESS, ctx.webVariableCorrelation().httpAccess().getText());
      if (ctx.webVariableCorrelation().stringLit() != null) {
        //Where will this be pulled from
        variable.putSquirrelledData(HTTP_SOURCE, ctx.webVariableCorrelation().stringLit().getText());
        //Obviously is httpAccess is HEADER then it must be a valid header name.
        //If httpAccess is PATH then it must exist in the PATH on the Method
        //And if QUERY then it will have to be one of the query parameters
        //These things are checked elsewhere - here we are just gathering info.
      }
    }

    return variable;
  }

  /**
   * Create and initialise a new variable symbol.
   * Typically, a variable like name &larr; "Steve", so 'name' is the variable.
   */
  public VariableSymbol newVariable(final EK9Parser.VariableDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);

    return newVariable(ctx.identifier(), ctx.QUESTION() != null, false);
  }

  /**
   * Create new variable typically when looking to create simulated variable.
   */
  public VariableSymbol newVariable(final String name,
                                    final IToken token,
                                    final boolean nullAllowed,
                                    final boolean injectionExpected) {

    AssertValue.checkNotNull("Failed to locate variable name", name);
    final var variable = new VariableSymbol(name);

    configureSymbol(variable, token);
    variable.setNullAllowed(nullAllowed);
    variable.setInjectionExpected(injectionExpected);

    return variable;
  }

  public VariableSymbol newVariable(final EK9Parser.IdentifierContext identifier,
                                    final boolean nullAllowed,
                                    final boolean injectionExpected) {

    AssertValue.checkNotNull("Failed to locate variable name", identifier);

    return newVariable(identifier.getText(), new Ek9Token(identifier.start), nullAllowed, injectionExpected);
  }

  /**
   * Create a new aggregate that represents an EK9 literal value.
   */
  public ConstantSymbol newLiteral(final IToken start, final String name) {

    AssertValue.checkNotNull("Start token cannot be null", start);
    AssertValue.checkNotNull("Name cannot be null", name);

    final var literal = new ConstantSymbol(name, true);

    configureSymbol(literal, start);
    literal.setInitialisedBy(start);
    //You cannot set this to any other value.
    literal.setNotMutable();

    return literal;
  }

  private AggregateWithTraitsSymbol newAggregateWithTraitsSymbol(final String className, final IToken start) {

    final var scope = parsedModule.getModuleScope();
    final var clazz = new AggregateWithTraitsSymbol(className, scope);

    configureAggregate(clazz, start);
    clazz.setGenus(ISymbol.SymbolGenus.CLASS);

    return clazz;
  }

  private void configureAggregate(final AggregateSymbol aggregate, final IToken start) {

    aggregate.setModuleScope(parsedModule.getModuleScope());
    //By their nature they are initialised and do not have to be referenced to be part of a system.
    aggregate.setInitialisedBy(start);
    aggregate.setReferenced(true);
    configureSymbol(aggregate, start);

  }

  private Consumer<MethodSymbol> getDefaultOperatorSymbolInitializer(final Consumer<ISymbol> initialise) {

    return operator -> {
      initialise.accept(operator);
      operator.getCallParameters().forEach(initialise);
      if (operator.isReturningSymbolPresent()) {
        initialise.accept(operator.getReturningSymbol());
      }
    };
  }

  private Consumer<ISymbol> getDefaultOperatorInitializer(final IToken startToken) {

    return symbol -> {
      symbol.setSourceToken(startToken);
      symbol.setInitialisedBy(startToken);
      configureSymbol(symbol, startToken);
    };
  }

  private void configureSymbol(final ISymbol symbol, final IToken start) {

    symbol.setParsedModule(Optional.of(parsedModule));
    symbol.setSourceToken(start);
    if (parsedModule.isExternallyImplemented()) {
      symbol.putSquirrelledData(EXTERN, "TRUE");
    }

  }

  private List<AggregateSymbol> createAndRegisterParameterisedSymbols(final EK9Parser.ParameterisedParamsContext ctx,
                                                                      final IScope scope) {
    final List<AggregateSymbol> rtn = new ArrayList<>();

    if (ctx != null) {
      for (int i = 0; i < ctx.parameterisedDetail().size(); i++) {
        final var detail = ctx.parameterisedDetail(i);
        final var startToken = new Ek9Token(detail.Identifier().getSymbol());

        //Then this is a generic type class we simulate and add the S, T, U whatever into the class scope it is
        //a simulated type we have no real idea what it could be, but it is a template parameter to be replace at
        //cookie cutting time.
        //getText(i) gives us the parameter name we just us 'T' here to denote a generic param

        final var t = aggregateFactory.createGenericT(detail.Identifier().getText(), scope);
        t.setSourceToken(startToken);
        t.setReferenced(true);

        //Now going forward we also have constraints of those type S extends String etc.
        //So after phase 1 - but before phase 5 (resolve) we will need to revisit this to ensure we have
        //applied any super class to these 'T's
        rtn.add(t);
        parsedModule.recordSymbol(detail, t);
      }

      //Now we have simulated S, T, P etc - what about adding additional constructors - we have the default
      //This would mean that we could do new T() and new T(an S) or new S(a T) etc. so just a single param
      //type constructor.
      //Otherwise, it get too hard on ordering.

      //Add other synthetic constructors now we have all the S, T, P etc.
      rtn.forEach(t -> rtn.forEach(s -> aggregateFactory.addConstructor(t, new VariableSymbol("arg", s))));
    }
    return rtn;
  }

}
