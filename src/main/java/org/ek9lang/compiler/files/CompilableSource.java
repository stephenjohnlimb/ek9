package org.ek9lang.compiler.files;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.EK9Lexer;
import org.ek9lang.compiler.tokenizer.TokenConsumptionListener;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.utils.Digest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds a reference to the name of the file being compiled a checksum of the
 * file and the date time last modified.
 * <p>
 * This is used to detect changes in source that needs to have a new parse tree
 * generated.
 */
public class CompilableSource implements Source, TokenConsumptionListener
{
	//Need to know if the source is a development source or a lib source or both or neither
	private boolean dev = false;

	//If it was brought in as part of a package then we need to know the module name of the package.
	//This is so we can let all parts of that package resolve against each other.
	//But stop code from other packages/main etc accessing anything put constructs defined at this
	//top level package name.

	private String packageModuleName;
	private boolean lib = false;

	// This is the full path to the filename.
	protected String filename = null;
	protected Digest.CheckSum checkSum = null;
	private long lastModified = -1;

	private EK9Parser parser;

	private ErrorListener errorListener;

	//As the tokens get consumed by the parser and pulled from the Lexer this
	//class listens for tokenConsumed messages and records the line and token so they can be search for
	//In an Language Server use this is really important.
	private Map<Integer, ArrayList<Token>> tokens = null;

	/**
	 * Set once parsed.
	 */
	private EK9Parser.CompilationUnitContext compilationUnitContext = null;

	public CompilableSource(String filename)
	{
		AssertValue.checkNotEmpty(filename, "Filename cannot be empty or null");
		this.filename = filename;
		resetDetails();
	}

	/**
	 * Informed when a token have been consumed out of the Lexer.
	 */
	@Override
	public void tokenConsumed(Token token)
	{
		ArrayList<Token> line = tokens.get(token.getLine());
		if(line == null)
		{
			//create and add in to the map
			line = new ArrayList<Token>();
			tokens.put(token.getLine(), line);
		}
		//Now add the token.
		line.add(token);
	}

	public TokenResult nearestToken(int line, int characterPosition)
	{
		//System.err.println("Searching for token on line " + line + " near position " + characterPosition);
		TokenResult rtn = new TokenResult();
		ArrayList<Token> lineOfTokens = tokens.get(line);
		if(lineOfTokens != null)
		{
			//Now the position won't be exact so we need to find the lower bound.
			for(int i = 0; i < lineOfTokens.size(); i++)
			{
				Token t = lineOfTokens.get(i);
				if(t.getCharPositionInLine() < characterPosition)
					rtn = new TokenResult(t, lineOfTokens, i);
				else
					return rtn;
			}
		}
		return rtn;
	}

	public EK9Parser.CompilationUnitContext getCompilationUnitContext()
	{
		return compilationUnitContext;
	}

	public boolean isDev()
	{
		return dev;
	}

	public CompilableSource setDev(boolean dev)
	{
		this.dev = dev;
		return this;
	}

	public boolean isLib()
	{
		return lib;
	}

	public String getPackageModuleName()
	{
		return packageModuleName;
	}

	public CompilableSource setLib(String packageModuleName, boolean lib)
	{
		this.packageModuleName = packageModuleName;
		this.lib = lib;
		return this;
	}

	/**
	 * Checks if the file content have changed from when first calculated
	 *
	 * @return true if modified, false if modified time and checksum are the
	 * same.
	 */
	public boolean isModified()
	{
		return lastModified != calculateLastModified() || !checkSum.equals(calculateCheckSum());
	}

	/**
	 * Just updates the last modified and the check sum to current values.
	 */
	public CompilableSource resetDetails()
	{
		updateFileDetails();
		resetTokens();
		return this;
	}

	private void resetTokens()
	{
		tokens = new HashMap<Integer, ArrayList<Token>>();
	}

	private void updateFileDetails()
	{
		lastModified = calculateLastModified();
		checkSum = calculateCheckSum();
	}

	private long calculateLastModified()
	{
		AssertValue.checkCanReadFile("Unable to read file " + filename, filename);
		File file = new File(filename);
		return file.lastModified();
	}

	private Digest.CheckSum calculateCheckSum()
	{
		AssertValue.checkCanReadFile("Unable to read file " + filename, filename);
		return Digest.digest(new File(filename));
	}

	@Override
	public int hashCode()
	{
		return filename.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == this)
			return true;
		if(obj instanceof CompilableSource)
			return ((CompilableSource)obj).filename.equals(filename);

		return false;
	}

	public CompilableSource prepareToParse()
	{
		try
		{
			//So make a new error listener to get all the errors
			setErrorListener(new ErrorListener());
			//we will set the parsed module once parsed.
			InputStream inputStream = getInputStream();
			EK9Lexer lexer = new EK9Lexer(CharStreams.fromStream(inputStream), EK9Parser.INDENT, EK9Parser.DEDENT).setSourceName(filename);
			lexer.setTokenListener(this);
			parser = new EK9Parser(new CommonTokenStream(lexer));
			lexer.removeErrorListeners();
			lexer.addErrorListener(errorListener);
			parser.removeErrorListeners();
			parser.addErrorListener(errorListener);
			return this;
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Unable to parse file " + filename, ex);
		}
	}

	public EK9Parser.CompilationUnitContext parse()
	{
		if(parser != null)
		{
			resetTokens();
			compilationUnitContext = parser.compilationUnit();
			return compilationUnitContext;
		}
		throw new RuntimeException("Need to call prepareToParse before accessing compilation unit");
	}

	private void setErrorListener(ErrorListener listener)
	{
		//System.out.println("Source set error listener [" + this.getFileName() + "]");
		this.errorListener = listener;
	}

	public ErrorListener getErrorListener()
	{
		if(this.errorListener == null)
			throw new RuntimeException("Need to call prepareToParse before accessing compilation unit and getting errors");

		return this.errorListener;
	}

	private InputStream getInputStream()
	{
		try
		{
			return new FileInputStream(new File(filename));
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Unable to open file " + filename);
		}
	}

	@Override
	public String toString()
	{
		return filename;
	}

	@Override
	public String getFileName()
	{
		return filename;
	}

	public String getGeneralIdentifier()
	{
		return getEncodedFileName(filename);
	}

	private String getEncodedFileName(String fileName)
	{
		String uri = Path.of(fileName).toUri().toString();
		return uri.replace("c:", "c%3A").replace("C:", "c%3A");
	}
}
