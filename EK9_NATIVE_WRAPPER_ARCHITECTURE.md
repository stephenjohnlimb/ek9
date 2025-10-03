# EK9 Native Wrapper Architecture

## Overview

The EK9 compiler uses a native C wrapper executable (`ek9`) to provide a seamless command-line experience that abstracts away Java execution details. This architecture enables users to run EK9 programs with simple commands like `ek9 script.ek9` or even `./script.ek9` with shebang support (`#!/usr/bin/env ek9`).

## Architecture Components

### 1. Native Wrapper (ek9.c)
**Location:** `ek9-wrapper/src/main/c/ek9.c`

A small (~13KB) native executable written in C99 that:
- Discovers the compiler JAR location
- Validates Java installation (requires Java 25+)
- Invokes the compiler JAR with user arguments
- Interprets compiler exit codes
- Executes generated run commands when appropriate

### 2. Compiler JAR (ek9c-jar-with-dependencies.jar)
**Location:** `compiler-main/target/ek9c-jar-with-dependencies.jar`

A self-contained fat JAR containing:
- Complete EK9 compiler implementation
- All dependencies bundled
- Main entry point: `ek9.Main`
- Manifest with `Main-Class` for `java -jar` execution

### 3. Er Command (Er.java)
**Location:** `compiler-main/src/main/java/org/ek9lang/cli/Er.java`

The "Execute and Run" command that:
- Checks if compiled artifacts are current
- Automatically recompiles if target is stale
- Generates the execution command for running EK9 programs
- Returns exit code 0 to signal "execute the command I printed"

## JAR Discovery Strategy

The wrapper uses a two-tier fallback strategy for locating the compiler JAR:

### Priority 1: EK9_HOME Environment Variable
```bash
export EK9_HOME=/path/to/ek9/installation
```

The wrapper looks for: `$EK9_HOME/ek9c-jar-with-dependencies.jar`

**Use case:** Power users who want to switch between multiple EK9 compiler versions (similar to `JAVA_HOME`, `GOROOT`).

### Priority 2: Relative Path (Zero-Config Mode)
If `EK9_HOME` is not set, the wrapper looks in the same directory as the executable.

**Use case:** Simple installation - just unzip `ek9` and `ek9c-jar-with-dependencies.jar` into a directory and add to `PATH`.

### Installation Structure
```
$EK9_HOME/  (or any directory on PATH)
├── ek9                              # Native wrapper executable
└── ek9c-jar-with-dependencies.jar   # Compiler JAR
```

No subdirectories needed - just two files.

## Exit Code Convention

### Compiler Internal Exit Codes

The EK9 compiler (Java component) uses an internal exit code protocol to communicate with the wrapper:

| Compiler Exit Code | Meaning | Internal Protocol |
|-------------------|---------|-------------------|
| **0** | `RUN_COMMAND_EXIT_CODE` - Success + command to run | Prints command to stdout for wrapper to execute |
| **1** | `SUCCESS_EXIT_CODE` - Success, nothing to run | Operation completed successfully (compile-only, versioning, help, etc.) |
| **2-10** | Error codes | Various error conditions (invalid parameters, compilation errors, etc.) |

### Wrapper Exit Codes (User-Facing)

The wrapper translates the compiler's internal protocol to standard Unix exit code conventions:

| Wrapper Exit Code | Meaning | How Determined |
|------------------|---------|----------------|
| **0** | Success | Compiler returned 1 (mapped to 0), OR executed program returned 0 |
| **1** | Cannot execute | Wrapper could not run compiler (process terminated abnormally) OR could not execute generated command (system() failed) |
| **2** | Invalid parameters | Compiler returned 2 |
| **3** | File processing error | Compiler returned 3 |
| **4** | Invalid combination of parameters | Compiler returned 4 |
| **5** | No programs found to be executed | Compiler returned 5 |
| **6** | Please specify which program to execute | Compiler returned 6 |
| **7** | Language Server failed to start | Compiler returned 7 |
| **8** | Compilation failed with errors | Compiler returned 8 |
| **9** | Wrong number of program arguments | Compiler returned 9 |
| **10** | Cannot convert argument to required type | Compiler returned 10 |
| **Any** | Program exit code | When compiler returns 0, wrapper executes program and returns its exit code |

