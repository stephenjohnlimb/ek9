#!/usr/bin/env node

/**
 * Standalone MCP-EK9 Server for EK9 Language Validation
 * No external dependencies - works with Node.js 12+
 */

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

class StandaloneMcpEk9Server {
  constructor() {
    this.projectRoot = process.env.EK9_PROJECT_ROOT || process.cwd();
    this.jarPath = path.resolve(path.join(this.projectRoot, 'compiler-main/target/ek9c-jar-with-dependencies.jar'));
    this.messageId = 1;
  }

  /**
   * Main MCP server loop
   */
  async run() {
    console.error('Standalone MCP-EK9 server started');
    
    process.stdin.setEncoding('utf8');
    let buffer = '';

    process.stdin.on('data', (chunk) => {
      buffer += chunk;
      this.processBuffer(buffer);
    });

    process.stdin.on('end', () => {
      console.error('MCP-EK9 server ending');
    });
  }

  /**
   * Process incoming MCP messages
   */
  processBuffer(buffer) {
    // Simple line-based processing for testing
    const lines = buffer.split('\n');
    
    for (const line of lines) {
      if (line.trim()) {
        try {
          const message = JSON.parse(line);
          this.handleMessage(message);
        } catch (error) {
          // Not a complete JSON message yet, continue
        }
      }
    }
  }

  /**
   * Handle MCP messages
   */
  async handleMessage(message) {
    console.error('Received message:', JSON.stringify(message, null, 2));

    if (message.method === 'tools/list') {
      this.sendResponse(message.id, {
        tools: [
          {
            name: 'validate_ek9_syntax',
            description: 'Validate EK9 source code using the EK9 compiler',
            input_schema: {
              type: 'object',
              properties: {
                code: { type: 'string', description: 'EK9 source code to validate' },
                filename: { type: 'string', description: 'Optional filename', default: 'temp.ek9' }
              },
              required: ['code']
            }
          },
          {
            name: 'validate_ek9_file',
            description: 'Validate EK9 file directly from filesystem',
            input_schema: {
              type: 'object',
              properties: {
                file_path: { 
                  type: 'string', 
                  description: 'Path to EK9 file to validate (relative to project root or absolute)' 
                }
              },
              required: ['file_path']
            }
          },
          {
            name: 'check_ek9_jar_status',
            description: 'Check EK9 compiler JAR availability',
            input_schema: { type: 'object', properties: {} }
          }
        ]
      });
    } else if (message.method === 'tools/call') {
      await this.handleToolCall(message);
    }
  }

  /**
   * Handle tool calls
   */
  async handleToolCall(message) {
    const { name, arguments: args } = message.params;

    try {
      let result;
      switch (name) {
        case 'validate_ek9_syntax':
          result = await this.validateEk9Syntax(args);
          break;
        case 'validate_ek9_file':
          result = await this.validateEk9File(args);
          break;
        case 'check_ek9_jar_status':
          result = await this.checkJarStatus();
          break;
        default:
          throw new Error(`Unknown tool: ${name}`);
      }

      this.sendResponse(message.id, {
        content: [{ type: 'text', text: result }]
      });
    } catch (error) {
      this.sendResponse(message.id, {
        content: [{ type: 'text', text: `Error: ${error.message}` }],
        isError: true
      });
    }
  }

  /**
   * Validate EK9 syntax by creating temporary file and compiling
   */
  async validateEk9Syntax(args) {
    const { code, filename = 'temp.ek9' } = args;

    // Make program and function names unique to avoid conflicts
    const uniqueCode = this.makeNamesUnique(code, filename);

    if (!fs.existsSync(this.jarPath)) {
      return `❌ EK9 JAR not found at: ${this.jarPath}\nPlease run: mvn clean install`;
    }

    // Create temporary file in mcp-lsp directory as required
    const tempDir = path.join(this.projectRoot, 'compiler-main/src/test/resources/claude/mcp-lsp');
    const timestamp = Date.now();
    const tempFile = path.join(tempDir, `McpValidation_${timestamp}.ek9`);

    try {
      // Ensure directory exists
      if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir, { recursive: true });
      }

      // Write EK9 code to temporary file (with unique names)
      fs.writeFileSync(tempFile, uniqueCode);

      // Run EK9 compiler in LSP mode for validation
      const result = await this.runEk9LspValidation(tempFile);
      
      // Clean up
      if (fs.existsSync(tempFile)) {
        fs.unlinkSync(tempFile);
      }

