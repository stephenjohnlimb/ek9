package org.ek9lang.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Designed to deal with managing dependencies of ek9 modules.
 * <p>
 * So when an ek9 module is defined and packaged it can reference dependencies and
 * development dependencies. These are basically a mix of both the module name and a version number.
 * <p>
 * Note that the module scope package IS public interface to your package, if you want internal,
 * implementations constructs put them in a module under the module you are packaging.
 * For example if publishing/packaging ek9open.google.tools.networking, and you want to have
 * internal stuff that can only be used inside the package then use:
 * ek9open.google.tools.networking.internal or ek9open.google.tools.networking.utils etc.
 * <p>
 * Full vector could be 'ek9open.google.tools.networking.utils-1.0.8-5'
 * module '-' MAJOR '.' MINOR '.' PATCH '-' BUILD
 * For feature branch builds it might be 'ek9open.google.tools.networking.utils-1.0.8-VSTS9889-5'
 * module '-' MAJOR '.' MINOR '.' PATCH ('-' FEATURE)? '-' BUILD
 * <p>
 * The actual artefact would have a suffix of '.zip'
 * <p>
 * So what's the big issue here, well we need to:
 * 1. Prohibit and detect circular dependencies.
 * 2. Remove dependencies that the developer does not want to include that other dependencies might pull in.
 * 3. Ensure only a single version (highest based on version numbering) is included.
 * 4. For semanticVersioning - fail build if major version gets pulled up when other dependencies need lower version.
 * <p>
 * For semantic versioning major > minor > patch > build and patch without feature is higher.
 * Features are ordered by alpha. i.e. Alpha > Beta for example.
 * <p>
 * This is done with a directed graph with back pointers back up the graph tree.
 * The main Nodes in the graph hold both the moduleName and the Version separately and have a flag to denote rejection.
 * They also hold a list of other Nodes that they depend on.
 * <p>
 * Once you have checked for circular dependencies (and there are none)
 * You can then use or reject exclude modules.
 * Then use this method so that modules that are the same but different version numbers can be
 * rationalised. This means finding the module of the same name but the highest version (that has not
 * already been rejected). Only keep the highest none rejected version, mark others as rejected.
 * <p>
 * BUT NOTE this sometimes means additional dependencies get included because we might have
 * rejected a module that had a number of dependencies that only that version of the module used!
 * <p>
 * At the end it is possible the developer excluded a set of dependencies that were needed.
 * The compiler will let the developer know because there will be missing symbols.
 */
public final class DependencyManager
{
	private final DependencyNode root;

	public DependencyManager(DependencyNode root)
	{
		this.root = root;
	}

	/**
	 * Mark modules as rejected if there is a higher version of the same module
	 * (that is not marked as rejected).
	 */
	public void rationalise()
	{
		listAllModuleNames().forEach(module -> {
			//Now there could be one or more here
			boolean selected = false;
			DependencyNode selectedNode = null;
			for(DependencyNode node : findByModuleName(module))
			{
				//pick first non-rejected node (it is possible all have already been rejected!)
				if(!selected && !node.isRejected())
				{
					selectedNode = node;
					selected = true;
				}
				else if(selectedNode != null && !node.isRejected())
				{
					if(selectedNode.getVersion().equals(node.getVersion()))
						node.setRejected(DependencyNode.RejectionReason.SAME_VERSION, true, false);
					else
						node.setRejected(DependencyNode.RejectionReason.RATIONALISATION, true, false);
				}
			}
		});
	}

	/**
	 * Marks dependencies that fall along a reject path as rejected.
	 * Can and probably should be called in a loop until returns false.
	 * Because you might process nodes in an order where a lower level tree node
	 * has a viable path back to parent and so is not marked as rejected.
	 * But then a little later in the processing a critical node in the path is
	 * marked as rejected. Now the lower level one does not have path back.
	 * <p>
	 * So call in a loop until fully optimised.
	 *
	 * @return true is optimisation took place, false if there was nothing to optimise.
	 */
	public boolean optimise()
	{
		boolean didOptimise = false;
		for(String module : listAllModuleNames())
		{
			List<DependencyNode> dependencies = findByModuleName(module);

			//Some of these will already be rejected, but we need to check from each of these
			//dependencies back up towards root to see if the parent dependency is rejected
			//if all dependencies all encounter a rejected node then clearly even any un-rejected
			//node is not required and can be optimised out.
			boolean allEncounterRejectedNode = true;
			boolean allRejectedAlready = true;

			for(DependencyNode dep : dependencies)
			{
				allRejectedAlready &= dep.isRejected();
				allEncounterRejectedNode &= dep.isParentRejected();
			}

			if(allEncounterRejectedNode && !allRejectedAlready)
			{
				didOptimise = true;
				dependencies
						.stream()
						.filter(dep -> !dep.isRejected())
						.forEach(dep -> dep.setRejected(DependencyNode.RejectionReason.OPTIMISED, true, false));
			}
		}
		return didOptimise;
	}