### Exit Code Mapping Logic

The wrapper implements the following mapping to provide standard Unix exit code behavior:

1. **Compiler exit 0**: Compiler prints command to stdout → wrapper executes it → wrapper returns the **executed program's exit code**
2. **Compiler exit 1**: Successful operation, nothing to run → wrapper returns **0** (Unix success)
3. **Compiler exit 2-10**: Error conditions → wrapper returns **same exit code** (Unix failure)

**Key Design Decision:** The wrapper maps compiler exit code 1 to 0, following Unix conventions where 0 = success. This ensures:
- `ek9 -C file.ek9` returns 0 on successful compilation
- `ek9 -V` returns 0 for version display
- CI/CD pipelines correctly detect success/failure
- Shell scripts work with standard `if ek9 ...; then` patterns

## Execution Flow

### Example: Running an EK9 Program

**User executes:**
```bash
ek9 helloWorld.ek9 HelloString "Alice"
```

**Step-by-step flow:**

#### Step 1: Wrapper Discovers JAR
```c
// In ek9.c: findCompilerJar()
// Checks EK9_HOME, falls back to relative path
char *ek9Home = getenv("EK9_HOME");
if(ek9Home != NULL) {
    snprintf(jarPath, jarPathSize, "%s/ek9c-jar-with-dependencies.jar", ek9Home);
} else {
    // Find JAR in same directory as executable
    char *dir = dirname(executablePath);
    snprintf(jarPath, jarPathSize, "%s/ek9c-jar-with-dependencies.jar", dir);
}
```

#### Step 2: Wrapper Validates Java
```c
// In ek9.c: javaAvailable()
popen("javac -version", "r");
// Parses version, ensures >= 25
```

#### Step 3: Wrapper Invokes Compiler
```c
// In ek9.c: main()
sprintf(commandLine, "java -jar \"%s\" ", pathToEK9Jar);
strcat(commandLine, ek9Params);  // "helloWorld.ek9 HelloString Alice"

FILE *p = popen(commandLine, "r");
// Captures stdout into buffer
int exitCode = pclose(p);
```

**Compiler command executed:**
```bash
java -jar /path/to/ek9c-jar-with-dependencies.jar helloWorld.ek9 HelloString "Alice"
```

#### Step 4: Compiler Determines Operation
```java
// In Ek9.java
if (compilationContext.commandLine().options().isRunOption()) {
    execution = new Er(compilationContext);
    rtn = RUN_COMMAND_EXIT_CODE;  // Returns 0
}
```

#### Step 5: Er Command Ensures Target is Current
```java
// In Er.java: ensureTargetExecutableCurrent()
private boolean ensureTargetExecutableCurrent() {
    if (!compilationContext.sourceFileCache().isTargetExecutableArtefactCurrent()) {
        log("Stale target - Compile");
        return new Eic(compilationContext).run();  // Automatic recompilation
    }
    return true;
}
```

**Key feature:** Automatic recompilation if source is newer than compiled artifact.

#### Step 6: Er Command Generates Run Command
```java
// In Er.java: doRun()
protected boolean doRun() {
    if (ensureTargetExecutableCurrent()) {
        log("Execute");

        final var theRunCommand = new StringBuilder("java");

        // Add debug options if requested
        if (compilationContext.commandLine().options().isDebugOption()) {
            theRunCommand.append(" -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005");
        }

        // Build execution command
        theRunCommand.append(" -jar");
        theRunCommand.append(" ").append(target);  // Path to compiled JAR
        theRunCommand.append(" -r ").append(compilationContext.commandLine().getModuleName())
            .append("::").append(compilationContext.commandLine().ek9ProgramToRun);

        // Append user parameters
        // ... parameter handling ...

        Logger.log(theRunCommand.toString());  // Prints to stdout
        return true;
    }
    return false;
}
```

