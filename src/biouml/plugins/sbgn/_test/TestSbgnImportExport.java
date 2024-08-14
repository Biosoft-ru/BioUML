package biouml.plugins.sbgn._test;

import java.io.File;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.SubDiagram;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbml.SbmlImporter;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestSbgnImportExport extends AbstractBioUMLTest
{

    public static final String OUTPUT_COLLECTION = "data/SBML/Diagarms";
    public TestSbgnImportExport(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestSbgnImportExport.class.getName());
        suite.addTest(new TestSbgnImportExport("testImport"));
        suite.addTest(new TestSbgnImportExport("testOpen"));
        return suite;
    }

    public void testImport() throws Exception
    {
        String repositoryPath = "../data/test/biouml/plugins/sbgn/SBGN diagrams";
        String diagramName = "01130-sbml-l3v1";
        DataCollection<?> repository = CollectionFactory.createRepository(repositoryPath);
        DataCollection<?> diagramCol = (DataCollection<?>)repository.get("Diagrams");

        DataElement de = diagramCol.get(diagramName);

        if( de != null )
        {
            diagramCol.remove(diagramName);
            System.out.println("Existing diagram removed.");
        }

        SbmlImporter importer = new SbmlImporter();
        DataElement element = importer.doImport( repository, new File( "../data/test/biouml/plugins/sbgn/Models/" + diagramName + ".xml" ),
                diagramName, null, null );
        assert ( element instanceof Diagram );
        Diagram diagram = (Diagram)element;
        checkDiagram(diagram);
    }

    public void testOpen() throws Exception
    {
        String repositoryPath = "../data/test/biouml/plugins/sbgn/SBGN diagrams";
        String diagramName = "01130-sbml-l3v1";
        DataCollection<?> repository = CollectionFactory.createRepository(repositoryPath);
        DataCollection<?> diagramCol = (DataCollection<?>)repository.get("Diagrams");
        Diagram diagram = (Diagram)diagramCol.get(diagramName);
        checkDiagram(diagram);
    }

    private void checkDiagram(Diagram diagram)
    {
        assert ( diagram.getType() instanceof SbgnDiagramType );
        Edge connection = diagram.stream( Edge.class ).filter( Util::isUndirectedConnection ).findAny().orElse( null );
        assertNotNull(connection);
        SubDiagram subDiagram = diagram.stream(SubDiagram.class).filter(s -> s.getName().equals("sub2")).findAny().orElse(null);
        assertNotNull(subDiagram);
        Diagram sub2Diagram = subDiagram.getDiagram();
        assert ( sub2Diagram.getType() instanceof SbgnDiagramType );
        State curState = sub2Diagram.getCurrentState();
        assertNotNull(curState);
    }
}
