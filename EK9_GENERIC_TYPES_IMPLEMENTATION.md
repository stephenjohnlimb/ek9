# EK9 Generic Types Implementation Guide

This document provides comprehensive guidance for implementing parameterized generic types in the EK9 language system. It covers the complete process from decorated name generation through testing and validation.

**Related Documentation:**
- **`CLAUDE.md`** - Main project overview and daily development guidelines  
- **`EK9_DEVELOPMENT_CONTEXT.md`** - General EK9 built-in type development patterns
- **`EK9_SESSION_NOTES.md`** - Historical implementation sessions and lessons learned

## EK9 Generic Type Decorated Name Generation

To generate decorated names for parameterized generic types (used internally by the compiler):
- `java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName <primaryName> <genericFQN> <param1FQN> [param2FQN...]`

**Examples:**
- List of String: `java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName List org.ek9.lang::List org.ek9.lang::String`
  - Result: `_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1`
- Iterator of String: `java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName Iterator org.ek9.lang::Iterator org.ek9.lang::String`
  - Result: `_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2`
- Dict of (String, String): `java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName Dict org.ek9.lang::Dict org.ek9.lang::String org.ek9.lang::String`
  - Result: `_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F`
- Dict of (String, Integer): `java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName Dict org.ek9.lang::Dict org.ek9.lang::String org.ek9.lang::Integer`
  - Result: `_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6`

**Pattern:** `_<primaryName>_<SHA256-hash-of-fully-qualified-names>`

## EK9 Generic Type Parameterization Pattern

EK9 implements generic types using a **Delegation Pattern with Type Parameterization**. When EK9 needs a concrete parameterized type like `Iterator<Character>`, it generates a type-safe wrapper that delegates to the generic base.

### **Core Pattern Structure**

1. **Decorated Name Generation**: Use DecoratedName utility to create unique class names
2. **Delegation to Generic Base**: Parameterized type contains `private final <Generic> delegate`
3. **Type-Safe Wrapper**: All methods delegate to base with appropriate type casting
4. **Annotation Preservation**: **All annotated methods from generic base must be replicated with type substitution**

### **Implementation Requirements**

**CRITICAL**: The parameterized type must implement **every annotated method** from the generic base:
- **Same method signature** but with type parameters substituted (e.g., `T` → `Character`)
- **All implementations delegate** to the base class with casting where needed
- **Maintains EK9 annotation consistency** across parameterizations

### **Example: Iterator<Character> Implementation**

```java
@Ek9ParameterisedType("Iterator of Character")
public class _Iterator_<hash> extends BuiltinType {
    private final Iterator delegate;  // Delegation to generic base
    
    // Constructor delegates
    public _Iterator_<hash>() {
        delegate = new Iterator();
    }
    
    public _Iterator_<hash>(Character arg0) {
        delegate = new Iterator(arg0);  // Accepts Character, passes as Any
    }
    
    // All annotated methods from base Iterator with type substitution:
    
    // Base: hasNext() <- rtn as Boolean?
    public Boolean hasNext() {
        return delegate.hasNext();  // Direct delegation
    }
    
    // Base: next() <- rtn as T?  →  Parameterized: next() <- rtn as Character?
    public Character next() {
        return (Character) delegate.next();  // Delegate + cast
    }
    
    // Base: operator ? <- rtn as Boolean?
    @Override
    public Boolean _isSet() {
        return delegate.hasNext();  // Delegate to base logic
    }
    
    // Base: operator == -> arg as Iterator of T <- rtn as Boolean?
    // Parameterized: -> arg as Iterator of Character
    public Boolean _eq(_Iterator_<hash> arg) {
        return delegate._eq(arg.delegate);  // Delegate comparison
    }
    
    // Base: operator #? <- rtn as Integer?
    @Override
    public Integer _hashcode() {
        return delegate._hashcode();  // Direct delegation
    }
}
```

### **Implementation Steps**

1. **Generate Decorated Name**: Use DecoratedName utility for unique class name
2. **Create Wrapper Class**: Extend BuiltinType with `@Ek9ParameterisedType` annotation
3. **Add Delegate Field**: `private final <GenericType> delegate`
4. **Implement All Constructors**: Delegate to base constructors with type handling
5. **Replicate All Annotated Methods**: 
   - Copy method signatures from generic base
   - Substitute type parameters (T → ConcreteType)
   - Delegate all implementations to base
   - Add casting for return types where needed
6. **Type-Specific Methods**: Add casting for methods returning parameterized types

