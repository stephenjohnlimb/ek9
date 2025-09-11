package org.ek9lang.compiler.backend;

import java.io.File;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ir.instructions.IRConstruct;

/**
 * A given construct from a specific source should be output to the targetFile.
 */
public record ConstructTargetTuple(IRConstruct construct,
                                   String relativeFileName,
                                   CompilerFlags compilerFlags,
                                   File targetFile) {
}
