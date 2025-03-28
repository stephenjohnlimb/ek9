package org.ek9lang.cli;

import java.util.Objects;
import org.ek9lang.core.Logger;
import org.ek9lang.core.TargetArchitecture;

/**
 * Run the application that has been built or is already built.
 */
class Er extends E {
  Er(final CompilationContext compilationContext) {

    super(compilationContext);

  }

  @Override
  protected String messagePrefix() {

    return "Run     : ";
  }

  @Override
  public boolean preConditionCheck() {

    return Objects.equals(compilationContext.commandLine().targetArchitecture, TargetArchitecture.JVM)
        && super.preConditionCheck();
  }

  @Override
  protected boolean doRun() {

    if (!compilationContext.sourceFileCache().isTargetExecutableArtefactCurrent()) {
      log("Stale target - Compile");

      final var compileResult = new Eic(compilationContext).run();

      if (compileResult) {
        log("Execute");

        //OK we can issue run command.
        final var theRunCommand = new StringBuilder("java");
        if (compilationContext.commandLine().options().isRunDebugMode()) {
          theRunCommand.append(" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:")
              .append(compilationContext.commandLine().debugPort);
        }

        final var target = compilationContext.sourceFileCache().getTargetExecutableArtefact().getAbsolutePath();

        compilationContext.commandLine().getEk9AppDefines()
            .forEach(define -> theRunCommand.append(" ").append("-D").append(define));

        theRunCommand.append(" -classpath");
        theRunCommand.append(" ").append(target);
        theRunCommand.append(" ").append(compilationContext.commandLine().getModuleName())
            .append(".").append("_")
            .append(compilationContext.commandLine().ek9ProgramToRun);

        compilationContext.commandLine().getEk9ProgramParameters()
            .forEach(param -> theRunCommand.append(" ").append(param));
        Logger.log(theRunCommand.toString());
      }
      return true;
    }

    return false;
  }
}
