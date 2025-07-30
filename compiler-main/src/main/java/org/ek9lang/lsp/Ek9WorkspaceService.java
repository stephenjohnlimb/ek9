package org.ek9lang.lsp;

import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.core.Logger;

/**
 * Part of the language server functionality.
 * See WorkspaceService for additional methods we can implement.
 */
final class Ek9WorkspaceService extends Ek9Service implements WorkspaceService {

  private final Workspace ek9WorkSpace = new Workspace();

  private final Map<FileChangeType, Consumer<FileEvent>> changeHandlers =
      Map.of(FileChangeType.Changed, this::reCompileSource,
          FileChangeType.Created, this::reCompileSource,
          FileChangeType.Deleted, this::cleanUpSourceAfterDelete);

  Ek9WorkspaceService(final Ek9LanguageServer languageServer) {

    super(languageServer);
  }

  @Override
  public void didChangeConfiguration(final DidChangeConfigurationParams params) {

    Logger.debug("didChangeConfiguration [" + params + "]");

  }

  @Override
  public void didChangeWatchedFiles(final DidChangeWatchedFilesParams params) {

    Logger.debug("didChangeWatchedFiles [" + params + "]");

    params
        .getChanges()
        .stream()
        .parallel()
        .filter(fileEvent -> changeHandlers.containsKey(fileEvent.getType()))
        .forEach(fileEvent -> changeHandlers.get(fileEvent.getType()).accept(fileEvent));
  }

  Workspace getEk9WorkSpace() {

    return ek9WorkSpace;
  }

  private void reCompileSource(final FileEvent fileEvent) {

    getCompilerService().recompileWorkSpace();

  }

  private void cleanUpSourceAfterDelete(final FileEvent fileEvent) {

    getWorkspace().removeSource(pathExtractor.apply(fileEvent.getUri()))
        .map(ErrorListener::getGeneralIdentifierOfSource)
        .ifPresent(this::clearOldCompiledDiagnostics);
  }

}
