/**
 * <b>Contains critical general components</b> used in the {@link org.ek9lang.compiler.Ek9Compiler} listeners.
 * <p>
 * The {@link org.ek9lang.compiler.support.SymbolFactory} and {@link org.ek9lang.compiler.support.AggregateFactory}
 * are the primary sources of new Symbols or additions to exiting symbols.
 * There are also other classes like {@link org.ek9lang.compiler.support.ResolveOrDefineExplicitParameterizedType} that
 * take generic types and parameters and create new parameterised types.
 * </p>
 * <p>
 * So this package is mainly focussed on creation of symbols and validation (to some extent).
 * There are some quite complex processes going on in here, specifically
 * {@link org.ek9lang.compiler.support.TypeSubstitution}.
 * </p>
 * <p>
 * Another set of classes also deal with the thorny issues around defining and creating parameterised
 * types (but take into account chaining the need to create further dependent parameterised types).
 * </p>
 * <ul>
 *   <li>{@link org.ek9lang.compiler.support.ResolverOrDefiner}</li>
 *   <li>{@link org.ek9lang.compiler.support.ResolveOrDefineTypes}</li>
 *   <li>{@link org.ek9lang.compiler.support.ResolveOrDefineExplicitParameterizedType}</li>
 *   <li>{@link org.ek9lang.compiler.support.ResolveOrDefineIdentifierReference}</li>
 *   <li>{@link org.ek9lang.compiler.support.ResolveOrDefineTypeDef}</li>
 * </ul>
 * <p>
 *   One of the trickiest bits of code is around the creation of parameterised types, this is explained below.
 * </p>
 * <p>
 * The method
 * {@link org.ek9lang.compiler.symbols.ModuleScope#resolveOrDefine(
 *org.ek9lang.compiler.symbols.PossibleGenericSymbol,
 * org.ek9lang.compiler.common.ErrorListener)}
 * calls into
 * the {@link org.ek9lang.compiler.CompilableProgram#resolveOrDefine(
 *org.ek9lang.compiler.symbols.PossibleGenericSymbol)}
 * This code checks if a parameterised symbol already exists for what is being asked for (the symbol being asked for
 * is not fully defined yet as it is just a form of lookup).
 * This 'lookup' originated in
 * {@link org.ek9lang.compiler.support.SymbolFactory#newParameterisedSymbol(
 *org.ek9lang.compiler.symbols.PossibleGenericSymbol, java.util.List)}.
 * That method delegated to {@link org.ek9lang.compiler.support.ParameterizedSymbolCreator}.
 * It was then 'routed via {@link org.ek9lang.compiler.common.SymbolsAndScopes}, this is a critical component
 * used in almost all the 'listeners' that are called when the 'ANTLR' AST is traversed.
 * </p>
 * <p>
 *   Anyway, back to the point; There is a little nuance that needs to be highlighted because it is different from how
 *   all other symbols are created. Almost all other symbols are created by the traversal of the AST and then the code
 *   that the EK9 developer writes gets pushed into the appropriate symbol (i.e. methods, properties, etc.).
 *   But when parameterizing generic types (polymorphic types), we have essentially a 'template' but using conceptual
 *   types like 'T' for example.
 * </p>
 * <p>
 *   So when we want to 'define or lookup' a parameterized polymorphic type if that type exists in the
 *   appropriate module we just want to reference it. So if the type does exist then we're all good and
 *   {@link org.ek9lang.compiler.ResolvedOrDefineResult} is returned with the symbol and a flag stating that it is
 *   <b>not</b> newly defined.
 * </p>
 * <p>
 *   But if it does not exist, then {@link org.ek9lang.compiler.CompilableProgram} will add the 'skeleton'
 *   {@link org.ek9lang.compiler.symbols.PossibleGenericSymbol} to the appropriate module, <b>but</b> will now set the
 *   returning flag to <b>new defined</b>.
 *   This is really critical, because now that 'skeleton' needs to actually be parameterised.
 *   This is done in {@link org.ek9lang.compiler.support.TypeSubstitution}
 *   (it is called by {@link org.ek9lang.compiler.symbols.ModuleScope} and it checks if the type is newly defined
 *   or not, if not then it goes about the task of parameterizing it.
 * </p>
 * <p>
 *   The Type Substitution is now driven from the phase of compilation, so the Parameterized Types stay as skeletons
 *   for much longer. Only at full resolution are they expanded.
 * </p>
 * <p>
 *   <b>But</b> there's a wrinkle, what if during that parameterization process more parameterized types are needed?
 *   The class {@link org.ek9lang.compiler.support.TypeSubstitution} does recursive calls back into the
 *   {@link org.ek9lang.compiler.CompilableProgram} calling
 *   {@link org.ek9lang.compiler.CompilableProgram#resolveOrDefine(org.ek9lang.compiler.symbols.PossibleGenericSymbol)}
 *   for its needs.
 * </p>
 * <p>
 *   So this is why the construction of parameterized types is in phases, because during parameterization there could
 *   (probably will) be references to other parameterized types that are in the process of being created.
 *   So by recording the 'skeleton' in the appropriate module, the {@link org.ek9lang.compiler.CompilableProgram} will
 *   return 'true' for newly defined the first time, but 'false' on additional requests (even though it is still just
 *   a skeleton). Eventually (just like any recursive function) the stack of Parameterized types being created will
 *   'unwind', resulting in any newly defined Parameterized types being created/record and then having methods and
 *   properties populated with the appropriate types.
 * </p>
 * <p>
 *   The end result is that the first time a Parameterized type is needed it is recorded and populated, but subsequent
 *   requests result in that initial type being returned. This is why the recording aspect is done within the
 *   {@link org.ek9lang.compiler.CompilableProgram}, because access is controlled within a mutex lock.
 * </p>
 */

package org.ek9lang.compiler.support;