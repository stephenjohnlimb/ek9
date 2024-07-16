/**
 * <b>E - Initial Symbol definition</b> by traversing the 'ANTLR' - 'AST'.
 * <p>
 * This phase actually has a number of passes through the 'ANTLR' 'AST', in fact the EK9 Compiler uses both the
 * 'ANTLR' Listener and Visitor models to process the 'AST'.
 * </p>
 * <p>
 * The reason for the multiple passes is to enable 'forward referencing', each successive pass tends to add
 * more detail to the Symbols that are recorded.
 * </p>
 * <p>
 * The passes in phase 1 are:
 * </p>
 * <ul>
 *   <li>{@link org.ek9lang.compiler.phase1.SymbolDefinition}</li>
 *   <li>{@link org.ek9lang.compiler.phase1.ModuleDuplicateSymbolChecks}</li>
 *   <li>{@link org.ek9lang.compiler.phase1.ReferenceChecks}</li>
 * </ul>
 * <p>
 * These are configured in {@link org.ek9lang.compiler.config.FrontEndSupplier}
 * </p>
 * <p>
 *   The {@link org.ek9lang.compiler.phase1.SymbolDefinition} mainly uses the
 *   {@link org.ek9lang.compiler.phase1.DefinitionListener} with an 'ANTLR' - tree walker to traverse the 'AST'.
 *   There are quite a few rules and checks - even at this early stage. Most of these are simple Java 'Consumers'
 *   or 'BiConsumers' and do simple basic checks - issuing errors by calling
 *   {@link org.ek9lang.compiler.common.ErrorListener#semanticError(
 *   org.antlr.v4.runtime.Token, java.lang.String, org.ek9lang.compiler.common.ErrorListener.SemanticClassification)}
 *   with an appropriate {@link org.ek9lang.compiler.common.ErrorListener.SemanticClassification}.
 * </p>
 * <p>
 *   In general many of the phases use the 'Listener' approach and 'walk the tree'. Sometimes they process
 *   'ANTLR' contexts on <u>entry</u> and at other times on <u>exit</u>. The 'Listeners' use
 *   {@link org.ek9lang.compiler.common.SymbolsAndScopes} and this uses a
 *   {@link org.ek9lang.compiler.common.ScopeStack} to 'push and 'pop' scopes as the 'AST' is traversed.
 *   In this way it is possible to add Symbols to the correct scope during AST traversal.
 * </p>
 * <p>
 *   Most of the checks on symbols and constructs are very focussed and have a 'single responsibility'. They are
 *   then composed and applied. Initially most of these were just methods on classes. But this became quite complex,
 *   so it was refactored in to 'many' separate classes (Functions/Consumers). While there are many more it has helped
 *   enable more focus.
 * </p>
 * <p>
 *   The latter two passes are really just cross source file and module checks and as such run in a single threaded
 *   manner.
 * </p>
 * <p>
 *   See {@link org.ek9lang.compiler.phase2} for the next stage of compilation.
 * </p>
 */

package org.ek9lang.compiler.phase1;