**Stdout from compiler (captured by wrapper):**
```bash
java -jar /tmp/ek9test/.ek9/helloWorld-0.0.1-0.jar -r helloWorld::HelloString "Alice"
```

#### Step 7: Wrapper Detects Exit Code 0 and Executes Command
```c
// In ek9.c: main()
int exitCode = pclose(p);

// Handle exit codes according to EK9 convention
if(exitCode == 0)
{
    // RUN_COMMAND_EXIT_CODE: Execute the command from stdout
    exitCode = runCommand(buffer);  // buffer contains the java -jar command
}

return(exitCode);
```

```c
// In ek9.c: runCommand()
int runCommand(char *command)
{
    FILE *p = popen(command, "r");

    // Stream output to stdout
    char ch;
    while((ch=fgetc(p)) != EOF)
        putchar(ch);

    int exitCode = pclose(p);
    return(exitCode);
}
```

**Final command executed by wrapper:**
```bash
java -jar /tmp/ek9test/.ek9/helloWorld-0.0.1-0.jar -r helloWorld::HelloString "Alice"
```

**Output to user:**
```
Hello, Alice!
```

## Key Design Principles

### 1. Wrapper Stays "Dumb"
The C wrapper doesn't understand:
- EK9 language semantics
- Target architectures (JVM/LLVM/WASM)
- Compilation phases
- Project structure

It only knows:
- How to find the JAR
- How to invoke Java
- How to interpret exit codes

**Rationale:** Single source of truth in the compiler. Adding new backends or features doesn't require updating the wrapper.

### 2. Automatic Recompilation
The `Er` command checks if the target artifact is stale and automatically recompiles.

**User benefit:** No manual `ek9 -c` before `ek9 -r`. The wrapper always runs the current version.

### 3. Seamless Java Abstraction
Users never see Java command-line details:
```bash
# User types:
ek9 script.ek9 MyProgram arg1 arg2

# Not:
java -jar compiler.jar script.ek9 MyProgram arg1 arg2
```

### 4. Shebang Support
EK9 scripts can be directly executable:

**script.ek9:**
```bash
#!/usr/bin/env ek9
defines module intro

program SayHello
  stdout <- Stdout()
  stdout.println("Hello from EK9!")
```

**Execute directly:**
```bash
chmod +x script.ek9
./script.ek9
```

The wrapper makes this work because it's a native executable on the system PATH.

### 5. Cross-Platform Consistency
Same user experience across platforms:
- macOS: Uses `_NSGetExecutablePath()`
- Linux: Uses `/proc/self/exe`
- Windows: Uses `GetModuleFileName()`

Platform differences are hidden in the wrapper's implementation.

## Multi-Target Backend Support

The wrapper architecture supports multiple compilation targets without modification:

### Current: JVM Target
```bash
ek9 script.ek9 MyProgram
# Compiles to: script-0.0.1-0.jar
# Runs with: java -jar script-0.0.1-0.jar -r script::MyProgram
```

### Future: LLVM Native Target
```bash
export EK9_TARGET=native
ek9 script.ek9 MyProgram
# Compiles to: script (native binary)
# Runs with: ./script MyProgram
```

### Future: WASM Target
```bash
ek9 -T wasm script.ek9 MyProgram
# Compiles to: script.wasm
# Runs with: wasmtime script.wasm MyProgram
```

**Key insight:** The `Er` command knows how to generate the appropriate run command for each target. The wrapper just executes whatever command is returned.

## Comparison with Other Languages

### Java
```bash
# User must know about classpath, main class
java -cp "lib/*:app.jar" com.example.Main arg1 arg2
```

### Go
```bash
# Requires separate build step
go build main.go
./main arg1 arg2

# Or uses 'go run' (compiles + runs)
go run main.go arg1 arg2
```