### **Benefits**
- **Code Reuse**: Generic logic written once in base class
- **Type Safety**: Each parameterization is type-safe at EK9 level
- **Performance**: Minimal overhead (delegation + casting)
- **EK9 Consistency**: All annotations preserved with correct types
- **Java Interop**: Works seamlessly with Java's type system

### **Usage Pattern for Other Generic Types**
This pattern applies to all EK9 generics:
- `List<String>` → `_List_<hash>` with String-typed methods
- `Dict<String,Integer>` → `_Dict_<hash>` with appropriate key/value typing
- `Optional<Boolean>` → `_Optional_<hash>` with Boolean-typed methods

## EK9 Generic Type Parameterization Implementation Guide

### **Complete Implementation Pattern - All Components Required**

Based on successful implementations of Iterator of String, Optional of String, Acceptor of String, Consumer of String, and List of String, this is the definitive guide for implementing EK9 parameterized generic types.

#### **1. Decorated Name Generation**
```bash
java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName <PrimaryName> <GenericFQN> <Param1FQN> [Param2FQN...]
```

**Examples:**
- List of String: `_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1`
- Iterator of String: `_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2`
- Optional of String: `_Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC`
- Acceptor of String: `_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA`
- Consumer of String: `_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0`

#### **2. Class Structure Template**
```java
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("<GenericType> of <ConcreteType>")  // or @Ek9Function for functions
public class _<DecoratedName> extends BuiltinType {
  
  private final <GenericType> delegate;

  // Constructors - delegate to base
  public _<DecoratedName>() {
    this.delegate = new <GenericType>();
  }

  public _<DecoratedName>(<ConcreteType> arg0) {
    this.delegate = new <GenericType>(arg0);
  }

  // Internal constructor for factory methods
  private _<DecoratedName>(<GenericType> delegate) {
    this.delegate = delegate;
  }

  // Factory methods
  public static _<DecoratedName> _of() { ... }
  public static _<DecoratedName> _of(<GenericType>) { ... }
  // Additional _of() overloads as needed
}
```

#### **3. Method Implementation Requirements**
**CRITICAL**: Every annotated method from the base generic type MUST be implemented:

```java
// Base method: someMethod() <- rtn as T?
public <ConcreteType> someMethod() {
  return (<ConcreteType>) delegate.someMethod();  // Delegate + cast
}

// Base method: someMethod() -> arg as List of T <- rtn as Boolean?  
public Boolean someMethod(_<DecoratedListName> arg) {
  if (arg != null) {
    return delegate.someMethod(arg.delegate);  // Delegate unwrapping
  }
  return new Boolean();
}

// Base operator: operator + -> arg as T <- rtn as List of T?
public _<DecoratedListName> _add(<ConcreteType> arg) {
  return new _<DecoratedListName>(delegate._add(arg));  // Delegate + wrap
}
```

#### **4. Integration Patterns**

**Iterator Integration (Collections):**
```java
// List of String iterator() method
public _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterator() {
  return _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(delegate.iterator());
}
```

**Function Integration (Optional):**
```java
// Optional of String whenPresent() method
public void whenPresent(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA acceptor) {
  if (acceptor != null && delegate._isSet().state) {
    acceptor._call(get()); // Call parameterized function with concrete type
  }
}
```

#### **5. Complete Test Coverage Pattern**
```java
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _<DecoratedName>Test extends Common {

  @Test
  void testConstruction() {
    // Test all constructor patterns
  }

  @Test  
  void testFactoryMethods() {
    // Test all _of() variants with null handling
  }

  @Test
  void testTypeSpecificMethods() {
    // Test methods returning concrete types (not Any)
  }

  @Test
  void testIteratorIntegration() {
    // Test iterator() returns correct parameterized type
  }

  @Test
  void testAllOperators() {
    // Test ALL operators from base type with type substitution
  }

  @Test
  void testDelegationBehavior() {
    // Verify consistency with base type behavior
  }

  @Test
  void testEdgeCasesAndNullHandling() {
    // Comprehensive null and edge case testing
  }
}
```

#### **6. Implementation Validation Process**
1. **Unit Test Validation**: `mvn test -pl ek9-lang` - All parameterized type tests pass
2. **EK9 Annotation Validation**: 
   - `mvn clean install -pl ek9-lang`
   - `mvn clean compile -pl compiler-main` 
   - `mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main`
3. **Integration Validation**: Full test suite passes (`mvn test`)

#### **7. Key Implementation Lessons**

**Type Safety Requirements:**
- ALL methods returning `T` must cast to concrete type: `(ConcreteType) delegate.method()`
- ALL methods accepting parameterized types must unwrap: `arg.delegate`
- ALL methods returning parameterized types must wrap: `new _DecoratedName(result)`

