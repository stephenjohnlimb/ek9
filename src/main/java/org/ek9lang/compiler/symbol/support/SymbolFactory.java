package org.ek9lang.compiler.symbol.support;

import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_STRING;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_VOID;

import java.util.ArrayList;
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
import org.ek9lang.compiler.errors.InvalidEnumeratedValue;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.rules.CheckAppropriateWebVariable;
import org.ek9lang.compiler.main.rules.CheckForInvalidServiceDefinition;
import org.ek9lang.compiler.main.rules.CheckForInvalidServiceOperator;
import org.ek9lang.compiler.main.rules.CheckSwitch;
import org.ek9lang.compiler.main.rules.CheckTryReturns;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbol.CallSymbol;
import org.ek9lang.compiler.symbol.ConstantSymbol;
import org.ek9lang.compiler.symbol.ExpressionSymbol;
import org.ek9lang.compiler.symbol.ForSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ICanCaptureVariables;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.IScopedSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.LocalScope;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.ParameterisedFunctionSymbol;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;
import org.ek9lang.compiler.symbol.ServiceOperationSymbol;
import org.ek9lang.compiler.symbol.StreamCallSymbol;
import org.ek9lang.compiler.symbol.StreamPipeLineSymbol;
import org.ek9lang.compiler.symbol.SwitchSymbol;
import org.ek9lang.compiler.symbol.Symbol;
import org.ek9lang.compiler.symbol.TrySymbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.utils.UniqueIdGenerator;

/**
 * Just a factory for all types of EK9 symbol.
 * Ensures that newly created symbols are initialised correctly.
 */
public class SymbolFactory {

  public static final String DEFAULTED = "DEFAULTED";
  public static final String HTTPVERB = "HTTPVERB";

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

  /**
   * Used for low level additions of methods to aggregates.
   */
  private final AggregateFactory aggregateFactory = new AggregateFactory();

  private final Consumer<Object> checkContextNotNull = ctx -> AssertValue.checkNotNull("CTX cannot be null", ctx);


  private final CheckSwitch checkSwitch;

  private final CheckTryReturns checkTryReturns;

  private final CheckForInvalidServiceDefinition checkForInvalidServiceDefinition;

  private final CheckForInvalidServiceOperator checkForInvalidServiceOperator;

  private final CheckAppropriateWebVariable checkAppropriateWebVariable;

  /**
   * Create a new symbol factory for use with the parsedModule.
   */
  public SymbolFactory(ParsedModule parsedModule) {
    AssertValue.checkNotNull("Parsed Module cannot be null", parsedModule);
    this.parsedModule = parsedModule;
    checkSwitch = new CheckSwitch(parsedModule.getSource().getErrorListener());
    checkTryReturns = new CheckTryReturns(parsedModule.getSource().getErrorListener());
    checkForInvalidServiceDefinition =
        new CheckForInvalidServiceDefinition(parsedModule.getSource().getErrorListener());
    checkForInvalidServiceOperator = new CheckForInvalidServiceOperator(parsedModule.getSource().getErrorListener());
    checkAppropriateWebVariable = new CheckAppropriateWebVariable(parsedModule.getSource().getErrorListener());
  }

  /**
   * Create a new EK9 package aggregate.
   */
  public AggregateSymbol newPackage(EK9Parser.PackageBlockContext ctx) {
    checkContextNotNull.accept(ctx);
    AggregateSymbol pack = new AggregateSymbol("Package", parsedModule.getModuleScope());
    configureAggregate(pack, ctx.start);
    pack.setGenus(ISymbol.SymbolGenus.META_DATA);
    return pack;
  }