### Python
```bash
# Interpreter handles execution directly
python script.py arg1 arg2
```

### EK9 (This Architecture)
```bash
# Looks like Python, but compiles like Go
ek9 script.ek9 MyProgram arg1 arg2

# Or direct execution like a shell script
./script.ek9 MyProgram arg1 arg2
```

**Advantage:** Combines the simplicity of scripting languages with the performance of compiled languages.

## Maven Build Integration

The C wrapper is built as part of the multi-module Maven build:

### Module Structure
```
ek9-wrapper/
├── pom.xml                    # Maven configuration
├── CMakeLists.txt             # CMake configuration
└── src/main/c/ek9.c          # C source code
```

### Build Process
```bash
mvn clean install
```

**Maven phases:**
1. **compile phase**: Invokes CMake to generate build files
2. **compile phase**: Invokes CMake to build executable
3. **compile phase**: Copies executable to `target/bin/ek9`

**Output:** `ek9-wrapper/target/bin/ek9` (~13KB native executable)

### Maven Configuration (pom.xml)
Uses `exec-maven-plugin` to invoke CMake:
```xml
<execution>
    <id>cmake-generate</id>
    <phase>compile</phase>
    <goals>
        <goal>exec</goal>
    </goals>
    <configuration>
        <executable>cmake</executable>
        <arguments>
            <argument>-B</argument>
            <argument>${project.build.directory}/cmake-build</argument>
            <argument>-DCMAKE_BUILD_TYPE=Release</argument>
        </arguments>
    </configuration>
</execution>
```

### CMake Configuration (CMakeLists.txt)
Cross-platform C99 compilation:
```cmake
project(ek9-wrapper C)
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

add_executable(ek9 ${SOURCE_FILE})

# Platform-specific linking
if(WIN32)
    target_link_libraries(ek9 PRIVATE kernel32)
endif()
```

## Deployment and Distribution

### Installation Artifacts
A complete EK9 installation consists of just two files:

1. **ek9** - Native wrapper executable (~13KB)
2. **ek9c-jar-with-dependencies.jar** - Compiler JAR (~50MB with dependencies)

### Distribution Package Structure
```
ek9-<version>-<platform>.zip
├── ek9                              # macOS/Linux
├── ek9.exe                          # Windows
└── ek9c-jar-with-dependencies.jar
```

### Installation Instructions

**Option 1: Zero-Config (Recommended for most users)**
```bash
# Download and extract
unzip ek9-0.0.1-macos.zip -d ~/tools/ek9

# Add to PATH
export PATH="$HOME/tools/ek9:$PATH"

# Run immediately
ek9 --version
```

**Option 2: EK9_HOME (For version switching)**
```bash
# Install multiple versions
/opt/ek9/
├── 0.0.1/
│   ├── ek9
│   └── ek9c-jar-with-dependencies.jar
└── 0.0.2/
    ├── ek9
    └── ek9c-jar-with-dependencies.jar

# Switch versions by changing EK9_HOME
export EK9_HOME=/opt/ek9/0.0.1
export PATH="$EK9_HOME:$PATH"
```

### Platform-Specific Considerations

**macOS:**
- Universal binary (ARM64 + x86_64) recommended for distribution
- May require code signing for Gatekeeper
- Use `chmod +x ek9` after extraction

**Linux:**
- Separate builds for x86_64, ARM64, ARM (Raspberry Pi)
- Requires `libc` compatibility (use musl for static linking)
- Package as `.deb` (Debian/Ubuntu) and `.rpm` (RHEL/Fedora)

**Windows:**
- Build `ek9.exe` with MSVC or MinGW
- Include in PATH via installer or manual setup
- No shebang support (require explicit `ek9 script.ek9` invocation)

## Error Handling and User Feedback

### Missing JAR File

