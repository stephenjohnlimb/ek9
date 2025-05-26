package org.ek9lang.compiler.common;

/**
 * Used to check if variables within code blocks when declared without being initialised, actually
 * get initialised before they are used. This also applies to returning variables.
 */
final class UninitialisedVariableAnalyzer extends CodeFlowMap {

  UninitialisedVariableAnalyzer() {

    super(new UninitialisedVariableToBeChecked(),
        access -> access.metaData().contains(SAFE_ACCESS),
        access -> access.metaData().add(SAFE_ACCESS));

  }
}
