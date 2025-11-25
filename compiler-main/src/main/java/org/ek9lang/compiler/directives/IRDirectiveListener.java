package org.ek9lang.compiler.directives;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.BiPredicate;
import org.ek9lang.compiler.IRModule;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.NodePrinter;
import org.ek9lang.compiler.support.LineByLineComparator;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Checks if there are any directives that relate to @IR in the parsed module and checks the
 * resolution through the symbol and checks the IR that was generated.
 */
public class IRDirectiveListener extends ResolvedDirectiveListener {

  private final BiPredicate<String, String> lineComparator = new LineByLineComparator(System.err);

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
    //Create defensive copy to avoid ConcurrentModificationException during parallel processing
    final var irModules = List.copyOf(compilationEvent
        .parsedModule()
        .getIRModules());

    final var construct = irModules
        .stream()
        .filter(module -> module.getSource().equals(compilationEvent.source()))
        .map(IRModule::getConstructs)
        .flatMap(constructs -> List.copyOf(constructs).stream())
        .filter(toCheck -> toCheck.isForSymbol(symbol))
        .findFirst()
        .orElseThrow(
            () -> new CompilerException("Unable to locate IRConstruct: " + resolutionDirective.getSymbolName()));

    final var output = new ByteArrayOutputStream();
    try (final var printWriter = new PrintWriter(output)) {
      final var printer = new NodePrinter(printWriter);
      printer.visit(construct);
    }

    if (!lineComparator.test(resolutionDirective.getAdditionalName(), output.toString())) {
      System.err.println(output);
      throw new CompilerException("Expected IR and Generated IR line difference");
    }

  }

}
