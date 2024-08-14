package biouml.plugins.agentmodeling._test;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.diagram.Util;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.SpecieReference;

import ru.biosoft.access.core.DataCollection;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CellAgentTest2 extends TestCase
{

    public CellAgentTest2(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(CellAgentTest2.class.getName());
        suite.addTest(new CellAgentTest2("test"));
        return suite;
    }

    public final static int LIMIT_PER_FILE = 500;
    public final static int START_NUM = 1;
    public final static int LIMIT = 4500;
    public final static String COLLECTION_NAME = "databases/Protein model/Data";
    final static String PATH_FILE = "C:";
    final static String DIAGRAM_COLLECTION_NAME = "databases/agentmodel_test/Diagrams/";
    final static String DIAGRAM_NAME = "ProteinModel";


    public void test() throws Exception
    {
        //        WriterAppender appender = new WriterAppender(new PatternLayout("%-5p :  %m%n"), System.out);
        //        appender.setThreshold(Level.INFO);
        //        BasicConfigurator.configure(appender);
        DataCollection collection = AgentTestingUtils.loadCollection(COLLECTION_NAME);

        DataCollection<Protein> proteinCollection = (DataCollection)collection.get("protein");
        DataCollection<RNA> rnaCollection = (DataCollection)collection.get("rna");

        int fileCounter = 0;
        int fileIndex = 0;
        int counter = 0;

        Iterator<Protein> iter = proteinCollection.iterator();

        while( iter.hasNext() && counter < START_NUM + LIMIT )
        {

            Protein protein = iter.next();

            String name = protein.getName();

            Object protCopiesAttribute = protein.getAttributes().getValue("protein_copies_avg");
            Object protKspAttribute = protein.getAttributes().getValue("ksp_avg");
            Object protHalflifeAttribute = protein.getAttributes().getValue("protein_halflife_avg");

            if( protCopiesAttribute == null || protKspAttribute == null || protHalflifeAttribute == null )
                continue;

            RNA rna = rnaCollection.get(name + "_m");

            if( rna == null )
                continue;

            Object rnaCopiesAttribute = rna.getAttributes().getValue("mrna_copies_avg");
            Object rnaVsrAttribute = rna.getAttributes().getValue("vsr_avg");
            Object rnaHalflifeAttribute = rna.getAttributes().getValue("mrna_halflife_avg");

            if( rnaCopiesAttribute == null || rnaVsrAttribute == null || rnaHalflifeAttribute == null )
                continue;

            fileCounter++;
            if( fileCounter > 500 )
            {
                fileCounter = 0;
                fileIndex++;
            }
            counter++;
            System.out.printf("%d/%d%n", Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory());
            System.out.println("protein number: " + counter + " produce");

            if( counter < START_NUM )
                continue;

            vsr = Double.parseDouble(rnaVsrAttribute.toString());
            mrnaCopies = Double.parseDouble(rnaCopiesAttribute.toString());
            mrnaHL = Double.parseDouble(rnaHalflifeAttribute.toString());

            protHL = Double.parseDouble(protHalflifeAttribute.toString());
            proteinCopies = Double.parseDouble(protCopiesAttribute.toString());
            ksp = Double.parseDouble(protKspAttribute.toString());

            //            Diagram diagram = generateDiagram(name);

            CellAgentTest simulationTest = new CellAgentTest("");
            System.out.printf("%d/%d%n", Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory());

            simulationTest.proteinCopies = proteinCopies;
            simulationTest.rnaCopies = mrnaCopies;
            simulationTest.proteinDegradation = ( Math.log(2) / protHL );
            simulationTest.rnaDegradation = ( Math.log(2) / mrnaHL );
            simulationTest.proteinSynthesis = ksp;
            simulationTest.rnaSynthesis = vsr;
            simulationTest.nameProtein = name;


//            simulationTest.test();
            simulationTest.setShowPlot(false);
            simulationTest.clearVars();
            simulationTest.setSaveToFile(true);

            simulationTest.addVarToFile("$pm_3_m", "$" + name + "_m", new File(PATH_FILE + "rnaData_" + fileIndex + ".txt"));
            simulationTest.addVarToFile("$pm_3", "$" + name, new File(PATH_FILE + "proteinData_" + fileIndex + ".txt"));
            simulationTest.test();
            System.out.printf("%d/%d%n", Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory());

        }
    }
    private double mrnaCopies;
    private double proteinCopies;
    private double ksp;
    private double protHL;
    private double vsr;
    private double mrnaHL;


    private Diagram generateDiagram(String name) throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator(name);

        String protein = name;
        String mrna = name + "_m";
        Node rnaNode = generator.createSpecies(mrna, mrnaCopies);
        Node proteinNode = generator.createSpecies(protein, proteinCopies);

        generator.createReaction(vsr + "*h", generator.createSpeciesReference(rnaNode, SpecieReference.PRODUCT));
        generator.createReaction( ( Math.log(2) / mrnaHL ) + "*$" + mrna, generator.createSpeciesReference(rnaNode,
                SpecieReference.REACTANT));
        Node proteinDegradation = (Node)generator.createReaction( ( Math.log( 2 ) / protHL ) + "*$" + protein,
                generator.createSpeciesReference( proteinNode, SpecieReference.REACTANT ) ).getElement();
        assertNotNull(proteinDegradation);

        List<SpecieReference> refs = new ArrayList<>();
        refs.add(generator.createSpeciesReference(proteinNode, SpecieReference.PRODUCT));
        refs.add(generator.createSpeciesReference(rnaNode, SpecieReference.MODIFIER));
        DiagramElementGroup reactionElements = generator.createReaction( ksp + "*$" + mrna, refs );
        Node proteinSynt = (Node)reactionElements.getElement( Util::isReaction );
        assertNotNull(proteinSynt);

        List<Assignment> entryAssignments = new ArrayList<>();
        entryAssignments.add(new Assignment("h", "0"));
        entryAssignments.add(new Assignment("m_stage", "1"));
        List<Assignment> exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("$" + mrna, "$" + mrna + "/2"));
        exitAssignments.add(new Assignment("$" + protein, "$" + protein + "/2"));
        exitAssignments.add(new Assignment("division", "1"));
        exitAssignments.add(new Assignment("m_stage", "0"));
        Node mNode = generator.createState("math-state_0", entryAssignments, exitAssignments);

        entryAssignments = new ArrayList<>();
        entryAssignments.add(new Assignment("h", "1"));
        entryAssignments.add(new Assignment("g1_stage", "1"));
        exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("g1_stage", "0"));
        Node g1Node = generator.createState("math-state_1", entryAssignments, exitAssignments, true);

        entryAssignments = new ArrayList<>();
        //        entryAssignments.add(new Assignment("h", "0.7"));
        entryAssignments.add(new Assignment("s_stage", "1"));
        exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("s_stage", "0"));
        Node sNode = generator.createState("math-state_2", entryAssignments, exitAssignments);

        entryAssignments = new ArrayList<>();
        //        entryAssignments.add(new Assignment("h", "0.5"));
        entryAssignments.add(new Assignment("g2_stage", "1"));
        exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("g2_stage", "0"));
        Node g2Node = generator.createState("math-state_3", entryAssignments, exitAssignments);

        generator.createTransition("m_g1", mNode, g1Node, null, "1");
        generator.createTransition("g1_s", g1Node, sNode, null, "24.5");
        generator.createTransition("s_g2", sNode, g2Node, null, "1");
        generator.createTransition("g2_m", g2Node, mNode, null, "1");


        //        String repositoryPath = "../data";
        //        DataCollection repository = CollectionFactory.createRepository(repositoryPath);
        //        DataCollection collection = CollectionFactory.getDataCollection(DIAGRAM_COLLECTION_NAME);
        Diagram diagram = generator.getDiagram();

        generator.getEModel().getVariable("h").setInitialValue(1);


        //        collection.put(diagram);


        return diagram;
    }


    //    public void test() throws Exception
    //    {
    //        File protein_f = new File("c://protein_hls.txt");
    //        File rna_f = new File("c://rna_hls.txt");
    //
    //        BufferedWriter protein_bw = new BufferedWriter(new FileWriter(protein_f, true));
    //        BufferedWriter rna_bw = new BufferedWriter(new FileWriter(rna_f, true));
    //
    //        String repositoryPath = "../data";
    //        DataCollection repository = CollectionFactory.createRepository(repositoryPath);
    //        DataCollection collection = CollectionFactory.getDataCollection(COLLECTION_NAME);
    //
    //        DataCollection<Protein> proteinCollection = (DataCollection)collection.get("protein");
    //        DataCollection<RNA> rnaCollection = (DataCollection)collection.get("rna");
    //
    //        int counter = 0;
    //        Iterator<Protein> iter = proteinCollection.iterator();
    //
    //        while( iter.hasNext() && counter < LIMIT )
    //        {
    //
    //            StringBuilder sb1 = new StringBuilder();
    //            StringBuilder sb2 = new StringBuilder();
    //
    //            Protein protein = iter.next();
    //
    //            String name = protein.getName();
    //            String title = protein.getTitle();
    //
    //            Object protCopiesAttribute = protein.getAttributes().getValue("protein_copies_avg");
    //            Object protKspAttribute = protein.getAttributes().getValue("ksp_avg");
    //            Object protHalflifeAttribute = protein.getAttributes().getValue("protein_halflife_avg");
    //
    //            if( protCopiesAttribute == null || protKspAttribute == null || protHalflifeAttribute == null )
    //                continue;
    //
    //            RNA rna = rnaCollection.get(name + "_m");
    //
    //            if( rna == null )
    //                continue;
    //
    //            Object rnaCopiesAttribute = rna.getAttributes().getValue("mrna_copies_avg");
    //            Object rnaVsrAttribute = rna.getAttributes().getValue("vsr_avg");
    //            Object rnaHalflifeAttribute = rna.getAttributes().getValue("mrna_halflife_avg");
    //
    //            if( rnaCopiesAttribute == null || rnaVsrAttribute == null || rnaHalflifeAttribute == null )
    //                continue;
    //
    //            counter++;
    //
    //            mrnaHL = Double.parseDouble(rnaHalflifeAttribute.toString());
    //            protHL = Double.parseDouble(protHalflifeAttribute.toString());
    //
    //            sb1.append(name);
    //            sb1.append("\t");
    //            sb1.append(protHL);
    //            sb1.append("\n");
    //
    //            sb2.append(name + "_m");
    //            sb2.append("\t");
    //            sb2.append(mrnaHL);
    //            sb2.append("\n");
    //
    //            protein_bw.append(sb1.toString());
    //            rna_bw.append(sb2.toString());
    //        }
    //
    //        protein_bw.close();
    //        rna_bw.close();
    //    }

}
