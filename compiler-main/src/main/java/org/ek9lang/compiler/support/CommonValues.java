package org.ek9lang.compiler.support;

/**
 * Fixed set of constants that tend to be used to 'squirrel' away data - these are the keys.
 */
public enum CommonValues {
  LOOP("LOOP"),
  DO("DO"),
  WHILE("WHILE"),
  UNINITIALISED_AT_DECLARATION("UNINITIALISED"),
  OK_ACCESS_REQUIRES_SAFE_ACCESS("OK_ACCESS_REQUIRES_SAFE_ACCESS"),
  ERROR_ACCESS_REQUIRES_SAFE_ACCESS("ERROR_ACCESS_REQUIRES_SAFE_ACCESS"),
  GET_ACCESS_REQUIRES_SAFE_ACCESS("GET_ACCESS_REQUIRES_SAFE_ACCESS"),
  NEXT_ACCESS_REQUIRES_SAFE_ACCESS("NEXT_ACCESS_REQUIRES_SAFE_ACCESS"),
  SAFE_ACCESS_EMPLOYED("SAFE_ACCESS_EMPLOYED"),
  NO_REFERENCED_RESET("NO_REFERENCED_RESET"),
  SUBSTITUTED("SUBSTITUTED"),
  ACCESSED("ACCESSED"),
  DEFAULTED("DEFAULTED"),
  EXTERN("EXTERN"),
  COMPLEXITY("COMPLEXITY"),
  CONSTRAIN("CONSTRAIN"),
  GENERIC_PARENT("GENERIC_PARENT"),
  BASE_NAME("BASE_NAME"),
  LANG("LANG"),
  FIXED("FIXED"),
  APPLICATION("APPLICATION"),
  HTTP_URI("HTTPURI"),
  URI_PROTO("URIPROTO"),
  HTTP_REQUEST("REQUEST"),
  HTTP_PATH("PATH"),
  HTTP_HEADER("HEADER"),
  HTTP_QUERY("QUERY"),
  HTTP_VERB("HTTPVERB"),
  HTTP_ACCESS("HTTPACCESS"),
  HTTP_SOURCE("HTTPSOURCE");

  private final String description;

  CommonValues(final String description) {
    this.description = description;
  }

  public String getDescription() {

    return description;
  }

  @Override
  public String toString() {
    return description;
  }


}
