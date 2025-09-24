package org.ek9lang.compiler.ir.instructions;

import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Specialized IR instruction for memory operations (LOAD, STORE, ALLOCA, REFERENCE).
 * <p>
 * Memory instructions handle variable access, allocation, and reference declaration in the EK9 IR.
 * </p>
 */
public final class MemoryInstr extends IRInstr {

  /**
   * Create LOAD instruction: LOAD dest = source_location.
   */
  public static MemoryInstr load(final String destination,
                                 final String sourceLocation) {
    return new MemoryInstr(IROpcode.LOAD, destination, null).addOperand(sourceLocation);
  }

  /**
   * Create LOAD instruction with debug info: LOAD dest = source_location.
   */
  public static MemoryInstr load(final String destination,
                                 final String sourceLocation,
                                 final DebugInfo debugInfo) {
    return new MemoryInstr(IROpcode.LOAD, destination, debugInfo).addOperand(sourceLocation);
  }

  /**
   * Create STORE instruction with debug info: STORE dest_location = source.
   */
  public static MemoryInstr store(final String destinationLocation,
                                  final String source,
                                  final DebugInfo debugInfo) {
    return new MemoryInstr(IROpcode.STORE, null, debugInfo).addOperands(destinationLocation, source);
  }

  /**
   * Create REFERENCE instruction: REFERENCE variable_name, type_info.
   */
  public static MemoryInstr reference(final String variableName, final String typeInfo) {
    return new MemoryInstr(IROpcode.REFERENCE, null, null).addOperands(variableName, typeInfo);
  }

  /**
   * Create REFERENCE instruction with debug info: REFERENCE variable_name, type_info.
   */
  public static MemoryInstr reference(final String variableName, final String typeInfo, final DebugInfo debugInfo) {
    return new MemoryInstr(IROpcode.REFERENCE, null, debugInfo).addOperands(variableName, typeInfo);
  }

  /**
   * Create RETAIN instruction with debug info: RETAIN object, i.e. increment the ARC (automatic reference counting)
   */
  public static MemoryInstr retain(final String object, final DebugInfo debugInfo) {
    return new MemoryInstr(IROpcode.RETAIN, null, debugInfo).addOperand(object);
  }

  /**
   * Create RELEASE instruction: RELEASE object, decrement ARC.
   */
  public static MemoryInstr release(final String object) {
    return new MemoryInstr(IROpcode.RELEASE, null, null).addOperand(object);
  }

  /**
   * Create RELEASE instruction with debug info: RELEASE object, decrement ARC.
   */
  public static MemoryInstr release(final String object, final DebugInfo debugInfo) {
    return new MemoryInstr(IROpcode.RELEASE, null, debugInfo).addOperand(object);
  }

  /**
   * Create IS_NULL instruction with debug info: IS_NULL result = operand.
   */
  public static MemoryInstr isNull(final String result, final String operand, final DebugInfo debugInfo) {
    return new MemoryInstr(IROpcode.IS_NULL, result, debugInfo).addOperand(operand);
  }

  /**
   * Create FUNCTION_INSTANCE instruction with debug info: FUNCTION_INSTANCE result = FunctionType.
   * Gets the singleton instance of the specified function type.
   */
  public static MemoryInstr functionInstance(final String result,
                                             final String functionType,
                                             final DebugInfo debugInfo) {
    return new MemoryInstr(IROpcode.FUNCTION_INSTANCE, result, debugInfo).addOperand(functionType);
  }

  private MemoryInstr(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    super(opcode, result, debugInfo);
  }

  @Override
  public MemoryInstr addOperand(final String operand) {
    super.addOperand(operand);
    return this;
  }

  @Override
  public MemoryInstr addOperands(final String... operands) {
    super.addOperands(operands);
    return this;
  }
}