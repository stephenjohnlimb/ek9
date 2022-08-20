package org.ek9lang.core.utils;

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

  public Glob(String include) {
    addInclude(include);
  }

  public Glob(String include, String exclude) {
    addInclude(include);
    addExclude(exclude);
  }

  public Glob(List<String> toInclude, List<String> toExclude) {
    toInclude.forEach(this::addInclude);
    toExclude.forEach(this::addExclude);
  }

  public void addInclude(String globPattern) {
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
    includes.add(matcher);
  }

  public void addExclude(String globPattern) {
    excludes.add(FileSystems.getDefault().getPathMatcher("glob:" + globPattern));
  }

  public boolean isAcceptable(String path) {
    return isAcceptable(Paths.get(path));
  }

  public boolean isAcceptable(Path path) {
    return included(path) && !excluded(path);
  }

  private boolean included(Path path) {
    return includes.stream().anyMatch(matcher -> matcher.matches(path));
  }

  private boolean excluded(Path path) {
    return excludes.stream().anyMatch(matcher -> matcher.matches(path));
  }
}
