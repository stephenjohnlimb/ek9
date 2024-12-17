package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.support.CommonValues.EXTERN;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Just for details that are common to all factories.
 */
class CommonFactory {

  protected final Consumer<Object> checkContextNotNull = ctx -> AssertValue.checkNotNull("CTX cannot be null", ctx);
  protected final ParsedModule parsedModule;
  protected final AggregateManipulator aggregateManipulator;


  CommonFactory(final ParsedModule parsedModule) {
    AssertValue.checkNotNull("Parsed Module cannot be null", parsedModule);
    this.parsedModule = parsedModule;
    this.aggregateManipulator
        = new AggregateManipulator(parsedModule.getEk9Types());

  }

  public AggregateManipulator getAggregateFactory() {

    return aggregateManipulator;
  }

  protected void configureAggregate(final AggregateSymbol aggregate, final IToken start) {

    aggregate.setModuleScope(parsedModule.getModuleScope());
    //By their nature they are initialised and do not have to be referenced to be part of a system.
    aggregate.setInitialisedBy(start);
    aggregate.setReferenced(true);
    configureSymbol(aggregate, start);

  }

  protected void configureSymbol(final ISymbol symbol, final IToken start) {

    symbol.setParsedModule(Optional.of(parsedModule));
    symbol.setSourceToken(start);
    if (parsedModule.isExternallyImplemented()) {
      symbol.putSquirrelledData(EXTERN, "TRUE");
    }
  }

  protected Consumer<MethodSymbol> getDefaultOperatorSymbolInitializer(final Consumer<ISymbol> initialise) {

    return operator -> {
      initialise.accept(operator);
      operator.getCallParameters().forEach(initialise);
      if (operator.isReturningSymbolPresent()) {
        initialise.accept(operator.getReturningSymbol());
      }
    };
  }

  protected Consumer<ISymbol> getDefaultOperatorInitializer(final IToken startToken) {

    return symbol -> {
      symbol.setSourceToken(startToken);
      symbol.setInitialisedBy(startToken);
      configureSymbol(symbol, startToken);
    };
  }


  protected List<AggregateSymbol> createAndRegisterParameterisedSymbols(final EK9Parser.ParameterisedParamsContext ctx,
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

        final var t = aggregateManipulator.createGenericT(detail.Identifier().getText(), scope);
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
      rtn.forEach(t -> rtn.forEach(s -> aggregateManipulator.addConstructor(t, new VariableSymbol("arg", s))));
    }
    return rtn;
  }

}
