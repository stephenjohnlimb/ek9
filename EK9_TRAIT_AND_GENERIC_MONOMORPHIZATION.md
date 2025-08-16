# EK9 Trait and Generic Monomorphization Strategy

> **Purpose**: Comprehensive guide for implementing unified monomorphization of traits and generic types in EK9
> 
> **Created**: From analysis of EK9 trait examples and generic type requirements
> 
> **Usage**: Reference this document when implementing trait IR generation and generic type monomorphization

## Overview

EK9 uses a unified **complete monomorphization strategy** with **InterfaceTables** for both traits and generic types. This approach generates concrete, specialized implementations at compile time, eliminating runtime dispatch overhead and enabling maximum performance optimization.

**Key Principle**: All polymorphism (traits and generics) is resolved at compile time through monomorphization, creating concrete implementations for each unique combination of types and trait implementations.

## Unified Monomorphization Strategy

### Core Architecture

Both traits and generics use the same fundamental approach:

1. **Collection Phase**: Gather all trait implementations and generic instantiations during FULL_RESOLUTION (Phase 6)
2. **Monomorphization Planning**: Create specialized versions for each unique combination
3. **IR Generation**: Generate concrete InterfaceTables and implementations during IR_GENERATION (Phase 10)
4. **Direct Dispatch**: Replace all polymorphic calls with direct calls to monomorphized implementations

## Trait Monomorphization

### Simple Trait Example

**EK9 Source:**
```ek9
trait CostAssessment
  lowCost() as pure <- rtn <- true

class SimpleProcessor with trait of CostAssessment
class XMLProcessor with trait of CostAssessment
```

**Monomorphized InterfaceTables:**
```
// Specialized for SimpleProcessor
InterfaceTable: SimpleProcessor$CostAssessment_A8B9C2D4
  ConcreteSlot: lowCost()->org.ek9.lang::Boolean [SimpleProcessor_CostAssessment_lowCost_impl]

// Specialized for XMLProcessor  
InterfaceTable: XMLProcessor$CostAssessment_A8B9C2D4
  ConcreteSlot: lowCost()->org.ek9.lang::Boolean [XMLProcessor_CostAssessment_lowCost_impl]
```

### Complex Trait Hierarchy

**EK9 Source (from JustTraits.ek9):**
```ek9
trait Processor with trait of Moniterable, CostAssessment
  process() <- response as ProcessingResponse?
  override lowCost() as pure
    <- rtn as Boolean: CostAssessment.lowCost()

class SimpleProcessor with trait of Processor
  override process()
    <- response as ProcessingResponse: StringResponse("Simple Message")
```

**Monomorphized IR:**
```
InterfaceTable: SimpleProcessor$Processor_B7E4F3A9
  // All methods are concrete implementations
  ConcreteSlot: available()->org.ek9.lang::Boolean [SimpleProcessor_available_impl]
  ConcreteSlot: lowCost()->org.ek9.lang::Boolean [SimpleProcessor_lowCost_impl]
  ConcreteSlot: process()->com.customer.traits::ProcessingResponse [SimpleProcessor_process_impl]
  ConcreteSlot: _isSet()->org.ek9.lang::Boolean [SimpleProcessor_isSet_impl]

// Concrete implementation methods
OperationDfn: SimpleProcessor_lowCost_impl()->org.ek9.lang::Boolean
BasicBlock: _entry_1
// Direct call to resolved trait method - no virtual dispatch
IRInstruction: _temp1 = CALL CostAssessment_lowCost_impl()
IRInstruction: RETURN _temp1

OperationDfn: SimpleProcessor_process_impl()->com.customer.traits::ProcessingResponse
BasicBlock: _entry_1
// Direct constructor call - fully monomorphized
IRInstruction: _temp1 = CALL (com.customer.traits::StringResponse)StringResponse_init_impl("Simple Message")
IRInstruction: RETURN _temp1
```

### Trait Delegation Monomorphization

**EK9 Source:**
```ek9
class DelegatingProcessor with trait of Processor by proc
  proc as Processor?
  
  override lowCost() as pure <- rtn as Boolean: false
```

