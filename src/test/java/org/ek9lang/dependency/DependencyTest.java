package org.ek9lang.dependency;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A range of different tests - could probably do with a lot more as this is a somewhat complex area.
 *
 * There are a couple of critical points here.
 * 1. Detect circular dependencies - that's show stopper
 * 2. Detect semantic version violations, i.e same module required at major v4 and major v3 - show stopper
 * 3. Apply developer defines exclusions first as that rules out loads of dependencies.
 * 4. In rationalisation always pick the highest semantic version of a dependency -
 *      even if that is a transitive dependency and cause of transitive is not included in final set.
 * 5. In final optimisation - reject dependencies that can not longer be reached in tree from root.
 * 6. Run optimisation multiple times as nodes can be in various orders and a path that was open can
 *      subsequently be rejected meaning more dependencies can be rejected.
 *
 * Finally we're aiming for the least number of dependencies that are all minor,patch,build number compatible.
 * If a developer releases a module package that does break backwards compatibility without bumping major version
 * The compiler will find that out and builds will fail, that module package will be useless.
 */
public class DependencyTest
{
    @Test
    public void testEmptyDependencies()
    {
        DependencyNode n = DependencyNode.of("a.b.c-1.0.0-0");
        DependencyManager underTest = new DependencyManager(n);
        assertTrue(underTest.reportCircularDependencies().size() == 0);
    }

    @Test
    public void testSingleLayerDependencies()
    {
        DependencyNode root = DependencyNode.of("a.b.c-1.0.0-0");

        root.addDependency(DependencyNode.of("a.b.d-1.0.0-0"));
        root.addDependency(DependencyNode.of("a.b.e-1.0.0-0"));
        root.addDependency(DependencyNode.of("a.b.f-1.0.0-0"));

        DependencyManager underTest = new DependencyManager(root);
        assertTrue(underTest.reportCircularDependencies().size() == 0);
    }

    @Test
    public void testTwoLayerDependencies()
    {
        DependencyNode root = DependencyNode.of("a.b.c-1.0.0-0");

        DependencyNode d1 = DependencyNode.of("a.b.d-1.0.0-0");
        DependencyNode d2 = DependencyNode.of("a.b.e-1.0.0-0");
        DependencyNode d3 = DependencyNode.of("a.b.f-1.0.0-0");

        root.addDependency(d1);
        root.addDependency(d2);
        root.addDependency(d3);

        d1.addDependency(DependencyNode.of("a.z.d-1.0.0-0"));
        d2.addDependency(DependencyNode.of("a.z.e-1.0.0-0"));
        d3.addDependency(DependencyNode.of("a.z.f-1.0.0-0"));

        DependencyManager underTest = new DependencyManager(root);
        assertTrue(underTest.reportCircularDependencies().size() == 0);
    }

    @Test
    public void testExcludeDependencies()
    {
        DependencyNode root = DependencyNode.of("a.b.c-1.0.0-0");
        //Record the fact that this will be a rejection for this configuration.
        //Though it still has to be applied -- see later.
        root.addDependencyRejection("a.b.f", "a.z.e");
        
        DependencyNode d1 = DependencyNode.of("a.b.d-1.0.0-0");
        root.addDependency(d1);
        DependencyNode d2 = DependencyNode.of("a.b.e-1.0.0-0");
        root.addDependency(d2);
        DependencyNode d3 = DependencyNode.of("a.b.f-1.0.0-0");
        root.addDependency(d3);

        DependencyNode c1 = DependencyNode.of("a.z.e-1.0.0-0");
        d2.addDependency(c1);
        DependencyNode c2 = DependencyNode.of("b.z.e-1.0.0-0");
        c1.addDependency(c2);
        c2.addDependency(DependencyNode.of("a.b.f-1.2.0-40"));

        //First check no circulars
        DependencyManager underTest = new DependencyManager(root);
        List<String> circulars = underTest.reportCircularDependencies();
        assertTrue(circulars.size() == 0);

        //find root
        List<DependencyNode> found = underTest.findByModuleName("a.b.c");
        assertTrue(found.size() == 1);
        assertTrue(found.get(0).getModuleName().equals("a.b.c"));

        //So expect the two
        found = underTest.findByModuleName("a.b.f");
        assertTrue(found.size() == 2);
        found.forEach(node -> {
            assertFalse(node.isRejected());
        });

        //Now a.b.f-1.2.0-40 needs to be excluded but only when pulled in by a.z.e
        //We want to stay on version 1.0.0-0 for some reason.
        
        List<DependencyNode> allDependencies = underTest.reportAcceptedDependencies();
		allDependencies.forEach(dep -> {
			Map<String, String> rejections = dep.getDependencyRejections();
			rejections.keySet().forEach(key -> {
				String moduleName = key ;
				String whenDependencyOf = rejections.get(key);				
				System.err.println("Resolve: Exclusion '" + moduleName + "' <- '" + whenDependencyOf + "'");
				underTest.reject(moduleName, whenDependencyOf);
			});
		});
        
		//There will still be two but one will now be marked as rejected.
        found = underTest.findByModuleName("a.b.f");
        assertTrue(found.size() == 2);
        
        System.out.println("testExcludeDependencies");
        System.out.println("Modules");
        found.forEach(System.out::println);
        
        assertTrue(found.get(0).isRejected());
        assertFalse(found.get(1).isRejected());
        
        assertFalse(underTest.optimise());
        
        System.out.println("Accepted after Optimisation");
        underTest.reportAcceptedDependencies().forEach(accept -> {
            System.out.println(accept.showPathToDependency(true));
        });
    }

