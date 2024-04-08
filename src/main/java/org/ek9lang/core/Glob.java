package org.ek9lang.core;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Used for file path matching GLOB not regex.
 * This is aimed just at file and path matching.
 * Setup one or more includes and then excludes if you need to.
 * But at least one include.
 */
public final class Glob {
  private final List<PathMatcher> includes = new ArrayList<>();
  private final List<PathMatcher> excludes = new ArrayList<>();

  public Glob() {
  }

  public Glob(final String include) {

    addInclude(include);

  }

  public Glob(final String include, final String exclude) {

    addInclude(include);
    addExclude(exclude);

  }

  public Glob(final List<String> toInclude, final List<String> toExclude) {

    toInclude.forEach(this::addInclude);
    toExclude.forEach(this::addExclude);

  }

  public void addInclude(final String globPattern) {

    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
    includes.add(matcher);

  }

  public void addExclude(final String globPattern) {

    excludes.add(FileSystems.getDefault().getPathMatcher("glob:" + globPattern));

  }

  public boolean isAcceptable(final String path) {

    return isAcceptable(Paths.get(path));
  }

  public boolean isAcceptable(final Path path) {

    return included(path) && !excluded(path);
  }

  private boolean included(final Path path) {

    return includes.stream().anyMatch(matcher -> matcher.matches(path));
  }

  private boolean excluded(final Path path) {
    
    return excludes.stream().anyMatch(matcher -> matcher.matches(path));
  }
}
