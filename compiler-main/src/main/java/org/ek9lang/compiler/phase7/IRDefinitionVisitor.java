package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9BaseVisitor;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.IRModule;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.SharedThreadContext;

/**
 * Deals with visiting all parts of the EK9 structure in a module (compilable source).
 * These parts are then added to the appropriate structures (in an IR graph) and then at the
 * top level (aggregate for example) are added to the IRModule.
 * <p>
 * The concept is to move away from the EK9 grammar, syntax and semantics. But does not move too
 * far towards any concrete final output.
 * </p>
 * <p>
 * So for example, everything in the IR will become some form of 'struct with operations'.
 * This includes all types of EK9 aggregates and even EK9 functions (as they can hold state).
 * </p>
 * <p>
 * Constants and actual EK9 functions will just be 'instances' of these 'struct with operations'.
 * </p>
 * <p>
 * Another example is 'for loops', 'while loops' and 'do while loops'. These will all be
 * defined in terms of executable blocks, with a 'condition' (that will appear in different locations)
 * and a 'Marker' for the start and end. So, here this is moving well towards something that can be
 * implemented in some form of machine/byte code.
 * </p>
 * <p>
 * Guards will also be broken down into 'basic blocks' with labels and a condition.
 * </p>
 */
public final class IRDefinitionVisitor extends EK9BaseVisitor<INode> {

  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private ParsedModule parsedModule;
  private final IRModule irModule;
  private final ProgramCreator programCreator;


  public IRDefinitionVisitor(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                             final CompilableSource source,
                             final IRModule irModule) {
    AssertValue.checkNotNull("compilableProgramAccess cannot be null", compilableProgramAccess);
    AssertValue.checkNotNull("source cannot be null", source);
    AssertValue.checkNotNull("irModule cannot be null", irModule);

    this.compilableProgramAccess = compilableProgramAccess;
    this.irModule = irModule;
    compilableProgramAccess.accept(compilableProgram ->
        this.parsedModule = compilableProgram.getParsedModuleForCompilableSource(source));

    programCreator = new ProgramCreator(parsedModule);

  }

  /**
   * Deals with method declarations and the specific case of EK9 programs.
   *
   * @param ctx the parse tree context that defines the method details.
   * @return An INode that represents the EK9 structure to be employed.
   */
  @Override
  public INode visitMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    if (symbol.getGenus() == SymbolGenus.PROGRAM) {
      AssertValue.checkTrue("Expect Program symbol to be Aggregate ", symbol instanceof AggregateSymbol);
      irModule.add(programCreator.apply(ctx));

    } else {
      throw new CompilerException("visitMethodDeclaration for normal methods not implemented yet");
    }
    return super.visitMethodDeclaration(ctx);
  }
}
