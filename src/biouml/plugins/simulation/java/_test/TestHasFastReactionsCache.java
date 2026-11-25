package biouml.plugins.simulation.java._test;

import java.beans.PropertyChangeEvent;
import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.Util;
import biouml.standard.type.Reaction;

/**
 * Tests the per-engine cache of {@code Util.hasFastReactions(diagram)} in
 * {@link JavaSimulationEngine}: the cached answer must match the direct diagram scan and must
 * be invalidated by the same signals (diagram property change, setDiagram) that invalidate the
 * generated model code.
 */
public class TestHasFastReactionsCache extends TestCase
{
    public TestHasFastReactionsCache(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestHasFastReactionsCache.class.getName());
        suite.addTest(new TestHasFastReactionsCache("testCacheTracksDiagramChanges"));
        return suite;
    }

    public void testCacheTracksDiagramChanges() throws Exception
    {
        File file = new File("./biouml/plugins/simulation/java/_test/TYSON2.XML");
        assertTrue("Can not find file: " + file.getCanonicalPath(), file.exists());
        Diagram diagram = SbmlModelFactory.readDiagram(file, null, null);

        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram(diagram);

        boolean initial = Util.hasFastReactions(diagram);
        assertEquals(initial, engine.hasFastReactions());
        // second call is served from the cache and must agree with the direct scan
        assertEquals(initial, engine.hasFastReactions());

        Reaction reaction = diagram.recursiveStream()
                .map(DiagramElement::getKernel)
                .filter(Reaction.class::isInstance)
                .map(Reaction.class::cast)
                .findFirst().orElse(null);
        assertNotNull("TYSON2 fixture must contain a reaction", reaction);

        // Flipping a reaction's fast flag fires a property change that propagates from the
        // role through the diagram to the engine's listener, invalidating the cache, so the
        // very next call must already see the new value.
        reaction.setFast(!initial);
        assertEquals(!initial, engine.hasFastReactions());

        reaction.setFast(initial);
        assertEquals(initial, engine.hasFastReactions());

        // An explicit diagram property change (what the engine's diagram listener forwards
        // on edits) must invalidate the cache too.
        reaction.setFast(!initial);
        engine.propertyChange(new PropertyChangeEvent(diagram, "role", null, diagram.getRole()));
        assertEquals(!initial, engine.hasFastReactions());

        // setDiagram must invalidate the cache as well.
        reaction.setFast(initial);
        engine.setDiagram(diagram);
        assertEquals(initial, engine.hasFastReactions());
    }
}
