package org.ek9.lang;

import java.io.PrintStream;

/**
 * Note exposed in EK9 as an interface, just common code for String output for a jave output stream.
 * Enables delegation.
 */
class StringOutputImpl extends BuiltinType implements StringOutput {

  private PrintStream out;

  StringOutputImpl(PrintStream ps) {
    if (ps != null) {
      out = ps;
      set();
    }
  }

  @Override
  public void println(String arg0) {
    if (canProcess(arg0)) {
      out.println(arg0.state);
    }
  }

  @Override
  public void print(String arg0) {
    if (canProcess(arg0)) {
      out.print(arg0.state);
    } // else ignore it
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Override
  public void _close() {
    try {
      out.close();
    } catch (Exception _) {
      //Ignore
    }
  }

  @Override
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }
}
