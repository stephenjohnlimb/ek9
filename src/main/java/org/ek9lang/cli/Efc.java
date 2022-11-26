package org.ek9lang.cli;

import org.ek9lang.cli.support.CompilationContext;

/**
 * Do a full recompile of all sources inside project.
 * Note this triggers a full resolution of dependencies and those too are pulled again
 * and recompiled.
 */
public class Efc extends Ec {
  public Efc(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Compile!: ";
  }

  protected boolean doRun() {
    if (!new Edp(compilationContext).run()) {
      return false;
    }

    prepareCompilation();

    return compile(compilationContext.sourceFileCache().getAllCompilableProjectFiles())
        && repackageTargetArtefact();
  }
}
