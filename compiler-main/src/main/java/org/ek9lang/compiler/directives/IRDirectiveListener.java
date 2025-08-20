package org.ek9lang.compiler.directives;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import org.ek9lang.compiler.IRModule;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.NodePrinter;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Checks if there are any directives that relate to @IR in the parsed module and checks the
 * resolution through the symbol and checks the IR that was generated.
 */
public class IRDirectiveListener extends ResolvedDirectiveListener {

  @Override
  public void accept(final CompilationEvent compilationEvent) {

    if (compilationEvent.parsedModule() != null) {
      //Only interested in extends directives for this phase.
      final var directives =
          compilationEvent.parsedModule().getDirectives(DirectiveType.IR, compilationEvent.phase());
      if (!directives.isEmpty()) {
        processDirectives(compilationEvent, directives);
      }
    }

  }

  @Override
  protected void symbolMatch(final CompilationEvent compilationEvent,
                             final ResolutionDirective resolutionDirective,
                             final ISymbol symbol) {

    //Check the types and the like.
    super.symbolMatch(compilationEvent, resolutionDirective, symbol);

    //Now get the construct for the symbol we are checking.
    final var construct = compilationEvent
        .parsedModule()
        .getIRModules()
        .stream()
        .filter(module -> module.getSource().equals(compilationEvent.source()))
        .map(IRModule::getConstructs)
        .flatMap(List::stream)
        .filter(toCheck -> toCheck.isForSymbol(symbol))
        .findFirst()
        .orElseThrow(() -> new CompilerException("Unable to locate IRConstruct: " + resolutionDirective.getSymbolName()));

    final var output = new ByteArrayOutputStream();
    try (final var printWriter = new PrintWriter(output)) {
      final var printer = new NodePrinter(printWriter);
      printer.visit(construct);
    }

    if (!compareStringsLineByLine(resolutionDirective.getAdditionalName(), output.toString())) {
      System.err.println(output);
      throw new CompilerException("Expected IR and Generated IR line difference");
    }

  }

  private boolean compareStringsLineByLine(final String expectedIR, final String generatedIR) {
    String[] expectedLines = expectedIR.split("\\r?\\n");
    String[] generatedLines = generatedIR.split("\\r?\\n");

    if (expectedLines.length != generatedLines.length) {
      System.err.printf("Number of lines in expected versus generated differ %d != %d %n",
          expectedLines.length, generatedLines.length);
      //But carry on so that we can see where they start to diverge.

    }
    for (int i = 0; i < expectedLines.length; i++) {
      final var expected = expectedLines[i].trim();
      final var generated = generatedLines[i].trim();
      if (!expected.equals(generated)) {
        System.err.printf("Line %d differs%n", i);
        System.err.println(expected);
        System.err.println(generated);
        return false;
      }
    }
    return true;
  }

}
