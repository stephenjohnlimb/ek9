package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.BasicBlock;
import org.ek9lang.compiler.ir.BranchInstruction;
import org.ek9lang.compiler.ir.CallInstruction;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.compiler.ir.MemoryInstruction;
import org.ek9lang.compiler.ir.ScopeInstruction;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Generates IR BasicBlock sequences from EK9 AST nodes.
 * <p>
 * Properly traverses the ANTLR AST and uses resolved symbols from ParsedModule
 * to generate accurate IR instructions that reflect the actual EK9 source code.
 * </p>
 */
public final class IRBlockGenerator {

  private final ParsedModule parsedModule;
  private final AtomicInteger tempCounter = new AtomicInteger(0);
  private final AtomicInteger blockCounter = new AtomicInteger(0);
  private final AtomicInteger scopeCounter = new AtomicInteger(0);

  public IRBlockGenerator(final ParsedModule parsedModule) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    this.parsedModule = parsedModule;
  }

  /**
   * Generate IR for a program's instruction block.
   * Properly traverses the AST to create BasicBlock with actual program statements.
   */
  public BasicBlock generateProgramBlock(final EK9Parser.InstructionBlockContext ctx) {
    AssertValue.checkNotNull("InstructionBlockContext cannot be null", ctx);

    java.lang.String blockLabel = generateBlockLabel("program_entry");
    java.lang.String scopeId = generateScopeId("main_scope");
    BasicBlock block = new BasicBlock(blockLabel);

    // Enter main scope for memory management
    block.addInstruction(ScopeInstruction.enter(scopeId));

    // Process all block statements from the actual AST
    for (EK9Parser.BlockStatementContext blockStmtCtx : ctx.blockStatement()) {
      List<IRInstruction> stmtInstructions = generateBlockStatement(blockStmtCtx, scopeId);
      block.addInstructions(stmtInstructions);
    }

    // Exit main scope (automatic RELEASE of all registered objects)
    block.addInstruction(ScopeInstruction.exit(scopeId));

    // Return from program
    block.addInstruction(BranchInstruction.returnVoid());

    return block;
  }

  /**
   * Generate IR instructions for a block statement using actual AST analysis.
   */
  private List<IRInstruction> generateBlockStatement(final EK9Parser.BlockStatementContext ctx,
                                                     final java.lang.String scopeId) {
    List<IRInstruction> instructions = new ArrayList<>();

    if (ctx.variableDeclaration() != null) {
      instructions.addAll(generateVariableDeclaration(ctx.variableDeclaration(), scopeId));
    } else if (ctx.variableOnlyDeclaration() != null) {
      instructions.addAll(generateVariableOnlyDeclaration(ctx.variableOnlyDeclaration(), scopeId));
    } else if (ctx.statement() != null) {
      instructions.addAll(generateStatement(ctx.statement(), scopeId));
    }

    return instructions;
  }

  /**
   * Generate IR for variable declaration using resolved symbols.
   * Example: stdout <- Stdout() -> uses actual symbol information
   */
  private List<IRInstruction> generateVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx,
                                                          final java.lang.String scopeId) {
    List<IRInstruction> instructions = new ArrayList<>();

    // Get the resolved variable symbol from the parsed module
    ISymbol varSymbol = parsedModule.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Variable symbol cannot be null", varSymbol);

    java.lang.String varName = varSymbol.getName();
    java.lang.String tempResult = generateTempName();

    // Process the assignment expression (right-hand side)
    if (ctx.assignmentExpression() != null) {
      instructions.addAll(generateAssignmentExpression(ctx.assignmentExpression(), tempResult, scopeId));

      // Store the result in the variable
      instructions.add(MemoryInstruction.store(varName, tempResult));
    }

    return instructions;
  }

  /**
   * Generate IR for variable only declaration using resolved symbols.
   */
  private List<IRInstruction> generateVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx,
                                                              final java.lang.String scopeId) {
    List<IRInstruction> instructions = new ArrayList<>();

    // Get the resolved variable symbol
    ISymbol varSymbol = parsedModule.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Variable symbol cannot be null", varSymbol);

    java.lang.String varName = varSymbol.getName();
    java.lang.String typeName = varSymbol.getType().orElse(varSymbol).getFullyQualifiedName();

    // Allocate space for the variable
    instructions.add(MemoryInstruction.alloca(varName, typeName));

    return instructions;
  }

  /**
   * Generate IR for a statement using AST analysis.
   */
  private List<IRInstruction> generateStatement(final EK9Parser.StatementContext ctx,
                                                final java.lang.String scopeId) {
    List<IRInstruction> instructions = new ArrayList<>();

    if (ctx.objectAccessExpression() != null) {
      java.lang.String tempResult = generateTempName();
      instructions.addAll(generateObjectAccessExpression(ctx.objectAccessExpression(), tempResult, scopeId));
    }

    return instructions;
  }

  /**
   * Generate IR for assignment expression using actual AST traversal.
   */
  private List<IRInstruction> generateAssignmentExpression(final EK9Parser.AssignmentExpressionContext ctx,
                                                           final java.lang.String resultVar,
                                                           final java.lang.String scopeId) {
    List<IRInstruction> instructions = new ArrayList<>();

    // Navigate through the assignment expression to find the actual call
    // For now, handle the simple case of direct object access
    if (ctx.expression() != null) {
      instructions.addAll(generateExpression(ctx.expression(), resultVar, scopeId));
    }

    return instructions;
  }

  /**
   * Generate IR for expression - simplified approach.
   */
  private List<IRInstruction> generateExpression(final EK9Parser.ExpressionContext ctx,
                                                 final java.lang.String resultVar,
                                                 final java.lang.String scopeId) {
    List<IRInstruction> instructions = new ArrayList<>();

    // For the simple hello world case, look for object access expressions
    if (ctx.objectAccessExpression() != null) {
      instructions.addAll(generateObjectAccessExpression(ctx.objectAccessExpression(), resultVar, scopeId));
    }

    return instructions;
  }

  /**
   * Generate IR for object access expression using resolved symbols.
   * This follows the pattern used in ObjectAccessCreator.
   */
  private List<IRInstruction> generateObjectAccessExpression(final EK9Parser.ObjectAccessExpressionContext ctx,
                                                             final java.lang.String resultVar,
                                                             final java.lang.String scopeId) {
    List<IRInstruction> instructions = new ArrayList<>();

    // Check if this is a simple constructor call (like Stdout())
    if (ctx.objectAccessStart() != null && ctx.objectAccessStart().call() != null) {
      // This is a constructor call - get the resolved symbol
      ISymbol callSymbol = parsedModule.getRecordedSymbol(ctx.objectAccessStart().call());

      if (callSymbol != null) {
        java.lang.String typeName = callSymbol.getFullyQualifiedName();

        // Generate constructor call using actual resolved type name
        instructions.add(CallInstruction.constructor(resultVar, typeName));

        // Add memory management for LLVM targets (no-ops on JVM)
        instructions.add(MemoryInstruction.retain(resultVar));
        instructions.add(ScopeInstruction.register(resultVar, scopeId));
      }
    } else if (ctx.objectAccess() != null) {
      // This is a method call on an object - handle chained access
      instructions.addAll(generateObjectAccess(ctx, resultVar, scopeId));
    }

    return instructions;
  }

  /**
   * Generate IR for chained object access (like stdout.println()).
   */
  private List<IRInstruction> generateObjectAccess(final EK9Parser.ObjectAccessExpressionContext ctx,
                                                   final java.lang.String resultVar,
                                                   final java.lang.String scopeId) {
    List<IRInstruction> instructions = new ArrayList<>();

    // Get the target variable (like stdout)
    java.lang.String targetVar = null;
    if (ctx.objectAccessStart() != null && ctx.objectAccessStart().identifier() != null) {
      targetVar = ctx.objectAccessStart().identifier().getText();
    }

    // Get the method call information
    if (ctx.objectAccess() != null && ctx.objectAccess().objectAccessType() != null) {
      EK9Parser.ObjectAccessTypeContext accessType = ctx.objectAccess().objectAccessType();

      if (accessType.operationCall() != null) {
        // Get the resolved symbol for the method call
        ISymbol methodSymbol = parsedModule.getRecordedSymbol(accessType.operationCall());

        if (methodSymbol instanceof CallSymbol callSymbol && targetVar != null) {
          MethodSymbol toBeCalled = (MethodSymbol) callSymbol.getResolvedSymbolToCall();

          if (toBeCalled != null) {
            java.lang.String methodName = toBeCalled.getName();

            // Load the target object
            java.lang.String tempObj = generateTempName();
            instructions.add(MemoryInstruction.load(tempObj, targetVar));

            // Extract arguments from the method call
            java.lang.String[] arguments = extractMethodArguments(accessType.operationCall());

            // Generate the method call
            instructions.add(CallInstruction.call(resultVar, tempObj, methodName, arguments));
          }
        }
      }
    }

    return instructions;
  }

  /**
   * Extract method arguments from operation call context.
   */
  private java.lang.String[] extractMethodArguments(final EK9Parser.OperationCallContext ctx) {
    List<java.lang.String> args = new ArrayList<>();

    if (ctx.paramExpression() != null && !ctx.paramExpression().expressionParam().isEmpty()) {
      // Extract arguments from the parameter expression
      ctx.paramExpression().expressionParam().forEach(paramCtx ->
          args.add(paramCtx.expression().getText()));
    }

    return args.toArray(new java.lang.String[0]);
  }

  // Utility methods for generating unique names and IDs

  private java.lang.String generateTempName() {
    return "temp" + tempCounter.incrementAndGet();
  }

  private java.lang.String generateBlockLabel(final java.lang.String prefix) {
    return prefix + "_" + blockCounter.incrementAndGet();
  }

  private java.lang.String generateScopeId(final java.lang.String prefix) {
    return prefix + "_" + scopeCounter.incrementAndGet();
  }
}