#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <libgen.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <limits.h>
#include <unistd.h>

#if __APPLE__
//headers for macos
#include <mach-o/dyld.h>
#elif __linux__
//headers for linux
#elif __unix__
//headers for unix
#else
//headers for windows
#include <libloaderapi.h>
#endif

#define MAX_PATH_LENGTH 1024

/*
 * EK9 Compiler Wrapper
 *
 * This native executable provides a simple interface to the EK9 compiler JAR.
 * It supports two modes:
 * 1. Zero-config: JAR located relative to this executable (simple installation)
 * 2. EK9_HOME: JAR located in $EK9_HOME/lib/ (version switching)
 *
 * Environment Variables:
 * - EK9_HOME: Directory containing EK9 compiler JAR (optional)
 * - EK9_COMPILER_MEMORY: JVM memory flags for compiler (default: -Xmx512m)
 *
 * The wrapper:
 * - Finds the compiler JAR (using EK9_HOME or relative path)
 * - Checks Java version (requires Java 25+)
 * - Invokes: java <EK9_COMPILER_MEMORY> -jar <jar> <user-args>
 * - Handles exit codes:
 *   - 0: Execute command from stdout (run compiled program)
 *   - 1-7: Pass through (compiler success/errors)
 */

/*
Check if Java is available and at required version (25+).
*/
int javaAvailable()
{
    /*
    We need more than just java we need the compiler as well
    and it needs to be version 25 or higher
    Note: javac outputs to stderr, not stdout
    */
    FILE *p = popen("javac -version 2>&1", "r");

    if(p == NULL)
    {
        fprintf(stderr, "Error: Unable to check Java version\n");
        return 1;
    }

    char buffer[256];
    size_t pos = 0;
    int ch;

    // Read with bounds checking
    while((ch = fgetc(p)) != EOF && pos < sizeof(buffer) - 1)
    {
        buffer[pos++] = (char)ch;
    }
    buffer[pos] = '\0';

    pclose(p);

    // Parse version more robustly
    // Expected format: "javac 25.0.1" or "javac 25.0.1.7"
    // Look for first digit sequence after "javac "
    char *versionStart = strstr(buffer, "javac ");
    if(versionStart == NULL)
    {
        fprintf(stderr, "Error: Unable to parse Java version from: %s\n", buffer);
        return 1;
    }

    // Skip "javac " (6 chars)
    versionStart += 6;

    // Extract major version number
    int majorVersion = atoi(versionStart);

    if(majorVersion < 25)
    {
        fprintf(stderr, "Error: Java version needs to be 25 or higher (found %d)\n", majorVersion);
        fprintf(stderr, "Suggest you download and install Java from https://www.azul.com/downloads/zulu-community/?package=jdk\n");
        return 1;
    }

    return 0;
}

/*
Check if a string contains spaces.
*/
int containsSpaces(const char* str)
{
    if(str == NULL)
        return 0;

    while(*str)
    {
        if(*str == ' ')
            return 1;
        str++;
    }
    return 0;
}

/*
Create a new string wrapped in single quotes: "text" -> "'text'"
Caller must free the returned string.
*/
char* wrapInQuotes(const char* str)
{
    if(str == NULL)
        return NULL;

    size_t len = strlen(str);
    // Need space for: ' + original + ' + \0
    char* quoted = malloc(len + 3);

    if(quoted == NULL)
        return NULL;

    quoted[0] = '\'';
    strcpy(quoted + 1, str);
    quoted[len + 1] = '\'';
    quoted[len + 2] = '\0';

    return quoted;
}

