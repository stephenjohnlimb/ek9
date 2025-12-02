package org.ek9lang.compiler.directives;

import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.TypeDefResolver;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks if there are any directives that relate to @Implements in the parsed module and checks the
 * resolution through the symbol and its hierarchy.
 */
public class ImplementsDirectiveListener extends ResolvedDirectiveListener {

  @Override
  public void accept(final CompilationEvent compilationEvent) {

    if (compilationEvent.parsedModule() != null) {

      final var directives =
          compilationEvent.parsedModule().getDirectives(DirectiveType.Implements, compilationEvent.phase());

      if (!directives.isEmpty()) {
        processDirectives(compilationEvent, directives);
      }
    }

  }

  @Override
  protected void symbolMatch(final CompilationEvent compilationEvent,
                             final ResolutionDirective resolutionDirective,
                             final ISymbol symbol) {

    //Check the types and the like.
    super.symbolMatch(compilationEvent, resolutionDirective, symbol);

    final var errorListener = compilationEvent.source().getErrorListener();
    final var scope = compilationEvent.parsedModule().getModuleScope();
    final var resolver = new TypeDefResolver(scope);
    final var messagePrefix = "Directive '" + resolutionDirective + "', specifically: '";

    try {

      final var resolved = resolver.typeDefToSymbol(resolutionDirective.getAdditionalName());

      resolved.ifPresentOrElse(additionalSymbol -> {
        if (symbol == additionalSymbol) {
          //Cannot extend self!
          errorListener.directiveError(resolutionDirective.getDirectiveToken(),
              messagePrefix + resolutionDirective.getSymbolName()
                  + "' == '" + resolutionDirective.getAdditionalName() + "',",
              ErrorListener.SemanticClassification.CIRCULAR_HIERARCHY_DETECTED);
        }
        if (!checkHierarchy(symbol, additionalSymbol)) {
          errorListener.directiveError(resolutionDirective.getDirectiveToken(),
              messagePrefix + resolutionDirective.getAdditionalName() + "',",
              ErrorListener.SemanticClassification.DIRECTIVE_HIERARCHY_NOT_RESOLVED);
        }
      }, () -> errorListener.directiveError(resolutionDirective.getDirectiveToken(),
          messagePrefix + "'" + resolutionDirective.getAdditionalName() + "'",
          ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_NOT_RESOLVED));

    } catch (IllegalArgumentException exception) {
      //In the case of directives (as a debugging tool) we may get an exception if the developer
      //incorrectly uses @Resolve @NoResolve with generic types
      errorListener.directiveError(resolutionDirective.getDirectiveToken(), exception.getMessage(),
          ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT);
    }

  }

  private boolean checkHierarchy(final ISymbol theMainSymbol, final ISymbol theSuperSymbol) {

    if (theMainSymbol instanceof IAggregateSymbol asAggregate
        && theSuperSymbol instanceof IAggregateSymbol checkSuper) {
      return asAggregate.isImplementingInSomeWay(checkSuper);
    } else if (theMainSymbol instanceof FunctionSymbol asFunction
        && theSuperSymbol instanceof FunctionSymbol checkSuper) {
      return asFunction.isImplementingInSomeWay(checkSuper);
    }

    return false;
  }
}
