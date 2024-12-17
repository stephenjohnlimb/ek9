package org.ek9lang.compiler.support;

import java.util.HashMap;
import java.util.List;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.ConstantSymbol;
import org.ek9lang.compiler.symbols.ExpressionSymbol;
import org.ek9lang.compiler.symbols.ICanCaptureVariables;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Factory for fairly basic symbols.
 */
class BasicSymbolFactory extends CommonFactory {
  BasicSymbolFactory(ParsedModule parsedModule) {
    super(parsedModule);
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
   * Create a new aggregate that represents an EK9 application.
   */
  public AggregateSymbol newApplication(final EK9Parser.ApplicationDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);
    final var applicationName = ctx.Identifier().getText();
    final var application = new AggregateSymbol(applicationName, parsedModule.getModuleScope());

    configureAggregate(application, new Ek9Token(ctx.start));
    application.setGenus(SymbolGenus.GENERAL_APPLICATION);

    return application;
  }

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

    checkContextNotNull.accept(ctx);
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
   * Symbol to model some type of call to a function, dynamic function, constructor, this, super etc.
   */
  public CallSymbol newCall(final EK9Parser.CallContext ctx, final IScope scope) {

    checkContextNotNull.accept(ctx);
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

    checkContextNotNull.accept(ctx);
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

    checkContextNotNull.accept(ctx);
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

    checkContextNotNull.accept(ctx);
    final var callSymbol = new CallSymbol("DictEntry", scope);
    final var startToken = new Ek9Token(ctx.start);

    configureSymbol(callSymbol, startToken);
    callSymbol.setInitialisedBy(startToken);

    return callSymbol;
  }

  /**
   * Create a new constant that represents the fixed text part of an interpolated String.
   */
  public ConstantSymbol newInterpolatedStringPart(final EK9Parser.StringPartContext ctx) {

    checkContextNotNull.accept(ctx);
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

    checkContextNotNull.accept(ctx);
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

    checkContextNotNull.accept(ctx);
    final var start = new Ek9Token(ctx.start);
    final var constant = new ConstantSymbol(ctx.Identifier().getText(), false);

    configureSymbol(constant, start);
    constant.setInitialisedBy(start);
    constant.setNotMutable();

    return constant;
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
