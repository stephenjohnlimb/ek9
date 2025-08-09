package org.ek9lang.compiler.common;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.IFunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Function that extracts signature-qualified-name from a symbol.
 * For functions and methods, includes parameters and return type.
 * For methods, includes the parent construct name.
 * For other symbols, returns the fully qualified name.
 */
public final class SymbolSignatureExtractor implements Function<ISymbol, String> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();

  @Override
  public String apply(final ISymbol symbol) {
    String baseQualifiedName;

    // For methods, include the parent construct name
    if (symbol.isMethod()) {
      baseQualifiedName = getMethodQualifiedName(symbol);
    } else {
      baseQualifiedName = symbol.getFullyQualifiedName();
    }

    // Only add signature details for functions and methods
    if (!symbol.isFunction() && !symbol.isMethod()) {
      return baseQualifiedName;
    }

    return buildSignatureQualifiedName(symbol, baseQualifiedName);
  }

  /**
   * Get qualified name for a method including parent construct.
   * Format: "module::construct.methodName"
   */
  private String getMethodQualifiedName(final ISymbol symbol) {
    if (symbol instanceof IScope methodScope) {
      final var enclosingScope = methodScope.getEnclosingScope();
      if (enclosingScope instanceof ISymbol parentSymbol) {
        // Parent construct name + method name: "module::construct.methodName"
        return parentSymbol.getFullyQualifiedName() + "." + symbol.getName();
      }
    }
    // Fallback if no parent found
    return symbol.getFullyQualifiedName();
  }

  /**
   * Build signature-qualified name with parameters and return type.
   */
  private String buildSignatureQualifiedName(final ISymbol symbol, final String baseQualifiedName) {
    final var signature = new StringBuilder(baseQualifiedName);

    // Add parameter types
    signature.append("(");
    final var parameterTypes = getParameterTypeNames(symbol);
    signature.append(String.join(",", parameterTypes));
    signature.append(")");

    // Add return type
    signature.append("->");
    signature.append(typeNameOrException.apply(symbol));

    return signature.toString();
  }

  /**
   * Extract parameter type names from the symbol.
   */
  private List<String> getParameterTypeNames(final ISymbol symbol) {

    // Get parameters from function or method symbol
    return switch (symbol) {
      case IFunctionSymbol functionSymbol ->
          functionSymbol.getCallParameters().stream().map(typeNameOrException).toList();
      case MethodSymbol methodSymbol -> methodSymbol.getCallParameters().stream().map(typeNameOrException).toList();
      default -> List.of();
    };
  }
}