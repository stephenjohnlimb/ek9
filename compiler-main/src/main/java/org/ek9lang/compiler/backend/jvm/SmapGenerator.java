package org.ek9lang.compiler.backend.jvm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Generates JSR-45 SMAP (Source Map) for debugging non-Java languages on the JVM.
 * <p>
 * SMAP format enables debuggers to map JVM bytecode back to original .ek9 source files.
 * This is critical for proper debugging support - without SMAP, debuggers expect .java files.
 * </p>
 * <p>
 * SMAP Structure:
 * </p>
 * <pre>
 * SMAP
 * GeneratedFileName
 * LanguageName
 * *S LanguageName
 * *F
 * + FileID FileName
 *   FilePath
 * *L
 * InputStartLine#FileID,InputLineCount:OutputStartLine,OutputLineIncrement
 * *E
 * </pre>
 *
 * @see <a href="https://jakarta.ee/specifications/debugging/2.0/">JSR-45 Specification</a>
 */
final class SmapGenerator {

  private static final String SMAP_HEADER = "SMAP";
  private static final String STRATUM_SECTION = "*S";
  private static final String FILE_SECTION = "*F";
  private static final String LINE_SECTION = "*L";
  private static final String END_SECTION = "*E";
  private static final String EK9_STRATUM = "EK9";

  private final String generatedFileName;
  private final Map<String, Integer> fileIdMap = new LinkedHashMap<>();
  private final List<LineMapping> lineMappings = new ArrayList<>();
  private int nextFileId = 1;

  /**
   * Create SMAP generator for a specific generated class.
   *
   * @param generatedFileName The name of the generated .class file (without .class extension)
   */
  public SmapGenerator(final String generatedFileName) {
    this.generatedFileName = generatedFileName;
  }

  /**
   * Add debug information from an IR instruction to the SMAP.
   *
   * @param debugInfo     Debug information from IR
   * @param outputLineNumber JVM bytecode line number
   */
  public void addMapping(final DebugInfo debugInfo, final int outputLineNumber) {
    if (debugInfo != null && debugInfo.isValidLocation()) {
      final int fileId = getOrCreateFileId(debugInfo.sourceFile());
      lineMappings.add(new LineMapping(
          debugInfo.lineNumber(),
          fileId,
          outputLineNumber
      ));
    }
  }

  /**
   * Collect all debug information from IR construct operations.
   *
   * @param construct IR construct containing operations with debug info
   */
  public void collectFromIRConstruct(final IRConstruct construct) {
    // Process each operation and its basic blocks
    for (var operation : construct.getOperations()) {
      if (operation.getBody() != null) {
        processBasicBlockForDebugInfo(operation.getBody());
      }
    }
  }

  /**
   * Recursively process basic block to extract debug information.
   */
  private void processBasicBlockForDebugInfo(final BasicBlockInstr basicBlock) {
    for (var instr : basicBlock.getInstructions()) {
      instr.getDebugInfo().ifPresent(debugInfo -> {
        if (debugInfo.isValidLocation()) {
          // For SMAP, we map source line to source line (1:1 mapping)
          // The actual bytecode offset mapping is handled by LineNumberTable
          addMapping(debugInfo, debugInfo.lineNumber());
        }
      });
    }
  }

  /**
   * Generate complete SMAP string in JSR-45 format.
   *
   * @return SMAP string for SourceDebugExtension attribute
   */
  public String generate() {
    if (fileIdMap.isEmpty() || lineMappings.isEmpty()) {
      return null; // No debug info available
    }

    final var smap = new StringBuilder();

    // Header
    smap.append(SMAP_HEADER).append('\n');
    smap.append(generatedFileName).append('\n');
    smap.append(EK9_STRATUM).append('\n');

    // Stratum section
    smap.append(STRATUM_SECTION).append(' ').append(EK9_STRATUM).append('\n');

    // File section
    smap.append(FILE_SECTION).append('\n');
    for (var entry : fileIdMap.entrySet()) {
      final String fileName = entry.getKey();
      final int fileId = entry.getValue();
      // Format: + FileID FileName
      smap.append("+ ").append(fileId).append(' ').append(fileName).append('\n');
      // Path (same as name for relative paths)
      smap.append(fileName).append('\n');
    }

    // Line section
    smap.append(LINE_SECTION).append('\n');
    for (var mapping : lineMappings) {
      // Format: InputStartLine#FileID:OutputStartLine
      // We use simple 1:1 line mapping (no line ranges for now)
      smap.append(mapping.inputLine)
          .append('#').append(mapping.fileId)
          .append(':').append(mapping.outputLine)
          .append('\n');
    }

    // End section
    smap.append(END_SECTION).append('\n');

    return smap.toString();
  }

  /**
   * Get or create file ID for a source file.
   * Note: sourceFile should already be normalized by IRConstruct.getNormalizedSourceFileName()
   * but we handle it here defensively in case raw paths are passed.
   */
  @SuppressWarnings("checkstyle:LambdaParameterName")
  private int getOrCreateFileId(final String sourceFile) {
    // Defensive normalization: strip ./ prefix for jdb compatibility
    final String normalizedPath = sourceFile.startsWith("./")
        ? sourceFile.substring(2)
        : sourceFile;
    return fileIdMap.computeIfAbsent(normalizedPath, _ -> nextFileId++);
  }

  /**
   * Line mapping record for SMAP generation.
   */
  private record LineMapping(int inputLine, int fileId, int outputLine) {
  }
}
