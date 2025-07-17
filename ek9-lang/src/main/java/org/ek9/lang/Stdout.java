package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * This is stock standard output.
 */
@Ek9Class("""
    Stdout with trait of StringOutput""")
public class Stdout extends BuiltinType implements StringOutput {

  private final StringOutputImpl out;

  @Ek9Constructor("""
      Stdout() as pure""")
  public Stdout() {
    //default constructor.
    set();
    out = new StringOutputImpl(System.out);
  }

  @Override
  @Ek9Method("""
      override println() as pure
        -> arg0 as String""")
  public void println(String arg0) {
    out.println(arg0);
  }

  @Override
  @Ek9Method("""
      override print() as pure
        -> arg0 as String""")
  public void print(String arg0) {
    out.print(arg0);
  }

  @Override
  @Ek9Operator("""
      override operator close as pure""")
  public void _close() {
    out._close();
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return out._isSet();
  }
}
