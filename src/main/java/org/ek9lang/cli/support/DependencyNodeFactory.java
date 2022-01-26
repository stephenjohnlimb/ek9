package org.ek9lang.cli.support;

import org.ek9lang.cli.CommandLineDetails;
import org.ek9lang.cli.EK9SourceVisitor;
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
public class DependencyNodeFactory extends Reporter
{
	private final CommandLineDetails commandLine;
	private final PackageResolver packageResolver;

	public DependencyNodeFactory(CommandLineDetails commandLine)
	{
		super(commandLine.isVerbose());
		this.commandLine = commandLine;
		packageResolver = new PackageResolver(commandLine);
	}

	@Override
	protected String messagePrefix()
	{
		return "Resolve : ";
	}

	public Optional<DependencyNode> createFrom(EK9SourceVisitor visitor)
	{
		return createFrom(null, visitor);
	}

	private Optional<DependencyNode> createFrom(DependencyNode parent, EK9SourceVisitor visitor)
	{
		DependencyNode workingNode = new DependencyNode(visitor.getModuleName(), visitor.getVersion());

		log("Processing '" + workingNode + "'");

		if(parent != null)
		{
			parent.addDependency(workingNode);
			log("Added " + workingNode + " as dependency of " + parent);
			String circulars = parent.reportCircularDependencies(true);
			if(circulars != null)
			{
				report("Circular dependency! '" + circulars + "'");
				return Optional.empty();
			}
		}

		log("deps (" + visitor.getDeps().size() + ")");
		if(!processDependencies(workingNode, visitor.getDeps()))
			return Optional.empty();

		log("devDeps (" + visitor.getDevDeps().size() + ")");
		if(!processDependencies(workingNode, visitor.getDevDeps()))
			return Optional.empty();

		Map<String, String> excludesDeps = visitor.getExcludeDeps();
		log("excludesDeps (" + excludesDeps.size() + ")");

		excludesDeps.keySet().forEach(key -> {
			String dependencyOf = excludesDeps.get(key);
			workingNode.addDependencyRejection(key, dependencyOf);
			log("From '" + workingNode + "': excluding '" + key + "' when dependency of '" + dependencyOf + "'");
		});

		return Optional.of(workingNode);
	}

	private boolean processDependencies(DependencyNode workingNode, Map<String, String> deps)
	{
		for(String key : deps.keySet())
		{
			String dependencyVector = commandLine.getFileHandling().makeDependencyVector(key, deps.get(key));
			log("Dependency '" + dependencyVector + "'");
			Optional<EK9SourceVisitor> depVisitor = packageResolver.resolve(dependencyVector);
			if(depVisitor.isEmpty())
				return false;

			//Build a recursive structure.
			if(createFrom(workingNode, depVisitor.get()).isEmpty())
				return false;
		}
		return true;
	}
}
