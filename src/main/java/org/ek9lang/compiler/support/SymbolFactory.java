package org.ek9lang.compiler.support;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.directives.Directive;
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
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.symbols.TrySymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.symbols.WhileSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Just a factory for all types of EK9 symbol.
 * Ensures that newly created symbols are initialised correctly.
 * Delegates to other factories as appropriate - just to keep the size and complexity manageable.
 */
public class SymbolFactory extends CommonFactory {

  /**
   * To be used to create parameterised versions of generic types and functions.
   * Will replace parameterizedTypeCreator and parameterizedFunctionCreator.
   */
  private final ParameterizedSymbolCreator parameterizedSymbolCreator =
      new ParameterizedSymbolCreator(new InternalNameFor());

  private final CheckAppropriateWebVariable checkAppropriateWebVariable;
  private final DirectiveFactory directiveFactory;
  private final StreamFactory streamFactory;
  private final ServiceFactory serviceFactory;
  private final TextFactory textFactory;
  private final FunctionFactory functionFactory;
  private final AggregateFactory aggregateFactory;
  private final OperationFactory operationFactory;

  /**
   * Create a new symbol factory for use with the parsedModule.
   */
  public SymbolFactory(final ParsedModule parsedModule) {
    super(parsedModule);

    this.checkAppropriateWebVariable
        = new CheckAppropriateWebVariable(parsedModule.getSource().getErrorListener());
    this.directiveFactory
        = new DirectiveFactory(parsedModule);
    this.streamFactory
        = new StreamFactory(parsedModule);
    this.serviceFactory
        = new ServiceFactory(parsedModule);
    this.textFactory
        = new TextFactory(parsedModule);
    this.functionFactory
        = new FunctionFactory(parsedModule);
    this.aggregateFactory
        = new AggregateFactory(parsedModule);
    this.operationFactory
        = new OperationFactory(parsedModule);

  }

  /**
   * Create a new directive to be used inside the compiler.
   */
  public Directive newDirective(final EK9Parser.DirectiveContext ctx) {

    return directiveFactory.newDirective(ctx);

  }

  /**
   * Create a new EK9 package aggregate.
   */
  public AggregateSymbol newPackage(final EK9Parser.PackageBlockContext ctx) {

    checkContextNotNull.accept(ctx);
    final var pack = new AggregateSymbol("Package", parsedModule.getModuleScope());

    configureAggregate(pack, new Ek9Token(ctx.start));
    //Also add in a default constructor.
    pack.setGenus(SymbolGenus.META_DATA);
    aggregateManipulator.addConstructor(pack);

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

    program.setGenus(SymbolGenus.PROGRAM);

    return program;
  }

  /**
   * Create a new aggregate that represents an EK9 class.
   * A bit tricky when it comes to parameterised (generic/template classes).
   */
  public AggregateWithTraitsSymbol newClass(final EK9Parser.ClassDeclarationContext ctx) {

    return aggregateFactory.newClass(ctx);
  }

  /**
   * Create a new aggregate that represents an EK9 component.
   */
  public AggregateWithTraitsSymbol newComponent(final EK9Parser.ComponentDeclarationContext ctx) {

    return aggregateFactory.newComponent(ctx);
  }

  /**
   * Create a new aggregate that represents an EK9 trait.
   */
  public AggregateWithTraitsSymbol newTrait(final EK9Parser.TraitDeclarationContext ctx) {

    return aggregateFactory.newTrait(ctx);
  }

  /**
   * Create a new aggregate that represents an EK9 record.
   */
  public AggregateSymbol newRecord(final EK9Parser.RecordDeclarationContext ctx) {

    return aggregateFactory.newRecord(ctx);
  }

  /**
   * Create a new function symbol that represents an EK9 function.
   */
  public FunctionSymbol newFunction(final EK9Parser.FunctionDeclarationContext ctx) {
    return functionFactory.newFunction(ctx);
  }

  /**
   * Create a new aggregate that represents EK9 text construct.
   * Note that this also creates a common base for all text with the same name.
   * It ensures that all methods are added to that common base.
   * Then we can check if all those methods on the common base are also defined in each and every concrete language
   * text aggregate.
   */
  public AggregateSymbol newText(final EK9Parser.TextDeclarationContext ctx, final String forLanguage) {
    return textFactory.newText(ctx, forLanguage);
  }

  /**
   * Create a new aggregate that represents an EK9 text body - this is represented by a method.
   */
  public MethodSymbol newTextBody(final EK9Parser.TextBodyDeclarationContext ctx, final IScope scope) {

    return textFactory.newTextBody(ctx, scope);
  }

  /**
   * Ensures that any method in a specific text block is always added to the base text for that component.
   */
  public void ensureTextBodyIsInSuper(final MethodSymbol textMethodSymbol) {
    textFactory.ensureTextBodyIsInSuper(textMethodSymbol);
  }

  /**
   * Create a new aggregate that represents an EK9 service.
   */
  public AggregateSymbol newService(final EK9Parser.ServiceDeclarationContext ctx) {

    return serviceFactory.newService(ctx);
  }

