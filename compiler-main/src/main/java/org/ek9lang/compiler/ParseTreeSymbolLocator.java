package org.ek9lang.compiler;

import java.util.Optional;
import java.util.function.BiFunction;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Find the ISymbol at a given token position by traversing the parse tree.
 * Returns the recorded symbol for the deepest node matching the token.
 * This is designed to be a field within ParsedModule - constructed with the module reference.
 */
final class ParseTreeSymbolLocator
    implements BiFunction<CompilableSource, IToken, Optional<ISymbol>> {

  private final ParsedModule parsedModule;

  ParseTreeSymbolLocator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
  }

  @Override
  public Optional<ISymbol> apply(final CompilableSource source, final IToken targetToken) {

    final var root = source.getCompilationUnitContext();
    if (root == null) {
      return Optional.empty();
    }

    return findSymbolAtToken(root, targetToken);
  }

  private Optional<ISymbol> findSymbolAtToken(final ParseTree node, final IToken targetToken) {

    // Check if this node's token matches our target
    final var located = getSymbol(node, targetToken);
    if (located.isPresent()) {
      return located;
    }

    // Recurse into children (depth-first to find most specific node)
    for (int i = 0; i < node.getChildCount(); i++) {
      final var child = node.getChild(i);
      final var found = findSymbolAtToken(child, targetToken);
      if (found.isPresent()) {
        return found;
      }
    }

    return Optional.empty();
  }

  private Optional<ISymbol> getSymbol(final ParseTree node, final IToken targetToken) {

    //While the node and the target token may match, it might be the wrong structure registered.
    //This is because we use the line and character position, this could match multiple context parts.
    //Not all are recorded with the ISymbol.
    if (nodeMatchesToken(node, targetToken)) {
      return Optional.ofNullable(parsedModule.getRecordedSymbol(node));
    }
    return Optional.empty();
  }

  private boolean nodeMatchesToken(final ParseTree node, final IToken targetToken) {

    if (node instanceof TerminalNode terminal) {
      final var nodeToken = terminal.getSymbol();
      return nodeToken.getLine() == targetToken.getLine()
          && nodeToken.getCharPositionInLine() == targetToken.getCharPositionInLine();
    }

    if (node instanceof ParserRuleContext ctx && ctx.start != null) {
      // Check if target token is within this rule's span
      final var startTokenPosition = ctx.start.getCharPositionInLine();
      final var targetTokenPosition = targetToken.getCharPositionInLine();
      return ctx.start.getLine() == targetToken.getLine() && startTokenPosition == targetTokenPosition;
    }

    return false;
  }
}
