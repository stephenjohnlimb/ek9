package org.ek9lang.compiler.main.phases.definition;

import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.SymbolFactory;
import org.ek9lang.compiler.main.phases.listeners.AbstractEK9PhaseListener;
import org.ek9lang.compiler.symbol.ConstantSymbol;
import org.ek9lang.compiler.symbol.IScopedSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.LocalScope;
import org.ek9lang.compiler.symbol.support.SymbolChecker;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Just go through and define the symbols and scopes putting into the ParsedModule against the appropriate context.
 * Also define the symbol in the parent scope (though its type is still to be determined - see next phase).
 * Check for duplicate variables/types/classes/constants and functions across parsed modules for the same module.
 * So at the end of phase one - we will have the names of Classes/Types/Functions etc recorded even though they
 * won't be fully defined.
 * This will also include the definition of generic/template types. But we cannot fully turn those into concrete
 * versions until all modules have been parsed by phase1. Only then will all types at least be recorded.
 * Now that if symbols are already defined we might need to push a dummy item on so the rest of the parsing can
 * take place with push and pop of what's on the stack. Else we get out of sync.
 * So yes we need to record errors and not put the named item on, but we need to put something in there to deal
 * with the pushing and popping on thr stack, because the rest of the code might be OK.
 */
public class DefinitionPhase1Listener extends AbstractEK9PhaseListener {

  /**
   * For creating new symbols during definition.
   */
  private final SymbolFactory symbolFactory;

  /**
   * Used mainly for checking for duplicate symbols in scopes.
   */
  private final SymbolChecker symbolChecker;

  /**
   * First phase after parsing. Define symbols and infer types where possible.
   */
  public DefinitionPhase1Listener(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                  ParsedModule parsedModule) {
    super(compilableProgramAccess, parsedModule);
    this.symbolChecker = new SymbolChecker(parsedModule.getSource().getErrorListener());
    this.symbolFactory = new SymbolFactory(parsedModule);
  }

  // Now we hook into the ANTLR listener events - lots of them!

  @Override
  public void enterModuleDeclaration(EK9Parser.ModuleDeclarationContext ctx) {
    var moduleName = ctx.dottedName().getText();
    //This is an assertion - because it is not an error in the developers work - but in this compiler.
    AssertValue.checkNotEmpty("Module Name must be defined", moduleName);
    AssertValue.checkTrue("Module Name mismatch", moduleName.equals(getParsedModule().getModuleName()));

    //Take not at module level if implementation is external - we'd expect no bodies.
    getParsedModule().setExternallyImplemented(ctx.EXTERN() != null);
  }

  @Override
  public void enterConstantDeclaration(EK9Parser.ConstantDeclarationContext ctx) {
    ParseTree constantCtx = ctx.Identifier();

    ConstantSymbol constant = new ConstantSymbol(constantCtx.getText(), false);
    constant.setSourceToken(ctx.start);
    constant.setParsedModule(Optional.ofNullable(getParsedModule()));

    if (!symbolChecker.errorsIfVariableSymbolAlreadyDefined(symbolAndScopeManagement.getTopScope(), constant)) {
      symbolAndScopeManagement.enterNewSymbol(constant, ctx);
    }

    super.enterConstantDeclaration(ctx);
  }

  @Override
  public void exitConstantDeclaration(EK9Parser.ConstantDeclarationContext ctx) {
    //Now because constants are and have to be quite simple we can work out the type
    //even in the def phase 1. That's because they can only be simple though.
    ConstantSymbol constant = (ConstantSymbol) getParsedModule().getRecordedSymbol(ctx);

    ISymbol constantValue = getParsedModule().getRecordedSymbol(ctx.constantInitialiser());
    AssertValue.checkNotNull("Need to be able to access the type of the constant.", constantValue);
    //So this constant will be the same type.
    constant.setType(constantValue.getType());
    //Mark as referenced as they are public and might not be used 'yet'.
    constant.setReferenced(true);
    super.exitConstantDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    final var newTypeSymbol = symbolFactory.newTrait(ctx);
    checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    final var newTypeSymbol = symbolFactory.newClass(ctx);
    checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    super.enterClassDeclaration(ctx);
  }

  @Override
  public void enterMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    //Now this is used quite widely in the grammar, the parent context is key here
    if (ctx.getParent() instanceof EK9Parser.ProgramBlockContext) {
      processProgramDeclaration(ctx);
    } else if (ctx.getParent() instanceof EK9Parser.AggregatePartsContext) {
      processMethodDeclaration(ctx);
    } else {
      throw new CompilerException("MPV implementation!");
    }

    super.enterMethodDeclaration(ctx);
  }

  @Override
  public void enterFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    final var function = symbolFactory.newFunction(ctx);
    checkAndDefineScopedSymbol(function, ctx);
    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    final var newTypeSymbol = symbolFactory.newRecord(ctx);
    checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    super.enterRecordDeclaration(ctx);
  }

  private void processProgramDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    var program = symbolFactory.newProgram(ctx, currentScope);

    checkAndDefineScopedSymbol(program, ctx);
  }

  /**
   * Checks for duplicate names symbols in the current parsedModule scope.
   * Adds the new scoped symbol or adds a dummy just to ensure parse will continue
   * with scope stack.
   */
  private void checkAndDefineScopedSymbol(final IScopedSymbol symbol, final ParseTree node) {
    final var moduleScope = getParsedModule().getModuleScope();
    if (!symbolChecker.errorsIfVariableSymbolAlreadyDefined(moduleScope, symbol)) {
      symbolAndScopeManagement.defineScopedSymbol(symbol, node);
    } else {
      symbolAndScopeManagement.recordScopeForStackConsistency(new LocalScope(moduleScope), node);
    }
  }

  private void processMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();

    //just for now put a local scope in and do the methods tomorrow!
    symbolAndScopeManagement.recordScopeForStackConsistency(new LocalScope(currentScope), ctx);
  }

  private ConstantSymbol recordConstant(ParseTree ctx, Token start, String typeName) {
    //Lets account for the optional '-' on some literals
    String literalText = ctx.getChild(0).getText();
    if (ctx.getChildCount() == 2) {
      literalText += ctx.getChild(1).getText();
    }
    ConstantSymbol literal = symbolFactory.newLiteral(start, literalText);
    literal.setType(symbolAndScopeManagement.getTopScope().resolve(new TypeSymbolSearch(typeName)));
    symbolAndScopeManagement.enterNewLiteral(literal, ctx);
    return literal;
  }
}
