import { spawn, ChildProcess } from 'child_process';
import { EventEmitter } from 'events';
import { Ek9JarManager } from './ek9-jar-manager';

export interface LSPMessage {
  jsonrpc: string;
  id?: number | string;
  method?: string;
  params?: any;
  result?: any;
  error?: any;
}

export interface EK9Diagnostic {
  range: {
    start: { line: number; character: number };
    end: { line: number; character: number };
  };
  severity: number; // 1=Error, 2=Warning, 3=Information, 4=Hint
  message: string;
  source: string;
}

/**
 * Client for communicating with EK9 Language Server Protocol server
 * Handles LSP initialization, document validation, and diagnostics
 */
export class Ek9LspClient extends EventEmitter {
  private lspProcess: ChildProcess | null = null;
  private jarManager: Ek9JarManager;
  private messageId = 1;
  private pendingRequests = new Map<number, { resolve: Function; reject: Function }>();
  private initialized = false;
  private buffer = '';

  constructor(projectRoot?: string) {
    super();
    this.jarManager = new Ek9JarManager(projectRoot);
  }

  /**
   * Starts the EK9 LSP server process
   * @param autoRebuild - Whether to automatically rebuild JAR if missing
   */
  async start(autoRebuild: boolean = false): Promise<void> {
    try {
      const jarPath = await this.jarManager.findEk9Jar(autoRebuild);
      
      console.log(`Starting EK9 LSP server with JAR: ${jarPath}`);
      
      // Start EK9 compiler in LSP mode with hover help
      this.lspProcess = spawn('java', [
        '-jar', jarPath,
        '-ls',  // Language Server mode
        '-lsh'  // Language Server with Hover help
      ], {
        stdio: ['pipe', 'pipe', 'pipe'],
        cwd: this.jarManager.getProjectRoot()
      });

      if (!this.lspProcess.stdout || !this.lspProcess.stdin || !this.lspProcess.stderr) {
        throw new Error('Failed to create LSP process streams');
      }

      // Set up message handling
      this.lspProcess.stdout.on('data', (data) => {
        this.handleLspOutput(data);
      });

      this.lspProcess.stderr.on('data', (data) => {
        console.error('EK9 LSP stderr:', data.toString());
      });

      this.lspProcess.on('exit', (code) => {
        console.log(`EK9 LSP process exited with code: ${code}`);
        this.lspProcess = null;
        this.initialized = false;
        this.emit('exit', code);
      });

      // Initialize LSP connection
      await this.initialize();
      
    } catch (error) {
      throw new Error(`Failed to start EK9 LSP server: ${error}`);
    }
  }

  /**
   * Stops the LSP server process
   */
  async stop(): Promise<void> {
    if (this.lspProcess) {
      // Send shutdown request
      try {
        await this.sendRequest('shutdown', {});
        this.sendNotification('exit', {});
      } catch (error) {
        console.error('Error during LSP shutdown:', error);
      }

      // Force kill if still running
      if (this.lspProcess && !this.lspProcess.killed) {
        this.lspProcess.kill('SIGTERM');
        setTimeout(() => {
          if (this.lspProcess && !this.lspProcess.killed) {
            this.lspProcess.kill('SIGKILL');
          }
        }, 5000);
      }
    }
    
    this.lspProcess = null;
    this.initialized = false;
    this.pendingRequests.clear();
  }

