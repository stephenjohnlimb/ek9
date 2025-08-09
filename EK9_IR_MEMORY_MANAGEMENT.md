# EK9 IR Memory Management Architecture

This document provides a comprehensive guide to EK9's Intermediate Representation (IR) memory management system, covering the complete architecture from basic principles to cross-platform implementation details.

## Overview

EK9's IR implements a sophisticated reference counting system that ensures memory safety across target backends (JVM and C++ LLVM). The system uses a dual-tracking approach with precise object lifecycle management and cross-scope assignment safety.

## Core Architecture Principles

### 1. Unified REFERENCE Memory Model
All variables use `REFERENCE` instructions instead of `ALLOCA`. This pushes memory allocation complexity to backend implementations while maintaining consistent semantics across JVM and C++ targets.

```
REFERENCE variableName, TypeName  // Declare variable reference
```

### 2. Object vs Variable Tracking
The system tracks **both objects and variables** to handle different memory management scenarios:

- **Object Tracking**: For cleanup of allocated memory (`_temp1`, `_temp2`, etc.)
- **Variable Tracking**: For cleanup of variable references (`someLocal`, `claude`, etc.)

### 3. Reference Counting Model
Objects start with refcount=0 when created. Only IR instructions modify reference counts:

- `RETAIN variable` → Increment refcount of pointed-to object
- `RELEASE variable` → Decrement refcount of pointed-to object
- `SCOPE_EXIT scope_id` → Release all registered variables/objects in scope

## Memory Management Instructions

### Core Instructions
```
REFERENCE var, Type     # Declare variable reference (no allocation)
RETAIN var             # Increment refcount of object var points to
RELEASE var            # Decrement refcount of object var points to (tolerant of uninitialized)
SCOPE_ENTER scope_id   # Enter scope for memory management
SCOPE_EXIT scope_id    # Exit scope (auto-release all registered items)
SCOPE_REGISTER var, scope_id  # Register var/object for cleanup on scope exit
```

### Memory Operations
```
LOAD var               # Load reference from variable
STORE target, source   # Store reference in target variable  
LOAD_LITERAL value     # Create literal object (refcount=0)
CALL Type.<init>()     # Create object via constructor (refcount=0)
```

## Scope Management Architecture

### Scope Types and Registration Rules

#### Parameters (NOT registered)
```ek9
-> arg0 as String      # Caller-managed, no scope registration
```

#### Return Variables (NOT registered)
```ek9
<- rtn as String       # Ownership transferred to caller, no variable registration
```

#### Local Variables (ARE registered)
```ek9
someLocal as String?   # Function-managed, registered in body scope
claude <- "Hello"      # Function-managed, registered in body scope
```

### Scope Hierarchy
```
_param_1               # Parameter scope (variables not registered)
_return_1              # Return scope (variables not registered, objects tracked)
_scope_1               # Function body scope (variables and objects registered)
```

## Assignment Patterns

### RELEASE-then-RETAIN Pattern
All variable assignments follow this memory-safe pattern:

```
RELEASE target_variable    # Decrement refcount of current value
# [evaluate new value]     # Create or load new value
RETAIN new_value          # Increment refcount of new value
STORE target_variable, new_value  # Assign
```

### Assignment Statement Pattern
For statements like `someLocal = "Hi"`:

```
RELEASE someLocal                     # Release old value (no-op if uninitialized)
_temp2 = LOAD_LITERAL "Hi"           # Create object, refcount=0
RETAIN _temp2                        # Object refcount: 0→1 (creation)
SCOPE_REGISTER _temp2, _scope_1      # Track object for cleanup
RETAIN _temp2                        # Object refcount: 1→2 (assignment)
STORE someLocal, _temp2              # someLocal → object
```

### Variable Declaration with Assignment Pattern  
For declarations like `claude <- "Hello Claude"`:

