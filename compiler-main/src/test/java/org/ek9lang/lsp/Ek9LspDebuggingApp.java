package org.ek9lang.lsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;

/**
 * Currently does not really work as I expect. It's also a bit half finished in terms of
 * collecting threads and processes.
 * <br/>
 * But does enable me to snip out bits of JSON that can be sent to the EK9 LSP server.
 * Given the following snip.json:
 * <pre>
 *   {
 *   "jsonrpc": "2.0",
 *   "id": 2,
 *   "method": "initialize",
 *   "params": {
 *     "clientID": "\u003cclient id\u003e",
 *     "adapterID": "\u003cadapter id\u003e",
 *     "linesStartAt1": false,
 *     "columnsStartAt1": false,
 *     "supportsRunInTerminalRequest": false
 *   }
 * }
 * </pre>
 * You can use this via the command line in the following way:
 * <pre>
 *  cat $HOME/snip.json | java -jar /Users/stevelimb/IdeaProjects/ek9/target/ek9c-jar-with-dependencies.jar -lsh
 * </pre>
 * Then look at the json result. Using this it is also possible to try out the native image version.
 * <pre>
 *  {"jsonrpc":"2.0","method":"window/showMessage","params":{"type":3,"message":"Welcome to Ek9"}}
 * </pre>
 * <pre>
 *  native-image --no-fallback -jar target/ek9c-jar-with-dependencies.jar
 *  cat $HOME/snip.json | ./ek9c-jar-with-dependencies -lsh
 * </pre>
 * Response is:
 * <pre>
 *  {"jsonrpc":"2.0","method":"window/showMessage","params":{}}
 * </pre>
 * But you will notice that the native-image version is broken!!
 */
public class Ek9LspDebuggingApp {
  public static void main(String[] args) {
    System.out.println("Started Ek9LspDebuggingApp");
    try {
      new Ek9LspDebuggingApp().startClient();
      System.out.println("Exit");
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println("Abnormal Exit");
    }
  }

  void startClient() throws ExecutionException, InterruptedException, TimeoutException, IOException {
    IDebugProtocolClient client = new CheckLanguageServerClient();
    ProcessBuilder builder =
        new ProcessBuilder("java", "-jar", "./target/ek9c-jar-with-dependencies.jar",
            "-lsh");
    Process process = builder.start();
    InputStream in = process.getInputStream();
    OutputStream out = process.getOutputStream();
    InputStream stderr = process.getErrorStream();

    System.out.println("Started EK9 LSP Server, listening to stderr");
    Thread errStreamReader = new Thread(() -> {
      try {
        String line = null;
        BufferedReader inErr = new BufferedReader(new InputStreamReader(stderr));
        while ((line = inErr.readLine()) != null) {
          System.err.println("From Process: " + line);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("Exit reading error stream");
    });
    errStreamReader.start();

    // Bootstrap the actual connection.
    Launcher<IDebugProtocolServer> launcher = DSPLauncher.createClientLauncher(client, in, out);

    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setClientID("<client id>");
    arguments.setAdapterID("<adapter id>");

    // Configure initialization as needed.
    arguments.setLinesStartAt1(false);
    arguments.setColumnsStartAt1(false);
    arguments.setSupportsRunInTerminalRequest(false);

    IDebugProtocolServer remoteProxy = launcher.getRemoteProxy();
    remoteProxy.configurationDone(new ConfigurationDoneArguments());

    var initResult = remoteProxy.initialize(arguments);
    try {
      remoteProxy.configurationDone(new ConfigurationDoneArguments());
      Capabilities capabilities = initResult.get(10, TimeUnit.SECONDS);
      assert capabilities != null;
    } catch (TimeoutException _) {
      System.err.println("***TIMEOUT***: on getting remote LSP capabilities");
    }

    //Now the shutdown of everything.

    System.out.println("Collecting the error stream reader thread");
    errStreamReader.join(1000);
    System.out.println("Collected the error stream reader thread");

    System.out.println("Destroying the LSP Server process");
    process.destroy();
    //Give it a short while to shutdown
    System.out.println("Waiting for LSP Server process to shutdown");
    Thread.sleep(2000);
    if (process.isAlive()) {
      System.out.println("Forcefully destroying the LSP Server process");
      process.destroyForcibly();
    } else {
      System.out.println("LSP Server process has been shutdown");
    }

    // At this point the client may start receiving events such as `stopped`, `terminated`, etc.
    System.out.println("Completed");
  }
}
