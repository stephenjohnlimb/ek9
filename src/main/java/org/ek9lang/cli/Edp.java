package org.ek9lang.cli;

import org.ek9lang.cli.support.DependencyNodeFactory;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.dependency.DependencyManager;
import org.ek9lang.dependency.DependencyNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Check and pull all dependencies in.
 * Not this does a full clean so that all existing artefacts/generated sources and targets are removed.
 */
public class Edp extends E
{
	public Edp(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Resolve : ";
	}

	protected boolean doRun()
	{
		log("- Clean");

		Ecl ecl = new Ecl(commandLine, sourceFileCache);
		if(!ecl.run())
			return false;

		if(!commandLine.isPackagePresent())
			log("No Dependencies defined");

		Optional<DependencyNode> rootNode = new DependencyNodeFactory(commandLine).createFrom(commandLine.getSourceVisitor());
		if(!rootNode.isPresent())
		{
			report("Failed");
			return false;
		}

		log("Loaded");

		if(!processDependencies(rootNode.get()))
		{
			report("Failed");
			return false;
		}
		return true;
	}

	private boolean processDependencies(DependencyNode fromNode)
	{
		DependencyManager dependencyManager = new DependencyManager(fromNode);
		log("Analysing");

		log("Circular?");

		List<String> circulars = dependencyManager.reportCircularDependencies(true);
		if(!circulars.isEmpty())
		{
			circulars.forEach(this::report);
			return false;
		}

		log("Exclusions!");

		//They will all be accepted at the moment, we need to get all their exclusions and apply them
		List<DependencyNode> allDependencies = dependencyManager.reportAcceptedDependencies();
		allDependencies.forEach(dep -> {
			Map<String, String> rejections = dep.getDependencyRejections();
			rejections.keySet().forEach(key -> {
				String moduleName = key;
				String whenDependencyOf = rejections.get(key);
				log("Exclusion '" + moduleName + "' <- '" + whenDependencyOf + "'");
				dependencyManager.reject(moduleName, whenDependencyOf);
			});
		});

		log("Version Promotions");

		dependencyManager.rationalise();

		log("Optimising");

		//This would manage a deep tree and ensure there can never been an infinite loop in case of error.
		int maxOptimisations = 100;
		int iterations = 0;
		boolean optimise = dependencyManager.optimise();
		while(optimise && iterations < maxOptimisations)
		{
			iterations++;
			optimise = dependencyManager.optimise();
			log("Optimisation " + iterations);
		}

		List<DependencyNode> breaches = dependencyManager.reportStrictSemanticVersionBreaches();
		log("Version breaches (" + breaches.size() + ")");

		//Now we must check for semantic version breaches ie some part of the
		//dependency tree requires 'major' version at one value and anther part another value
		if(!breaches.isEmpty())
		{
			report("Semantic version breaches follow:");
			breaches.forEach(this::report);
			report("You must review/refine your dependencies");
			return false;
		}

		//Now some information
		if(commandLine.isVerbose())
		{
			List<DependencyNode> rejected = dependencyManager.reportRejectedDependencies();
			if(!rejected.isEmpty())
			{
				System.err.print(messagePrefix() + "Not Applied:");
				rejected.stream().map(node -> " '" + node.toString() + "'").forEach(System.err::print);
				System.err.println(".");
			}

			List<DependencyNode> accepted = dependencyManager.reportAcceptedDependencies();
			if(!accepted.isEmpty())
			{
				System.err.print(messagePrefix() + "Applied:");
				accepted.stream().map(node -> " '" + node.toString() + "'").forEach(System.err::print);
				System.err.println(".");
			}
		}
		return true;
	}
}