**Monomorphized IR:**
```
InterfaceTable: DelegatingProcessor$Processor_C9F2E5A1
  ConcreteSlot: available()->org.ek9.lang::Boolean [DelegatingProcessor_available_delegate]
  ConcreteSlot: lowCost()->org.ek9.lang::Boolean [DelegatingProcessor_lowCost_override] 
  ConcreteSlot: process()->com.customer.traits::ProcessingResponse [DelegatingProcessor_process_delegate]
  ConcreteSlot: _isSet()->org.ek9.lang::Boolean [DelegatingProcessor_isSet_delegate]

// Auto-generated delegation implementations
OperationDfn: DelegatingProcessor_process_delegate()->com.customer.traits::ProcessingResponse
BasicBlock: _entry_1
IRInstruction: _temp1 = LOAD this.proc
IRInstruction: _temp2 = CALL (org.ek9.lang::Boolean)_temp1._isSet()._true()
IRInstruction: BRANCH_TRUE _temp2, _delegate_block, _error_block

BasicBlock: _delegate_block
// Direct call to monomorphized proc implementation
IRInstruction: _temp3 = CALL proc_type_process_impl(_temp1)
IRInstruction: RETURN _temp3

BasicBlock: _error_block
IRInstruction: _temp4 = CALL Exception_init_impl("Delegation object not set")
IRInstruction: THROW _temp4
```

## Generic Type Monomorphization

### Simple Generic Class

**EK9 Source:**
```ek9
class List of type T
  items as ArrayList of T: ArrayList()
  
  add() -> item as T
    items.add(item)
    
  get() -> index as Integer <- rtn as T?
    <- rtn as T: items.get(index)

// Usage creates monomorphized instances
stringList <- List of String()
integerList <- List of Integer()  
personList <- List of Person()
```

**Monomorphized Constructs:**
```
// Monomorphized for String
ConstructDfn: _List_String_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1
Field: items, _ArrayList_String_[hash]

OperationDfn: _List_String_[hash].add(org.ek9.lang::String)->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: REFERENCE item, org.ek9.lang::String
// Direct call to monomorphized ArrayList<String>.add
IRInstruction: CALL _ArrayList_String_add_impl(this.items, item)
IRInstruction: RETURN

OperationDfn: _List_String_[hash].get(org.ek9.lang::Integer)->org.ek9.lang::String
BasicBlock: _entry_1
IRInstruction: REFERENCE index, org.ek9.lang::Integer
// Direct call to monomorphized ArrayList<String>.get
IRInstruction: _temp1 = CALL _ArrayList_String_get_impl(this.items, index)
IRInstruction: RETURN _temp1

// Monomorphized for Integer
ConstructDfn: _List_Integer_2A5C3F4D9E8B7A1F6C0D4E8F3B2A9C5E7D1B4F8A6C3E0D5B8F2A7C4E9D6B1F3A8
Field: items, _ArrayList_Integer_[hash]

OperationDfn: _List_Integer_[hash].add(org.ek9.lang::Integer)->org.ek9.lang::Void
BasicBlock: _entry_1
// Optimized for Integer - possible value type optimization
IRInstruction: CALL _ArrayList_Integer_add_impl(this.items, item)
IRInstruction: RETURN

// Monomorphized for Person (reference type)
ConstructDfn: _List_Person_[hash]
Field: items, _ArrayList_Person_[hash]
// Reference type handling with proper ARC management
```

### Generic Trait Combination

**EK9 Source:**
```ek9
trait Comparable of type T
  compare() -> other as T <- rtn as Integer?
  
trait Sortable of type T with trait of Comparable of T
  sort() -> items as List of T <- rtn as List of T?

class Person with trait of Comparable of Person
  name as String?
  age as Integer?
  
  override compare() -> other as Person <- rtn as Integer
    <- rtn as Integer: this.name._cmp(other.name)

class PersonList with trait of Sortable of Person
  items as List of Person: List()
  
  override sort() -> items as List of Person
    <- rtn as List of Person: items.sortWith(Person.compare)
```

