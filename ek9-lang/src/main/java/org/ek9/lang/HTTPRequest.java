package org.ek9.lang;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Trait;

/**
 * For use with http communications.
 * Provides a default implementation with content being empty.
 */
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:MethodName"})
@Ek9Trait("""
    HTTPRequest as open""")
public interface HTTPRequest extends Any {

  @Ek9Method("""
      content() as pure
        <- rtn as String?""")
  default String content() {
    return String._of("");
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  default Boolean _isSet() {
    return Boolean._of(true);
  }


  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  default String _string() {
    return content();
  }
}
