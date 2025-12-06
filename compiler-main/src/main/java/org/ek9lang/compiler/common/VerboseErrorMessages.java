package org.ek9lang.compiler.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides verbose error messages for AI-assisted development.
 * When enabled via the -ve command line flag, these detailed messages
 * are appended to error output to help AI assistants understand and
 * resolve EK9 compilation errors more effectively.
 *
 * <p>Each error code has a verbose message containing:
 * <ul>
 *   <li>COMMON CAUSES - typical reasons for this error</li>
 *   <li>TO FIX - suggested remediation steps</li>
 *   <li>DISTINCTION - clarification from similar error codes</li>
 * </ul>
 */
public final class VerboseErrorMessages {

  private static boolean verboseEnabled = false;
  private static final Map<String, String> VERBOSE_MESSAGES = new HashMap<>();

  static {
    initializeMessages();
  }

  private VerboseErrorMessages() {
  }

  public static void setVerboseEnabled(final boolean enabled) {
    verboseEnabled = enabled;
  }

  public static boolean isVerboseEnabled() {
    return verboseEnabled;
  }

  public static String getVerboseMessage(final String errorCode) {
    return VERBOSE_MESSAGES.get(errorCode);
  }

  @SuppressWarnings("java:S138")
  private static void initializeMessages() {
    // Phase 01: PARSING errors
    VERBOSE_MESSAGES.put("E01010", """
        COMMON CAUSES:
        - Using just the type name without module prefix in references section
        - Missing '::' separator between module and type name
        TO FIX:
        - Use fully qualified format: 'module.name::TypeName'
        DISTINCTION:
        - E01010 is for reference format
        - E03030 is when reference doesn't resolve""");

    VERBOSE_MESSAGES.put("E01020", """
        COMMON CAUSES:
        - Using reserved namespace 'org.ek9.lang' or 'org.ek9.math'
        TO FIX:
        - Choose a different module name (e.g., 'com.mycompany.project')
        NOTE:
        - Only 'org.ek9.lang' and 'org.ek9.math' are reserved""");

    VERBOSE_MESSAGES.put("E01030", """
        COMMON CAUSES:
        - Variable/parameter name conflicts with a function name
        - Property name same as module-level function
        - Method name duplicates existing function
        TO FIX:
        - Rename the conflicting identifier to avoid ambiguity
        DISTINCTION:
        - E01030 is name collision
        - E01040 is duplicate type definition""");

    VERBOSE_MESSAGES.put("E01040", """
        COMMON CAUSES:
        - Defining same class/record/trait twice
        - Type name conflicts with existing type
        TO FIX:
        - Use unique type names within each module
        DISTINCTION:
        - E01040 is type duplication
        - E01030 is name collision with functions""");

    VERBOSE_MESSAGES.put("E01050", """
        COMMON CAUSES:
        - Enumeration values that differ only by case (ACTIVE vs active)
        - Values differing only by underscores (VALUE_1 vs VALUE1)
        TO FIX:
        - Ensure enumeration values are distinct after normalization
        NOTE:
        - EK9 normalizes by uppercasing and removing underscores""");

    // Phase 02: SYMBOL_DEFINITION errors
    VERBOSE_MESSAGES.put("E02010", """
        COMMON CAUSES:
        - Child class declares property with same name as parent
        - Record extends another and redeclares a field
        TO FIX:
        - Use a different property name or remove the duplicate
        DISTINCTION:
        - E02010 is property shadowing
        - E02030 is method duplication""");

    VERBOSE_MESSAGES.put("E02020", """
        COMMON CAUSES:
        - Using $$ (JSON) operator on type with duplicate property names in hierarchy
        TO FIX:
        - Rename duplicate properties to enable JSON serialization
        NOTE:
        - $$ operator requires unique property names across hierarchy""");

    VERBOSE_MESSAGES.put("E02030", """
        COMMON CAUSES:
        - Same method signature defined multiple times
        - Overloaded methods with ambiguous signatures
        TO FIX:
        - Remove duplicate or change signature to be distinct
        DISTINCTION:
        - E02030 is method duplication
        - E06140 is ambiguous method call""");

    VERBOSE_MESSAGES.put("E02040", """
        COMMON CAUSES:
        - Capturing same variable twice in dynamic function/class
        - Variable name collision in capture block
        TO FIX:
        - Capture each variable only once with unique field names""");

    VERBOSE_MESSAGES.put("E02050", """
        COMMON CAUSES:
        - Listing same trait multiple times in 'with' clause
        TO FIX:
        - Reference each trait only once""");

    VERBOSE_MESSAGES.put("E02060", """
        COMMON CAUSES:
        - Same enumeration value appears in multiple case clauses
        TO FIX:
        - Each enumeration value should appear in only one case
        DISTINCTION:
        - E02060 is switch duplication
        - E01050 is enum definition duplication""");

    VERBOSE_MESSAGES.put("E02070", """
        COMMON CAUSES:
        - Two service methods with same HTTP path structure
        - Path patterns that match same URLs
        TO FIX:
        - Use distinct path patterns for each service method""");

    VERBOSE_MESSAGES.put("E02080", """
        COMMON CAUSES:
        - Function delegate field has same name as a method
        TO FIX:
        - Rename either the delegate or the method""");

    // Phase 03: REFERENCE_CHECKS errors
    VERBOSE_MESSAGES.put("E03010", """
        COMMON CAUSES:
        - Local type name conflicts with referenced type
        TO FIX:
        - Rename local type or use different reference""");

    VERBOSE_MESSAGES.put("E03020", """
        COMMON CAUSES:
        - Multiple references bring in same-named types
        TO FIX:
        - Remove conflicting reference or use fully qualified names""");

    VERBOSE_MESSAGES.put("E03030", """
        COMMON CAUSES:
        - Referenced module or type doesn't exist
        - Typo in module path or type name
        TO FIX:
        - Check module path and type name spelling
        DISTINCTION:
        - E03030 is reference resolution
        - E50001 is general identifier resolution""");

    // Phase 04: EXPLICIT_TYPE_SYMBOL_DEFINITION errors
    VERBOSE_MESSAGES.put("E04010", """
        COMMON CAUSES:
        - Attempting to constrain a type that doesn't support constraints
        TO FIX:
        - Only constrain types designed for constraining (like String, Integer)""");

    VERBOSE_MESSAGES.put("E04020", """
        COMMON CAUSES:
        - Interpolation expression result cannot be converted to String
        - Type missing $ operator or String promotion
        TO FIX:
        - Implement $ operator or ensure type can promote to String""");

    VERBOSE_MESSAGES.put("E04030", """
        COMMON CAUSES:
        - Using non-Exception type in throw/catch
        TO FIX:
        - Extend Exception for custom exception types""");

    VERBOSE_MESSAGES.put("E04040", """
        COMMON CAUSES:
        - Passing non-function where function expected
        - Using class/record where delegate required
        TO FIX:
        - Provide a function or function delegate""");

    VERBOSE_MESSAGES.put("E04050", """
        COMMON CAUSES:
        - Using complex aggregate where simple type expected
        TO FIX:
        - Use simple aggregate, list, or dict type""");

    VERBOSE_MESSAGES.put("E04060", """
        COMMON CAUSES:
        - Calling method on non-aggregate type
        - Treating primitive as object
        TO FIX:
        - Ensure type is a class, record, or trait""");

    VERBOSE_MESSAGES.put("E04070", """
        COMMON CAUSES:
        - Using type parameters on non-generic type
        TO FIX:
        - Remove type parameters or use a generic type
        DISTINCTION:
        - E04070 is not a template
        - E04080 is template missing parameters""");

    VERBOSE_MESSAGES.put("E04080", """
        COMMON CAUSES:
        - Using generic type without providing type parameters
        - Missing angle brackets with type arguments
        TO FIX:
        - Provide required type parameters (e.g., List of String)
        DISTINCTION:
        - E04080 is missing parameters
        - E04070 is not a template at all""");

    // Phase 05: TYPE_HIERARCHY_CHECKS errors
    VERBOSE_MESSAGES.put("E05020", """
        COMMON CAUSES:
        - Class A extends B which extends A
        - Interface inheritance loop
        TO FIX:
        - Restructure hierarchy to eliminate cycles""");

    VERBOSE_MESSAGES.put("E05030", """
        COMMON CAUSES:
        - Extending class not marked as 'open'
        - Trying to extend final type
        TO FIX:
        - Mark base class as 'open' or use composition instead""");

    VERBOSE_MESSAGES.put("E05040", """
        COMMON CAUSES:
        - Using super() when implicit Any is the base
        TO FIX:
        - Remove super() call - Any base is implicit""");

    VERBOSE_MESSAGES.put("E05050", """
        COMMON CAUSES:
        - this() or super() not first statement in constructor
        TO FIX:
        - Move this()/super() to be the first statement""");

    VERBOSE_MESSAGES.put("E05060", """
        COMMON CAUSES:
        - Using this() or super() outside constructor
        - Confusing this()/super() with this./super.
        TO FIX:
        - Use 'this.' or 'super.' for member access""");

    VERBOSE_MESSAGES.put("E05070", """
        COMMON CAUSES:
        - Using 'this' in static context
        - 'this' in function (not method)
        TO FIX:
        - 'this' only valid in instance methods/constructors""");

    VERBOSE_MESSAGES.put("E05080", """
        COMMON CAUSES:
        - Using 'super' when no explicit parent
        - 'super' in trait or function
        TO FIX:
        - 'super' only valid in classes with explicit parent""");

    VERBOSE_MESSAGES.put("E05090", """
        COMMON CAUSES:
        - Direct assignment to this/super (this := something)
        TO FIX:
        - Use :=:, :~:, +=, -=, /=, or *= with this/super""");

    VERBOSE_MESSAGES.put("E05100", """
        COMMON CAUSES:
        - Using 'override' on method with no parent method
        TO FIX:
        - Remove 'override' or ensure parent has matching method
        DISTINCTION:
        - E05100 is nothing to override
        - E05110 is missing 'override' keyword""");

    VERBOSE_MESSAGES.put("E05110", """
        COMMON CAUSES:
        - Method signature matches parent but missing 'override'
        TO FIX:
        - Add 'override' keyword to the method
        DISTINCTION:
        - E05110 is missing keyword
        - E05100 is nothing to override""");

    VERBOSE_MESSAGES.put("E05120", """
        COMMON CAUSES:
        - Method name/signature shadows parent method
        TO FIX:
        - Use 'override' if intentional, or rename method""");

    VERBOSE_MESSAGES.put("E05130", """
        COMMON CAUSES:
        - Override changes public to private
        - Access modifier less permissive than parent
        TO FIX:
        - Use same or more permissive access modifier""");

    VERBOSE_MESSAGES.put("E05140", """
        COMMON CAUSES:
        - Extending function with different signature
        TO FIX:
        - Match parent function's parameter and return types""");

    VERBOSE_MESSAGES.put("E05150", """
        COMMON CAUSES:
        - Parent is pure, child is not
        TO FIX:
        - Mark child method/function as 'pure'""");

    VERBOSE_MESSAGES.put("E05160", """
        COMMON CAUSES:
        - Parent is not pure, child is pure
        TO FIX:
        - Remove 'pure' from child or make parent pure""");

    VERBOSE_MESSAGES.put("E05170", """
        COMMON CAUSES:
        - Dispatcher method purity doesn't match target
        TO FIX:
        - Ensure dispatcher and target have same purity""");

    VERBOSE_MESSAGES.put("E05180", """
        COMMON CAUSES:
        - Dispatcher target method is private in parent
        TO FIX:
        - Change access modifier or use different target""");

    VERBOSE_MESSAGES.put("E05190", """
        COMMON CAUSES:
        - Some constructors pure, others not
        TO FIX:
        - Make all constructors pure or none pure""");

    VERBOSE_MESSAGES.put("E05200", """
        COMMON CAUSES:
        - Generic type instantiation with incompatible constructor
        TO FIX:
        - Ensure constructor arguments match generic parameter types""");

    // Phase 06: FULL_RESOLUTION errors
    VERBOSE_MESSAGES.put("E06010", """
        COMMON CAUSES:
        - Using generic type/function without type parameters
        - Missing 'of' clause for parameterized type
        TO FIX:
        - Supply required type parameters (e.g., List of String)
        DISTINCTION:
        - E06010 is missing params
        - E06020 is wrong count""");

    VERBOSE_MESSAGES.put("E06020", """
        COMMON CAUSES:
        - Wrong number of type parameters provided
        - Dict needs two types, only one provided
        TO FIX:
        - Provide correct number of type parameters
        DISTINCTION:
        - E06020 is wrong count
        - E06010 is missing params""");

    VERBOSE_MESSAGES.put("E06030", """
        COMMON CAUSES:
        - Generic type constructor params don't match type inference needs
        TO FIX:
        - Ensure constructor parameter count matches generic parameter count""");

    VERBOSE_MESSAGES.put("E06040", """
        COMMON CAUSES:
        - Generic type missing required constructors
        TO FIX:
        - Add default constructor and inferred-type constructor""");

    VERBOSE_MESSAGES.put("E06050", """
        COMMON CAUSES:
        - Constructor argument types don't match parametric types
        TO FIX:
        - Ensure constructor parameters match generic type parameters in order""");

    VERBOSE_MESSAGES.put("E06060", """
        COMMON CAUSES:
        - Private or protected constructor in generic type
        TO FIX:
        - Make constructors public in generic types""");

    VERBOSE_MESSAGES.put("E06070", """
        COMMON CAUSES:
        - Using type inference (:=) inside generic/template
        TO FIX:
        - Use explicit type declarations in generics""");

    VERBOSE_MESSAGES.put("E06080", """
        COMMON CAUSES:
        - Using function as constraining type for generic
        TO FIX:
        - Use class/record/trait as constraining type""");

    VERBOSE_MESSAGES.put("E06090", """
        COMMON CAUSES:
        - Named dynamic class inside generic type/function
        TO FIX:
        - Use anonymous dynamic class or define outside generic""");

    VERBOSE_MESSAGES.put("E06100", """
        COMMON CAUSES:
        - Generic function declared without implementation
        TO FIX:
        - Provide implementation body for generic function""");

    VERBOSE_MESSAGES.put("E06110", """
        COMMON CAUSES:
        - Constructor in generic uses function as type
        TO FIX:
        - Avoid function types in generic constructors""");

    VERBOSE_MESSAGES.put("E06120", """
        COMMON CAUSES:
        - Using function types in generic beyond '?' check
        TO FIX:
        - Only use '?' (isSet) operator with functions in generics""");

    VERBOSE_MESSAGES.put("E06130", """
        COMMON CAUSES:
        - Constraining type constructor missing from parameterizing type
        TO FIX:
        - Ensure type parameter has all constructors of constraining type""");

    VERBOSE_MESSAGES.put("E06140", """
        COMMON CAUSES:
        - Multiple methods match with equal cost
        - Overloads too similar to distinguish
        TO FIX:
        - Make method signatures more distinct or cast arguments
        DISTINCTION:
        - E06140 is ambiguous
        - E50060 is not found at all""");

    VERBOSE_MESSAGES.put("E06150", """
        COMMON CAUSES:
        - Inherited methods conflict with each other
        TO FIX:
        - Override conflicting method to resolve""");

    VERBOSE_MESSAGES.put("E06160", """
        COMMON CAUSES:
        - Accessing trait method that isn't immediate trait
        TO FIX:
        - Access only immediate trait methods with trait prefix""");

    VERBOSE_MESSAGES.put("E06170", """
        COMMON CAUSES:
        - Trait method access syntax in wrong context
        TO FIX:
        - Use trait method access only in implementing class""");

    VERBOSE_MESSAGES.put("E06180", """
        COMMON CAUSES:
        - Accessing private method from outside class
        - Protected method from non-subclass
        TO FIX:
        - Change access modifier or access from appropriate context""");

    VERBOSE_MESSAGES.put("E06190", """
        COMMON CAUSES:
        - Result type uses same type for both success and error
        TO FIX:
        - Use two different types for Result generic""");

    VERBOSE_MESSAGES.put("E06200", """
        COMMON CAUSES:
        - Unnecessary parentheses in EK9 expression
        TO FIX:
        - Remove parentheses - EK9 uses different syntax""");

    VERBOSE_MESSAGES.put("E06210", """
        COMMON CAUSES:
        - Missing parentheses where required
        TO FIX:
        - Add parentheses for function/method calls""");

    VERBOSE_MESSAGES.put("E06220", """
        COMMON CAUSES:
        - Mixing empty parentheses with values or type
        TO FIX:
        - Use either () with values OR type definition, not both""");

    VERBOSE_MESSAGES.put("E06230", """
        COMMON CAUSES:
        - Captured variable not named when using expression
        TO FIX:
        - Name captured variables: 'captured as fieldName'""");

    VERBOSE_MESSAGES.put("E06240", """
        COMMON CAUSES:
        - Some parameters named, others not
        TO FIX:
        - Either name all parameters or none""");

    VERBOSE_MESSAGES.put("E06250", """
        COMMON CAUSES:
        - Named parameters in wrong order
        - Named parameter doesn't match argument name
        TO FIX:
        - Match parameter names and order exactly""");

    VERBOSE_MESSAGES.put("E06260", """
        COMMON CAUSES:
        - Parameter type doesn't match expected type
        TO FIX:
        - Provide correct parameter type""");

    VERBOSE_MESSAGES.put("E06270", """
        COMMON CAUSES:
        - Function parameter types don't match
        TO FIX:
        - Ensure function signature matches expected delegate""");

    VERBOSE_MESSAGES.put("E06280", """
        COMMON CAUSES:
        - More arguments than parameters
        TO FIX:
        - Remove extra arguments""");

    VERBOSE_MESSAGES.put("E06290", """
        COMMON CAUSES:
        - Fewer arguments than required parameters
        TO FIX:
        - Provide all required arguments""");

    VERBOSE_MESSAGES.put("E06300", """
        COMMON CAUSES:
        - Operation expects single argument, got different count
        TO FIX:
        - Provide exactly one argument""");

    VERBOSE_MESSAGES.put("E06310", """
        COMMON CAUSES:
        - Function/operation expects no arguments
        TO FIX:
        - Remove all arguments from call""");

    VERBOSE_MESSAGES.put("E06320", """
        COMMON CAUSES:
        - Wrong number of parameters in definition
        TO FIX:
        - Match expected parameter count""");

    VERBOSE_MESSAGES.put("E06330", """
        COMMON CAUSES:
        - Type arguments incompatible with each other
        TO FIX:
        - Use compatible types for generic parameters""");

    // Phase 07: POST_RESOLUTION_CHECKS errors
    VERBOSE_MESSAGES.put("E07010", """
        COMMON CAUSES:
        - Private method marked with 'override'
        TO FIX:
        - Remove 'override' - private methods can't override""");

    VERBOSE_MESSAGES.put("E07020", """
        COMMON CAUSES:
        - Method has both 'override' and 'abstract'
        TO FIX:
        - Choose one - override implies implementation""");

    VERBOSE_MESSAGES.put("E07030", """
        COMMON CAUSES:
        - Using 'default' for operator in trait
        TO FIX:
        - Traits don't support default operators""");

    VERBOSE_MESSAGES.put("E07040", """
        COMMON CAUSES:
        - Using 'by' delegation in trait
        TO FIX:
        - 'by' delegation only works in classes""");

    VERBOSE_MESSAGES.put("E07050", """
        COMMON CAUSES:
        - Constructor marked as abstract
        TO FIX:
        - Remove 'abstract' - constructors create instances""");

    VERBOSE_MESSAGES.put("E07060", """
        COMMON CAUSES:
        - Constructor marked with 'override'
        TO FIX:
        - Remove 'override' - constructors don't override""");

    VERBOSE_MESSAGES.put("E07070", """
        COMMON CAUSES:
        - Defining constructor in trait
        TO FIX:
        - Remove constructor - traits can't have constructors""");

    VERBOSE_MESSAGES.put("E07080", """
        COMMON CAUSES:
        - 'default' constructor with parameters
        TO FIX:
        - Default constructor must have no parameters""");

    VERBOSE_MESSAGES.put("E07090", """
        COMMON CAUSES:
        - 'default' modifier on regular method
        TO FIX:
        - 'default' only valid for constructors""");

    VERBOSE_MESSAGES.put("E07100", """
        COMMON CAUSES:
        - Abstract method has implementation body
        TO FIX:
        - Remove body or remove 'abstract'""");

    VERBOSE_MESSAGES.put("E07110", """
        COMMON CAUSES:
        - Method has no body but not marked abstract
        TO FIX:
        - Add implementation or mark as 'abstract'""");

    VERBOSE_MESSAGES.put("E07120", """
        COMMON CAUSES:
        - Dispatcher method missing implementation
        TO FIX:
        - Provide base implementation for dispatcher""");

    VERBOSE_MESSAGES.put("E07130", """
        COMMON CAUSES:
        - Type has abstract methods but not marked abstract
        TO FIX:
        - Mark type as 'abstract' or implement all methods""");

    VERBOSE_MESSAGES.put("E07140", """
        COMMON CAUSES:
        - Dynamic class doesn't implement all abstract methods
        TO FIX:
        - Implement all abstract methods in dynamic class""");

    VERBOSE_MESSAGES.put("E07150", """
        COMMON CAUSES:
        - Text block missing method for language variant
        TO FIX:
        - Implement text method for all required languages""");

    VERBOSE_MESSAGES.put("E07160", """
        COMMON CAUSES:
        - Non-abstract method missing implementation
        TO FIX:
        - Provide implementation body""");

    VERBOSE_MESSAGES.put("E07170", """
        COMMON CAUSES:
        - Uninitialized properties without constructor
        TO FIX:
        - Add constructor to initialize properties""");

    VERBOSE_MESSAGES.put("E07180", """
        COMMON CAUSES:
        - 'default' operator requires operator missing from this type
        - For 'default operator <=>', type needs comparison capability
        - For 'default operator ==', type needs '<=>'' defined
        TO FIX:
        - Implement required operator on this type first
        - Check that '<=>' is defined if using comparison operators
        SEMANTICS NOTE:
        - 'default operator ?' returns true if ANY field is set (not all)
        - 'default operator #?/$/$' return UNSET if no fields are set""");

    VERBOSE_MESSAGES.put("E07190", """
        COMMON CAUSES:
        - 'default' operator requires operator missing from super
        - Parent class needs same operator capability for inheritance to work
        TO FIX:
        - Implement required operator in parent type
        - Or remove inheritance and implement directly
        SEMANTICS NOTE:
        - For '?', if super.? returns true, derived class also returns true""");

    VERBOSE_MESSAGES.put("E07200", """
        COMMON CAUSES:
        - 'default' operator requires operator missing from property type
        - A field's type doesn't support the operator being defaulted
        TO FIX:
        - Ensure property type has required operator
        - For primitives like Integer/String, operators are built-in
        - For custom types, add 'default operator X' or explicit implementation
        SEMANTICS NOTE:
        - Each field type must support the operator for 'default' to work""");

    VERBOSE_MESSAGES.put("E07210", """
        COMMON CAUSES:
        - Function delegate with 'default' operator not supported
        - Fields that are function delegates have limited default support
        TO FIX:
        - Only '?' operator supported with function delegate fields
        - For other operators, implement them manually
        NOTE:
        - Function delegates can only be checked for set/unset status""");

    VERBOSE_MESSAGES.put("E07220", """
        COMMON CAUSES:
        - Using 'default' on unsupported operator
        - Not all operators can be auto-generated
        TO FIX:
        - 'default' works with: ?, ==, <>, <, <=, >, >=, <=>, $, $$, #?, :=:
        - For other operators, implement them explicitly
        SEMANTICS NOTE:
        - 'default operator ?' = true if ANY field set (not all)
        - 'default operator #?/$/$' = UNSET if object completely empty
        - Custom ? does NOT affect default #?/$/$ guards""");

    VERBOSE_MESSAGES.put("E07230", """
        COMMON CAUSES:
        - 'default' operator with explicit signature/body
        - Cannot specify parameters or implementation for default operators
        TO FIX:
        - Remove signature and body from 'default' operator
        - Use just: 'default operator <symbol>'
        - If you need custom behavior, don't use 'default'""");

    VERBOSE_MESSAGES.put("E07240", """
        COMMON CAUSES:
        - Non-web method in service marked protected
        TO FIX:
        - Use public or private for non-web methods""");

    VERBOSE_MESSAGES.put("E07250", """
        COMMON CAUSES:
        - Component method marked protected
        TO FIX:
        - Use public or private for component methods""");

    VERBOSE_MESSAGES.put("E07260", """
        COMMON CAUSES:
        - Protected method in non-open class
        TO FIX:
        - Mark class as 'open' or use different access""");

    VERBOSE_MESSAGES.put("E07270", """
        COMMON CAUSES:
        - Access modifier on trait method
        TO FIX:
        - Remove access modifier - trait methods are public""");

    VERBOSE_MESSAGES.put("E07280", """
        COMMON CAUSES:
        - Unnecessary public access modifier
        TO FIX:
        - Remove 'public' - methods are public by default""");

    VERBOSE_MESSAGES.put("E07290", """
        COMMON CAUSES:
        - Regular method defined on record
        TO FIX:
        - Records only support constructors and operators""");

    VERBOSE_MESSAGES.put("E07300", """
        COMMON CAUSES:
        - Explicit 'uninitialized' where not needed
        TO FIX:
        - Remove declaration - already supports uninitialized""");

    VERBOSE_MESSAGES.put("E07310", """
        COMMON CAUSES:
        - Switch on enum doesn't cover all values
        TO FIX:
        - Add cases for all enum values or add default""");

    VERBOSE_MESSAGES.put("E07320", """
        COMMON CAUSES:
        - Switch statement missing default case
        TO FIX:
        - Add 'default' case to switch statement""");

    VERBOSE_MESSAGES.put("E07330", """
        COMMON CAUSES:
        - Switch expression missing default case
        TO FIX:
        - Add 'default' case to switch expression""");

    VERBOSE_MESSAGES.put("E07340", """
        COMMON CAUSES:
        - Pre-flow control variable not resolved
        TO FIX:
        - Ensure iteration variable is properly typed""");

    VERBOSE_MESSAGES.put("E07350", """
        COMMON CAUSES:
        - Guard (<-) used in expression context
        TO FIX:
        - Guards only valid in control flow statements
        NOTE:
        - Guard may leave variable uninitialized in else branch""");

    VERBOSE_MESSAGES.put("E07360", """
        COMMON CAUSES:
        - Application selection in wrong context
        TO FIX:
        - Use application selection only where supported""");

    VERBOSE_MESSAGES.put("E07370", """
        COMMON CAUSES:
        - Statement after unconditional exception throw
        TO FIX:
        - Remove unreachable statement or restructure logic""");

    VERBOSE_MESSAGES.put("E07380", """
        COMMON CAUSES:
        - Return after path that only throws
        TO FIX:
        - Remove unreachable return or add non-throwing path""");

    VERBOSE_MESSAGES.put("E07390", """
        COMMON CAUSES:
        - Constant Boolean in condition (if true, while false)
        TO FIX:
        - Remove pointless constant condition""");

    VERBOSE_MESSAGES.put("E07400", """
        COMMON CAUSES:
        - Function/method missing return declaration
        TO FIX:
        - Add returning variable and type""");

    VERBOSE_MESSAGES.put("E07405", """
        COMMON CAUSES:
        - Assignment needs return value but none declared
        TO FIX:
        - Add returning block for expression context""");

    VERBOSE_MESSAGES.put("E07406", """
        COMMON CAUSES:
        - Returning block where no assignment uses it
        TO FIX:
        - Remove returning block or use in assignment""");

    VERBOSE_MESSAGES.put("E07410", """
        COMMON CAUSES:
        - Constructor returns different type than class
        TO FIX:
        - Return type must match enclosing type""");

    VERBOSE_MESSAGES.put("E07420", """
        COMMON CAUSES:
        - Promotion returns same type
        TO FIX:
        - Promotion must return different type""");

    VERBOSE_MESSAGES.put("E07430", """
        COMMON CAUSES:
        - Return value in void context
        TO FIX:
        - Remove return value or change to expression""");

    VERBOSE_MESSAGES.put("E07440", """
        COMMON CAUSES:
        - Override returns incompatible type
        TO FIX:
        - Return same type or covariant subtype""");

    VERBOSE_MESSAGES.put("E07450", """
        COMMON CAUSES:
        - Function has parameters but shouldn't
        TO FIX:
        - Remove parameters from function""");

    VERBOSE_MESSAGES.put("E07460", """
        COMMON CAUSES:
        - Function needs exactly one parameter
        TO FIX:
        - Provide exactly one parameter""");

    VERBOSE_MESSAGES.put("E07470", """
        COMMON CAUSES:
        - Function needs exactly two parameters
        TO FIX:
        - Provide exactly two parameters""");

    VERBOSE_MESSAGES.put("E07480", """
        COMMON CAUSES:
        - Expected function delegate, got something else
        TO FIX:
        - Provide function delegate type""");

    VERBOSE_MESSAGES.put("E07490", """
        COMMON CAUSES:
        - Function expected to return value but doesn't
        TO FIX:
        - Add return value to function""");

    VERBOSE_MESSAGES.put("E07500", """
        COMMON CAUSES:
        - Operator should be pure but isn't
        TO FIX:
        - Mark operator as 'pure'""");

    VERBOSE_MESSAGES.put("E07510", """
        COMMON CAUSES:
        - Operator marked pure but shouldn't be
        TO FIX:
        - Remove 'pure' from operator""");

    VERBOSE_MESSAGES.put("E07520", """
        COMMON CAUSES:
        - Function/method must return Boolean but doesn't
        TO FIX:
        - Return Boolean type""");

    VERBOSE_MESSAGES.put("E07530", """
        COMMON CAUSES:
        - Expression type not compatible with Boolean
        TO FIX:
        - Use Boolean-compatible type""");

    VERBOSE_MESSAGES.put("E07540", """
        COMMON CAUSES:
        - Non-Boolean in Boolean context
        TO FIX:
        - Convert to Boolean or use Boolean type""");

    VERBOSE_MESSAGES.put("E07550", """
        COMMON CAUSES:
        - Function must return Integer but doesn't
        TO FIX:
        - Return Integer type""");

    VERBOSE_MESSAGES.put("E07560", """
        COMMON CAUSES:
        - Integer value must be > 0
        TO FIX:
        - Use positive Integer value""");

    VERBOSE_MESSAGES.put("E07570", """
        COMMON CAUSES:
        - Function must return String but doesn't
        TO FIX:
        - Return String type""");

    VERBOSE_MESSAGES.put("E07580", """
        COMMON CAUSES:
        - Function must return JSON but doesn't
        TO FIX:
        - Return JSON type""");

    VERBOSE_MESSAGES.put("E07590", """
        COMMON CAUSES:
        - Program returns non-Integer
        TO FIX:
        - Program exit code must be Integer""");

    VERBOSE_MESSAGES.put("E07600", """
        COMMON CAUSES:
        - Program argument type not supported
        TO FIX:
        - Use supported built-in types for program args""");

    VERBOSE_MESSAGES.put("E07610", """
        COMMON CAUSES:
        - Incompatible program argument combination
        TO FIX:
        - Use compatible argument types""");

    VERBOSE_MESSAGES.put("E07620", """
        COMMON CAUSES:
        - Using operator not defined for type
        - Operator exists but not for this argument type combination
        - Type mismatch between left and right operands
        TO FIX:
        - Define operator or use different type
        - Check types on both sides of operator match expectations
        - Ensure operand types support the operator
        DISTINCTION:
        - E07620 is operator not defined
        - E50060 is method not resolved""");

    VERBOSE_MESSAGES.put("E07630", """
        COMMON CAUSES:
        - Operator not valid for enumeration use
        TO FIX:
        - Use supported enumeration operators""");

    VERBOSE_MESSAGES.put("E07640", """
        COMMON CAUSES:
        - Using != instead of <>
        TO FIX:
        - Use '<>' for not-equal in EK9""");

    VERBOSE_MESSAGES.put("E07650", """
        COMMON CAUSES:
        - Using ! instead of ~
        TO FIX:
        - Use '~' for logical not in EK9""");

    VERBOSE_MESSAGES.put("E07660", """
        COMMON CAUSES:
        - Using operator name as method name
        TO FIX:
        - Use 'operator' keyword for operators""");

    VERBOSE_MESSAGES.put("E07670", """
        COMMON CAUSES:
        - Unsupported operator in service
        TO FIX:
        - Use +, +=, -, -=, :^:, :~:, or ? only""");

    VERBOSE_MESSAGES.put("E07680", """
        COMMON CAUSES:
        - URI path variable in unsupported context
        TO FIX:
        - Use URI variables only where supported""");

    VERBOSE_MESSAGES.put("E07690", """
        COMMON CAUSES:
        - HTTP verb not supported in context
        TO FIX:
        - Use supported HTTP verbs""");

    VERBOSE_MESSAGES.put("E07700", """
        COMMON CAUSES:
        - Invalid HTTP path parameter format
        TO FIX:
        - Use valid path parameter syntax""");

    VERBOSE_MESSAGES.put("E07710", """
        COMMON CAUSES:
        - QUERY/HEADER/PATH parameter needs name
        TO FIX:
        - Provide parameter name qualifier""");

    VERBOSE_MESSAGES.put("E07720", """
        COMMON CAUSES:
        - Qualifier not allowed for this parameter type
        TO FIX:
        - Remove parameter name qualifier""");

    VERBOSE_MESSAGES.put("E07730", """
        COMMON CAUSES:
        - Path variables don't match parameter count
        TO FIX:
        - Match path variable count with parameters""");

    VERBOSE_MESSAGES.put("E07740", """
        COMMON CAUSES:
        - Service method without body (services can't be abstract)
        TO FIX:
        - Provide implementation for service method""");

    VERBOSE_MESSAGES.put("E07750", """
        COMMON CAUSES:
        - Service return type not HTTPResponse compatible
        TO FIX:
        - Return HTTPResponse or compatible type""");

    VERBOSE_MESSAGES.put("E07760", """
        COMMON CAUSES:
        - Service parameter type not supported
        TO FIX:
        - Use Integer, String, Date, Time, DateTime, Milliseconds, Duration, or HTTPRequest""");

    VERBOSE_MESSAGES.put("E07770", """
        COMMON CAUSES:
        - HTTPRequest parameter type for non-REQUEST
        TO FIX:
        - Use HTTPRequest only for REQUEST access""");

    VERBOSE_MESSAGES.put("E07780", """
        COMMON CAUSES:
        - HTTPRequest with other parameters
        TO FIX:
        - HTTPRequest must be only parameter""");

    VERBOSE_MESSAGES.put("E07790", """
        COMMON CAUSES:
        - HTTPRequest used for non-REQUEST access
        TO FIX:
        - Don't use HTTPRequest for other access types""");

    VERBOSE_MESSAGES.put("E07800", """
        COMMON CAUSES:
        - Service method missing return
        TO FIX:
        - Return HTTPResponse-compatible value""");

    VERBOSE_MESSAGES.put("E07810", """
        COMMON CAUSES:
        - Dispatch in non-class context
        TO FIX:
        - Dispatch only supported in classes""");

    VERBOSE_MESSAGES.put("E07820", """
        COMMON CAUSES:
        - Multiple methods marked as dispatcher entry
        TO FIX:
        - Only one method can be dispatcher entry""");

    VERBOSE_MESSAGES.put("E07830", """
        COMMON CAUSES:
        - Type missing pipe operator for stream
        TO FIX:
        - Implement '|' operator for type""");

    VERBOSE_MESSAGES.put("E07840", """
        COMMON CAUSES:
        - Type missing iterator methods
        TO FIX:
        - Implement iterator(), hasNext(), next() methods""");

    VERBOSE_MESSAGES.put("E07850", """
        COMMON CAUSES:
        - Multiple exceptions in single catch
        TO FIX:
        - Use single exception type per catch""");

    VERBOSE_MESSAGES.put("E07860", """
        COMMON CAUSES:
        - Function or delegate required but not provided
        TO FIX:
        - Provide function or function delegate""");

    VERBOSE_MESSAGES.put("E07870", """
        COMMON CAUSES:
        - Integer or function/delegate required
        TO FIX:
        - Provide Integer value or function/delegate""");

    VERBOSE_MESSAGES.put("E07880", """
        COMMON CAUSES:
        - Function/delegate provided where not needed
        TO FIX:
        - Remove function/delegate""");

    VERBOSE_MESSAGES.put("E07890", """
        COMMON CAUSES:
        - Attempting to mutate immutable value
        TO FIX:
        - Use mutable variable or different approach""");

    VERBOSE_MESSAGES.put("E07900", """
        COMMON CAUSES:
        - Invalid value for context
        TO FIX:
        - Provide valid value""");

    // Phase 08: PRE_IR_CHECKS errors
    VERBOSE_MESSAGES.put("E08010", """
        COMMON CAUSES:
        - Using identifier before it's declared
        TO FIX:
        - Declare variable before use
        DISTINCTION:
        - E08010 is used before declared
        - E08020 is declared but uninitialized""");

    VERBOSE_MESSAGES.put("E08020", """
        COMMON CAUSES:
        - Variable declared but possibly not initialized before use
        - Conditional initialization not covering all paths
        TO FIX:
        - Initialize variable on all code paths
        DISTINCTION:
        - E08020 is uninitialized
        - E08010 is undeclared""");

    VERBOSE_MESSAGES.put("E08030", """
        COMMON CAUSES:
        - Accessing method/property without checking if set
        TO FIX:
        - Use '?' check or guard before access""");

    VERBOSE_MESSAGES.put("E08040", """
        COMMON CAUSES:
        - Reassigning variable in safe access scope
        TO FIX:
        - Don't reassign within '?' safe access block""");

    VERBOSE_MESSAGES.put("E08050", """
        COMMON CAUSES:
        - Return variable not initialized on all paths
        TO FIX:
        - Initialize return on all code paths""");

    VERBOSE_MESSAGES.put("E08060", """
        COMMON CAUSES:
        - Variable possibly not initialized
        TO FIX:
        - Ensure initialization before use""");

    VERBOSE_MESSAGES.put("E08070", """
        COMMON CAUSES:
        - Variable declared but never assigned
        TO FIX:
        - Initialize variable or remove if unused""");

    VERBOSE_MESSAGES.put("E08080", """
        COMMON CAUSES:
        - Assigning variable to itself
        TO FIX:
        - Remove self-assignment""");

    VERBOSE_MESSAGES.put("E08090", """
        COMMON CAUSES:
        - Variable never used after declaration/assignment
        TO FIX:
        - Use the variable or remove it""");

    VERBOSE_MESSAGES.put("E08100", """
        COMMON CAUSES:
        - Reassigning in pure context with :=
        TO FIX:
        - Use ':=?' for conditional assignment in pure scope""");

    VERBOSE_MESSAGES.put("E08110", """
        COMMON CAUSES:
        - Reassigning function/method parameter
        TO FIX:
        - Create local copy to modify""");

    VERBOSE_MESSAGES.put("E08120", """
        COMMON CAUSES:
        - Mutating operation in pure context
        TO FIX:
        - Avoid mutation or remove 'pure'""");

    VERBOSE_MESSAGES.put("E08130", """
        COMMON CAUSES:
        - Calling non-pure function from pure context
        TO FIX:
        - Only call pure functions from pure scope""");

    VERBOSE_MESSAGES.put("E08140", """
        COMMON CAUSES:
        - Component injection in pure context
        TO FIX:
        - Don't inject components in pure methods""");

    VERBOSE_MESSAGES.put("E08150", """
        COMMON CAUSES:
        - Injecting concrete component instead of abstract
        TO FIX:
        - Use abstract base component for injection""");

    VERBOSE_MESSAGES.put("E08160", """
        COMMON CAUSES:
        - Injection not possible in this context
        TO FIX:
        - Use constructor injection or restructure""");

    VERBOSE_MESSAGES.put("E08170", """
        COMMON CAUSES:
        - Direct reassignment of injected component
        TO FIX:
        - Use ':=?' for conditional reassignment""");

    VERBOSE_MESSAGES.put("E08180", """
        COMMON CAUSES:
        - Property not initialized and not marked for injection
        TO FIX:
        - Initialize property or mark for injection""");

    // Phase 09-10 errors
    VERBOSE_MESSAGES.put("E09010", """
        COMMON CAUSES:
        - Using construct in wrong context
        TO FIX:
        - Use appropriate construct for context""");

    VERBOSE_MESSAGES.put("E10010", """
        COMMON CAUSES:
        - Using void return type with assignment
        TO FIX:
        - Void functions can't be used in assignments""");

    VERBOSE_MESSAGES.put("E10020", """
        COMMON CAUSES:
        - Void type in stream pipeline
        TO FIX:
        - Use typed return for stream operations""");

    VERBOSE_MESSAGES.put("E10030", """
        COMMON CAUSES:
        - Constructing abstract type directly
        TO FIX:
        - Use concrete subtype constructor""");

    // Phase 11 errors
    VERBOSE_MESSAGES.put("E11010", """
        COMMON CAUSES:
        - Method/function too complex (high cyclomatic complexity)
        TO FIX:
        - Extract helper methods, simplify conditionals""");

    VERBOSE_MESSAGES.put("E11011", """
        COMMON CAUSES:
        - Too many nested control structures
        TO FIX:
        - Extract nested blocks into separate functions""");

    // Common errors (E50xxx)
    VERBOSE_MESSAGES.put("E50001", """
        COMMON CAUSES:
        - Typo in identifier name
        - Variable not declared in scope
        - Missing import/reference statement
        - Using before definition
        TO FIX:
        - Check spelling, add reference, or declare variable
        DISTINCTION:
        - E50001 is for identifiers/variables
        - E50010 is for types
        - E50060 is for methods""");

    VERBOSE_MESSAGES.put("E50010", """
        COMMON CAUSES:
        - Type name misspelled
        - Missing generic type parameter
        - Type in different module without reference
        - Cascading from earlier resolution failure (fix earlier error first)
        TO FIX:
        - Check spelling, add 'references module::TypeName'
        - If multiple E50010 errors, fix the earliest one first
        DISTINCTION:
        - E50010 is for types
        - E50001 is for variables""");

    VERBOSE_MESSAGES.put("E50020", """
        COMMON CAUSES:
        - Using incompatible genus (class vs trait vs record)
        TO FIX:
        - Use compatible construct types""");

    VERBOSE_MESSAGES.put("E50030", """
        COMMON CAUSES:
        - Assigning incompatible types
        - Return type doesn't match declaration
        - Coalescing operators (??, ?:) with different types result in 'Any'
        TO FIX:
        - Ensure types are compatible or add conversion
        - For coalescing, ensure both branches return same type
        NOTE:
        - Coalescing dissimilar types yields 'Any', which may cause downstream errors""");

    VERBOSE_MESSAGES.put("E50040", """
        COMMON CAUSES:
        - Using 'abstract' where not supported
        TO FIX:
        - Remove 'abstract' from this construct""");

    VERBOSE_MESSAGES.put("E50050", """
        COMMON CAUSES:
        - Variable name already declared in scope
        TO FIX:
        - Use unique variable name""");

    VERBOSE_MESSAGES.put("E50060", """
        COMMON CAUSES:
        - Method name typo
        - Wrong number of parameters
        - Wrong parameter types (no matching overload)
        - Calling private method from outside class
        - In method chain, previous method returns different type than expected
        TO FIX:
        - Check method name, parameter count and types
        - For chains, verify each method's return type matches next call's receiver
        DISTINCTION:
        - E50060 is for method/function calls
        - E50001 is for identifier lookup
        - E07620 is for operators""");

    VERBOSE_MESSAGES.put("E50070", """
        COMMON CAUSES:
        - Using abstract function improperly
        TO FIX:
        - Use concrete function or proper abstract pattern""");

    VERBOSE_MESSAGES.put("E50080", """
        COMMON CAUSES:
        - Calling abstract function/type directly
        TO FIX:
        - Call concrete implementation instead""");

    VERBOSE_MESSAGES.put("E50090", """
        COMMON CAUSES:
        - Parameter genus doesn't match expected
        TO FIX:
        - Use compatible parameter type genus""");

    VERBOSE_MESSAGES.put("E50100", """
        COMMON CAUSES:
        - Type category mismatch
        TO FIX:
        - Use correct type category""");

    // Directive errors (E50200-E50310)
    VERBOSE_MESSAGES.put("E50200", """
        COMMON CAUSES:
        - Invalid @directive syntax
        TO FIX:
        - Use valid directive format: @Directive: PHASE: CLASSIFICATION""");

    VERBOSE_MESSAGES.put("E50210", """
        COMMON CAUSES:
        - Expected error directive but none found
        TO FIX:
        - Add @Error directive before expected error line""");

    VERBOSE_MESSAGES.put("E50220", """
        COMMON CAUSES:
        - Directive classification doesn't match actual error
        TO FIX:
        - Use correct classification in directive""");

    VERBOSE_MESSAGES.put("E50230", """
        COMMON CAUSES:
        - Directive expects error but none occurred
        TO FIX:
        - Remove directive or fix code to produce expected error""");

    VERBOSE_MESSAGES.put("E50240", """
        COMMON CAUSES:
        - @Complexity directive value mismatch
        TO FIX:
        - Update directive with correct complexity value""");

    VERBOSE_MESSAGES.put("E50250", """
        COMMON CAUSES:
        - @Resolved directive symbol not found
        TO FIX:
        - Check symbol name in directive""");

    VERBOSE_MESSAGES.put("E50260", """
        COMMON CAUSES:
        - Type hierarchy check failed in directive
        TO FIX:
        - Verify hierarchy is as expected""");

    VERBOSE_MESSAGES.put("E50270", """
        COMMON CAUSES:
        - Symbol category doesn't match directive
        TO FIX:
        - Use correct category in directive""");

    VERBOSE_MESSAGES.put("E50280", """
        COMMON CAUSES:
        - Symbol genus doesn't match directive
        TO FIX:
        - Use correct genus in directive""");

    VERBOSE_MESSAGES.put("E50290", """
        COMMON CAUSES:
        - Directive references non-existent genus
        TO FIX:
        - Use valid genus name""");

    VERBOSE_MESSAGES.put("E50300", """
        COMMON CAUSES:
        - Unexpected symbol resolved at location
        TO FIX:
        - Check directive expects correct symbol""");

    VERBOSE_MESSAGES.put("E50310", """
        COMMON CAUSES:
        - Error count doesn't match directive expectation
        TO FIX:
        - Verify expected error count""");
  }
}