```
REFERENCE claude, String             # Declare variable reference
SCOPE_REGISTER claude, _scope_1      # Register variable for cleanup
_temp10 = LOAD_LITERAL "Hello Claude" # Create object, refcount=0
RETAIN _temp10                       # Object refcount: 0→1 (creation)
SCOPE_REGISTER _temp10, _scope_1     # Track object for cleanup
RETAIN _temp10                       # Object refcount: 1→2 (assignment)
STORE claude, _temp10                # claude → object
```

**Critical**: Both patterns generate identical reference counting behavior.

## Cross-Scope Assignment Safety

### The Challenge
When assigning across scopes (e.g., `rtn: claude`), objects must survive scope boundaries without memory leaks or use-after-free errors.

### Solution: RETAIN for Cross-Scope Survival
```
RELEASE rtn                          # Release old return value
_temp11 = LOAD claude                # Load local variable value
RETAIN _temp11                       # Increment refcount for cross-scope assignment
STORE rtn, _temp11                   # Assign to return variable
```

### Memory Flow Example
Given `rtn: claude` where `claude` points to Object X:

**Before Assignment:**
- Object X: refcount=2 (creation + assignment)
- `claude` and `_temp10` both point to Object X

**During Assignment:**
- `RETAIN _temp11`: Object X refcount 2→3
- Object X now survives local scope exit

**Scope Exit:**
- `claude` release: Object X refcount 3→2
- `_temp10` release: Object X refcount 2→1
- Object X survives with refcount=1 ✅

## Operator and Expression Support

### Question Operator (`?`)
The `?` operator generates `_isSet()` method calls:

```ek9
assert someLocal?     # EK9 source
```

```
_temp4 = LOAD someLocal              # Load variable
_temp3 = CALL _temp4._isSet()        # Call _isSet() method
_temp5 = CALL _temp3._true()         # Convert to primitive boolean
ASSERT _temp5                        # Assert the result
```

### Assert Statements
Assert statements use EK9 Boolean tri-state semantics:

```ek9
assert expression     # EK9 source
```

```
_temp = [evaluate expression]        # Evaluate to EK9 Boolean
_tempResult = CALL _temp._true()     # Get primitive boolean
ASSERT _tempResult                   # Assert on primitive value
```

## Scope Counter Architecture

### Per-Prefix Counter System
Uses `Map<String, AtomicInteger>` for logical numbering:

- **Before**: `_param_1`, `_return_2`, `_scope_3` (confusing global counter)
- **After**: `_param_1`, `_return_1`, `_scope_1` (logical per-prefix counters)

### Implementation
```java
private final ConcurrentHashMap<String, AtomicInteger> prefixCounters = new ConcurrentHashMap<>();

private int getNextCounterFor(final String prefix) {
    return prefixCounters.computeIfAbsent(prefix, k -> new AtomicInteger(0)).incrementAndGet();
}
```

## Complete IR Generation Example

### EK9 Source
```ek9
aSimpleFunction()
  -> arg0 as String
  <- rtn as String: String()
  
  someLocal as String?
  someLocal = "Hi"
  assert someLocal?
  
  claude <- "Hello Claude"
  rtn: claude
```