**Null Handling Patterns:**
- Factory methods: `_of(null)` returns valid empty/default instance
- Parameter validation: Check `if (arg != null)` before delegation
- EK9 semantics: Invalid operations return unset values, not exceptions

**Integration Dependencies:**
- Collections need parameterized Iterator: `List<String>` needs `Iterator<String>`
- Functions need concrete types: `Optional<String>` needs `Acceptor<String>`, `Consumer<String>`
- Always implement dependent types first

**Complete API Coverage:**
- Count ALL `@Ek9Method`, `@Ek9Operator`, `@Ek9Constructor` annotations in base type
- Implement EVERY single one with proper type substitution
- Example: List has 27 annotated methods - ALL must be implemented

This comprehensive pattern ensures consistent, type-safe, and fully-functional parameterized generic types that integrate seamlessly with the EK9 type system.

## EK9 Iterator and String Integration Analysis

### **String Character Iteration Pattern**
The String class demonstrates sophisticated integration with parameterized generic Iterator types, enabling natural iteration through characters:

**String.iterator() Implementation:**
```java
@Ek9Method("""
    iterator() as pure
      <- rtn as Iterator of Character?""")
public _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 iterator() {
  if (isSet) {
    // Multi-stage type conversion for EK9 semantics:
    final var iterator = Iterator._of(
      this.state.chars()                    // Java IntStream of chars
        .mapToObj(Character::_of)           // Stream<Character> (EK9 Characters)  
        .map(Any.class::cast)              // Stream<Any> (required for generic Iterator)
        .iterator()                        // java.util.Iterator<Any>
    );
    return _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of(iterator);
  }
  return _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of();
}
```

### **Enhanced Parameterized Iterator Implementation**
Recent improvements to the Iterator of Character implementation include:

**1. Internal Constructor Pattern:**
```java
// Public constructors delegate to internal constructor
public _Iterator_...(Character arg0) {
  this(new Iterator(arg0));
}

// Internal constructor for factory methods
private _Iterator_...(Iterator delegate) {
  this.delegate = delegate;
}
```

**2. Comprehensive Factory Methods:**
```java
public static _Iterator_... _of() {
  return new _Iterator_...();
}

public static _Iterator_... _of(Iterator iterator) {
  if (iterator != null) {
    return new _Iterator_...(iterator);
  }
  return new _Iterator_...();
}
```

**3. Perfect Type Safety with Delegation:**
```java
// All annotated methods from base Iterator are replicated with type substitution
public Boolean hasNext() {
  return delegate.hasNext();  // Direct delegation
}

public Character next() {
  return (Character) delegate.next();  // Delegate + safe cast
}

public Boolean _eq(_Iterator_... arg) {
  return delegate._eq(arg.delegate);  // Delegate comparison
}
```

### **Integration Testing Patterns**
The String and Iterator tests demonstrate comprehensive validation:

**String Character Iteration Test:**
```java
@Test
void testIterator() {
  final var underTest = String._of("Steve");
  final var iterator = underTest.iterator();
  assertSet.accept(iterator);
  
  final var expect = new char[] {'S', 't', 'e', 'v', 'e'};
  for (final char c : expect) {
    assertTrue.accept(iterator.hasNext());
    final var ch = iterator.next();
    assertSet.accept(ch);
    assertEquals(Character._of(c), ch);
  }
  assertFalse.accept(iterator.hasNext());
}
```

**Factory Method Validation:**
```java
@Test
void testFactoryMethods() {
  // Unset factory
  final var unset1 = _Iterator_..._of();
  assertUnset.accept(unset1);
  
  // Null safety
  final var unset2 = _Iterator_..._of(null);
  assertUnset.accept(unset2);
  
  // Valid Iterator wrapping
  final var set1 = _Iterator_..._of(Iterator._of(Character._of("S")));
  assertSet.accept(set1);
  assertEquals(Character._of('S'), set1.next());
}
```

### **Key Architectural Benefits**

1. **Seamless Type Conversion**: Java chars → EK9 Characters → Any → Iterator → Parameterized Iterator
2. **EK9 Semantic Preservation**: All set/unset behavior and operators work correctly
3. **Type Safety**: Compile-time guarantee that iterator returns Character objects
4. **Performance**: Minimal overhead through delegation pattern
5. **Java Interoperability**: Uses Java streams and collections efficiently
6. **Extensible Pattern**: Same approach works for all parameterized generics

### **Iterator Design Patterns Summary**

