package org.ek9.lang;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Trait;

/**
 * Ek9 model for http responses.
 * Provides a basic default implementation, So the developer only needs to
 * implement the methods they need to.
 */
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:MethodName"})
@Ek9Trait("""
    HTTPResponse as open""")
public interface HTTPResponse extends Any {

  @Ek9Method("""
      etag()
        <- rtn as String?""")
  default String etag() {
    return new String();
  }

  @Ek9Method("""
      cacheControl() as pure
        <- rtn as String?""")
  default String cacheControl() {
    return String._of("public,max-age=3600,must-revalidate");
  }

  @Ek9Method("""
      contentType() as pure
        <- rtn as String?""")
  default String contentType() {
    return String._of("text/plain");
  }

  @Ek9Method("""
      contentLanguage() as pure
        <- rtn as String?""")
  default String contentLanguage() {
    return String._of("en");
  }

  @Ek9Method("""
      contentLocation() as pure
        <- rtn as String?""")
  default String contentLocation() {
    return new String();
  }

  @Ek9Method("""
      content()
        <- rtn as String?""")
  default String content() {
    return String._of("");
  }

  @Ek9Method("""
      lastModified() as pure
        <- rtn as DateTime?""")
  default DateTime lastModified() {
    return new DateTime();
  }

  @Ek9Method("""
      status() as pure
        <- rtn as Integer?""")
  default Integer status() {
    return Integer._of(404);
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  default Boolean _isSet() {
    return Boolean._of(true);
  }


  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  default String _string() {
    return String._of(status().toString() + ":" + content().toString());
  }
}
