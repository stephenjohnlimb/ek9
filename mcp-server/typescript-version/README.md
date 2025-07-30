# MCP-EK9 Server

Model Context Protocol server for EK9 language validation using the EK9 compiler's Language Server Protocol (LSP) implementation.

## Overview

This MCP server provides real-time EK9 code validation by interfacing with the EK9 compiler's built-in LSP server. It enables Claude Code and other MCP clients to validate EK9 syntax, semantics, and provide intelligent feedback on EK9 code.

## Features

- **Real-time EK9 validation** - Syntax and semantic analysis using the actual EK9 compiler
- **Robust JAR management** - Automatic discovery and optional rebuilding of EK9 compiler JAR
- **LSP integration** - Full Language Server Protocol communication with EK9 compiler
- **Intelligent diagnostics** - Detailed error reporting with line/column information and suggestions
- **Development-friendly** - Handles missing JARs gracefully during active development

## Installation

```bash
cd mcp-lsp-ek9-server
npm install
npm run build
```

## Usage

### Available MCP Tools

1. **`validate_ek9_syntax`** - Validate EK9 source code
   - Input: `code` (string) - EK9 source code to validate
   - Input: `filename` (optional string) - Context filename
   - Returns: Validation results with diagnostics and suggestions

2. **`check_ek9_jar_status`** - Check EK9 compiler availability
   - Returns: JAR status, LSP server status, configuration info

3. **`start_ek9_lsp`** - Start the EK9 Language Server
   - Input: `autoRebuild` (optional boolean) - Auto-rebuild JAR if missing
   - Returns: Start status

4. **`stop_ek9_lsp`** - Stop the EK9 Language Server
   - Returns: Stop status

### Configuration

Environment variables:
- `EK9_PROJECT_ROOT` - Path to EK9 project root (defaults to current directory)
- `EK9_AUTO_REBUILD` - Set to 'true' to enable automatic JAR rebuilding

### Claude Code Integration

Add to your Claude Code MCP settings:

```json
{
  "mcpServers": {
    "ek9-validator": {
      "command": "node",
      "args": ["/path/to/ek9/mcp-lsp-ek9-server/dist/index.js"],
      "env": {
        "EK9_PROJECT_ROOT": "/path/to/ek9",
        "EK9_AUTO_REBUILD": "true"
      }
    }
  }
}
```

## Architecture

### Components

1. **Ek9JarManager** - Handles EK9 compiler JAR discovery and Maven rebuilding
2. **Ek9LspClient** - LSP client for communicating with EK9 compiler
3. **Ek9McpServer** - Main MCP server providing validation tools

### Process Flow

1. MCP client requests EK9 validation
2. JAR manager ensures EK9 compiler is available (rebuilds if needed)
3. LSP client starts EK9 compiler in Language Server mode
4. EK9 code is sent to LSP server for validation
5. Diagnostics are returned with detailed error information
6. Results are formatted with EK9-specific suggestions

## EK9-Specific Features

### Tri-State Semantics Validation
- Validates EK9's unique object states (absent/unset/set)
- Checks proper use of `?` (isSet) operator

### Assignment Operator Validation
- Distinguishes between `<-` (declaration) and `=` (assignment)
- Validates proper variable declaration patterns

### Native Literal Support
- Validates EK9's native literals (dates, times, money, etc.)
- Checks string interpolation syntax

### Indentation Validation
- Enforces EK9's mandatory 2-space semantic indentation
- Validates block structure and nesting

## Development

### Building
```bash
npm run build
```

### Development Mode
```bash
npm run dev  # Watch mode with TypeScript compilation
```

### Testing
Test with sample EK9 code:

```bash
echo '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"validate_ek9_syntax","arguments":{"code":"#!ek9\ndefines module test\n  defines program\n    main()\n      stdout <- Stdout()\n      stdout.println(\"Hello EK9\")\n//EOF"}},"id":1}' | node dist/index.js
```

## Error Handling

The server provides graceful error handling for common development scenarios:

- **Missing JAR**: Clear error message with Maven rebuild instructions
- **LSP communication failures**: Automatic retry and fallback mechanisms  
- **Invalid EK9 code**: Detailed diagnostics with suggestions
- **Build failures**: Comprehensive Maven error reporting

## Current Status

### ✅ Working Features
- **Complete LSP Integration** - Full handshake, document lifecycle, and proper message parsing
- **File Validation** - Direct processing of actual source files (no temporary copies)
- **Debug Capability** - Complete stderr capture from EK9 LSP server
- **MCP Protocol** - All three tools (`validate_ek9_syntax`, `validate_ek9_file`, `check_ek9_jar_status`) functional

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

**Analysis**: LSP correctly processes files but doesn't convert compilation errors to LSP diagnostics, indicating either incomplete compilation in LSP mode or broken error mapping.

## Dependencies

- `@modelcontextprotocol/sdk` - MCP protocol implementation
- `node-json-rpc` - JSON-RPC communication
- `ws` - WebSocket support
- Java runtime (for EK9 compiler execution)
- Maven (for JAR rebuilding)

## License

MIT License - See LICENSE file for details