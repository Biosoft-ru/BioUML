package ru.biosoft.bsa._test;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.FastaExporter;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.TempFiles;

public class FastaExporterTest extends AbstractBioUMLTest
{
    public static final String SEQUENCE_PATH = "../data/test/ru/biosoft/bsa/bsa/sequences";
    
    private DataCollection baseDC = null;
    private DataCollection fastaDC = null;
    private DataCollection emblDC = null;
    
    public FastaExporterTest(String name)
    {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        baseDC = CollectionFactory.createRepository(SEQUENCE_PATH);
        fastaDC = (DataCollection)baseDC.get("fasta");
        emblDC = (DataCollection)baseDC.get("embl");
    }
    
    public void testDoExportFromFasta() throws Exception
    {
        FastaExporter exporter = new FastaExporter();
        File file = TempFiles.file("exportTest.fasta");
        assertTrue(exporter.accept(fastaDC)>0);
        exporter.getProperties(fastaDC, file).setNucleotidesPerSection(10);
        exporter.doExport(fastaDC, file);
        File expectedFile = new File("ru/biosoft/bsa/_test/fasta2fasta.fasta");
        assertFileEquals("export from fasta collection to fasta file", expectedFile, file);
        file.delete();
        
        exporter = new FastaExporter();
        file = TempFiles.file("exportTest.fasta");
        MapAsVector seq = new MapAsVector("test", null, new LinearSequence("ACGTACGTACGATCGATCGATCGTAGCATGCTAGCTAGCTACGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATGCATCGTAC", Nucleotide5LetterAlphabet.getInstance()), null);
        exporter.getProperties(seq, file).setNucleotidesPerSection(0);
        exporter.getProperties(seq, file).setNucleotidesPerLine(10);
        FunctionJobControl fjc = new FunctionJobControl(null);
        exporter.doExport(seq, file, fjc);
        assertEquals(100, fjc.getPreparedness());
        expectedFile = new File("ru/biosoft/bsa/_test/seq2fasta.fasta");
        assertFileEquals("export from fasta collection to fasta file", expectedFile, file);
        file.delete();
    }
    
    public void testDoExportFromEMBL() throws Exception
    {
        FastaExporter exporter = new FastaExporter();
        File file = TempFiles.file("exportTest.fasta");
        assertNotNull(emblDC);
        assertTrue(exporter.accept(emblDC)>0);
        exporter.getProperties(emblDC, file).setNucleotidesPerSection(0);
        exporter.doExport(emblDC, file);
        File expectedFile = new File("ru/biosoft/bsa/_test/embl2fasta.fasta");
        assertFileEquals("export from embl collection to fasta file", expectedFile, file);
        file.delete();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(FastaExporterTest.class);
        suite.addTest(new FastaExporterTest("testDoExportFromFasta"));
        suite.addTest(new FastaExporterTest("testDoExportFromEMBL"));
        return suite;
    }
    
    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(FastaExporterTest.class);
        }
    }
}