**Monomorphized IR:**
```
// Monomorphized trait for Person
InterfaceTable: Person$Comparable_Person_D5E8F1A4
  ConcreteSlot: compare(Person)->org.ek9.lang::Integer [Person_compare_impl]

OperationDfn: Person_compare_impl(Person)->org.ek9.lang::Integer
BasicBlock: _entry_1
IRInstruction: REFERENCE other, Person
// Direct call to String comparison - fully monomorphized
IRInstruction: _temp1 = CALL String_cmp_impl(this.name, other.name)
IRInstruction: RETURN _temp1

// Monomorphized sortable trait for PersonList
InterfaceTable: PersonList$Sortable_Person_E7A3B9C2
  ConcreteSlot: compare(Person)->org.ek9.lang::Integer [PersonList_compare_inherited]
  ConcreteSlot: sort(_List_Person_[hash])->_List_Person_[hash] [PersonList_sort_impl]

OperationDfn: PersonList_sort_impl(_List_Person_[hash])->_List_Person_[hash]
BasicBlock: _entry_1
// Direct call to monomorphized List<Person>.sortWith with Person.compare
IRInstruction: _temp1 = CALL _List_Person_sortWith_impl(items, Person_compare_impl)
IRInstruction: RETURN _temp1
```

## Implementation Strategy

### Phase Integration

**Phase 6: FULL_RESOLUTION**
```java
// Collect all trait implementations
Map<TraitSymbol, Set<ImplementingClass>> traitImplementations;

// Collect all generic instantiations  
Map<GenericSymbol, Set<TypeArguments>> genericInstantiations;

// Plan monomorphization
for (TraitSymbol trait : allTraits) {
    for (ImplementingClass impl : traitImplementations.get(trait)) {
        generateMonomorphizedTraitPlan(trait, impl);
    }
}

for (GenericSymbol generic : allGenerics) {
    for (TypeArguments args : genericInstantiations.get(generic)) {
        generateMonomorphizedGenericPlan(generic, args);
    }
}
```

**Phase 10: IR_GENERATION**
```java
// Generate monomorphized InterfaceTables
for (MonomorphizedTrait mt : plannedTraits) {
    generateTraitInterfaceTable(mt);
    generateConcreteTraitMethods(mt);
}

// Generate monomorphized generic types
for (MonomorphizedGeneric mg : plannedGenerics) {
    generateMonomorphizedConstruct(mg);
    generateSpecializedMethods(mg);
}

// Replace polymorphic calls with direct calls
replaceTraitCallsWithDirectCalls();
replaceGenericCallsWithMonomorphizedCalls();
```

### Monomorphization Algorithm

**Trait Monomorphization:**
```java
String generateTraitInterfaceTable(TraitSymbol trait, ClassSymbol implementingClass) {
    // Create unique identifier
    String tableId = implementingClass.getName() + "$" + trait.getName() + "_" + generateHash(trait, implementingClass);
    
    // Generate concrete method implementations
    for (MethodSymbol method : trait.getMethods()) {
        if (method.isAbstract()) {
            // Use implementing class's method
            generateConcreteSlot(tableId, method, implementingClass.getMethod(method.getName()));
        } else {
            // Use trait's default method, specialized for implementing class
            generateSpecializedTraitMethod(tableId, method, implementingClass);
        }
    }
    
    return tableId;
}
```

**Generic Monomorphization:**
```java
String generateMonomorphizedGeneric(GenericSymbol generic, List<TypeSymbol> typeArgs) {
    // Create unique identifier with type arguments
    String constructId = "_" + generic.getName() + "_" + 
        typeArgs.stream().map(TypeSymbol::getName).collect(joining("_")) + "_" + 
        generateHash(generic, typeArgs);
    
    // Generate specialized implementations
    for (MethodSymbol method : generic.getMethods()) {
        generateSpecializedGenericMethod(constructId, method, typeArgs);
    }
    
    return constructId;
}
```

## Target Code Generation Benefits

