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

  @Ek9Constructor("""
      Stderr() as pure""")
  public Stderr() {
    //default constructor.
    set();
  }

  @Override
  @Ek9Method("""
      override println() as pure
        -> arg0 as String""")
  public void println(String arg0) {
    if (canProcess(arg0)) {
      System.err.println(arg0.state);
    }
  }

  @Override
  @Ek9Method("""
      override println() as pure
        -> arg0 as Any""")
  public void println(Any arg0) {
    if (canProcess(arg0)) {
      println(arg0._string());
    } //else ignore it.
  }

  @Override
  @Ek9Method("""
      override print() as pure
        -> arg0 as String""")
  public void print(String arg0) {
    if (canProcess(arg0)) {
      System.err.print(arg0.state);
    } // else ignore it
  }

  @Override
  @Ek9Method("""
      override print() as pure
        -> arg0 as Any""")
  public void print(Any arg0) {
    if (canProcess(arg0)) {
      print(arg0._string());
    } //else ignore it.
  }

  @Override
  @Ek9Operator("""
      override operator |
        -> arg0 as String""")
  public void _pipe(String arg0) {
    println(arg0);
  }

  @Override
  @Ek9Operator("""
      override operator |
        -> arg0 as Any""")
  public void _pipe(Any arg0) {
    if (canProcess(arg0)) {
      println(arg0._string());
    } //else ignore it.
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {

    return Boolean._of(isSet);
  }
}
