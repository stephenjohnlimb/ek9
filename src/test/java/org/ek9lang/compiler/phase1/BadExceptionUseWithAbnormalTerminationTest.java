package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad use of exceptions, which can only cause block abnormal termination.
 * This is a simple early static check.
 */
class BadExceptionUseWithAbnormalTerminationTest extends BadSymbolDefinitionTest {


  public BadExceptionUseWithAbnormalTerminationTest() {
    super("/examples/parseButFailCompile/abnormalBlockTermination",
        List.of("bad.flowcontrol.examples"));
  }
}
