package org.ek9lang.compiler.phase1;

import static org.ek9lang.compiler.support.AggregateFactory.EK9_BITS;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_BOOLEAN;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_CHARACTER;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_COLOUR;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_DATE;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_DATETIME;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_DIMENSION;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_DURATION;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_FLOAT;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_INTEGER;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_MILLISECOND;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_MONEY;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_PATH;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_REGEX;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_RESOLUTION;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_STRING;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_TIME;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_VERSION;

import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.AbstractEK9PhaseListener;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.support.ResolveOrDefineExplicitParameterizedType;
import org.ek9lang.compiler.support.ResolveOrDefineTypeDef;
import org.ek9lang.compiler.support.SymbolChecker;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.support.TextLanguageExtraction;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.ConstantSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ICanCaptureVariables;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.LocalScope;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.StackConsistencyScope;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

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
 * with the pushing and popping on the stack, because the rest of the code might be OK.
 * There are a number of very simple 'early' checks used in this listener, the idea is a fail to compile as early
 * as possible. It could be argued that you should do all the checks once you have an IR.
 * My thought is that as soon as you can detect an error you should report it in the earliest phase as possible.
 */
final class DefinitionListener extends AbstractEK9PhaseListener {

  private final SymbolFactory symbolFactory;
  private final BlockScopeName blockScopeName = new BlockScopeName();
  private final SymbolChecker symbolChecker;
  private final TextLanguageExtraction textLanguageExtraction;
  private final EmitUnreachableStatementError emitUnreachableStatementError;
  private final ValidMethodOrError validMethodOrError;
  private final ProcessSyntheticReturn processSyntheticReturn;
  private final AppropriateFunctionBodyOrError appropriateFunctionBodyOrError;
  private final ImplementationPresentOrError implementationPresentOrError;
  private final ThisAndSuperAssignmentOrError thisAndSuperAssignmentOrError;
  private final ProcessVariableOnlyOrError processVariableOnlyOrError;
  private final VariableDeclarationOrError variableDeclarationOrError;
  private final DynamicVariableCaptureOrError dynamicVariableCaptureOrError;
  private final DynamicClassDeclarationOrError dynamicClassDeclarationOrError;
  private final ConsistentNamedParametersOrError consistentNamedParametersOrError;
  private final NormalTerminationOrError normalTerminationOrError;
  private final NotABooleanLiteralOrError notABooleanLiteralOrError;
  private final ApplicationOnMethodOrError applicationOnMethodOrError;
  private final ValidParameterisedTypeOrError validParameterisedTypeOrError;
  private final ResolveOrDefineTypeDef resolveOrDefineTypeDef;
  private final ResolveOrDefineExplicitParameterizedType resolveOrDefineExplicitParameterizedType;
  private final ValidPreFlowAndReturnOrError validPreFlowAndReturnOrError;
  private String currentTextBlockLanguage;

  /**
   * First phase after parsing. Define symbols and infer types where possible.
   * Uses a symbol factory to actually create the appropriate symbols.
   */
  DefinitionListener(ParsedModule parsedModule) {
    super(parsedModule);
    var errorListener = parsedModule.getSource().getErrorListener();
    symbolChecker = new SymbolChecker(errorListener);
    symbolFactory = new SymbolFactory(parsedModule);

    emitUnreachableStatementError = new EmitUnreachableStatementError(errorListener);
    textLanguageExtraction = new TextLanguageExtraction(errorListener);
    validMethodOrError = new ValidMethodOrError(symbolsAndScopes, errorListener);
    processSyntheticReturn = new ProcessSyntheticReturn(symbolsAndScopes, symbolFactory, errorListener);
    appropriateFunctionBodyOrError = new AppropriateFunctionBodyOrError(symbolsAndScopes, errorListener);
    implementationPresentOrError = new ImplementationPresentOrError(errorListener);

    thisAndSuperAssignmentOrError = new ThisAndSuperAssignmentOrError(errorListener);
    processVariableOnlyOrError = new ProcessVariableOnlyOrError(errorListener);
    variableDeclarationOrError = new VariableDeclarationOrError(symbolsAndScopes, errorListener);
    dynamicClassDeclarationOrError = new DynamicClassDeclarationOrError(symbolsAndScopes, errorListener);
    dynamicVariableCaptureOrError = new DynamicVariableCaptureOrError(errorListener);
    consistentNamedParametersOrError = new ConsistentNamedParametersOrError(errorListener);
    normalTerminationOrError = new NormalTerminationOrError(errorListener);
    notABooleanLiteralOrError = new NotABooleanLiteralOrError(errorListener);

    applicationOnMethodOrError =
        new ApplicationOnMethodOrError(symbolsAndScopes, errorListener);

    validParameterisedTypeOrError = new ValidParameterisedTypeOrError(errorListener);

    resolveOrDefineTypeDef = new ResolveOrDefineTypeDef(symbolsAndScopes, symbolFactory, errorListener, false);

    resolveOrDefineExplicitParameterizedType =
        new ResolveOrDefineExplicitParameterizedType(symbolsAndScopes, symbolFactory, errorListener, false);

    validPreFlowAndReturnOrError = new ValidPreFlowAndReturnOrError(symbolsAndScopes, errorListener);

  }

  // Now we hook into the ANTLR listener events - lots of them!
  //This is the main/primary and ideally only purpose of this class.

  //I've tried to group the events logically. Also pulled out most processing to other 'factories' where possible.

  @Override
  public void enterModuleDeclaration(final EK9Parser.ModuleDeclarationContext ctx) {

    final var moduleName = ctx.dottedName().getText();
    //This is an assertion - because it is not an error in the developers work - but in this compiler.
    AssertValue.checkNotEmpty("Module Name must be defined", moduleName);
    AssertValue.checkTrue("Module Name mismatch", moduleName.equals(getParsedModule().getModuleName()));

    //Take note at module level if implementation is external - we'd expect no bodies.
    getParsedModule().setExternallyImplemented(ctx.EXTERN() != null);

  }

