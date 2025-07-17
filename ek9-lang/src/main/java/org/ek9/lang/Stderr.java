package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * This is stock standard error output.
 * Sames as Stdout - but with Ek9 annotation and uses err rather than out.
 */
@Ek9Class("""
    Stderr with trait of StringOutput""")
public class Stderr extends BuiltinType implements StringOutput {

  private final StringOutputImpl err;

  @Ek9Constructor("""
      Stderr() as pure""")
  public Stderr() {
    //default constructor.
    set();
    err = new StringOutputImpl(System.err);
  }

  @Override
  @Ek9Method("""
      override println() as pure
        -> arg0 as String""")
  public void println(String arg0) {
    err.println(arg0);
  }

  @Override
  @Ek9Method("""
      override print() as pure
        -> arg0 as String""")
  public void print(String arg0) {
    err.print(arg0);
  }

  @Override
  @Ek9Operator("""
      override operator close as pure""")
  public void _close() {
    err._close();
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return err._isSet();
  }
}
