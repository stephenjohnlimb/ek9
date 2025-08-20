package org.ek9lang.compiler.ir;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Debug information for IR instructions to enable source mapping and debugging.
 * Contains original source file location information extracted from ISymbol source tokens.
 */
public record DebugInfo(
    String sourceFile,      // Original .ek9 file path
    int lineNumber,         // 1-based line number
    int columnNumber,       // 1-based column number
    String originalText     // Original EK9 source text (optional, can be null)
) {

  /**
   * Create DebugInfo from an ISymbol's source token.
   * Returns null if the symbol has no source token information.
   * Uses filename only (without path) for portability and consistency with EK9 error reporting.
   */
  public static DebugInfo from(final CompilableSource compilableSource, final IToken token) {
    if (token != null) {
      return new DebugInfo(
          compilableSource.getRelativeFileName(),
          token.getLine(),
          token.getCharPositionInLine() + 1, // Convert to 1-based column
          token.getText()
      );
    }
    return null;
  }

  /**
   * Create DebugInfo with just file and position (no original text).
   */
  public static DebugInfo of(String sourceFile, int lineNumber, int columnNumber) {
    return new DebugInfo(sourceFile, lineNumber, columnNumber, null);
  }

  /**
   * Format debug info as IR comment for output.
   * Example: "// workarea.ek9:12:15"
   */
  public String toIRComment() {
    return String.format("// %s:%d:%d", sourceFile, lineNumber, columnNumber);
  }

  /**
   * Check if this debug info represents a valid source location.
   */
  public boolean isValidLocation() {
    return sourceFile != null && !sourceFile.isEmpty() && lineNumber > 0 && columnNumber > 0;
  }

  /**
   * Compact constructor with validation.
   */
  public DebugInfo {
    Objects.requireNonNull(sourceFile, "sourceFile cannot be null");
    if (lineNumber < 1) {
      throw new IllegalArgumentException("lineNumber must be >= 1, got: " + lineNumber);
    }
    if (columnNumber < 1) {
      throw new IllegalArgumentException("columnNumber must be >= 1, got: " + columnNumber);
    }
  }

  @Override
  @Nonnull
  public String toString() {
    return toIRComment();
  }
}