The Iterator implementation showcases three key EK9 design patterns:
- **Delegation Pattern**: Parameterized types wrap generic base implementations
- **Factory Method Pattern**: Static `_of()` methods with proper null handling
- **Type Bridge Pattern**: Safe conversion between Java and EK9 type systems

This enables natural EK9 syntax like iterating through String characters while maintaining full type safety and EK9 semantic consistency.

## Critical Parameterized Types Lessons (2025-01-21)

### **Challenge: Dict of (String, Integer) Complex Implementation**

This section covers the complete implementation of a two-parameter generic Dict of (String, Integer) with 4 interdependent parameterized types, plus debugging runtime ClassCastException issues and accommodating base class API changes.

#### **CRITICAL LESSON: Always Do Detailed Type Analysis First**

**❌ Initial Error Made:**
When implementing `Dict of (String, Integer).keys()` method, the wrong Iterator type was used:
- `_Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408` (**Iterator of Character**)
- Instead of: `_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2` (**Iterator of String**)

**🔥 Result:** `ClassCastException: String cannot be cast to Character` at runtime

**✅ Root Cause Analysis Process:**
1. **Always generate and verify decorated names** for each parameterized type needed
2. **Map type parameters to concrete types**: Dict of (String, Integer) → K=String, V=Integer
3. **Check if parameterized types exist**: Use `java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName`
4. **Verify type consistency across all methods**: keys() → Iterator of K → Iterator of String

#### **Decorated Name Generation Patterns**
```bash
# Generate names for all required parameterized types
java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName Iterator org.ek9.lang::Iterator org.ek9.lang::String
# → _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2

java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName Iterator org.ek9.lang::Iterator org.ek9.lang::Character  
# → _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408

java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName Dict org.ek9.lang::Dict org.ek9.lang::String org.ek9.lang::Integer
# → _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6
```

#### **Critical Pre-Implementation Checklist**

**BEFORE implementing ANY parameterized type:**

1. **📋 Type Parameter Mapping**
   ```
   Base Type: Dict of (K, V)
   Concrete Type: Dict of (String, Integer)
   
   Map all type parameters:
   - K → String  
   - V → Integer
   ```

2. **🔍 Required Parameterized Types Analysis**
   ```
   Dict of (String, Integer) needs:
   - iterator() → Iterator of DictEntry of (String, Integer) ✓
   - keys() → Iterator of String (NOT Iterator of Character!) ✓  
   - values() → Iterator of Integer ✓
   ```

3. **🎯 Generate ALL Decorated Names**
   ```bash
   # Check if each parameterized type exists
   find . -name "_Iterator_852BE8F78E9C*.java" 
   # If missing, implement it first
   ```

4. **⚠️ Verify Type Consistency**
   - Check return types match parameter types
   - Verify test assertions expect correct types
   - Validate at compile time, not runtime

#### **Base Class Change Impact Management**

**Challenge:** Base class method changes (e.g., `Dict.get()` from returning null → throwing exceptions) affected BOTH:
- Parameterized Dict tests (`_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6Test`)  
- Base Dict tests (`DictTest`)

**✅ Systematic Fix Process:**
1. **Identify all affected test files**: Use `grep -r "\.get(" src/test/`
2. **Categorize get() calls**:
   - Valid keys (existing) → Should work normally
   - Invalid keys (missing/null/unset) → Need try-catch blocks
3. **Update ALL affected test files**, not just the obvious ones
4. **Add required imports**: `import static org.junit.jupiter.api.Assertions.fail;`

#### **Testing Strategy for Complex Parameterized Types**

**Individual Component Tests:**
```bash
mvn test -Dtest=_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2Test -pl ek9-lang
mvn test -Dtest=_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4ETest -pl ek9-lang
mvn test -Dtest=_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6Test -pl ek9-lang
```

**Integration Validation:**
```bash
mvn test -Dtest="*Dict*Test,*Iterator*Test,*DictEntry*Test" -pl ek9-lang
mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main  # EK9 annotations
```

#### **Final Architecture Achieved**

**Complete Two-Parameter Generic Implementation:**
- **Dict of (String, Integer)** - Main container with proper type-safe get(), keys(), values()
- **DictEntry of (String, Integer)** - Type-safe key-value pairs  
- **Iterator of String** - For keys() iteration (CRITICAL: not Iterator of Character!)
- **Iterator of Integer** - For values() iteration
- **Iterator of DictEntry of (String, Integer)** - For iterator() iteration

**Validation Results:**
- All 15 Dict of (String, Integer) tests passing ✅
- All parameterized type tests passing (68 total) ✅ 
- Bootstrap test validates EK9 annotations ✅
- No ClassCastException runtime errors ✅