  @Override
  public void exitDirective(final EK9Parser.DirectiveContext ctx) {

    final var directive = symbolFactory.newDirective(ctx);
    if (directive != null) {
      //i.e. the configuration by the developer was valid.
      getParsedModule().recordDirective(directive);
    }

    super.exitDirective(ctx);

  }

  @Override
  public void enterPackageBlock(final EK9Parser.PackageBlockContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newPackage(ctx), ctx);
    super.enterPackageBlock(ctx);

  }

  @Override
  public void exitPackageBlock(final EK9Parser.PackageBlockContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateSymbol packageSymbol) {
      //Now let's manipulate those properties that have been added
      packageSymbol.getProperties().forEach(prop -> {
        final VariableSymbol symbol = (VariableSymbol) prop;
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
  public void enterProgramBlock(final EK9Parser.ProgramBlockContext ctx) {

    //Nothing to do here, check enterMethodDeclaration - this checks parent and processes
    //a method as if it were a method (which in a way it is).
    super.enterProgramBlock(ctx);

  }

  @Override
  public void enterMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    //Now this (method) is used quite widely in the grammar, the parent context is key here
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
  public void exitMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    //Can be null if during definition 'enter' it was a duplicate method
    if (symbolsAndScopes.getTopScope() instanceof MethodSymbol method) {
      validMethodOrError.accept(method, ctx);
      if ((ctx.operationDetails() == null || (ctx.operationDetails() != null
          && ctx.operationDetails().returningParam() == null))
          && !method.isConstructor()) {
        processSyntheticReturn.accept(method);
      }
    }

    super.exitMethodDeclaration(ctx);

  }

  @Override
  public void enterOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    final var currentScope = symbolsAndScopes.getTopScope();
    MethodSymbol newOperator = null;
    if (currentScope instanceof IAggregateSymbol aggregate) {
      //Now attempt to make an operator, but other checks could result in this being null.
      newOperator = symbolFactory.newOperator(ctx, aggregate);
    }

    if (newOperator != null) {
      //Can define directly because overloaded methods are allowed.
      symbolsAndScopes.defineScopedSymbol(newOperator, ctx);
    } else {
      symbolsAndScopes.recordScopeForStackConsistency(new StackConsistencyScope(currentScope), ctx);
    }

    super.enterOperatorDeclaration(ctx);

  }

  @Override
  public void exitOperatorDeclaration(EK9Parser.OperatorDeclarationContext ctx) {

    final var currentScope = symbolsAndScopes.getTopScope();
    //Can be null if during definition 'enter' it was a duplicate method
    if (currentScope instanceof MethodSymbol method
        && ctx.operationDetails() != null
        && ctx.operationDetails().returningParam() == null) {
      processSyntheticReturn.accept(method);
    }

    super.exitOperatorDeclaration(ctx);

  }

  //See later phases for checking operators - because type checking is needed.

  @Override
  public void enterDefaultOperator(final EK9Parser.DefaultOperatorContext ctx) {

    final var currentScope = symbolsAndScopes.getTopScope();
    boolean applied = false;
    if (currentScope instanceof IAggregateSymbol aggregate) {
      applied = symbolFactory.addMissingDefaultOperators(ctx, aggregate);
    }

    if (!applied) {
      symbolsAndScopes.recordScopeForStackConsistency(new StackConsistencyScope(currentScope), ctx);
    }

    super.enterDefaultOperator(ctx);

  }

  @Override
  public void enterFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newFunction(ctx), ctx);
    super.enterFunctionDeclaration(ctx);

  }

  @Override
  public void exitFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    final var functionSymbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);

    if (functionSymbol != null) {
      appropriateFunctionBodyOrError.accept(functionSymbol, ctx.operationDetails());
    }

    //There is returning so use a return of Void, might be null if we had duplicate names.
    if (functionSymbol != null && ctx.operationDetails() != null && ctx.operationDetails().returningParam() == null) {
      processSyntheticReturn.accept(functionSymbol);
    }

    super.exitFunctionDeclaration(ctx);

  }

  @Override
  public void enterRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newRecord(ctx), ctx);
    super.enterRecordDeclaration(ctx);

  }

  @Override
  public void enterTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newTrait(ctx), ctx);
    super.enterTraitDeclaration(ctx);

  }

  @Override
  public void enterClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newClass(ctx), ctx);
    super.enterClassDeclaration(ctx);

  }

  @Override
  public void enterComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newComponent(ctx), ctx);
    super.enterComponentDeclaration(ctx);

  }

  @Override
  public void enterTextBlock(final EK9Parser.TextBlockContext ctx) {

    currentTextBlockLanguage = textLanguageExtraction.apply(ctx.stringLit());
    super.enterTextBlock(ctx);

  }

  @Override
  public void exitTextBlock(final EK9Parser.TextBlockContext ctx) {

    currentTextBlockLanguage = null;
    super.exitTextBlock(ctx);

  }

  @Override
  public void enterTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newText(ctx, currentTextBlockLanguage), ctx);
    super.enterTextDeclaration(ctx);

  }

  @Override
  public void enterTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    //Can define directly because overloaded methods are allowed.
    symbolsAndScopes.defineScopedSymbol(
        symbolFactory.newTextBody(ctx, symbolsAndScopes.getTopScope()), ctx);

    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void exitTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {
    //We have to do this on the exit side, so that the whole method body params are defined
    //That way it can be cloned and added to the conceptual base.

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof MethodSymbol textMethodSymbol) {
      symbolFactory.ensureTextBodyIsInSuper(textMethodSymbol);
    }

    super.exitTextBodyDeclaration(ctx);

  }

  @Override
  public void enterServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newService(ctx), ctx);
    super.enterServiceDeclaration(ctx);

  }

  @Override
  public void enterServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    //Duplicates are checked in later phase (EXPLICIT_TYPE_SYMBOL_DEFINITION).
    symbolsAndScopes.defineScopedSymbol(
        symbolFactory.newServiceOperation(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterServiceOperationDeclaration(ctx);

  }

  @Override
  public void enterApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    checkAndDefineModuleScopedSymbol(symbolFactory.newApplication(ctx), ctx);
    super.enterApplicationDeclaration(ctx);

  }

  @Override
  public void enterDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    final var enclosingMainTypeOrFunction = symbolsAndScopes.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);
    enclosingMainTypeOrFunction.ifPresentOrElse(scope -> {
      final var newTypeSymbol = symbolFactory.newDynamicClass((IScopedSymbol) scope, ctx);
      checkAndDefineDynamicModuleScopedSymbol(newTypeSymbol, ctx);
      dynamicClassDeclarationOrError.accept(ctx);
    }, () -> {
      throw new CompilerException("Compiler error dynamic class must be contained");
    });

    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    final var enclosingMainTypeOrFunction = symbolsAndScopes.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);
    enclosingMainTypeOrFunction.ifPresentOrElse(scope -> {
      final var newTypeSymbol = symbolFactory.newDynamicFunction((IScopedSymbol) scope, ctx);
      checkAndDefineDynamicModuleScopedSymbol(newTypeSymbol, ctx);
    }, () -> {
      throw new CompilerException("Compiler error dynamic function must be contained");
    });

    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    final var currentScope = symbolsAndScopes.getTopScope();
    dynamicVariableCaptureOrError.accept(ctx);

    if (currentScope instanceof ICanCaptureVariables canCaptureVariables) {
      final var enclosingBlockScope = symbolsAndScopes.traverseBackUpStack(IScope.ScopeType.BLOCK);
      AssertValue.checkTrue("Compiler error expecting block scope", enclosingBlockScope.isPresent());
      final var newScope = symbolFactory.newDynamicVariableCapture(canCaptureVariables, enclosingBlockScope.get());
      newScope.setOpenToEnclosingScope(true);
      symbolsAndScopes.enterNewScope(newScope, ctx);
    } else {
      throw new CompilerException("Compiler error looking use dynamic variable but current scope is wrong");
    }

    super.enterDynamicVariableCapture(ctx);
  }

  @Override
  public void exitDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    if (symbolsAndScopes.getTopScope() instanceof CaptureScope captureScope) {
      captureScope.setOpenToEnclosingScope(false);
    }
    super.exitDynamicVariableCapture(ctx);

  }

  @Override
  public void enterTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    //It is also possible to forward declare template types - but I might be
    //able to get rid of that if I work hard at it.
    if (ctx.Identifier() != null) {
      checkAndDefineModuleScopedSymbol(symbolFactory.newType(ctx), ctx);
    }
    super.enterTypeDeclaration(ctx);

  }

  @Override
  public void enterEnumerationDeclaration(final EK9Parser.EnumerationDeclarationContext ctx) {

    //Now get the parent enumeration this enumeration items are to be defined in
    if (symbolsAndScopes.getRecordedSymbol(ctx.parent) instanceof AggregateSymbol enumerationSymbol) {
      symbolFactory.populateEnumeration(enumerationSymbol, ctx.Identifier());
    }
    super.enterEnumerationDeclaration(ctx);

  }

  @Override
  public void enterStreamStatement(final EK9Parser.StreamStatementContext ctx) {

    final var streamSymbol = symbolFactory.newStream(ctx).setType(symbolsAndScopes.getEk9Types().ek9Void());
    symbolsAndScopes.recordSymbol(streamSymbol, ctx);
    super.enterStreamStatement(ctx);

  }

  @Override
  public void enterStreamExpression(final EK9Parser.StreamExpressionContext ctx) {

    symbolsAndScopes.recordSymbol(symbolFactory.newStream(ctx), ctx);
    super.enterStreamExpression(ctx);

  }

  @Override
  public void enterStreamCat(final EK9Parser.StreamCatContext ctx) {

    symbolsAndScopes.recordSymbol(symbolFactory.newStreamCat(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterStreamCat(ctx);

  }

  @Override
  public void exitStreamSource(final EK9Parser.StreamSourceContext ctx) {

    //Either cat of for - there is no need to create and intermediate object here.
    final var contextToRecord = ctx.streamCat() != null ? ctx.streamCat() : ctx.streamFor();
    final var symbolToRecord = symbolsAndScopes.getRecordedSymbol(contextToRecord);
    symbolsAndScopes.recordSymbol(symbolToRecord, ctx);
    super.exitStreamSource(ctx);

  }

  @Override
  public void enterStreamFor(final EK9Parser.StreamForContext ctx) {

    symbolsAndScopes.recordSymbol(symbolFactory.newStreamFor(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterStreamFor(ctx);

  }

  @Override
  public void enterStreamPart(final EK9Parser.StreamPartContext ctx) {

    symbolsAndScopes.recordSymbol(
        symbolFactory.newStreamPart(ctx, symbolsAndScopes.getTopScope()), ctx);

    super.enterStreamPart(ctx);
  }

  @Override
  public void enterStreamStatementTermination(final EK9Parser.StreamStatementTerminationContext ctx) {

    symbolsAndScopes.recordSymbol(
        symbolFactory.newStreamTermination(ctx, ctx.op.getText(), symbolsAndScopes.getTopScope()), ctx);
    super.enterStreamStatementTermination(ctx);

  }

  @Override
  public void enterStreamExpressionTermination(final EK9Parser.StreamExpressionTerminationContext ctx) {

    symbolsAndScopes.recordSymbol(
        symbolFactory.newStreamTermination(ctx, ctx.op.getText(), symbolsAndScopes.getTopScope()), ctx);
    super.enterStreamExpressionTermination(ctx);

  }

  //Pipeline Part is not defined here, just reference the symbol being employed against the context in phase3.

  /**
   * Need to create a local scope for the if statement so that it is possible to detect
   * if all paths result in abnormal termination (i.e. Exception).
   * If this is the case then the statement can only result in an exception, so any statements
   * after it will be unreachable. Though it is not an issue for all paths in an if else to result
   * in abnormal termination - only if there are statements follow or a return in the method/function.
   */
  @Override
  public void enterIfStatement(final EK9Parser.IfStatementContext ctx) {

    final IScope outerScope = symbolsAndScopes.getTopScope();
    final var ifScope = new LocalScope("If-line-" + ctx.start.getLine(), outerScope);

    symbolsAndScopes.enterNewScope(ifScope, ctx);
    ctx.ifControlBlock().stream()
        .map(ifBlock -> ifBlock.preFlowAndControl().control)
        .forEach(notABooleanLiteralOrError);

    super.enterIfStatement(ctx);

  }

  @Override
  public void exitIfStatement(final EK9Parser.IfStatementContext ctx) {

    pullIfElseTerminationUp(ctx);

    //Now pop off the scope stack to get the containing scope and pull that result up.
    super.exitIfStatement(ctx);

    pullBlockTerminationUp(ctx);

  }

  /**
   * A couple of wrinkles with the switch because is can be used as a normal statement
   * But also as an expression. If used as an expression then it must have a return part.
   * If used as a statement then the return part is meaningless
   */
  @Override
  public void enterSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    symbolsAndScopes.enterNewScopedSymbol(
        symbolFactory.newSwitch(ctx, symbolsAndScopes.getTopScope()), ctx);
    notABooleanLiteralOrError.accept(ctx.preFlowAndControl().control);
    super.enterSwitchStatementExpression(ctx);

  }

  @Override
  public void exitSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    final var thisSwitchScope = symbolsAndScopes.getTopScope();
    if (thisSwitchScope != null) {
      pullSwitchCaseDefaultUp(ctx);

      if (ctx.returningParam() != null) {
        final var token = new Ek9Token(ctx.returningParam().LEFT_ARROW().getSymbol());
        normalTerminationOrError.accept(token, thisSwitchScope);
      }
      //It is not an error at this point, but in a wider set of statements could be
    }

    super.exitSwitchStatementExpression(ctx);

    if (thisSwitchScope != null) {
      //So pull up this termination type and then if any statements follow and error will be issued.
      pullBlockTerminationUp(ctx);
    }
    validPreFlowAndReturnOrError.accept(ctx.preFlowAndControl().preFlowStatement(), ctx.returningParam());
  }

  /**
   * Now push it on to stack and record against this context as scope and symbol.
   * So this try is the outer scope, the body of the try block also has its own instruction block scope.
   * Each of the catch blocks have their scope, the finally has its scope.
   * But the variables used with try with are available throughout all scopes as they are in the outer scope.
   * These are a bit like 'Java' 'try with' variables. Just like 'Java' 'close' methods are called if present.
   */
  @Override
  public void enterTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    symbolsAndScopes.enterNewScopedSymbol(
        symbolFactory.newTry(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterTryStatementExpression(ctx);

  }

  /**
   * A bit complex, because we need to check for normal termination on the
   * try instructions, the catch and the finally blocks.
   * Only if there is a returning variable is this an error.
   * But we do need to pull the termination status up in to the parent set of instructions.
   */
  @Override
  public void exitTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    final IScope thisTryScope = symbolsAndScopes.getTopScope();
    if (thisTryScope != null) {
      pullTryCatchFinallyUp(ctx);

      if (ctx.returningParam() != null) {
        final var token = new Ek9Token(ctx.returningParam().LEFT_ARROW().getSymbol());
        normalTerminationOrError.accept(token, thisTryScope);
      }
    }
    //It is not an error at this point, but in a wider set of statements could be

    super.exitTryStatementExpression(ctx);

    if (thisTryScope != null) {

      //So pull up this termination type and then if any statements follow and error will be issued.
      pullBlockTerminationUp(ctx);
    }

    validPreFlowAndReturnOrError.accept(ctx.preFlowStatement(), ctx.returningParam());
  }

  @Override
  public void enterCatchStatementExpression(final EK9Parser.CatchStatementExpressionContext ctx) {

    final IScope outerScope = symbolsAndScopes.getTopScope();
    final var catchScope = new LocalScope("Catch-line-" + ctx.start.getLine(), outerScope);
    symbolsAndScopes.enterNewScope(catchScope, ctx);
    super.enterCatchStatementExpression(ctx);

  }

  @Override
  public void exitCatchStatementExpression(final EK9Parser.CatchStatementExpressionContext ctx) {

    pullBlockTerminationUp(ctx.instructionBlock());
    super.exitCatchStatementExpression(ctx);

  }

  @Override
  public void enterFinallyStatementExpression(final EK9Parser.FinallyStatementExpressionContext ctx) {

    //Don't really need this but for symmetry makes sense - really will just be an instructionBlock.
    final IScope outerScope = symbolsAndScopes.getTopScope();
    final var catchScope = new LocalScope("Finally-line-" + ctx.start.getLine(), outerScope);
    symbolsAndScopes.enterNewScope(catchScope, ctx);

    super.enterFinallyStatementExpression(ctx);

  }

  @Override
  public void exitFinallyStatementExpression(final EK9Parser.FinallyStatementExpressionContext ctx) {

    pullBlockTerminationUp(ctx.block());
    super.exitFinallyStatementExpression(ctx);

  }

  /**
   * This is a key event as it in effect causes the current scope to fail with abnormal termination.
   * Note down that this scope has encountered an exception at this line number
   * but not if it already has encountered one.
   */
  @Override
  public void enterThrowStatement(final EK9Parser.ThrowStatementContext ctx) {

    final IScope currentScope = symbolsAndScopes.getTopScope();

    if (currentScope.isTerminatedNormally()) {
      currentScope.setEncounteredExceptionToken(new Ek9Token(ctx.getStart()));
    }

    super.enterThrowStatement(ctx);

  }

  @Override
  public void enterForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    symbolsAndScopes.enterNewScopedSymbol(
        symbolFactory.newForLoop(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterForStatementExpression(ctx);

  }

  @Override
  public void exitForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    final var scope = symbolsAndScopes.getTopScope();
    pullBlockTerminationUp(ctx.instructionBlock());

    if (!scope.isTerminatedNormally()) {
      emitUnreachableStatementError.accept(new Ek9Token(ctx.start), scope.getEncounteredExceptionToken());
    }

    final var preFlow = ctx.forLoop() != null ? ctx.forLoop().preFlowStatement() : ctx.forRange().preFlowStatement();
    validPreFlowAndReturnOrError.accept(preFlow, ctx.returningParam());
    super.exitForStatementExpression(ctx);

  }

  @Override
  public void enterWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    symbolsAndScopes.enterNewScopedSymbol(
        symbolFactory.newWhileLoop(ctx, symbolsAndScopes.getTopScope()), ctx);

    //EK9 does not allow a literal true in a while.
    notABooleanLiteralOrError.accept(ctx.control);
    super.enterWhileStatementExpression(ctx);

  }

  @Override
  public void exitWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    final var scope = symbolsAndScopes.getTopScope();
    pullBlockTerminationUp(ctx.instructionBlock());

    if (!scope.isTerminatedNormally()) {
      emitUnreachableStatementError.accept(new Ek9Token(ctx.start), scope.getEncounteredExceptionToken());
    }

    validPreFlowAndReturnOrError.accept(ctx.preFlowStatement(), ctx.returningParam());
    super.exitWhileStatementExpression(ctx);

  }

  @Override
  public void enterForLoop(final EK9Parser.ForLoopContext ctx) {

    notABooleanLiteralOrError.accept(ctx.expression());
    checkAndDefineSymbol(symbolFactory.newLoopVariable(ctx), ctx, false);
    super.enterForLoop(ctx);

  }

  @Override
  public void enterForRange(final EK9Parser.ForRangeContext ctx) {

    checkAndDefineSymbol(symbolFactory.newLoopVariable(ctx), ctx, false);
    super.enterForRange(ctx);

  }

  /**
   * A local scope is used to hold the returning parameter.
   * The argumentParam values are defined in the scope of the method/function etc.
   * These are the incoming parameters.
   * The returningParam is held outside of those and must be declared inside the
   * function/method (but not in the areas for incoming parameters), just as a normal
   * local variable in the main block.
   * Now the issue is that we must check that this returning variable name does not
   * collide with any of the incoming parameters, nor any of the variables the developer declares
   * in the main block.
   */
  @Override
  public void enterReturningParam(final EK9Parser.ReturningParamContext ctx) {

    final IScope scope = symbolsAndScopes.getTopScope();
    final LocalScope returningScope = new LocalScope("Returning Param", scope);
    symbolsAndScopes.enterNewScope(returningScope, ctx);

    super.enterReturningParam(ctx);

  }

  /**
   * A bit tricky or more not like many other methods.
   * This is because returns in ek9 are defined as part of the signature up front in terms of variable name and type.
   * This has the implication of the fact that named variable must be added to the relevant scope before any sort of
   * body processing. Be that just an instruction block, catch, finally, or case blocks.
   * This it to ensure that variables of the same name do not get declared at any point in those blocks.
   * So you want wait until the end of the method, switch/try to add the return. It must be up front as
   * sub blocks come in and out of scope within the main blocks. But we must stop duplicate named vars being used if
   * declared as a returning value.
   */
  @Override
  public void exitReturningParam(final EK9Parser.ReturningParamContext ctx) {

    //Now get back to the parent scope, function, method, try, switch etc.
    super.exitReturningParam(ctx);

    final var currentScope = symbolsAndScopes.getTopScope();

    final ParseTree child =
        ctx.variableDeclaration() != null ? ctx.variableDeclaration() : ctx.variableOnlyDeclaration();
    final var symbol = symbolsAndScopes.getRecordedSymbol(child);

    if (symbol instanceof VariableSymbol variableSymbol) {
      //Now also record the same symbol against this context for later use.
      symbolsAndScopes.recordSymbol(symbol, ctx);
      variableSymbol.setReturningParameter(true);
      if (!symbolChecker.errorsIfSymbolAlreadyDefined(currentScope, variableSymbol, true)) {
        if (currentScope instanceof MethodSymbol method) {
          method.setReturningSymbol(variableSymbol);
        } else if (currentScope instanceof FunctionSymbol function) {
          function.setReturningSymbol(variableSymbol);
        } else {
          currentScope.define(variableSymbol);
        }
      }
    }

  }

  @Override
  public void enterBlock(final EK9Parser.BlockContext ctx) {

    final var scopeName = blockScopeName.apply(new Ek9Token(ctx.start));
    final LocalScope instructionBlock = new LocalScope(scopeName, symbolsAndScopes.getTopScope());
    symbolsAndScopes.enterNewScope(instructionBlock, ctx);
    super.enterBlock(ctx);

  }

  @Override
  public void enterSingleStatementBlock(final EK9Parser.SingleStatementBlockContext ctx) {

    final var scopeName = blockScopeName.apply(new Ek9Token(ctx.start));
    final LocalScope instructionBlock = new LocalScope(scopeName, symbolsAndScopes.getTopScope());
    symbolsAndScopes.enterNewScope(instructionBlock, ctx);
    super.enterSingleStatementBlock(ctx);

  }

  /**
   * There is a returning parameter, so the instruction block must terminate normally.
   * i.e. not all paths in the instruction block can result in an Exception else there is no way
   * it is possible ever return anything.
   */
  @Override
  public void exitOperationDetails(final EK9Parser.OperationDetailsContext ctx) {

    //It is not mandatory to have either, typically one or the other or both.
    if (ctx.instructionBlock() != null) {
      pullBlockTerminationUp(ctx.instructionBlock());
    }

    if (ctx.returningParam() != null && ctx.instructionBlock() != null) {
      final var instructionBlockScope = symbolsAndScopes.getRecordedScope(ctx.instructionBlock());
      final var token = new Ek9Token(ctx.returningParam().LEFT_ARROW().getSymbol());
      normalTerminationOrError.accept(token, instructionBlockScope);
    }

    super.exitOperationDetails(ctx);
  }

  @Override
  public void exitBlock(final EK9Parser.BlockContext ctx) {

    pullBlockTerminationUp(ctx.instructionBlock());
    super.exitBlock(ctx);

  }

  /**
   * This is the main context for ek9 expressions and statements to be employed.
   * i.e. it is THE place for the developers ek9 code to be expressed.
   */
  @Override
  public void enterInstructionBlock(final EK9Parser.InstructionBlockContext ctx) {

    final var scopeName = blockScopeName.apply(new Ek9Token(ctx.start));
    final LocalScope instructionBlock = new LocalScope(scopeName, symbolsAndScopes.getTopScope());
    symbolsAndScopes.enterNewScope(instructionBlock, ctx);
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
  public void enterBlockStatement(final EK9Parser.BlockStatementContext ctx) {

    final IScope scope = symbolsAndScopes.getTopScope();
    if (!scope.isTerminatedNormally()) {
      emitUnreachableStatementError.accept(new Ek9Token(ctx.start), scope.getEncounteredExceptionToken());
    }

    super.enterBlockStatement(ctx);

  }

  /**
   * Just like the block statement it is possible to see if the scope has already been
   * marked as terminating abnormally, in which case the register statement won;t be reachable.
   */
  @Override
  public void enterRegisterStatement(final EK9Parser.RegisterStatementContext ctx) {

    final IScope scope = symbolsAndScopes.getTopScope();
    if (!scope.isTerminatedNormally()) {
      emitUnreachableStatementError.accept(new Ek9Token(ctx.start), scope.getEncounteredExceptionToken());
    }

    super.enterRegisterStatement(ctx);

  }

  @Override
  public void enterCall(final EK9Parser.CallContext ctx) {

    symbolsAndScopes.recordSymbol(
        symbolFactory.newCall(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterCall(ctx);

  }


  //For phase 1 no need to process objectAccessExpression, objectAccessStart, objectAccess, objectAccessType
  //But operationCall is processed here

  @Override
  public void enterOperationCall(final EK9Parser.OperationCallContext ctx) {

    symbolsAndScopes.recordSymbol(
        symbolFactory.newOperationCall(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterOperationCall(ctx);

  }

  //Not sure these List, Dict and DictEntry calls are needed.
  //If not remove them later.
  @Override
  public void enterList(final EK9Parser.ListContext ctx) {

    symbolsAndScopes.recordSymbol(symbolFactory.newList(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterList(ctx);

  }

  @Override
  public void enterDict(final EK9Parser.DictContext ctx) {

    symbolsAndScopes.recordSymbol(symbolFactory.newDict(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterDict(ctx);

  }

  @Override
  public void enterInitValuePair(final EK9Parser.InitValuePairContext ctx) {

    symbolsAndScopes.recordSymbol(symbolFactory.newDictEntry(ctx, symbolsAndScopes.getTopScope()), ctx);
    super.enterInitValuePair(ctx);

  }

  @Override
  public void exitParamExpression(final EK9Parser.ParamExpressionContext ctx) {

    consistentNamedParametersOrError.accept(ctx);
    super.exitParamExpression(ctx);

  }

  @Override
  public void enterParameterisedType(final EK9Parser.ParameterisedTypeContext ctx) {

    //Now as I've altered the grammar we need to add a rule to ensure that it has
    //valid structure in its context.
    validParameterisedTypeOrError.accept(ctx);
    resolveOrDefineExplicitParameterizedType.apply(ctx);
    super.enterParameterisedType(ctx);

  }

  @Override
  public void enterVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    final var variable = symbolFactory.newVariable(ctx);
    //Now it's not an error if we cannot resolve at this phase - but if these are built in types then we're all good.
    final var varType = resolveOrDefineTypeDef.apply(ctx.typeDef());
    variable.setType(varType);
    processVariableOnlyOrError.accept(ctx, variable);
    final var limitToBlocks = variable.isIncomingParameter() || variable.isPropertyField();
    checkAndDefineSymbol(variable, ctx, limitToBlocks);
    super.enterVariableOnlyDeclaration(ctx);

  }

  /**
   * Just a straight forward declaration of a variable.
   */
  @Override
  public void enterVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx) {

    final var variable = symbolFactory.newVariable(ctx);

    variableDeclarationOrError.accept(ctx);
    //Now it's not an error if we cannot resolve at this phase - but if these are built in types then we're all good.
    final var varType = resolveOrDefineTypeDef.apply(ctx.typeDef());
    variable.setType(varType);
    checkAndDefineSymbol(variable, ctx, false);
    super.enterVariableDeclaration(ctx);

  }

  /**
   * Now we have an assignment expression we can note that this variable was initialised.
   * For some simple literals we can work out the type early as well.
   */
  @Override
  public void exitVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx) {

    final VariableSymbol variable = (VariableSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    //Might not have been registered if detected as a duplicate.
    if (variable != null) {
      variable.setInitialisedBy(new Ek9Token(ctx.assignmentExpression().start));
      //Now it's not an error if we cannot resolve at this phase - but if these are built in types then we're all good.
      //But parameterised types cannot be resolved at all yet.
      //Nor can we resolve types through expressions.
      if (ctx.typeDef() != null && ctx.typeDef().identifierReference() != null) {
        final var varType = resolveOrDefineTypeDef.apply(ctx.typeDef());
        variable.setType(varType);
      } else if (ctx.assignmentExpression() != null && ctx.assignmentExpression().expression() != null
          && ctx.assignmentExpression().expression().primary() != null
          && ctx.assignmentExpression().expression().primary().literal() != null) {
        final var symbol =
            symbolsAndScopes.getRecordedSymbol(ctx.assignmentExpression().expression().primary().literal());
        variable.setType(symbol.getType());
      }
    }

    super.exitVariableDeclaration(ctx);
  }

  @Override
  public void exitAggregateProperty(final EK9Parser.AggregatePropertyContext ctx) {

    //Record the same symbol at the aggregate property context as well.
    if (ctx.variableDeclaration() != null) {
      final var field = symbolsAndScopes.getRecordedSymbol(ctx.variableDeclaration());
      if (field != null) {
        symbolsAndScopes.recordSymbol(field, ctx);
      }
    } else {
      var field = symbolsAndScopes.getRecordedSymbol(ctx.variableOnlyDeclaration());
      if (field != null) {
        symbolsAndScopes.recordSymbol(field, ctx);
      }
    }

    super.exitAggregateProperty(ctx);
  }

  /**
   * <pre>
   *   preFlowStatement
   *     : (variableDeclaration | assignmentStatement | guardExpression) (WITH|THEN)
   *     ;
   * </pre>
   * The declarations and assignments are always true, but the guard may result in an 'un-set' value.
   * In that case the result would be false. But in all cases just a boolean.
   */
  @Override
  public void exitPreFlowStatement(final EK9Parser.PreFlowStatementContext ctx) {

    //This always results in a boolean.
    final var expr = symbolFactory.newExpressionSymbol(new Ek9Token(ctx.start), ctx.getText(),
        Optional.of(symbolsAndScopes.getEk9Types().ek9Boolean()));
    symbolsAndScopes.recordSymbol(expr, ctx);

    super.exitPreFlowStatement(ctx);
  }

  @Override
  public void exitGuardExpression(final EK9Parser.GuardExpressionContext ctx) {

    //Checkin of the types in the guard assignment is done later - but this results in
    //and expression with a boolean return type.
    final var expr = symbolFactory.newExpressionSymbol(new Ek9Token(ctx.start), ctx.getText(),
        Optional.of(symbolsAndScopes.getEk9Types().ek9Boolean()));
    symbolsAndScopes.recordSymbol(expr, ctx);

    super.exitGuardExpression(ctx);
  }

  @Override
  public void exitAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx) {

    thisAndSuperAssignmentOrError.accept(ctx);
    super.enterAssignmentStatement(ctx);

  }

  @Override
  public void enterConstantDeclaration(final EK9Parser.ConstantDeclarationContext ctx) {

    symbolsAndScopes.enterNewConstant(symbolFactory.newConstant(ctx), ctx, symbolChecker);
    super.enterConstantDeclaration(ctx);

  }

  @Override
  public void exitConstantDeclaration(final EK9Parser.ConstantDeclarationContext ctx) {

    //Now because constants are and have to be quite simple we can work out the type
    //even in the def phase 1. That's because they can only be simple though.

    //Could be null if detected as duplicate.
    if (getParsedModule().getRecordedSymbol(ctx) instanceof ConstantSymbol constant) {
      //See exitConstantInitialiser below for how this is populated.
      final ISymbol constantValue = getParsedModule().getRecordedSymbol(ctx.constantInitialiser());
      AssertValue.checkNotNull("Need to be able to access the type of the constant.", constantValue);
      //So this constant will be the same type.
      constant.setType(constantValue.getType());
      //Mark as referenced as they are public and might not be used 'yet'.
      constant.setReferenced(true);
    }

    super.exitConstantDeclaration(ctx);

  }

  // OK down at the lower levels of defining literal

  /**
   * This is a very important exit, as it takes the literals defined and pulls them up.
   * So the same literal will now be recorded against a constant initialiser context.
   */
  @Override
  public void exitConstantInitialiser(final EK9Parser.ConstantInitialiserContext ctx) {

    final var literalSymbol = getParsedModule().getRecordedSymbol(ctx.literal());
    AssertValue.checkNotNull("Need to have literals resolved in phase1: " + ctx.getText(), literalSymbol);
    getParsedModule().recordSymbol(ctx, literalSymbol); //pass same symbol back up by recording on ctx.

    super.exitConstantInitialiser(ctx);

  }

  @Override
  public void enterIntegerLiteral(final EK9Parser.IntegerLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_INTEGER);

    super.enterIntegerLiteral(ctx);

  }

  @Override
  public void enterFloatingPointLiteral(final EK9Parser.FloatingPointLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_FLOAT);

    super.enterFloatingPointLiteral(ctx);

  }

  @Override
  public void enterBinaryLiteral(final EK9Parser.BinaryLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_BITS);

    super.enterBinaryLiteral(ctx);

  }

  @Override
  public void enterBooleanLiteral(final EK9Parser.BooleanLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_BOOLEAN);

    super.enterBooleanLiteral(ctx);

  }

  @Override
  public void enterCharacterLiteral(final EK9Parser.CharacterLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_CHARACTER);

    super.enterCharacterLiteral(ctx);

  }

  @Override
  public void enterStringLiteral(final EK9Parser.StringLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_STRING);

    super.enterStringLiteral(ctx);

  }

  @Override
  public void enterStringPart(final EK9Parser.StringPartContext ctx) {

    if (ctx.STRING_TEXT() != null) {
      //Then it is just a snip of TEXT
      final var literal = symbolFactory.newInterpolatedStringPart(ctx);
      symbolsAndScopes.recordSymbol(literal, ctx);
    } else {
      //Ah, so it is an expression part of an interpolated String
      final var expression = symbolFactory.newInterpolatedExpressionPart(ctx);
      symbolsAndScopes.recordSymbol(expression, ctx);
    }

    super.enterStringPart(ctx);

  }

  @Override
  public void enterTimeLiteral(final EK9Parser.TimeLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_TIME);
    super.enterTimeLiteral(ctx);

  }

  @Override
  public void enterDateLiteral(final EK9Parser.DateLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_DATE);
    super.enterDateLiteral(ctx);

  }

  @Override
  public void enterDateTimeLiteral(final EK9Parser.DateTimeLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_DATETIME);
    super.enterDateTimeLiteral(ctx);

  }

  @Override
  public void enterDurationLiteral(final EK9Parser.DurationLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_DURATION);
    super.enterDurationLiteral(ctx);

  }

  @Override
  public void enterMillisecondLiteral(final EK9Parser.MillisecondLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_MILLISECOND);
    super.enterMillisecondLiteral(ctx);

  }

  @Override
  public void enterDecorationDimensionLiteral(final EK9Parser.DecorationDimensionLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_DIMENSION);
    super.enterDecorationDimensionLiteral(ctx);

  }

  @Override
  public void enterDecorationResolutionLiteral(final EK9Parser.DecorationResolutionLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_RESOLUTION);
    super.enterDecorationResolutionLiteral(ctx);

  }

  @Override
  public void enterColourLiteral(final EK9Parser.ColourLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_COLOUR);
    super.enterColourLiteral(ctx);

  }

  @Override
  public void enterMoneyLiteral(final EK9Parser.MoneyLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_MONEY);
    super.enterMoneyLiteral(ctx);

  }

  @Override
  public void enterRegularExpressionLiteral(final EK9Parser.RegularExpressionLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_REGEX);
    super.enterRegularExpressionLiteral(ctx);

  }

  @Override
  public void enterVersionNumberLiteral(final EK9Parser.VersionNumberLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_VERSION);
    super.enterVersionNumberLiteral(ctx);

  }

  @Override
  public void enterPathLiteral(final EK9Parser.PathLiteralContext ctx) {

    recordConstant(ctx, ctx.start, EK9_PATH);
    super.enterPathLiteral(ctx);

  }

  private void processProgramDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    final var program = symbolFactory.newProgram(ctx);
    checkAndDefineModuleScopedSymbol(program, ctx);
    implementationPresentOrError.accept(ctx);
    //Should we now add in a "main program" method to hold the instruction blocks?
    final var newMainProgramMethod = symbolFactory.newMethod(ctx, "main", program);
    //But record against the operation details as the Program Aggregate is registered again main ctx
    //This will also push this method scope on to the stack. But as this is a method the developer
    //may have use abstract (incorrectly) and left the operation details out.
    if (ctx.operationDetails() != null) {
      symbolsAndScopes.defineScopedSymbol(newMainProgramMethod, ctx.operationDetails());
    } else {
      symbolsAndScopes.recordScopeForStackConsistency(new StackConsistencyScope(program), ctx);
    }

  }

  /**
   * Just for pure Symbols - not ScopedSymbols - checks for duplicate symbols just within
   * the current scope.
   */
  private void checkAndDefineSymbol(final ISymbol symbol, final ParseTree node, final boolean limitToBlock) {

    if (!symbolChecker.errorsIfSymbolAlreadyDefined(symbolsAndScopes.getTopScope(), symbol, limitToBlock)) {
      symbolsAndScopes.enterNewSymbol(symbol, node);
    }

  }

  /**
   * Checks for duplicate names symbols in the current parsedModule scope.
   * Adds the new scoped symbol or adds a dummy just to ensure parse will continue
   * with scope stack.
   */
  private void checkAndDefineModuleScopedSymbol(final IScopedSymbol symbol, final ParseTree node) {

    symbolsAndScopes.enterModuleScopedSymbol(symbol, node, symbolChecker);

  }

  private void checkAndDefineDynamicModuleScopedSymbol(final IScopedSymbol symbol, final ParseTree node) {

    symbolsAndScopes.enterModuleScopedSymbol(symbol, node, symbolChecker);

  }

  private void processMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    applicationOnMethodOrError.accept(ctx);
    final var currentScope = symbolsAndScopes.getTopScope();
    if (currentScope instanceof IScopedSymbol scopedSymbol) {
      final var method = symbolFactory.newMethod(ctx, scopedSymbol);
      //Can define directly because overloaded methods are allowed.
      symbolsAndScopes.defineScopedSymbol(method, ctx);
    } else {
      symbolsAndScopes.recordScopeForStackConsistency(new StackConsistencyScope(currentScope), ctx);
    }

  }

  private void recordConstant(final ParseTree ctx, final Token start, final String typeName) {


    //Let's account for the optional '-' on some literals
    var literalText = ctx.getChild(0).getText();
    if (ctx.getChildCount() == 2) {
      literalText += ctx.getChild(1).getText();
    }

    //Now this type should be resolved as it is passed in and is a built-in type.
    final var scope = symbolsAndScopes.getTopScope();
    final var resolvedType = scope.resolve(new TypeSymbolSearch(typeName));

    //Make the literal
    final var literal = symbolFactory.newLiteral(new Ek9Token(start), literalText);

    var source = literal.getSourceToken().getSourceName();
    var line = literal.getSourceToken().getLine();
    var msg = "Type of constant for '"
        + literal
        + "' should have resolved in '"
        + scope.getFriendlyScopeName()
        + "' source "
        + source
        + " on line "
        + line;

    AssertValue.checkTrue(msg, resolvedType.isPresent());
    literal.setType(resolvedType);
    symbolsAndScopes.enterNewLiteral(literal, ctx);

  }

}
