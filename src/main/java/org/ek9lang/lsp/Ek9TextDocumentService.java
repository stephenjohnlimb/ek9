package org.ek9lang.lsp;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.utils.Logger;
import org.ek9lang.lsp.Ek9LanguageWords.KeyWordInformation;

/**
 * Part of the language server functionality.
 * Some methods could be implemented but Ignore the events as we pick
 * up changes by implementing didChangeWatchedFiles in the workspace service.
 * Note there are lots of other methods we will implement in here.
 * Like type hierarchy for example. See TextDocumentService for other methods we can implement.
 */
public class Ek9TextDocumentService extends Ek9Service implements TextDocumentService {
  private final Ek9LanguageWords languageWords;

  public Ek9TextDocumentService(Ek9LanguageServer languageServer) {
    super(languageServer);
    languageWords = new Ek9LanguageWords();
  }

  protected Ek9LanguageWords getLanguageWords() {
    return languageWords;
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      CompletionParams position) {
    Logger.debug("Would do completion [" + position.toString() + "]");

    return CompletableFuture.supplyAsync(() -> {
      List<CompletionItem> list = new ArrayList<>();

      TokenResult tokenResult = getNearestToken(position);
      if (tokenResult.isPresent()) {
        List<String> languageMatches = getLanguageWords().fuzzyMatch(tokenResult);

        languageMatches.forEach(completion -> {
          if (list.size() < getLanguageServer().getCompilerConfig().getNumberOfSuggestions()) {
            Logger.debug("Adding completion of [" + completion + "]");
            list.add(new CompletionItem(completion));
          }
        });
        //TODO add in Symbol matches as well
      }

      return Either.forLeft(list);
    });
  }

  @Override
  public CompletableFuture<Hover> hover(HoverParams params) {
    Logger.debug("Would do hover [" + params.toString() + "]");

    return CompletableFuture.supplyAsync(() -> {

      TokenResult tokenResult = getNearestToken(params);
      if (tokenResult.isPresent()) {
        //TODO Now we'd do a symbol lookup here.
        KeyWordInformation match = null;
        //If not a match on the symbols, and we're configured just do a language keyword search.
        if (match == null && getLanguageServer().getCompilerConfig().isProvideLanguageHoverHelp()) {
          match = getLanguageWords().exactMatch(tokenResult);
          if (match != null) {
            MarkupContent markedUp = new MarkupContent("plaintext", match.hoverText);
            return new Hover(markedUp);
          }
        }
      }
      return null;
    });
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    try {
      String uri = getFilename(params.getTextDocument());
      Logger.debug("didOpen Opened Source [" + uri + "]");

      var inputStream = new ByteArrayInputStream(params.getTextDocument().getText().getBytes());
      reportOnCompiledSource(getWorkspace().reParseSource(uri, inputStream));
    } catch (RuntimeException rex) {
      Logger.debug("didOpen exception: " + rex.getMessage());
    }
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    //Ignore as we implement watched files and work on saved data.
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    Logger.debug("didClose [" + params + "]");
    String uri = getFilename(params.getTextDocument());
    clearOldCompiledDiagnostics(uri);
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    //Ignore as we implement watched files and work on saved data.
  }
}
