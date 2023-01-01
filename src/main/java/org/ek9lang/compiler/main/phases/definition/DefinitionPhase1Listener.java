package org.ek9lang.compiler.main.phases.definition;

import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.UnreachableStatement;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.phases.listeners.AbstractEK9PhaseListener;
import org.ek9lang.compiler.main.rules.CheckProtectedServiceMethods;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.ConstantSymbol;
import org.ek9lang.compiler.symbol.ForSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.IScopedSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.LocalScope;
import org.ek9lang.compiler.symbol.ScopedSymbol;
import org.ek9lang.compiler.symbol.TrySymbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.ek9lang.compiler.symbol.support.SymbolChecker;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.compiler.symbol.support.TextLanguageExtraction;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Just go through and define the symbols and scopes putting into the ParsedModule against the appropriate context.
 * Also define the symbol in the parent scope (though its type is still to be determined - see next phase).
 * Check for duplicate variables/types/classes/constants and functions across parsed modules for the same module.
 * So at the end of phase one - we will have the names of Classes/Types/Functions etc. recorded even though they
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
   * Used for processing text blocks, so we can hold state of the current language the block is for.
   */
  private String currentTextBlockLanguage;

  /**
   * For creating new symbols during definition.
   */
  private final SymbolFactory symbolFactory;

  /**
   * Used mainly for checking for duplicate symbols in scopes.
   */
  private final SymbolChecker symbolChecker;

  /**
   * Extractor/checker of the language like 'en_GB'.
   */
  private final TextLanguageExtraction textLanguageExtraction;

  /**
   * Error if it is possible to detect that a statement is unreachable.
   */
  private final UnreachableStatement unreachableStatement;

  /**
   * First phase after parsing. Define symbols and infer types where possible.
   * Uses a symbol factory to actually create the appropriate symbols.
   */
  public DefinitionPhase1Listener(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                  ParsedModule parsedModule) {
    super(compilableProgramAccess, parsedModule);
    this.symbolChecker = new SymbolChecker(parsedModule.getSource().getErrorListener());
    this.symbolFactory = new SymbolFactory(parsedModule);
    this.unreachableStatement = new UnreachableStatement(parsedModule.getSource().getErrorListener());
    this.textLanguageExtraction = new TextLanguageExtraction(parsedModule.getSource().getErrorListener());
  }

  // Now we hook into the ANTLR listener events - lots of them!
  //This is the main/primary and ideally only purpose of this class.

  //I've tried to group the events logically. Also pulled out most processing to other 'factories' where possible.

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
  public void enterPackageBlock(EK9Parser.PackageBlockContext ctx) {
    var pack = symbolFactory.newPackage(ctx);
    checkAndDefineScopedSymbol(pack, ctx);
    super.enterPackageBlock(ctx);
  }

  @Override
  public void exitPackageBlock(EK9Parser.PackageBlockContext ctx) {
    var pack = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (pack instanceof AggregateSymbol packageSymbol) {
      //Now lets manipulate those properties that have been added
      packageSymbol.getProperties().forEach(prop -> {
        VariableSymbol symbol = (VariableSymbol) prop;
        //EK9 will be referencing these.
        symbol.setReferenced(true);
        symbol.setAggregatePropertyField(true);
        symbol.setPrivate(false);
      });
    }
    super.exitPackageBlock(ctx);
  }

  @SuppressWarnings("java:S1185")
  @Override
  public void enterProgramBlock(EK9Parser.ProgramBlockContext ctx) {
    //Nothing to do here, check enterMethodDeclaration - this checks parent and processes
    //a method as if it were a method (which in a way it is).
    super.enterProgramBlock(ctx);
  }

  @Override
  public void enterMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    //Now this is used quite widely in the grammar, the parent context is key here
    if (ctx.getParent() instanceof EK9Parser.ProgramBlockContext) {
      processProgramDeclaration(ctx);
    } else if (ctx.getParent() instanceof EK9Parser.AggregatePartsContext) {
      processMethodDeclaration(ctx);
    } else if (ctx.getParent() instanceof EK9Parser.ServiceDeclarationContext) {
      processMethodDeclaration(ctx);
    }
    super.enterMethodDeclaration(ctx);
  }

  @Override
  public void enterOperatorDeclaration(EK9Parser.OperatorDeclarationContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    if (currentScope instanceof IScopedSymbol scopedSymbol) {
      final var newTypeSymbol = symbolFactory.newOperator(ctx, scopedSymbol);
      //Can define directly because overloaded methods are allowed.
      symbolAndScopeManagement.defineScopedSymbol(newTypeSymbol, ctx);
    } else {
      symbolAndScopeManagement.recordScopeForStackConsistency(new LocalScope(currentScope), ctx);
    }
    super.enterOperatorDeclaration(ctx);
  }

  private void processProgramDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    var program = symbolFactory.newProgram(ctx);
    symbolAndScopeManagement.defineScopedSymbol(program, ctx);
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
  public void enterComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    final var newTypeSymbol = symbolFactory.newComponent(ctx);
    checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    super.enterComponentDeclaration(ctx);
  }

  @Override
  public void enterTextBlock(EK9Parser.TextBlockContext ctx) {
    currentTextBlockLanguage = textLanguageExtraction.apply(ctx.stringLit());

    super.enterTextBlock(ctx);
  }

  @Override
  public void exitTextBlock(EK9Parser.TextBlockContext ctx) {
    currentTextBlockLanguage = null;
    super.exitTextBlock(ctx);
  }

  @Override
  public void enterTextDeclaration(EK9Parser.TextDeclarationContext ctx) {
    final var newTypeSymbol = symbolFactory.newText(ctx, currentTextBlockLanguage);
    checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    super.enterTextDeclaration(ctx);
  }

  @Override
  public void enterTextBodyDeclaration(EK9Parser.TextBodyDeclarationContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    final var newTypeSymbol = symbolFactory.newTextBody(ctx, currentScope);
    //Can define directly because overloaded methods are allowed.
    symbolAndScopeManagement.defineScopedSymbol(newTypeSymbol, ctx);
    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void enterServiceDeclaration(EK9Parser.ServiceDeclarationContext ctx) {
    final var newTypeSymbol = symbolFactory.newService(ctx);
    checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    super.enterServiceDeclaration(ctx);
  }

  @Override
  public void exitServiceDeclaration(EK9Parser.ServiceDeclarationContext ctx) {
    var aggregateSymbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);

    var methodCheck = new CheckProtectedServiceMethods(getParsedModule().getSource().getErrorListener());
    methodCheck.accept(aggregateSymbol);
    super.exitServiceDeclaration(ctx);
  }

  @Override
  public void enterServiceOperationDeclaration(EK9Parser.ServiceOperationDeclarationContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    final var newTypeSymbol = symbolFactory.newServiceOperation(ctx, currentScope);
    checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    super.enterServiceOperationDeclaration(ctx);
  }

  @Override
  public void enterApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
    final var newTypeSymbol = symbolFactory.newApplication(ctx);
    checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    super.enterApplicationDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    //It is necessary to pull this scope symbol upto module level.
    final var newTypeSymbol = symbolFactory.newDynamicClass(ctx);
    final var moduleScope = getParsedModule().getModuleScope();
    if (!symbolChecker.errorsIfSymbolAlreadyDefined(moduleScope, newTypeSymbol, true)) {
      symbolAndScopeManagement.enterNewDynamicScopedSymbol(newTypeSymbol, ctx);
    } else {
      symbolAndScopeManagement.recordScopeForStackConsistency(new LocalScope(moduleScope), ctx);
    }
    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterTypeDeclaration(EK9Parser.TypeDeclarationContext ctx) {

    //It is also possible to forward declare template types - but I might be
    //able to get rid of that if I work hard at it.
    if (ctx.Identifier() != null) {
      final var newTypeSymbol = symbolFactory.newType(ctx);
      checkAndDefineScopedSymbol(newTypeSymbol, ctx);
    }
    super.enterTypeDeclaration(ctx);
  }

  @Override
  public void enterEnumerationDeclaration(EK9Parser.EnumerationDeclarationContext ctx) {
    //Now get the parent enumeration this enumeration items are to be defined in
    var enumerationSymbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx.parent);
    symbolFactory.populateEnumeration(enumerationSymbol, ctx.Identifier());

    super.enterEnumerationDeclaration(ctx);
  }

  @Override
  public void enterStream(EK9Parser.StreamContext ctx) {
    final var newTypeSymbol = symbolFactory.newStream(ctx);
    symbolAndScopeManagement.recordSymbol(newTypeSymbol, ctx);
    super.enterStream(ctx);
  }

  @Override
  public void enterStreamCat(EK9Parser.StreamCatContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    final var newTypeSymbol = symbolFactory.newStreamCat(ctx, currentScope);
    symbolAndScopeManagement.recordSymbol(newTypeSymbol, ctx);
    super.enterStreamCat(ctx);
  }

  @Override
  public void enterStreamFor(EK9Parser.StreamForContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    final var newTypeSymbol = symbolFactory.newStreamFor(ctx, currentScope);
    symbolAndScopeManagement.recordSymbol(newTypeSymbol, ctx);
    super.enterStreamFor(ctx);
  }

  @Override
  public void enterStreamPart(EK9Parser.StreamPartContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    final var newStreamPartSymbol = symbolFactory.newStreamPart(ctx, currentScope);
    symbolAndScopeManagement.recordSymbol(newStreamPartSymbol, ctx);

    super.enterStreamPart(ctx);
  }

  @Override
  public void enterStreamTermination(EK9Parser.StreamTerminationContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    final var newStreamPartSymbol = symbolFactory.newStreamTermination(ctx, currentScope);
    symbolAndScopeManagement.recordSymbol(newStreamPartSymbol, ctx);

    super.enterStreamTermination(ctx);
  }

  /**
   * A couple of wrinkles with the switch because is can be used as a normal statement
   * But also as an expression. If used as an expression then it must have a return part.
   * If used as a statement then the return part is meaningless
   */
  @Override
  public void enterSwitchStatementExpression(EK9Parser.SwitchStatementExpressionContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    final var newSwitchSymbol = symbolFactory.newSwitch(ctx, currentScope);
    symbolAndScopeManagement.enterNewScopedSymbol(newSwitchSymbol, ctx);

    super.enterSwitchStatementExpression(ctx);
  }

  @Override
  public void enterForLoop(EK9Parser.ForLoopContext ctx) {
    pushNewForLoopScope(ctx);
    final var variable = symbolFactory.newLoopVariable(ctx);
    checkAndDefineSymbol(variable, ctx);
    super.enterForLoop(ctx);
  }

  @Override
  public void enterForRange(EK9Parser.ForRangeContext ctx) {
    pushNewForLoopScope(ctx);
    final var variable = symbolFactory.newLoopVariable(ctx);
    checkAndDefineSymbol(variable, ctx);
    super.enterForRange(ctx);
  }

  private void pushNewForLoopScope(final ParserRuleContext ctx) {
    IScope outerScope = symbolAndScopeManagement.getTopScope();
    var forBlock = new ForSymbol(outerScope);
    forBlock.setSourceToken(ctx.start);
    symbolAndScopeManagement.enterNewScope(forBlock, ctx);
  }

  @Override
  public void enterTryStatementExpression(EK9Parser.TryStatementExpressionContext ctx) {
    IScope outerScope = symbolAndScopeManagement.getTopScope();
    var tryScope = new TrySymbol(outerScope);
    tryScope.setSourceToken(ctx.start);
    symbolAndScopeManagement.enterNewScope(tryScope, ctx);
    super.enterTryStatementExpression(ctx);
  }

  @Override
  public void enterCatchStatementExpression(EK9Parser.CatchStatementExpressionContext ctx) {
    IScope outerScope = symbolAndScopeManagement.getTopScope();
    var catchScope = new LocalScope("Catch", outerScope);
    symbolAndScopeManagement.enterNewScope(catchScope, ctx);
    super.enterCatchStatementExpression(ctx);
  }

  @Override
  public void enterFinallyStatementExpression(EK9Parser.FinallyStatementExpressionContext ctx) {
    //Don't really need this but for symmetry makes sense - really will just be an instructionBlock.
    IScope outerScope = symbolAndScopeManagement.getTopScope();
    var catchScope = new LocalScope("Finally", outerScope);
    symbolAndScopeManagement.enterNewScope(catchScope, ctx);
    super.enterFinallyStatementExpression(ctx);
  }

  /**
   * This is the main context for ek9 expressions and statements to be employed.
   * i.e. it is THE place for the developers ek9 code to be expressed.
   */
  @Override
  public void enterInstructionBlock(EK9Parser.InstructionBlockContext ctx) {
    IScope scope = symbolAndScopeManagement.getTopScope();
    String parentScopeName = scope.getScopeName();
    if (scope instanceof ScopedSymbol scopedSymbol) {
      parentScopeName = scopedSymbol.getFriendlyScopeName();
    }
    LocalScope instructionBlock = new LocalScope(parentScopeName, scope);
    symbolAndScopeManagement.enterNewScope(instructionBlock, ctx);
    super.enterInstructionBlock(ctx);
  }

  /**
   * This is just a normal statement or variable declaration within a block.
   * But we actually do some early analysis here. We check if the scope this is in has been
   * marked as not terminating normally.
   * What this means is that this block has been marked because a known uncaught exception has
   * been issued.
   * This works on the principle that a previous block or try has issued an exception that was not caught.
   * As we listen to the events the previous blocks will have been entered and existed.
   * They may have marked the scope this statement is in as abnormally terminated.
   */
  @Override
  public void enterBlockStatement(EK9Parser.BlockStatementContext ctx) {
    //So this will be called on each block statement
    //if a previous block statement triggered an exception then
    //this block has effectively terminated and additional statements are pointless and un-reachable
    //We know this at compile time.
    IScope scope = symbolAndScopeManagement.getTopScope();
    if (!scope.isTerminatedNormally()) {
      unreachableStatement.accept(ctx.start, scope.getEncounteredExceptionToken());
    }

    super.enterBlockStatement(ctx);
  }

  @Override
  public void enterVariableOnlyDeclaration(EK9Parser.VariableOnlyDeclarationContext ctx) {
    final var variable = symbolFactory.newVariable(ctx);
    checkAndDefineSymbol(variable, ctx);
    super.enterVariableOnlyDeclaration(ctx);
  }

  /**
   * Just a straight forward declaration of a variable.
   * But just like Kotlin we can explicitly let the variable not be allocated memory.
   * This is done with the '?' suffix.
   */
  @Override
  public void enterVariableDeclaration(EK9Parser.VariableDeclarationContext ctx) {
    final var variable = symbolFactory.newVariable(ctx);

    checkAndDefineSymbol(variable, ctx);
    super.enterVariableDeclaration(ctx);
  }

  /**
   * Now we have an assignment expression we can note that this variable was initialised.
   */
  @Override
  public void exitVariableDeclaration(EK9Parser.VariableDeclarationContext ctx) {
    VariableSymbol varSymbol = (VariableSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    //Might not have been registered if detected as a duplicate.
    if (varSymbol != null) {
      varSymbol.setInitialisedBy(ctx.assignmentExpression().start);
    }
    super.exitVariableDeclaration(ctx);
  }

  /**
   * Just for pure Symbols - not ScopedSymbols - checks for duplicate symbols just within
   * the current scope.
   */
  private void checkAndDefineSymbol(final ISymbol symbol, final ParseTree node) {
    IScope scope = symbolAndScopeManagement.getTopScope();
    if (!symbolChecker.errorsIfVariableSymbolAlreadyDefined(scope, symbol)) {
      symbolAndScopeManagement.enterNewSymbol(symbol, node);
    }
  }

  /**
   * Checks for duplicate names symbols in the current parsedModule scope.
   * Adds the new scoped symbol or adds a dummy just to ensure parse will continue
   * with scope stack.
   */
  private void checkAndDefineScopedSymbol(final IScopedSymbol symbol, final ParseTree node) {
    final var moduleScope = getParsedModule().getModuleScope();
    if (!symbolChecker.errorsIfSymbolAlreadyDefined(moduleScope, symbol, true)) {
      symbolAndScopeManagement.defineScopedSymbol(symbol, node);
    } else {
      symbolAndScopeManagement.recordScopeForStackConsistency(new LocalScope(moduleScope), node);
    }
  }

  private void processMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    var currentScope = symbolAndScopeManagement.getTopScope();
    if (currentScope instanceof IScopedSymbol scopedSymbol) {
      final var newTypeSymbol = symbolFactory.newMethod(ctx, scopedSymbol);
      //Can define directly because overloaded methods are allowed.
      symbolAndScopeManagement.defineScopedSymbol(newTypeSymbol, ctx);
    } else {
      symbolAndScopeManagement.recordScopeForStackConsistency(new LocalScope(currentScope), ctx);
    }
  }

  private ConstantSymbol recordConstant(ParseTree ctx, Token start, String typeName) {
    //Lets account for the optional '-' on some literals
    String literalText = ctx.getChild(0).getText();
    if (ctx.getChildCount() == 2) {
      literalText += ctx.getChild(1).getText();
    }
    ConstantSymbol literal = symbolFactory.newLiteral(start, literalText);
    var resolvedType = symbolAndScopeManagement.getTopScope().resolve(new TypeSymbolSearch(typeName));
    var source = literal.getSourceToken().getTokenSource().getSourceName();
    var line = literal.getSourceToken().getTokenSource().getLine();
    AssertValue.checkTrue("Type of constant for '"
            + literal
            + "' should have resolved in ["
            + source
            + "] on line "
            + line,
        resolvedType.isPresent());
    literal.setType(resolvedType);
    symbolAndScopeManagement.enterNewLiteral(literal, ctx);
    return literal;
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

    //See exitConstantInitialiser below for how this is populated.
    ISymbol constantValue = getParsedModule().getRecordedSymbol(ctx.constantInitialiser());
    AssertValue.checkNotNull("Need to be able to access the type of the constant.", constantValue);
    //So this constant will be the same type.
    constant.setType(constantValue.getType());
    //Mark as referenced as they are public and might not be used 'yet'.
    constant.setReferenced(true);
    super.exitConstantDeclaration(ctx);
  }
  // OK down at the lower levels of defining literal


  /**
   * This is a very important exit, as it takes the literals defined and pulls them up.
   * So the same literal will now be recorded against a constant initialiser context.
   */
  @Override
  public void exitConstantInitialiser(EK9Parser.ConstantInitialiserContext ctx) {
    ISymbol literalSymbol = getParsedModule().getRecordedSymbol(ctx.literal());
    AssertValue.checkNotNull("Need to have literals resolved in phase1: " + ctx.getText(), literalSymbol);
    getParsedModule().recordSymbol(ctx, literalSymbol); //pass same symbol back up by recording on ctx.

    super.exitConstantInitialiser(ctx);
  }

  @Override
  public void enterIntegerLiteral(EK9Parser.IntegerLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Integer");
    super.enterIntegerLiteral(ctx);
  }

  @Override
  public void enterFloatingPointLiteral(EK9Parser.FloatingPointLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Float");
    super.enterFloatingPointLiteral(ctx);
  }

  @Override
  public void enterBinaryLiteral(EK9Parser.BinaryLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Binary");
    super.enterBinaryLiteral(ctx);
  }

  @Override
  public void enterBooleanLiteral(EK9Parser.BooleanLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Boolean");
    super.enterBooleanLiteral(ctx);
  }

  @Override
  public void enterCharacterLiteral(EK9Parser.CharacterLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Character");
    super.enterCharacterLiteral(ctx);
  }

  @Override
  public void enterStringLiteral(EK9Parser.StringLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::String");
    super.enterStringLiteral(ctx);
  }

  @Override
  public void enterTimeLiteral(EK9Parser.TimeLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Time");
    super.enterTimeLiteral(ctx);
  }

  @Override
  public void enterDateLiteral(EK9Parser.DateLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Date");
    super.enterDateLiteral(ctx);
  }

  @Override
  public void enterDateTimeLiteral(EK9Parser.DateTimeLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::DateTime");
    super.enterDateTimeLiteral(ctx);
  }

  @Override
  public void enterDurationLiteral(EK9Parser.DurationLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Duration");
    super.enterDurationLiteral(ctx);
  }

  @Override
  public void enterMillisecondLiteral(EK9Parser.MillisecondLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Millisecond");
    super.enterMillisecondLiteral(ctx);
  }

  @Override
  public void enterDecorationDimensionLiteral(EK9Parser.DecorationDimensionLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Dimension");
    super.enterDecorationDimensionLiteral(ctx);
  }

  @Override
  public void enterDecorationResolutionLiteral(EK9Parser.DecorationResolutionLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Resolution");
    super.enterDecorationResolutionLiteral(ctx);
  }

  @Override
  public void enterColourLiteral(EK9Parser.ColourLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Colour");
    super.enterColourLiteral(ctx);
  }

  @Override
  public void enterMoneyLiteral(EK9Parser.MoneyLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Money");
    super.enterMoneyLiteral(ctx);
  }

  @Override
  public void enterRegularExpressionLiteral(EK9Parser.RegularExpressionLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::RegEx");
    super.enterRegularExpressionLiteral(ctx);
  }

  @Override
  public void enterVersionNumberLiteral(EK9Parser.VersionNumberLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Version");
    super.enterVersionNumberLiteral(ctx);
  }

  @Override
  public void enterPathLiteral(EK9Parser.PathLiteralContext ctx) {
    recordConstant(ctx, ctx.start, "org.ek9.lang::Path");
    super.enterPathLiteral(ctx);
  }
}