  /**
   * Create a new aggregate that represents an EK9 program.
   */
  public AggregateSymbol newProgram(EK9Parser.MethodDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate program name", ctx.identifier());

    String programName = ctx.identifier().getText();
    AggregateSymbol program = new AggregateSymbol(programName, parsedModule.getModuleScope());
    configureAggregate(program, ctx.start);

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
  public AggregateWithTraitsSymbol newClass(EK9Parser.ClassDeclarationContext ctx) {
    final var moduleScope = parsedModule.getModuleScope();
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate class name", ctx.Identifier());
    String className = ctx.Identifier().getText();
    final var newClass = newAggregateWithTraitsSymbol(className, ctx.start);
    newClass.setOpenForExtension(ctx.ABSTRACT() != null);
    newClass.setGenus(ISymbol.SymbolGenus.CLASS);

    var parameterisedSymbols = createAndRegisterParameterisedSymbols(ctx.parameterisedParams(), moduleScope);
    if (!parameterisedSymbols.isEmpty()) {
      //Now need to register against the class we are creating
      parameterisedSymbols.forEach(newClass::addParameterisedType);
      //It is also important to hold on to the context when it comes to template/generic expansion.
      newClass.setContextForParameterisedType(ctx);
    }
    return newClass;
  }

  /**
   * Create a new aggregate that represents an EK9 component.
   */
  public AggregateWithTraitsSymbol newComponent(EK9Parser.ComponentDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate component name", ctx.Identifier());
    String componentName = ctx.Identifier().getText();
    var component = newAggregateWithTraitsSymbol(componentName, ctx.start);
    component.setOpenForExtension(ctx.ABSTRACT() != null);
    component.setGenus(ISymbol.SymbolGenus.COMPONENT);

    return component;
  }

  /**
   * Create a new aggregate that represents an EK9 trait.
   */
  public AggregateWithTraitsSymbol newTrait(EK9Parser.TraitDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate trait name", ctx.Identifier());

    String traitName = ctx.Identifier().getText();
    AggregateWithTraitsSymbol trait = newAggregateWithTraitsSymbol(traitName, ctx.start);
    configureAggregate(trait, ctx.start);

    trait.setGenus(ISymbol.SymbolGenus.CLASS_TRAIT);
    //All traits are designed to be open to extending and use/override.
    trait.setOpenForExtension(true);

    return trait;
  }

  /**
   * Create a new aggregate that represents an EK9 record.
   */
  public AggregateSymbol newRecord(EK9Parser.RecordDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate record name", ctx.Identifier());
    String recordName = ctx.Identifier().getText();
    AggregateSymbol newRecord = new AggregateSymbol(recordName, parsedModule.getModuleScope());
    configureAggregate(newRecord, ctx.start);
    newRecord.setGenus(ISymbol.SymbolGenus.RECORD);
    return newRecord;
  }

  /**
   * Create a new aggregate that represents an EK9 function.
   */
  public FunctionSymbol newFunction(EK9Parser.FunctionDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    final var moduleScope = parsedModule.getModuleScope();
    AssertValue.checkNotNull("Failed to locate function name", ctx.Identifier());

    String functionName = ctx.Identifier().getText();
    FunctionSymbol newFunction = new FunctionSymbol(functionName, moduleScope);
    configureSymbol(newFunction, ctx.start);

    //More like a library - so we mark as referenced.
    newFunction.setReferenced(true);
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
      newFunction.setMarkedAbstract(true);
    }

    var parameterisedSymbols = createAndRegisterParameterisedSymbols(ctx.parameterisedParams(), moduleScope);
    if (!parameterisedSymbols.isEmpty()) {
      //Now need to register against the class we are creating
      parameterisedSymbols.forEach(newFunction::addParameterisedType);
      //It is also important to hold on to the context when it comes to template/generic expansion.
      newFunction.setContextForParameterisedType(ctx);
    }

    //make a note of this - could be null
    newFunction.setReturningParamContext(ctx.operationDetails().returningParam());

    return newFunction;
  }

  /**
   * Create a new aggregate that represents EK9 text construct.
   */
  public AggregateSymbol newText(EK9Parser.TextDeclarationContext ctx, String forLanguage) {
    //an error will have been created for the developer as language does not conform.
    if (forLanguage != null) {
      String textName = ctx.Identifier().getText() + "_" + forLanguage;
      AggregateSymbol text = new AggregateSymbol(textName, parsedModule.getModuleScope());
      configureAggregate(text, ctx.start);
      text.setGenus(ISymbol.SymbolGenus.TEXT);
      text.putSquirrelledData("LANG", forLanguage);

      return text;
    }
    //return a place holder
    return new AggregateSymbol(ctx.Identifier().getText(), parsedModule.getModuleScope());
  }

