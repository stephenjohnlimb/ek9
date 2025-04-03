package org.ek9lang.compiler.backend;

import java.io.File;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ir.Construct;

/**
 * A given construct from a specific source should be output to the targetFile.
 */
public record ConstructTargetTuple(Construct construct,
                                   String relativeFileName,
                                   CompilerFlags compilerFlags,
                                   File targetFile) {
}
