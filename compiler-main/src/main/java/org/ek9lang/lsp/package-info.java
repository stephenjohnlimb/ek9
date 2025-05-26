/**
 * <p>
 * <b>A-2 - Start here for tooling entry point</b> specifically 'language server' implementation.
 * This is really the bridge software between something like VSCode and the EK9Compiler, it does not
 * produce a finally executable. It is designed for UI tooling to complete all the front/middle end of
 * the compilation process.
 * </p>
 * <p>
 * The main entry point is 'Server', this is called from 'Ek9.runAsLanguageServer', it is designed to
 * be started as a command by something like VS-Code. The LSP protocol can then use stdin,stout and stderr to
 * communicate back and forth.
 * </p>
 */

package org.ek9lang.lsp;