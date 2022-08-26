package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class ServerTest {

  @Test
  void startAndStopServer() throws ExecutionException, InterruptedException {

    var languageServer = Server.runEk9LanguageServer(System.in, System.out, true);

    assertFalse(languageServer.isDone());
    assertFalse(languageServer.isCancelled());

    //We now need to wait for it to get running and then stop it.
    Thread.sleep(500);
    var didCancel = languageServer.cancel(true);
    assertTrue(didCancel);

  }

}
