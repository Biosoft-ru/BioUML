package biouml.plugins.biopax._test;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyURIMapper;
import org.semanticweb.owl.util.SimpleURIMapper;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.biopax.reader.BioPAXReader;
import biouml.plugins.biopax.reader.BioPAXReaderFactory;
import biouml.plugins.biopax.reader.BioPAXReader_level2;
import biouml.plugins.biopax.reader.BioPAXReader_level3;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Structure;
import biouml.standard.type.Substance;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * @author anna
 *
 */
public class BioPAXReaderTest extends AbstractBioUMLTest
{
    public static final String dcPath = "../data/test/biopax/";
    private DataCollection<?> repository = null;
    public BioPAXReaderTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(BioPAXReaderTest.class.getName());
        suite.addTest(new BioPAXReaderTest("testReaderLevel2"));
        suite.addTest(new BioPAXReaderTest("testReaderLevel3"));
        return suite;
    }

    public void testReaderLevel2() throws Exception
    {
        Module module = null;
        String fileName = dcPath + "/biopax-example-short-pathway.owl";
        File file = new File(fileName);
        
        OWLOntologyManager manager = BioPAXReader.getOWLOntologyManager();
        OWLOntologyURIMapper mapper = new SimpleURIMapper(URI.create("http://www.biopax.org/release/biopax-level2.owl"), new File(dcPath
                + "/biopax-level2.owl").toURI());
        manager.addURIMapper(mapper);
        mapper = new SimpleURIMapper(URI.create("http://www.biopax.org/release/biopax-level1.owl"), new File(dcPath
                + "/biopax-level1.owl").toURI());
        manager.addURIMapper(mapper);
        System.out.println( "Try to load file " + file.toURI() );
        OWLOntology ontology = manager.loadOntologyFromPhysicalURI(file.toURI());
        BioPAXReader reader = BioPAXReaderFactory.getReader(ontology);

        DataCollection<?> dcExpected = DataElementPath.create("databases/test/biopax/TestCollectionLevel2").getDataCollection();
        System.out.println(dcExpected.getSize());

        assertTrue("Wrong reader class " + reader.getClass().getName(), reader.getClass().isAssignableFrom(BioPAXReader_level2.class));
        Properties props = new Properties();
        props.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
        DataCollection<DataCollection> data = new VectorDataCollection<>( Module.DATA, module, props );
        DataCollection<Diagram> diagrams = new VectorDataCollection<>( Module.DIAGRAM, module, props );
        DataCollection<DataCollection> dictionaries = new VectorDataCollection<>( Module.METADATA, module, props );
        reader.setCollections(data, diagrams, dictionaries);
        reader.read("", null);
        
        DataCollection<DataCollection<?>> dataExpected = (DataCollection<DataCollection<?>>)dcExpected.get(Module.DATA);
        DataCollection<Diagram> diagramsExpected = (DataCollection<Diagram>)dcExpected.get(Module.DIAGRAM);

        //Data subcollections
        DataCollection<?> controls = data.get("Controls");
        DataCollection<?> controlsExpected = dataExpected.get("Controls");
        assertEquals("Wrong number of controls loaded", controlsExpected.getSize(), controls.getSize());
        
        DataCollection<?> conversions = data.get("Conversion");
        DataCollection<?> conversionsExpected = dataExpected.get("Conversion");
        assertEquals("Wrong number of conversions loaded", conversionsExpected.getSize(), conversions.getSize());
        
        DataCollection<?> participants = data.get("Participants");
        DataCollection<?> participantsExpected = dataExpected.get("Participants");
        assertEquals("Wrong number of participants loaded", participantsExpected.getSize(), participants.getSize());
        
        DataCollection<?> proteins = data.get("Protein");
        DataCollection<?> proteinsExpected = dataExpected.get("Protein");
        assertEquals("Wrong number of proteins loaded", proteinsExpected.getSize(), proteins.getSize());
        
        DataCollection<?> smallmols = data.get("SmallMolecule");
        DataCollection<?> smallmolsExpected = dataExpected.get("SmallMolecule");
        assertEquals("Wrong number of small molecules loaded", smallmolsExpected.getSize(), smallmols.getSize());
        
        DataCollection<?> publications = data.get("Publications");
        assertEquals("Wrong number of publications loaded", 1, publications.getSize());

        //Data elements
        DataElement testDE = controls.get("catalysis5");
        DataElement origDE = controlsExpected.get("catalysis5");
        checkControlElement(testDE, origDE);
        
        checkControlElement(controls.get("catalysis43"), controlsExpected.get("catalysis43"));
        
        testDE = conversions.get("biochemicalReaction37");
        origDE = conversionsExpected.get("biochemicalReaction37");
        checkConversionElement(testDE, origDE);
        
        testDE = participants.get("physicalEntityParticipant38");
        origDE = participantsExpected.get("physicalEntityParticipant38");
        checkParticipant(testDE, origDE);
        
        testDE = proteins.get("protein45");
        origDE = proteinsExpected.get("protein45");
        checkProtein(testDE, origDE);
        
        testDE = smallmols.get("smallMolecule18");
        origDE = smallmolsExpected.get("smallMolecule18");
        checkSmallMolecule(testDE, origDE);
        
        //Diagrams
        assertEquals("Wrong number of diagrams loaded", diagramsExpected.getSize(), diagrams.getSize());
        checkDiagram(diagrams.get("pathway50"), diagramsExpected.get("pathway50"));
    }

    public void testReaderLevel3() throws Exception
    {
        Module module = null;
        String fileName = dcPath + "/biopax3-short-metabolic-pathway.owl";
        File file = new File(fileName);
        OWLOntologyManager manager = BioPAXReader.getOWLOntologyManager();
        OWLOntologyURIMapper mapper = new SimpleURIMapper(URI.create("http://www.biopax.org/release/biopax-level3.owl"), new File(dcPath
                + "/biopax-level3.owl").toURI());
        manager.addURIMapper(mapper);
        OWLOntology ontology = manager.loadOntologyFromPhysicalURI(file.toURI());
        BioPAXReader reader = BioPAXReaderFactory.getReader(ontology);
        
        DataCollection<?> dcL3 = DataElementPath.create("databases/test/biopax/TestCollectionLevel3").getDataCollection();
        System.out.println(dcL3.getSize());

        assertTrue("Wrong reader class " + reader.getClass().getName(), reader.getClass().isAssignableFrom(BioPAXReader_level3.class));
        Properties props = new Properties();
        props.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
        DataCollection<DataCollection> data = new VectorDataCollection<>( Module.DATA, module, props );
        DataCollection<Diagram> diagrams = new VectorDataCollection<>( Module.DIAGRAM, module, props );
        DataCollection<DataCollection> dictionaries = new VectorDataCollection<>( Module.METADATA, module, props );
        reader.setCollections(data, diagrams, dictionaries);
        reader.read("", null);
        
        DataCollection<DataCollection<?>> dataL3 = (DataCollection<DataCollection<?>>)dcL3.get(Module.DATA);
        DataCollection<Diagram> diagramsL3 = (DataCollection<Diagram>)dcL3.get(Module.DIAGRAM);

        //Data subcollections
        DataCollection<?> controls = data.get("Controls");
        DataCollection<?> controlsL3 = dataL3.get("Controls");
        assertEquals("Wrong number of controls loaded", controlsL3.getSize(), controls.getSize());
        
        DataCollection<?> conversions = data.get("Conversion");
        DataCollection<?> conversionsL3 = dataL3.get("Conversion");
        assertEquals("Wrong number of conversions loaded", conversionsL3.getSize(), conversions.getSize());
        
        DataCollection<?> participants = data.get("Participants");
        DataCollection<?> participantsL3 = dataL3.get("Participants");
        assertEquals("Wrong number of participants loaded", participantsL3.getSize(), participants.getSize());
        
        DataCollection<?> proteins = data.get("Protein");
        DataCollection<?> proteinsL3 = dataL3.get("Protein");
        assertEquals("Wrong number of proteins loaded", proteinsL3.getSize(), proteins.getSize());
        
        DataCollection<?> smallmols = data.get("SmallMolecule");
        DataCollection<?> smallmolsL3 = dataL3.get("SmallMolecule");
        assertEquals("Wrong number of small molecules loaded", smallmolsL3.getSize(), smallmols.getSize());
        
        DataCollection<?> publications = data.get("Publications");
        assertEquals("Wrong number of publications loaded", 1, publications.getSize());

        //Data elements
        DataElement testDE = controls.get( "glucokinase_converts_alpha_D_glu_to_alpha_D_glu_6_p" );
        DataElement origDE = controlsL3.get( "glucokinase_converts_alpha_D_glu_to_alpha_D_glu_6_p" );
        checkControlElement(testDE, origDE);
        
        testDE = conversions.get("glucokinase");
        origDE = conversionsL3.get("glucokinase");
        checkConversionElement(testDE, origDE);
        
        testDE = participants.get( "beta_D_fructose_6_phosphate_as_product" );
        origDE = participantsL3.get( "beta_D_fructose_6_phosphate_as_product" );
        checkParticipant(testDE, origDE);
        
        testDE = proteins.get("ProteinReference_16");
        origDE = proteinsL3.get("ProteinReference_16");
        checkProtein(testDE, origDE);
        
        testDE = smallmols.get("SmallMoleculeReference_12");
        origDE = smallmolsL3.get("SmallMoleculeReference_12");
        checkSmallMolecule(testDE, origDE);
        
        //Diagrams
        assertEquals("Wrong number of diagrams loaded", diagramsL3.getSize(), diagrams.getSize());
        checkDiagram(diagrams.get("Embden-Meyerhof pathway"), diagramsL3.get("Embden-Meyerhof pathway"));
    }

    private void checkControlElement(DataElement testDE, DataElement origDE)
    {
        assertNotNull("Control was not found", testDE);
        assertTrue("Control object " + testDE.getName() + " is not a SemanticRelation", testDE instanceof SemanticRelation);
        
        SemanticRelation orig = ( (SemanticRelation)origDE );
        SemanticRelation test = (SemanticRelation)testDE;
        assertEquals("Input element is wrong", orig.getInputElementName(), test.getInputElementName());
        assertEquals("Output element is wrong", orig.getOutputElementName(), test.getOutputElementName());
        assertEquals("Wrong relation type", orig.getRelationType(), test.getRelationType());
        assertEquals("Wrong comment", orig.getComment(), test.getComment());
        assertEquals("Wrong title", orig.getTitle(), test.getTitle());
        DynamicPropertySet dps = test.getAttributes();
        DynamicPropertySet dpsRel = orig.getAttributes();
        assertEquals("Wrong direction", dpsRel.getValue("Direction"), dps.getValue("Direction"));
        assertEquals("Wrong relation type", dpsRel.getValue("Type"), dps.getValue("Type"));
        Object dso = dps.getValue("DataSource");
        assertNotNull("No datasource found", dso);
        String[] ds = (String[])dso;
        String[] dsRel = (String[])dpsRel.getValue("DataSource");
        assertArrayEquals("Wrong dataSource", ds, dsRel);
    }

    private void checkConversionElement(DataElement testDE, DataElement origDE)
    {
        assertNotNull("Conversion was not found", testDE);
        assertTrue("Conversion object " + testDE.getName() + " is not a Reaction", testDE instanceof Reaction);
        
        Reaction orig = ( (Reaction)origDE );
        Reaction test = (Reaction)testDE;
        assertEquals("Wrong reversible property", orig.isReversible(), test.isReversible());
        assertEquals("Wrong title", orig.getTitle(), test.getTitle());
        SpecieReference[] srOrig = orig.getSpecieReferences();
        SpecieReference[] sr = test.getSpecieReferences();
        assertEquals("Wrong SpecieReference number", srOrig.length, sr.length);
        for(int i = 0; i < sr.length; i++)
        {
            assertEquals("SpecieReference name", srOrig[i].getName(), sr[i].getName());
            assertEquals("SpecieReference specie", srOrig[i].getSpecie(), sr[i].getSpecie());
        }
        
        checkDatabaseReferences(test.getDatabaseReferences(), orig.getDatabaseReferences());
        
        DynamicPropertySet dps = test.getAttributes();
        DynamicPropertySet dpsRel = orig.getAttributes();
        assertEquals("Wrong spontaneous property",  dpsRel.getValue("Spontaneous"), dps.getValue("Spontaneous"));
        assertEquals("Wrong reaction type", dpsRel.getValue("Type"), dps.getValue("Type"));
        Object dso = dps.getValue("DataSource");
        assertNotNull("No datasource found", dso);
        String[] ds = (String[])dso;
        String[] dsRel = (String[])dpsRel.getValue("DataSource");
        assertArrayEquals("Wrong dataSource", ds, dsRel);
        String[] ecNumber = (String[])dps.getValue( "EcNumber" );
        String[] ecNumberRel = (String[])dpsRel.getValue( "EcNumber" );
        assertArrayEquals("Wrong ecNumber number", ecNumber, ecNumberRel);
    }

    private void checkSmallMolecule(DataElement testDE, DataElement origDE)
    {
        assertNotNull("SmallMolecule was not found", testDE);
        assertTrue("SmallMolecule object " + testDE.getName() + " is not a Substance", testDE instanceof Substance);
        
        Substance orig = ( (Substance)origDE );
        Substance test = (Substance)testDE;
        
        assertEquals("Wrong title", orig.getTitle(), test.getTitle());
        checkDatabaseReferences(test.getDatabaseReferences(), orig.getDatabaseReferences());
        
        DynamicPropertySet dps = test.getAttributes();
        DynamicPropertySet dpsRel = orig.getAttributes();
        assertEquals("Wrong ChemicalFormula", dpsRel.getValue("ChemicalFormula"), dps.getValue("ChemicalFormula"));
        assertEquals("Wrong MolecularWeight", dpsRel.getValue("MolecularWeight"), dps.getValue("MolecularWeight"));
        assertEquals("Wrong StandardName", dpsRel.getValue("StandardName"), dps.getValue("StandardName"));
        assertEquals("Wrong Chemical structure", ( (Structure)dpsRel.getValue("ChemicalStructure") ).getData(), ( (Structure)dps.getValue("ChemicalStructure") ).getData());
    }

    private void checkProtein(DataElement testDE, DataElement origDE)
    {
        assertNotNull("Protein was not found", testDE);
        assertTrue("Protein object " + testDE.getName() + " is not a Protein", testDE instanceof Protein);
        
        Protein orig = ( (Protein)origDE );
        Protein test = (Protein)testDE;
        
        assertEquals("Wrong title", orig.getTitle(), test.getTitle());
        assertEquals("Wrong comment", orig.getComment(), test.getComment());
        assertEquals("Wrong synonyms", orig.getSynonyms(), test.getSynonyms());
        checkDatabaseReferences(test.getDatabaseReferences(), orig.getDatabaseReferences());
        
        DynamicPropertySet dps = test.getAttributes();
        DynamicPropertySet dpsRel = orig.getAttributes();
        assertEquals("Wrong Organism", dpsRel.getValue("Organism"), dps.getValue("Organism"));
        assertEquals("Wrong Sequence", dpsRel.getValue("Sequence"), dps.getValue("Sequence"));
        assertEquals("Wrong StandardName", dpsRel.getValue("StandardName"), dps.getValue("StandardName"));
    }

    private void checkParticipant(DataElement testDE, DataElement origDE)
    {
        assertNotNull("Participant was not found", testDE);
        assertTrue("Participant object " + testDE.getName() + " is not a SpecieReference", testDE instanceof SpecieReference);
        
        SpecieReference orig = ( (SpecieReference)origDE );
        SpecieReference test = (SpecieReference)testDE;
        
        //Title is not supported by SpecieReferenceBeanInfo
        //assertEquals("Wrong title", orig.getTitle(), test.getTitle());
        assertEquals("Wrong role", orig.getRole(), test.getRole());
        assertEquals("Wrong stoichiomentry", Double.parseDouble(orig.getStoichiometry()), Double.parseDouble(test.getStoichiometry()));
        
        DynamicPropertySet dps = test.getAttributes();
        DynamicPropertySet dpsRel = orig.getAttributes();
        assertEquals("Wrong CellularLocation", dpsRel.getValue("CellularLocation"), dps.getValue("CellularLocation"));
        assertEquals("Wrong StandardName", dpsRel.getValue("StandardName"), dps.getValue("StandardName"));
        assertEquals("Wrong type", dpsRel.getValue("Type"), dps.getValue("Type"));
        checkDatabaseReferences((DatabaseReference[])dpsRel.getValue("DatabaseReference"), (DatabaseReference[])dps.getValue("DatabaseReference"));
        Object dso = dps.getValue("DataSource");
        if(dso != null)
        {
            String[] ds = (String[])dso;
            String[] dsRel = (String[])dpsRel.getValue("DataSource");
            assertArrayEquals("Wrong dataSource", ds, dsRel);
        }
    }
    
    private void checkDiagram(DataElement testDE, DataElement origDE)
    {
        assertNotNull("Diagram was not found", testDE);
        assertTrue("Diagram object " + testDE.getName() + " is not a Diagram", testDE instanceof Diagram);

        Diagram origD = ( (Diagram)origDE );
        Diagram testD = (Diagram)testDE;

        DiagramInfo orig = (DiagramInfo)origD.getKernel();
        DiagramInfo test = (DiagramInfo)testD.getKernel();
        assertEquals("Wrong title", orig.getTitle(), test.getTitle());
        assertEquals("Wrong comment", orig.getComment(), test.getComment());
        checkDatabaseReferences(test.getDatabaseReferences(), orig.getDatabaseReferences());

        DynamicPropertySet dps = test.getAttributes();
        DynamicPropertySet dpsRel = orig.getAttributes();
        assertEquals("Wrong Organism", dpsRel.getValue("Organism"), dps.getValue("Organism"));
        assertEquals("Wrong Availability", dpsRel.getValue("Availability"), dps.getValue("Availability"));
        assertEquals("Wrong Synonyms", dpsRel.getValue("Synonyms"), dps.getValue("Synonyms"));
        Object dso = dps.getValue("DataSource");
        assertNotNull("No datasource found", dso);
        String[] ds = (String[])dso;
        String[] dsRel = (String[])dpsRel.getValue("DataSource");
        assertArrayEquals("Wrong dataSource", ds, dsRel);

        assertEquals("Wrong number of nodes ", origD.getNodes().length, testD.getNodes().length);
        assertEquals("Wrong number of edges ", origD.getEdges().length, testD.getEdges().length);

    }
    
    private void checkDatabaseReferences(DatabaseReference[] testDR, DatabaseReference[] origDR)
    {
        if( testDR == null && origDR == null )
            return;

        assertNotNull( "testDR array of DatabaseReference is null, but origDR is not", testDR );
        assertNotNull( "origDR array of DatabaseReference is null, but testDR is not", origDR );

        assertEquals("Wrong database references number", testDR.length, origDR.length);
        for( int i = 0; i < origDR.length; i++ )
        {
            assertEquals("Wrong DR " + i, origDR[i].toString(), testDR[i].toString());
        }
    }
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        repository = CollectionFactory.createRepository("../data");
        assertNotNull("Can't create repository", repository);
        Application.setPreferences( new Preferences() );
        //module = (Module)repository.get("BioPAX_test");
        //assertNotNull("Can't find module 'BioPAX_test'", module);
    }
}
