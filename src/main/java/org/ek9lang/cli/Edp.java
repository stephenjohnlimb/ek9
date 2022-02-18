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

		if(!new Ecl(commandLine, sourceFileCache).run())
			return false;

		if(commandLine.noPackageIsPresent())
			log("No Dependencies defined");

		Optional<DependencyNode> rootNode = new DependencyNodeFactory(commandLine).createFrom(commandLine.getSourceVisitor());
		if(rootNode.isEmpty())
		{
			report("Failed");
			return false;
		}

		if(!processDependencies(new DependencyManager(rootNode.get())))
		{
			report("Failed");
			return false;
		}

		return true;
	}

	private boolean processDependencies(DependencyManager dependencyManager)
	{
		log("Analysing Dependencies");

		if(hasCircularDependencies(dependencyManager))
			return false;

		processExclusions(dependencyManager);

		log("Version Promotions");
		dependencyManager.rationalise();

		optimise(dependencyManager);

		if(hasSemanticVersionBreaches(dependencyManager))
			return false;

		if(commandLine.isVerbose())
			showAnalysisInformation(dependencyManager);

		return true;
	}

	private boolean hasCircularDependencies(DependencyManager dependencyManager)
	{
		List<String> circulars = dependencyManager.reportCircularDependencies(true);

		log("Circular Dependencies (" + circulars.size() + ")");

		if(circulars.isEmpty())
			return false;

		circulars.forEach(this::report);
		return true;
	}

	private void processExclusions(DependencyManager dependencyManager)
	{
		//They will all be accepted at the moment, we need to get all their exclusions and apply them
		List<DependencyNode> allDependencies = dependencyManager.reportAcceptedDependencies();
		log("Exclusions (" + allDependencies.size() + ")");
		allDependencies.forEach(dep -> {
			Map<String, String> rejections = dep.getDependencyRejections();
			rejections.keySet().forEach(moduleName -> {
				String whenDependencyOf = rejections.get(moduleName);
				log("Exclusion '" + moduleName + "' <- '" + whenDependencyOf + "'");
				dependencyManager.reject(moduleName, whenDependencyOf);
			});
		});
	}

	private void optimise(DependencyManager dependencyManager)
	{
		log("Optimising");

		//This would manage a deep tree and ensure there can never been an infinite loop in case of error.
		int maxOptimisations = 100;
		int iterations = 0;
		boolean optimise = dependencyManager.optimise();
		while(optimise && iterations < maxOptimisations)
		{
			iterations++;
			optimise = dependencyManager.optimise();
			log("Optimisation (" + iterations + ")");
		}
	}

	private boolean hasSemanticVersionBreaches(DependencyManager dependencyManager)
	{
		List<DependencyNode> breaches = dependencyManager.reportStrictSemanticVersionBreaches();
		log("Version breaches (" + breaches.size() + ")");

		if(breaches.isEmpty())
			return false;

		report("Semantic version breaches:");
		breaches.forEach(this::report);
		report("You must review/refine your dependencies");
		return true;
	}

	private void showAnalysisInformation(DependencyManager dependencyManager)
	{
		showAnalysisDetails("Not Applied:", dependencyManager.reportRejectedDependencies());
		showAnalysisDetails("Applied:", dependencyManager.reportAcceptedDependencies());
	}

	private void showAnalysisDetails(String application, List<DependencyNode> list)
	{
		if(list.isEmpty())
			return;

		System.err.print(messagePrefix() + application);
		list.stream().map(node -> " '" + node + "'").forEach(System.err::print);
		System.err.println(".");
	}
}