  /**
   * Create a new aggregate that represents an EK9 service operation - a specialised method.
   */
  public ServiceOperationSymbol newServiceOperation(final EK9Parser.ServiceOperationDeclarationContext ctx,
                                                    final IScope scope) {

    return serviceFactory.newServiceOperation(ctx, scope);
  }

  /**
   * Create a new aggregate that represents an EK9 application.
   */
  public AggregateSymbol newApplication(final EK9Parser.ApplicationDeclarationContext ctx) {

    final var applicationName = ctx.Identifier().getText();
    final var application = new AggregateSymbol(applicationName, parsedModule.getModuleScope());

    configureAggregate(application, new Ek9Token(ctx.start));
    application.setGenus(SymbolGenus.GENERAL_APPLICATION);

    return application;
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic class.
   */
  public AggregateWithTraitsSymbol newDynamicClass(final IScopedSymbol enclosingMainTypeOrFunction,
                                                   final EK9Parser.DynamicClassDeclarationContext ctx) {

    return aggregateFactory.newDynamicClass(enclosingMainTypeOrFunction, ctx);
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic function.
   */
  public FunctionSymbol newDynamicFunction(final IScopedSymbol enclosingMainTypeOrFunction,
                                           final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    return functionFactory.newDynamicFunction(enclosingMainTypeOrFunction, ctx);
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

    return operationFactory.newOperator(ctx, aggregate);

  }

  /**
   * A bit of a beast this method.
   * Check if operator is not present if so add one in.
   * But as it is used in very early phase of compilation not all types will be known.
   * So here this method just uses the raw name of the method.
   */
  public boolean addMissingDefaultOperators(final EK9Parser.DefaultOperatorContext ctx,
                                            final IAggregateSymbol aggregate) {


    return operationFactory.addMissingDefaultOperators(ctx, aggregate);
  }

  /**
   * Create a new method that represents an EK9 class/component method.
   */
  public MethodSymbol newMethod(final EK9Parser.MethodDeclarationContext ctx, final IScopedSymbol scopedSymbol) {
    return operationFactory.newMethod(ctx, scopedSymbol);
  }

  /**
   * Create a new method with a specific name.
   */
  public MethodSymbol newMethod(final EK9Parser.MethodDeclarationContext ctx,
                                final String methodName,
                                final IScopedSymbol scopedSymbol) {

    return operationFactory.newMethod(ctx, methodName, scopedSymbol);
  }

  /**
   * Create a new aggregate that represents an EK9 type, constrained or enumeration.
   */
  public AggregateSymbol newType(final EK9Parser.TypeDeclarationContext ctx) {
    return operationFactory.newType(ctx);
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

    return streamFactory.newStream(ctx);
  }

  /**
   * Create a new symbol that represents an EK9 'cat' part of a stream pipeline.
   */
  public StreamCallSymbol newStreamCat(final EK9Parser.StreamCatContext ctx, final IScope scope) {

    return streamFactory.newStreamCat(ctx, scope);
  }

  /**
   * Create a new symbol that represents an EK9 'for' part of a stream pipeline.
   */
  public StreamCallSymbol newStreamFor(final EK9Parser.StreamForContext ctx, final IScope scope) {

    return streamFactory.newStreamFor(ctx, scope);
  }

  /**
   * Create a new symbol that represents an EK9 stream function part of a stream pipeline.
   */
  public StreamCallSymbol newStreamPart(final EK9Parser.StreamPartContext ctx, final IScope scope) {

    return streamFactory.newStreamPart(ctx, scope);
  }

  /**
   * Create a new symbol that represents an EK9 terminal part of a stream pipeline.
   */
  public StreamCallSymbol newStreamTermination(final ParserRuleContext ctx,
                                               final String operation,
                                               final IScope scope) {

    return streamFactory.newStreamTermination(ctx, operation, scope);
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

    return operationFactory.newOperationCall(ctx, scope);
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
  public ConstantSymbol newInterpolatedStringPart(final EK9Parser.StringPartContext ctx) {

    //if interpolated string had a " in now needs to be escaped because we will wrapping ""
    //But also if we had and escaped $ i.e. \$ we can turn that back into a dollar now.
    final var literalText = ctx.getChild(0).getText().replace("\"", "\\\"").replace("\\$", "$").replace("\\`", "`");
    final var literal = newLiteral(new Ek9Token(ctx.start), "\"" + literalText + "\"");

    literal.setType(parsedModule.getEk9Types().ek9String());

    return literal;
  }

  /**
   * Create a new expression that represents the expression part of and interpolated String.
   */
  public ExpressionSymbol newInterpolatedExpressionPart(final EK9Parser.StringPartContext ctx) {

    final var expressionSymbol = new ExpressionSymbol(ctx.getText());

    configureSymbol(expressionSymbol, new Ek9Token(ctx.start));
    //Must always return a string.
    expressionSymbol.setType(parsedModule.getEk9Types().ek9String());
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
      serviceFactory.configureWebVariable(ctx, variable);
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

}
