/**
 * <b>G - Full Symbol Resolution</b> can now take place.
 * <p>
 * If the EK9 developer has defined all the necessary symbols then this whole phase should be able to resolve them.
 * This includes Parameterized Generic/Template types.
 * </p>
 * <p>
 * {@link org.ek9lang.compiler.phase3.SymbolResolution} is the main entry point for this phase. Most of the
 * processing is delegated to {@link org.ek9lang.compiler.phase3.ResolveDefineInferredTypeListener}.
 * </p>
 * <p>
 * This now follows the same pattern as most of the other Listeners, using
 * {@link org.ek9lang.compiler.common.SymbolsAndScopes} for handling scope traversals and locating recorded
 * symbols and scopes.
 * </p>
 * <p>
 * This phase is quite complex because it involves processing 'calls' and
 * 'expressions'. There are also lot's more rules that can now be applied, because all 'types' are present.
 * </p>
 */

package org.ek9lang.compiler.phase3;