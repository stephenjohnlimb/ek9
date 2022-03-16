package org.ek9lang.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.lsp.EK9LanguageWords.KeyWordInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EK9TextDocumentService extends EK9Service implements TextDocumentService
{
	private final EK9LanguageWords languageWords;

	public EK9TextDocumentService(EK9LanguageServer languageServer)
	{
		super(languageServer);
		languageWords = new EK9LanguageWords();
	}

	protected EK9LanguageWords getLanguageWords()
	{
		return languageWords;
	}

	/**
	 * Go to definition of symbol
	 */
	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params)
	{
		return CompletableFuture.supplyAsync(() -> {
			System.err.println("Would do definition of [" + params.toString() + "]");
			List<? extends Location> rtn = new ArrayList<>();
			//TODO search for definition in symbol table and populate location
			return Either.forLeft(rtn);
		});
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> declaration(DeclarationParams params)
	{
		return CompletableFuture.supplyAsync(() -> {
			System.err.println("Would do declaration of [" + params.toString() + "]");
			List<? extends Location> rtn = new ArrayList<>();
			//TODO search for a declaration in symbol table and populate location
			return Either.forLeft(rtn);
		});
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params)
	{
		return CompletableFuture.supplyAsync(() -> {
			System.err.println("Would do references of [" + params.toString() + "]");
			//TODO search for reference use in symbol table and populate location
			return new ArrayList<>();
		});
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position)
	{
		return CompletableFuture.supplyAsync(() -> {
			List<CompletionItem> list = new ArrayList<>();

			TokenResult tokenResult = getNearestToken(position);
			if(tokenResult.isPresent())
			{
				List<String> languageMatches = getLanguageWords().fuzzyMatch(tokenResult);

				languageMatches.forEach(completion -> {
					if(list.size() < getLanguageServer().getCompilerConfig().getNumberOfSuggestions())
						list.add(new CompletionItem(completion));
				});
				//TODO add in Symbol matches as well
			}

			return Either.forLeft(list);
		});
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params)
	{
		return CompletableFuture.supplyAsync(() -> {

			TokenResult tokenResult = getNearestToken(params);
			if(tokenResult.isPresent())
			{
				//TODO Now we'd do a symbol lookup here.
				KeyWordInformation match = null;
				//If not a match on the symbols, and we're configured just do a language keyword search.
				if(match == null && getLanguageServer().getCompilerConfig().isProvideLanguageHoverHelp())
				{
					match = getLanguageWords().exactMatch(tokenResult);
					if(match != null)
					{
						MarkupContent markedUp = new MarkupContent("plaintext", match.hoverText);
						return new Hover(markedUp);
					}
				}
			}
			return null;
		});
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params)
	{
		try
		{
			String uri = getFilename(params.getTextDocument());
			System.err.println("didOpen Opened Source [" + uri + "]");
			reportOnCompiledSource(getWorkspace().reParseSource(uri));
		}
		catch(RuntimeException rex)
		{
			System.err.println("didOpen exception: " + rex.getMessage());
		}
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params)
	{
		try
		{
			String uri = getFilename(params.getTextDocument());
			System.err.println("didChange Changed Source [" + uri + "]");
			reportOnCompiledSource(getWorkspace().reParseSource(uri));
		}
		catch(RuntimeException rex)
		{
			System.err.println("didChange exception: " + rex.getMessage());
		}
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params)
	{
		System.err.println("didClose [" + params + "]");
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params)
	{
		try
		{
			String uri = getFilename(params.getTextDocument());
			System.err.println("didSave Changed Source [" + uri + "]");
			reportOnCompiledSource(getWorkspace().reParseSource(uri));
		}
		catch(RuntimeException rex)
		{
			System.err.println("didSave exception: " + rex.getMessage());
		}
	}
}
