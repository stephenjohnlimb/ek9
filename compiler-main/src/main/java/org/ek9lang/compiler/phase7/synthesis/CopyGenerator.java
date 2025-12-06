package org.ek9lang.compiler.phase7.synthesis;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Generates synthetic IR for the _copy (:=:) operator.
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * Void _copy(T source):
 *   // For each field:
 *   fieldValue = LOAD source.field
 *   STORE this.field, fieldValue
 *
 *   return
 * </pre>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>Copies ALL fields from source to this</li>
 *   <li>This is a mutating operation (modifies this)</li>
 *   <li>Returns Void (no return value)</li>
 *   <li>Both SET and UNSET field values are copied</li>
 *   <li>Super's _copy is called if super is not Any</li>
 * </ul>
 *
 * <p>Note: This performs a shallow copy of field references. For deep copy
 * semantics, aggregate-typed fields would need their own :=: operator called,
 * but that is a more complex implementation reserved for future enhancement.</p>
 */
final class CopyGenerator extends AbstractSyntheticGenerator {

  private static final String SOURCE_PARAM = "param";

  CopyGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _copy operator IR for the given aggregate.
   *
   * @param operatorSymbol  The _copy operator method symbol
   * @param aggregateSymbol The aggregate containing the operator
   * @return List of IR instructions implementing the operator
   */
  List<IRInstr> generate(final MethodSymbol operatorSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(operatorSymbol);
    final var scopeId = stackContext.generateScopeId("_copy");
    final var aggregateTypeName = aggregateSymbol.getFullyQualifiedName();

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Call super._copy if super is not Any
    if (superHasOperator(aggregateSymbol, ":=:")) {
      instructions.addAll(generateSuperCopyCall(aggregateSymbol, debugInfo, scopeId));
    }

    // Copy each field from source to this
    final var fields = getSyntheticFields(aggregateSymbol);
    for (final var field : fields) {
      instructions.addAll(generateFieldCopy(field, debugInfo, scopeId));
    }

    // Return void
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnVoid(debugInfo));

    return instructions;
  }

  /**
   * Generate super._copy(param) call.
   *
   * <p>For _copy, we call the parent's implementation to handle inherited fields.
   * Note that _copy returns Void, so no result variable or memory management needed.</p>
   */
  private List<IRInstr> generateSuperCopyCall(final AggregateSymbol aggregateSymbol,
                                              final DebugInfo debugInfo,
                                              final String scopeId) {
    final var superOpt = aggregateSymbol.getSuperAggregate();
    if (superOpt.isEmpty() || isAnyType(superOpt.get())) {
      return List.of();
    }

    final var superAggregate = superOpt.get();

    // Call super._copy(param) - void return, no result handling needed
    // The generateMethodCall with null resultVar handles void returns
    return generateMethodCall(
        null, // No result for void return
        IRConstants.SUPER,
        superAggregate.getFullyQualifiedName(),
        IRConstants.COPY_METHOD,
        SOURCE_PARAM,
        superAggregate.getFullyQualifiedName(),
        getVoidTypeName(),
        debugInfo,
        scopeId
    );
  }

  /**
   * Generate field copy for a single field.
   *
   * <p>Pattern (ARC-compliant):</p>
   * <pre>
   *   sourceValue = LOAD param.field
   *   RETAIN sourceValue
   *   RELEASE this.field    // Release old value before overwrite
   *   STORE this.field, sourceValue
   *   RETAIN this.field
   * </pre>
   */
  private List<IRInstr> generateFieldCopy(final ISymbol field,
                                          final DebugInfo debugInfo,
                                          final String scopeId) {
    final var fieldName = field.getName();

    // Load field value from source (param)
    final var sourceValueVar = generateTempName();
    final var instructions =
        new ArrayList<>(generateFieldLoad(sourceValueVar, SOURCE_PARAM, fieldName, debugInfo, scopeId));

    // Store to this.field - must RELEASE old value first for ARC
    final var thisFieldRef = IRConstants.THIS + "." + fieldName;
    instructions.add(MemoryInstr.release(thisFieldRef, debugInfo));
    instructions.add(MemoryInstr.store(thisFieldRef, sourceValueVar, debugInfo));
    instructions.add(MemoryInstr.retain(thisFieldRef, debugInfo));

    return instructions;
  }
}
