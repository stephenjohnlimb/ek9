package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.ShouldRegisterVariableInScope;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.core.AssertValue;

/**
 * Pulls out the common code for variable and variable-only declarations. DRY.
 */
abstract class AbstractVariableDeclGenerator extends AbstractGenerator {

  private final ShouldRegisterVariableInScope shouldRegisterVariableInScope = new ShouldRegisterVariableInScope();
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();

  AbstractVariableDeclGenerator(final IRContext context) {
    super(context);
  }

  public List<IRInstr> getDeclInstrs(final ParseTree ctx,
                                     final String scopeId) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);

    final var variableSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Variable symbol cannot be null", variableSymbol);

    final var instructions = new ArrayList<IRInstr>();

    final var variableName = variableNameForIR.apply(variableSymbol);
    final var variableTypeName = typeNameOrException.apply(variableSymbol);
    final var variableDebugInfo = debugInfoCreator.apply(variableSymbol);

    instructions.add(MemoryInstr.reference(variableName, variableTypeName, variableDebugInfo));

    //It's not always appropriate the register the variable in a scope.
    //This is because we may wish to hang on to the memory used, and not have reference count decremented by scope.
    if (shouldRegisterVariableInScope.test(scopeId)) {
      instructions.add(ScopeInstr.register(variableName, scopeId, variableDebugInfo));
    }

    return instructions;
  }
}
