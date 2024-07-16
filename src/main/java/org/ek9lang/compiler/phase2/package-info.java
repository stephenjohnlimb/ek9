/**
 * <b>F - Resolution of Explicitly Defined Type Symbols</b> at this point phase1 processing should have defined
 * most of the explicitly defined symbols, so they should be resolvable.
 * <p>
 * {@link org.ek9lang.compiler.phase2.NonInferredTypeDefinition} is the main entry point here and it
 * delegates through to {@link org.ek9lang.compiler.phase2.ResolveDefineExplicitTypeListener}.
 * Again the {@link org.ek9lang.compiler.common.SymbolsAndScopes} class is used to manage the scopes.
 * </p>
 * <p>
 * The Symbols and scopes that were defined in phase1 are now lookup up using the
 * {@link org.ek9lang.compiler.common.SymbolsAndScopes} and in some cases augmented or in other cases checked.
 * </p>
 * <p>
 * Following the pass described above and second pass of the 'AST' is done using
 * {@link org.ek9lang.compiler.phase2.TypeHierarchyChecks}. This is focussed on checking for loops in type hierarchies.
 * </p>
 * <p>
 * See {@link org.ek9lang.compiler.phase3} for the next stage of compilation.
 * </p>
 */

package org.ek9lang.compiler.phase2;