    @Test
    public void testSameDependencies()
    {
        DependencyNode root = DependencyNode.of("a.b.c-1.0.0-0");

        DependencyNode d1 = DependencyNode.of("a.b.d-1.0.0-0");
        DependencyNode d2 = DependencyNode.of("a.b.e-1.2.4-21");
        DependencyNode d3 = DependencyNode.of("a.b.f-1.0.0-0");

        root.addDependency(d1);
        root.addDependency(d2);
        root.addDependency(d3);

        d3.addDependency(DependencyNode.of("a.b.d-1.2.0-0"));

        DependencyNode checker = DependencyNode.of("a.b.e-1.2.4-20");
        d3.addDependency(checker);
        //Note this still gets included even though above is rejected, because this is a compatible higher version.
        checker.addDependency(DependencyNode.of("a.b.d-1.2.4-0"));

        DependencyManager underTest = new DependencyManager(root);
        underTest.rationalise();
        List<DependencyNode> rejected = underTest.reportRejectedDependencies();
        List<DependencyNode> accepted = underTest.reportAcceptedDependencies();        
        assertTrue(rejected.size() == 3);
        assertTrue(accepted.size() == 4);
        
        /*
        System.out.println("testSameDependencies");
        System.out.println("Rejected");
        rejected.forEach(System.out::println);
        System.out.println("Accepted");
        accepted.forEach(System.out::println);
        */
    }

    @Test
    public void testMajorVersionIncompatibleDependencies()
    {
        DependencyNode root = DependencyNode.of("a.b.c-1.0.0-0");

        DependencyNode d1 = DependencyNode.of("a.b.d-1.9.0-0");
        DependencyNode d2 = DependencyNode.of("a.b.e-1.2.4-21");
        DependencyNode d3 = DependencyNode.of("a.b.f-1.0.0-0");

        root.addDependency(d1);
        root.addDependency(d2);
        root.addDependency(d3);

        //Yes this version is higher than 1.9.0-0  - but it's major version of 2 means not compatible.
        d3.addDependency(DependencyNode.of("a.b.d-2.2.0-0"));
        DependencyManager underTest = new DependencyManager(root);

        //Uncomment to avoid the breach
        //underTest.reject("a.b.d", "a.b.c");

        underTest.rationalise();
        List<DependencyNode> rejected = underTest.reportRejectedDependencies();
        List<DependencyNode> accepted = underTest.reportAcceptedDependencies();
        List<DependencyNode> breaches = underTest.reportStrictSemanticVersionBreaches();
        
        assertTrue(rejected.size() == 1);
        assertTrue(accepted.size() == 4);
        assertTrue(breaches.size() == 1);
        
        //So even though "a.b.d-2.2.0-0" is in the accepted list it is also in the breaches - so must be addressed
        /*
        System.out.println("testMajorVersionIncompatibleDependencies");
        System.out.println("Rejected");
        rejected.forEach(System.out::println);
        System.out.println("Accepted");
        accepted.forEach(System.out::println);

        System.out.println("Breaches");
        breaches.forEach(System.out::println);
        */
    }

