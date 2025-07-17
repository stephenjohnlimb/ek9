package org.ek9.lang;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Note exposed in EK9 as an interface, just common code for String inputs for a jave input stream.
 * Enables delegation.
 */
class StringInputImpl extends BuiltinType implements StringInput {
  private final Scanner scanner;
  private boolean closed = false;

  StringInputImpl(InputStream is) {
    scanner = new Scanner(is);
  }

  @Override
  public String next() {

    if (!closed && scanner.hasNext()) {
      return String._of(scanner.nextLine());
    }
    return new String();
  }

  @Override
  public Boolean hasNext() {
    if (closed) {
      return Boolean._of(false);
    }
    return Boolean._of(scanner.hasNext());
  }

  @Override
  public void _close() {
    if (!closed) {
      closed = true;
      scanner.close();
    }
  }

  @Override
  public Boolean _isSet() {
    return hasNext();
  }
}
