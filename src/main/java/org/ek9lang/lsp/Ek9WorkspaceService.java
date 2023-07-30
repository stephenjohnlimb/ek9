package org.ek9lang.lsp;

import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.core.utils.Logger;

/**
 * Part of the language server functionality.
 * See WorkspaceService for additional methods we can implement.
 */
public class Ek9WorkspaceService extends Ek9Service implements WorkspaceService {

  private final Workspace ek9WorkSpace = new Workspace();

  private final Consumer<FileEvent> reParseSource = fileEvent ->
      reportOnCompiledSource(getWorkspace()
          .reParseSource(getPath(fileEvent.getUri())).getErrorListener());

  private final Consumer<FileEvent> cleanUpSourceAfterDelete = fileEvent ->
      getWorkspace().removeSource(getPath(fileEvent.getUri()))
          .map(ErrorListener::getGeneralIdentifierOfSource)
          .ifPresent(this::clearOldCompiledDiagnostics);

  private final Map<FileChangeType, Consumer<FileEvent>> changeHandlers =
      Map.of(FileChangeType.Changed, reParseSource,
          FileChangeType.Created, reParseSource,
          FileChangeType.Deleted, cleanUpSourceAfterDelete);

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

    params
        .getChanges()
        .stream()
        .parallel()
        .filter(fileEvent -> changeHandlers.containsKey(fileEvent.getType()))
        .forEach(fileEvent -> changeHandlers.get(fileEvent.getType()).accept(fileEvent));
  }

  public Workspace getEk9WorkSpace() {
    return ek9WorkSpace;
  }
}