    @Test
    public void testCircularDependencies()
    {
        DependencyNode root = DependencyNode.of("a.b.c-1.0.0-0");

        DependencyNode d1 = DependencyNode.of("a.b.d-1.0.0-0");
        DependencyNode d3 = DependencyNode.of("a.b.f-1.0.0-0");

        root.addDependency(d1);
        root.addDependency(d3);

        DependencyNode d2 = DependencyNode.of("a.b.e-1.0.0-0");
        root.addDependency(d2);
        DependencyNode c1 = DependencyNode.of("a.z.e-1.0.0-0");
        d2.addDependency(c1);
        DependencyNode c2 = DependencyNode.of("b.z.e-1.0.0-0");
        c1.addDependency(c2);
        c2.addDependency(DependencyNode.of("a.b.e-1.0.0-0"));

        d1.addDependency(DependencyNode.of("a.z.d-1.0.0-0"));
        DependencyNode circCause = DependencyNode.of("a.z.f-1.0.0-0");
        circCause.addDependency(DependencyNode.of("a.b.f-1.0.0-0"));
        d3.addDependency(circCause);

        DependencyManager underTest = new DependencyManager(root);
        
        List<String> deps = underTest.reportAllDependencies();
        assertEquals(deps.size(),  10);
        
        deps = underTest.listAllModuleNames();
        assertEquals(deps.size(),  8);

        List<String> circulars = underTest.reportCircularDependencies(true);
        assertEquals(2, circulars.size());
        
        /*
        System.out.println("testCircularDependencies");
        System.out.println("All");
        deps.forEach(System.out::println);
        System.out.println("----");

        System.out.println("Uniq");
        deps = underTest.listAllModuleNames();
        deps.forEach(System.out::println);
        System.out.println("----");
        System.out.println("Circulars");
        assertTrue(circulars.size() == 2);
        circulars.forEach(System.out::println);
        */
    }

