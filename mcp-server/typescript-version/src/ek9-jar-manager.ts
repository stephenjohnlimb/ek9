import * as fs from 'fs';
import * as path from 'path';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

/**
 * Manages EK9 compiler JAR file discovery and Maven rebuilding
 * Implements robust jar discovery with fallback to Maven rebuild
 */
export class Ek9JarManager {
  private readonly projectRoot: string;
  private readonly jarPath: string;
  private cachedJarPath: string | null = null;
  private lastCheckTime: number = 0;
  private readonly checkIntervalMs = 5000; // 5 seconds

  constructor(projectRoot?: string) {
    this.projectRoot = projectRoot || process.cwd();
    this.jarPath = path.join(this.projectRoot, 'compiler-main/target/ek9c-jar-with-dependencies.jar');
  }

  /**
   * Finds the EK9 compiler JAR, rebuilding if necessary
   * @param autoRebuild - Whether to automatically rebuild if jar is missing
   * @returns Path to the EK9 JAR file
   * @throws Error if JAR cannot be found or built
   */
  async findEk9Jar(autoRebuild: boolean = false): Promise<string> {
    const now = Date.now();
    
    // Use cached path if recent and valid
    if (this.cachedJarPath && 
        (now - this.lastCheckTime) < this.checkIntervalMs && 
        fs.existsSync(this.cachedJarPath)) {
      return this.cachedJarPath;
    }

    // Check if jar exists
    if (fs.existsSync(this.jarPath)) {
      this.cachedJarPath = this.jarPath;
      this.lastCheckTime = now;
      return this.jarPath;
    }

    // JAR missing - try to rebuild if enabled
    if (autoRebuild) {
      console.log('EK9 JAR not found, attempting Maven rebuild...');
      try {
        await this.rebuildJar();
        
        // Check again after rebuild
        if (fs.existsSync(this.jarPath)) {
          this.cachedJarPath = this.jarPath;
          this.lastCheckTime = now;
          return this.jarPath;
        }
      } catch (error) {
        throw new Error(`Failed to rebuild EK9 JAR: ${error}`);
      }
    }

    // Final failure
    throw new Error(
      `EK9 compiler JAR not found at: ${this.jarPath}\n` +
      'Please run: mvn clean install\n' +
      'Or enable auto-rebuild in MCP server configuration'
    );
  }

  /**
   * Rebuilds the EK9 compiler JAR using Maven
   */
  private async rebuildJar(): Promise<void> {
    try {
      const { stdout, stderr } = await execAsync(
        'mvn clean install -pl compiler-main -q',
        { 
          cwd: this.projectRoot,
          timeout: 300000 // 5 minutes timeout
        }
      );

      if (stderr && stderr.includes('ERROR')) {
        throw new Error(`Maven build failed: ${stderr}`);
      }

      console.log('Maven rebuild completed successfully');
    } catch (error) {
      throw new Error(`Maven execution failed: ${error}`);
    }
  }

  /**
   * Checks if the JAR exists without rebuilding
   */
  jarExists(): boolean {
    return fs.existsSync(this.jarPath);
  }

  /**
   * Gets the expected JAR path (may not exist)
   */
  getJarPath(): string {
    return this.jarPath;
  }

  /**
   * Invalidates the cached JAR path, forcing next lookup
   */
  invalidateCache(): void {
    this.cachedJarPath = null;
    this.lastCheckTime = 0;
  }

  /**
   * Gets project root directory
   */
  getProjectRoot(): string {
    return this.projectRoot;
  }
}