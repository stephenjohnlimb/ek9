package org.ek9lang.compiler;

import java.io.Serial;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.SharedThreadContext;

/**
 * Represents the Intermediate Representation of the EK9 code for a particular EK9 module.
 * <p>
 * The idea is to pull out all the EK9 specifics and move more towards a general target
 * CPU based implementation. By this I mean take out things like 'guards' and
 * </p>
 */
public final class IRModule implements Module {
  @Serial
  private static final long serialVersionUID = 1L;
  private final CompilableSource source;
  private final SharedThreadContext<CompilableProgram> compilableProgram;
  private String moduleName;

  public IRModule(final CompilableSource source,
                  final SharedThreadContext<CompilableProgram> compilableProgram) {

    AssertValue.checkNotNull("CompilableSource cannot be null", source);
    AssertValue.checkNotNull("CompilableProgram cannot be null", compilableProgram);
    this.source = source;
    this.compilableProgram = compilableProgram;

  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public String getScopeName() {
    return moduleName;
  }

  @Override
  public boolean isEk9Core() {

    //Any module that start with this is deemed core.
    return moduleName.startsWith("org.ek9");
  }

  public void acceptCompilationUnitContext(final EK9Parser.CompilationUnitContext compilationUnitContext) {

    AssertValue.checkNotNull("CompilationUnitContext cannot be null", compilationUnitContext);
    this.moduleName = compilationUnitContext.moduleDeclaration().dottedName().getText();
    AssertValue.checkNotEmpty("ModuleName must have a value", moduleName);
  }
}
