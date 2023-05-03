package org.ek9lang.compiler.main.phases.definition;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.resolvedefine.CheckAssignmentExpression;
import org.ek9lang.compiler.main.resolvedefine.CheckAssignmentStatement;
import org.ek9lang.compiler.main.resolvedefine.CheckInstructionBlockVariables;
import org.ek9lang.compiler.main.resolvedefine.CheckValidCall;
import org.ek9lang.compiler.main.resolvedefine.CheckValidExpression;
import org.ek9lang.compiler.main.resolvedefine.CheckValidIdentifierReference;
import org.ek9lang.compiler.main.resolvedefine.CheckValidPrimary;
import org.ek9lang.compiler.main.resolvedefine.CheckValidThisOrSuper;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * This listener just deals with expressions and the types that result from expressions.
 * Note that the correct must be inplace for these expressions to 'reside in'.
 * See 'instructionBlock' and enter/exit in the 'ScopeStackConsistencyListener' and 'AbstractEK9PhaseListener'.
 * Now some of this may feel upside down, processing on 'exit' and from the 'bottom up'.
 */
public abstract class ExpressionsListener extends ScopeStackConsistencyListener {

  protected final SymbolFactory symbolFactory;
  private final CheckValidThisOrSuper checkValidThisOrSuper;

  private final CheckValidPrimary checkValidPrimary;

  private final CheckValidIdentifierReference checkValidIdentifierReference;

  private final CheckValidExpression checkValidExpression;

  private final CheckInstructionBlockVariables checkInstructionBlockVariables;

  private final CheckAssignmentExpression checkAssignmentExpression;

  private final CheckAssignmentStatement checkAssignmentStatement;

  private final CheckValidCall checkValidCall;

  protected ExpressionsListener(ParsedModule parsedModule) {
    super(parsedModule);

    symbolFactory = new SymbolFactory(parsedModule);

    var errorListener = parsedModule.getSource().getErrorListener();
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
        new CheckAssignmentExpression(symbolAndScopeManagement);

    checkAssignmentStatement =
        new CheckAssignmentStatement(symbolAndScopeManagement, errorListener);

    checkValidCall =
        new CheckValidCall(symbolAndScopeManagement, errorListener);
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
    //For a List of type to be created, we need the common super/trait to apply

    super.exitList(ctx);
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
      var resolvedIdentifier =
          symbolAndScopeManagement.getTopScope().resolve(new SymbolSearch(ctx.identifier().getText()));
      if (resolvedIdentifier.isPresent()) {
        //Record against both
        var resolved = resolvedIdentifier.get();
        resolved.setReferenced(true);
        symbolAndScopeManagement.recordSymbol(resolved, ctx.identifier());
        symbolAndScopeManagement.recordSymbol(resolved, ctx);
      } else {
        System.out.println("Unable to resolve " + ctx.identifier().getText());
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

    var objectAccessStart = symbolAndScopeManagement.getRecordedSymbol(ctx.objectAccessStart());
    if (objectAccessStart != null) {
      //If it is null then there will have already been an error
      System.out.println("Have objectAccessStart [" + objectAccessStart.getFriendlyName() + "]");

      //Now we have to follow the objectAccess and objectAccessType objectAccess?
      //But this has to be driven from here - rather than bottom up, because the context of resolution
      //is driven that way.
      resolveObjectAccess(ctx.objectAccess(), objectAccessStart);
    }
    super.exitObjectAccessExpression(ctx);
  }

  private void resolveObjectAccess(final EK9Parser.ObjectAccessContext ctx, final ISymbol fromThisSymbol) {
    //TODO pull out to function with exitObjectAccessExpression
    var theType = fromThisSymbol.getType();
    if (theType.isPresent()) {
      if (ctx.objectAccessType().identifier() != null) {
        var identifier = ctx.objectAccessType().identifier().getText();
        System.out.println(
            "It's just a lookup of an identifier [" + identifier + "] on [" + theType.get().getFriendlyName() + "]");
      } else if (ctx.objectAccessType().operationCall() != null) {
        var methodCallText = ctx.objectAccessType().operationCall().getText();
        System.out.println(
            "It's just a lookup of a method on [" + methodCallText + "] on [" + theType.get().getFriendlyName() + "]");
      }
    }
  }

  //Now a block statement can be a variable declaration, variable only declaration or just a statement
  //So lets deal with those first, earlier passes may have dealt with all the simple typing.
  @Override
  public void exitVariableDeclaration(EK9Parser.VariableDeclarationContext ctx) {
    //TODO pull out to function
    var variable = symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Expecting a symbol to be present", variable);
    //Now it should have been typed if it has a typeDef - if not then that's an error in previous phases.
    //Of an error in this phase.
    if (ctx.typeDef() != null) {
      if (variable.getType().isPresent()) {
        System.out.println("Already typed " + variable.getType().get().getFriendlyName());
      } else {
        var theType = symbolAndScopeManagement.getRecordedSymbol(ctx.typeDef());
        if (theType == null) {
          System.out.println(ctx.typeDef().getText() + " was not resolved on line: " + ctx.typeDef().start.getLine());
        } else {
          variable.setType(theType);
        }
      }
    } else {
      //This means it's a 'left arrow' inference time! But it might have been typed if it was a built in type.
      if (variable.getType().isEmpty()) {
        var assignedFrom = symbolAndScopeManagement.getRecordedSymbol(ctx.assignmentExpression());
        if (assignedFrom != null && assignedFrom.getType().isPresent()) {
          variable.setType(assignedFrom.getType());
        } else {
          System.out.println(
              "Still need to provide type for '" + variable.getFriendlyName() + "' " + variable.getSourceToken()
                  .getLine());
        }
      }
    }

    super.exitVariableDeclaration(ctx);
  }

}
