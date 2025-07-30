# MCP-EK9 LSP Testing Guide

## 🎯 Purpose
This guide documents the complete process to test EK9 files using Model Context Protocol (MCP) integration with the EK9 compiler in Language Server Protocol (LSP) mode (`-ls` flag).

## 📁 Directory Structure

After following this guide, you'll have:

```
ek9/
├── mcp-server/                           # MCP integration directory
│   ├── .mcp.json                        # MCP configuration for Claude Code
│   ├── standalone-mcp-ek9.js            # Standalone MCP server (Node.js)
│   ├── README.md                        # MCP server documentation
│   ├── test/                            # MCP protocol tests
│   │   ├── test-mcp-validation.js
│   │   ├── test-claude-integration.md
│   │   └── test-valid-mcp.js
│   └── typescript-version/              # TypeScript MCP implementation
│       ├── README.md
│       ├── package.json
│       └── src/
├── compiler-main/
│   ├── target/
│   │   └── ek9c-jar-with-dependencies.jar  # EK9 compiler JAR
│   └── src/test/resources/claude/mcp-lsp/   # EK9 test files (Maven structure)
│       └── StringAndDateIsSet.ek9           # ⚠️  ALL MCP TEST FILES LOCATED HERE
└── (other project files...)
```

## 🔧 Prerequisites

### 1. Build EK9 Compiler
```bash
cd /path/to/ek9
mvn clean install -pl compiler-main
```

**Verify JAR exists:**
```bash
ls -la compiler-main/target/ek9c-jar-with-dependencies.jar
```

### 2. Enable Debug Mode in EK9 LSP Server
The EK9 LSP server should have debug enabled in `compiler-main/src/main/java/org/ek9lang/lsp/Ek9LanguageServer.java`:

```java
// Line 36: Enable debug for LSP diagnostics
Logger.enableDebug(true);

// Line 42: Use PRE_IR_CHECKS phase (not IR_ANALYSIS)
this.compilerConfig = new Ek9CompilerConfig(CompilationPhase.PRE_IR_CHECKS);
```

## 🏗️ Setup Process

### Step 1: Create MCP Server Directory Structure
```bash
cd /path/to/ek9
mkdir -p mcp-server/test
```

### Step 2: Configure MCP Server (.mcp.json)
Create `mcp-server/.mcp.json`:
```json
{
  "mcpServers": {
    "ek9-validator": {
      "command": "node",
      "args": ["./standalone-mcp-ek9.js"],
      "env": {
        "EK9_PROJECT_ROOT": "..",
        "EK9_AUTO_REBUILD": "true",
        "NODE_ENV": "production"
      }
    }
  }
}
```

### Step 3: Create Standalone MCP Server
Create `mcp-server/standalone-mcp-ek9.js` with the key features:

**Critical Configuration Points:**
```javascript
class StandaloneMcpEk9Server {
  constructor() {
    // CRITICAL: Use environment variable for project root
    this.projectRoot = process.env.EK9_PROJECT_ROOT || process.cwd();
    
    // CRITICAL: Use absolute path for JAR to avoid spawn issues
    this.jarPath = path.resolve(path.join(this.projectRoot, 'compiler-main/target/ek9c-jar-with-dependencies.jar'));
    
    this.messageId = 1;
  }
  
  async runEk9LspValidation(filePath) {
    return new Promise((resolve, reject) => {
      // CRITICAL: Use -ls flag for LSP mode (not -c for compilation)
      const process = spawn('java', ['-jar', this.jarPath, '-ls'], {
        cwd: this.projectRoot,
        stdio: ['pipe', 'pipe', 'pipe']
      });
      
      // LSP protocol implementation...
    });
  }
}
```

### Step 4: Create Test EK9 File

**📍 IMPORTANT: All EK9 files for MCP testing must be placed in:**
```
compiler-main/src/test/resources/claude/mcp-lsp/
```

Create `compiler-main/src/test/resources/claude/mcp-lsp/StringAndDateIsSet.ek9` with intentional syntax error:

```ek9
#!ek9
defines module net.customer.issettests

  defines function
    testDate()
      stdout <- Stdout()
      
      unsetValue = Date()    # ❌ ERROR: Should be <- not =
      stdout.println(`Unset Date isSet: ${unsetValue?}`)
      
      setValue <- 2024-01-15
      stdout.println(`Set Date isSet: ${setValue?}`)

  defines program
    ExampleOfUnsetAndSetBehavior()
      testDate()

//EOF
```

## 🧪 Testing Process

### Step 1: Verify EK9 JAR Status
```bash
cd mcp-server
export EK9_PROJECT_ROOT=..
echo '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"check_ek9_jar_status","arguments":{}},"id":1}' | node standalone-mcp-ek9.js
```

