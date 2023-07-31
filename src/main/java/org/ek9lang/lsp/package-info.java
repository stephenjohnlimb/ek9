/**
 * <p>
 * Focus on 'language server' implementation aspects, this is really the bridge software between
 * something like VSCode and the EK9Compiler.
 * </p>
 * <p>
 * The main entry point is 'Server', this is called from 'Ek9.runAsLanguageServer', it is designed to
 * be started as a command by something like VS-Code. The LSP protocol can then use stdin,stout and stderr to
 * communicate back and forth.
 * </p>
 */

package org.ek9lang.lsp;