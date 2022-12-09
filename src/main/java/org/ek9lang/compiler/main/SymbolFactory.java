package org.ek9lang.compiler.main;

import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.ConstantSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.core.exception.AssertValue;

public class SymbolFactory {

  private final ParsedModule parsedModule;

  public SymbolFactory(ParsedModule parsedModule) {
    AssertValue.checkNotNull("Parsed Module cannot be null", parsedModule);
    this.parsedModule = parsedModule;
  }

  public AggregateSymbol newProgram(EK9Parser.MethodDeclarationContext ctx, IScope scope)
  {
    AssertValue.checkNotNull("Failed to locate program name", ctx.identifier());

    //TODO Need to deal with the application setup part.
    //We underscore to class name and main processing method don't clash and look like a constructor.
    String programName = "_" + ctx.identifier().getText();

    AggregateSymbol program = new AggregateSymbol(programName, scope);
    configureAggregate(program, ctx.start);

    return program;
  }

  public ConstantSymbol newLiteral(Token start, String name) {
    AssertValue.checkNotNull("Start token cannot be null", start);
    AssertValue.checkNotNull("Name cannot be null", name);

    ConstantSymbol literal = new ConstantSymbol(name, true);

    literal.setSourceToken(start);
    literal.setParsedModule(Optional.of(parsedModule));
    //You cannot set this to any other value.
    literal.setNotMutable();
    return literal;
  }

  private void configureAggregate(AggregateSymbol aggregate, Token start)
  {
    aggregate.setModuleScope(parsedModule.getModuleScope());
    aggregate.setParsedModule(Optional.of(parsedModule));
    aggregate.setSourceToken(start);
  }

}
