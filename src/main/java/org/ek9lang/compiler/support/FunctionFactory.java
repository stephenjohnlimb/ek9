package org.ek9lang.compiler.support;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.UniqueIdGenerator;

class FunctionFactory extends CommonFactory {
  FunctionFactory(ParsedModule parsedModule) {
    super(parsedModule);
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
      newFunction.setGenus(SymbolGenus.FUNCTION);
    } else {
      newFunction.setGenus(SymbolGenus.FUNCTION_TRAIT);
    }

    final var parameterisedSymbols = createAndRegisterParameterisedSymbols(ctx.parameterisedParams(), moduleScope);
    if (!parameterisedSymbols.isEmpty()) {
      //Now need to register against the class we are creating
      parameterisedSymbols.forEach(newFunction::addTypeParameterOrArgument);
    }

    return newFunction;
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic function.
   */
  public FunctionSymbol newDynamicFunction(final IScopedSymbol enclosingMainTypeOrFunction,
                                           final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    checkContextNotNull.accept(ctx);

    //As above need to consider how S, T etc. would be resolved in later phases.
    final var functionName = "_Function_" + UniqueIdGenerator.getNewUniqueId();
    final var newFunction = new FunctionSymbol(functionName, parsedModule.getModuleScope());

    newFunction.setOuterMostTypeOrFunction(enclosingMainTypeOrFunction);
    configureSymbol(newFunction, new Ek9Token(ctx.start));
    newFunction.setModuleScope(parsedModule.getModuleScope());
    newFunction.setGenus(SymbolGenus.FUNCTION);
    newFunction.setScopeType(IScope.ScopeType.DYNAMIC_BLOCK);
    newFunction.setMarkedPure(ctx.PURE() != null);
    newFunction.setReferenced(true);

    return newFunction;
  }
}