#### **Key Takeaway for Future Parameterized Types**

**ALWAYS DO COMPREHENSIVE TYPE ANALYSIS BEFORE CODING**
- Generate decorated names for ALL dependent types
- Verify type parameter mappings are correct
- Check existing implementations before assuming they exist  
- Test incrementally: individual types → integration → full suite
- Remember that base class changes affect ALL dependent test files

This systematic approach prevents runtime ClassCastExceptions and ensures type-safe parameterized implementations.

## Two-Parameter Generic Implementation Pattern

### **EK9 Two-Parameter Generic Dependency Chain**
Successfully implemented complete dependency chain for Dict of (String, Integer):

1. **Iterator of Integer** (`_Iterator_2648BF49...`) - Simple single-parameter delegation
2. **DictEntry of (String, Integer)** (`_DictEntry_87A55D44...`) - Dual-parameter type (K→String, V→Integer)  
3. **Iterator of DictEntry of (String, Integer)** (`_Iterator_48D70134...`) - Nested parameterization
4. **Dict of (String, Integer)** (`_Dict_7E7710D38A91...`) - Complex integration of all types

### **Key Architectural Insights**

**1. Base Class Method Limitations Discovery**
- **Iterator base class** only has: `hasNext()`, `next()`, `_isSet()`, `_eq()`, `_hashcode()`
- **DictEntry base class** only has: `key()`, `value()`, `_isSet()`, `_eq()`, `_neq()`, `_cmp()`, `_string()`, `_hashcode()`
- **NO**: `_copy()`, `_replace()`, `_merge()`, `_addAss()`, `_add()` methods exist on base Iterator/DictEntry
- **Pattern**: Only implement methods that actually exist on base classes

**2. Delegate Access Pattern**
For complex operations requiring access to base delegate objects:
```java
// Add getDelegate() method to parameterized types
public DictEntry getDelegate() {
  return delegate;
}

// Use delegate in operations requiring base type
delegate._pipe(arg.getDelegate());
```

**3. Nested Type Conversion Pattern**
```java
// Converting base Iterator results to parameterized DictEntry
public _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E next() {
  final var baseEntry = (DictEntry) delegate.next();
  return _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(baseEntry);
}
```

### **Comprehensive Two-Parameter Implementation**

**Dict of (String, Integer) Key Features:**
- **5 Methods**: `get()`, `getOrDefault()`, `iterator()`, `keys()`, `values()`  
- **19 Operators**: All Dict operators with proper type substitution
- **Type-Safe Returns**: 
  - `keys()` → Iterator of String
  - `values()` → Iterator of Integer  
  - `iterator()` → Iterator of DictEntry of (String, Integer)

### **Critical Implementation Lessons**

**1. DecoratedName Usage for Complex Generics**
```bash
# Correct command for nested parameterization
java -cp ./compiler-main/target/classes org.ek9lang.compiler.support.DecoratedName \
  Iterator org.ek9.lang::Iterator org.ek9.lang::_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E
```

**2. Namespace Consistency Rule**
All parameterized types remain in same namespace as base generic type:
- `Dict` → `org.ek9.lang`
- `Dict of (String, Integer)` → `org.ek9.lang`
- `Iterator of Character` → `org.ek9.lang`

**3. Testing Strategy for Complex Generics**
- **Focus on actual available methods** only
- **Test type safety at all levels** (String keys, Integer values, parameterized returns)
- **Verify delegation consistency** between base and parameterized types
- **Test complete integration** (Dict → Iterator → DictEntry interactions)

### **Key Pattern Summary**

**EK9 Two-Parameter Generic Delegation Pattern:**
```java
@Ek9ParameterisedType("Dict of (String, Integer)")
public class _Dict_<hash> extends BuiltinType {
  private final Dict delegate;  // Delegate to base generic
  
  // Type-safe methods with parameter substitution
  public Integer get(String key) { return (Integer) delegate.get(key); }
  public _Iterator_<StringHash> keys() { return _Iterator_<StringHash>._of(delegate.keys()); }
  public _Iterator_<IntegerHash> values() { return _Iterator_<IntegerHash>._of(delegate.values()); }
  
  // All operators delegate with type safety
  public Boolean _eq(_Dict_<hash> arg) { return delegate._eq(arg.delegate); }
}
```

This complete pattern ensures:
- All 4 interdependent parameterized types implemented and tested
- Complete two-parameter generic delegation pattern established
- Complex nested parameterization working correctly
- Full type safety maintained across all operations
- Integration with existing EK9 Iterator of String confirmed
- Ready for production use and as pattern for future multi-parameter generics