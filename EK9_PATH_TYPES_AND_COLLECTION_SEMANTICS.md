# EK9 Path Types and Collection Semantics

## Path vs FileSystemPath Distinction

### **EK9 Path Type**
- **Purpose**: For structured data paths (JSON-like queries)
- **Format**: Always starts with `$?` 
- **Usage**: Accessing structured data elements
- **Examples**: 
  - `$?.name` 
  - `$?.data.items[0]`
  - `$?.config.settings`
- **Regex Considerations**: The `$?` characters are special in regex and need escaping: `\\$\\?`

### **EK9 FileSystemPath Type**  
- **Purpose**: For actual file system paths
- **Format**: Standard file system path formats
- **Usage**: File operations, directory navigation
- **Examples**:
  - `/test/file.txt`
  - `/home/user/documents`
  - `./src/main/java`

### **Implementation Impact**
- RegEx `matches` operator supports both types with overloaded methods
- Path constructor may validate format, rejecting invalid syntax (resulting in unset Path)
- FileSystemPath follows standard file system conventions

## EK9 Collection Semantics

### **Key Rule: Collections Cannot Be Unset**
Collections in EK9 (List, Dict) have unique semantics:

- **Always Set**: Collections are always considered "set" even when empty
- **Empty ≠ Unset**: An empty collection is still a valid, set collection
- **Constructor Behavior**: `new List()` creates a set (but empty) collection

### **Testing Implications**
When testing collection operations:
```java
// CORRECT: Test for empty collections
assertTrue.accept(result._empty());
assertEquals(0, result._len().state);

// INCORRECT: Don't test for unset collections  
assertUnset.accept(result); // This will fail - collections are always set
```

### **Invalid Operation Results**
When collection operations fail:
- **Return**: Empty collection (not unset collection)
- **Example**: `regEx.split(unsetString)` returns empty List, not unset List

## EK9 Unset Poison Semantics

### **Rule for Primitive Types**
When one or both operands are unset:
- **Comparison operations** (`==`, `<>`, `<=>`) → return unset Boolean/Integer
- **No special handling** for unset-to-unset comparisons
- **Example**: `unsetRegex._eq(anotherUnsetRegex)` → returns unset Boolean

### **Pattern Application**
```java
// Correct unset handling
if (canProcess(arg)) {
    // Only process when both operands are set
    return Boolean._of(actualComparison);
}
return new Boolean(); // Return unset for invalid operations
```

## Development Guidelines

### **When Adding New Types**
1. **Choose appropriate base**: BuiltinType for primitives, delegation for collections
2. **Follow semantics**: Unset poison for primitives, always-set for collections  
3. **Test comprehensively**: Include unset operand scenarios
4. **Document expectations**: Clarify set/unset behavior in tests

### **When Working with Paths**
1. **Validate path format**: Some path constructors may reject invalid formats
2. **Handle unset gracefully**: Test for both valid and invalid path scenarios
3. **Use correct type**: Path for structured data, FileSystemPath for files
4. **Escape regex patterns**: Remember `$?` needs escaping in regex

### **Testing Best Practices**
1. **Test both types**: When supporting multiple types, test each separately
2. **Handle edge cases**: Invalid formats, unset inputs, empty results
3. **Follow semantics**: Collections check empty, primitives check unset
4. **Use conditional testing**: Adapt expectations based on actual object state

This distinction is crucial for correct EK9 type usage and testing.