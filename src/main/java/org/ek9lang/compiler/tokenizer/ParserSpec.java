package org.ek9lang.compiler.tokenizer;

import java.io.InputStream;
import org.ek9lang.compiler.Source;
import org.ek9lang.compiler.common.ErrorListener;

/**
 * To be used to construct a parser, with a lexer.
 */
public record ParserSpec(Source src,
                         InputStream inputStream,
                         ErrorListener errorListener,
                         TokenConsumptionListener listener) {
}