  /**
   * Create a new aggregate that represents an EK9 text body - this is represented by a method.
   */
  public MethodSymbol newTextBody(EK9Parser.TextBodyDeclarationContext ctx, IScope scope) {
    String methodName = ctx.Identifier().getText();

    MethodSymbol method = new MethodSymbol(methodName, scope);
    configureSymbol(method, ctx.start);

    method.setOverride(true);
    method.setMarkedAbstract(false);
    method.setMarkedPure(true);

    method.setOperator(false);
    //Always returns a String - no need or ability to declare it.
    method.setType(scope.resolve(new TypeSymbolSearch(EK9_STRING)));

    return method;
  }

  /**
   * Create a new aggregate that represents an EK9 service.
   */
  public AggregateSymbol newService(EK9Parser.ServiceDeclarationContext ctx) {
    String serviceName = ctx.Identifier().getText();
    AggregateSymbol service = new AggregateSymbol(serviceName, parsedModule.getModuleScope());
    configureAggregate(service, ctx.start);
    service.setGenus(ISymbol.SymbolGenus.SERVICE);

    var uri = ctx.Uriproto().getText();
    service.putSquirrelledData("HTTPURI", uri);
    checkForInvalidServiceDefinition.accept(service);

    return service;
  }

  /**
   * Create a new aggregate that represents an EK9 service operation - a specialised method.
   */
  public ServiceOperationSymbol newServiceOperation(EK9Parser.ServiceOperationDeclarationContext ctx, IScope scope) {
    boolean operator = false;
    String methodName = null;
    if (ctx.operator() != null) {
      methodName = ctx.operator().getText();
      operator = true;
    } else if (ctx.identifier() != null) {
      methodName = ctx.identifier().getText();
    }

    ServiceOperationSymbol serviceOperation = new ServiceOperationSymbol(methodName, scope);
    configureSymbol(serviceOperation, ctx.start);

    serviceOperation.setOverride(false);
    serviceOperation.setMarkedAbstract(false);
    serviceOperation.setMarkedPure(false);
    serviceOperation.setOperator(operator);

    serviceOperation.putSquirrelledData("URIPROTO", ctx.Uriproto().getText());
    //Make some initial assumptions, these can then be overridden
    serviceOperation.putSquirrelledData(HTTPVERB, "GET");

    if (ctx.httpVerb() != null) {
      //A specific verb has been declared for use by the developer
      var httpVerb = ctx.httpVerb().getText();
      serviceOperation.putSquirrelledData(HTTPVERB, httpVerb);
    }

    if (ctx.operator() != null) {
      //Check operator used is valid.
      checkForInvalidServiceOperator.accept(serviceOperation);
      serviceOperation.putSquirrelledData(HTTPVERB,
          operatorToHttpVerbMap.getOrDefault(serviceOperation.getName(), ""));
    }

    return serviceOperation;
  }

