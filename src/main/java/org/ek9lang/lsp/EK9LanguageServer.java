/**
 * 
 */
package org.ek9lang.lsp;


import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.ek9lang.core.utils.Glob;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The Language Server Implementation into the modular EK9 compiler.
 */
public class EK9LanguageServer extends EK9Service implements LanguageServer, LanguageClientAware
{

	private OsSupport osSupport = new OsSupport();
	private EK9CompilerConfig compilerConfig;
	private EK9TextDocumentService textDocumentService;
    private EK9WorkspaceService workspaceService;
    private LanguageClient client;
    //To be used when the application exits, set to zero on shutdown by client.
    private int errorCode = 1;
    
    public EK9LanguageServer()
    {    	
    	this.textDocumentService = new EK9TextDocumentService(this);
        this.workspaceService = new EK9WorkspaceService(this);
        this.compilerConfig = new EK9CompilerConfig();
    }    
    
	@Override
	protected EK9LanguageServer getLanguageServer()
	{
		return this;
	}

	@Override
	public void connect(LanguageClient client)
	{
		this.client = client;		
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params)
	{
		//System.err.println("initialize [" + params.toString() + "]");
		final InitializeResult initializeResult = new InitializeResult(new ServerCapabilities());
		
		List<WorkspaceFolder> folders = params.getWorkspaceFolders();
		if(folders != null)
			folders.forEach(folder -> {
				Path path = getPath(folder.getUri());
				
				//System.err.println("Folder: " + path.toString());
				
				 Glob searchCondition = new Glob("**.ek9");
		         List<File> fileList = osSupport.getFilesRecursivelyFrom(path.toFile(), searchCondition);
		         fileList.forEach(file -> {
		        	 //System.err.println("File [" + file.toPath().toString() + "]");
		        	 //TODO use thread pool to process these files in terms of parsing concurrently.
		        	 try
		        	 {		        		 
		        		 getWorkspace().reParseSource(file.toPath());
		        		 reportOnCompiledSource(getWorkspace().getSource(file.toPath()));
		        	 }
		        	 catch(RuntimeException rex)
		        	 {
		        		 System.err.println("Failed to load and parse " + file.toString());
		        	 }
		         });		         
			});
		
        // We have to have full documents
        initializeResult.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);

        //Now tell client what capabilities this server supports.
        CompletionOptions completionOptions = new CompletionOptions();
        initializeResult.getCapabilities().setCompletionProvider(completionOptions);
        
		initializeResult.getCapabilities().setHoverProvider(true);
		initializeResult.getCapabilities().setDefinitionProvider(true);

		initializeResult.getCapabilities().setDeclarationProvider(true);
		initializeResult.getCapabilities().setReferencesProvider(true);

		return CompletableFuture.supplyAsync(()->initializeResult);
	}

	public LanguageClient getClient()
	{		
		return client;
	}
		
	public EK9CompilerConfig getCompilerConfig()
	{
		return compilerConfig;
	}

	@Override
	public CompletableFuture<Object> shutdown()
	{
		errorCode = 0;
		return null;
	}

	@Override
	public void exit()
	{
		System.exit(errorCode);
	}

	@Override
	public EK9TextDocumentService getTextDocumentService()
	{		
		return textDocumentService;
	}

	@Override
	public EK9WorkspaceService getWorkspaceService()
	{		
		return workspaceService;
	}
}
