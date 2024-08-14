package biouml.plugins.kegg.access._test;

import biouml.model.Diagram;
import biouml.model.Module;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class TestDiagramReader extends AbstractBioUMLTest
{

    public TestDiagramReader(String name)
    {
        super(name);
    }

    public void testRead_map00010() throws Exception
    {
        DataCollection repository = CollectionFactory.createRepository("../data");
        assertNotNull( "Can't create repository",repository );
        Module module = (Module)repository.get("KEGG");
        assertNotNull( "Can't find module 'KEGG'",module );

        Diagram diagram = module.getDiagram("map00010.xml");
        
        // check diagram
        assertNotNull( "Diagram shouldn't be null",diagram );
    }
}