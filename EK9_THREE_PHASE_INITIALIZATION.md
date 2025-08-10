# EK9 Three-Phase Object Initialization Architecture

## Overview

EK9 implements a three-phase object initialization system that provides clear separation between class loading, object creation, and constructor execution. This architecture ensures proper inheritance handling, memory management, and backend compatibility for both JVM and C++ LLVM targets.

## Initialization Phases

### Phase 1: `c_init` - Construct/Class Initialization
- **When**: Once per class loading
- **Purpose**: Static field initialization, constants, and class-level setup
- **Inheritance**: Super class `c_init` called first
- **Backend Mapping**: 
  - JVM: `<clinit>` static initialization blocks
  - C++: Static initialization and global constructors

### Phase 2: `i_init` - Instance Initialization  
- **When**: Once per object creation (before any constructor)
- **Purpose**: 
  - Field REFERENCE declarations
  - Field immediate initializations
  - Memory allocation setup
- **Inheritance**: Super class `i_init` called first
- **Backend Mapping**:
  - JVM: Instance initialization blocks and field declarations
  - C++: Constructor initialization lists and member initialization

### Phase 3: `init` - Constructor Methods
- **When**: Per constructor call (after `i_init` completes)
- **Purpose**: Constructor-specific logic only
- **Inheritance**: Super class `init` called first
- **Backend Mapping**:
  - JVM: `<init>` constructor methods
  - C++: Constructor body logic

## Inheritance Chain Execution Order

For class hierarchy `Child extends Parent extends Base`:

### Class Loading Time
```
1. Base.c_init()    // Static initialization
2. Parent.c_init()  // Static initialization  
3. Child.c_init()   // Static initialization
```

### Object Creation Time (`new Child()`)
```
4. Base.i_init()    // Field declarations and immediate initialization
5. Parent.i_init()  // Field declarations and immediate initialization
6. Child.i_init()   // Field declarations and immediate initialization
7. Base.init()      // Base constructor logic
8. Parent.init()    // Parent constructor logic  
9. Child.init()     // Child constructor logic
```

## EK9 Source Code Examples

### Simple Class with Inheritance
```ek9
// Base class
defines class
  Base
    baseField as String := "base value"
    
    Base() as pure
      Stdout().println("Base constructor")

// Child class  
defines class  
  Child extends Base
    childField as Integer := 42
    optionalField as String?
    
    Child() as pure
      optionalField :=? "initialized in constructor"
      Stdout().println("Child constructor")
```

### Generated IR Structure

#### Base Class IR
```
Construct: Base

Operation: Base.c_init()->org.ek9.lang::Void
  // No static fields in this example
  IRInstruction: RETURN

Operation: Base.i_init()->org.ek9.lang::Void  
  // Field declarations
  IRInstruction: REFERENCE this.baseField, org.ek9.lang::String
  
  // Immediate initialization: baseField := "base value"
  IRInstruction: _temp1 = LOAD_LITERAL "base value", org.ek9.lang::String
  IRInstruction: RETAIN _temp1
  IRInstruction: STORE this.baseField, _temp1
  IRInstruction: RETAIN this.baseField
  IRInstruction: RETURN

Operation: Base.init()->Base
  // Constructor logic: Stdout().println("Base constructor")
  IRInstruction: SCOPE_ENTER _scope_1
  IRInstruction: _temp1 = CALL org.ek9.lang::Stdout.i_init()
  IRInstruction: CALL _temp1.init()
  IRInstruction: _temp2 = LOAD_LITERAL "Base constructor", org.ek9.lang::String
  IRInstruction: CALL _temp1.println(_temp2)  
  IRInstruction: SCOPE_EXIT _scope_1
  IRInstruction: RETURN
```

