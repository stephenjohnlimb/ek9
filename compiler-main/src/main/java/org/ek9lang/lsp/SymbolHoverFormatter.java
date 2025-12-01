package org.ek9lang.lsp;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IFunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Formats ISymbol information as hover content for the LSP.
 * Creates markdown-formatted hover text showing type information,
 * parameters, return types, and modifiers.
 */
final class SymbolHoverFormatter implements Function<Optional<ISymbol>, Hover> {

  @Override
  public Hover apply(final Optional<ISymbol> symbol) {

    return symbol.map(this::createHover).orElse(null);
  }

  /**
   * Create hover content from a symbol.
   */
  private Hover createHover(final ISymbol symbol) {

    final var content = formatSymbol(symbol);
    final var markup = new MarkupContent(MarkupKind.MARKDOWN, content);

    return new Hover(markup);
  }

  private String formatSymbol(final ISymbol symbol) {

    final var category = symbol.getCategory();

    return switch (category) {
      case VARIABLE -> formatVariable(symbol);
      case FUNCTION, TEMPLATE_FUNCTION -> formatFunction(symbol);
      case METHOD -> formatMethod(symbol);
      case TYPE, TEMPLATE_TYPE -> formatType(symbol);
      case CONTROL -> formatControl(symbol);
      case ANY -> formatAny(symbol);
    };
  }

  private String formatVariable(final ISymbol symbol) {

    final var builder = new StringBuilder();
    final var typeName = symbol.getType()
        .map(ISymbol::getFriendlyName)
        .orElse("unknown");

    builder.append("**").append(symbol.getName()).append("**");
    builder.append(" : `").append(typeName).append("`\n\n");
    builder.append("*Kind*: Variable\n");

    if (!symbol.isMutable()) {
      builder.append("*Modifier*: constant\n");
    }

    appendSourceLocation(builder, symbol);

    return builder.toString();
  }

  private String formatFunction(final ISymbol symbol) {

    final var builder = new StringBuilder();
    builder.append("**").append(symbol.getFriendlyName()).append("**\n\n");

    if (symbol instanceof IFunctionSymbol functionSymbol) {
      appendParameters(builder, functionSymbol);
    }

    appendReturnType(builder, symbol);
    appendModifiers(builder, symbol);
    appendSourceLocation(builder, symbol);

    return builder.toString();
  }

  private String formatMethod(final ISymbol symbol) {

    final var builder = new StringBuilder();
    builder.append("**").append(symbol.getName()).append("**\n\n");

    if (symbol instanceof MethodSymbol methodSymbol) {
      final var parentScope = methodSymbol.getParentScope();
      if (parentScope != null) {
        builder.append("*On*: `").append(parentScope.getFriendlyScopeName()).append("`\n");
      }
      appendParameters(builder, methodSymbol);
    }

    appendReturnType(builder, symbol);
    appendModifiers(builder, symbol);
    appendSourceLocation(builder, symbol);

    return builder.toString();
  }

  private String formatType(final ISymbol symbol) {

    final var builder = new StringBuilder();
    builder.append("**").append(symbol.getFriendlyName()).append("**\n\n");
    builder.append("*Kind*: ").append(symbol.getGenus().getDescription()).append("\n");

    if (symbol.isGenericInNature()) {
      builder.append("*Generic*: yes\n");
    }
    if (symbol.isParameterisedType()) {
      builder.append("*Parameterised*: yes\n");
    }

    appendSourceLocation(builder, symbol);

    return builder.toString();
  }

  private String formatControl(final ISymbol symbol) {

    final var builder = new StringBuilder();
    builder.append("**").append(symbol.getName()).append("**\n\n");
    builder.append("*Kind*: Control structure\n");

    appendReturnType(builder, symbol);
    appendSourceLocation(builder, symbol);

    return builder.toString();
  }

  private String formatAny(final ISymbol symbol) {

    final var builder = new StringBuilder();
    builder.append("**").append(symbol.getFriendlyName()).append("**\n\n");
    builder.append("*Kind*: ").append(symbol.getGenus().getDescription()).append("\n");

    appendSourceLocation(builder, symbol);

    return builder.toString();
  }

  private void appendParameters(final StringBuilder builder, final IFunctionSymbol symbol) {

    final var params = symbol.getCallParameters();
    if (!params.isEmpty()) {
      final var paramList = params.stream()
          .filter(p -> p.isIncomingParameter() || !p.isReturningParameter())
          .map(this::formatParameter)
          .collect(Collectors.joining(", "));
      if (!paramList.isEmpty()) {
        builder.append("*Parameters*: `(").append(paramList).append(")`\n");
      }
    }
  }

  private void appendParameters(final StringBuilder builder, final MethodSymbol symbol) {

    final var params = symbol.getCallParameters();
    if (!params.isEmpty()) {
      final var paramList = params.stream()
          .filter(p -> p.isIncomingParameter() || !p.isReturningParameter())
          .map(this::formatParameter)
          .collect(Collectors.joining(", "));
      if (!paramList.isEmpty()) {
        builder.append("*Parameters*: `(").append(paramList).append(")`\n");
      }
    }
  }

  private String formatParameter(final ISymbol param) {

    final var typeName = param.getType()
        .map(ISymbol::getFriendlyName)
        .orElse("?");

    return param.getName() + " as " + typeName;
  }

  private void appendReturnType(final StringBuilder builder, final ISymbol symbol) {

    if (symbol instanceof FunctionSymbol functionSymbol) {
      final var returnSymbol = functionSymbol.getReturningSymbol();
      if (returnSymbol != null) {
        returnSymbol.getType().ifPresent(returnType ->
            builder.append("*Returns*: `").append(returnType.getFriendlyName()).append("`\n"));
      }
    } else if (symbol instanceof MethodSymbol methodSymbol) {
      final var returnSymbol = methodSymbol.getReturningSymbol();
      if (returnSymbol != null) {
        returnSymbol.getType().ifPresent(returnType ->
            builder.append("*Returns*: `").append(returnType.getFriendlyName()).append("`\n"));
      }
    } else {
      symbol.getType().ifPresent(type ->
          builder.append("*Type*: `").append(type.getFriendlyName()).append("`\n"));
    }
  }

  private void appendModifiers(final StringBuilder builder, final ISymbol symbol) {

    if (symbol.isMarkedPure()) {
      builder.append("*Modifier*: pure\n");
    }
    if (symbol.isMarkedAbstract()) {
      builder.append("*Modifier*: abstract\n");
    }
  }

  private void appendSourceLocation(final StringBuilder builder, final ISymbol symbol) {

    final var sourceToken = symbol.getSourceToken();
    if (sourceToken != null) {
      builder.append("\n*Defined at*: Line ").append(sourceToken.getLine());
    }
  }
}
