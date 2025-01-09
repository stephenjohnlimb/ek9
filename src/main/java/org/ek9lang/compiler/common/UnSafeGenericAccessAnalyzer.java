package org.ek9lang.compiler.common;

/**
 * Checks to see if a variable has a specific generic type.
 */
final class UnSafeGenericAccessAnalyzer extends CodeFlowMap {

  UnSafeGenericAccessAnalyzer(final String accessMaker) {

    super(symbol -> (true),
        access -> access.metaData().contains(accessMaker),
        access -> access.metaData().add(accessMaker));

  }

}
