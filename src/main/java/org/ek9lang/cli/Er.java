package org.ek9lang.cli;

import java.util.Objects;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.Ek9DirectoryStructure;
import org.ek9lang.core.utils.Logger;

/**
 * Run the application that has been built or is already built.
 */
public class Er extends E {
  public Er(CommandLineDetails commandLine, FileCache sourceFileCache) {
    super(commandLine, sourceFileCache);
  }

  @Override
  protected String messagePrefix() {
    return "Run     : ";
  }

  @Override
  public boolean preConditionCheck() {
    return Objects.equals(commandLine.targetArchitecture, Ek9DirectoryStructure.JAVA)
        && super.preConditionCheck();
  }

  protected boolean doRun() {
    boolean rtn = true;

    if (!sourceFileCache.isTargetExecutableArtefactCurrent()) {
      log("Stale target - Compile");

      rtn = new Eic(commandLine, sourceFileCache).run();

      if (rtn) {
        log("Execute");

        //OK we can issue run command.
        StringBuilder theRunCommand = new StringBuilder("java");
        if (commandLine.isRunDebugMode()) {
          theRunCommand.append(" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:")
              .append(commandLine.debugPort);
        }

        String target = sourceFileCache.getTargetExecutableArtefact().getAbsolutePath();
        commandLine.getEk9AppDefines()
            .forEach(define -> theRunCommand.append(" ").append("-D").append(define));
        theRunCommand.append(" -classpath");
        theRunCommand.append(" ").append(target);
        theRunCommand.append(" ").append(commandLine.getModuleName()).append(".").append("_")
            .append(commandLine.ek9ProgramToRun);

        commandLine.getEk9ProgramParameters()
            .forEach(param -> theRunCommand.append(" ").append(param));
        Logger.log(theRunCommand.toString());
      }
    }

    return rtn;
  }
}