### JVM Bytecode Advantages

**Direct Method Calls:**
```java
// Generated concrete classes - no interface dispatch
public class SimpleProcessor_Processor_A8B9C2D4 {
    public ProcessingResponse process() {
        return new StringResponse_B7E4F3A9("Simple Message");
    }
    
    public Boolean lowCost() {
        return CostAssessment_lowCost_impl(); // Direct static call
    }
}

// Monomorphized generics
public class List_String_8F118296 {
    private ArrayList_String_[hash] items;
    
    public void add(String item) {  // No boxing/unboxing
        items.addString(item);      // Type-specific method
    }
    
    public String get(int index) {  // Direct return type
        return items.getString(index);
    }
}
```

### LLVM Code Generation Advantages

**Optimal Memory Layouts:**
```c++
// Specialized struct layouts per type
struct List_Integer {
    int64_t* items;    // Direct primitive storage
    size_t count;
    size_t capacity;
};

struct List_String {
    String** items;    // Reference storage for objects
    size_t count;
    size_t capacity;
};

// Direct function calls - no vtable overhead
void List_Integer_add(List_Integer* list, int64_t item) {
    // Highly optimized for integers
    if (list->count >= list->capacity) {
        list->items = realloc(list->items, list->capacity * 2 * sizeof(int64_t));
        list->capacity *= 2;
    }
    list->items[list->count++] = item;
}

String* List_String_get(List_String* list, int32_t index) {
    // Bounds checking and direct access
    if (index < 0 || index >= list->count) {
        return exception_throw("Index out of bounds");
    }
    return list->items[index];
}
```

**Trait Implementation:**
```c++
// Direct function calls - no dynamic dispatch
struct SimpleProcessor {
    // No vtable pointer needed
};

ProcessingResponse* SimpleProcessor_process(SimpleProcessor* this) {
    return StringResponse_new("Simple Message");
}

Boolean SimpleProcessor_lowCost(SimpleProcessor* this) {
    return CostAssessment_lowCost_impl(); // Direct function call
}
```

## Performance Benefits

### Compile-Time Optimization

1. **Complete Inlining**: All method calls can be inlined since targets are known
2. **Dead Code Elimination**: Unused monomorphized versions are not generated
3. **Type-Specific Optimization**: Each monomorphized version optimized for its types
4. **Constant Propagation**: Type information enables aggressive constant folding

### Runtime Performance

1. **Zero Virtual Dispatch**: All calls resolved at compile time
2. **Optimal Memory Access**: Type-specific memory layouts
3. **No Type Checking**: All type safety verified at compile time
4. **Cache Efficiency**: Monomorphized code has better cache locality

### Memory Efficiency

1. **No Type Erasure**: Full type information preserved
2. **Specialized Storage**: Primitives stored directly, objects as references
3. **Minimal Metadata**: No runtime type information needed
4. **Optimal Alignment**: Type-specific memory alignment

## Code Size Management

### Smart Monomorphization

**Shared Implementations:**
```java
// Detect identical implementations
if (List<String>.add() == List<Person>.add()) {
    // Generate shared List_Reference_add_impl for all reference types
    generateSharedImplementation("List_Reference_add", referenceTypes);
} else {
    // Generate separate implementations
    generateSeparateImplementations();
}
```

**Lazy Monomorphization:**
```java
// Only generate what's actually used
Set<MonomorphizationRequest> usedCombinations = collectUsedCombinations();
for (MonomorphizationRequest request : usedCombinations) {
    if (request.isActuallyUsed()) {
        generateMonomorphizedVersion(request);
    }
}
```

**Template Specialization Hierarchy:**
```
List<T> where T extends Object
├── List_Reference<T> (shared for all reference types)
│   ├── List_String (if needed for specific optimizations)
│   ├── List_Person (if needed for specific optimizations)
│   └── List_CustomClass (if needed for specific optimizations)
└── List_Primitive<T> (shared for all primitive types)
    ├── List_Integer (value type optimization)
    ├── List_Float (value type optimization)  
    └── List_Boolean (bit packing optimization)
```

