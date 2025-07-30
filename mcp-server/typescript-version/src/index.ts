#!/usr/bin/env node

import { Server } from '@modelcontextprotocol/sdk/server/index';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool,
} from '@modelcontextprotocol/sdk/types';
import { Ek9LspClient, EK9Diagnostic } from './ek9-lsp-client';

/**
 * MCP Server for EK9 Language Validation
 * Provides real-time EK9 code validation using the EK9 compiler's LSP server
 */
class Ek9McpServer {
  private server: Server;
  private lspClient: Ek9LspClient;
  private autoRebuild: boolean;

  constructor() {
    this.server = new Server(
      { name: 'mcp-ek9-server', version: '1.0.0' },
      { capabilities: { tools: {} } }
    );

    // Get configuration from environment variables
    this.autoRebuild = process.env.EK9_AUTO_REBUILD === 'true';
    const projectRoot = process.env.EK9_PROJECT_ROOT;

    this.lspClient = new Ek9LspClient(projectRoot);
    
    this.setupToolHandlers();
    this.setupErrorHandlers();
  }

  /**
   * Set up MCP tool handlers
   */
  private setupToolHandlers(): void {
    // List available tools
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      return {
        tools: [
          {
            name: 'validate_ek9_syntax',
            description: 'Validate EK9 source code syntax and semantics using the EK9 compiler',
            inputSchema: {
              type: 'object',
              properties: {
                code: {
                  type: 'string',
                  description: 'EK9 source code to validate'
                },
                filename: {
                  type: 'string',
                  description: 'Optional filename for context (defaults to temp.ek9)',
                  default: 'temp.ek9'
                }
              },
              required: ['code']
            }
          },
          {
            name: 'check_ek9_jar_status',
            description: 'Check if the EK9 compiler JAR is available and get build status',
            inputSchema: {
              type: 'object',
              properties: {},
              additionalProperties: false
            }
          },
          {
            name: 'start_ek9_lsp',
            description: 'Start the EK9 Language Server (if not already running)',
            inputSchema: {
              type: 'object',
              properties: {
                autoRebuild: {
                  type: 'boolean',
                  description: 'Whether to automatically rebuild JAR if missing',
                  default: false
                }
              },
              additionalProperties: false
            }
          },
          {
            name: 'stop_ek9_lsp',
            description: 'Stop the EK9 Language Server',
            inputSchema: {
              type: 'object',
              properties: {},
              additionalProperties: false
            }
          }
        ] as Tool[]
      };
    });

    // Handle tool calls
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      try {
        switch (request.params.name) {
          case 'validate_ek9_syntax':
            return await this.handleValidateEk9Syntax(request.params.arguments);

          case 'check_ek9_jar_status':
            return await this.handleCheckJarStatus();

          case 'start_ek9_lsp':
            return await this.handleStartLsp(request.params.arguments);

          case 'stop_ek9_lsp':
            return await this.handleStopLsp();

          default:
            throw new Error(`Unknown tool: ${request.params.name}`);
        }
      } catch (error) {
        return {
          content: [{
            type: 'text',
            text: `Error: ${error instanceof Error ? error.message : String(error)}`
          }],
          isError: true
        };
      }
    });
  }

  /**
   * Handle EK9 syntax validation
   */
  private async handleValidateEk9Syntax(args: any): Promise<any> {
    const { code, filename = 'temp.ek9' } = args;

    if (!code || typeof code !== 'string') {
      throw new Error('Code parameter is required and must be a string');
    }

    // Ensure LSP client is running
    if (!this.lspClient.isReady()) {
      console.log('Starting EK9 LSP server for validation...');
      await this.lspClient.start(this.autoRebuild);
    }

    try {
      const uri = `file:///${filename}`;
      const diagnostics = await this.lspClient.validateEk9Code(code, uri);

      const result = this.formatValidationResult(diagnostics, code);
      
      return {
        content: [{
          type: 'text',
          text: result
        }]
      };
    } catch (error) {
      throw new Error(`EK9 validation failed: ${error}`);
    }
  }

  /**
   * Handle JAR status check
   */
  private async handleCheckJarStatus(): Promise<any> {
    const jarStatus = this.lspClient.getJarStatus();
    const lspReady = this.lspClient.isReady();

    const status = {
      jarExists: jarStatus.exists,
      jarPath: jarStatus.path,
      lspServerRunning: lspReady,
      autoRebuildEnabled: this.autoRebuild
    };

    return {
      content: [{
        type: 'text',
        text: `EK9 Compiler Status:\n${JSON.stringify(status, null, 2)}`
      }]
    };
  }

  /**
   * Handle LSP server start
   */
  private async handleStartLsp(args: any): Promise<any> {
    const { autoRebuild = this.autoRebuild } = args;

    if (this.lspClient.isReady()) {
      return {
        content: [{
          type: 'text',
          text: 'EK9 LSP server is already running'
        }]
      };
    }

    try {
      await this.lspClient.start(autoRebuild);
      return {
        content: [{
          type: 'text',
          text: 'EK9 LSP server started successfully'
        }]
      };
    } catch (error) {
      throw new Error(`Failed to start EK9 LSP server: ${error}`);
    }
  }

  /**
   * Handle LSP server stop
   */
  private async handleStopLsp(): Promise<any> {
    if (!this.lspClient.isReady()) {
      return {
        content: [{
          type: 'text',
          text: 'EK9 LSP server is not running'
        }]
      };
    }

    try {
      await this.lspClient.stop();
      return {
        content: [{
          type: 'text',
          text: 'EK9 LSP server stopped successfully'
        }]
      };
    } catch (error) {
      throw new Error(`Failed to stop EK9 LSP server: ${error}`);
    }
  }

  /**
   * Format validation results into human-readable text
   */
  private formatValidationResult(diagnostics: EK9Diagnostic[], code: string): string {
    if (diagnostics.length === 0) {
      return '✅ EK9 code validation successful - no errors or warnings found';
    }

    const lines = code.split('\n');
    let result = `❌ EK9 validation found ${diagnostics.length} issue(s):\n\n`;

    diagnostics.forEach((diagnostic, index) => {
      const severity = this.getDiagnosticSeverity(diagnostic.severity);
      const line = diagnostic.range.start.line + 1; // LSP is 0-based, display is 1-based
      const char = diagnostic.range.start.character + 1;
      
      result += `${index + 1}. ${severity} at line ${line}, column ${char}:\n`;
      result += `   ${diagnostic.message}\n`;
      
      // Show the problematic line if available
      if (lines[diagnostic.range.start.line]) {
        result += `   Code: ${lines[diagnostic.range.start.line].trim()}\n`;
        
        // Add pointer to the specific character
        const pointer = ' '.repeat(char - 1) + '^';
        result += `         ${pointer}\n`;
      }
      
      result += '\n';
    });

    // Add suggestions for common EK9 issues
    result += this.getEk9Suggestions(diagnostics);

    return result;
  }

  /**
   * Get human-readable diagnostic severity
   */
  private getDiagnosticSeverity(severity: number): string {
    switch (severity) {
      case 1: return '🔴 ERROR';
      case 2: return '🟡 WARNING';
      case 3: return '🔵 INFO';
      case 4: return '💡 HINT';
      default: return '❓ UNKNOWN';
    }
  }

  /**
   * Generate EK9-specific suggestions based on common errors
   */
  private getEk9Suggestions(diagnostics: EK9Diagnostic[]): string {
    const messages = diagnostics.map(d => d.message.toLowerCase());
    let suggestions = 'EK9-specific suggestions:\n';

    if (messages.some(msg => msg.includes('not resolved'))) {
      suggestions += '• Check variable declarations use `<-` operator (not `=`)\n';
      suggestions += '• Ensure proper 2-space indentation\n';
    }

    if (messages.some(msg => msg.includes('assignment'))) {
      suggestions += '• Use `<-` for declaration with initialization\n';
      suggestions += '• Use `=` or `:=` for assignment to existing variables\n';
    }

    if (messages.some(msg => msg.includes('literal'))) {
      suggestions += '• Use EK9 native literals: dates (2024-01-15), times (10:30:00), etc.\n';
      suggestions += '• Check string interpolation syntax: `Hello ${name}`\n';
    }

    return suggestions + '\n';
  }

  /**
   * Set up error handlers
   */
  private setupErrorHandlers(): void {
    this.server.onerror = (error) => {
      console.error('[MCP Error]', error);
    };

    process.on('SIGINT', async () => {
      console.log('Shutting down MCP-EK9 server...');
      await this.lspClient.stop();
      process.exit(0);
    });

    process.on('SIGTERM', async () => {
      console.log('Shutting down MCP-EK9 server...');
      await this.lspClient.stop();
      process.exit(0);
    });
  }

  /**
   * Start the MCP server
   */
  async run(): Promise<void> {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    console.error('MCP-EK9 server started successfully');
  }
}

// Start the server
if (require.main === module) {
  const server = new Ek9McpServer();
  server.run().catch((error) => {
    console.error('Failed to start MCP-EK9 server:', error);
    process.exit(1);
  });
}

export default Ek9McpServer;