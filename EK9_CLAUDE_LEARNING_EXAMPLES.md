# EK9 Claude Learning Examples

This document captures the learning experience and patterns discovered while creating EK9 examples with Claude Code, providing a foundation for creating comprehensive EK9 test examples and educational materials.

## Learning Context and Methodology

### **Test-Driven Learning Approach**
Using Steve's `ClaudeGeneratedBasicsTest` framework provides immediate feedback on EK9 compilation:
- **Test Location**: `compiler-main/src/test/java/org/ek9lang/compiler/main/ClaudeGeneratedBasicsTest.java`
- **Example Directory**: `compiler-main/src/test/resources/claude/basics/`
- **Command**: `mvn test -Dtest=ClaudeGeneratedBasicsTest -pl compiler-main`
- **Future Expansion**: Additional directories and test classes for different complexity levels

### **Error-Driven Learning Benefits**
The EK9 compiler provides excellent diagnostic information:
```
EK9Comp : Error   : 'stdout' on line 16 position 6: 'stdout': not resolved
```
- Clear line/position information
- Specific error descriptions
- Multi-phase compilation feedback (FULL_RESOLUTION, PRE_IR_CHECKS, etc.)

## Key EK9 Language Insights Discovered

### **1. Assignment Operator Semantics**
**CRITICAL DISTINCTION**: EK9 has different assignment operators with distinct semantics:

- **`<-`** - Variable declaration with initialization
  ```ek9
  stdout <- Stdout()  // Declares and initializes 'stdout'
  ```
- **`=` or `:=`** - Assignment to existing variable
  ```ek9
  stdout = Stdout()   // ERROR: if 'stdout' not previously declared
  ```

**Learning**: Variables must be declared before assignment. This is stricter than many languages.

### **2. EK9 Native Literals (Major Difference from Other Languages)**
EK9 provides native literal syntax for complex types:

**Date Literals**:
```ek9
setValue <- 2024-01-15  // Native date literal
```

**Other Native Literals** (from grammar analysis):
- **Time**: `10:30:00`
- **DateTime**: `2024-01-01T10:30:00Z`
- **Duration**: `P1Y2M3DT4H5M6S` (ISO 8601)
- **Money**: `100.50#USD`
- **Colour**: `#FF0000`
- **Dimension**: `10.5px`, `3.2em`
- **Resolution**: `300dpi`
- **Path**: `$?.some.path[0].array`
- **RegEx**: `/[a-z]+/`
- **Millisecond**: `1500ms`
- **Version**: `1.2.3-4`

**Significance**: Unlike other languages that use string constructors, EK9 parses these directly.

### **3. Indentation-Based Syntax Requirements**
**2-space indentation is mandatory** and semantically significant:
```ek9
defines module net.customer.issettests

  defines function

    testString()
      stdout <- Stdout()
      
      unsetValue <- String()
```

**Pattern**: Consistent 2-space indentation throughout, similar to Python but with EK9-specific block structures.

### **4. String Interpolation Syntax**
EK9 uses backticks with `${}` syntax:
```ek9
stdout.println(`Unset String isSet: ${unsetValue?}`)
```

**Not**: Standard double quotes with different interpolation syntax.

### **5. EK9 Documentation System**
EK9 provides a comprehensive documentation system similar to JavaDoc but with EK9-specific syntax:

**Documentation Comment Types**:
- **`<?- ... -?>`** - CODE_COMMENT for documentation (like JavaDoc)
- **`<!- ... -!>`** - BLOCK_COMMENT2 for regular comments  
- **`// ...`** - LINE_COMMENT for inline comments

**Documentation Placement**:
```ek9
#!ek9
<?-
  Module-level documentation here
  
  @author ClaudeCode
  @version 1.0
  @since EK9 0.0.1-0
-?>
defines module net.customer.example

  defines function

    <?-
      Function-level documentation here
    -?>
    testFunction()
      // Inline comments here
```

**Standard Documentation Tags**:
- `@author` - Author attribution
- `@version` - Version information
- `@since` - EK9 version compatibility
- Custom descriptive text and bullet points

**Key Benefits**:
- Documentation comments are processed by lexer as `CODE_COMMENT` tokens
- Don't interfere with compilation (properly skipped)
- Can document all major language constructs (modules, functions, programs)
- Maintains EK9's clean, consistent syntax philosophy

### **6. Tri-State Object Semantics**
EK9's unique object model distinguishes:
1. **Object Absent** - doesn't exist
2. **Object Present but Unset** - exists but no meaningful value
3. **Object Present and Set** - exists with valid value