### Generated IR
```
# Parameter declaration (not scope-registered)
REFERENCE arg0, org.ek9.lang::String

# Return scope with initial value
SCOPE_ENTER _return_1
REFERENCE rtn, org.ek9.lang::String
_temp1 = CALL org.ek9.lang::String.<init>()
RETAIN _temp1                        # Object A refcount: 0→1
SCOPE_REGISTER _temp1, _return_1     # Track object
RETAIN _temp1                        # Object A refcount: 1→2
STORE rtn, _temp1

# Function body scope
SCOPE_ENTER _scope_1

# Local variable declaration and assignment
REFERENCE someLocal, org.ek9.lang::String
SCOPE_REGISTER someLocal, _scope_1   # Track variable
RELEASE someLocal                    # No-op (uninitialized)
_temp2 = LOAD_LITERAL "Hi"
RETAIN _temp2                        # Object X refcount: 0→1
SCOPE_REGISTER _temp2, _scope_1      # Track object
RETAIN _temp2                        # Object X refcount: 1→2
STORE someLocal, _temp2

# Assert with ? operator
_temp4 = LOAD someLocal
_temp3 = CALL _temp4._isSet()
_temp5 = CALL _temp3._true()
ASSERT _temp5

# Variable declaration with assignment
REFERENCE claude, org.ek9.lang::String
SCOPE_REGISTER claude, _scope_1      # Track variable
_temp10 = LOAD_LITERAL "Hello Claude"
RETAIN _temp10                       # Object Y refcount: 0→1
SCOPE_REGISTER _temp10, _scope_1     # Track object
RETAIN _temp10                       # Object Y refcount: 1→2
STORE claude, _temp10

# Cross-scope assignment
RELEASE rtn                          # Object A refcount: 2→1
_temp11 = LOAD claude
RETAIN _temp11                       # Object Y refcount: 2→3
STORE rtn, _temp11

# Scope cleanup
SCOPE_EXIT _scope_1                  # Release all _scope_1 registrations
                                     # someLocal: Object X refcount decrements
                                     # claude: Object Y refcount 3→2
                                     # _temp2: Object X refcount decrements
                                     # _temp10: Object Y refcount 2→1

SCOPE_EXIT _return_1                 # Release _return_1 objects
                                     # _temp1: Object A refcount 1→0 → freed

RETURN rtn                           # Return Object Y (refcount=1) ✅
```

## Backend Implementation Mapping

### JVM/ASM Backend
```java
// RETAIN operation
public void retain(String variable) {
    // JVM: No-op (GC handles memory) or increment reference counter
    if (REFERENCE_COUNTING_ENABLED) {
        Object obj = getVariableValue(variable);
        if (obj instanceof EK9Object ek9Obj) {
            ek9Obj.incrementRefCount();
        }
    }
}

// RELEASE operation  
public void release(String variable) {
    if (REFERENCE_COUNTING_ENABLED) {
        Object obj = getVariableValue(variable);
        if (obj instanceof EK9Object ek9Obj) {
            ek9Obj.decrementRefCount();
            if (ek9Obj.getRefCount() == 0) {
                // Cleanup object
            }
        }
    }
}

// SCOPE_EXIT operation
public void scopeExit(String scopeId) {
    List<String> registered = scopeRegistrations.get(scopeId);
    for (String item : registered) {
        release(item);  // Release all registered items
    }
}
```

### C++ LLVM Backend with ARC
```cpp
namespace ek9::lang {

// Base class with intrusive reference counting
class BuiltinType {
private:
    mutable std::atomic<int> refCount{0};
    bool isSet = false;

public:
    // ARC operations called from LLVM IR
    void retain() const { 
        refCount.fetch_add(1, std::memory_order_relaxed); 
    }
    
    void release() const { 
        if (refCount.fetch_sub(1, std::memory_order_acq_rel) == 1) {
            delete this;  // Automatic cleanup
        }
    }
    
    int getRefCount() const { return refCount; }
    
    // EK9 tri-state semantics
    bool _isSet() const { return isSet; }
    bool _false() const { return !isSet; }  // Optimized for :=? operator
    void unSet() { isSet = false; }
    void set() { isSet = true; }
    
    virtual ~BuiltinType() = default;
};

} // namespace ek9::lang

// Runtime functions called from LLVM IR
extern "C" {
    void ek9_retain(BuiltinType* obj) { 
        if(obj) obj->retain(); 
    }
    
    void ek9_release(BuiltinType* obj) { 
        if(obj) obj->release(); 
    }
    
    void ek9_scope_exit(ScopeManager* scope) {
        scope->releaseAll();  // RAII cleanup
        delete scope;
    }
}

// RAII Scope Management
class ScopeManager {
private:
    std::vector<BuiltinType*> registeredObjects;
    
public:
    void registerObject(BuiltinType* obj) {
        if (obj) registeredObjects.push_back(obj);
    }
    
    void releaseAll() {
        for (auto* obj : registeredObjects) {
            obj->release();  // Decrement reference counts
        }
        registeredObjects.clear();
    }
    
    ~ScopeManager() { releaseAll(); }  // Automatic cleanup
};
```


