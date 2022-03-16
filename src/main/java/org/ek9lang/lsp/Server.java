package org.ek9lang.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * EK9 LSP server launcher. You can run from main below, but it is designed to work with cli/EK9.java
 * <p>
 * Plan to use much of the compiler as possible in the first instance.
 * This will be mainly for the diagnostics and the like.
 * All of that is wrapped up in the EK9LanguageServer and its components.
 */
public class Server
{
	public static void main(String[] args)
	{
		try
		{
			runEK9LanguageServer(System.in, System.out, true);
		}
		catch(Exception ex)
		{
			System.err.println("Failed to Start Language Server");
		}
	}

	public static void runEK9LanguageServer(InputStream in, OutputStream out, boolean provideLanguageHoverHelp)
	{
		//Switch off any logging as we are using stdin/stdout for protocol exchange
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(Level.OFF);

		//Ready for document and workspace processing.
        /*
    	EK9LanguageServer languageServer = new EK9LanguageServer();
    	languageServer.getCompilerConfig().setProvideLanguageHoverHelp(provideLanguageHoverHelp);
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(languageServer, in, out);

        LanguageClient client = launcher.getRemoteProxy();

        languageServer.connect(client);

        Future<?> startListening = launcher.startListening();

        //Basically this will cause the to keep listening until it get the shutdown call
        //Then the server will issue System.exit.
        System.err.println("EK9 Language Server Listening");
        startListening.get();
        */
	}
}