/*
Build argv array for execvp - bypasses shell parsing entirely.
Arguments containing spaces are wrapped in single quotes for Java CLI parsing.
Returns newly allocated array that must be freed by caller.
Also returns array of booleans indicating which strings were dynamically allocated.
*/
char** buildJavaArgv(int argc, char *argv[], char *jarPath, int *newArgc, char **allocatedFlags)
{
    // Get memory setting from environment or use default
    const char *memoryFlag = getenv("EK9_COMPILER_MEMORY");
    if(memoryFlag == NULL || memoryFlag[0] == '\0')
    {
        memoryFlag = "-Xmx512m";
    }

    // argv: [java, memoryFlag, -jar, jarPath, user_arg1, user_arg2, ..., NULL]
    // Count: 1 (java) + 1 (memory) + 1 (-jar) + 1 (jarPath) + (argc-1) user args + 1 (NULL)
    *newArgc = argc + 4;
    char **javaArgv = malloc((*newArgc + 1) * sizeof(char*));

    if(javaArgv == NULL)
        return NULL;

    // Allocate flags array to track which strings need freeing
    *allocatedFlags = malloc(*newArgc * sizeof(char));
    if(*allocatedFlags == NULL)
    {
        free(javaArgv);
        return NULL;
    }
    memset(*allocatedFlags, 0, *newArgc);

    javaArgv[0] = "java";
    javaArgv[1] = (char*)memoryFlag;  // Memory flag from env or default
    javaArgv[2] = "-jar";
    javaArgv[3] = jarPath;

    // Process user arguments (argv[1..argc-1])
    for(int i = 1; i < argc; i++)
    {
        int targetIndex = i + 3;  // Offset increased by 1 due to memory flag

        if(containsSpaces(argv[i]))
        {
            // Wrap in quotes for Java CLI regex parsing
            javaArgv[targetIndex] = wrapInQuotes(argv[i]);
            if(javaArgv[targetIndex] == NULL)
            {
                // Allocation failed - clean up
                for(int j = 0; j < targetIndex; j++)
                {
                    if((*allocatedFlags)[j])
                        free(javaArgv[j]);
                }
                free(javaArgv);
                free(*allocatedFlags);
                return NULL;
            }
            (*allocatedFlags)[targetIndex] = 1;
        }
        else
        {
            // No spaces - use original string
            javaArgv[targetIndex] = argv[i];
            (*allocatedFlags)[targetIndex] = 0;
        }
    }

    javaArgv[*newArgc] = NULL;  // execvp requires NULL terminator

    return javaArgv;
}

/*
Free allocated Java argv array and its dynamically allocated strings.
*/
void cleanupJavaArgv(char **javaArgv, char *allocatedFlags, int argc)
{
    if(javaArgv)
    {
        for(int i = 0; i < argc; i++)
        {
            if(allocatedFlags && allocatedFlags[i])
                free(javaArgv[i]);
        }
        free(javaArgv);
    }
    if(allocatedFlags)
        free(allocatedFlags);
}

/*
Parse command string into argv array for execvp.
Handles single-quoted strings (preserves quotes for execvp to parse).
Returns newly allocated array that must be freed by caller.
Returns argument count in *argc.
*/
char** parseCommand(const char *command, int *argc)
{
    if(command == NULL || argc == NULL)
        return NULL;

    // Allocate argv array (estimate max args)
    int maxArgs = 64;
    char **argv = malloc(maxArgs * sizeof(char*));
    if(argv == NULL)
        return NULL;

    *argc = 0;
    const char *p = command;

    // Skip leading whitespace
    while(*p && *p == ' ')
        p++;

    while(*p)
    {
        // Reallocate if needed
        if(*argc >= maxArgs - 1)
        {
            maxArgs *= 2;
            char **newArgv = realloc(argv, maxArgs * sizeof(char*));
            if(newArgv == NULL)
            {
                // Cleanup on failure
                for(int i = 0; i < *argc; i++)
                    free(argv[i]);
                free(argv);
                return NULL;
            }
            argv = newArgv;
        }

        const char *start = p;
        const char *end;

        // Handle quoted strings
        if(*p == '\'')
        {
            // Find closing quote (include quotes in argument)
            start = p;
            p++;
            while(*p && *p != '\'')
                p++;
            if(*p == '\'')
                p++;
            end = p;
        }
        else
        {
            // Regular argument - read until space
            while(*p && *p != ' ')
                p++;
            end = p;
        }

        // Copy argument
        size_t len = end - start;
        argv[*argc] = malloc(len + 1);
        if(argv[*argc] == NULL)
        {
            // Cleanup on failure
            for(int i = 0; i < *argc; i++)
                free(argv[i]);
            free(argv);
            return NULL;
        }
        strncpy(argv[*argc], start, len);
        argv[*argc][len] = '\0';
        (*argc)++;

        // Skip trailing whitespace
        while(*p && *p == ' ')
            p++;
    }

    argv[*argc] = NULL;  // NULL terminator for execvp
    return argv;
}