## Memory Safety Guarantees

### 1. No Memory Leaks
- All created objects are tracked via SCOPE_REGISTER
- All variables pointing to objects are tracked in appropriate scopes
- SCOPE_EXIT ensures cleanup of all tracked items

### 2. No Use-After-Free
- Cross-scope assignments use RETAIN to increment reference counts
- Objects survive scope boundaries when referenced elsewhere
- Reference counting ensures objects remain valid while referenced

### 3. No Double-Free
- RELEASE operations check reference counts before freeing
- SCOPE_EXIT only releases what's actually registered
- Tolerant design handles uninitialized variables gracefully

### 4. Cross-Platform Consistency
- Same IR works identically on JVM and C++ LLVM backends
- Backend implementations map to appropriate memory management APIs (GC vs ARC)
- Semantic correctness maintained across both target architectures

## Implementation Classes

### Key Classes
- `OperationDfnGenerator`: Orchestrates IR generation with proper scope management
- `VariableDeclarationInstrGenerator`: Handles variable declarations and registrations
- `StatementInstrGenerator`: Implements RELEASE-then-RETAIN assignment pattern
- `ExpressionInstrGenerator`: Handles operators, literals, and method calls
- `IRContext`: Provides per-prefix counter system and temporary variable generation

### Scope Registration Logic
```java
private boolean shouldRegisterVariableInScope(final String scopeId) {
    // Don't register parameters - managed by caller
    if (scopeId.startsWith("_param_")) return false;
    
    // Don't register return variables - ownership transferred to caller  
    if (scopeId.startsWith("_return_")) return false;
    
    // Register local variables - managed by this function
    if (scopeId.startsWith("_scope_")) return true;
    
    return false;
}
```

## Testing and Validation

The memory management system is validated through comprehensive test cases covering:

- Basic variable assignments
- Cross-scope assignments (return variables)
- Operator expressions (`?` operator)
- Assert statements with tri-state Boolean logic
- Constructor calls and literal assignments
- Scope entry/exit sequences

### Reference Test Case
The `workarea.ek9` test case demonstrates all memory management patterns and serves as the canonical example for IR generation correctness.

## Future Considerations

### Optimization Opportunities
- **JVM Backend**: Most RETAIN/RELEASE operations can be no-ops (GC handles cleanup)
- **C++ Backend**: Escape analysis to reduce reference counting overhead
- **Cross-Platform**: Uniform optimization passes applied at IR level before backend generation
- **ARC Optimizations**: Copy elision, move semantics, and stack allocation for temporary objects

### C++ Runtime Extensions
- **Smart Pointer Integration**: `Ref<T>` wrapper for automatic RETAIN/RELEASE
- **Circular Reference Detection**: Weak reference support in C++ runtime
- **Memory Pool Management**: Custom allocators for EK9 object types
- **Property Lifecycle**: Distinct property vs local variable memory management
- **Inheritance Support**: Super constructor chaining with proper cleanup order

## Property vs Local Variable Memory Management

### Property Lifecycle Distinction

**Properties** (class/object fields):
- **Lifetime**: Object lifetime (not constructor scope)
- **IR Operations**: `PROPERTY_STORE`, `PROPERTY_LOAD`, `PROPERTY_RETAIN`
- **Scope Registration**: Properties get `RETAIN` but **no SCOPE_REGISTER**
- **Memory Management**: Manual reference counting, survive constructor scope

**Local Variables** (constructor/method locals):
- **Lifetime**: Method/constructor scope
- **IR Operations**: `STORE`, `LOAD`, `RETAIN`, `RELEASE`
- **Scope Registration**: Variables get both `RETAIN` and `SCOPE_REGISTER`
- **Memory Management**: Automatic scope-based cleanup via `SCOPE_EXIT`

### Conditional Assignment (`:=?`) Memory Management

