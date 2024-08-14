package biouml.plugins.reactome._test;

import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.journal.ProjectUtils;

/**
 * @author anna, manikitos
 *
 */
public class ReactomeMatchingHubTest extends AbstractBioUMLTest
{
    private static final String REACTOME_PROP = ProjectUtils.DATABASE_VERSION_PROPERTY_PREFIX + "Reactome";
    private static final String REACTOME_TEST_PROJECT = "Research: Reactome_test";
    public static final String repositoryPath = "../data";
    public static final String projects = "../data_resources";

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( repositoryPath );
        CollectionFactory.createRepository( projects );
        Application.setPreferences( new Preferences() );
        JournalRegistry.setCurrentJournal( REACTOME_TEST_PROJECT );
    }

    private void selectReactomeVersion(String version)
    {
        DataElementPath projectPath = JournalRegistry.getProjectPath( JournalRegistry.getCurrentJournal() );
        assertNotNull( "Cannot select project", projectPath );
        projectPath.getDataCollection().getInfo().getProperties().setProperty( REACTOME_PROP, version );
    }

    public void testMatchingOldFormat() throws Exception
    {
        assertNotNull( "ReactomeUniprotHub registered", BioHubRegistry.getBioHub( "ReactomeUniprotHub (45)" ) );
        selectReactomeVersion( "45" );

        String[][] tests = {
                {"REACT_10143", "Proteins: UniProt", "Homo sapiens", "O14893"},
                {"P25942", "Proteins: Reactome", "Homo sapiens", "REACT_11508"},
                {"P62994", "Proteins: Reactome", "Rattus norvegicus", "REACT_4608"}
        };
        
        for(String[] test: tests)
            checkSingle( test );

        //Matching one-to-many
        String[] testMulti = {"O14893", "Proteins: Reactome", "Homo sapiens", "REACT_10171", "REACT_10143"};
        checkMulti( testMulti );
    }

    public void testMatching() throws Exception
    {
        assertNotNull( "ReactomeUniprotHub registered", BioHubRegistry.getBioHub( "ReactomeUniprotHub (63)" ) );
        selectReactomeVersion( "63" );
        
        String[][] tests = {
                {"R-HSA-191835", "Proteins: UniProt", "Homo sapiens", "O14893"},
                {"P25942", "Proteins: Reactome", "Homo sapiens", "R-HSA-198117"},
                {"P00505", "Proteins: Reactome", "Rattus norvegicus", "R-RNO-70593"}
        };

        for( String[] test : tests )
            checkSingle( test );

        //Matching one-to-many
        String[] testMulti = {"O14893", "Proteins: Reactome", "Homo sapiens", "R-HSA-191795", "R-HSA-191835"};
        checkMulti( testMulti );
    }

    private void checkSingle(String[] test)
    {
        ReferenceType outputType = ReferenceTypeRegistry.getReferenceType( test[1] );
        assertNotNull( "Output type exists: " + test[1], outputType );
        ReferenceType inputType = ReferenceTypeRegistry.detectReferenceType( test[0] );
        assertNotNull( "Input type exists: " + test[0], inputType );
        Properties inputProperties = BioHubSupport.createProperties( test[2], inputType );
        Properties outputProperties = BioHubSupport.createProperties( test[2], outputType );
        Map<String, String[]> references = BioHubRegistry.getReferences( new String[] {test[0]}, inputProperties, outputProperties,
                null );
        assertNotNull( "Matching result exists: " + test[0] + "->" + test[3], references );
        assertEquals( "Matching result not empty: " + test[0] + "->" + test[3], references.size(), 1 );
        String[] matched = references.get( test[0] );
        assertNotNull( "Matching result not empty: " + test[0] + "->" + test[3], matched );
        assertEquals( "Matching result contains 1 hit: " + test[0] + "->" + test[3], 1, matched.length );
        assertEquals( "Matching result correct: " + test[0] + "->" + test[3], test[3], matched[0] );
    }

    private void checkMulti(String[] testMulti)
    {
        ReferenceType outputType = ReferenceTypeRegistry.getReferenceType( testMulti[1] );
        assertNotNull( "Output type exists: " + testMulti[1], outputType );
        ReferenceType inputType = ReferenceTypeRegistry.detectReferenceType( testMulti[0] );
        assertNotNull( "Input type exists: " + testMulti[0], inputType );
        Properties inputProperties = BioHubSupport.createProperties( testMulti[2], inputType );
        Properties outputProperties = BioHubSupport.createProperties( testMulti[2], outputType );
        Map<String, String[]> references = BioHubRegistry.getReferences( new String[] {testMulti[0]}, inputProperties, outputProperties,
                null );
        assertNotNull( "Matching result exists: " + testMulti[0] + "->" + testMulti[3] + "," + testMulti[4], references );
        assertEquals( "Matching result not empty: " + testMulti[0] + "->" + testMulti[3] + "," + testMulti[4], 1, references.size() );
        String[] matched = references.get( testMulti[0] );
        assertNotNull( "Matching result not empty: " + testMulti[0] + "->" + testMulti[3] + "," + testMulti[4], matched );
        assertEquals( "Matching result contains 2 hits: " + testMulti[0] + "->" + testMulti[3] + "," + testMulti[4], 2, matched.length );
        assertTrue( "Matching result correct: " + testMulti[0] + "->" + testMulti[3] + "," + testMulti[4],
                ( matched[0].equals( testMulti[3] ) && matched[1].equals( testMulti[4] ) )
                        || ( matched[0].equals( testMulti[4] ) && matched[1].equals( testMulti[3] ) ) );
    }
}
