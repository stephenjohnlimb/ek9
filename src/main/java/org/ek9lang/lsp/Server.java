package org.ek9lang.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.ek9lang.core.OsSupport;

/**
 * EK9 LSP server launcher. You can run from main below, but it is designed to work with
 * cli/EK9.java
 * Plan to use much of the compiler as possible in the first instance.
 * This will be mainly for the diagnostics and the like.
 * All of that is wrapped up in the EK9LanguageServer and its components.
 */
public final class Server {

  /**
   * Main entry point to start and run the language server.
   */
  @SuppressWarnings("java:S106")
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    var startListening = runEk9LanguageServer(new OsSupport(), System.in, System.out,
        true, true);
    //Will cause main thread to block until control-C ends program.
    startListening.get();
  }

  /**
   * Triggers the actual execution of the language service.
   */
  public static Future<Void> runEk9LanguageServer(final OsSupport osSupport, final InputStream in,
                                                  final OutputStream out,
                                                  final boolean provideLanguageHoverHelp,
                                                  final boolean enableDebug) {
    //Only if required do we output all the debug messages in the LSP when processing.
    org.ek9lang.core.Logger.enableDebug(enableDebug);

    //Switch off any logging as we are using stdin/stdout for protocol exchange
    LogManager.getLogManager().reset();
    Logger globalLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    globalLogger.setLevel(Level.OFF);

    //Ready for document and workspace processing.

    Ek9LanguageServer languageServer = new Ek9LanguageServer(osSupport);
    languageServer.getCompilerConfig().setProvideLanguageHoverHelp(provideLanguageHoverHelp);
    Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(languageServer, in, out);

    LanguageClient client = launcher.getRemoteProxy();

    languageServer.connect(client);

    Future<Void> startListening = launcher.startListening();

    //Basically this will cause the to keep listening until it get the shutdown call
    //Then the server will issue System.exit.
    org.ek9lang.core.Logger.error("EK9 Language Server Listening");

    return startListening;
  }
}
