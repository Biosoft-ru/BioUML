package biouml.standard._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.logging.LogManager;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.standard.type.Cell;
import biouml.standard.type.Compartment;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Gene;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.RelationType;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Species;
import biouml.standard.type.Substance;
import biouml.standard.type.Unit;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Test for access to MySQL database which stores standard module types.
 */
public class SqlModuleTest extends TestCase
{
    static DataCollection<?> repository = null;

    public SqlModuleTest(String name)
    {
        super(name);

        // Setup log
        File configFile = new File( "./biouml/standard/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(SqlModuleTest.class.getName());

        suite.addTest(new SqlModuleTest("testInit"));

        suite.addTest(new SqlModuleTest("testConceptDC"));
        suite.addTest(new SqlModuleTest("testCompartmentDC"));
        suite.addTest(new SqlModuleTest("testCellDC"));

        suite.addTest(new SqlModuleTest("testSubstanceDC"));
        suite.addTest(new SqlModuleTest("testGeneDC"));
        suite.addTest(new SqlModuleTest("testRnaDC"));
        suite.addTest(new SqlModuleTest("testProteinDC"));

        suite.addTest(new SqlModuleTest("testSpeciesDC"));
        suite.addTest(new SqlModuleTest("testUnitDC"));
        suite.addTest(new SqlModuleTest("testRelationTypeDC"));
        suite.addTest(new SqlModuleTest("testDatabaseInfoDC"));

        suite.addTest(new SqlModuleTest("testRelationDC"));
        suite.addTest(new SqlModuleTest("testReactionDC"));

        suite.addTest(new SqlModuleTest("testDiagramDC"));

        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testInit() throws Exception
    {
        // create repository
        repository = CollectionFactory.createRepository("../data/cyclonet (local)");
        assertNotNull( "Can't create repository", repository );

        CollectionFactory.registerRoot(repository);
    }

    /** General routine to test concept properties. */
    public <T extends Referrer> void testConcept(Class<T> c, T concept, boolean remove) throws Exception
    {
        DataCollection<T> dc = ((Module)repository).getCategory(c);
        assertNotNull( "Can't get concept collection", dc);

        List<String> names = dc.getNameList();
        assertNotNull( "Can't get name list", names);
        System.out.println(concept.getType() + " names: " + names);

        concept.setTitle("title");
        concept.setDescription("description");
        concept.setComment("comment");

        if( concept instanceof Concept )
        {
            ((Concept)concept).setCompleteName("complete name");
            ((Concept)concept).setSynonyms("syn_1; syn_2");
        }

        DatabaseReference[] refs = new DatabaseReference[1];
        refs[0] = new DatabaseReference("EMBL", "XXX", "001");
        refs[0].setComment("comment");
        concept.setDatabaseReferences(refs);

        String[] liter = new String[1];
        liter[0] = "ref 1";
        concept.setLiteratureReferences(liter);

        dc.put(concept);
        assertNotNull( "Can't get inserted concept " + concept.getName(), dc.get(concept.getName()));

        if( remove )
        {
            dc.remove(concept.getName());
            assertNull("Concept " + concept.getName() + " was not removed", dc.get(concept.getName()));
        }
    }

    public <T extends DataElement> void testDictionary(String type, Class<T> c, T de, boolean remove) throws Exception
    {
        DataCollection<T> dc = ((Module)repository).getCategory(c);
        assertNotNull( "Can't get dictionary", dc);

        List<String> names = dc.getNameList();
        assertNotNull( "Can't get name list", names);
        System.out.println(type + " names: " + names);

        dc.put(de);
        assertNotNull( "Can't get inserted dictionary " + de.getName(), dc.get(de.getName()));

        if( remove )
        {
            dc.remove(de.getName());
            assertNull(type + " " + de.getName() + " was not removed", dc.get(de.getName()));
        }
    }

    public void testCellDC() throws Exception
    {
        DataCollection<Cell> cells = ((Module)repository).getCategory(Cell.class);
        assertNotNull( "Can't get cells collection", cells);

        assertEquals("Cells size=", 2, cells.getSize());
        assertEquals("Contains CEL000001", cells.contains("CEL000001"), true);
        assertEquals("Contains CEL00000a", cells.contains("CEL00000a"), false);

        Cell cell = new Cell(null, "CEL000003");
        cell.setSpecies("Homo sapiens");
        testConcept(Cell.class, cell, true);
    }

    public void testConceptDC() throws Exception
    {
        Concept concept = new Concept(null, "CPT000002");
        testConcept(Concept.class, concept, true);
    }

    public void testCompartmentDC() throws Exception
    {
        Compartment concept = new Compartment(null, "CMP000002");
        testConcept(Compartment.class, concept, true);
    }

    public void testSubstanceDC() throws Exception
    {
        Substance concept = new Substance(null, "SUB000002");
        testConcept(Substance.class, concept, true);
    }

    public void testGeneDC() throws Exception
    {
        Gene gene = new Gene(null, "GEN000002");
        gene.setSpecies("Homo sapiens");
        gene.getAttributes().add( new DynamicProperty( Gene.LOCATION_PD, String.class, "chrom 1") );
        testConcept(Gene.class, gene, true);
    }

    public void testRnaDC() throws Exception
    {
        RNA rna = new RNA(null, "RNA000002");
        rna.setSpecies("Homo sapiens");
        rna.setGene("GEN000001");
        rna.setRnaType("rRNA");
        testConcept(RNA.class, rna, true);
    }

    public void testProteinDC() throws Exception
    {
        Protein protein = new Protein(null, "PRT000002");
        protein.setSpecies("Homo sapiens");
        protein.setGene("GEN000001");
        protein.setFunctionalState("active");
        protein.setStructure("homodimer");
        protein.setModification("phosphorylated");
        testConcept(Protein.class, protein, true);
    }

    ///////////////////////////////////////////////////////////////////////////
    // test dictionaries
    //

    public void testSpeciesDC() throws Exception
    {
        Species species = new Species(null, "Arabidopsis thaliana");
        species.setCommonName("mouse ear-cress");
        species.setAbbreviation("At");
        species.setDescription("description");

        testDictionary("Species", Species.class, species, true);
    }

    public void testUnitDC() throws Exception
    {
        Unit unit = new Unit(null, "liter");
        unit.setComment("description");

        testDictionary("Units", Unit.class, unit, true);
    }

    public void testRelationTypeDC() throws Exception
    {
        RelationType type = new RelationType(null, "predecessor");
        type.setTitle("predecessor");
        type.setDescription("description");
        type.setComment("comment");
        type.setStroke("stroke");

        testDictionary("Relation type", RelationType.class, type, true);
    }

    public void testDatabaseInfoDC() throws Exception
    {
        DatabaseInfo dbInfo = new DatabaseInfo(null, "DBI001");
        dbInfo.setTitle("GeneNet");
        dbInfo.setDescription("GeneNet");
        dbInfo.setComment("comment");
        dbInfo.setURL("wwwmgs");
        dbInfo.setQueryById("query: $id$");
        dbInfo.setQueryByAc("query: $ac$");

        testDictionary("DatabaseInfo type", DatabaseInfo.class, dbInfo, true);
    }

    ///////////////////////////////////////////////////////////////////////////
    // test relation and reactions
    //

    public void testRelationDC() throws Exception
    {
        SemanticRelation relation = new SemanticRelation(null, "SRL000001");
        relation.setInputElementName("input");
        relation.setOutputElementName("output");
        relation.setParticipation("indirect");
        relation.setRelationType("part of");

        testConcept(SemanticRelation.class, relation, true);
    }

    public void testReactionDC() throws Exception
    {
        Reaction reaction = new Reaction(null, "RCT000001");
        reaction.setFast(true);
        reaction.setReversible(true);

        KineticLaw law = new KineticLaw();
        law.setFormula("a+b");
        law.setTimeUnits("time");
        law.setSubstanceUnits("substance");
        law.setComment("comment");
        reaction.setKineticLaw(law);

        SpecieReference[] refs = new SpecieReference[2];
        SpecieReference reactant = new SpecieReference(reaction, "CRR0001");
        SpecieReference modifier = new SpecieReference(reaction, "CRR0002");
        modifier.setModifierAction(SpecieReference.ACTION_INHIBITOR);
        refs[0] = reactant;
        refs[1] = modifier;
        reaction.setSpecieReferences(refs);

        testConcept(Reaction.class, reaction, true);
    }

    ///////////////////////////////////////////////////////////////////////////
    // test diagram
    //

    public void testDiagramDC() throws Exception
    {
        DiagramInfo info = new DiagramInfo(null, "DIA001");

        DataCollection<Diagram> diagrams = (DataCollection<Diagram>)repository.get("Diagrams");
        assertNotNull( "Can't get Diagrams DC", diagrams);

        List<String> names = diagrams.getNameList();
        assertNotNull( "Can't get name list", names);
        System.out.println("Diagram names: " + names);

        Diagram diagram = new Diagram(diagrams, info, new biouml.standard.diagram.SemanticNetworkDiagramType());
        diagrams.put(diagram);
        assertNotNull( "Can't put the diagram " + diagram.getName(), diagrams.get(diagram.getName()));
    }

}
