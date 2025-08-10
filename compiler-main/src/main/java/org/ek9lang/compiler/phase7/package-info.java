/**
 * <b>K - Intermediate Representation Generation</b>.
 * <p>
 * All the symbols and plugins should now have been resolved.
 * This means that it is now worthwhile creating the Intermediate Representation so that the
 * code can be fully analysed/optimised and prepared for code generation.
 * </p>
 * <p>
 * The key aspect of generating the IR, it to very clearly move away from EK9 semantics and structures and,
 * move very firmly and clearly to sets of instructions that are much closer to what we will need in final code
 * generation.
 * </p>
 * <p>
 * You will notice the there is some use of abbreviations in the class naming, this is because
 * words like Statement (Stmt), Definition (Dfn), Instruction (Instr) and Declaration (Decl) are quite long
 * and in combination would become excessive. Plus these abbreviations used are fairly stlandard.
 * </p>
 * <p>
 * {@link org.ek9lang.compiler.phase7.IRGenerator} is the main entry point for this phase.
 * </p>
 * <p>
 * This does not use the ANTLR visitor or listener patterns. Instead, the 'generators' follow the
 * 'flow' of the AST in a very specific way. This is to be able to generate the appropriate IR for
 * that code.
 * </p>
 */

package org.ek9lang.compiler.phase7;