#### Child Class IR
```
Construct: Child extends Base

Operation: Child.c_init()->org.ek9.lang::Void
  IRInstruction: CALL Base.c_init()  // Super class first
  // No static fields in this example
  IRInstruction: RETURN

Operation: Child.i_init()->org.ek9.lang::Void
  IRInstruction: CALL Base.i_init()  // Super instance init first
  
  // Child field declarations
  IRInstruction: REFERENCE this.childField, org.ek9.lang::Integer
  IRInstruction: REFERENCE this.optionalField, org.ek9.lang::String
  
  // Immediate initialization: childField := 42  
  IRInstruction: _temp1 = LOAD_LITERAL 42, org.ek9.lang::Integer
  IRInstruction: RETAIN _temp1
  IRInstruction: STORE this.childField, _temp1
  IRInstruction: RETAIN this.childField
  
  // optionalField has no immediate initialization
  IRInstruction: RETURN

Operation: Child.init()->Child  
  IRInstruction: CALL Base.init()    // Super constructor first
  IRInstruction: SCOPE_ENTER _scope_1
  
  // Constructor logic: optionalField :=? "initialized in constructor"
  IRInstruction: _temp1 = LOAD this.optionalField
  IRInstruction: _temp2 = IS_NULL _temp1
  IRInstruction: BRANCH_TRUE _temp2, assign_label
  IRInstruction: BRANCH skip_label
  
  // assign_label:
  IRInstruction: _temp3 = LOAD_LITERAL "initialized in constructor", org.ek9.lang::String
  IRInstruction: RETAIN _temp3
  IRInstruction: STORE this.optionalField, _temp3
  IRInstruction: RETAIN this.optionalField
  
  // skip_label:
  IRInstruction: _temp4 = CALL org.ek9.lang::Stdout.i_init()
  IRInstruction: CALL _temp4.init()  
  IRInstruction: _temp5 = LOAD_LITERAL "Child constructor", org.ek9.lang::String
  IRInstruction: CALL _temp4.println(_temp5)
  IRInstruction: SCOPE_EXIT _scope_1
  IRInstruction: RETURN
```

## Object Creation Call Sequence

### Creating a new object: `child <- Child()`

```
IRInstruction: _temp1 = ALLOCATE Child                    // Heap allocation
IRInstruction: CALL _temp1.i_init()                      // Instance initialization
IRInstruction: CALL _temp1.init()                        // Constructor call  
IRInstruction: STORE child, _temp1                       // Assign to variable
```

### Creating with constructor arguments: `child <- Child("param")`

```
IRInstruction: _temp1 = ALLOCATE Child                    // Heap allocation
IRInstruction: CALL _temp1.i_init()                      // Instance initialization
IRInstruction: _temp2 = LOAD_LITERAL "param", org.ek9.lang::String
IRInstruction: CALL _temp1.init(_temp2)                  // Constructor with args
IRInstruction: STORE child, _temp1                       // Assign to variable
```

## Memory Management Implications

### Reference Counting Integration
- **`c_init`**: No reference counting (static/class-level data)
- **`i_init`**: Properties get `RETAIN` but no `SCOPE_REGISTER` (object lifetime)
- **`init`**: Local variables get `RETAIN` and `SCOPE_REGISTER` (method scope)

### Property vs Local Variable Distinction
- **Properties**: Declared in `i_init`, lifetime tied to object
- **Local variables**: Declared in `init`, lifetime tied to method scope
- **Property naming**: `this.fieldName` 
- **Local variable naming**: `variableName_scopeId`

## Backend Code Generation

### JVM Bytecode Generation
```java
// c_init() -> <clinit>
static {
  // Static field initialization
}

// i_init() -> Instance initialization
{
  // Field declarations and immediate initialization
  this.baseField = "base value";
  this.childField = 42;
}

// init() -> <init>  
public Child() {
  super(); // Calls parent constructor chain
  // Constructor-specific logic
  if (this.optionalField == null) {
    this.optionalField = "initialized in constructor";
  }
}
```

