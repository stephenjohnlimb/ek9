package org.ek9lang.compiler.phase7;

import java.util.Comparator;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.ProgramMetadataExtractor;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Construct for an Aggregate of type 'program'.
 */
final class ProgramDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.MethodDeclarationContext, IRConstruct> {

  private final ProgramMetadataExtractor programMetadataExtractor = new ProgramMetadataExtractor();

  /**
   * Constructor using stack context - the single source of state.
   */
  ProgramDfnGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  @Override
  public IRConstruct apply(final EK9Parser.MethodDeclarationContext ctx) {
    final var symbol = getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    if (symbol instanceof AggregateSymbol aggregateSymbol && symbol.getGenus() == SymbolGenus.PROGRAM) {

      final var construct = new IRConstruct(symbol);

      // Create the PROGRAM_ENTRY_POINT_BLOCK with all discovered programs
      createProgramEntryPointBlock(construct);

      //Now for 'programs' we have used just a 'method' in the grammar, mangled it to an aggregate.
      //then created an artificial method on that aggregate - so there is no 'parse tree' for this.
      //We must now manually make the OperationInstr and then we can jump back to the parse tree (ctx) and
      //process that.
      createOperations(construct, aggregateSymbol, ctx);
      return construct;
    }
    throw new CompilerException("Cannot create Program - expect AggregateSymbol of 'PROGRAM' Genus");
  }

  /**
   * Create the PROGRAM_ENTRY_POINT_BLOCK containing all discovered programs.
   * Uses the cached AllProgramsSupplier data from IRModule.
   * Adds the same block to each program construct for backend convenience.
   */
  private void createProgramEntryPointBlock(final IRConstruct construct) {
    final var irModule = stackContext.getCurrentIRContext().getIrModule();

    // Get all programs discovered during compilation (cached in IRModule)
    final var allPrograms = irModule.getAllPrograms();

    //Ensure they always come out in sorted order so @IR is deterministic.
    final var programDefinitions = allPrograms
        .stream()
        .sorted(Comparator.comparing(Symbol::getFullyQualifiedName))
        .map(programMetadataExtractor)
        .toList();

    // Create the IR instruction and add to this program construct
    final var programEntryPoint = new ProgramEntryPointInstr(programDefinitions, null);
    construct.setProgramEntryPoint(programEntryPoint);
  }

  private void createOperations(final IRConstruct construct, final AggregateSymbol aggregateSymbol,
                                final EK9Parser.MethodDeclarationContext ctx) {
    AssertValue.checkTrue("Expecting two method on program",
        aggregateSymbol.getAllMethods().size() == 2);

    // Add c_init and i_init BEFORE the constructor
    createProgramInitMethod(construct, aggregateSymbol, IRConstants.C_INIT_METHOD);
    createProgramInitMethod(construct, aggregateSymbol, IRConstants.I_INIT_METHOD);
    populateConstructor(construct, aggregateSymbol);
    populateMainMethod(construct, aggregateSymbol, ctx);

  }

  private void populateConstructor(final IRConstruct construct,
                                   final AggregateSymbol aggregateSymbol) {
    final var constructor = aggregateSymbol
        .getAllMethods()
        .stream()
        .filter(MethodSymbol::isConstructor)
        .findFirst().orElseThrow();

    // Now process the synthetic constructor which will call the i_init we just created
    super.processSyntheticConstructor(construct, constructor, null);

  }

  private void populateMainMethod(final IRConstruct construct,
                                  final AggregateSymbol aggregateSymbol,
                                  final EK9Parser.MethodDeclarationContext ctx) {

    final var context = newPerConstructContext();
    final var mainMethod = aggregateSymbol
        .getAllMethods()
        .stream()
        .filter(method -> !method.isConstructor())
        .findFirst().orElseThrow();

    final var debugInfo = new DebugInfoCreator(context).apply(mainMethod.getSourceToken());
    final var operation = new OperationInstr(mainMethod, debugInfo);
    operationDfnGenerator.accept(operation, ctx.operationDetails());
    construct.add(operation);
  }

  private void createProgramInitMethod(final IRConstruct construct,
                                       final AggregateSymbol aggregateSymbol,
                                       final String methodName) {
    // Create synthetic init method for programs (no-op)
    final var initOperation = newSyntheticInitOperation(aggregateSymbol, methodName);

    stackContext.enterMethodScope(methodName, initOperation.getDebugInfo(), IRFrameType.METHOD);

    var instructionBuilder = new IRInstructionBuilder(stackContext);
    instructionBuilder.returnVoid();

    initOperation.setBody(instructionBuilder.createBasicBlock(IRConstants.ENTRY_LABEL));
    construct.add(initOperation);
    stackContext.exitScope();
  }

}
