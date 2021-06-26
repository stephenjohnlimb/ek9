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
 *
 * Setup one or more includes and then excludes if you need to.
 * But at least one include.
 */
public class Glob
{
    private List<PathMatcher> includes = new ArrayList<>();
    private List<PathMatcher> excludes = new ArrayList<>();

        
    public Glob()
    {
    }

    public Glob(String include)
    {
    	addInclude(include);
    }
    
    public Glob(String include, String exclude)
    {
    	addInclude(include);
    	addExclude(exclude);
    }
    
    public Glob(List<String> toInclude, List<String> toExclude)
    {
        toInclude.forEach(include -> addInclude(include));
        toExclude.forEach(exclude -> addExclude(exclude));
    }

    public Glob addInclude(String globPattern)
    {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:"+globPattern);
        includes.add(matcher);
        return this;
    }

    public Glob addExclude(String globPattern)
    {
        excludes.add(FileSystems.getDefault().getPathMatcher("glob:"+globPattern));
        return this;
    }    
    
    public boolean isAcceptable(String path)
    {
        return isAcceptable(Paths.get(path));
    }

    public boolean isAcceptable(Path path)
    {
        return included(path) && !excluded(path);
    }

    private boolean included(Path path)
    {
        for(PathMatcher m : includes)
        {
            boolean matches = m.matches(path);
            if (matches)
                return true;
        }
        return false;
    }

    private boolean excluded(Path path)
    {
        for(PathMatcher m : excludes)
            if (m.matches(path))
                return true;
        return false;
    }
}
