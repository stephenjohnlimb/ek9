package org.ek9lang.compiler.support;

import java.util.Arrays;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.directives.ByteCodeDirective;
import org.ek9lang.compiler.directives.ComplexityDirective;
import org.ek9lang.compiler.directives.Directive;
import org.ek9lang.compiler.directives.DirectiveSpecExtractor;
import org.ek9lang.compiler.directives.DirectiveType;
import org.ek9lang.compiler.directives.DirectivesCompilationPhase;
import org.ek9lang.compiler.directives.DirectivesNextLineNumber;
import org.ek9lang.compiler.directives.ErrorDirective;
import org.ek9lang.compiler.directives.GenusDirective;
import org.ek9lang.compiler.directives.IRDirective;
import org.ek9lang.compiler.directives.ImplementsDirective;
import org.ek9lang.compiler.directives.NotResolvedDirective;
import org.ek9lang.compiler.directives.ResolvedDirective;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Just focuses on creating directives, these are used when testing EK9 source code.
 */
class DirectiveFactory extends CommonFactory {

  private final DirectiveSpecExtractor directiveSpecExtractor = new DirectiveSpecExtractor();
  private final DirectivesNextLineNumber directivesNextLineNumber = new DirectivesNextLineNumber();
  private final DirectivesCompilationPhase directivesCompilationPhase = new DirectivesCompilationPhase();


  DirectiveFactory(final ParsedModule parsedModule) {
    super(parsedModule);
  }

  public Directive newDirective(final EK9Parser.DirectiveContext ctx) {

    checkContextNotNull.accept(ctx);
    final var nameOfDirective = ctx.identifier().getText();
    try {
      final var typeOfDirective = DirectiveType.valueOf(nameOfDirective);
      return switch (typeOfDirective) {
        case Error -> newErrorDirective(ctx);
        case Complexity -> newComplexityDirective(ctx);
        case Resolved -> newResolutionDirective(ctx, true);
        case Implements -> newImplementsDirective(ctx);
        case NotResolved -> newResolutionDirective(ctx, false);
        case Genus -> newGenusDirective(ctx);
        case IR -> newIRDirective(ctx);
        case BYTECODE -> newByteCodeDirective(ctx);
        case Symbols, Compiler, Instrument ->
            throw new IllegalArgumentException("Unsupported '@" + nameOfDirective + "':");
      };
    } catch (IllegalArgumentException ex) {
      var errorListener = parsedModule.getSource().getErrorListener();
      errorListener.semanticError(ctx.start, ex.getMessage()
          + ",", ErrorListener.SemanticClassification.UNKNOWN_DIRECTIVE);
    }

    return null;
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private Directive newErrorDirective(final EK9Parser.DirectiveContext ctx) {

    checkContextNotNull.accept(ctx);
    if (ctx.directivePart().size() != 2) {
      throw new IllegalArgumentException("Expecting, compilerPhase: errorClassification");
    }

    try {
      final var applyToLine = directivesNextLineNumber.apply(ctx);
      final var compilerPhase = directivesCompilationPhase.apply(ctx);
      final var errorClassification = ErrorListener.SemanticClassification.valueOf(ctx.directivePart(1).getText());

      return new ErrorDirective(new Ek9Token(ctx.start), compilerPhase, errorClassification, applyToLine);
    } catch (IllegalArgumentException _) {
      throw new IllegalArgumentException("Expecting one of: " + Arrays.toString(
          ErrorListener.SemanticClassification.values()));
    }

  }

  private Directive newComplexityDirective(final EK9Parser.DirectiveContext ctx) {
    checkContextNotNull.accept(ctx);
    final var spec = directiveSpecExtractor.apply(ctx);

    return new ComplexityDirective(spec);
  }

  private Directive newImplementsDirective(final EK9Parser.DirectiveContext ctx) {

    checkContextNotNull.accept(ctx);
    final var spec = directiveSpecExtractor.apply(ctx);

    return new ImplementsDirective(spec);
  }

  private Directive newIRDirective(final EK9Parser.DirectiveContext ctx) {

    checkContextNotNull.accept(ctx);
    final var spec = directiveSpecExtractor.apply(ctx);

    return new IRDirective(spec);
  }

  private Directive newByteCodeDirective(final EK9Parser.DirectiveContext ctx) {

    checkContextNotNull.accept(ctx);
    final var spec = directiveSpecExtractor.apply(ctx);

    return new ByteCodeDirective(spec);
  }

  private Directive newResolutionDirective(final EK9Parser.DirectiveContext ctx, final boolean resolve) {

    checkContextNotNull.accept(ctx);
    final var spec = directiveSpecExtractor.apply(ctx);

    if (resolve) {
      return new ResolvedDirective(spec);
    }

    return new NotResolvedDirective(spec);
  }

  private Directive newGenusDirective(final EK9Parser.DirectiveContext ctx) {

    checkContextNotNull.accept(ctx);
    final var spec = directiveSpecExtractor.apply(ctx);

    return new GenusDirective(spec);
  }

}