      return this.formatValidationResult(result, code);
    } catch (error) {
      // Clean up on error
      if (fs.existsSync(tempFile)) {
        fs.unlinkSync(tempFile);
      }
      throw error;
    }
  }

  /**
   * Run EK9 compiler in LSP mode to validate EK9 code
   */
  async runEk9LspValidation(filePath) {
    return new Promise((resolve, reject) => {
      // Use EK9 compiler in LSP mode with -ls flag
      const process = spawn('java', ['-jar', this.jarPath, '-ls'], {
        cwd: this.projectRoot,
        stdio: ['pipe', 'pipe', 'pipe']
      });

      let stdout = '';
      let stderr = '';
      let diagnostics = [];
      let messageBuffer = '';
      let initialized = false;

      const sendLspMessage = (message) => {
        const content = JSON.stringify(message);
        const header = `Content-Length: ${content.length}\r\n\r\n`;
        process.stdin.write(header + content);
      };

      process.stdout.on('data', (data) => {
        stdout += data.toString();
        messageBuffer += data.toString();
        
        // Process complete LSP messages
        this.processLspMessages(messageBuffer, (message) => {
          if (message.id === 1 && message.result && !initialized) {
            // Initialization response received, send initialized notification
            initialized = true;
            sendLspMessage({
              jsonrpc: '2.0',
              method: 'initialized',
              params: {}
            });

            // Open document for validation
            const fileContent = fs.readFileSync(filePath, 'utf8');
            const fileUri = `file://${path.resolve(filePath)}`;
            
            sendLspMessage({
              jsonrpc: '2.0',
              method: 'textDocument/didOpen',
              params: {
                textDocument: {
                  uri: fileUri,
                  languageId: 'ek9',
                  version: 1,
                  text: fileContent
                }
              }
            });

            // Wait for diagnostics, then close document and shutdown
            setTimeout(() => {
              sendLspMessage({
                jsonrpc: '2.0',
                method: 'textDocument/didClose',
                params: {
                  textDocument: { uri: fileUri }
                }
              });

              sendLspMessage({
                jsonrpc: '2.0',
                id: 2,
                method: 'shutdown',
                params: {}
              });

              sendLspMessage({
                jsonrpc: '2.0',
                method: 'exit',
                params: {}
              });
            }, 10000);
          } else if (message.method === 'textDocument/publishDiagnostics') {
            diagnostics = diagnostics.concat(message.params.diagnostics || []);
          }
        });
      });

      process.stderr.on('data', (data) => {
        stderr += data.toString();
      });

      process.on('close', (code) => {
        resolve({
          code,
          stdout,
          stderr,
          diagnostics,
          success: diagnostics.length === 0,
          rawOutput: stdout  // Include raw LSP output for debugging
        });
      });

      process.on('error', (error) => {
        reject(error);
      });

      // Start LSP handshake
      sendLspMessage({
        jsonrpc: '2.0',
        id: 1,
        method: 'initialize',
        params: {
          processId: process.pid,
          rootUri: `file://${this.projectRoot}`,
          capabilities: {
            textDocument: {
              synchronization: {
                dynamicRegistration: false,
                willSave: false,
                willSaveWaitUntil: false,
                didSave: false
              }
            }
          }
        }
      });
    });
  }

  /**
   * Process LSP messages from buffer and call handler for complete messages
   */
  processLspMessages(buffer, messageHandler) {
    while (true) {
      const headerMatch = buffer.match(/Content-Length: (\d+)\r?\n\r?\n/);
      if (!headerMatch) break;
      
      const contentLength = parseInt(headerMatch[1]);
      const headerEnd = headerMatch.index + headerMatch[0].length;
      
      if (buffer.length < headerEnd + contentLength) break; // Incomplete message
      
      const messageContent = buffer.substring(headerEnd, headerEnd + contentLength);
      buffer = buffer.substring(headerEnd + contentLength);
      
      try {
        const message = JSON.parse(messageContent);
        messageHandler(message);
      } catch (e) {
        console.error('Failed to parse LSP message:', e);
      }
    }
  }

  /**
   * Format validation results
   */
  formatValidationResult(result, code) {
    const lines = code.split('\n');
    
    // Always include raw LSP output and stderr for debugging
    let output = `📋 **LSP Raw Output:**\n${result.rawOutput}\n\n`;
    output += `🚨 **LSP Stderr:**\n${result.stderr}\n\n`;
    output += `📊 **Diagnostics Count:** ${result.diagnostics.length}\n\n`;
    
    if (result.success) {
      return output + '✅ EK9 code validation successful - no errors found!';
    }

    // Use LSP diagnostics if available, fallback to stderr parsing
    const errors = result.diagnostics && result.diagnostics.length > 0 
      ? this.formatLspDiagnostics(result.diagnostics)
      : this.parseEk9Errors(result.stderr);
    
    if (errors.length === 0) {
      return output + `❌ EK9 validation failed but no specific errors found.\nStderr: ${result.stderr}`;
    }

    output += `❌ EK9 validation found ${errors.length} error(s):\n\n`;

    errors.forEach((error, index) => {
      output += `${index + 1}. 🔴 ERROR at line ${error.line}, column ${error.column}:\n`;
      output += `   ${error.message}\n`;
      
      // Show the problematic line
      if (lines[error.line - 1]) {
        output += `   Code: ${lines[error.line - 1].trim()}\n`;
        if (error.column > 0) {
          const pointer = ' '.repeat(error.column - 1) + '^';
          output += `         ${pointer}\n`;
        }
      }
      output += '\n';
    });

    // Add EK9-specific suggestions
    output += this.getEk9Suggestions(errors);
    
    return output;
  }

  /**
   * Format LSP diagnostics into error objects
   */
  formatLspDiagnostics(diagnostics) {
    return diagnostics.map(diagnostic => ({
      line: diagnostic.range.start.line + 1, // LSP is 0-based, convert to 1-based
      column: diagnostic.range.start.character + 1,
      message: diagnostic.message,
      severity: diagnostic.severity || 1 // 1 = Error
    }));
  }

  /**
   * Parse EK9 compiler errors from stderr
   */
  parseEk9Errors(stderr) {
    const errors = [];
    const errorRegex = /'([^']+)' on line (\d+) position (\d+): (.+)/g;
    
    let match;
    while ((match = errorRegex.exec(stderr)) !== null) {
      errors.push({
        variable: match[1],
        line: parseInt(match[2]),
        column: parseInt(match[3]),
        message: match[4]
      });
    }
    
    return errors;
  }

  /**
   * Generate EK9-specific suggestions
   */
  getEk9Suggestions(errors) {
    let suggestions = 'EK9-specific suggestions:\n';
    
    const hasNotResolved = errors.some(e => e.message.includes('not resolved'));
    const hasAssignment = errors.some(e => e.message.includes('assignment'));
    
    if (hasNotResolved) {
      suggestions += '• Check variable declarations use `<-` operator (not `=`)\n';
      suggestions += '• Ensure proper 2-space indentation\n';
    }
    
    if (hasAssignment) {
      suggestions += '• Use `<-` for declaration with initialization\n';
      suggestions += '• Use `=` or `:=` for assignment to existing variables\n';
    }
    
    suggestions += '• Verify EK9 syntax: native literals, string interpolation, etc.\n';
    
    return suggestions + '\n';
  }

  /**
   * Validate EK9 file directly from filesystem
   */
  async validateEk9File(args) {
    const { file_path } = args;

    if (!file_path) {
      throw new Error('file_path parameter is required');
    }

    // Resolve file path (handle both relative and absolute paths)
    let resolvedPath;
    if (path.isAbsolute(file_path)) {
      resolvedPath = file_path;
    } else {
      resolvedPath = path.join(this.projectRoot, file_path);
    }

    // Check if file exists
    if (!fs.existsSync(resolvedPath)) {
      return `❌ File not found: ${resolvedPath}`;
    }

    // Check if it's an .ek9 file
    if (!resolvedPath.endsWith('.ek9')) {
      return `❌ File must have .ek9 extension: ${resolvedPath}`;
    }

    if (!fs.existsSync(this.jarPath)) {
      return `❌ EK9 JAR not found at: ${this.jarPath}\nPlease run: mvn clean install`;
    }

    try {
      // Validate the actual file directly without modification
      const result = await this.runEk9LspValidation(resolvedPath);
      
      // Read file content for result formatting
      const code = fs.readFileSync(resolvedPath, 'utf8');
      return this.formatValidationResult(result, code);
    } catch (error) {
      throw new Error(`Failed to validate file ${resolvedPath}: ${error.message}`);
    }
  }

  /**
   * Check JAR status
   */
  async checkJarStatus() {
    const exists = fs.existsSync(this.jarPath);
    
    const status = {
      jarExists: exists,
      jarPath: this.jarPath,
      projectRoot: this.projectRoot
    };
    
    return `EK9 Compiler Status:\n${JSON.stringify(status, null, 2)}`;
  }

  /**
   * Make program and function names unique to avoid conflicts with existing test files
   */
  makeNamesUnique(code, filename) {
    const timestamp = Date.now();
    const baseName = filename.replace('.ek9', '').replace(/[^a-zA-Z0-9]/g, '');
    const uniqueSuffix = `_${baseName}_${timestamp}`;

    // Replace program names to make them unique (programs are defined at 4-space indent)
    let uniqueCode = code.replace(
      /^(    )([a-zA-Z][a-zA-Z0-9]*)\(\)(\s*)$/gm, 
      `$1$2${uniqueSuffix}()$3`
    );

    // Replace function names to make them unique (functions are defined at 4-space indent)
    uniqueCode = uniqueCode.replace(
      /^(    )([a-zA-Z][a-zA-Z0-9]*)\(\)(\s*)$/gm,
      `$1$2${uniqueSuffix}()$3`
    );

    // Update function calls to match renamed functions (calls are at 6+ space indent)
    uniqueCode = uniqueCode.replace(
      /^(      +)([a-zA-Z][a-zA-Z0-9]*)\(\)/gm,
      `$1$2${uniqueSuffix}()`
    );

    return uniqueCode;
  }

  /**
   * Send MCP response
   */
  sendResponse(id, result) {
    const response = {
      jsonrpc: '2.0',
      id,
      result
    };
    
    console.log(JSON.stringify(response));
  }
}

// Start server if run directly
if (require.main === module) {
  const server = new StandaloneMcpEk9Server();
  server.run().catch(error => {
    console.error('Server error:', error);
    process.exit(1);
  });
}

module.exports = StandaloneMcpEk9Server;