/**
 * <b>K - Intermediate Representation Generation</b>.
 * <p>
 * All the symbols and plugins should now have been resolved.
 * This means that it is now worthwhile creating the Intermediate Representation so that the
 * code can be fully analysed/optimised and prepared for code generation.
 * </p>
 * <p>
 *   The key aspect of generating the IR, it to very clearly move away from EK9 semantics and structures and,
 *   move very firmly and clearly to sets of instructions that are much closer to what we will need in final code
 *   generation.
 * </p>
 * <p>
 *   In this respect a simple ek9 expression like:
 * </p>
 * <pre>
 *   var1 &lt;- "Goose"  // Set to "Goose"
 *   var2 &lt;- "Duck"   // Set to "Duck"
 *
 *   lesserThan &lt;- var1 &lt;? var2    // Should be "Duck" (Duck &lt; Goose alphabetically)
 *   greaterThan &lt;- var1 &gt;? var2   // Should be "Goose" (Goose &gt; Duck alphabetically)
 *
 * </pre>
 * <p>
 *   When actually expanded to an IR it would be more like (are you ready?).
 *   We are moving into old style Assembly type code, basically we are using
 *   SSA (Static Single Assignment)
 * </p>
 * <pre>
 *   BasicBlock: _coalescing_block_1
 *     // Load operands
 *     _temp1 = LOAD var1          // _temp1 = "Goose"
 *     _temp2 = LOAD var2          // _temp2 = "Duck"
 *
 *     // Check if var1 is set
 *     _temp3 = CALL _temp1._isSet()    // _temp3 = true
 *     BRANCH_FALSE _temp3, _var1_unset_1   // Skip (var1 is set)
 *
 *     // var1 is set - check if var2 is set
 *     _temp4 = CALL _temp2._isSet()    // _temp4 = true
 *     BRANCH_FALSE _temp4, _var2_unset_1   // Skip (var2 is set)
 *
 *     // Both are set - do comparison var1 &lt; var2 ("Goose" &lt; "Duck")
 *     _temp5 = CALL _temp1._lt(_temp2)     // _temp5 = false (Goose > Duck)
 *     _temp6 = CALL _temp5._isSet()        // _temp6 = true (comparison valid)
 *     BRANCH_FALSE _temp6, _comparison_invalid_1   // Skip (comparison valid)
 *
 *     // Comparison is valid - use ternary logic
 *     BRANCH_TRUE _temp5, _var1_smaller_1  // Skip (false - var1 not smaller)
 *
 *     // var1 &gt;= var2, so use var2 (the smaller one for &lt;? operator)
 *     _temp_result = STORE _temp2          // _temp_result = "Duck"
 *     BRANCH _end_1
 *
 *   _var1_smaller_1:
 *     // var1 &lt; var2, so use var1 (the smaller one)
 *     _temp_result = STORE _temp1
 *     BRANCH _end_1
 *
 *   _var1_unset_1:
 *     // var1 is unset - use var2 regardless
 *     _temp_result = STORE _temp2
 *     BRANCH _end_1
 *
 *   _var2_unset_1:
 *     // var2 is unset but var1 is set - use var1
 *     _temp_result = STORE _temp1
 *     BRANCH _end_1
 *
 *   _comparison_invalid_1:
 *     // Comparison failed - create unset result
 *     _temp_result = CALL String()  // unset String
 *     BRANCH _end_1
 *
 *   _end_1:
 *     // Store final result
 *     STORE lesserThan, _temp_result       // lesserThan = "Duck"
 *
 *   IR Generation for greaterThan &lt;- var1 &gt;? var2
 *
 *   BasicBlock: _coalescing_block_2
 *     // Load operands (reuse existing temps or create new ones)
 *     _temp7 = LOAD var1          // _temp7 = "Goose"
 *     _temp8 = LOAD var2          // _temp8 = "Duck"
 *
 *     // Check if var1 is set
 *     _temp9 = CALL _temp7._isSet()    // _temp9 = true
 *     BRANCH_FALSE _temp9, _var1_unset_2   // Skip (var1 is set)
 *
 *     // var1 is set - check if var2 is set
 *     _temp10 = CALL _temp8._isSet()   // _temp10 = true
 *     BRANCH_FALSE _temp10, _var2_unset_2  // Skip (var2 is set)
 *
 *     // Both are set - do comparison var1 > var2 ("Goose" &gt; "Duck")
 *     _temp11 = CALL _temp7._gt(_temp8)    // _temp11 = true (Goose > Duck)
 *     _temp12 = CALL _temp11._isSet()      // _temp12 = true (comparison valid)
 *     BRANCH_FALSE _temp12, _comparison_invalid_2  // Skip (comparison valid)
 *
 *     // Comparison is valid - use ternary logic
 *     BRANCH_TRUE _temp11, _var1_greater_2 // Take this branch (true - var1 is greater)
 *
 *     // var1 &lt;= var2, so use var2 (the greater one for &gt;? operator)
 *     _temp_result2 = STORE _temp8
 *     BRANCH _end_2
 *
 *   _var1_greater_2:
 *     // var1 > var2, so use var1 (the greater one)
 *     _temp_result2 = STORE _temp7         // _temp_result2 = "Goose"
 *     BRANCH _end_2
 *
 *   _var1_unset_2:
 *     // var1 is unset - use var2 regardless
 *     _temp_result2 = STORE _temp8
 *     BRANCH _end_2
 *
 *   _var2_unset_2:
 *     // var2 is unset but var1 is set - use var1
 *     _temp_result2 = STORE _temp7
 *     BRANCH _end_2
 *
 *   _comparison_invalid_2:
 *     // Comparison failed - create unset result
 *     _temp_result2 = CALL String()  // unset String
 *     BRANCH _end_2
 *
 *   _end_2:
 *     // Store final result
 *     STORE greaterThan, _temp_result2     // greaterThan = "Goose"
 * </pre>
 * <p>
 * {@link org.ek9lang.compiler.phase7.IRGeneration} is the main entry point for this phase.
 * </p>
 */

package org.ek9lang.compiler.phase7;