package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for EK9 operator → JVM method name mapping.
 * Validates that all operators use OperatorMap.java mappings.
 *
 * <p>Critical Validations (from OperatorMap.java):</p>
 * <ul>
 *   <li>operator ? → _isSet()</li>
 *   <li>operator close → _close()</li>
 *   <li>operator $ → _string()</li>
 *   <li>operator #? → _hashcode()</li>
 *   <li>operator &lt;=&gt; → _cmp(...)</li>
 *   <li>operator == → _eq(...)</li>
 *   <li>operator &lt;&gt; → _neq(...)</li>
 *   <li>operator + → _add(...)</li>
 *   <li>operator &lt; → _lt(...)</li>
 *   <li>All method invocations use mapped names</li>
 * </ul>
 *
 * <p>This test will initially FAIL or not compile, documenting the bug.
 * After fixing operator mapping in JVM backend, this test validates the fix.</p>
 */
class OperatorMappingTest extends AbstractExecutableBytecodeTest {

  public OperatorMappingTest() {
    super("/examples/bytecodeGeneration/operatorMapping",
        "bytecode.test.operators",
        "TestOperatorMapping",
        List.of(new SymbolCountCheck("bytecode.test.operators", 2)));
  }
}
