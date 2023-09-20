package org.ek9lang.compiler.phase3;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ScopeStackConsistencyListener;
import org.ek9lang.compiler.support.ReturnTypeExtractor;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * This listener just deals with expressions and the types that result from expressions.
 * Note that the correct must be inplace for these expressions to 'reside in'.
 * See 'instructionBlock' and enter/exit in the 'ScopeStackConsistencyListener' and 'AbstractEK9PhaseListener'.
 * Now some of this may feel upside down, processing on 'exit' and from the 'bottom up'.
 */
abstract class ExpressionsListener extends ScopeStackConsistencyListener {

  protected final SymbolFactory symbolFactory;
  protected final ErrorListener errorListener;
  private final ReturnTypeExtractor returnTypeExtractor = new ReturnTypeExtractor();
  private final CheckValidThisOrSuper checkValidThisOrSuper;
  private final CheckValidPrimary checkValidPrimary;
  private final CheckValidIdentifierReference checkValidIdentifierReference;
  private final CheckValidExpression checkValidExpression;
  private final CheckInstructionBlockVariables checkInstructionBlockVariables;
  private final CheckAssignmentExpression checkAssignmentExpression;
  private final CheckAssignmentStatement checkAssignmentStatement;
  private final CheckAndTypeList checkAndTypeList;
  private final CheckAndTypeDict checkAndTypeDict;
  private final CheckValidCall checkValidCall;
  private final CheckRange checkRange;
  private final CheckForRange checkForRange;
  private final ResolveIdentifierOrError resolveIdentifierOrError;
  private final CheckVariableAssignmentDeclaration checkVariableAssignmentDeclaration;
  private final ResolveFieldOrError resolveFieldOrError;
  private final ResolveOperationCallOrError resolveOperationCallOrError;
  private final CheckPipelinePart checkPipelinePart;

