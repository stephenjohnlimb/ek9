package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HTTPTraitsTest extends Common{

  @Test
  void testHttpRequestTrait() {

    //Just need to check it is possible to 'new' one of these and not need to
    //implement any methods. i.e. basic default behaviour.
    final var theDefault = new HTTPRequest() {

    };

    assertEquals(String._of(""), theDefault.content());
    assertEquals(String._of(""), theDefault._string());

    assertSet.accept(theDefault._isSet());
  }

  @Test
  void testHttpResponseTrait() {

    //Just need to check it is possible to 'new' one of these and not need to
    //implement any methods. i.e. basic default behaviour.
    final var theDefault = new HTTPResponse() {

    };

    assertUnset.accept(theDefault.etag());

    assertEquals(String._of("public,max-age=3600,must-revalidate"), theDefault.cacheControl());
    assertEquals(String._of("text/plain"), theDefault.contentType());
    assertEquals(String._of("en"), theDefault.contentLanguage());
    assertUnset.accept(theDefault.contentLocation());
    assertEquals(String._of(""), theDefault.content());
    assertUnset.accept(theDefault.lastModified());
    assertEquals(Integer._of(404), theDefault.status());
    assertSet.accept(theDefault._isSet());
    assertEquals(String._of("404:"), theDefault._string());

  }
}
