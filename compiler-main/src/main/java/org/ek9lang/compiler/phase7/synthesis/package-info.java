/**
 * Synthetic operator and method generation for EK9.
 *
 * <p>This package contains the infrastructure for generating IR instructions for
 * synthetic operators and methods. These are operators/methods declared with the
 * 'default' keyword in EK9 source code, where the compiler generates the implementation
 * automatically based on the aggregate's fields.</p>
 *
 * <h2>Architecture</h2>
 * <p>Synthetic generation happens at IR level (Phase 7), not at the backend level.
 * This ensures:</p>
 * <ul>
 *   <li>Single implementation serves both JVM and LLVM backends</li>
 *   <li>Testable via @IR directives</li>
 *   <li>IR optimization (Phase 12) benefits both backends</li>
 *   <li>Backends remain "pretty dumb" - just translate IR instructions</li>
 * </ul>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link org.ek9lang.compiler.phase7.synthesis.SyntheticOperatorGenerator} -
 *       Main coordinator that dispatches to specific generators</li>
 *   <li>{@link org.ek9lang.compiler.phase7.synthesis.AbstractSyntheticGenerator} -
 *       Base class with common patterns (isSet guards, return blocks)</li>
 * </ul>
 *
 * <h2>Supported Operators</h2>
 * <p>For records and classes with 'default operator':</p>
 * <ul>
 *   <li>_eq (==) - Field-by-field equality</li>
 *   <li>_neq (&lt;&gt;) - Negation of _eq</li>
 *   <li>_hashcode (#?) - Combined hash of all fields</li>
 *   <li>_string ($) - String representation</li>
 *   <li>_copy (:=:) - Copy all fields</li>
 * </ul>
 *
 * <p>For enumerations (implicitly generated):</p>
 * <ul>
 *   <li>_lt, _lte, _gt, _gte - Ordinal comparisons</li>
 *   <li>_cmp (&lt;=&gt;) - Three-way comparison</li>
 *   <li>_inc, _dec (++, --) - Navigation through enum values</li>
 * </ul>
 */

package org.ek9lang.compiler.phase7.synthesis;