    /**
     * This is a contrived test to build a structure in a specific way
     * then mark a couple of key parts (dependencies) as rejected
     * and then check that the optimise mechanism can find all nodes under those
     * key dependencies and mark those as rejected.
     *
     * In general optimise would always be the last phase of dependency management
     * but we need to check it can handle a range of scenarios.
     */
    @Test
    public void testJustDependencyOptimisation()
    {
        //As the dependency manager gets the nodes in alphabetical order for processing
        //We need to build our test tree with the understanding.
        //We will build a multi-layer tree with the 'a' at the leaves and the 'z' at the root.

        //We will then run optimise several times, but also set nodes in the middle part of the
        //tree to rejected (manually), then try optimise and see if multiple runs will in effect
        //optimise away all the dependencies under the ones we hae rejected.

        //This might seem strange as we could just traverse down the nodes, but the point here
        //Is that a real tree can have nodes with same module but different versions, so it's not
        //that simple.

        char[] alphabet = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r' ,'s', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

        DependencyNode root = DependencyNode.of("z.y.x-1.0.0-0");

        //First Layer
        DependencyNode reject1 = DependencyNode.of("t.s.r-1.5.0-0");
        DependencyNode reject2 = DependencyNode.of("n.m.l-1.6.0-0");
        root.addDependency(DependencyNode.of("w.v.u-1.0.0-0"));
        root.addDependency(reject1);
        root.addDependency(DependencyNode.of("q.p.o-1.1.5-0"));
        root.addDependency(reject2);

        //Second layer
        for(int i=1; i<5; i++)
        {
            int patchNo = 3;
            for(DependencyNode dep : root.getDependencies())
            {
                String verString = "-"+i+"."+i+"." + patchNo++ + "-0";
                String module = "k."+ alphabet[i]+"."+alphabet[patchNo];
                DependencyNode secondLayer = DependencyNode.of(module + verString);
                dep.addDependency(secondLayer);
                for(int j=4; j<6; j++)
                {
                    verString = "-"+(i+j)+"."+(i*j)+"." + patchNo++ + "-" +j;
                    module = "h."+ alphabet[i+j]+"."+alphabet[patchNo];
                    DependencyNode thirdLayer = DependencyNode.of(module + verString);
                    secondLayer.addDependency(thirdLayer);

                    verString = "-"+(i+j)+"."+(i*j)+"." + patchNo++ + "-" +j;
                    module = "e."+ alphabet[patchNo]+"."+alphabet[patchNo];
                    thirdLayer.addDependency(DependencyNode.of(module + verString));
                }
            }
        }

        DependencyManager underTest = new DependencyManager(root);
        assertRejectionsAcceptance(underTest, 0, 85);

        //We don't expect any changes.
        assertFalse(underTest.optimise());

        //Now lets reject high in the tree - but not the dependencies.
        reject1.setRejected(DependencyNode.RejectionReason.MANUAL, true, false);
        assertRejectionsAcceptance(underTest, 1, 84);

        boolean didOptimise = underTest.optimise();
        assertTrue(didOptimise);
        assertRejectionsAcceptance(underTest, 5, 80);

        //So note how we had to run optimise twice here
        didOptimise = underTest.optimise();
        assertTrue(didOptimise);
        assertRejectionsAcceptance(underTest, 13, 72);

        //And again - so the deeper the tree the more times optimise needs to be called
        //until it's worked it's way right down - but this is contrived because I've made
        //the processing and structure of the tree such that it processes the data in a certain way
        //In real life the tree would be in a very different structure fat bit's/long tails etc.
        didOptimise = underTest.optimise();
        assertTrue(didOptimise);
        assertRejectionsAcceptance(underTest, 21, 64);

        //OK so that's then end - no more
        didOptimise = underTest.optimise();
        assertFalse(didOptimise);

        //But now lets reject number 2!
        reject2.setRejected(DependencyNode.RejectionReason.MANUAL, true, false);
        //now optimise
        didOptimise = underTest.optimise();
        assertTrue(didOptimise);
        assertRejectionsAcceptance(underTest, 26, 59);

        //Again no change just optimise again
        didOptimise = underTest.optimise();
        assertTrue(didOptimise);
        assertRejectionsAcceptance(underTest, 34, 51);

        didOptimise = underTest.optimise();
        assertTrue(didOptimise);
        assertRejectionsAcceptance(underTest, 42, 43);

        //OK so that's then end - no more
        didOptimise = underTest.optimise();
        assertFalse(didOptimise);

        //Now reject root!
        root.setRejected(DependencyNode.RejectionReason.MANUAL, true, false);
        while(underTest.optimise());
        //We should have rejected everything now
        assertRejectionsAcceptance(underTest, 85, 0);

    }

    private void assertRejectionsAcceptance(DependencyManager underTest, int expectNumRejected, int expectNumAccepted)
    {
        List<DependencyNode> rejected = underTest.reportRejectedDependencies();
        List<DependencyNode> accepted = underTest.reportAcceptedDependencies();

        assertEquals(expectNumRejected, rejected.size());
        assertEquals(expectNumAccepted, accepted.size());

        /*
        System.out.println("Rejected");
        rejected.forEach(System.out::println);
        System.out.println("Accepted");
        accepted.forEach(System.out::println);
        */        
    }


