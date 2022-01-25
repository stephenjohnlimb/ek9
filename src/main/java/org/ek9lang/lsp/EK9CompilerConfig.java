package org.ek9lang.lsp;


import org.ek9lang.compiler.files.CompilerFlags;

/**
 * Designed to hold the all the soft coded configurations for the
 * EK9 language server, these will typically be things like.
 * <p>
 * Max number of completions
 * <p>
 * But note it extends the standard compiler flags.
 * <p>
 * These settings are really just for general interactive use. The compiler flags
 * can be used in the command line but are also useful via an IDE.
 */
public class EK9CompilerConfig extends CompilerFlags
{
	private boolean provideLanguageHoverHelp = true;

	public boolean isProvideLanguageHoverHelp()
	{
		return provideLanguageHoverHelp;
	}

	public void setProvideLanguageHoverHelp(boolean provideLanguageHoverHelp)
	{
		this.provideLanguageHoverHelp = provideLanguageHoverHelp;
	}
}
