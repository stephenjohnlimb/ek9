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
 * For operators, converts EK9 operator symbols to internal method names using OperatorMap.
 * For other symbols, returns the fully qualified name.
 */
public final class SymbolSignatureExtractor implements Function<ISymbol, String> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final OperatorMap operatorMap = new OperatorMap();

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
   * For operators, converts the operator symbol to its internal method name.
   */
  private String getMethodQualifiedName(final ISymbol symbol) {
    if (symbol instanceof IScope methodScope) {
      final var enclosingScope = methodScope.getEnclosingScope();
      if (enclosingScope instanceof ISymbol parentSymbol) {
        // Parent construct name + method name: "module::construct.methodName"
        // For operators, convert to internal method name using OperatorMap
        final var methodName = getMethodName(symbol);
        return parentSymbol.getFullyQualifiedName() + "." + methodName;
      }
    }
    // Fallback if no parent found
    return symbol.getFullyQualifiedName();
  }

  /**
   * Get the method name for a symbol.
   * For operators, converts the EK9 operator symbol to its internal method name.
   * For example: "?" -> "_isSet", "==" -> "_eq", "#?" -> "_hashcode"
   */
  private String getMethodName(final ISymbol symbol) {
    final var symbolName = symbol.getName();

    // Check if this is an operator that needs conversion
    if (symbol instanceof MethodSymbol methodSymbol && methodSymbol.isOperator()) {
      if (operatorMap.hasOperator(symbolName)) {
        return operatorMap.getForward(symbolName);
      }
    }

    // Return original name for non-operators or operators not in map
    return symbolName;
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