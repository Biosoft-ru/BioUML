package biouml.plugins.agentmodeling._test;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Equation;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.SpecieReference;

import ru.biosoft.access.core.DataCollection;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CellAgentTestGD2 extends TestCase
{

    public double arrestProbability = 0.0;
    public double g1Length = 15.5;
    public double g2Length = 4;
    public double sLength = 7;
    public double mLength = 1;

    public CellAgentTestGD2(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(CellAgentTestGD2.class.getName());
        suite.addTest(new CellAgentTestGD2("test"));
        return suite;
    }

    public final static int LIMIT_PER_FILE = 500;
    public final static int LIMIT = 4500;
    public final static int START_NUM = 1;
    public final static String RESULT_PATH = "databases/Protein model";
    public final static String COLLECTION_NAME = "databases/Protein model/Data";

    final static String DIAGRAM_COLLECTION_NAME = "databases/agentmodel_test/Diagrams/";
    final static String DIAGRAM_NAME = "ProteinModel";


    public void test() throws Exception
    {
        DataCollection collection = AgentTestingUtils.loadCollection(COLLECTION_NAME);

        DataCollection<Protein> proteinCollection = (DataCollection)collection.get("protein");
        DataCollection<RNA> rnaCollection = (DataCollection)collection.get("rna");

        int fileCounter = 0;
        int fileIndex = 0;
        int counter = 0;

        Iterator<Protein> iter = proteinCollection.iterator();

        while( iter.hasNext() && counter < LIMIT + START_NUM )
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
            if( fileCounter > LIMIT_PER_FILE )
            {
                fileCounter = 0;
                fileIndex++;
            }
            counter++;

            if( counter < START_NUM )
                continue;

            System.out.println("protein number: " + counter + " produce");
            System.out.printf("%d/%d%n", Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory());
            vsr = Double.parseDouble(rnaVsrAttribute.toString());
            mrnaCopies = Double.parseDouble(rnaCopiesAttribute.toString());
            mrnaHL = Double.parseDouble(rnaHalflifeAttribute.toString());
            rnaDegradation = ( Math.log(2) / mrnaHL );

            protHL = Double.parseDouble(protHalflifeAttribute.toString());
            proteinCopies = Double.parseDouble(protCopiesAttribute.toString());
            ksp = Double.parseDouble(protKspAttribute.toString());
            proteinDegradation = ( Math.log(2) / protHL );
            Diagram diagram = generateDiagram(name);

            CellAgentTestGD simulationTest = new CellAgentTestGD("");
            simulationTest.proteinCopies = proteinCopies;
            simulationTest.rnaCopies = mrnaCopies;
            simulationTest.proteinHL = protHL;
            simulationTest.rnaHL = mrnaHL;

            simulationTest.setShowPlot(false);
            simulationTest.clearVars();
            simulationTest.setSaveToFile(true);
            simulationTest.addVarToFile("$" + name + "_m", new File(RESULT_PATH + "/rna_" + fileIndex + ".txt"));
            simulationTest.addVarToFile("$" + name, new File(RESULT_PATH + "/protein_" + fileIndex + ".txt"));
            simulationTest.simulate(diagram);
            System.out.printf("%d/%d%n", Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory());
        }
    }
    private double mrnaCopies;
    private double proteinCopies;
    private double ksp;
    private double protHL;
    private double proteinDegradation;
    private double vsr;
    private double mrnaHL;
    private double rnaDegradation;

    private Diagram generateDiagram(String name) throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator(name);

        String protein = name;
        String mrna = name + "_m";
        Node rnaNode = generator.createSpecies(mrna, mrnaCopies);
        Node proteinNode = generator.createSpecies(protein, proteinCopies);

        generator.createReaction(vsr + "*h", generator.createSpeciesReference(rnaNode, SpecieReference.PRODUCT));
        generator.createReaction(rnaDegradation + "*$" + mrna, generator.createSpeciesReference(rnaNode, SpecieReference.REACTANT));
        generator.createReaction(proteinDegradation + "*$" + protein, generator.createSpeciesReference(proteinNode,
                SpecieReference.REACTANT));

        List<SpecieReference> refs = new ArrayList<>();
        refs.add(generator.createSpeciesReference(proteinNode, SpecieReference.PRODUCT));
        refs.add(generator.createSpeciesReference(rnaNode, SpecieReference.MODIFIER));
        generator.createReaction(ksp + "*$" + mrna, refs);


        generator.createEquation("rand", "random()*(1 + arrestProbability)*m", Equation.TYPE_INITIAL_ASSIGNMENT);
        generator.createEquation("g1", "g1_length", Equation.TYPE_INITIAL_ASSIGNMENT);
        generator.createEquation("s", "g1_length + s_length", Equation.TYPE_INITIAL_ASSIGNMENT);
        generator.createEquation("g2", "g1_length + s_length + g2_length", Equation.TYPE_INITIAL_ASSIGNMENT);
        generator.createEquation("m", "g1_length + s_length + g2_length + m_length", Equation.TYPE_INITIAL_ASSIGNMENT);
        generator.createEquation("g1_rand", "random()*2 - 1", Equation.TYPE_INITIAL_ASSIGNMENT);

        List<Assignment> entryAssignments = new ArrayList<>();
        entryAssignments.add(new Assignment("h", "0"));
        entryAssignments.add(new Assignment("m_stage", "1"));
        List<Assignment> exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("$" + mrna, "$" + mrna + "/2"));
        exitAssignments.add(new Assignment("$" + protein, "$" + protein + "/2"));
        exitAssignments.add(new Assignment("division", "1"));
        exitAssignments.add(new Assignment("m_stage", "0"));
        exitAssignments.add(new Assignment("currentStateTime", "time"));
        Node mNode = generator.createState("math-state_0", entryAssignments, exitAssignments);

        entryAssignments = new ArrayList<>();
        entryAssignments.add(new Assignment("h", "1"));
        entryAssignments.add(new Assignment("g1_stage", "1"));
        entryAssignments.add(new Assignment("arrest", "piecewise( random() < arrestProbability => 1; 0 )"));
        exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("g1_stage", "0"));
        exitAssignments.add(new Assignment("currentStateTime", "time"));
        Node g1Node = generator.createState("math-state_4", entryAssignments, exitAssignments);

        entryAssignments = new ArrayList<>();
        entryAssignments.add(new Assignment("h", "0.7"));
        entryAssignments.add(new Assignment("s_stage", "1"));
        exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("s_stage", "0"));
        exitAssignments.add(new Assignment("currentStateTime", "time"));
        Node sNode = generator.createState("math-state_2", entryAssignments, exitAssignments);

        entryAssignments = new ArrayList<>();
        entryAssignments.add(new Assignment("h", "2"));
        entryAssignments.add(new Assignment("g2_stage", "1"));
        exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("g2_stage", "0"));
        exitAssignments.add(new Assignment("currentStateTime", "time"));
        Node g2Node = generator.createState("math-state_3", entryAssignments, exitAssignments);

        entryAssignments = new ArrayList<>();
        entryAssignments.add(new Assignment("g0_stage", "1"));
        exitAssignments = new ArrayList<>();
        Node g0Node = generator.createState("math-state_1", entryAssignments, exitAssignments);

        entryAssignments = new ArrayList<>();
        exitAssignments = new ArrayList<>();
        exitAssignments.add(new Assignment("currentStateTime",
                "piecewise( rand < g1 || rand == m => -rand; rand < s => g1 - rand; rand < g2 => s - rand; rand < m => g2 - rand; 0 )"));
        Node distrNode = generator.createState("math-state_5", entryAssignments, exitAssignments, true);


        generator.createTransition("m_g1", mNode, g1Node, "time > currentStateTime + m_length", null);
        generator.createTransition("g1_s", g1Node, sNode, "time > currentStateTime + g1_length + g1_rand", null);
        generator.createTransition("s_g2", sNode, g2Node, "time > currentStateTime + s_length", null);
        generator.createTransition("g2_m", g2Node, mNode, "time > currentStateTime + g2_length", null);
        generator.createTransition("g1_g0", g1Node, g0Node, "time > currentStateTime + 2 && arrest == 1.0", null);

        generator.createTransition("distr_g1", distrNode, g1Node, "rand < g1 && time > 0.1", null);
        generator.createTransition("distr_s", distrNode, sNode, "g1 < rand && rand < s && time > 0.1", null);
        generator.createTransition("distr_g2", distrNode, g2Node, "s < rand && rand < g2 && time > 0.1", null);
        generator.createTransition("distr_m", distrNode, mNode, "g2 < rand && rand < m && time > 0.1", null);
        generator.createTransition("distr_g0", distrNode, g0Node, "rand > m && time > 0.1", null);

        generator.getEModel().getVariable("h").setInitialValue(1);
        generator.getEModel().getVariable("arrestProbability").setInitialValue(arrestProbability);
        generator.getEModel().getVariable("g1_length").setInitialValue(g1Length);
        generator.getEModel().getVariable("s_length").setInitialValue(sLength);
        generator.getEModel().getVariable("g2_length").setInitialValue(g2Length);
        generator.getEModel().getVariable("m_length").setInitialValue(mLength);
        generator.getEModel().getVariable("division").setInitialValue(0);

        Diagram diagram = generator.getDiagram();
        //        String repositoryPath = "../data";
        //        DataCollection repository = CollectionFactory.createRepository(repositoryPath);
        //        DataCollection collection = CollectionFactory.getDataCollection(DIAGRAM_COLLECTION_NAME);
        //        collection.put(diagram);
        return diagram;
    }
}
