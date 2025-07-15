package org.ek9.lang;

import java.util.Scanner;
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
public class Stdin implements StringInput {
  private final Scanner scanner;
  private boolean closed = false;

  @Ek9Constructor("""
      Stdin() as pure""")
  public Stdin() {
    //Just the default constructor
    scanner = new Scanner(System.in);
  }

  @Override
  @Ek9Method("""
      override next() as pure
        <- rtn as String?""")
  public String next() {

    if (!closed && scanner.hasNext()) {
      return String._of(scanner.nextLine());
    }
    return new String();
  }

  @Override
  @Ek9Method("""
      override hasNext() as pure
        <- rtn as Boolean?""")
  public Boolean hasNext() {
    if (closed) {
      return Boolean._of(false);
    }
    return Boolean._of(scanner.hasNext());
  }

  @Override
  @Ek9Operator("""
      override operator close as pure""")
  public void _close() {
    if (!closed) {
      closed = true;
      scanner.close();
    }
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return hasNext();
  }
}
