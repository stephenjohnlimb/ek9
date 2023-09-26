package org.ek9lang.lsp;

import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Just an interface mechanism to work with the LSP 4J code.
 */
@JsonSegment("client")
public interface IEk9LanguageClient extends LanguageClient {
  void start(LanguageServer serverLauncher);
}

