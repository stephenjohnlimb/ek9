package org.ek9lang.dependency;

import java.util.*;

import org.ek9lang.core.utils.SemanticVersion;

/**
 * Represents a single dependency by moduleName and version.
 */
public class DependencyNode
{
    //Navigational graph structure.
    //Will remain null if this is the top node.
    private DependencyNode parent = null;
    private List<DependencyNode> dependencies = new ArrayList<>();
    //When loading up nodes these are the defined rejections that need to be applied.
    private Map<String, String> dependencyRejections = new HashMap<>();

    private boolean rejected = false;
    private RejectionReason reason;

    private String moduleName;
    private SemanticVersion version;

    /**
     * Create a node from a vector
     * @param dependencyVector - example a.b.c-3.8.3-feature32-90
     * @return A new dependency node.
     */
    public static DependencyNode of(String dependencyVector)
    {
        int versionStart = dependencyVector.indexOf('-');
        String moduleName = dependencyVector.substring(0, versionStart);
        String versionPart = dependencyVector.substring(versionStart+1);

        DependencyNode rtn = new DependencyNode(moduleName, versionPart);

        return rtn;
    }

    public DependencyNode(String moduleName, String version)
    {
        this.moduleName = moduleName;
        this.version = SemanticVersion._of(version);
    }

    public DependencyNode addDependencyRejection(String moduleName, String whenDependencyOf)
    {
        this.dependencyRejections.put(moduleName, whenDependencyOf);
        return this;
    }

    public Map<String, String> getDependencyRejections()
    {
        return dependencyRejections;
    }

    public DependencyNode addDependency(DependencyNode node)
    {
        node.setParent(this);
        dependencies.add(node);
        return this;
    }

    public boolean isDependencyOf(String whenDependencyOf)
    {
        DependencyNode d = parent;
        while(d != null)
        {
            if(d.getModuleName().equals(whenDependencyOf))
                return true;
            d = d.getParent();
        }
        return false;
    }

    public List<String> reportAllDependencies()
    {
    	List<String> rtn = new ArrayList<>();
    	rtn.add(getModuleName());
    	for(DependencyNode dep: dependencies)
    		rtn.addAll(dep.reportAllDependencies());
    	
    	return rtn;
    }
    
    public String reportCircularDependencies(boolean includeVersion)
    {
        DependencyNode d = parent;
        while(d != null)
        {
            if(d.isSameModule(this))
            {
                //Found a circular dependency
                return showPathToDependency(includeVersion);
            }
            d = d.getParent();
        }
        return null;
    }


    public String showPathToDependency(boolean includeVersion)
    {
        StringBuffer backTrace = new StringBuffer(toString(includeVersion));

        DependencyNode d = parent;
        while (d != null)
        {
            backTrace.insert(0, " ~> ");
            if(includeVersion)
                backTrace.insert(0, d.toString(includeVersion));
            d = d.getParent();
        }
        return backTrace.toString();
    }

    public boolean isParentRejected()
    {
        if(parent != null)
            return parent.isRejected();
        return false;
    }

    public boolean isSameModule(DependencyNode node)
    {
        return this.moduleName.equals(node.moduleName);
    }

    public void setParent(DependencyNode parent)
    {
        this.parent = parent;
    }

    public boolean isRejected()
    {
        return rejected;
    }

    public void setRejected(RejectionReason reason, boolean rejected, boolean alsoRejectDependencies)
    {
        this.rejected = rejected;
        this.reason = reason;
        if(alsoRejectDependencies)
            dependencies.forEach(dep -> dep.setRejected(reason, rejected, alsoRejectDependencies));
    }

    public DependencyNode getParent()
    {
        return parent;
    }

    public List<DependencyNode> getDependencies()
    {
        return dependencies;
    }

    public String getModuleName()
    {
        return moduleName;
    }

    public SemanticVersion getVersion()
    {
        return version;
    }

    private String toString(boolean includeVersion)
    {
        if(includeVersion)
            return toString();
        if(rejected)
            return moduleName + " ("+ reason + ")";
        return moduleName;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof DependencyNode)
        {
            DependencyNode dep = (DependencyNode) obj;
            String thisVector = moduleName + "-" + version;
            String otherVector = dep.moduleName + "-" + dep.version;
            return thisVector.equals(otherVector);
        }
        return super.equals(obj);
    }

    @Override
    public String toString()
    {
        if(!rejected)
            return moduleName + "-" + version;
        else
            return moduleName + "-" + version + " ("+ reason + ")";
    }

    public static enum RejectionReason
    {
        //The developer configured this dependency to be rejected in the package directive.
        MANUAL,

        //When the Dependency manager resolved all the dependencies did it find a later version
        // And resolve this version of the dependency away.
        RATIONALISATION,

        //If we find the same module and also the same version then we have it.
        SAME_VERSION,

        //After rationalisation did the Dependency manager then workout that actually this
        //dependency is not needed at any version number, this can happen if lower version
        //numbered dependencies pull in other dependencies, but then the lower version
        //numbered dependency gets rationalised away, leaving a trail of stuff it pulled in
        //that is now no longer needed. You can just reject the dependencies and all it's
        //dependencies directly as those dependencies might be used elsewhere.
        OPTIMISED;

        //But note is something that was rationalised/optimised out did bring in a dependency
        //that it at a higher level than one that is still needed elsewhere then we will use that
        //in preference.
    }
}
