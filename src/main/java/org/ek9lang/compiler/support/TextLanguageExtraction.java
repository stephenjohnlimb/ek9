package org.ek9lang.compiler.support;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9lang.antlr.EK9Parser.StringLitContext;
import org.ek9lang.compiler.common.ErrorListener;

/**
 * Extract the valid language value from the String literal or null and error.
 */
public class TextLanguageExtraction implements Function<StringLitContext, String> {

  private final ErrorListener errorListener;

  private final Pattern languagePattern = Pattern.compile("\"([a-z]+(_[A-Z]+)?)\"");

  public TextLanguageExtraction(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public String apply(StringLitContext stringLitContext) {
    Matcher m = languagePattern.matcher(stringLitContext.getText());
    if (m.find()) {
      return m.group(1);
    } else {
      errorListener.semanticError(stringLitContext.start, "Language must be \"[a-z]+(+_[A-Z]+)?\"",
          ErrorListener.SemanticClassification.INVALID_VALUE);
    }
    return null;
  }
}
