package org.ek9lang.compiler.common;

/**
 * For compiler, errors, warning and other information from the compiler.
 */
public interface CompilationIssue {

  String getClassificationDescription();

  String getTypeOfError();

  int getLineNumber();

  int getPosition();

  int getTokenLength();

  String getLikelyOffendingSymbol();

  String toLinePositionReference();
}
