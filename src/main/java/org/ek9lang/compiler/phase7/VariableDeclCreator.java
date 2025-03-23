package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.Assignment;
import org.ek9lang.compiler.ir.Instructions;
import org.ek9lang.compiler.ir.VariableDecl;
import org.ek9lang.compiler.ir.VariableRef;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Deals with creating a set of instructions for the task of declaring a variable and assigning an initial value to it.
 * In EK9, this can be completed in a number of ways.
 * <pre>
 *   variableDeclaration
 *     : identifier AS? typeDef QUESTION? op=(ASSIGN | ASSIGN2 | COLON | MERGE) assignmentExpression
 *     | identifier op=LEFT_ARROW assignmentExpression
 *     ;
 * </pre>
 * <p>
 * So, the set of 'instructions' will differ depending on the EK9 code used.
 * </p>
 */
public class VariableDeclCreator implements Function<EK9Parser.VariableDeclarationContext, Instructions> {

  private final ParsedModule parsedModule;

  private final AssignmentExpressionCreator assignmentExpressionCreator;

  public VariableDeclCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.assignmentExpressionCreator = new AssignmentExpressionCreator(parsedModule);

  }

  @Override
  public Instructions apply(final EK9Parser.VariableDeclarationContext ctx) {
    final var variableSymbol = parsedModule.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Variable declaration cannot be null", variableSymbol);

    //Need to define multiple instructions for this declaration and assignment.
    final var instructions = new Instructions();

    //But first things first need to define the fact a variable is needed.
    instructions.add(new VariableDecl(variableSymbol));

    //Now a reference to that same variable will be needed for the assignment.
    final var variableRef = new VariableRef(variableSymbol);

    final var rhs = assignmentExpressionCreator.apply(ctx.assignmentExpression());

    //Assignment
    if (ctx.LEFT_ARROW() != null || ctx.ASSIGN() != null || ctx.ASSIGN2() != null || ctx.COLON() != null) {
      //Then it is just a simple assignment, these are just syntactic sugar for the same thing.
      //The LEFT_ARROW is just a mechanism for type inference.
      instructions.add(new Assignment(variableRef, rhs));
    } else {
      //It is a merge of an existing variable into this new variable.
      throw new CompilerException("Variable declaration MERGE on declaration not implemented yet");
    }


    //TODO think about how an assignment should be defined.

    return instructions;
  }
}
