package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.options.FullPhaseSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.parsing.SourceFileList;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 * Just test simple enumerations compile.
 */
class SimpleEnumerationCompilationTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private static final Supplier<Workspace> ek9Workspace = () -> {
    final SourceFileList sourceFileList = new SourceFileList();
    Workspace rtn = new Workspace();
    sourceFileList.apply("/examples/constructs/types")
        .stream()
        .forEach(rtn::addSource);

    return rtn;
  };

  @Test
  void testReferencePhasedDevelopment() {
    //Just start with the basics and most on to the next phase one implemented.
    CompilationPhase upToPhase = CompilationPhase.REFERENCE_CHECKS;

    CompilationPhaseListener listener = (phase, source) -> {
      if (!source.getErrorListener().isErrorFree()) {
        System.out.println("Errors  : " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getErrors().forEachRemaining(System.out::println);
      }
    };
    var sharedCompilableProgram= sharedContext.get();

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    assertTrue(compiler.compile(ek9Workspace.get(), new CompilerFlags(upToPhase, true)));

    sharedCompilableProgram.accept(program -> {
      //Now this should have some enumerations and records/functions.

      var theModule = program.getParsedModules("com.customer.enumerations");
      assertNotNull(theModule);

      assertEquals(1, theModule.size());
      var parsedModule = theModule.get(0);
      assertEquals(7, parsedModule.getModuleScope().getSymbolsForThisScope().size());

      var cardRank = parsedModule
          .getModuleScope()
          .resolveInThisScopeOnly(new TypeSymbolSearch("com.customer.enumerations::CardRank"));
      assertTrue(cardRank.isPresent());
      assertEquals(ISymbol.SymbolGenus.CLASS_ENUMERATION, cardRank.get().getGenus());

      var cardSuit = parsedModule
          .getModuleScope()
          .resolveInThisScopeOnly(new TypeSymbolSearch("com.customer.enumerations::CardSuit"));
      assertTrue(cardSuit.isPresent());
      assertEquals(ISymbol.SymbolGenus.CLASS_ENUMERATION, cardSuit.get().getGenus());

      var card = parsedModule
          .getModuleScope()
          .resolveInThisScopeOnly(new TypeSymbolSearch("com.customer.enumerations::Card"));
      assertTrue(card.isPresent());
      assertEquals(ISymbol.SymbolGenus.RECORD, card.get().getGenus());

      var cardCreator = parsedModule
          .getModuleScope()
          .resolveInThisScopeOnly(new FunctionSymbolSearch("com.customer.enumerations::cardCreator"));
      assertTrue(cardCreator.isPresent());
      assertEquals(ISymbol.SymbolGenus.FUNCTION_TRAIT, cardCreator.get().getGenus());

      var fullRankCreator = parsedModule
          .getModuleScope()
          .resolveInThisScopeOnly(new FunctionSymbolSearch("com.customer.enumerations::fullRankCreator"));
      assertTrue(fullRankCreator.isPresent());
      assertEquals(ISymbol.SymbolGenus.FUNCTION, fullRankCreator.get().getGenus());
    });
  }
}