**Without EK9_HOME:**
```
Error: EK9 compiler JAR not found
Searched: /usr/local/bin/ek9c-jar-with-dependencies.jar

JAR should be in same directory as this executable.
Alternatively, set EK9_HOME environment variable:
  export EK9_HOME=/path/to/ek9/installation
  (JAR should be at: $EK9_HOME/ek9c-jar-with-dependencies.jar)
```

**With EK9_HOME:**
```
Error: EK9 compiler JAR not found
Searched: /opt/ek9/0.0.1/ek9c-jar-with-dependencies.jar

Using EK9_HOME environment variable.
Please verify:
  1. EK9_HOME is set correctly
  2. JAR exists at: $EK9_HOME/ek9c-jar-with-dependencies.jar
```

### Missing Java Installation
```
Unable to check Java version
```

### Incorrect Java Version
```
Java version needs to be 25 or higher
Suggest you download and install Java from https://www.azul.com/downloads/zulu-community/?package=jdk
```

## Performance Characteristics

### Wrapper Overhead
The native wrapper adds minimal overhead:
- **JAR discovery:** <1ms (environment variable lookup + file stat)
- **Java version check:** ~50-100ms (spawns `javac -version`)
- **Process spawning:** ~10-50ms (platform dependent)

**Total wrapper overhead:** ~60-150ms before compiler starts

### Comparison to Direct Java Invocation
```bash
# Direct Java (bypasses wrapper)
time java -jar ek9c-jar-with-dependencies.jar --version
# Real: 0.180s

# Via wrapper
time ek9 --version
# Real: 0.242s

# Overhead: ~60ms (acceptable for CLI tool)
```

### Optimization: Avoiding Double Compilation
The `Er` command's automatic recompilation check prevents wasted work:
```bash
# First run: Compilation needed
ek9 script.ek9 MyProgram
# Compiles + runs (~2-5 seconds)

# Second run: No changes
ek9 script.ek9 MyProgram
# Skips compilation, runs immediately (~0.3 seconds)
```

## Security Considerations

### JAR Discovery Security
The wrapper uses a priority-based search:
1. **EK9_HOME** (explicit user configuration - trusted)
2. **Relative path** (same directory as executable - trusted)

**No PATH search** - avoids JAR injection attacks where malicious JARs could be placed on PATH.

### Command Injection Protection
The wrapper properly quotes paths containing spaces:
```c
sprintf(commandLine, "java -jar \"%s\" ", pathToEK9Jar);
```

### Java Version Validation
The wrapper enforces minimum Java version (25+) to ensure:
- Language features are available (virtual threads, pattern matching)
- Security patches are applied
- Compatibility with EK9 compiler requirements

## Future Enhancements

### 1. Native Binary Compilation (GraalVM)
Compile the wrapper and compiler into a single native binary:
```bash
native-image --no-fallback -jar ek9c-jar-with-dependencies.jar
```

**Benefits:**
- No Java installation required
- Instant startup (~10ms vs ~500ms for JVM)
- Single file distribution

**Tradeoffs:**
- Larger binary size (~100MB vs 13KB + 50MB)
- Platform-specific builds required
- Potential compatibility issues with reflection-heavy code

### 2. LLVM Backend Support
When LLVM backend is complete:
```bash
export EK9_TARGET=native
ek9 script.ek9 MyProgram
# Compiles to native binary, runs directly
```

The `Er` command will detect the target and generate appropriate run command:
```bash
# JVM target
java -jar script.jar -r script::MyProgram

# Native target
./script MyProgram
```

### 3. Embedded Runtime JAR
Package the runtime JAR inside the wrapper executable:
- Extract to temp directory on first run
- Cache for subsequent executions
- Truly zero-dependency installation

**Implementation:** Use C resource embedding or `xxd` to convert JAR to C array.

### 4. Version Management Commands
Add version switching commands:
```bash
ek9 --list-versions       # Show installed versions
ek9 --use-version 0.0.2   # Switch active version
ek9 --install 0.0.3       # Download and install new version
```

**Implementation:** Wrapper manages multiple JARs and updates EK9_HOME.

