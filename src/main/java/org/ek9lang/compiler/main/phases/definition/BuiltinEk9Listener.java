package org.ek9lang.compiler.main.phases.definition;

import org.ek9lang.antlr.EK9BaseListener;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.core.exception.AssertValue;

/**
 * For builtin types (defined in builtin.ek9) a light weight shallow visit is required.
 * That's what this visitor is for.
 */
public class BuiltinEk9Listener extends EK9BaseListener {

  private final ParsedModule parsedModule;

  public BuiltinEk9Listener(ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
  }

  @Override
  public void enterModuleDeclaration(EK9Parser.ModuleDeclarationContext ctx) {
    var moduleName = ctx.dottedName().getText();
    AssertValue.checkNotEmpty("Module Name must be defined", moduleName);
    AssertValue.checkTrue("Module Name mismatch", moduleName.equals(parsedModule.getModuleName()));
  }

  @Override
  public void enterClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var moduleScope = parsedModule.getModuleScope();
    var className = ctx.Identifier().getText();
    var newTypeSymbol = new AggregateSymbol(className, moduleScope);
    moduleScope.define(newTypeSymbol);

  }


}
