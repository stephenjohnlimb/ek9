package org.ek9lang.lsp;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Just designed to exercise the EK9LanguageServer.
 * Deals with the responses or messages initiated by the EK9 Language server.
 */
public class CheckLanguageServerClient implements IEk9LanguageClient, IDebugProtocolClient {

  public CheckLanguageServerClient() {
    System.err.println("Client: construction");
  }

  @Override
  public void telemetryEvent(Object o) {
    System.err.println("Client: telemetryEvent");
  }

  @Override
  public void publishDiagnostics(PublishDiagnosticsParams publishDiagnosticsParams) {
    System.err.println("Client: publishDiagnostics");
  }

  @Override
  public void showMessage(MessageParams messageParams) {
    System.err.println("Client: showMessage " + messageParams);
  }

  @Override
  public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams showMessageRequestParams) {
    System.err.println("Client: showMessageRequest");
    return null;
  }

  @Override
  public void logMessage(MessageParams messageParams) {
    System.err.println("Client: logMessage");
  }

  public void start(LanguageServer serverLauncher) {
    System.out.println("CLIENT SEND REQUEST TO THE SERVER");
    serverLauncher.initialize(null);
  }
}
