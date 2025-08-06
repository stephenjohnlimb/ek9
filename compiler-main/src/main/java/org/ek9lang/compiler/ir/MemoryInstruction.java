package org.ek9lang.compiler.ir;

/**
 * Specialized IR instruction for memory operations (LOAD, STORE, ALLOCA).
 * <p>
 * Memory instructions handle variable access and allocation in the EK9 IR.
 * </p>
 */
public final class MemoryInstruction extends IRInstruction {

  /**
   * Create LOAD instruction: LOAD dest = source_location
   */
  public static MemoryInstruction load(final String destination, final String sourceLocation) {
    return new MemoryInstruction(IROpcode.LOAD, destination, null).addOperand(sourceLocation);
  }

  /**
   * Create LOAD instruction with debug info: LOAD dest = source_location
   */
  public static MemoryInstruction load(final String destination, final String sourceLocation, final DebugInfo debugInfo) {
    return new MemoryInstruction(IROpcode.LOAD, destination, debugInfo).addOperand(sourceLocation);
  }

  /**
   * Create STORE instruction: STORE dest_location = source
   */
  public static MemoryInstruction store(final String destinationLocation, final String source) {
    return new MemoryInstruction(IROpcode.STORE, null, null).addOperands(destinationLocation, source);
  }

  /**
   * Create STORE instruction with debug info: STORE dest_location = source
   */
  public static MemoryInstruction store(final String destinationLocation, final String source, final DebugInfo debugInfo) {
    return new MemoryInstruction(IROpcode.STORE, null, debugInfo).addOperands(destinationLocation, source);
  }

  /**
   * Create ALLOCA instruction: ALLOCA variable_name, type_info
   */
  public static MemoryInstruction alloca(final String variableName, final String typeInfo) {
    return new MemoryInstruction(IROpcode.ALLOCA, null, null).addOperands(variableName, typeInfo);
  }

  /**
   * Create ALLOCA instruction with debug info: ALLOCA variable_name, type_info
   */
  public static MemoryInstruction alloca(final String variableName, final String typeInfo, final DebugInfo debugInfo) {
    return new MemoryInstruction(IROpcode.ALLOCA, null, debugInfo).addOperands(variableName, typeInfo);
  }

  /**
   * Create ALLOC_OBJECT instruction: ALLOC_OBJECT result = constructor_call
   */
  public static MemoryInstruction allocObject(final String result, final String constructorCall) {
    return new MemoryInstruction(IROpcode.ALLOC_OBJECT, result, null).addOperand(constructorCall);
  }

  /**
   * Create ALLOC_OBJECT instruction with debug info: ALLOC_OBJECT result = constructor_call
   */
  public static MemoryInstruction allocObject(final String result, final String constructorCall, final DebugInfo debugInfo) {
    return new MemoryInstruction(IROpcode.ALLOC_OBJECT, result, debugInfo).addOperand(constructorCall);
  }

  /**
   * Create RETAIN instruction: RETAIN object
   */
  public static MemoryInstruction retain(final String object) {
    return new MemoryInstruction(IROpcode.RETAIN, null, null).addOperand(object);
  }

  /**
   * Create RETAIN instruction with debug info: RETAIN object
   */
  public static MemoryInstruction retain(final String object, final DebugInfo debugInfo) {
    return new MemoryInstruction(IROpcode.RETAIN, null, debugInfo).addOperand(object);
  }

  /**
   * Create RELEASE instruction: RELEASE object
   */
  public static MemoryInstruction release(final String object) {
    return new MemoryInstruction(IROpcode.RELEASE, null, null).addOperand(object);
  }

  /**
   * Create RELEASE instruction with debug info: RELEASE object
   */
  public static MemoryInstruction release(final String object, final DebugInfo debugInfo) {
    return new MemoryInstruction(IROpcode.RELEASE, null, debugInfo).addOperand(object);
  }

  private MemoryInstruction(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    super(opcode, result, debugInfo);
  }

  @Override
  public MemoryInstruction addOperand(final String operand) {
    super.addOperand(operand);
    return this;
  }

  @Override
  public MemoryInstruction addOperands(final String... operands) {
    super.addOperands(operands);
    return this;
  }
}