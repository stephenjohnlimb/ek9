package org.ek9lang.compiler.backend.jvm;

import java.io.FileOutputStream;
import java.io.IOException;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.ek9lang.compiler.ir.instructions.ForRangePolymorphicInstr;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LabelInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.ir.instructions.LogicalOperationInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.MethodVisitor;

/**
 * Enhanced visitor that uses specialized ASM generators to produce JVM bytecode for IR constructs.
 * Implements visitor pattern with typed visit methods for each IR instruction type.
 * Uses single responsibility principle with separate generators for each instruction type.
 */
public final class OutputVisitor implements INodeVisitor {

  private final ConstructTargetTuple constructTargetTuple;

  // Specialized ASM generators following single responsibility principle
  private final AsmStructureCreator asmStructureCreator;
  private final CallInstrAsmGenerator callInstrGenerator;
  private final LiteralInstrAsmGenerator literalInstrGenerator;
  private final MemoryInstrAsmGenerator memoryInstrGenerator;
  private final BranchInstrAsmGenerator branchInstrGenerator;
  private final LabelInstrAsmGenerator labelInstrGenerator;
  private final ScopeInstrAsmGenerator scopeInstrGenerator;
  private final ControlFlowChainAsmGenerator controlFlowChainGenerator;
  private final LogicalOperationAsmGenerator logicalOperationGenerator;
  private final ForRangePolymorphicAsmGenerator forRangePolymorphicGenerator;

  public OutputVisitor(final ConstructTargetTuple constructTargetTuple) {
    AssertValue.checkNotNull("File cannot be null", constructTargetTuple.targetFile());
    this.constructTargetTuple = constructTargetTuple;
    asmStructureCreator = new AsmStructureCreator(constructTargetTuple, this);

    // Initialize all specialized generators eagerly with shared ClassWriter
    final var classWriter = asmStructureCreator.getClassWriter();
    this.callInstrGenerator = new CallInstrAsmGenerator(constructTargetTuple, this, classWriter);
    this.literalInstrGenerator = new LiteralInstrAsmGenerator(constructTargetTuple, this, classWriter);
    this.memoryInstrGenerator = new MemoryInstrAsmGenerator(constructTargetTuple, this, classWriter);
    this.branchInstrGenerator = new BranchInstrAsmGenerator(constructTargetTuple, this, classWriter);
    this.labelInstrGenerator = new LabelInstrAsmGenerator(constructTargetTuple, this, classWriter);
    this.scopeInstrGenerator = new ScopeInstrAsmGenerator(constructTargetTuple, this, classWriter);
    this.controlFlowChainGenerator = new ControlFlowChainAsmGenerator(constructTargetTuple, this, classWriter);
    this.logicalOperationGenerator = new LogicalOperationAsmGenerator(constructTargetTuple, this, classWriter);
    this.forRangePolymorphicGenerator = new ForRangePolymorphicAsmGenerator(constructTargetTuple, this, classWriter);
  }

  /**
   * Set method context for all generators before processing a method's instructions.
   * Must be called before processing each method to ensure proper variable/label management.
   *
   * @param methodContext Shared context for variable and label maps
   * @param mv            Method visitor for this specific method
   * @param isConstructor Whether we're processing a constructor
   */
  public void setMethodContext(final AbstractAsmGenerator.MethodContext methodContext,
                               final MethodVisitor mv,
                               final boolean isConstructor) {
    // Share context with all generators
    callInstrGenerator.setSharedMethodContext(methodContext);
    literalInstrGenerator.setSharedMethodContext(methodContext);
    memoryInstrGenerator.setSharedMethodContext(methodContext);
    branchInstrGenerator.setSharedMethodContext(methodContext);
    labelInstrGenerator.setSharedMethodContext(methodContext);
    scopeInstrGenerator.setSharedMethodContext(methodContext);
    controlFlowChainGenerator.setSharedMethodContext(methodContext);
    logicalOperationGenerator.setSharedMethodContext(methodContext);
    forRangePolymorphicGenerator.setSharedMethodContext(methodContext);

    // Set method visitor for all generators
    callInstrGenerator.setCurrentMethodVisitor(mv);
    literalInstrGenerator.setCurrentMethodVisitor(mv);
    memoryInstrGenerator.setCurrentMethodVisitor(mv);
    branchInstrGenerator.setCurrentMethodVisitor(mv);
    labelInstrGenerator.setCurrentMethodVisitor(mv);
    scopeInstrGenerator.setCurrentMethodVisitor(mv);
    controlFlowChainGenerator.setCurrentMethodVisitor(mv);
    logicalOperationGenerator.setCurrentMethodVisitor(mv);
    forRangePolymorphicGenerator.setCurrentMethodVisitor(mv);

    // Set constructor mode for branch generator
    branchInstrGenerator.setConstructorMode(isConstructor);
  }

