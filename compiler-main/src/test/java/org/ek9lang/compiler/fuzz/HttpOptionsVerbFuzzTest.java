package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for HTTP OPTIONS verb in service operations.
 *
 * <p>Tests HTTP OPTIONS verb grammar coverage including:
 * - Basic OPTIONS usage for CORS preflight
 * - OPTIONS with path parameters
 * - CORS pattern validation
 *
 * <p>These tests exercise the httpVerb grammar rule with HTTP_OPTIONS token
 * while producing SERVICE_INCOMPATIBLE_RETURN_TYPE and related errors.
 */
class HttpOptionsVerbFuzzTest extends FuzzTestBase {

  public HttpOptionsVerbFuzzTest() {
    super("httpOptionsVerb", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testHttpOptionsVerbRobustness() {
    assertTrue(runTests() != 0);
  }
}
