package org.ek9lang.cli;

/**
 * Do a full recompile of all sources inside project.
 * Note this triggers a full resolution of dependencies and those too are pulled again
 * and recompiled.
 */
final class Efc extends Ec {
  Efc(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Compile!: ";
  }

  @Override
  protected boolean doRun() {
    if (!new Edp(compilationContext).run()) {
      return false;
    }

    prepareCompilation();

    return compile(compilationContext.sourceFileCache().getAllCompilableProjectFiles())
        && repackageTargetArtefact();
  }
}