/*
Free parsed command argv array.
*/
void cleanupCommandArgv(char **argv, int argc)
{
    if(argv == NULL)
        return;

    for(int i = 0; i < argc; i++)
    {
        if(argv[i])
            free(argv[i]);
    }
    free(argv);
}

/*
Find the compiler JAR using either EK9_HOME or relative to executable.
Returns 0 on success, 1 on failure.
Populates jarPath with the discovered location.
*/
int findCompilerJar(char *jarPath, size_t jarPathSize, int *usedEk9Home)
{
    // Priority 1: Check EK9_HOME environment variable
    char *ek9Home = getenv("EK9_HOME");

    if(ek9Home != NULL && strlen(ek9Home) > 0)
    {
        snprintf(jarPath, jarPathSize, "%s/ek9c-jar-with-dependencies.jar", ek9Home);
        *usedEk9Home = 1;
        return(0);
    }

    // Priority 2: Find JAR relative to this executable (zero-config mode)
    char pathToCommand[MAX_PATH_LENGTH+1] = "";

#if __APPLE__
    uint32_t path_len = MAX_PATH_LENGTH;
    // SPI first appeared in Mac OS X 10.2
    _NSGetExecutablePath(pathToCommand, &path_len);
#elif __linux__
    int len = readlink("/proc/self/exe", pathToCommand, sizeof(pathToCommand)-1);
    if (len != -1)
      pathToCommand[len] = '\0';
#elif __unix__
    pathToCommand[0] = '\0';
    printf("Warning: Unix executable path detection not implemented\n");
#else
    GetModuleFileName(NULL, pathToCommand, MAX_PATH_LENGTH);
#endif

    // JAR is in same directory as executable
    char *dir = dirname(pathToCommand);
    snprintf(jarPath, jarPathSize, "%s/ek9c-jar-with-dependencies.jar", dir);
    *usedEk9Home = 0;

    return(0);
}

