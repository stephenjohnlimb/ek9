package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import org.ek9lang.core.OsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

//Specific tests that manipulate files and specifics in ek9 must not run in parallel.
@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
final class ServerTest {

  @Test
  @SuppressWarnings("java:S2925")
  void startAndStopServer() throws InterruptedException {

    var osSupport = new OsSupport(true);
    var languageServer = Server.runEk9LanguageServer(osSupport, System.in,
        System.out, true, false);

    assertFalse(languageServer.isDone());
    assertFalse(languageServer.isCancelled());

    //We now need to wait for it to get running and then stop it.
    Thread.sleep(500);
    var didCancel = languageServer.cancel(true);
    assertTrue(didCancel);

  }

}
