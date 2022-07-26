package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.EK9DirectoryStructure;
import org.ek9lang.core.utils.ZipSet;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An abstract base for creating the target artefact.
 * Normally extended by compile commands.
 */
public abstract class Ec extends E
{
	public Ec(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	protected void prepareCompilation()
	{
		log("Preparing");
		// Set if not already set
		if(!isDebuggingInstrumentation())
			setDebuggingInstrumentation(commandLine.isDebuggingInstrumentation());
		if(!isDevBuild())
			setDevBuild(commandLine.isDevBuild());

		if(isDebuggingInstrumentation())
			log("Instrumenting");
		if(isDevBuild())
			log("Development");
	}

	protected boolean compile(List<File> compilableProjectFiles)
	{
		log(compilableProjectFiles.size() + " source file(s)");

		//TODO the actual compilation!

		//At present if in log mode show the list of files to be compiled.
		compilableProjectFiles.forEach(file -> log(file.getAbsolutePath()));

		//TODO compile this with appropriate compiler

		var generatedOutputDirectory = getMainGeneratedOutputDirectory();
		//This will be some sort of intermediate form (i.e. java we then need to actually compile.

		if(this.isDevBuild())
		{
			var devGeneratedOutputDirectory = getDevGeneratedOutputDirectory();
			//This will be some sort of intermediate form (i.e. java we then need to actually compile.
		}
		return true; //or false if compilation failed
	}

	protected boolean repackageTargetArtefact()
	{
		//We can only build a jar for java at present.
		if(Objects.equals(commandLine.targetArchitecture, EK9DirectoryStructure.JAVA))
		{
			log("Creating target");

			List<ZipSet> zipSets = new ArrayList<>();
			addProjectResources(zipSets);
			addClassesFrom(getMainFinalOutputDirectory(), zipSets);
			//TODO Need to go through the deps and locate the jar file we made for each dependency and pull that in.

			if(super.isDevBuild())
			{
				addClassesFrom(getDevFinalOutputDirectory(), zipSets);
				//TODO Need to go through the dev-deps and locate the jar file we made for each dependency and pull that in.
			}

			//The parts of the EK9 runtime that we need to package in the jar.
			zipSets.add(getCoreComponents());

			String targetFileName = sourceFileCache.getTargetExecutableArtefact().getAbsolutePath();
			if(getFileHandling().createJar(targetFileName, zipSets))
				return true;
			report("Target creating failed");
		}

		return false;
	}

	/**
	 * This will be the stock set of runtime code that we need to bundle.
	 */
	private ZipSet getCoreComponents()
	{
		return new ZipSet();
	}

	private List<ZipSet> addClassesFrom(File classesDir, List<ZipSet> zipSetList)
	{
		log("Including classes from " + classesDir.getAbsolutePath());
		List<File> listOfFiles = getOsSupport().getFilesRecursivelyFrom(classesDir);
		zipSetList.add(new ZipSet(classesDir.toPath(), listOfFiles));
		return zipSetList;
	}

	private List<ZipSet> addProjectResources(List<ZipSet> zipSetList)
	{
		File projectDirectory = new File(commandLine.getSourceFileDirectory());
		Path fromPath = projectDirectory.toPath();
		List<File> listOfFiles = sourceFileCache.getAllNonCompilableProjectFiles();
		zipSetList.add(new ZipSet(fromPath, listOfFiles));
		return zipSetList;
	}
}
