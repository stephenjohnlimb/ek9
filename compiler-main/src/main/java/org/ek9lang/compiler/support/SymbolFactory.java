package org.ek9lang.compiler.support;

import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.directives.Directive;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.AnyTypeSymbol;
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
  private final BasicSymbolFactory basicSymbolFactory;

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
    this.basicSymbolFactory
        = new BasicSymbolFactory(parsedModule);

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

    return basicSymbolFactory.newPackage(ctx);
  }

  /**
   * Create a new aggregate that represents an EK9 program.
   */
  public AggregateSymbol newProgram(final EK9Parser.MethodDeclarationContext ctx) {

    return basicSymbolFactory.newProgram(ctx);
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

    return basicSymbolFactory.newApplication(ctx);
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

    return basicSymbolFactory.newDynamicVariableCapture(scope, enclosingBlockScope);
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
   * Creates the builtin 'Any' type super of 'things'.
   */
  public AnyTypeSymbol newAny(final EK9Parser.ModuleDeclarationContext ctx) {
    return operationFactory.newAny(ctx);
  }

  public void addMethodsToAny(final AggregateSymbol anyTypeSymbol) {
    operationFactory.addMethodsToAny(anyTypeSymbol);
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

    basicSymbolFactory.populateEnumeration(enumerationSymbol, identifiers);

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

    return basicSymbolFactory.newCall(ctx, scope);
  }

  /**
   * Create a new EK9 List (a generic type).
   */
  public CallSymbol newList(final EK9Parser.ListContext ctx, final IScope scope) {

    return basicSymbolFactory.newList(ctx, scope);
  }

  /**
   * Create a new EK9 Dictionary - like a Map.
   */
  public CallSymbol newDict(final EK9Parser.DictContext ctx, final IScope scope) {

    return basicSymbolFactory.newDict(ctx, scope);
  }

  /**
   * Create a new entry for the Dictionary, basically a tuple.
   */
  public CallSymbol newDictEntry(final EK9Parser.InitValuePairContext ctx, final IScope scope) {

    return basicSymbolFactory.newDictEntry(ctx, scope);
  }

  /**
   * New call but modelled as an operator if marked as such.
   */
  public CallSymbol newOperationCall(final EK9Parser.OperationCallContext ctx, final IScope scope) {

    return operationFactory.newOperationCall(ctx, scope);
  }

  /**
   * Create a new CallSymbol for switch case expression operator.
   */
  public CallSymbol newCaseExpressionCall(final EK9Parser.CaseExpressionContext ctx, final IScope scope) {

    return operationFactory.newCaseExpressionCall(ctx, scope);
  }

  /**
   * Create a new CallSymbol for an operator in an expression.
   * This enables uniform handling of all operators (binary, unary) like method calls.
   */
  public CallSymbol newOperatorCall(final String operator, final EK9Parser.ExpressionContext ctx,
                                    final IScope scope) {

    return operationFactory.newOperatorCall(operator, ctx, scope);
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

    final var variable = basicSymbolFactory.newVariable(identifier, false, false);
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

    return basicSymbolFactory.newInterpolatedStringPart(ctx);
  }

  /**
   * Create a new expression that represents the expression part of and interpolated String.
   */
  public ExpressionSymbol newInterpolatedExpressionPart(final EK9Parser.StringPartContext ctx) {

    return basicSymbolFactory.newInterpolatedExpressionPart(ctx);
  }

  /**
   * Create a new constant as declared in the constants section.
   */
  public ConstantSymbol newConstant(final EK9Parser.ConstantDeclarationContext ctx) {

    return basicSymbolFactory.newConstant(ctx);
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

    return basicSymbolFactory.newVariable(ctx);
  }

  /**
   * Create new variable typically when looking to create simulated variable.
   */
  public VariableSymbol newVariable(final String name,
                                    final IToken token,
                                    final boolean nullAllowed,
                                    final boolean injectionExpected) {

    return basicSymbolFactory.newVariable(name, token, nullAllowed, injectionExpected);
  }

  public VariableSymbol newVariable(final EK9Parser.IdentifierContext identifier,
                                    final boolean nullAllowed,
                                    final boolean injectionExpected) {

    return basicSymbolFactory.newVariable(identifier, nullAllowed, injectionExpected);
  }

  /**
   * Create a new aggregate that represents an EK9 literal value.
   */
  public ConstantSymbol newLiteral(final IToken start, final String name) {

    return basicSymbolFactory.newLiteral(start, name);
  }

}
