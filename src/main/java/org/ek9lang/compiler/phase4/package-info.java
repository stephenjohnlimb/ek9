/**
 * <b>H - Generic/Parameterised types</b> can have their methods checked when used in specific contexts.
 * <p>
 * If the EK9 developer has defined any generic types and has used them with specific parameterizing types
 * then the use of the generic type with the specific type must now be checked.
 * </p>
 * <p>
 * {@link org.ek9lang.compiler.phase4.PostSymbolResolutionChecks} is the main entry point for this phase. Most of the
 * processing is delegated to {@link org.ek9lang.compiler.phase4.ParameterisedTypeOrError}.
 * </p>
 * <p>
 * This now follows the same pattern as most of the other Listeners, using
 * {@link org.ek9lang.compiler.common.SymbolsAndScopes} for handling scope traversals and locating recorded
 * symbols and scopes.
 * </p>
 * <p>
 * The model of use of generics/template types in EK9 is to assume that the generic parameters of say 'T', 'K' or 'V'
 * for example has all the normal operators on and may also have constructors. This allows a generic type to be defined
 * so that the 'T' or whatever can be used in quite a full manner (or at least as full as it needs to be).
 * </p>
 * <p>
 * However, when the generic type is actually parameterized with a set of type arguments, the assumptions must then be
 * checked. It maybe for example that the generic type expected the 'T' to have an 'equality' operator, but when used
 * with a real parameterizing argument (type), that type did not actually implement the 'equality' operator. Hence, the
 * generic type cannot actually be used with the parameterizing argument (type). An error must be emitted for this.
 * </p>
 */

package org.ek9lang.compiler.phase4;