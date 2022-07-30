package org.ek9lang.lsp;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.core.exception.CompilerException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EK9WorkspaceService extends EK9Service implements WorkspaceService
{
	private final Workspace ek9WorkSpace = new Workspace();

	public EK9WorkspaceService(EK9LanguageServer languageServer)
	{
		super(languageServer);
	}

	@Override
	public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params)
	{
		String globalSymbolToFind = params.getQuery();
		//TODO the actual processing.
		System.err.println("symbol [" + globalSymbolToFind + "]");
		return null;
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params)
	{
		System.err.println("didChangeConfiguration [" + params + "]");
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params)
	{
		params.getChanges().forEach(fileEvent -> {
			switch(fileEvent.getType())
			{
				case Changed, Created -> reportOnCompiledSource(getWorkspace().reParseSource(getPath(fileEvent.getUri())));
				case Deleted -> clearOldCompiledDiagnostics(getWorkspace().removeSource(getPath(fileEvent.getUri())));
				default -> throw new CompilerException("Unknown Event Type");
			}
		});
	}

	public Workspace getEk9WorkSpace()
	{
		return ek9WorkSpace;
	}
}
