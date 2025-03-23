package org.ek9lang.compiler.ir;

import org.ek9lang.core.AssertValue;

/**
 * Represents a single statement, could be also in the form of an expression.
 * For now just hold the text and then move on to deal with processing assignments.
 */
public final class Statement implements INode {

  private final String statementText;

  public Statement(final String text) {

    AssertValue.checkNotNull("Text cannot be null", text);
    this.statementText = text;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Statement{" +
        "statementText='" + statementText + '\'' +
        '}';
  }
}
