package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * This is abstract for EK9, meaning that the developer must extend and implement this function interface.
 * It is concrete for Java meaning that, I can do basic unit tests.
 */
@Ek9Function("""
    TCPHandler as abstract
      ->
        input as StringInput
        output as StringOutput""")
public class TCPHandler implements Any {

  @Ek9Constructor("TCPHandler() as pure")
  public TCPHandler() {
    //default constructor.
  }

  @SuppressWarnings("checkstyle:MethodName")
  public void _call(StringInput input, StringOutput output) {

    if (isValid(input) && isValid(output)) {
      //Just consume from input and send to output.
      while (isTrue(input.hasNext())) {
        final var value = input.next();
        output.println(value);
      }
      input._close();
      output._close();
    }
  }

  private boolean isTrue(Boolean value) {
    return value != null && value.isSet && value.state;
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }
}
