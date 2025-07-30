# EK9 MCP Integration - Quick Reference

## 🚀 **Instant EK9 Validation Commands**

### **Validate EK9 File from Filesystem**
Ask Claude Code:
> "Please validate the EK9 file at ./compiler-main/src/test/resources/claude/basics/StringAndDateIsSet.ek9 using the validate_ek9_file tool"

### **Validate EK9 Code (Direct)**
Ask Claude Code:
> "Please validate this EK9 code using the validate_ek9_syntax tool: [paste your EK9 code here]"

### **Check EK9 Compiler Status**
Ask Claude Code:
> "Please check the EK9 compiler status using the check_ek9_jar_status tool"

## 📋 **Common EK9 Validation Scenarios**

### **✅ Test Valid EK9 Syntax**
```ek9
#!ek9
defines module test.example

  defines function
    
    simpleTest()
      stdout <- Stdout()
      value <- String("Hello EK9!")
      stdout.println(`Message: ${value}`)

  defines program
    
    main()
      simpleTest()

//EOF
```

### **❌ Test Error Detection**
```ek9
#!ek9
defines module test.errors

  defines function
    
    errorTest()
      stdout <- Stdout()
      badVar = String("Error here!")  // Should use <- not =
      stdout.println(badVar)

  defines program
    
    main()
      errorTest()

//EOF
```

## 🎯 **Expected Results**

### **Valid Code ✅**
```
✅ EK9 code validation successful - no errors found!
```

### **Invalid Code ❌**
```
❌ EK9 validation found 2 error(s):

1. 🔴 ERROR at line 8, column 6:
   'badVar': not resolved
   Code: badVar = String("Error here!")
              ^

EK9-specific suggestions:
• Check variable declarations use `<-` operator (not `=`)
• Ensure proper 2-space indentation
```

## 🔧 **Integration Features**

- **File-based validation** - No need to paste code, just specify file paths
- **Real-time validation** using actual EK9 compiler
- **Precise error locations** with line/column numbers
- **EK9-specific suggestions** for common issues
- **Automatic JAR management** with rebuild capability
- **Clean error formatting** with code snippets
- **Unique naming** to avoid conflicts in test directories

## 💡 **Pro Tips**

1. **Assignment Operators**: Always use `<-` for variable declaration
2. **Indentation**: EK9 requires exactly 2 spaces for semantic indentation
3. **String Interpolation**: Use backtick syntax: `` `Hello ${variable}` ``
4. **Native Literals**: Use EK9's native date/time literals: `2024-01-15`
5. **Tri-State Semantics**: Test with `variable?` to check if set

## 🚀 **Ready to Use!**

The MCP-EK9 integration is now active in this project. Just ask Claude Code to validate any EK9 file by path or code snippet and get instant, professional feedback!

### **Available MCP Tools:**
- **`validate_ek9_file`** - Validate EK9 files directly from filesystem
- **`validate_ek9_syntax`** - Validate EK9 code snippets  
- **`check_ek9_jar_status`** - Check EK9 compiler availability

### **Example File Validation:**
> "Use the validate_ek9_file tool to validate ./compiler-main/src/test/resources/claude/basics/ValidationTestBad.ek9"