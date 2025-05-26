package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DEFAULT_AND_TRAIT;
import static org.ek9lang.compiler.support.CommonValues.DEFAULTED;

import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AnyTypeSymbol;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;

class OperationFactory extends CommonFactory {

  private final CheckAndPopulateOperator checkAndPopulateOperator;

  OperationFactory(ParsedModule parsedModule) {
    super(parsedModule);

    this.checkAndPopulateOperator
        = new CheckAndPopulateOperator(aggregateManipulator, parsedModule.getSource().getErrorListener());
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

    if (aggregate.getGenus().equals(SymbolGenus.CLASS_TRAIT)) {
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

    for (MethodSymbol operator : aggregateManipulator.getAllPossibleDefaultOperators(aggregate)) {
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
      method.setType(aggregateManipulator.resolveVoid(scopedSymbol));
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
      aggregateSymbol.setGenus(SymbolGenus.CLASS_CONSTRAINED);
    } else if (ctx.enumerationDeclaration() != null) {
      aggregateSymbol.setGenus(SymbolGenus.CLASS_ENUMERATION);
      aggregateManipulator.addEnumerationMethods(aggregateSymbol);
    }

    return aggregateSymbol;
  }

  public AnyTypeSymbol newAny(final EK9Parser.ModuleDeclarationContext ctx) {
    final var anyTypeSymbol = new AnyTypeSymbol("Any", parsedModule.getModuleScope());

    configureAggregate(anyTypeSymbol, new Ek9Token(ctx.start));
    anyTypeSymbol.setOpenForExtension(true);
    aggregateManipulator.addConstructor(anyTypeSymbol, List.of(), true, true);

    //The '?' is baked into compiler code for Any so the operator does not need to exist.

    return anyTypeSymbol;
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

}
