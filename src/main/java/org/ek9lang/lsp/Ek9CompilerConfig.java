package org.ek9lang.lsp;


import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.main.phases.CompilationPhase;

/**
 * Designed to hold the all the soft coded configurations for the
 * EK9 language server, these will typically be things like.
 * Is hover help required.
 * But note it extends the standard compiler flags.
 * These settings are really just for general interactive use. The compiler flags
 * can be used in the command line but are also useful via an IDE.
 */
public class Ek9CompilerConfig extends CompilerFlags {
  private boolean provideLanguageHoverHelp = true;

  public Ek9CompilerConfig() {
    this(CompilationPhase.APPLICATION_PACKAGING);
  }

  public Ek9CompilerConfig(CompilationPhase compileToPhase) {
    super(compileToPhase);
  }

  public boolean isProvideLanguageHoverHelp() {
    return provideLanguageHoverHelp;
  }

  public void setProvideLanguageHoverHelp(boolean provideLanguageHoverHelp) {
    this.provideLanguageHoverHelp = provideLanguageHoverHelp;
  }
}
