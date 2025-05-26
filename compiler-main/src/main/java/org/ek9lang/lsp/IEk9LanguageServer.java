package org.ek9lang.lsp;

import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Basic interface signature for the language server.
 */
@JsonSegment("server")
public interface IEk9LanguageServer extends LanguageServer, LanguageClientAware {
}


