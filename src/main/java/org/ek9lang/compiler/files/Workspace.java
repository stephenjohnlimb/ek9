package org.ek9lang.compiler.files;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Designed to represent one or more source files that are part of a workspace.
 * Needs to become thread safe. use synchronized for now.
 */
public class Workspace
{
	//The maps of source code file to compliable source objects
	private Map<String, CompilableSource> sources = new HashMap<>();

	//TODO rework when we develop rest of ParsedModule etc.

	//Once the Compilable Source has been parsed we will have a set of ParsedModules.
	//Used to be held in the CompilableSource but now decoupled. 
	//private Map<CompilableSource, ParsedModule> modules = new HashMap<>();

	//The Intermediate representation as nodes with links to Symbols and Tokens.
	//private Map<CompilableSource, IRModule> irModules = new HashMap<>();

	/**
	 * ReParses or loads and parses a source file.
	 */
	public synchronized CompilableSource reParseSource(Path path)
	{
		return reParseSource(path.toString());
	}

	public synchronized CompilableSource reParseSource(String uri)
	{
		System.err.println("parsing/reparsing [" + uri + "]");
		CompilableSource compilableSource = null;
		if(isSourcePresent(uri))
		{
			compilableSource = getSource(uri);
		}
		else
		{
			compilableSource = new CompilableSource(uri);
			addSource(compilableSource);
		}
		compilableSource.prepareToParse().parse();

		System.err.println("Workspace has " + sources.size() + " source files");
		return compilableSource;
	}

	/*
	public synchronized Workspace addParsedModule(ParsedModule module)
	{
		modules.put(module.getCompilableSource(), module);
		return this;
	}
	
	public ParsedModule getParsedModule(CompilableSource source)
	{
		return modules.get(source);		
	}
	
	public synchronized Workspace addIRModule(IRModule irModule)
	{
		irModules.put(irModule.getCompilableSource(), irModule);
		return this;
	}
	
	public IRModule getIRModule(CompilableSource source)
	{
		return irModules.get(source);		
	}

	*/

	public Workspace addSource(CompilableSource source)
	{
		sources.put(source.getFileName(), source);
		return this;
	}

	public boolean isSourcePresent(String fileName)
	{
		return sources.containsKey(fileName);
	}

	public CompilableSource getSource(Path path)
	{
		return sources.get(path.toString());
	}

	public CompilableSource getSource(String fileName)
	{
		return sources.get(fileName);
	}

	public synchronized CompilableSource removeSource(Path path)
	{
		CompilableSource rtn = sources.remove(path.toString());
		//also remove from modules.
		//modules.remove(rtn);

		System.err.println("Workspace now has " + sources.size() + " source files");
		return rtn;
	}

	public Collection<CompilableSource> getSources()
	{
		return sources.values();
	}
}