/*
Main entry point.
*/
int main(int argc, char *argv[])
{
    char pathToEK9Jar[MAX_PATH_LENGTH+1] = "";
    int usedEk9Home = 0;

    // Find compiler JAR (EK9_HOME or relative to executable)
    if(findCompilerJar(pathToEK9Jar, sizeof(pathToEK9Jar), &usedEk9Home) != 0)
    {
        fprintf(stderr, "Error: Unable to determine compiler JAR location\n");
        return 1;
    }

    // Check Java is available and correct version
    if(javaAvailable() != 0)
        return 1;

    // Check compiler JAR exists with helpful error message
    if(access(pathToEK9Jar, R_OK|F_OK ) != 0)
    {
        fprintf(stderr, "Error: EK9 compiler JAR not found\n");
        fprintf(stderr, "Searched: %s\n", pathToEK9Jar);

        if(usedEk9Home)
        {
            fprintf(stderr, "\nUsing EK9_HOME environment variable.\n");
            fprintf(stderr, "Please verify:\n");
            fprintf(stderr, "  1. EK9_HOME is set correctly\n");
            fprintf(stderr, "  2. JAR exists at: $EK9_HOME/ek9c-jar-with-dependencies.jar\n");
        }
        else
        {
            fprintf(stderr, "\nJAR should be in same directory as this executable.\n");
            fprintf(stderr, "Alternatively, set EK9_HOME environment variable:\n");
            fprintf(stderr, "  export EK9_HOME=/path/to/ek9/installation\n");
            fprintf(stderr, "  (JAR should be at: $EK9_HOME/ek9c-jar-with-dependencies.jar)\n");
        }

        return 1;
    }

    // Build argv for Java execution (bypasses shell)
    int javaArgc;
    char *allocatedFlags;
    char **javaArgv = buildJavaArgv(argc, argv, pathToEK9Jar, &javaArgc, &allocatedFlags);

    if(javaArgv == NULL)
    {
        fprintf(stderr, "Error: Failed to allocate memory for arguments\n");
        return 1;
    }

    // Create pipe for capturing stdout
    int pipefd[2];
    if(pipe(pipefd) == -1)
    {
        fprintf(stderr, "Error: Failed to create pipe\n");
        cleanupJavaArgv(javaArgv, allocatedFlags, javaArgc);
        return 1;
    }

    // Fork to run Java
    pid_t pid = fork();

    if(pid < 0)
    {
        fprintf(stderr, "Error: Failed to fork process\n");
        close(pipefd[0]);
        close(pipefd[1]);
        cleanupJavaArgv(javaArgv, allocatedFlags, javaArgc);
        return 1;
    }

    if(pid == 0)
    {
        // Child process: redirect stdout to pipe and execute Java
        close(pipefd[0]);  // Close read end
        dup2(pipefd[1], STDOUT_FILENO);
        close(pipefd[1]);

        execvp("java", javaArgv);

        // If execvp returns, it failed
        fprintf(stderr, "Error: Failed to execute java\n");
        exit(1);
    }

    // Parent process: read stdout and wait for child
    close(pipefd[1]);  // Close write end

    char outputBuffer[4096];
    ssize_t bytesRead = read(pipefd[0], outputBuffer, sizeof(outputBuffer) - 1);
    close(pipefd[0]);

    if(bytesRead < 0)
    {
        fprintf(stderr, "Error: Failed to read from pipe\n");
        cleanupJavaArgv(javaArgv, allocatedFlags, javaArgc);
        return 1;
    }
    else if(bytesRead > 0)
    {
        outputBuffer[bytesRead] = '\0';
    }

    int status;
    waitpid(pid, &status, 0);

    // Clean up allocated strings
    cleanupJavaArgv(javaArgv, allocatedFlags, javaArgc);

    int exitCode;
    if(WIFEXITED(status))
    {
        exitCode = WEXITSTATUS(status);
    }
    else
    {
        fprintf(stderr, "Error: Unable to execute EK9 compiler. The compiler process terminated unexpectedly.\n");
        fprintf(stderr, "This may indicate a corrupted installation. Try reinstalling EK9.\n");
        return 1;
    }

    // If exit code is 0 and we captured a command, execute it
    if(exitCode == 0 && bytesRead > 0)
    {
        // Remove trailing newline if present
        if(outputBuffer[bytesRead - 1] == '\n')
            outputBuffer[bytesRead - 1] = '\0';

        // Parse command into argv for direct execution (preserves stdin/stdout)
        int commandArgc;
        char **commandArgv = parseCommand(outputBuffer, &commandArgc);

        if(commandArgv == NULL)
        {
            fprintf(stderr, "Error: Failed to parse execution command\n");
            return 1;
        }

        // Fork and execute command directly (inherits stdin/stdout/stderr from parent)
        pid_t execPid = fork();

        if(execPid < 0)
        {
            fprintf(stderr, "Error: Failed to fork process for program execution\n");
            cleanupCommandArgv(commandArgv, commandArgc);
            return 1;
        }

        if(execPid == 0)
        {
            // Child process: execute compiled program directly
            // stdin/stdout/stderr are inherited from parent (ek9 wrapper)
            execvp(commandArgv[0], commandArgv);

            // If execvp returns, it failed
            fprintf(stderr, "Error: Failed to execute compiled program: %s\n", commandArgv[0]);
            exit(1);
        }

        // Parent process: wait for child and get exit code
        int execStatus;
        waitpid(execPid, &execStatus, 0);

        // Clean up parsed command
        cleanupCommandArgv(commandArgv, commandArgc);

        if(WIFEXITED(execStatus))
        {
            exitCode = WEXITSTATUS(execStatus);
        }
        else
        {
            fprintf(stderr, "Error: Unable to run compiled program. The program terminated unexpectedly.\n");
            return 1;
        }
    }
    else if(bytesRead > 0)
    {
        // If exit code wasn't 0, print the compiler output (errors, etc.)
        printf("%s", outputBuffer);
    }

    // Map compiler exit code 1 (success, nothing to run) to 0 (Unix success)
    // The compiler uses exit code 1 for successful operations that don't require
    // running a program (e.g., -C compile only, -V version, -Gk generate keys)
    // Following Unix conventions, we return 0 for all successful operations
    if(exitCode == 1)
    {
        exitCode = 0;
    }

    return exitCode;
}