  /**
   * Validates EK9 source code and returns diagnostics
   * @param content - EK9 source code content
   * @param uri - Document URI (for identification)
   */
  async validateEk9Code(content: string, uri: string = 'file:///temp.ek9'): Promise<EK9Diagnostic[]> {
    if (!this.initialized) {
      throw new Error('LSP client not initialized');
    }

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('Validation timeout'));
      }, 30000); // 30 second timeout

      // Listen for diagnostics
      const diagnosticsHandler = (diagnostics: EK9Diagnostic[]) => {
        clearTimeout(timeout);
        this.removeListener('diagnostics', diagnosticsHandler);
        resolve(diagnostics);
      };

      this.on('diagnostics', diagnosticsHandler);

      // Send document to LSP server
      this.sendNotification('textDocument/didOpen', {
        textDocument: {
          uri: uri,
          languageId: 'ek9',
          version: 1,
          text: content
        }
      });
    });
  }

  /**
   * Initialize LSP connection
   */
  private async initialize(): Promise<void> {
    if (!this.lspProcess || !this.lspProcess.stdin) {
      throw new Error('LSP process not started');
    }

    const initResult = await this.sendRequest('initialize', {
      processId: process.pid,
      clientInfo: { name: 'mcp-ek9-server', version: '1.0.0' },
      capabilities: {
        textDocument: {
          synchronization: { dynamicRegistration: false },
          completion: { dynamicRegistration: false },
          hover: { dynamicRegistration: false },
          publishDiagnostics: { relatedInformation: true }
        }
      },
      workspaceFolders: [{
        uri: `file://${this.jarManager.getProjectRoot()}`,
        name: 'ek9-workspace'
      }]
    });

    this.sendNotification('initialized', {});
    this.initialized = true;
    
    console.log('EK9 LSP initialized successfully');
  }

  /**
   * Send LSP request and wait for response
   */
  private sendRequest(method: string, params: any): Promise<any> {
    return new Promise((resolve, reject) => {
      const id = this.messageId++;
      this.pendingRequests.set(id, { resolve, reject });

      const message: LSPMessage = {
        jsonrpc: '2.0',
        id,
        method,
        params
      };

      this.sendMessage(message);

      // Timeout after 10 seconds
      setTimeout(() => {
        if (this.pendingRequests.has(id)) {
          this.pendingRequests.delete(id);
          reject(new Error(`Request timeout: ${method}`));
        }
      }, 10000);
    });
  }

  /**
   * Send LSP notification (no response expected)
   */
  private sendNotification(method: string, params: any): void {
    const message: LSPMessage = {
      jsonrpc: '2.0',
      method,
      params
    };

    this.sendMessage(message);
  }

  /**
   * Send raw LSP message
   */
  private sendMessage(message: LSPMessage): void {
    if (!this.lspProcess || !this.lspProcess.stdin) {
      throw new Error('LSP process not available');
    }

    const content = JSON.stringify(message);
    const header = `Content-Length: ${Buffer.byteLength(content, 'utf8')}\r\n\r\n`;
    const fullMessage = header + content;

    this.lspProcess.stdin.write(fullMessage);
  }

  /**
   * Handle LSP server output
   */
  private handleLspOutput(data: Buffer): void {
    this.buffer += data.toString();

    // Process complete messages
    let headerEndIndex;
    while ((headerEndIndex = this.buffer.indexOf('\r\n\r\n')) !== -1) {
      const header = this.buffer.substring(0, headerEndIndex);
      const contentLengthMatch = header.match(/Content-Length: (\d+)/);
      
      if (!contentLengthMatch) {
        console.error('Invalid LSP header:', header);
        this.buffer = this.buffer.substring(headerEndIndex + 4);
        continue;
      }

      const contentLength = parseInt(contentLengthMatch[1]);
      const messageStart = headerEndIndex + 4;
      const messageEnd = messageStart + contentLength;

      if (this.buffer.length < messageEnd) {
        // Incomplete message, wait for more data
        break;
      }

      const messageContent = this.buffer.substring(messageStart, messageEnd);
      this.buffer = this.buffer.substring(messageEnd);

      try {
        const message: LSPMessage = JSON.parse(messageContent);
        this.handleLspMessage(message);
      } catch (error) {
        console.error('Failed to parse LSP message:', error, messageContent);
      }
    }
  }

  /**
   * Handle parsed LSP message
   */
  private handleLspMessage(message: LSPMessage): void {
    // Handle responses to our requests
    if (message.id !== undefined && this.pendingRequests.has(message.id as number)) {
      const pending = this.pendingRequests.get(message.id as number)!;
      this.pendingRequests.delete(message.id as number);

      if (message.error) {
        pending.reject(new Error(message.error.message || 'LSP error'));
      } else {
        pending.resolve(message.result);
      }
      return;
    }

    // Handle server notifications
    if (message.method === 'textDocument/publishDiagnostics') {
      const diagnostics = message.params?.diagnostics || [];
      this.emit('diagnostics', diagnostics);
    }
  }

  /**
   * Check if LSP server is running and initialized
   */
  isReady(): boolean {
    return this.initialized && this.lspProcess !== null && !this.lspProcess.killed;
  }

  /**
   * Get current JAR status
   */
  getJarStatus(): { exists: boolean; path: string } {
    return {
      exists: this.jarManager.jarExists(),
      path: this.jarManager.getJarPath()
    };
  }
}