## Advanced Monomorphization Patterns

### Conditional Monomorphization

**EK9 Source:**
```ek9
trait Serializable of type T
  serialize() -> item as T <- rtn as String?
  deserialize() -> data as String <- rtn as T?

class Person with trait of Serializable of Person
class Product with trait of Serializable of Product
```

**Conditional IR Generation:**
```java
// Different strategies based on type properties
if (typeArgument.hasCustomSerialization()) {
    generateCustomSerializationMonomorph(trait, typeArgument);
} else if (typeArgument.isSimpleValue()) {
    generateSimpleSerializationMonomorph(trait, typeArgument);
} else {
    generateReflectionBasedMonomorph(trait, typeArgument);
}
```

### Cross-Module Monomorphization

**Module A:**
```ek9
trait Processor of type T
  process() -> item as T <- rtn as ProcessResult of T?
```

**Module B:**
```ek9
class DataProcessor with trait of Processor of Document
  override process() -> item as Document 
    <- rtn as ProcessResult of Document: processDocument(item)
```

**Monomorphization Coordination:**
```java
// Cross-module monomorphization plan
Map<ModuleSymbol, Set<MonomorphizationRequest>> crossModuleRequests;

// Generate bridge implementations
for (ModuleSymbol module : crossModuleRequests.keySet()) {
    generateCrossModuleBridges(module, crossModuleRequests.get(module));
}
```

## Debugging and Tooling Support

### Source Mapping

**Generated IR Includes Source References:**
```
OperationDfn: SimpleProcessor_process_impl()->ProcessingResponse  // ./JustTraits.ek9:67:7
BasicBlock: _entry_1
IRInstruction: _temp1 = CALL StringResponse_init_impl("Simple Message")  // ./JustTraits.ek9:68:53
IRInstruction: RETURN _temp1  // ./JustTraits.ek9:68:53
```

### Monomorphization Reports

```
Monomorphization Report:
==================
Trait Implementations: 15 traits × 23 classes = 37 monomorphized versions
Generic Instantiations: 8 generics × 12 type combinations = 31 monomorphized versions
Shared Implementations: 18 (reduced from 68 total)
Code Size Impact: +23% (145KB → 178KB)
Performance Gain: +67% (benchmark average)
```

### IDE Integration

```java
// Show monomorphized versions in IDE
@MonomorphizedFrom("List of type T")
class List_String_8F118296 {
    // IDE shows: "Specialized version of List<String>"
    public void add(String item) { ... }
}
```

## Future Extensibility

### Partial Monomorphization

For very large codebases, support partial monomorphization:

```java
// Hybrid approach: monomorphize hot paths, use dispatch for cold paths
if (method.isHotPath() || method.getCallFrequency() > threshold) {
    generateMonomorphizedVersion(method);
} else {
    generateDynamicDispatchVersion(method);
}
```

### Incremental Monomorphization

```java
// Only regenerate changed monomorphized versions
if (traitDefinitionChanged(trait)) {
    regenerateAffectedMonomorphizations(trait);
} else {
    reuseExistingMonomorphizations(trait);
}
```

## Summary

The unified monomorphization strategy with InterfaceTables provides:

1. **Maximum Performance**: All dispatch resolved at compile time
2. **Type Safety**: Complete type checking during monomorphization  
3. **Target Flexibility**: Same IR works optimally for JVM and LLVM
4. **Code Quality**: Clean, optimized generated code
5. **Developer Experience**: Full trait and generic support with zero runtime cost
6. **Debugging Support**: Clear mapping from source to generated implementations
7. **Scalability**: Smart code sharing and lazy generation prevent code explosion

This approach gives EK9 the performance characteristics of Rust with the expressiveness of Scala's trait system and the familiarity of Java's generics - truly the best of all worlds!

---

**Created**: Based on analysis of EK9 trait examples (JustTraits.ek9, AmbiguousMethods.ek9) and generic type requirements

**Usage**: Reference this document when implementing TraitDfnGenerator and generic type monomorphization in Phase 10 IR generation