	/**
	 * Any situation where the top selected dependency has one or more, lower version numbers where the
	 * major version is less that that selected.
	 * Basically this means you cannot continue with this set of dependencies - it just won't work at all for you.
	 * Unlike Java, we do actually get the packages and recompile them, so public interfaces in packages
	 * must match.
	 *
	 * @return The list of offending nodes.
	 */
	public List<DependencyNode> reportStrictSemanticVersionBreaches()
	{
		List<DependencyNode> rtn = new ArrayList<>();
		listAllModuleNames().forEach(module -> {
			DependencyNode selected = null;
			for(DependencyNode node : findByModuleName(module))
			{
				if(!node.isRejected())
					selected = node;
				else if(selected != null && selected.getVersion().major() > node.getVersion().major() && !rtn.contains(selected))
					rtn.add(selected);
			}
		});
		return rtn;
	}

	/**
	 * Provide a list of all rejected dependencies.
	 *
	 * @return The list.
	 */
	public List<DependencyNode> reportRejectedDependencies()
	{
		return reportFilteredDependencies(DependencyNode::isRejected);
	}

	/**
	 * Provide a list of all accepted dependencies.
	 *
	 * @return The list.
	 */
	public List<DependencyNode> reportAcceptedDependencies()
	{
		return reportFilteredDependencies(dep -> !dep.isRejected());
	}

	private List<DependencyNode> reportFilteredDependencies(Predicate<DependencyNode> byPredicate)
	{
		return listAllModuleNames()
				.stream()
				.map(this::findByModuleName)
				.flatMap(Collection::stream)
				.filter(byPredicate)
				.collect(Collectors.toList());
	}

	/**
	 * Reject a moduleName dependency if is has been pulled in when is it a dependency of another module.
	 */
	public void reject(String moduleName, String whenDependencyOf)
	{
		findByModuleName(moduleName)
				.stream()
				.filter(node -> node.isDependencyOf(whenDependencyOf))
				.forEach(node -> node.setRejected(DependencyNode.RejectionReason.MANUAL, true, true));
	}

	/**
	 * Traverse the tree graph to find all the distinct module names in use ignoring the version numbers.
	 *
	 * @return The list of unique module names.
	 */
	public List<String> listAllModuleNames()
	{
		HashSet<String> uniqueResults = new HashSet<>(reportAllDependencies());
		return uniqueResults.stream().sorted().collect(Collectors.toList());
	}

	/**
	 * Search for a particular moduleName, can return multiple matches if there are
	 * multiple versions of the same dependency.
	 * The highest version number is returned first.
	 *
	 * @param moduleName The module name to search for.
	 * @return The list of modules that match (i.e. same module name but maybe multiple versions).
	 */
	public List<DependencyNode> findByModuleName(String moduleName)
	{
		List<DependencyNode> rtn = doFindByModuleName(root, moduleName);
		rtn.sort((DependencyNode o1, DependencyNode o2) -> o1.getVersion().compareTo(o2.getVersion()) * -1);
		return rtn;
	}

	private List<DependencyNode> doFindByModuleName(DependencyNode from, String moduleName)
	{
		List<DependencyNode> rtn = new ArrayList<>();
		if(from != null)
		{
			if(from.getModuleName().equals(moduleName))
				rtn.add(from);
			from.getDependencies().forEach(dependency -> rtn.addAll(doFindByModuleName(dependency, moduleName)));
		}

		return rtn;
	}

	/**
	 * Check if there are any circular references and report back up on the
	 * path the dependency was found in.
	 *
	 * @return One or more circular paths in the graph/tree.
	 */
	public List<String> reportCircularDependencies(boolean includeVersion)
	{
		return doReportCircularDependencies(this.root, includeVersion);
	}

	public List<String> reportCircularDependencies()
	{
		return doReportCircularDependencies(this.root, false);
	}

	private List<String> doReportCircularDependencies(DependencyNode from, boolean includeVersion)
	{
		List<String> rtn = new ArrayList<>();
		if(from != null)
		{
			from.getDependencies().forEach(dependency -> {
				var backPath = dependency.reportCircularDependencies(includeVersion);
				backPath.ifPresent(rtn::add);
				rtn.addAll(doReportCircularDependencies(dependency, includeVersion));
			});
		}

		return rtn;
	}

	/**
	 * Reports all the dependencies there are from root.
	 * If root is not set then this is an empty list.
	 *
	 * @return The list of all the dependencies.
	 */
	public List<String> reportAllDependencies()
	{
		return root != null ? root.reportAllDependencies() : List.of();
	}
}
