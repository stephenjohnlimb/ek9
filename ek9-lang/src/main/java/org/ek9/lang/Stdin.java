package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Enables reading from the standard input channel.
 * i.e. input from another program or form an end user via the terminal.
 */
@Ek9Class("""
    Stdin with trait of StringInput""")
public class Stdin extends BuiltinType implements StringInput {
  private final StringInputImpl stringInput;

  @Ek9Constructor("""
      Stdin() as pure""")
  public Stdin() {
    //Just the default constructor
    stringInput = new StringInputImpl((System.in));
  }

  @Override
  @Ek9Method("""
      override next() as pure
        <- rtn as String?""")
  public String next() {
    return stringInput.next();
  }

  @Override
  @Ek9Method("""
      override hasNext() as pure
        <- rtn as Boolean?""")
  public Boolean hasNext() {
    return stringInput.hasNext();
  }

  @Override
  @Ek9Operator("""
      override operator close as pure""")
  public void _close() {
    stringInput._close();
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return stringInput._isSet();
  }
}