**EK9 `:=?` Operator**: Assign only if target is null (uninitialized) OR unset (EK9 tri-state)

**Optimized IR Pattern:**
```
// For: this.aField :=? "value"
_temp1 = LOAD_LITERAL "value"
_temp2 = PROPERTY_LOAD this.aField                // Load current property
_temp3 = IS_NULL _temp2                           // Check null (uninitialized)
BRANCH_TRUE _temp3, assign_label                  // If null, assign
_temp4 = CALL _temp2._isSet()                     // Check EK9 tri-state
_temp5 = CALL _temp4._false()                     // Optimized: direct false check
BRANCH_TRUE _temp5, assign_label                  // If unset, assign
BRANCH skip_label                                 // Skip assignment
assign_label:
  PROPERTY_STORE this.aField, _temp1              // Property assignment
  PROPERTY_RETAIN this.aField                     // Property reference management
skip_label:
```

**C++ Implementation:**
```cpp
// C++ runtime for conditional property assignment
extern "C" void ek9_property_assign_if_unset(BuiltinType** field_ptr, BuiltinType* new_value) {
    // Check if field is null (uninitialized)
    if (*field_ptr == nullptr) {
        *field_ptr = new_value;
        if (new_value) new_value->retain();
        return;
    }
    
    // Check if field is unset (EK9 tri-state)
    if (!(*field_ptr)->_isSet()) {
        (*field_ptr)->release();  // Release old unset value
        *field_ptr = new_value;
        if (new_value) new_value->retain();
    }
}
```

## Constructor and Inheritance Memory Management

### Constructor Execution Order

Following industry standards (Java, C++):
1. **Super constructor call** (object hierarchy initialization)
2. **Property default values/immediate assignments** (`:=`, `<-`)
3. **Constructor body** (`:=?` assignments and user logic)

### Super Constructor IR Pattern

**EK9 Inheritance:**
```ek9
Extension1 extends Base
  Extension1()
    -> arg as String
    super(arg)           // Explicit super constructor call
    someProperty := arg  // Property initialization
```

**Generated IR:**
```
CALL super.<init>(arg)                     // Super constructor first
// Super constructor handles its property initialization
PROPERTY_STORE this.someProperty, arg      // Derived property initialization
PROPERTY_RETAIN this.someProperty          // Property reference management
SCOPE_ENTER _scope_1                       // Constructor body scope
// Constructor body logic...
SCOPE_EXIT _scope_1                        // Constructor locals cleanup
RETURN                                     // Constructor return
```

**C++ Runtime Integration:**
```cpp
// Super constructor call from LLVM IR
extern "C" Base* ek9_super_constructor(Base* this_ptr, /* args */) {
    // Call C++ base class constructor logic
    return new (this_ptr) Base(/* args */);
}

// Property initialization distinct from constructor scope
extern "C" void ek9_property_init_derived(Extension1* obj) {
    // Initialize derived class properties after super constructor
    // Properties managed separately from constructor locals
}
```

## Conclusion

The EK9 IR memory management system provides a robust foundation for memory-safe code generation with perfect alignment to C++ ARC implementation. The dual-tracking approach (objects + variables), combined with precise reference counting and scope-based cleanup, ensures memory safety across JVM and C++ backends while maintaining semantic consistency and performance.

**Key Architectural Strengths:**
- **Perfect C++ Mapping**: RETAIN/RELEASE → atomic reference counting operations
- **Property Awareness**: Distinct property vs local variable lifecycle management  
- **Inheritance Support**: Industry-standard constructor execution order with super chaining
- **Conditional Assignment**: Optimized `:=?` operator with null + unset checking
- **RAII Integration**: Scope management leverages C++ automatic cleanup patterns
- **Cross-Platform**: Same IR semantics work for both GC (JVM) and manual (C++) memory management

The system enables backend implementations to focus on target-specific optimizations while relying on the IR's mathematical correctness for memory safety guarantees, with C++ providing the ideal native runtime for high-performance EK9 applications.