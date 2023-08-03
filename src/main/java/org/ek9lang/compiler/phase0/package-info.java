/**
 * <b>D - For the first of the compilation phases</b>;it triggers source file <b>Lexing and Parsing</b> using 'ANTLR'.
 * <p>
 * The {@link org.ek9lang.compiler.phase0.Parsing} class is used to coordinate the preparation of
 * the {@link org.ek9lang.compiler.CompilableSource} objects state; that are part of the
 * {@link org.ek9lang.compiler.Workspace}.
 * </p>
 * <p>
 * Each {@link org.ek9lang.compiler.CompilableSource} has its own {@link org.ek9lang.compiler.common.ErrorListener}
 * and error state. The methods:
 * </p>
 * <ul>
 *   <li>{@link org.ek9lang.compiler.CompilableSource#prepareToParse()}</li>
 *   <li>{@link org.ek9lang.compiler.CompilableSource#completeParsing()}</li>
 * </ul>
 * <p>
 *   Are then called in turn for each source file, note that the initial parsing of the source files is multi-threaded.
 *   This can be done in a multi-threaded manner because the compiler does not attempt to to cross reference anything.
 * </p>
 * <p>
 *   The {@link org.ek9lang.compiler.phase0.Parsing} class uses the methods within each
 *   {@link org.ek9lang.compiler.CompilableSource} to do the actual parsing.
 * </p>
 * <p>
 *   {@link org.ek9lang.compiler.tokenizer.ParserCreator} is used by the {@link org.ek9lang.compiler.CompilableSource}
 *   to create the appropriate {@link org.ek9lang.compiler.tokenizer.Ek9LexerForInput} and ANTLR based 'EK9Parser'.
 *   The {@link org.ek9lang.compiler.tokenizer.ParserCreator} configured both the lexer and the parser in an appropriate
 *   manner (typically error listening).
 * </p>
 * <p>
 *   It is the 'double team' of the Lexer and Parser that opens the file, scans for tokens and then pulls those tokens
 *   in to build an in-memory 'AST'. If there is anything wrong in the code at this stage, the errors will be
 *   'ANTLR' style errors.
 * </p>
 * <p>
 *   <b>Only if this phase fully passes for every single source file do the EK9 Compiler continue to the next phase</b>.
 * </p>
 * <p>
 *   This same philosophy applies in all phases, all files must pass each phase before the compiler moves on.
 *   Importantly the EK9 Compiler attempts to identify issues in code as early as possible in each phase. It does not
 *   attempt to process most of the code to build an 'IR' and then assess the IR. Many of the rules for error detection
 *   are contained within each phase. There is an 'IR' generation phase and error assessment, but EK9 attempts to give
 *   error feedback as soon as it can.
 * </p>
 * <p>
 *   So you may be thinking 'but where is the state stored'? Basically for phase 0, the
 *   {@link org.ek9lang.compiler.CompilableSource} keeps a reference to the 'ANTLR' 'EK9Parser.CompilationUnitContext'.
 *   The {@link org.ek9lang.compiler.CompilableSource} is used in most of the other phases in conjunction with
 *   {@link org.ek9lang.compiler.ParsedModule} and the {@link org.ek9lang.compiler.CompilableProgram} to build uo
 *   state.
 * </p>
 * <p>
 *   The type of symbols that are available from {@link org.ek9lang.compiler.symbols} are added to the
 *   appropriate {@link org.ek9lang.compiler.ParsedModule}.
 * </p>
 * <p>
 *   See {@link org.ek9lang.compiler.phase1} for the next stage of compilation.
 * </p>
 */

package org.ek9lang.compiler.phase0;