  /**
   * Create a new aggregate that represents an EK9 application.
   */
  public AggregateSymbol newApplication(EK9Parser.ApplicationDeclarationContext ctx) {
    String applicationName = ctx.Identifier().getText();
    AggregateSymbol application = new AggregateSymbol(applicationName, parsedModule.getModuleScope());
    configureAggregate(application, ctx.start);

    //By default a general program application
    application.setGenus(ISymbol.SymbolGenus.GENERAL_APPLICATION);
    EK9Parser.ApplicationBlockContext applicationBlockContext = (EK9Parser.ApplicationBlockContext) ctx.getParent();

    if (applicationBlockContext.appType != null && applicationBlockContext.appType.getType() == EK9Parser.SERVICE) {
      application.setGenus(ISymbol.SymbolGenus.SERVICE_APPLICATION);
    }

    return application;
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic class.
   */
  public AggregateWithTraitsSymbol newDynamicClass(EK9Parser.DynamicClassDeclarationContext ctx) {
    //Name is optional - if not present then generate a dynamic value.
    String dynamicClassName = ctx.Identifier() != null
        ? ctx.Identifier().getText()
        : "_Class_" + UniqueIdGenerator.getNewUniqueId();

    //Perhaps keep a reference to the scope where this dynamic class was defined.
    AggregateWithTraitsSymbol rtn = newAggregateWithTraitsSymbol(dynamicClassName, ctx.start);
    rtn.setReferenced(true);
    return rtn;
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic function.
   */
  public FunctionSymbol newDynamicFunction(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    //As above need to consider how S, T etc would be resolved in later phases.
    var functionName = "_Function_" + UniqueIdGenerator.getNewUniqueId();
    var newFunction = new FunctionSymbol(functionName, parsedModule.getModuleScope());

    configureSymbol(newFunction, ctx.start);
    newFunction.setModuleScope(parsedModule.getModuleScope());
    newFunction.setGenus(ISymbol.SymbolGenus.FUNCTION);
    newFunction.setMarkedPure(ctx.PURE() != null);
    newFunction.setReferenced(true);

    return newFunction;
  }

  /**
   * Create a new parameterised type, i.e. take a generic type and a type parameter and create
   * what is in effect a new type out of the two. 'List of String' or 'List of Date' for example.
   */
  public ParameterisedTypeSymbol newParameterisedTypeSymbol(final AggregateSymbol genericAggregateType,
                                                            final List<ISymbol> parameterizingTypes) {
    //Use the same module scope as the generic type as there is where the new type will reside.
    var scope = genericAggregateType.getModuleScope();
    var theType = new ParameterisedTypeSymbol(genericAggregateType, parameterizingTypes, scope);
    //Use the same source token for this as the generic type.
    theType.setModuleScope(genericAggregateType.getModuleScope());
    theType.setParsedModule(genericAggregateType.getParsedModule());
    theType.setSourceToken(genericAggregateType.getSourceToken());

    return theType;
  }

  /**
   * Create a new parameterised function, i.e. take a generic function and a type parameter and create
   * what is in effect a new function out of the two. 'Supplier of String' or 'Consumer of Date' for example.
   */
  public ParameterisedFunctionSymbol newParameterisedFunctionSymbol(FunctionSymbol genericFunction,
                                                                    List<ISymbol> parameterizingTypes) {
    //Use the same module scope as the generic type as there is where the new type will reside.
    var scope = genericFunction.getModuleScope();
    var theFunction = new ParameterisedFunctionSymbol(genericFunction, parameterizingTypes, scope);
    //Use the same source token for this as the generic function.
    theFunction.setModuleScope(genericFunction.getModuleScope());
    theFunction.setParsedModule(genericFunction.getParsedModule());
    theFunction.setSourceToken(genericFunction.getSourceToken());

    return theFunction;
  }

  /**
   * Create a new local scope just for variables to be defined/captured in for dynamic classes/functions.
   */
  public LocalScope newDynamicVariableCapture(ICanCaptureVariables scope) {
    //This local scope cannot access anything else in the scope it was defined in.
    //It is a very isolated scope just for captured variables.
    final var captureScope = new LocalScope("CaptureScope", parsedModule.getModuleScope());
    //Now the dynamic class/function needs to know about this additional scope it may have to resolve variables in.
    scope.setCapturedVariables(captureScope);

    //Need to check if the naming is valid.
    return captureScope;
  }

  /**
   * Create a new aggregate that represents an EK9 operator, uses a method for this.
   */
  public MethodSymbol newOperator(EK9Parser.OperatorDeclarationContext ctx, IScopedSymbol scopedSymbol) {
    String methodName = ctx.operator().getText();
    MethodSymbol method = new MethodSymbol(methodName, scopedSymbol);

    configureSymbol(method, ctx.start);

    method.setOverride(ctx.OVERRIDE() != null);
    method.setMarkedAbstract(ctx.ABSTRACT() != null);
    method.setMarkedPure(ctx.PURE() != null);

    //Set this as default unless we have a returning section
    method.setType(scopedSymbol.resolve(new TypeSymbolSearch(EK9_VOID)));

    return method;
  }

  /**
   * A bit of a beast this method.
   * Check if operator is not present if so add one in.
   * But as it is used in very early phase of compilation not all types will be known.
   * So here this method just uses the raw name of the method.
   */
  public void addMissingDefaultOperators(EK9Parser.DefaultOperatorContext ctx, IAggregateSymbol aggregate) {
    var existingMethodNames = aggregate.getAllNonAbstractMethods()
        .stream()
        .map(ISymbol::getName)
        .toList();

    for (MethodSymbol operator : aggregateFactory.getAllPossibleDefaultOperators(aggregate)) {
      if (!existingMethodNames.contains(operator.getName())) {
        operator.setSourceToken(ctx.start);
        operator.putSquirrelledData(DEFAULTED, "TRUE");
        //Now we can add that operator in.
        aggregate.define(operator);
      }
    }

    //For records also add in the to JSON operator if not present.
    if (aggregate.getGenus() == ISymbol.SymbolGenus.RECORD && !existingMethodNames.contains("$$")) {
      var jsonOperator = aggregateFactory.createToJsonSimpleOperator(aggregate);
      jsonOperator.setSourceToken(ctx.start);
      jsonOperator.putSquirrelledData(DEFAULTED, "TRUE");
      //Now we can add that operator in.
      aggregate.define(jsonOperator);
    }
  }

  /**
   * Create a new aggregate that represents an EK9 class/component method.
   */
  public MethodSymbol newMethod(EK9Parser.MethodDeclarationContext ctx, IScopedSymbol scopedSymbol) {
    //So now we should have an aggregate we are adding this method into.
    String methodName = ctx.identifier().getText();
    return newMethod(ctx, methodName, scopedSymbol);
  }

  /**
   * Create a new method with a specific name.
   */
  public MethodSymbol newMethod(final EK9Parser.MethodDeclarationContext ctx, final String methodName,
                                final IScopedSymbol scopedSymbol) {

    MethodSymbol method = new MethodSymbol(methodName, scopedSymbol);

    configureSymbol(method, ctx.start);

    method.setOverride(ctx.OVERRIDE() != null);
    method.setMarkedAbstract(ctx.ABSTRACT() != null);
    method.setMarkedPure(ctx.PURE() != null);
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
      method.setType(scopedSymbol.resolve(new TypeSymbolSearch(EK9_VOID)));
    }

    return method;
  }

  /**
   * Create a new aggregate that represents an EK9 type, constrained or enumeration.
   */
  public AggregateSymbol newType(EK9Parser.TypeDeclarationContext ctx) {
    String newTypeName = ctx.Identifier().getText();
    AggregateSymbol clazz = new AggregateSymbol(newTypeName, parsedModule.getModuleScope());
    configureAggregate(clazz, ctx.start);

    if (ctx.typeDef() != null) {
      clazz.setGenus(ISymbol.SymbolGenus.CLASS_CONSTRAINED);
    } else if (ctx.enumerationDeclaration() != null) {
      clazz.setGenus(ISymbol.SymbolGenus.CLASS_ENUMERATION);
      aggregateFactory.addEnumerationMethods(clazz);
    }
    return clazz;
  }

  /**
   * Populates the enumeration with each of the values supplied in the identifiers.
   * This does check for duplicates and will raise errors if there are any.
   */
  public void populateEnumeration(AggregateSymbol enumerationSymbol, List<TerminalNode> identifiers) {
    //Quick way to ensure uniqueness, could have used enumerationSymbol to check.
    //But I want to search irrespective of case and '_' and error on stuff that is similar.
    var checkValues = new HashMap<String, Token>();

    identifiers.forEach(identifier -> {
      //For checking duplicates we uppercase and remove '_', developer may use mixed case in enumerations.
      //But same/similar word in different cases is highly likely to cause issues or be an error (opinion).
      var enumValue = identifier.getText().toUpperCase().replace("_", "");
      var existing = checkValues.get(enumValue);

      if (existing == null) {
        checkValues.put(enumValue, identifier.getSymbol());
        addEnumeratedValue(identifier, enumerationSymbol);
      } else {
        new InvalidEnumeratedValue(parsedModule.getSource().getErrorListener())
            .accept(identifier.getSymbol(), existing);
      }
    });
  }

  private void addEnumeratedValue(TerminalNode ctx, AggregateSymbol inEnumeration) {
    ConstantSymbol symbol = new ConstantSymbol(ctx.getText());
    configureSymbol(symbol, ctx.getSymbol());
    symbol.setNotMutable();
    //The nature of an enumeration is to define possible values
    //These do not have to be referenced to be valuable. So mark referenced.
    symbol.setNullAllowed(false);
    symbol.setInitialisedBy(ctx.getSymbol());
    symbol.setReferenced(true);
    symbol.setType(inEnumeration);
    inEnumeration.define(symbol);
  }

  /**
   * Create a new symbol that represents an EK9 concept of a stream pipeline.
   */
  public StreamPipeLineSymbol newStream(EK9Parser.StreamContext ctx) {
    StreamPipeLineSymbol pipeLine = new StreamPipeLineSymbol("stream");
    configureSymbol(pipeLine, ctx.start);
    pipeLine.setReferenced(true);
    pipeLine.setNotMutable();
    return pipeLine;
  }

  /**
   * Create a new symbol that represents an EK9 'cat' part of a stream pipeline.
   */
  public StreamCallSymbol newStreamCat(EK9Parser.StreamCatContext ctx, IScope scope) {
    StreamCallSymbol call = new StreamCallSymbol("cat", scope);
    configureStreamCallSymbol(call, ctx.start);
    return call;
  }

  /**
   * Create a new symbol that represents an EK9 'for' part of a stream pipeline.
   */
  public StreamCallSymbol newStreamFor(EK9Parser.StreamForContext ctx, IScope scope) {
    StreamCallSymbol call = new StreamCallSymbol("for", scope);
    configureStreamCallSymbol(call, ctx.start);
    return call;
  }

  /**
   * Create a new symbol that represents an EK9 stream function part of a stream pipeline.
   */
  public StreamCallSymbol newStreamPart(EK9Parser.StreamPartContext ctx, IScope scope) {
    final var operation = ctx.op.getText();
    StreamCallSymbol call = new StreamCallSymbol(operation, scope);
    configureStreamCallSymbol(call, ctx.start);

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
  public StreamCallSymbol newStreamTermination(EK9Parser.StreamTerminationContext ctx, IScope scope) {
    final var operation = ctx.op.getText();
    StreamCallSymbol call = new StreamCallSymbol(operation, scope);
    configureStreamCallSymbol(call, ctx.start);
    call.setSinkInNature(true);
    return call;
  }

  private void configureStreamCallSymbol(final StreamCallSymbol call, final Token token) {
    configureSymbol(call, token);
    call.setReferenced(true);
    call.setNotMutable();
  }

  /**
   * Just for general symbols, like references.
   */
  public Symbol newGeneralSymbol(final Token token, String name) {
    Symbol symbol = new Symbol(name);
    configureSymbol(symbol, token);
    return symbol;
  }

  /**
   * Symbol to model some type of call to a function, dynamic function, constructor, this, super etc.
   */
  public CallSymbol newCall(EK9Parser.CallContext ctx, IScope scope) {
    var symbol = new CallSymbol(ctx.getText(), scope);
    configureSymbol(symbol, ctx.start);

    symbol.setInitialisedBy(ctx.start);

    return symbol;
  }

  /**
   * Create a new EK9 List (a generic type).
   */
  public CallSymbol newList(EK9Parser.ListContext ctx, IScope scope) {
    var symbol = new CallSymbol("List", scope);
    configureSymbol(symbol, ctx.start);

    symbol.setInitialisedBy(ctx.start);

    return symbol;
  }

  /**
   * Create a new EK9 Dictionary - like a Map.
   */
  public CallSymbol newDict(EK9Parser.DictContext ctx, IScope scope) {
    var symbol = new CallSymbol("Dict", scope);
    configureSymbol(symbol, ctx.start);

    symbol.setInitialisedBy(ctx.start);

    return symbol;
  }

  /**
   * Create a new entry for the Dictionary, basically a tuple.
   */
  public CallSymbol newDictEntry(EK9Parser.InitValuePairContext ctx, IScope scope) {
    var symbol = new CallSymbol("DictEntry", scope);
    configureSymbol(symbol, ctx.start);

    symbol.setInitialisedBy(ctx.start);

    return symbol;
  }


  /**
   * New call but modelled as an operator if marked as such.
   */
  public CallSymbol newOperationCall(EK9Parser.OperationCallContext ctx, IScope scope) {

    var callName = ctx.operator() != null ? ctx.operator().getText() : ctx.identifier().getText();
    var symbol = new CallSymbol(callName, scope);
    configureSymbol(symbol, ctx.start);
    symbol.setOperator(ctx.operator() != null);
    symbol.setInitialisedBy(ctx.start);

    return symbol;
  }

  /**
   * Create a new symbol that represents an EK9 'switch' block.
   */
  public SwitchSymbol newSwitch(EK9Parser.SwitchStatementExpressionContext ctx, IScope scope) {
    SwitchSymbol switchSymbol = new SwitchSymbol(scope);
    configureSymbol(switchSymbol, ctx.start);
    checkSwitch.accept(ctx);
    return switchSymbol;
  }

  /**
   * Create a new symbol that represents an EK9 'try' block.
   */
  public TrySymbol newTry(EK9Parser.TryStatementExpressionContext ctx, IScope scope) {
    TrySymbol trySymbol = new TrySymbol(scope);
    configureSymbol(trySymbol, ctx.start);
    checkTryReturns.accept(ctx);
    return trySymbol;
  }

  /**
   * Create a new symbol that represents an EK9 'for' loop.
   */
  public ForSymbol newForLoop(ParserRuleContext ctx, IScope scope) {
    var forLoop = new ForSymbol(scope);
    configureSymbol(forLoop, ctx.start);
    return forLoop;
  }

  /**
   * Create a new aggregate that represents an EK9 loop variable.
   */
  public VariableSymbol newLoopVariable(EK9Parser.ForLoopContext ctx) {
    checkContextNotNull.accept(ctx);
    return newLoopVariable(ctx.identifier());
  }

  /**
   * Create a new aggregate that represents an EK9 loop variable for a range.
   */
  public VariableSymbol newLoopVariable(EK9Parser.ForRangeContext ctx) {
    checkContextNotNull.accept(ctx);
    //The second identifier (which may not be present) is the loop step either literal or another identifer.
    return newLoopVariable(ctx.identifier(0));
  }

  private VariableSymbol newLoopVariable(EK9Parser.IdentifierContext identifier) {
    checkContextNotNull.accept(identifier);
    var variable = newVariable(identifier, false, false);
    //Make note is a loop variable and EK9 will initialise it, developer cannot mutate it.
    variable.setLoopVariable(true);
    variable.setInitialisedBy(identifier.start);
    variable.setNotMutable();
    return variable;
  }


  /**
   * Create a new constant that represents the fixed text part of an interpolated String.
   */
  public ConstantSymbol newInterpolatedStringPart(final EK9Parser.StringPartContext ctx, final IScope scope) {
    //if interpolated string had a " in now needs to be escaped because we will wrapping ""
    //But also if we had and escaped $ i.e. \$ we can turn that back into a dollar now.
    String literalText = ctx.getChild(0).getText().replace("\"", "\\\"").replace("\\$", "$").replace("\\`", "`");
    ConstantSymbol literal = newLiteral(ctx.start, "\"" + literalText + "\"");
    literal.setType(scope.resolve(new TypeSymbolSearch(EK9_STRING)));

    return literal;
  }

  /**
   * Create a new expression that represents the expression part of and interpolated String.
   */
  public ExpressionSymbol newInterpolatedExpressionPart(final EK9Parser.StringPartContext ctx) {
    ExpressionSymbol expressionSymbol = new ExpressionSymbol(ctx.getText());
    configureSymbol(expressionSymbol, ctx.start);

    return expressionSymbol;
  }

  /**
   * Create a new constant as declared in the constants section.
   */
  public ConstantSymbol newConstant(EK9Parser.ConstantDeclarationContext ctx) {
    ConstantSymbol constant = new ConstantSymbol(ctx.Identifier().getText(), false);
    configureSymbol(constant, ctx.start);

    return constant;
  }

  /**
   * Just a declaration of a variable by itself - i.e. without an assignment.
   */
  public VariableSymbol newVariable(EK9Parser.VariableOnlyDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    //Now this is also used in web services and additional ctx.webVariableCorrelation()
    //Makes sense but only for services.
    checkAppropriateWebVariable.accept(ctx);
    var variable = newVariable(ctx.identifier(), ctx.QUESTION() != null, ctx.BANG() != null);

    if (ctx.webVariableCorrelation() != null) {
      //We need to squirrel away the information, so it can be both checked and used elsewhere
      variable.putSquirrelledData("HTTPACCESS", ctx.webVariableCorrelation().httpAccess().getText());
      if (ctx.webVariableCorrelation().stringLit() != null) {
        //Where will this be pulled from
        variable.putSquirrelledData("HTTPSOURCE", ctx.webVariableCorrelation().stringLit().getText());
        //Obviously is httpAccess is HEADER then it must be a valid header name.
        //If httpsAccess is PATH then it must exist in the PATH on the Method
        //These things are checked elsewhere - here we are just gathering info.
      } else {
        variable.putSquirrelledData("HTTPSOURCE", variable.getName());
      }
    }
    return variable;
  }

  /**
   * Create and initialise a new variable symbol.
   * Typically, a variable like name <- "Steve", so 'name' is the variable.
   */
  public VariableSymbol newVariable(final EK9Parser.VariableDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    return newVariable(ctx.identifier(), ctx.QUESTION() != null, false);
  }

  /**
   * Crrate new variable typically when looking to create simulated variable.
   */
  public VariableSymbol newVariable(final String name, final Token token,
                                    final boolean nullAllowed, final boolean injectionExpected) {
    AssertValue.checkNotNull("Failed to locate variable name", name);
    VariableSymbol variable = new VariableSymbol(name);
    configureSymbol(variable, token);
    variable.setNullAllowed(nullAllowed);
    variable.setInjectionExpected(injectionExpected);

    return variable;
  }

  public VariableSymbol newVariable(final EK9Parser.IdentifierContext identifier,
                                     final boolean nullAllowed, final boolean injectionExpected) {
    AssertValue.checkNotNull("Failed to locate variable name", identifier);
    return newVariable(identifier.getText(), identifier.start, nullAllowed, injectionExpected);
  }


  /**
   * Create a new aggregate that represents an EK9 literal value.
   */
  public ConstantSymbol newLiteral(Token start, String name) {
    AssertValue.checkNotNull("Start token cannot be null", start);
    AssertValue.checkNotNull("Name cannot be null", name);

    ConstantSymbol literal = new ConstantSymbol(name, true);
    configureSymbol(literal, start);
    //You cannot set this to any other value.
    literal.setNotMutable();

    return literal;
  }

  private AggregateWithTraitsSymbol newAggregateWithTraitsSymbol(String className, Token start) {
    var scope = parsedModule.getModuleScope();
    AggregateWithTraitsSymbol clazz = new AggregateWithTraitsSymbol(className, scope);
    configureAggregate(clazz, start);
    clazz.setGenus(ISymbol.SymbolGenus.CLASS);
    return clazz;
  }

  private void configureAggregate(AggregateSymbol aggregate, Token start) {
    aggregate.setModuleScope(parsedModule.getModuleScope());
    configureSymbol(aggregate, start);
  }

  private void configureSymbol(ISymbol symbol, Token start) {
    symbol.setParsedModule(Optional.of(parsedModule));
    symbol.setSourceToken(start);
    if (parsedModule.isExternallyImplemented()) {
      symbol.putSquirrelledData("EXTERN", "TRUE");
    }
  }

  private List<AggregateSymbol> createAndRegisterParameterisedSymbols(final EK9Parser.ParameterisedParamsContext ctx,
                                                                      final IScope scope) {
    List<AggregateSymbol> rtn = new ArrayList<>();
    if (ctx != null) {
      for (int i = 0; i < ctx.parameterisedDetail().size(); i++) {
        EK9Parser.ParameterisedDetailContext detail = ctx.parameterisedDetail(i);
        //Then this is a generic type class we simulate and add the S, T, U whatever into the class scope it is
        //a simulated type we have no real idea what it could be but it is a template parameter to be replace at
        //cookie cutting time.
        //getText(i) gives us the parameter name we just us 'T' here to denote a generic param

        AggregateSymbol t = aggregateFactory.createGenericT(detail.Identifier().getText(), scope);

        //Now going forward we also have constraints of those type S extends String etc.
        //So after phase 1 - but before phase 5 (resolve) we will need to revisit this to ensure we have
        //applied any super class to these 'T's
        rtn.add(t);
        parsedModule.recordSymbol(detail, t);
      }
      //Now we have simulated S, T, P etc - what about adding additional constructors - we have the default
      //This would mean that we could do new T() and new T(an S) or new S(a T) etc. so just a single param
      //type constructor.
      //Otherwise it get too hard on ordering.

      //Add other synthetic constructors now we have all the S, T, P etc.
      rtn.forEach(t -> rtn.forEach(s -> aggregateFactory.addConstructor(t, new VariableSymbol("arg", s))));
    }
    return rtn;
  }


}