### C++ Code Generation
```cpp
// c_init() -> Static initialization
static bool Child_c_init_done = false;
void Child::c_init() {
  if (!Child_c_init_done) {
    Base::c_init();  // Super class first
    // Static initialization here
    Child_c_init_done = true;
  }
}

// i_init() -> Member initialization
void Child::i_init() {
  Base::i_init();  // Super instance init first
  // Field initialization (constructor initialization list equivalent)
  baseField = std::make_shared<String>("base value");
  childField = std::make_shared<Integer>(42);
  optionalField = nullptr;  // Optional field starts unset
}

// init() -> Constructor body
Child::Child() {
  i_init();  // Instance initialization first
  // Constructor-specific logic
  if (optionalField == nullptr) {
    optionalField = std::make_shared<String>("initialized in constructor");
  }
}
```

## Property Types and Initialization Patterns

### 1. Uninitialized Properties
```ek9
aField as String?  // No immediate initialization
```
**IR**: REFERENCE declaration in `i_init`, no STORE instruction

### 2. Constructor-Initialized Properties  
```ek9
bField as String := String()  // Constructor call initialization
```
**IR**: REFERENCE + constructor call + STORE in `i_init`

### 3. Literal-Initialized Properties
```ek9  
cField as String := "Steve"  // Literal initialization
```
**IR**: REFERENCE + LOAD_LITERAL + STORE in `i_init`

### 4. Direct Assignment Properties
```ek9
dField <- "Stephen"  // Direct assignment (equivalent to := for properties)
```
**IR**: REFERENCE + LOAD_LITERAL + STORE in `i_init`

### 5. Constructor-Conditional Assignment
```ek9
Example() as pure
  aField :=? "Now Initialised"  // Conditional assignment in constructor
```  
**IR**: IS_NULL + BRANCH + conditional STORE in `init`

## Integration with EK9 Compiler Phases

### Phase 7: IR Generation
1. **ClassDfnGenerator**: Creates `c_init`, `i_init`, and `init` operations
2. **PropertyDeclInstrGenerator**: Targets `i_init` operation
3. **OperationDfnGenerator**: Processes constructor bodies for `init` operation
4. **Inheritance handling**: Ensures proper super call sequences

### Compilation Order
1. Parse and resolve all class hierarchies
2. Generate `c_init` operations (class initialization)
3. Generate `i_init` operations (field declarations and immediate initialization)
4. Generate `init` operations (constructor bodies)
5. Ensure proper inheritance call chains

## Error Handling and Validation

### Compile-Time Checks
- Verify inheritance chain is acyclic
- Ensure super calls are first instruction in each phase
- Validate field initialization order
- Check for circular dependencies in immediate initialization

### Runtime Considerations  
- Class initialization happens once and is thread-safe
- Instance initialization is atomic per object
- Constructor failures leave object in partially initialized state

## Future Extensions

### Generic Types
Each parameterized generic type gets its own initialization operations:
```
Operation: List<String>.c_init()->org.ek9.lang::Void
Operation: List<String>.i_init()->org.ek9.lang::Void  
Operation: List<String>.init()->List<String>
```

### Traits and Mixins
Trait initialization integrated into inheritance chain:
```
// Class with traits: Child extends Parent with TraitA, TraitB
1. Parent.c_init(), TraitA.c_init(), TraitB.c_init(), Child.c_init()
2. Parent.i_init(), TraitA.i_init(), TraitB.i_init(), Child.i_init()
3. Parent.init(), TraitA.init(), TraitB.init(), Child.init()
```

### Interface Implementation
Interface requirements validated during compilation, no runtime initialization needed.

## Migration from Current Architecture

### Current Issues
- Property REFERENCE declarations in constructor (duplicated across multiple constructors)
- Mixed object creation and constructor logic  
- Incorrect use of `<init>` for object creation calls

### Migration Steps
1. Extract property processing from constructors to `i_init` operations
2. Create `c_init` operations for static/class-level initialization
3. Modify object creation calls to use `ALLOCATE + i_init + init` sequence
4. Update inheritance handling to call super methods in each phase
5. Refactor PropertyDeclInstrGenerator to target `i_init`

This three-phase architecture provides a solid foundation for EK9's object model that scales cleanly with inheritance, generics, and traits while maintaining excellent backend compatibility.