**Testing Pattern**:
```ek9
unsetValue <- String()     // Creates unset but defined object
setValue <- String(`hello`) // Creates set object

stdout.println(`Unset: ${unsetValue?}`)  // false - not set
stdout.println(`Set: ${setValue?}`)      // true - is set
```

## Successful Example Patterns

### **Basic Type isSet Testing Pattern**
Template for testing any basic type's tri-state behavior:

```ek9
#!ek9
defines module net.customer.issettests

  defines function

    testTypeName()
      stdout <- Stdout()
      
      unsetValue <- TypeName()
      stdout.println(`Unset TypeName isSet: ${unsetValue?}`)
      
      setValue <- TypeName(validValue)  // or native literal
      stdout.println(`Set TypeName isSet: ${setValue?}`)

  defines program

    main()
      testTypeName()
```

### **File Structure Requirements**
- **Shebang**: `#!ek9` (first line)
- **Documentation**: `<?- ... -?>` after shebang, before module declaration
- **Module declaration**: `defines module` with dotted name
- **Block structure**: `defines function`, `defines program`
- **Function documentation**: `<?- ... -?>` before each function
- **EOF comment**: `//EOF` (following existing patterns)

### **Complete Documented Example Pattern**
```ek9
#!ek9
<?-
  Brief description of the module's purpose
  
  @author ClaudeCode
  @version 1.0
  @since EK9 0.0.1-0
-?>
defines module net.customer.example

  defines function

    <?-
      Function documentation explaining purpose
    -?>
    testFunction()
      // Implementation with inline comments

  defines program

    <?-
      Main program documentation
    -?>
    main()
      testFunction()

//EOF
```

## Analysis Methodology That Worked

### **1. Multi-Source Analysis**
- **Grammar files**: `EK9.g4` and `EK9LexerRules.g4` for syntax rules
- **Existing examples**: `JustString.ek9`, `JustDate.ek9`, `Basic.ek9` for patterns
- **Built-in definitions**: `Ek9BuiltinLangSupplier.java` for available types and operators
- **CLAUDE.md**: Architectural context and coding standards

### **2. Pattern Recognition Across Examples**
- Consistent indentation patterns
- String interpolation usage
- Variable declaration patterns
- Module structure conventions
- Comment styles and placement

### **3. Grammar-First Understanding**
Understanding the ANTLR grammar rules provided the foundation for:
- Literal syntax recognition
- Operator precedence and usage
- Block structure requirements
- Token definitions and lexical rules

## Future Development Strategy

### **Incremental Complexity Approach**
1. **Phase 1**: Basic types and operators (`claude/basics/`)
2. **Phase 2**: Control structures (`claude/control/`)
3. **Phase 3**: Object-oriented features (`claude/oop/`)
4. **Phase 4**: Generic types (`claude/generics/`)
5. **Phase 5**: Advanced features (`claude/advanced/`)

### **Test Framework Expansion**
Each phase should have:
- Dedicated test directory
- Corresponding Java test class
- Documented learning outcomes
- Error pattern examples (for compiler testing)

### **Documentation Benefits**
These examples serve multiple purposes:
1. **Claude learning reference** for future EK9 development
2. **EK9 developer education** showing idiomatic patterns
3. **Compiler testing** with both valid and invalid examples
4. **Language documentation** with working examples

## Lessons for Creating EK9 Examples

### **What Works Well**
1. **Start with grammar analysis** to understand available constructs
2. **Study multiple existing examples** for consistent patterns  
3. **Use the test framework** for immediate feedback
4. **Focus on EK9's unique features** (literals, tri-state, operators)
5. **Pay attention to indentation** - it's semantically critical
6. **Add comprehensive documentation** using EK9's `<?- -?>` syntax

### **Common Pitfalls to Avoid**
1. **Using wrong assignment operators** (`=` vs `<-`)
2. **Assuming string-based constructors** for types with native literals
3. **Incorrect indentation** breaking the parse
4. **Missing variable declarations** before assignment
5. **Ignoring tri-state semantics** when testing object states

## Integration with Existing Documentation

This document complements:
- **CLAUDE.md** - Overall project context and architecture
- **EK9_DEVELOPMENT_CONTEXT.md** - Built-in type development patterns
- **EK9_CODING_STANDARDS.md** - Code formatting and style requirements
- **EK9_COMPILER_ARCHITECTURE_AND_DESIGN.md** - Compiler phases and processing

## Next Steps

1. **Expand basic type coverage** - Integer, Float, Boolean, Character, etc.
2. **Create control structure examples** - if/else, loops, switch
3. **Develop error examples** for compiler testing
4. **Document additional patterns** as they're discovered
5. **Build comprehensive test suite** for EK9 educational materials