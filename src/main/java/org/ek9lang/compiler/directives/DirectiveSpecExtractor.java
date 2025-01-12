package org.ek9lang.compiler.directives;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Extracts the 'spec' from the free format data in the directive.
 */
public class DirectiveSpecExtractor implements Function<EK9Parser.DirectiveContext, DirectiveSpec> {
  private final DirectivesNextLineNumber directivesNextLineNumber = new DirectivesNextLineNumber();
  private final DirectivesCompilationPhase directivesCompilationPhase = new DirectivesCompilationPhase();
  private final DirectivesSymbolCategory directivesSymbolCategory = new DirectivesSymbolCategory();
  private final DirectivesSymbolName directivesSymbolName = new DirectivesSymbolName(2);
  private final DirectivesSymbolName directivesAdditionalSymbolName = new DirectivesSymbolName(3);

  @Override
  public DirectiveSpec apply(final EK9Parser.DirectiveContext ctx) {

    final var numParams = ctx.directivePart().size();

    if (numParams != 3 && numParams != 4) {
      throw new IllegalArgumentException(
          "Expecting, compilerPhase: symbolCategory: \"type/function\": {\"type/function\"}");
    }

    final var applyToLine = directivesNextLineNumber.apply(ctx);
    final var compilerPhase = directivesCompilationPhase.apply(ctx);
    final var category = directivesSymbolCategory.apply(ctx);
    final var symbolName = directivesSymbolName.apply(ctx);
    final var additionalName = numParams == 4 ? directivesAdditionalSymbolName.apply(ctx) : null;

    return new DirectiveSpec(new Ek9Token(ctx.start), compilerPhase, category, symbolName, additionalName, applyToLine);
  }

}
