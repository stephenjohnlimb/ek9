package org.ek9lang.cli.support;

import org.ek9lang.cli.CommandLineDetails;
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
		var details = visitor.getPackageDetails();
		if(details.isPresent())
		{
			var packageDetails = details.get();
			DependencyNode workingNode = new DependencyNode(packageDetails.moduleName(), packageDetails.version());

			log("Processing '" + workingNode + "'");

			if(parent != null)
			{
				parent.addDependency(workingNode);
				log("Added " + workingNode + " as dependency of " + parent);
				var circulars = parent.reportCircularDependencies(true);
				if(circulars.isPresent())
				{
					report("Circular dependency! '" + circulars.get() + "'");
					return Optional.empty();
				}
			}

			log("deps (" + packageDetails.deps().size() + ")");
			if(processDependencies(workingNode, packageDetails.deps()))
			{
				log("devDeps (" + packageDetails.devDeps().size() + ")");
				if(processDependencies(workingNode, packageDetails.devDeps()))
				{
					log("excludesDeps (" + packageDetails.excludeDeps().size() + ")");
					packageDetails.excludeDeps().forEach((key, value) -> {
						workingNode.addDependencyRejection(key, value);
						log("From '" + workingNode + "': excluding '" + key + "' when dependency of '" + value + "'");
					});
					return Optional.of(workingNode);
				}
			}
		}
		return Optional.empty();
	}

	private boolean processDependencies(DependencyNode workingNode, Map<String, String> deps)
	{
		for(var entry : deps.entrySet())
		{
			String dependencyVector = commandLine.getFileHandling().makeDependencyVector(entry.getKey(), entry.getValue());
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
