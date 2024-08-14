package biouml.plugins.antimony._test;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.antimony.AntimonyEditor;
import biouml.standard.type.Reaction;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class GenerateDiagramTest extends AbstractBioUMLTest
{
    final static DataElementPath COLLECTION_NAME = DataElementPath.create("databases/test/Antimony/Diagrams");

    final static String DIAGRAM_NAME = "newDiagramTest";

    Diagram diagram;
    AntimonyEditor editor;
    String antimonyText;

    public GenerateDiagramTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(GenerateDiagramTest.class.getName());
        suite.addTest(new GenerateDiagramTest("addCompartment"));
        suite.addTest(new GenerateDiagramTest("addSpecie"));
        suite.addTest(new GenerateDiagramTest("addReaction"));
        suite.addTest(new GenerateDiagramTest("removeCompartment"));
        suite.addTest(new GenerateDiagramTest("removeSpecie"));
        suite.addTest(new GenerateDiagramTest("removeReaction"));

        return suite;
    }

    public void addCompartment() throws Exception
    {
        preprocess();

        antimonyText = antimonyText.replaceAll("end", "   compartment newComp;\nend");

        editor.setText(antimonyText);
        diagram = editor.getAntimony().generateDiagram(editor.getText());
        Compartment comp = (Compartment)diagram.findNode("newComp");
        assertTrue(comp != null);
    }

    public void addSpecie() throws Exception
    {
        preprocess();

        antimonyText = antimonyText.replaceAll("end", "   species newS in comp;\nend");

        editor.setText(antimonyText);
        diagram = editor.getAntimony().generateDiagram(editor.getText());
        Node specie = diagram.findNode("newS");
        assertTrue(specie != null);
        assertTrue(specie.getCompartment().getName().equals("comp"));
    }

    public void addReaction() throws Exception
    {
        preprocess();

        antimonyText = antimonyText.replaceAll("end", "   newReaction: s2 -> s1; 3;\r\nend");

        editor.setText(antimonyText);
        diagram = editor.getAntimony().generateDiagram(editor.getText());
        Node reactionNode = diagram.findNode("newReaction");
        assertTrue(reactionNode != null);
        assertTrue(reactionNode.getName().equals("newReaction"));
        assertTrue( ( (Reaction)reactionNode.getKernel() ).getFormula().equals("3"));
    }

    public void removeCompartment() throws Exception
    {
        preprocess();

        Node comp = diagram.findNode("comp");
        Node s1 = diagram.findNode("s1");
        Node reaction = diagram.findNode("R000003");
        assertTrue(comp != null);
        assertTrue(s1 != null);
        assertTrue(reaction != null);

        antimonyText = AntimonyTestUtil.clean(antimonyText);

        String toReplace = "compartment comp;\nspecies s1 in comp, s2 in comp;\nvar c, g, m;\ncomp = 1.0 litre;\ns2 = 8.0 / comp;\n";
        antimonyText = antimonyText.replace(toReplace, "");
        antimonyText = antimonyText.replace("comp, s2, ", "");
        antimonyText = antimonyText.replace("@sbgn s1.type = \"unspecified\";\n@sbgn s2.type = \"unspecified\";", "");
        antimonyText = antimonyText.replace("\nR000003: s1 => s2; g*(c+m);", "");

        editor.setText(antimonyText);

        diagram = editor.getAntimony().generateDiagram(editor.getText());
        comp = diagram.findNode("comp");
        s1 = diagram.findNode("s1");
        reaction = diagram.findNode("R000003");

        assertTrue(comp == null);
        assertTrue(s1 == null);
        assertTrue(reaction == null);
    }

    public void removeSpecie() throws Exception
    {
        preprocess();

        Node s2 = diagram.findNode("s2");
        Node reaction = diagram.findNode("R000003");
        assertTrue(s2 != null);
        assertTrue(reaction != null);

        antimonyText = antimonyText.replace(", s2 in comp", "");
        antimonyText = antimonyText.replace("   s2 = 8.0;\n", "");
        antimonyText = antimonyText.replace("   @sbgn s2.type = \"unspecified\";\n", "");
        antimonyText = antimonyText.replace("\n   R000003: s1 => s2; g*(c+m);", "");

        editor.setText(antimonyText);

        diagram = editor.getAntimony().generateDiagram(editor.getText());
        s2 = diagram.findNode("s2");
        reaction = diagram.findNode("R000003");
        assertTrue(s2 == null);
        assertTrue(reaction == null);
    }

    public void removeReaction() throws Exception
    {
        preprocess();

        Node reaction = diagram.findNode("R000003");
        assertTrue(reaction != null);

        antimonyText = antimonyText.replace("\n   R000003: s1 => s2; g*(c+m);", "");

        editor.setText(antimonyText);
        diagram = editor.getAntimony().generateDiagram(editor.getText());
        reaction = diagram.findNode("R000003");
        assertTrue(reaction == null);
    }

    protected void preprocess() throws Exception
    {
        String repositoryPath = "../data";
        CollectionFactory.createRepository(repositoryPath);
        diagram = COLLECTION_NAME.getChildPath(DIAGRAM_NAME).getDataElement(Diagram.class);
        editor = new AntimonyEditor();
        editor.explore(diagram, null);

        antimonyText = editor.getText();
    }
}
