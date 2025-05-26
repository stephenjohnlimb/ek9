package org.ek9lang.compiler.common;

/**
 * Checks to see if a variable has a specific generic type.
 */
final class UnSafeGenericAccessAnalyzer extends CodeFlowMap {

  UnSafeGenericAccessAnalyzer() {

    super(symbol -> (true),
        access -> access.metaData().contains(SAFE_ACCESS),
        access -> access.metaData().add(SAFE_ACCESS));

  }

}
