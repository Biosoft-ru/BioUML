package biouml.standard.filter._test;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.standard.filter.BiopolimerDiagramFilter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

/**
 * Batch unit test for biouml.model package.
 */
public class FilterTest extends TestCase
{
    Diagram diagram;

    /** Standart JUnit constructor */
    public FilterTest( String name )
    {
        super(name);
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite ( FilterTest.class.getName() );
        //TODO: GeneNet plugin and module moved to attic, use another diagram with BiopolymerFilter
        //suite.addTest(new FilterTest("testOpenDiagram"));
        //suite.addTest(new FilterTest("testFilterInitialisation"));
        //suite.addTest( new FilterTest( "testReadWrite") );
        return suite;
    }

    @Override
    public void setUp() throws Exception
    {
        String path = "../data";
        DataCollection<?> repository = CollectionFactory.createRepository( path );
        assertNotNull( "repository", repository );

        diagram = (Diagram)CollectionFactory.getDataCollection( "databases/GeneNet/Diagrams/Antiviral response" );
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testOpenDiagram() throws Exception
    {
        assertNotNull("diagram", diagram);
    }

    public void testFilterInitialisation() throws Exception
    {
        long creationTime  = System.currentTimeMillis();
        BiopolimerDiagramFilter filter = new BiopolimerDiagramFilter(diagram);
        creationTime  = System.currentTimeMillis() - creationTime;

        checkFilter( filter );
    }

    private void checkFilter(BiopolimerDiagramFilter filter)
    {
        // test species initialisation
        String[] speciesList = filter.getDiagramSpeciesList();
        assertEquals( "species list size", 3, speciesList.length );
        assertEquals( "species list: Gallus gallus", "Gallus gallus", speciesList[0] );
        assertEquals( "species list: Homo sapiens", "Homo sapiens", speciesList[1] );
        assertEquals( "species list: Mus musculus", "Mus musculus", speciesList[2] );

        // test cell types initialisation
        String[] cellTypesList = filter.getDiagramCellTypesList();
        assertEquals( "cell types list", 28, cellTypesList.length );
        assertEquals( "cell type [0]: Gg:fibroblast", "Gg:fibroblast", cellTypesList[0] );
        assertEquals( "cell type [26]: Mm:thymus", "Mm:thymus", cellTypesList[26] );

        // test inducers initialisation
        String[] inducersList = filter.getDiagramInducersList();
        assertEquals( "inducers list", 35, inducersList.length );
        assertEquals( "inducer [0]:  GM-CSF", "GM-CSF", inducersList[0] );
        assertEquals( "inducer [34]: virus infection (NDV)", "virus infection (NDV)", inducersList[34] );
    }

    public void testReadWrite() throws Exception
    {
        DataCollection<DataElement> dc = DataElementPath.create( "databases/test/Diagrams" ).getDataCollection();
        String filterTestWriteDiagram = "FilterTestDiagram";
        Diagram d2 = diagram.clone( dc, filterTestWriteDiagram );
        BiopolimerDiagramFilter filter = new BiopolimerDiagramFilter( d2 );
        d2.setFilterList( new DiagramFilter[] {filter} );
        d2.setDiagramFilter( filter );
        d2.save();

        d2 = (Diagram)CollectionFactory.getDataCollection( "databases/test/Diagrams/" + filterTestWriteDiagram );
        assertNotNull( "Diagram copy", d2 );
        assertEquals( BiopolimerDiagramFilter.class, d2.getFilter().getClass() );
        BiopolimerDiagramFilter readFilter = (BiopolimerDiagramFilter)d2.getFilter();
        checkFilter( readFilter );

        dc.remove( filterTestWriteDiagram );
    }

}