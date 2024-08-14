package biouml.standard.diagram._test;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.LogManager;

import javax.swing.JTextField;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.diagram.PathwaySemanticController;
import biouml.standard.diagram.ReactionEditPane;
import biouml.standard.diagram.ReactionPane;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import junit.framework.TestSuite;
import one.util.streamex.EntryStream;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.DefaultViewEditorHelper;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class ReactionCreationTest extends AbstractBioUMLTest
{
    static String repositoryPath = "../data";
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        ApplicationUtils.copyFile("../data/test/Data/reaction.dat.tmp", "../data/test/Data/reaction.dat");
    }

    /** Standart JUnit constructor */
    public ReactionCreationTest(String name)
    {
        super(name);

        // Setup log
        File configFile = new File( "./biouml/standard/diagram/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite ( ReactionCreationTest.class.getName() );

        suite.addTest(new ReactionCreationTest("testReaction_1"));
        suite.addTest(new ReactionCreationTest("testReaction_2"));
        suite.addTest(new ReactionCreationTest("testReaction_3"));
        suite.addTest(new ReactionCreationTest("testReaction_4"));
        suite.addTest(new ReactionCreationTest("testReaction_5"));

        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //
    static Module module;
    static Diagram diagram;
    static DataCollection<Reaction> reactionDC;
    private void loadDiagram() throws Exception
    {
        DataCollection<?> repository = CollectionFactory.createRepository(repositoryPath);
        assertNotNull(repository);

        module = DataElementPath.create("databases/test").getDataElement(Module.class);
        diagram = DataElementPath.create("databases/test/Diagrams/reaction").getDataElement(Diagram.class);
        reactionDC = DataElementPath.create("databases/test/Data/reaction").getDataCollection(Reaction.class);
    }

    public static class ReactionInfo
    {
        String[] refs;
        String[] components;
        String[] roles;
        String   nameToBeGenerated;
        String   titleToBeGenerated;

        public ReactionInfo(String[] refs, String[] components, String[] roles,
                            String nameToBeGenerated, String titleToBeGenerated)
        {
            this.refs = refs;
            this.components = components;
            this.roles = roles;
            this.nameToBeGenerated = nameToBeGenerated;
            this.titleToBeGenerated = titleToBeGenerated;
        }
    }

    protected Object invoke(Object obj, String methodName, Class[] classes, Object[] parameters) throws Exception
    {
        Method method = obj.getClass().getDeclaredMethod(methodName, classes);
        method.setAccessible(true);
        return method.invoke(obj, parameters);
    }

    protected Object get(Object obj, String fieldName) throws Exception
    {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    public void createReaction(ReactionInfo info) throws Exception
    {
        ViewEditorPane editorPane = new ViewEditorPane(new DefaultViewEditorHelper());
        ReactionPane reactionPane = new ReactionPane(diagram, diagram, new Point(0, 0), editorPane);

        EntryStream.zip( info.components, info.roles ).forKeyValue( reactionPane::addComponent );

        String generatedName = PathwaySemanticController.generateReactionName( reactionDC );
        JTextField reactionName = (JTextField)get( reactionPane, "reactionName" );
        assertEquals( "Reaction name is incorrect: ", reactionName.getText(), generatedName );

        ReactionEditPane editPane = (ReactionEditPane)get( reactionPane, "reactionEditPane" );

        //String generatedTitle = DiagramUtility.generateReactionTitle( editPane.getComponentList() );
        String reactionTitle = reactionPane.getReactionTitle();
        assertEquals( "Generated reaction title is incorrect: ", info.titleToBeGenerated, reactionTitle );

        JTextField rate = (JTextField)get(editPane, "reactionRate");
        rate.setText("rate_" + generatedName);

        invoke(reactionPane, "createReaction", new Class[] {}, new Object[] {});
        Reaction reaction = reactionDC.get(generatedName);
        assertNotNull("Reaction was not created.", reaction);

        // test the reaction parsing
        reactionDC.release(generatedName);
        reaction = reactionDC.get(generatedName);
        assertNotNull("Reaction was not reloaded.", reaction);

        for(int i=0; i<info.components.length; i++)
        {
            String name = generatedName + "__" + info.refs[i];
            SpecieReference specie = reaction.get(name);
            assertNotNull("Specie reference was not reloaded, specie=" + info.refs[i], specie);

            assertEquals("Incorect role for specie reference " + specie.getName(), info.roles[i], specie.getRole());
        }

        // test diagram reading
        File file = new File("../data/test/Diagrams/reaction.test");
        DiagramXmlWriter writer = new DiagramXmlWriter(new FileOutputStream(file));
        writer.write(diagram);

        DiagramXmlReader.readDiagram(diagram.getName ( ), new FileInputStream(file), null, null, module);
        // tear down
        reactionDC.remove(generatedName);

        diagram.getOrigin().release(diagram.getName());
        diagram = (Diagram)CollectionFactory.getDataCollection("databases/test/Diagrams/reaction");
        assertNotNull("Can not reload diagram", diagram);
    }

    /** simple synthesis reaction: -> sA */
    public void testReaction_1() throws Exception
    {
        loadDiagram();
        ReactionInfo info = new ReactionInfo(
            new String[] {"sA_as_product"},
            new String[] { "cell/membrane/vacuole/sA" },
            new String[] { "product" },
            "R000001",
            "-> sA" );

        createReaction(info);
    }

    /** simple decay reaction: sA -> */
    public void testReaction_2() throws Exception
    {
        loadDiagram();
        ReactionInfo info = new ReactionInfo(
            new String[] {"sA_as_reactant"},
            new String[] { "cell/membrane/vacuole/sA" },
            new String[] { "reactant" },
            "R000001",
            "sA ->" );

        createReaction(info);
    }

    /** simple convertion reaction: sA -> sB */
    public void testReaction_3() throws Exception
    {
        loadDiagram();
        ReactionInfo info = new ReactionInfo(
            new String[] {"sA_as_reactant", "sB_as_product"},
            new String[] { "cell/membrane/vacuole/sA", "cell/membrane/vacuole/sB" },
            new String[] { "reactant", "product" },
            "R000001",
            "sA -> sB" );

        createReaction(info);
    }

    /** catalysis reaction: sA -pA-> sB */
    public void testReaction_4() throws Exception
    {
        loadDiagram();
        ReactionInfo info = new ReactionInfo(
                new String[] {"sA_as_reactant", "pA_as_modifier", "sB_as_product"},
            new String[] { "cell/membrane/vacuole/sA", "cell/membrane/vacuole/pA", "cell/membrane/vacuole/sB" },
            new String[] { "reactant", "modifier", "product" },
            "R000001",
            "sA -pA-> sB" );

        createReaction(info);
    }

    /** catalysis reaction: sA + sB -pA, sX-> sX + sY */
    public void testReaction_5() throws Exception
    {
        loadDiagram();
        ReactionInfo info = new ReactionInfo(
            new String[] {"sA_as_reactant", "sB_as_reactant", "pA_as_modifier",
                          "sX_as_modifier", "sX_as_product", "sY_as_product"},
            new String[] { "cell/membrane/vacuole/sA", "cell/membrane/vacuole/sB",
                           "cell/membrane/vacuole/pA", "cell/membrane/vacuole/sX",
                           "cell/membrane/vacuole/sX", "cell/membrane/vacuole/sY"},
            new String[] { "reactant", "reactant", "modifier", "modifier", "product", "product" },
            "R000001",
            "sA + sB -pA, sX-> sX + sY" );

        createReaction(info);
    }

}
