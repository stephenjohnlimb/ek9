package org.ek9lang.compiler.main.directives;

import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.DirectiveType;
import org.ek9lang.compiler.support.JustTypeDef;
import org.ek9lang.compiler.support.ResolutionDirective;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Just checks if there are any directives that relate to @Implements in the parsed module and checks the
 * resolution through the symbol and its hierarchy.
 */
public class ImplementsDirectiveListener extends ResolvedDirectiveListener {

  @Override
  public void accept(final CompilationEvent compilationEvent) {
    if (compilationEvent.parsedModule() != null) {
      //Only interested in extends directives for this phase.
      var directives =
          compilationEvent.parsedModule().getDirectives(DirectiveType.Implements, compilationEvent.phase());
      if (!directives.isEmpty()) {
        processDirectives(compilationEvent, directives);
      }
    }
  }

  @Override
  protected void symbolMatch(final CompilationEvent compilationEvent, ResolutionDirective resolutionDirective,
                             ISymbol symbol) {

    //Check the types and the like.
    super.symbolMatch(compilationEvent, resolutionDirective, symbol);

    var errorListener = compilationEvent.source().getErrorListener();
    var scope = compilationEvent.parsedModule().getModuleScope();
    JustTypeDef resolver = new JustTypeDef(scope);

    try {
      var resolved = resolver.typeDefToSymbol(resolutionDirective.getAdditionalName());
      resolved.ifPresentOrElse(additionalSymbol -> {

        if (symbol == additionalSymbol) {
          //Cannot extend self!
          errorListener.directiveError(resolutionDirective.getDirectiveToken(),
              "'" + resolutionDirective.getSymbolName() + "' == '" + resolutionDirective.getAdditionalName() + "'",
              ErrorListener.SemanticClassification.CANNOT_EXTEND_IMPLEMENT_ITSELF);

        }
        if (!checkHierarchy(symbol, additionalSymbol)) {
          errorListener.directiveError(resolutionDirective.getDirectiveToken(),
              "'" + resolutionDirective.getAdditionalName() + "'",
              ErrorListener.SemanticClassification.DIRECTIVE_HIERARCHY_NOT_RESOLVED);
        }
      }, () -> errorListener.directiveError(resolutionDirective.getDirectiveToken(),
          "'" + resolutionDirective.getAdditionalName() + "'",
          ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_NOT_RESOLVED));
    } catch (IllegalArgumentException exception) {
      //In the case of directives (as a debugging tool) we may get an exception if the developer
      //incorrectly uses @Resolve @NoResolve with generic types
      errorListener.directiveError(resolutionDirective.getDirectiveToken(), exception.getMessage(),
          ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT);
    }
  }

  private boolean checkHierarchy(final ISymbol theMainSymbol, final ISymbol theSuperSymbol) {
    boolean implementsInSomeWay = true;

    if (theMainSymbol instanceof IAggregateSymbol asAggregate
        && theSuperSymbol instanceof IAggregateSymbol checkSuper) {
      implementsInSomeWay = asAggregate.isImplementingInSomeWay(checkSuper);
    } else if (theMainSymbol instanceof FunctionSymbol asFunction
        && theSuperSymbol instanceof FunctionSymbol checkSuper) {
      implementsInSomeWay = asFunction.isImplementingInSomeWay(checkSuper);
    }
    return implementsInSomeWay;
  }
}
