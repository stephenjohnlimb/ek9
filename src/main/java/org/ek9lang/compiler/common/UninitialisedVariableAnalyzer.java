package org.ek9lang.compiler.common;

/**
 * Used to check if variables within code blocks when declared without being initialised, actually
 * get initialised before they are used. This also applies to returning variables.
 */
public class UninitialisedVariableAnalyzer extends CodeFlowMap {
  private static final String INITIALISED = "INITIALISED";

  protected UninitialisedVariableAnalyzer() {

    super(new UninitialisedVariableToBeChecked(),
        access -> access.metaData().contains(INITIALISED),
        access -> access.metaData().add(INITIALISED));

  }
}
