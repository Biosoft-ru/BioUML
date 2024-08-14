package biouml.plugins.agentmodeling._test;

import java.io.File;
import java.util.Iterator;

import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import ru.biosoft.access.core.DataCollection;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CellAgentTestDif2 extends TestCase
{

    public CellAgentTestDif2(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(CellAgentTestDif2.class.getName());
        suite.addTest(new CellAgentTestDif2("test"));
        return suite;
    }

    public final static int LIMIT_PER_FILE = Integer.MAX_VALUE;
    public final static int LIMIT = Integer.MAX_VALUE;
    public final static String COLLECTION_NAME = "databases/Protein model/Data";
   
    public void test() throws Exception
    {
        DataCollection collection = AgentTestingUtils.loadCollection(COLLECTION_NAME);

        DataCollection<Protein> proteinCollection = (DataCollection)collection.get("protein");
        DataCollection<RNA> rnaCollection = (DataCollection)collection.get("rna");

        int fileCounter = 0;
        int fileIndex = 0;
        int counter = 0;

        Iterator<Protein> iter = proteinCollection.iterator();

        CellAgentTestDif simulationTest = new CellAgentTestDif("");
        simulationTest.showPlot = false;
        simulationTest.saveToFile = true;
        
        simulationTest.prepare();
        
        while( iter.hasNext() && counter < LIMIT )
        {
            if( fileCounter >= LIMIT_PER_FILE )
            {
                fileCounter = 0;
                fileIndex++;
            }
            fileCounter++;
            counter++;
            
            Protein protein = iter.next();

            String name = protein.getName();
            RNA rna = rnaCollection.get(name + "_m");
            
            Object protCopiesAttribute = protein.getAttributes().getValue("protein_copies_avg");
            Object protKspAttribute = protein.getAttributes().getValue("ksp_avg");
            Object protHalflifeAttribute = protein.getAttributes().getValue("protein_halflife_avg");

            if( protCopiesAttribute == null || protKspAttribute == null || protHalflifeAttribute == null || rna == null)
                continue;

            Object rnaCopiesAttribute = rna.getAttributes().getValue("mrna_copies_avg");
            Object rnaVsrAttribute = rna.getAttributes().getValue("vsr_avg");
            Object rnaHalflifeAttribute = rna.getAttributes().getValue("mrna_halflife_avg");

            if( rnaCopiesAttribute == null || rnaVsrAttribute == null || rnaHalflifeAttribute == null )
                continue;


            System.out.println("protein number: " + counter + " produce");

            simulationTest.proteinCopies = Double.parseDouble(protCopiesAttribute.toString());
            simulationTest.rnaCopies = Double.parseDouble(rnaCopiesAttribute.toString());
            simulationTest.proteinDegradation = ( Math.log(2) / Double.parseDouble(protHalflifeAttribute.toString()) );
            simulationTest.rnaDegradation = ( Math.log(2) / Double.parseDouble(rnaHalflifeAttribute.toString()) );
            simulationTest.proteinSynthesis =  Double.parseDouble(protKspAttribute.toString());
            simulationTest.rnaSynthesis = Double.parseDouble(rnaVsrAttribute.toString());

            simulationTest.clearVars();
            simulationTest.addVarToFile("$RNA", "$" + name + "_m", new File("C:/projects/rnaDataDif_" + fileIndex + ".txt"));
            simulationTest.addVarToFile("$Protein", "$" + name, new File("C:/projects/proteinDataDif_" + fileIndex + ".txt"));
            simulationTest.simulate();

        }
    }
}
