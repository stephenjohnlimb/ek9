package org.ek9lang.lsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

final class PathExtractor implements Function<String, Path> {
  @SuppressWarnings("checkstyle:CatchParameterName")
  @Override
  public Path apply(final String uri) {
    try {
      return Paths.get(new URI(uri));
    } catch (URISyntaxException _) {
      throw new IllegalArgumentException("Unable to create Path from uri [" + uri + "]");
    }
  }
}