  protected ExpressionsListener(ParsedModule parsedModule) {
    super(parsedModule);

    this.symbolFactory = new SymbolFactory(parsedModule);

    this.errorListener = parsedModule.getSource().getErrorListener();

    checkValidThisOrSuper =
        new CheckValidThisOrSuper(symbolAndScopeManagement, symbolFactory, errorListener);

    checkValidPrimary
        = new CheckValidPrimary(symbolAndScopeManagement, errorListener);

    checkValidIdentifierReference
        = new CheckValidIdentifierReference(symbolAndScopeManagement, errorListener);

    checkValidExpression
        = new CheckValidExpression(symbolAndScopeManagement, symbolFactory, errorListener);

    checkInstructionBlockVariables =
        new CheckInstructionBlockVariables(symbolAndScopeManagement, errorListener);

    checkAssignmentExpression =
        new CheckAssignmentExpression(symbolAndScopeManagement, errorListener);

    checkAssignmentStatement =
        new CheckAssignmentStatement(symbolAndScopeManagement, errorListener);

    checkValidCall =
        new CheckValidCall(symbolAndScopeManagement, symbolFactory, errorListener);

    checkAndTypeList =
        new CheckAndTypeList(symbolAndScopeManagement, symbolFactory, errorListener);

    checkAndTypeDict =
        new CheckAndTypeDict(symbolAndScopeManagement, symbolFactory, errorListener);

    checkPipelinePart =
        new CheckPipelinePart(symbolAndScopeManagement, errorListener);

    checkRange =
        new CheckRange(symbolAndScopeManagement, symbolFactory, errorListener);

    checkForRange =
        new CheckForRange(symbolAndScopeManagement, errorListener);

    resolveIdentifierOrError
        = new ResolveIdentifierOrError(symbolAndScopeManagement, errorListener);

    checkVariableAssignmentDeclaration
        = new CheckVariableAssignmentDeclaration(symbolAndScopeManagement, errorListener);

    resolveFieldOrError
        = new ResolveFieldOrError(symbolAndScopeManagement, errorListener);

    resolveOperationCallOrError
        = new ResolveOperationCallOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void exitIdentifierReference(EK9Parser.IdentifierReferenceContext ctx) {
    checkValidIdentifierReference.apply(ctx);
    super.exitIdentifierReference(ctx);
  }

  @Override
  public void exitPrimaryReference(EK9Parser.PrimaryReferenceContext ctx) {
    //Here we are modelling 'this' or 'super', idea is to set type or issue errors.
    checkValidThisOrSuper.accept(ctx);
    super.exitPrimaryReference(ctx);
  }

  @Override
  public void exitPrimary(EK9Parser.PrimaryContext ctx) {
    checkValidPrimary.accept(ctx);
    super.exitPrimary(ctx);
  }

  @Override
  public void exitCall(EK9Parser.CallContext ctx) {
    checkValidCall.accept(ctx);
    super.exitCall(ctx);
  }

  @Override
  public void exitList(EK9Parser.ListContext ctx) {
    checkAndTypeList.accept(ctx);
    super.exitList(ctx);
  }

  @Override
  public void exitDict(EK9Parser.DictContext ctx) {
    checkAndTypeDict.accept(ctx);
    super.exitDict(ctx);
  }

  @Override
  public void exitRange(EK9Parser.RangeContext ctx) {
    checkRange.accept(ctx);
    super.exitRange(ctx);
  }

  @Override
  public void exitForRange(EK9Parser.ForRangeContext ctx) {
    checkForRange.accept(ctx);
    super.exitForRange(ctx);
  }

  @Override
  public void exitExpression(EK9Parser.ExpressionContext ctx) {
    checkValidExpression.accept(ctx);
    super.exitExpression(ctx);
  }

  @Override
  public void exitAssignmentExpression(EK9Parser.AssignmentExpressionContext ctx) {
    checkAssignmentExpression.accept(ctx);

    super.exitAssignmentExpression(ctx);
  }

  @Override
  public void exitAssignmentStatement(EK9Parser.AssignmentStatementContext ctx) {
    checkAssignmentStatement.accept(ctx);
    super.exitAssignmentStatement(ctx);
  }

  @Override
  public void exitInstructionBlock(EK9Parser.InstructionBlockContext ctx) {
    checkInstructionBlockVariables.accept(ctx);
    super.exitInstructionBlock(ctx);
  }

  @Override
  public void exitObjectAccessStart(EK9Parser.ObjectAccessStartContext ctx) {
    //TODO pull out to function
    //primaryReference | identifier | call
    if (ctx.primaryReference() != null) {
      var resolved = symbolAndScopeManagement.getRecordedSymbol(ctx.primaryReference());
      if (resolved != null) {
        symbolAndScopeManagement.recordSymbol(resolved, ctx);
      }
    } else if (ctx.identifier() != null) {
      //In this context we can resolve the identifier and record it. - its not an identifierReference but an identifier.
      //This is done to find the compromise of reuse in the grammar. But makes this a bit more inconsistent.

      var resolved = resolveIdentifierOrError.apply(ctx.identifier());

      if (resolved != null) {
        //Record against both
        resolved.setReferenced(true);
        symbolAndScopeManagement.recordSymbol(resolved, ctx.identifier());
        symbolAndScopeManagement.recordSymbol(resolved, ctx);
      }
    } else if (ctx.call() != null) {
      var resolved = symbolAndScopeManagement.getRecordedSymbol(ctx.call());
      if (resolved != null) {
        symbolAndScopeManagement.recordSymbol(resolved, ctx);
      } else {
        System.out.println("ctx call is not recorded " + ctx.call().getText());
      }
    }
    super.exitObjectAccessStart(ctx);
  }

  @Override
  public void exitObjectAccessExpression(EK9Parser.ObjectAccessExpressionContext ctx) {
    //TODO also pull out to function

    var objectAccessStartSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx.objectAccessStart());
    if (objectAccessStartSymbol != null) {
      //If it is null then there will have already been an error

      //Now we have to follow the objectAccess and objectAccessType objectAccess?
      //But this has to be driven from here - rather than bottom up, because the context of resolution
      //is driven that way.
      var searchOnThisSymbol = objectAccessStartSymbol;
      var accessContext = ctx.objectAccess();

      boolean hasMoreInAccessChain;
      do {
        //Keep resolving in the chain until end of chain or a failure to resolve.
        resolveObjectAccess(accessContext, searchOnThisSymbol);

        searchOnThisSymbol = symbolAndScopeManagement.getRecordedSymbol(accessContext);
        hasMoreInAccessChain = accessContext.objectAccess() != null && searchOnThisSymbol != null;
        if (hasMoreInAccessChain) {
          accessContext = accessContext.objectAccess();
        }
      } while (hasMoreInAccessChain);

      //Now whatever is left in searchOnThisSymbol is the end of the chain and if not null we record it.
      if (searchOnThisSymbol != null) {
        symbolAndScopeManagement.recordSymbol(searchOnThisSymbol, ctx);
      }
    }
    super.exitObjectAccessExpression(ctx);
  }

  private void resolveObjectAccess(final EK9Parser.ObjectAccessContext ctx, final ISymbol inThisSymbol) {
    //TODO pull out to function with exitObjectAccessExpression
    var theType = inThisSymbol.getType();
    if (theType.isPresent()) {
      if (ctx.objectAccessType().identifier() != null) {
        var resolved = resolveFieldOrError.apply(ctx.objectAccessType().identifier(), (IScope) theType.get());
        var typeToRecord = returnTypeExtractor.apply(resolved);
        typeToRecord.ifPresent(type -> symbolAndScopeManagement.recordSymbol(type, ctx));
      } else if (ctx.objectAccessType().operationCall() != null) {
        var resolved =
            resolveOperationCallOrError.apply(ctx.objectAccessType().operationCall(), (IScope) theType.get());
        var typeToRecord = returnTypeExtractor.apply(resolved);
        typeToRecord.ifPresent(type -> symbolAndScopeManagement.recordSymbol(type, ctx));
      }
    }
  }

  @Override
  public void exitVariableDeclaration(EK9Parser.VariableDeclarationContext ctx) {
    checkVariableAssignmentDeclaration.accept(ctx);
    super.exitVariableDeclaration(ctx);
  }

  @Override
  public void exitPipelinePart(EK9Parser.PipelinePartContext ctx) {
    checkPipelinePart.accept(ctx);

    super.exitPipelinePart(ctx);
  }
}
