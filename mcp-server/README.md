# MCP-EK9 Server Configuration

Model Context Protocol server integration for EK9 language validation using the EK9 compiler's Language Server Protocol (LSP).

## 🏗️ Directory Structure

```
mcp-server/
├── .mcp.json                    # Main MCP configuration for Claude Code
├── standalone-mcp-ek9.js        # Standalone Node.js MCP server
├── README.md                    # This file
├── test/                        # MCP protocol tests and integration tests
│   ├── test-mcp-validation.js   # MCP server validation tests
│   ├── test-claude-integration.md
│   └── test-valid-mcp.js
└── typescript-version/          # TypeScript implementation (alternative)
    ├── README.md
    ├── package.json
    ├── tsconfig.json
    └── src/

# EK9 test files remain in proper Maven structure:
../compiler-main/src/test/resources/claude/mcp-lsp/
└── StringAndDateIsSet.ek9       # EK9 source files for LSP testing
```

## 🚀 Usage

### For Claude Code Integration

1. **Configure Claude Code** to use this MCP configuration:
   ```bash
   # Point Claude Code to use this MCP configuration
   claude-code --mcp-config /path/to/ek9/mcp-server/.mcp.json
   ```

2. **Or set in Claude Code settings** (preferred):
   - Add `mcp-server/.mcp.json` as your MCP configuration file
   - Relative paths will work from any location

### Available MCP Tools

1. **`validate_ek9_syntax`** - Validate EK9 source code
2. **`validate_ek9_file`** - Validate EK9 files from filesystem
3. **`check_ek9_jar_status`** - Check EK9 compiler availability

## 🔧 Configuration Details

### `.mcp.json` Configuration
```json
{
  "mcpServers": {
    "ek9-validator": {
      "command": "node",
      "args": ["./standalone-mcp-ek9.js"],
      "env": {
        "EK9_PROJECT_ROOT": "..",           # Points to project root
        "EK9_AUTO_REBUILD": "true",         # Auto-rebuild JAR if missing
        "NODE_ENV": "production"
      }
    }
  }
}
```

### Path Structure
- **MCP Server**: Runs from `mcp-server/` directory
- **Project Root**: `EK9_PROJECT_ROOT` points to `..` (parent directory)
- **EK9 JAR**: Located at `../compiler-main/target/ek9c-jar-with-dependencies.jar`
- **EK9 Test Files**: Located at `../compiler-main/src/test/resources/claude/mcp-lsp/`

## 🔍 Current Status

### ✅ Working Features
- **Complete LSP Integration** - Full handshake, document lifecycle, and proper message parsing
- **File Validation** - Direct processing of actual source files (no temporary copies)
- **Debug Capability** - Complete stderr capture from EK9 LSP server
- **MCP Protocol** - All three tools functional with proper error handling

### ⚠️ Known Limitations
- **Diagnostic Generation** - EK9 LSP server processes files but generates 0 diagnostics for syntax errors
- **Error Detection** - Known syntax errors (e.g., `=` vs `<-` assignment operators) not reported
- **Compilation Phases** - May not be running full compilation or error-to-diagnostic mapping is defective

### Debug Evidence
```
DEBUG: didOpen Opened Source [file-path]
DEBUG: parsing/re-parsing [file-path] but with direct input stream  
DEBUG: Reporting on file://file-path
Sending back diagnostics 0
```

**Analysis**: LSP correctly processes files but doesn't convert compilation errors to LSP diagnostics.

## 🏃 Quick Start

1. **Ensure EK9 JAR exists**:
   ```bash
   cd .. && mvn clean install -pl compiler-main
   ```

2. **Test MCP server**:
   ```bash
   cd mcp-server
   echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | node standalone-mcp-ek9.js
   ```

3. **Configure Claude Code** to use `mcp-server/.mcp.json`

## 📁 File Organization Benefits

- **Clean Root Directory** - No MCP files cluttering the project root
- **Maven Compliance** - EK9 test files remain in proper Maven test resources
- **Portable Configuration** - Relative paths work on any machine/user
- **Clear Separation** - MCP infrastructure separate from EK9 compiler code
- **Easy Maintenance** - All MCP components organized together

## 🔧 Development

- **Standalone Version**: `standalone-mcp-ek9.js` (production-ready, no dependencies)
- **TypeScript Version**: `typescript-version/` (development version with full tooling)
- **Test Files**: `test/` (MCP protocol and integration tests)
- **EK9 Test Sources**: `../compiler-main/src/test/resources/claude/mcp-lsp/` (Maven structure)