    /**
     * Expect final set of dependencies to be:
     * a.b.c-1.0.0-0
     * a.b.d-1.0.0-0
     * a.b.e-1.0.0-0
     * a.b.f-1.0.0-0
     * c.d.e-8.7.1-1 - because manually rejected and c.d.e when pulled in by p.q.r
     * j.k.l-2.7.3-23 - because used in two of the p.q.r deps
     * l.m.n-1.4.3-3 - because used in p.q.r and even though the dep that had this version was removed we pick highest
     * p.q.r-9.9.1-6 - because higher version than p.q.r-9.8.1-6 and p.q.r-9.8.8-1
     * q.r.t-1.6.0-0
     * v.w.x-10.10.10-10 - because used in p.q.r that we did select
     * x.y.z-2.9.1-4 - because transient and higher than x.y.z-2.8.0-6
     *
     * Now what we rejected
     * c.d.e-8.8.1-1 (MANUAL)
     * c.d.e-8.8.0-10 (MANUAL)
     * j.k.l-2.7.3-23 (SAME_VERSION) - because we found the same module and version in more than one place - so rejected one.
     * l.m.n-1.4.3-2 (RATIONALISATION) - higher version found.
     * p.q.r-9.8.8-1 (RATIONALISATION) - higher version p.q.r-9.9.1-6
     * p.q.r-9.8.1-6 (RATIONALISATION) - higher version p.q.r-9.9.1-6
     * x.y.z-2.8.0-6 (RATIONALISATION) - higher version x.y.z-2.9.1-4
     *
     * n.m.p-2.8.8-10 (OPTIMISED) - because p.q.r-9.8.1-6 and p.q.r-9.8.8-1 were rationalised and n.m.p was only used in those.
     * s.t.u-12.3.1-12 (OPTIMISED) - because only used in p.q.r-9.8.1-6 and that was rationalised.
     */
    @Test
    public void testDependencyOptimisation()
    {
        DependencyNode root = DependencyNode.of("a.b.c-1.0.0-0");

        DependencyNode d1 = DependencyNode.of("a.b.d-1.0.0-0");
        DependencyNode d2 = DependencyNode.of("a.b.e-1.0.0-0");
        DependencyNode d3 = DependencyNode.of("a.b.f-1.0.0-0");
        DependencyNode d4 = DependencyNode.of("q.r.t-1.6.0-0");

        root.addDependency(d1);
        root.addDependency(d2);
        root.addDependency(d3);
        root.addDependency(d4);

        //Ok so idea is that there are two versions of the same module
        //Obviously highest version will win out.
        //But each of those version brings in some transitive dependencies
        //We need to ensure we end up with the minimal viable et of those transitive dependencies.

        //So here is a dependency that we need via a single path
        DependencyNode outlier = DependencyNode.of("x.y.z-2.8.0-6");
        DependencyNode preferredVersion = new DependencyNode("c.d.e", "8.7.1-1");
        d1.addDependency(outlier);
        d1.addDependency(preferredVersion);

        //Now the two dependencies that have different transitive dependencies.
        //Only one of these will win out, through rationalisation

        DependencyNode rationalise1 = DependencyNode.of("p.q.r-9.8.1-6");
        d2.addDependency(rationalise1);
        //This dependency is only ever used in p.q.r-9.8.1-6 - so if that is reject then we don't need this.
        rationalise1.addDependency(DependencyNode.of("s.t.u-12.3.1-12"));
        //This is used in two versions of 'p.q.r'
        rationalise1.addDependency(DependencyNode.of("j.k.l-2.7.3-23"));
        //This is also used in both version of 'p.q.r' - but rationalise2 actually uses a earlier version.
        rationalise1.addDependency(DependencyNode.of("l.m.n-1.4.3-3"));
        //note x.y.z is not used in later version of "p.q.r" but is still used in "a.b.d" above
        rationalise1.addDependency(DependencyNode.of("x.y.z-2.9.1-4"));
        rationalise1.addDependency(DependencyNode.of("c.d.e-8.8.0-10"));
        rationalise1.addDependency(DependencyNode.of("n.m.p-2.8.0-10"));

        DependencyNode rationalise3 = DependencyNode.of("p.q.r-9.8.8-1");
        d4.addDependency(rationalise3);
        rationalise3.addDependency(DependencyNode.of("n.m.p-2.8.8-10"));

        //This one will win out.
        DependencyNode rationalise2 = DependencyNode.of("p.q.r-9.9.1-6");
        d3.addDependency(rationalise2);
        rationalise2.addDependency(DependencyNode.of("j.k.l-2.7.3-23"));
        //note only build two used here.
        rationalise2.addDependency(DependencyNode.of("l.m.n-1.4.3-2"));
        //This is only ever used in rationalise2
        rationalise2.addDependency(DependencyNode.of("v.w.x-10.10.10-10"));
        //This is also used in "a.b.d" but an alder version and that what we'd prefer to use
        rationalise2.addDependency(DependencyNode.of("c.d.e-8.8.1-1"));

        //OK so that's the scene
        DependencyManager underTest = new DependencyManager(root);

        List<String> deps = underTest.reportAllDependencies();
        List<DependencyNode> rejected = underTest.reportRejectedDependencies();
        List<String> circulars = underTest.reportCircularDependencies();
        List<String> uniqueDepNames = underTest.listAllModuleNames();

        assertEquals(21, deps.size());
        assertEquals(0, rejected.size());
        //There should be no circular references in what we have defined
        assertEquals(0, circulars.size());
        //Unique names before exclusions
        assertEquals(13, uniqueDepNames.size());
        
        /*
        System.out.println("testDependencyOptimisation");
        System.out.println("All module names");
        deps.forEach(System.out::println);
        
        System.out.println("Unique module names");
        uniqueDepNames.forEach(System.out::println);
        System.out.println("Rejected");
        rejected.forEach(System.out::println);
        */
        
        //So lets manually exclude c.d.e
        underTest.reject("c.d.e", "p.q.r");
        
        /*
        System.out.println("After manually removing 'c.d.e' when dependency of 'p.q.r'");
        uniqueDepNames = underTest.listAllModuleNames();
        System.out.println("Unique module names");
        uniqueDepNames.forEach(System.out::println);
        
        rejected = underTest.reportRejectedDependencies();
        assertEquals(2, rejected.size());
        
        System.out.println("Rejected");
        rejected.forEach(System.out::println);
        */

        //OK now to rationalise.
        underTest.rationalise();

        /*
        System.out.println("After rationalising");
        uniqueDepNames = underTest.listAllModuleNames();
        System.out.println("Unique module names");
        uniqueDepNames.forEach(System.out::println);
        */

        rejected = underTest.reportRejectedDependencies();
        assertEquals(8, rejected.size());
        List<DependencyNode> accepted = underTest.reportAcceptedDependencies();
        
        /*
        System.out.println("Rejected");
        rejected.forEach(reject -> {
            System.out.println(reject.showPathToDependency(true));
        });

        System.out.println("Accepted");
        accepted.forEach(accept -> {
            System.out.println(accept.showPathToDependency(true));
        });
        */

        //So there will be some transitive dependencies pulled in that we now no longer need
        //We can optimise those away now.
        //For example s.t.u is only every used in p.q.r-9.8.1-6 and that has been rationalised.
        //n.m.p is used in p.q.r-9.8.8-1 and p.q.r-9.8.1-6 - both rationalised.
        //So those could and should be removed.
        //System.out.println("Optimising");
        while(underTest.optimise());

        rejected = underTest.reportRejectedDependencies();
        assertEquals(10, rejected.size());
        accepted = underTest.reportAcceptedDependencies();
        assertEquals(11, accepted.size());
        
        System.out.println("Rejected after Optimisation");
        rejected.forEach(reject -> {
            System.out.println(reject.showPathToDependency(true));
        });

        System.out.println("Accepted after Optimisation");
        accepted.forEach(accept -> {
            System.out.println(accept.showPathToDependency(false));
        });

        System.out.println("Final set of Dependencies");
        accepted.forEach(accept -> {
            System.out.println(accept.toString());
        });
        System.out.println("Final Rejections");
        rejected.forEach(reject -> {
            System.out.println(reject.toString());
        });
        

        assertPresent("a.b.c-1.0.0-0", accepted);
        assertPresent("a.b.d-1.0.0-0", accepted);
        assertPresent("a.b.e-1.0.0-0", accepted);
        assertPresent("a.b.f-1.0.0-0", accepted);
        assertPresent("c.d.e-8.7.1-1", accepted);
        assertPresent("j.k.l-2.7.3-23", accepted);
        assertPresent("l.m.n-1.4.3-3", accepted);
        assertPresent("p.q.r-9.9.1-6", accepted);
        assertPresent("q.r.t-1.6.0-0", accepted);
        assertPresent("v.w.x-10.10.10-10", accepted);
        assertPresent("x.y.z-2.9.1-4", accepted);

        assertEquals(10, rejected.size());
        assertPresent("c.d.e-8.8.1-1", rejected);
        assertPresent("c.d.e-8.8.0-10", rejected);
        assertPresent("j.k.l-2.7.3-23", rejected);
        assertPresent("l.m.n-1.4.3-2", rejected);
        assertPresent("n.m.p-2.8.8-10", rejected);
        assertPresent("n.m.p-2.8.0-10", rejected);
        assertPresent("p.q.r-9.8.8-1", rejected);
        assertPresent("p.q.r-9.8.1-6", rejected);
        assertPresent("s.t.u-12.3.1-12", rejected);
        assertPresent("x.y.z-2.8.0-6", rejected);
    }

    private void assertPresent(String dependencyName, Collection<DependencyNode> dependencies)
    {
        assertTrue(dependencies.contains(DependencyNode.of(dependencyName)));
    }


}