### 5. Shell Completion
Generate completion scripts for bash/zsh/fish:
```bash
ek9 --completion bash > /etc/bash_completion.d/ek9
```

## Testing Strategy

### Unit Testing (C Code)
Use `check` or `cmocka` framework for C unit tests:
```c
START_TEST(test_findCompilerJar_with_ek9_home) {
    setenv("EK9_HOME", "/opt/ek9", 1);
    char jarPath[1024];
    int usedEk9Home = 0;

    int result = findCompilerJar(jarPath, sizeof(jarPath), &usedEk9Home);

    ck_assert_int_eq(result, 0);
    ck_assert_int_eq(usedEk9Home, 1);
    ck_assert_str_eq(jarPath, "/opt/ek9/ek9c-jar-with-dependencies.jar");
}
END_TEST
```

### Integration Testing (Java)
Test the complete flow from wrapper to execution:
```java
@Test
void testWrapperExecutesCompiledProgram() throws Exception {
    // Compile a simple EK9 program
    File source = new File("test-resources/HelloWorld.ek9");

    // Execute via wrapper
    ProcessBuilder pb = new ProcessBuilder("ek9", source.getPath(), "HelloWorld");
    Process process = pb.start();

    String output = new String(process.getInputStream().readAllBytes());
    int exitCode = process.waitFor();

    assertEquals("Hello, World!\n", output);
    assertEquals(0, exitCode);
}
```

### End-to-End Testing
Test complete user workflows:
```bash
#!/bin/bash
# Test shebang execution
cat > test.ek9 << 'EOF'
#!/usr/bin/env ek9
defines module test

program Main
  stdout <- Stdout()
  stdout.println("Shebang works!")
EOF

chmod +x test.ek9
./test.ek9 Main

# Verify output
[[ $(./test.ek9 Main) == "Shebang works!" ]] || exit 1
```

## Troubleshooting Guide

### Issue: "EK9 compiler JAR not found"
**Cause:** JAR not in expected location

**Solutions:**
1. Verify JAR exists: `ls -l $EK9_HOME/ek9c-jar-with-dependencies.jar`
2. Check EK9_HOME: `echo $EK9_HOME`
3. Ensure wrapper and JAR are in same directory (zero-config mode)

### Issue: "Java version needs to be 25 or higher"
**Cause:** Incompatible Java version

**Solutions:**
1. Check current version: `javac -version`
2. Install Java 25+: https://www.azul.com/downloads/zulu-community/?package=jdk
3. Update PATH to use newer Java: `export PATH="/path/to/jdk-25/bin:$PATH"`

### Issue: "command not found: ek9"
**Cause:** Wrapper not on PATH

**Solutions:**
1. Add to PATH: `export PATH="/path/to/ek9:$PATH"`
2. Move to existing PATH directory: `mv ek9 /usr/local/bin/`
3. Create symbolic link: `ln -s /path/to/ek9 /usr/local/bin/ek9`

### Issue: Shebang doesn't work
**Cause:** Executable bit not set or wrapper not on PATH

**Solutions:**
1. Set executable bit: `chmod +x script.ek9`
2. Verify shebang line: `#!/usr/bin/env ek9`
3. Ensure `ek9` is on PATH: `which ek9`

## Summary

The EK9 native wrapper architecture provides:

✅ **Simple user experience** - `ek9 script.ek9` just works
✅ **Zero-configuration** - Unzip and run
✅ **Flexible deployment** - EK9_HOME for power users
✅ **Automatic recompilation** - Always runs current code
✅ **Multi-target support** - JVM, LLVM, WASM backends
✅ **Shebang support** - Executable EK9 scripts
✅ **Cross-platform** - macOS, Linux, Windows
✅ **Minimal overhead** - ~60ms wrapper cost
✅ **Maintainable** - Wrapper stays simple, compiler has all logic

This architecture enables EK9 to provide a Python-like developer experience while maintaining the performance and safety of a compiled language.
