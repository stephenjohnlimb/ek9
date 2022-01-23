package org.ek9lang.cli.support;

import org.ek9lang.cli.CommandLineDetails;
import org.ek9lang.cli.EK9SourceVisitor;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;
import org.ek9lang.dependency.DependencyNode;

import java.util.Map;
import java.util.Optional;

/**
 * Just creates DependencyNodes by using a Source Visitor on some EK9 source that has a package defined in it.
 * Uses a package resolver to get the dependencies and that will unpack zips and in the future pull them from
 * remote servers can validate the contents.
 * <p>
 * There's a bit of recursion going on here.
 */
public class DependencyNodeFactory
{
	private final CommandLineDetails commandLine;
	private final FileHandling fileHandling;
	private final OsSupport osSupport;
	private PackageResolver packageResolver;

	public DependencyNodeFactory(CommandLineDetails commandLine, FileHandling fileHandling, OsSupport osSupport)
	{
		this.commandLine = commandLine;
		this.fileHandling = fileHandling;
		this.osSupport = osSupport;
		packageResolver = new PackageResolver(commandLine, fileHandling, osSupport);
	}

	public Optional<DependencyNode> createFrom(EK9SourceVisitor visitor)
	{
		return createFrom(null, visitor);
	}

	private Optional<DependencyNode> createFrom(DependencyNode parent, EK9SourceVisitor visitor)
	{
		DependencyNode rtn = null;
		boolean errorsEncountered = false;

		DependencyNode workingNode = new DependencyNode(visitor.getModuleName(), visitor.getVersion());

		if(commandLine.isVerbose())
			System.err.println("Resolve : Processing '" + workingNode.toString() + "'");

		if(parent != null)
		{
			parent.addDependency(workingNode);
			if(commandLine.isVerbose())
				System.err.println("Resolve : Added " + workingNode + " as dependency of " + parent);
			String circulars = parent.reportCircularDependencies(true);
			errorsEncountered = circulars != null;
			if(errorsEncountered)
				System.err.println("Resolve : Circular dependency! '" + circulars + "'");
		}

		if(!errorsEncountered)
		{
			Map<String, String> deps = visitor.getDeps();

			if(commandLine.isVerbose())
				System.err.println("Resolve : deps (" + deps.size() + ")");

			for(String key : deps.keySet())
			{
				String dependencyVector = fileHandling.makeDependencyVector(key, deps.get(key));
				if(commandLine.isVerbose())
					System.err.println("Resolve : Dependency '" + dependencyVector + "'");
				Optional<EK9SourceVisitor> depVisitor = packageResolver.resolve(dependencyVector);
				if(depVisitor.isPresent())
				{
					Optional<DependencyNode> maybeDep = createFrom(workingNode, depVisitor.get());
					if(!maybeDep.isPresent())
						errorsEncountered = true;
				}
				else
				{
					errorsEncountered = true;
				}
			}

			Map<String, String> devDeps = visitor.getDevDeps();

			if(commandLine.isVerbose())
				System.err.println("Resolve : devDeps (" + devDeps.size() + ")");

			for(String key : devDeps.keySet())
			{
				String dependencyVector = fileHandling.makeDependencyVector(key, devDeps.get(key));
				if(commandLine.isVerbose())
					System.err.println("Resolve : '" + dependencyVector + "'");
				Optional<EK9SourceVisitor> depVisitor = packageResolver.resolve(dependencyVector);
				if(depVisitor.isPresent())
				{
					Optional<DependencyNode> maybeDep = createFrom(workingNode, depVisitor.get());
					if(!maybeDep.isPresent())
						errorsEncountered = true;
				}
				else
				{
					errorsEncountered = true;
				}
			}

			Map<String, String> excludesDeps = visitor.getExcludeDeps();
			if(commandLine.isVerbose())
				System.err.println("Resolve : excludesDeps (" + excludesDeps.size() + ")");

			excludesDeps.keySet().forEach(key -> {
				String moduleName = key;
				String dependencyOf = excludesDeps.get(key);
				workingNode.addDependencyRejection(moduleName, dependencyOf);
				if(commandLine.isVerbose())
					System.err.println("Resolve : From '" + workingNode.toString() + "': excluding '" + moduleName + "' when dependency of '" + dependencyOf + "'");
			});
		}

		if(!errorsEncountered)
			rtn = workingNode;

		return Optional.ofNullable(rtn);
	}
}
