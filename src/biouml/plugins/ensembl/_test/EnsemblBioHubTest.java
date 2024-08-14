package biouml.plugins.ensembl._test;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import biouml.plugins.ensembl.tabletype.AffymetrixProbeTableType;
import biouml.plugins.ensembl.tabletype.AgilentProbeTableType;
import biouml.plugins.ensembl.tabletype.EntrezGeneTableType;
import biouml.plugins.ensembl.tabletype.GenBankGeneTableType;
import biouml.plugins.ensembl.tabletype.GeneSymbolTableType;
import biouml.plugins.ensembl.tabletype.RefSeqProteinTableType;
import biouml.plugins.ensembl.tabletype.RefSeqTranscriptTableType;
import biouml.plugins.ensembl.tabletype.UniGeneTableType;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;

public class EnsemblBioHubTest extends AbstractBioUMLTest
{
    public static final String repositoryPath = "../data/test/ru/biosoft/analysis/databases";

    public EnsemblBioHubTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(EnsemblBioHubTest.class.getName());

        suite.addTest(new EnsemblBioHubTest("testTypeDetection"));
        suite.addTest(new EnsemblBioHubTest("testMatching"));

        return suite;
    }
    
    public void testMatching() throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        String[][] tests = {
                {"LOC296149", "Genes: Ensembl", "Rattus norvegicus", "ENSRNOG00000026061"},
                {"TP53", "Genes: Ensembl", "Homo sapiens", "ENSG00000141510"},
                {"TP53", "Genes: Ensembl", "Rattus norvegicus", "ENSRNOG00000010756"},
                {"Z25521", "Genes: Ensembl", "Homo sapiens", "ENSG00000196776"},
                {"ENSP00000381309", "Genes: Ensembl", "Homo sapiens", "ENSG00000196776"},
                {"203927_at", "Genes: Ensembl", "Homo sapiens", "ENSG00000146232"},
                {"NM_004556", "Genes: Ensembl", "Homo sapiens", "ENSG00000146232"},
        };
        assertNotNull( "External to Ensembl gene registered", BioHubRegistry.getBioHub( "External to Ensembl gene" ) );
        for( String[] test : tests )
        {
            ReferenceType outputType = ReferenceTypeRegistry.getReferenceType(test[1]);
            assertNotNull("Output type exists: " + test[1], outputType);
            ReferenceType inputType = ReferenceTypeRegistry.detectReferenceType(test[0]);
            assertNotNull("Input type exists: " + test[0], inputType);
            Properties inputProperties = BioHubSupport.createProperties( test[2], inputType );
            Properties outputProperties = BioHubSupport.createProperties( test[2], test[1] );
            Map<String, String[]> references = BioHubRegistry
                    .getReferences(new String[] {test[0]}, inputProperties, outputProperties, null);
            assertNotNull("Matching result exists: " + test[0] + "->" + test[3], references);
            assertEquals("Matching result not empty: " + test[0] + "->" + test[3], references.size(), 1);
            String[] matched = references.get(test[0]);
            assertNotNull("Matching result not empty: " + test[0] + "->" + test[3], matched);
            assertEquals("Matching result contains single hit: " + test[0] + "->" + test[3], 1, matched.length);
            assertEquals("Matching result correct: " + test[0] + "->" + test[3], test[3], matched[0]);

        }

        Properties inputProperties = BioHubSupport.createProperties( "Homo sapiens", "Genes: Gene symbol" );
        Properties outputProperties = BioHubSupport.createProperties( "Homo sapiens", "Proteins: UniProt" );

        Set<String> uniprot = BioHubRegistry.getReferencesFlat(new String[] {"MEF2C"}, inputProperties, outputProperties, null);
        assertTrue(uniprot.contains("Q06413"));
    }

    public void testTypeDetection() throws Exception
    {
        String[][] tests = {
            {"207901_at", AffymetrixProbeTableType.class.getName()},
            {"LOC296149", GeneSymbolTableType.class.getName()},
            {"Hs.674", UniGeneTableType.class.getName()},
            {"DDX17", GeneSymbolTableType.class.getName()},
            {"1234", EntrezGeneTableType.class.getName()},
            {"210229_s_at", AffymetrixProbeTableType.class.getName()},
            {"NM_001511", RefSeqTranscriptTableType.class.getName()},
            {"Z25521", GenBankGeneTableType.class.getName()},
            {"NP_012345", RefSeqProteinTableType.class.getName()},
            {"A_23_P110288", AgilentProbeTableType.class.getName()}
        };

        for( String[] test : tests )
        {
            assertEquals(test[0] + ": " + test[1], ReferenceTypeRegistry.detectReferenceType(test[0]).getClass().getName(), test[1]);
        }
    }
}
