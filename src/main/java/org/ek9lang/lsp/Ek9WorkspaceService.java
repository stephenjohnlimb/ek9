package org.ek9lang.lsp;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.utils.Logger;

/**
 * Part of the language server functionality.
 * See WorkspaceService for additional methods we can implement.
 */
public class Ek9WorkspaceService extends Ek9Service implements WorkspaceService {

  private final Workspace ek9WorkSpace = new Workspace();

  public Ek9WorkspaceService(Ek9LanguageServer languageServer) {
    super(languageServer);
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    Logger.debug("didChangeConfiguration [" + params + "]");
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    Logger.debug("didChangeWatchedFiles [" + params + "]");

    params.getChanges().forEach(fileEvent -> {
      switch (fileEvent.getType()) {
        case Changed, Created -> reportOnCompiledSource(
            getWorkspace().reParseSource(getPath(fileEvent.getUri())));
        case Deleted -> getWorkspace().removeSource(getPath(fileEvent.getUri()))
            .map(ErrorListener::getGeneralIdentifierOfSource)
            .ifPresent(this::clearOldCompiledDiagnostics);
        default -> throw new CompilerException("Unknown Event Type");
      }
    });
  }

  public Workspace getEk9WorkSpace() {
    return ek9WorkSpace;
  }
}
