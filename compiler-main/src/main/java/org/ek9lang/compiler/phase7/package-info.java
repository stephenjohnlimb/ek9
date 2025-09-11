/**
 * <b>K - Intermediate Representation Generation</b>.
 * <p>
 * All the symbols and plugins should now have been resolved.
 * This means that it is now worthwhile creating the Intermediate Representation so that the
 * code can be fully analysed/optimised and prepared for code generation.
 * </p>
 * <p>
 * The key aspect of generating the IR, it to start to move away from EK9 semantics and structures and,
 * move very firmly and clearly to sets of instructions that are much closer to what we will need in final code
 * generation. However, the IR we generate here is more of an MIR (medium level).
 * </p>
 * <p>
 * I decided to do this so that there is more opportunity to use optimisation both in the EK9 MIR phase 12, but
 * also to enable back-ends to have more of an overall 'context' to apply further optimisations.
 * </p>
 * <p>
 * You will notice the there is some use of abbreviations in the class naming, this is because
 * words like Statement (Stmt), Definition (Dfn), Instruction (Instr) and Declaration (Decl) are quite long
 * and in combination would become excessive. Plus these abbreviations used are fairly standard.
 * </p>
 * <p>
 * {@link org.ek9lang.compiler.phase7.IRGenerator} is the main entry point for this phase.
 * </p>
 * <p>
 * This does not use the ANTLR visitor or listener patterns. Instead, the 'generators' follow the
 * 'flow' of the AST in a very specific way. This is to be able to generate the appropriate IR for
 * that code.
 * </p>
 * <p>
 *   This phase and the generation of the IR is quite 'fiddly' and by its nature a single simple expression or
 *   statement in ek9, will actually result in quite a bit of code being executed to generate quite a lot of MIR.
 *   For example:<br>
 * </p>
 * <pre>
 *   birdA &lt;- bird1 &lt;? bird2
 * </pre>
 * <p>
 *   While a very simple single line statement in ek9 will generate a significant amount of IR. There are implicit
 *   '_isSet' calls being made, checks for 'null', memory management of temporary variables (to deal with possible
 *   exceptions). The above example just uses variable 'bird1'. But imaging a function/method call, that could throw
 *   and exception. We'd need to manage memory (heap retain/release, or stack (with auto release).
 * </p>
 */

package org.ek9lang.compiler.phase7;