**Expected Output:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [{
      "type": "text",
      "text": "EK9 Compiler Status:\n{\n  \"jarExists\": true,\n  \"jarPath\": \"/absolute/path/to/ek9/compiler-main/target/ek9c-jar-with-dependencies.jar\",\n  \"projectRoot\": \"..\"\n}"
    }]
  }
}
```

### Step 2: Test EK9 File Validation Using LSP Mode
```bash
cd mcp-server
export EK9_PROJECT_ROOT=..
echo '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"validate_ek9_file","arguments":{"file_path":"compiler-main/src/test/resources/claude/mcp-lsp/StringAndDateIsSet.ek9"}},"id":1}' | node standalone-mcp-ek9.js
```

**Expected LSP Stderr Output (Debug Info):**
```
EK9     : EK9 running as LSP languageHelp=false
DEBUG: EK9: connect from client
EK9 Language Server Listening
DEBUG: EK9: initialize
DEBUG: didOpen Opened Source [/path/to/StringAndDateIsSet.ek9]
DEBUG: parsing/re-parsing [/path/to/StringAndDateIsSet.ek9] but with direct input stream
DEBUG: Reporting on file:///path/to/StringAndDateIsSet.ek9
Sending back diagnostics 0
DEBUG: didClose [DidCloseTextDocumentParams...]
DEBUG: EK9: Shutdown
DEBUG: EK9: Exit
```

**Key Success Indicators:**
- ✅ `EK9 running as LSP languageHelp=false` - Confirms LSP mode
- ✅ `EK9 Language Server Listening` - LSP server started
- ✅ `didOpen Opened Source` - File processed
- ✅ `parsing/re-parsing` - EK9 compiler processing file
- ✅ `Sending back diagnostics 0` - LSP protocol working (but 0 diagnostics found)

## 🔍 Known Status & Limitations

### ✅ Working Components
1. **MCP Protocol** - Full JSON-RPC 2.0 implementation
2. **LSP Integration** - Complete handshake and document lifecycle
3. **File Processing** - EK9 compiler processes files correctly
4. **Debug Capability** - Full stderr capture from EK9 LSP server
5. **Path Resolution** - Relative paths work correctly with environment variables
6. **Parse Error Detection** - EK9 LSP correctly detects and reports parsing errors via diagnostics

### ✅ Confirmed Working: Parse Error Detection
**Breakthrough**: EK9 LSP server now successfully generates diagnostics for parsing errors!

**Test Case**: Changed `#!ek9` to `#!ek 9` (added space)
**Result**: 
```json
{
  "range": {"start": {"line": 0, "character": 0}, "end": {"line": 0, "character": 1}},
  "severity": 1,
  "message": "mismatched input '#' expecting {'defines', '#!ek9', '@'}"
}
```

**Debug Evidence**: `Sending back diagnostics 1` (not 0!)

### ⚠️ Current Limitation
**Semantic Error Detection**: EK9 LSP only catches parse errors, not semantic errors from compiler front-end phases.

**Evidence**: Files with semantic errors like `unsetValue = Date()` (should be `<-`) don't generate diagnostics, suggesting only parsing occurs in LSP mode.

**Next Step Required**: Trigger EK9 compiler front-end phases (SYMBOL_DEFINITION, REFERENCE_CHECKS, etc.) in LSP mode to detect semantic errors beyond parsing.

## 🛠️ Available MCP Tools

### 1. `validate_ek9_file`
**Purpose**: Validate EK9 files from filesystem using LSP

**📍 File Location**: All test files are located in `compiler-main/src/test/resources/claude/mcp-lsp/`

**Usage**:
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "validate_ek9_file",
    "arguments": {
      "file_path": "compiler-main/src/test/resources/claude/mcp-lsp/StringAndDateIsSet.ek9"
    }
  },
  "id": 1
}
```

### 2. `validate_ek9_syntax`
**Purpose**: Validate EK9 code strings using LSP
**Usage**:
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "validate_ek9_syntax",
    "arguments": {
      "code": "#!ek9\ndefines module test\n  defines program\n    main()\n      stdout <- Stdout()\n//EOF"
    }
  },
  "id": 1
}
```

### 3. `check_ek9_jar_status`
**Purpose**: Verify EK9 compiler availability and configuration
**Usage**:
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "check_ek9_jar_status",
    "arguments": {}
  },
  "id": 1
}
```

## 🚀 Integration with Claude Code

### Configuration
Add to Claude Code MCP settings:
```json
{
  "mcpServers": {
    "ek9-validator": {
      "command": "node",
      "args": ["/absolute/path/to/ek9/mcp-server/standalone-mcp-ek9.js"],
      "env": {
        "EK9_PROJECT_ROOT": "/absolute/path/to/ek9",
        "EK9_AUTO_REBUILD": "true"
      }
    }
  }
}
```

### Usage in Claude Code
```
Please use the validate_ek9_file tool to check the syntax of StringAndDateIsSet.ek9
```

## 📋 Troubleshooting

### Issue: "EK9 JAR not found"
**Solution**: 
1. Verify `EK9_PROJECT_ROOT` environment variable is set correctly
2. Run `mvn clean install -pl compiler-main` to rebuild JAR
3. Check absolute path resolution in constructor

### Issue: "File not found"
**Solution**:
1. Verify file path is relative to `EK9_PROJECT_ROOT`
2. Check file exists at expected location
3. Ensure `.ek9` file extension

### Issue: "Unable to access jarfile"
**Solution**:
1. Use `path.resolve()` to create absolute JAR path
2. Verify JAR exists with `ls -la compiler-main/target/ek9c-jar-with-dependencies.jar`

## 🎯 Success Criteria

You've successfully reached the working MCP-LSP integration point when:

1. ✅ **JAR Status Check** returns `jarExists: true`
2. ✅ **LSP Mode Confirmed** in stderr: `EK9 running as LSP languageHelp=false`
3. ✅ **File Processing** shows `didOpen Opened Source [file-path]`
4. ✅ **LSP Protocol** completes with proper handshake and shutdown
5. ✅ **Debug Output** shows complete LSP message flow

The integration is working correctly at this point - the diagnostic generation issue is a separate EK9 compiler development task.

## 📁 File Locations Summary

- **MCP Configuration**: `mcp-server/.mcp.json`
- **MCP Server**: `mcp-server/standalone-mcp-ek9.js`
- **EK9 Compiler JAR**: `compiler-main/target/ek9c-jar-with-dependencies.jar`
- **📍 EK9 Test Files**: `compiler-main/src/test/resources/claude/mcp-lsp/` ⚠️ **ALL MCP TEST FILES HERE**
- **Documentation**: `mcp-server/README.md`

This guide provides a complete restoration path to the working MCP-EK9 LSP integration state.