  @Override
  public void visit() {
    visit(constructTargetTuple.construct());

    //So that should be it, as long as each IR Node has been asked to accept this visitor
    //then with the double dispatch each of the relevant IRNodes in the code should have been visited.
    final var byteCodes = asmStructureCreator.getByteCode();

    // Only save if we have actual bytecode
    if (byteCodes.length > 0) {
      try (var stream = new FileOutputStream(constructTargetTuple.targetFile())) {
        stream.write(byteCodes);
      } catch (IOException e) {
        System.err.println("Failed to save bytecodes to file " + constructTargetTuple.targetFile());
        throw new CompilerException("Unable to save generated bytecode", e);
      }
    }

    // Note: ek9/Main.class generation is handled at the end of all construct processing
    // in CodeGenerationAggregates phase to ensure it's generated only once
  }

  @Override
  public void visit(final IRConstruct construct) {
    asmStructureCreator.processClass();
  }

  /**
   * Dispatch IRInstr to typed visit methods.
   * This is called by IRInstr.accept(visitor) polymorphically.
   */
  @Override
  public void visit(final IRInstr irInstr) {
    switch (irInstr) {
      case CallInstr i -> visit(i);
      case LiteralInstr i -> visit(i);
      case MemoryInstr i -> visit(i);
      case BranchInstr i -> visit(i);
      case LabelInstr i -> visit(i);
      case ScopeInstr i -> visit(i);
      case ControlFlowChainInstr i -> visit(i);
      case LogicalOperationInstr i -> visit(i);
      case ForRangePolymorphicInstr i -> visit(i);
      default -> throw new CompilerException("Operation [" + irInstr + " not implemented yet");
    }
  }

  /**
   * Typed visit method for CallInstr - delegates to specialized generator.
   */
  public void visit(final CallInstr callInstr) {
    callInstrGenerator.accept(callInstr);
  }

  /**
   * Typed visit method for LiteralInstr - delegates to specialized generator.
   */
  public void visit(final LiteralInstr literalInstr) {
    literalInstrGenerator.accept(literalInstr);
  }

  /**
   * Typed visit method for MemoryInstr - delegates to specialized generator.
   */
  public void visit(final MemoryInstr memoryInstr) {
    memoryInstrGenerator.accept(memoryInstr);
  }

  /**
   * Typed visit method for ScopeInstr - delegates to specialized generator.
   */
  public void visit(final ScopeInstr scopeInstr) {
    scopeInstrGenerator.accept(scopeInstr);
  }

  /**
   * Typed visit method for BranchInstr - delegates to specialized generator.
   */
  public void visit(final BranchInstr branchInstr) {
    branchInstrGenerator.accept(branchInstr);
  }

  /**
   * Typed visit method for LabelInstr - delegates to specialized generator.
   */
  public void visit(final LabelInstr labelInstr) {
    labelInstrGenerator.accept(labelInstr);
  }

  /**
   * Typed visit method for ControlFlowChainInstr - delegates to specialized generator.
   * Handles all EK9 control flow constructs: question operator, if/else, switch, loops, try/catch.
   */
  public void visit(final ControlFlowChainInstr instr) {
    controlFlowChainGenerator.generate(instr);
  }

  /**
   * Typed visit method for LogicalOperationInstr - delegates to specialized generator.
   * Handles logical AND/OR operations with short-circuit evaluation.
   */
  public void visit(final LogicalOperationInstr instr) {
    logicalOperationGenerator.generate(instr);
  }

  /**
   * Typed visit method for ForRangePolymorphicInstr - delegates to specialized generator.
   * Handles polymorphic FOR_RANGE loops with runtime direction detection.
   */
  public void visit(final ForRangePolymorphicInstr instr) {
    forRangePolymorphicGenerator.generate(instr);
  }

  /**
   * Generate LocalVariableTable for the current method.
   * Must be called after processing all instructions but before visitMaxs().
   * Delegates to any generator (they all share the same AbstractAsmGenerator base).
   */
  public void generateLocalVariableTable() {
    // Use any generator since they all share the same MethodContext
    // and have access to the same generateLocalVariableTable() method
    callInstrGenerator.generateLocalVariableTable();
  }
}
