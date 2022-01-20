package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;
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
	public Edp(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	public boolean run()
	{
		log("Resolve: - Clean");

		Ecl ecl = new Ecl(commandLine, sourceFileCache, osSupport);
		if(ecl.run())
		{
			log("Resolve: Prepare");

			if(!commandLine.isPackagePresent())
			{
				report("Resolve: No Dependencies defined");
			}
			else
			{
				//TODO add in the factory and http request stuff.
				/*
				DependencyNodeFactory nodeFactory = new DependencyNodeFactory(commandLine.isVerbose());
				Optional<DependencyNode> rootNode = nodeFactory.createFrom(commandLine.getSourceVisitor());
				*/
				Optional<DependencyNode> rootNode = Optional.ofNullable(null);
				if(rootNode.isPresent())
				{
					log("Resolve: All loaded");

					DependencyManager dependencyManager = new DependencyManager(rootNode.get());
					if(processDependencies(dependencyManager))
					{
						log("Resolve: Processing Success");
					}
					else
					{
						report("Resolve: Processing Failure");
						return false;
					}
				}
				else
				{
					report("Resolve: Failure");
					return false;
				}
			}

			log("Resolve: Complete");

			return true;
		}

		return false;
	}

	private boolean processDependencies(DependencyManager dependencyManager)
	{
		log("Resolve: Analysing");

		log("Resolve: Circular?");

		List<String> circulars = dependencyManager.reportCircularDependencies(true);
		if(!circulars.isEmpty())
		{
			circulars.forEach(this::report);
			return false;
		}

		log("Resolve: Exclusions!");

		//They will all be accepted at the moment, we need to get all their exclusions and apply them
		List<DependencyNode> allDependencies = dependencyManager.reportAcceptedDependencies();
		allDependencies.forEach(dep -> {
			Map<String, String> rejections = dep.getDependencyRejections();
			rejections.keySet().forEach(key -> {
				String moduleName = key;
				String whenDependencyOf = rejections.get(key);
				log("Resolve: Exclusion '" + moduleName + "' <- '" + whenDependencyOf + "'");
				dependencyManager.reject(moduleName, whenDependencyOf);
			});
		});

		log("Resolve: Promotions");

		dependencyManager.rationalise();

		log("Resolve: Optimising");

		//This would manage a deep tree and ensure there can never been an infinite loop in case of error.
		int maxOptimisations = 100;
		int iterations = 0;
		boolean optimised = dependencyManager.optimise();
		while(optimised && iterations < maxOptimisations)
		{
			iterations++;
			optimised = dependencyManager.optimise();
			log("Resolve: Optimisation " + iterations);
		}

		log("Resolve: Version breaches?");

		//Now we must check for semantic version breaches ie some part of the
		//dependency tree requires 'major' version at one value and anther part another value
		List<DependencyNode> breaches = dependencyManager.reportStrictSemanticVersionBreaches();
		if(!breaches.isEmpty())
		{
			report("Resolve: Semantic version breaches follow:");
			breaches.forEach(this::report);
			report("Resolve: You must review/refine your dependencies");
			return false;
		}

		//Now some information
		if(commandLine.isVerbose())
		{
			List<DependencyNode> rejected = dependencyManager.reportRejectedDependencies();
			if(!rejected.isEmpty())
				report("Resolve: Not applied:");
			rejected.forEach(this::report);

			report("Resolve: Applied:");
			List<DependencyNode> accepted = dependencyManager.reportAcceptedDependencies();
			accepted.forEach(this::report);
		}
		return true;
	}
}
