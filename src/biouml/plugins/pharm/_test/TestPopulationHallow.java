package biouml.plugins.pharm._test;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.pharm.analysis.Patient;
import biouml.plugins.pharm.analysis.PopulationSampling;
import biouml.plugins.pharm.analysis.PopulationSampling.PatientProcessor;
import biouml.plugins.pharm.analysis.PopulationSamplingParameters;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.Util;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestPopulationHallow extends AbstractBioUMLTest implements PatientProcessor
{
    public TestPopulationHallow(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestPopulationHallow.class.getName());
        suite.addTest(new TestPopulationHallow("simpleTest"));
        return suite;
    }

    //STRUCTURAL MODEL
    private String diagramName = "Complex model Hallow plain";
    private String diagramCollection = "data/Collaboration (git)/Cardiovascular system/Complex model/";

    //OUTPUT PATH
    private boolean appendToExisting = true;//if true then new patients will be appended to existing in output folder otherwise output folder will be cleaned
    private String resultFolder = "/Results Hallow Population 13 nov C_at fixed constr";
    private String observedResult = resultFolder + "/Observed.txt";
    private String parametersResult = resultFolder + "/Parameters.txt";
    private String fullResult = resultFolder + "/All.txt";

    //POPULATION PARAMETRES
//    private String[] modelParameters = {"Heart\\V_AL0", "Heart\\V_HL0", "Heart\\Y_ALVL0", "Heart\\Y_HLAL", "Heart\\w_AL0", "Heart\\w_HL0",
//            "Heart\\w_VL0", "Heart\\G_AL0", "Heart\\G_HL", "Heart\\G_VL0"};
    
    private String[] modelParameters = {"C_AT1_ea", "c_chym", "R_bv2", "A_al_DT", "A_al_CD", "B_al_DT", "B_al_CD", "C_K", "Fi_sodin", "B_AT1_ea"};

    
    private String[] observedNames = {"P_ma2"};

    //DESIRED DISTRIBUTION
    private double[] mean = new double[] {120};
    private double[][] sd = new double[][] {{25}};

    //ALGORITHM PARAMETERS
    int acceptanceRate = 5;//how many patients should be skiped (e.g. each 5th patient will be added to population)
    int populationSize = 5000; //desired population size
    int preliminarySteps = 10; //how many patients will be skipped at start (to "forget" about initial values
    double atol = 10; //absolute tolerance for steady state
    //here we set atol = 10 to guarantee that simulation will be stopped at model time = startSearchTime
    double startSearchTime = 1E8; //start model time for steady state detection
    int validationSize = 1; //number of consequent time points at which model should demonstrate steady state
    
    private boolean debug = true;
    
    //TECHNICAL
    private boolean started = false;
    //id of first patient
    private int id = 1;
    private PopulationSampling analysis;
    
    public void simpleTest() throws Exception
    {
        Diagram d = getDiagram();

        prepare(d);
        
        //here we retrieve values of parameters from diagram and adjust initial values and steps for algorithm for each parameter
        TableDataCollection initialData = prepareInitialData(d, modelParameters);

        analysis = new PopulationSampling(null, "");
        analysis.setDebug(debug);
        analysis.setObservedDistribution(mean, sd);

        PopulationSamplingParameters parameters = analysis.getParameters();
        parameters.setInitialData(initialData);
        parameters.setDiagram(getDiagram());
        parameters.setObservedVariables(new VariableSet(d, observedNames));
        parameters.setEstimatedVariables(new VariableSet(d, modelParameters));

        //main parameters of algorithm
        parameters.setAcceptanceRate(acceptanceRate);//how many patients should be skiped (e.g. each 5th patient will be added to population)
        parameters.setPopulationSize(populationSize); //desired population size
        parameters.setPreliminarySteps(preliminarySteps); //how many patients will be skipped at start (to "forget" about initial values
        parameters.setAtol(atol); //absolute tolerance for steady state
        parameters.setStartSearchTime(startSearchTime); //start model time for steady state detection
        parameters.setValidationSize(validationSize); //number of consequent time points at which model should demonstrate steady state
        parameters.getEngineWrapper().getEngine().setCompletionTime(1E9);
        parameters.getEngineWrapper().getEngine().setTimeIncrement(1000);
        
        //set this test as processor to write data about patients on the fly
        analysis.setPatientProcessor(this);

        double startTime = System.currentTimeMillis();
        analysis.justAnalyze();
        System.out.println("Elapsed time: " + ( System.currentTimeMillis() - startTime ) / 1000);
    }

    private TableDataCollection prepareInitialData(Diagram d, String[] modelParameters)
    {
        double[][] initialData = new double[modelParameters.length][];

        for( int i = 0; i < modelParameters.length; i++ )
        {
            Diagram parameterDiagram = d;
            String parameterName = modelParameters[i];
            if( modelParameters[i].contains("\\") )
            {
                String subDiagramName = modelParameters[i].substring(0, modelParameters[i].lastIndexOf("\\"));
                parameterDiagram = ( (SubDiagram)d.get(subDiagramName) ).getDiagram();
                parameterName = modelParameters[i].substring(modelParameters[i].lastIndexOf("\\") + 1, modelParameters[i].length());

            }
            EModel emodel = parameterDiagram.getRole(EModel.class);
            Variable var = emodel.getVariable(parameterName);
            initialData[i] = new double[] {var.getInitialValue(), var.getInitialValue() / 30, 0, var.getInitialValue() * 1.5};
        }

        return TableDataCollectionUtils.createTable("init", initialData, new String[] {"Mean", "Variance", "Min", "Max"}, modelParameters);
    }

    private Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(diagramCollection);
        DataElement de = collection.get(diagramName);
        return (Diagram)de;
    }

    private void writeResult(PopulationSampling analysis, List<Patient> result) throws Exception
    {

        getTestFile(resultFolder).delete();
        getTestFile(resultFolder).mkdirs();
        String header = StreamEx.of(observedNames).joining("\t");
        ApplicationUtils.writeString(getTestFile(observedResult),
                StreamEx.of(result).map(p -> DoubleStreamEx.of(p.getObserved()).joining("\t")).prepend(header).joining("\n"));

        header = StreamEx.of(modelParameters).joining("\t");
        ApplicationUtils.writeString(getTestFile(parametersResult),
                StreamEx.of(result).map(p -> DoubleStreamEx.of(p.getInput()).joining("\t")).prepend(header).joining("\n"));

        Map<String, Integer> mapping = analysis.getParameters().getEngineWrapper().getEngine().getVarPathIndexMapping();
        Map<Integer, String> inverted = EntryStream.of(mapping).invert().toSortedMap();

        header = StreamEx.of(inverted.values()).joining("\t");
        ApplicationUtils.writeString(getTestFile(fullResult),
                StreamEx.of(result).map(p -> DoubleStreamEx.of(p.getAllValues()).joining("\t")).prepend(header).joining("\n"));
    }

    File inputFile;
    File observedFile;
    File fullFile;

    private void initFiles(SimulationEngine engine) throws IOException
    {
        getTestFile(resultFolder).mkdirs();
        observedFile = getTestFile(observedResult);
        observedFile.createNewFile();

        inputFile = getTestFile(parametersResult);
        inputFile.createNewFile();

        fullFile = getTestFile(fullResult);
        fullFile.createNewFile();

        if (!appendToExisting)
        {
            observedFile.delete();
            inputFile.delete();
            fullFile.delete();
        }
        
        observedFile.createNewFile();
        inputFile.createNewFile();
        fullFile.createNewFile();
            
        id = ApplicationUtils.readAsList(fullFile).size();

        if( id != 0 )
        {
            id--; //account for existing header
            return;
        }
        
        try (BufferedWriter result = ApplicationUtils.utfAppender(observedFile);
                BufferedWriter input = ApplicationUtils.utfAppender(inputFile);
                BufferedWriter full = ApplicationUtils.utfAppender(fullFile))
        {


            String inputHeader = StreamEx.of(modelParameters).prepend("ID").joining("\t") + "\n";
            input.write(inputHeader);

            Map<String, Integer> mapping = engine.getVarPathIndexMapping();
            Map<Integer, String> inverted = EntryStream.of(mapping).invert().toSortedMap();

            String fullHeader = StreamEx.of(inverted.values()).prepend("ID").joining("\t") + "\n";
            full.write(fullHeader);

            result.write(StreamEx.of(observedNames).prepend("ID").joining("\t") + "\n");
        }
    }

    private void writePatient(Patient patient)
    {
        try (BufferedWriter result = ApplicationUtils.utfAppender(observedFile);
                BufferedWriter input = ApplicationUtils.utfAppender(inputFile);
                BufferedWriter full = ApplicationUtils.utfAppender(fullFile))
        {
            result.append(DoubleStreamEx.of(patient.getObserved()).prepend(id).joining("\t") + "\n");
            input.append(DoubleStreamEx.of(patient.getInput()).prepend(id).joining("\t") + "\n");
            full.append(DoubleStreamEx.of(patient.getAllValues()).prepend(id).joining("\t") + "\n");

            id++;
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }


    @Override
    public void process(Patient patient)
    {
        try
        {
            if( !started )
            {
                started = true;
                initFiles(analysis.getParameters().getEngineWrapper().getEngine());
            }
            writePatient(patient);
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
    
    protected static void prepare(Diagram diagram)
    {
        Set<String> variables = new HashSet<>();
        variables.add("Fi_cd_sodreab");
        variables.add("Fi_co");
        variables.add("Fi_dt_sod");
        variables.add("Fi_dt_sodreab");
        variables.add("Fi_filsod");
        variables.add("Fi_gfilt");
        variables.add("Fi_md_sod3");
        variables.add("Fi_pt_sodreab"); 
        variables.add("Fi_rb");
        variables.add("Fi_u_sod");
        variables.add("Fi_sodin");
        variables.add("Fi_t_wreab");
        addConstraints(diagram, "", variables);
    }
    
    private static void addConstraints(Diagram diagram, String path, Set<String> variables)
    {
        Diagram innerDiagram = diagram;
        if( !path.isEmpty() )
        {
            SubDiagram subDiagram = Util.getSubDiagram(diagram, path);
            if( subDiagram == null )
            {
                System.out.println("Subdiagram "+path+" not found!");
                return;
            }
            innerDiagram = Util.getSubDiagram(diagram, path).getDiagram();
        }
        for( String varName : variables) 
        {
            Constraint constraint = new Constraint(null, varName + " > 0",  varName +" became negative");
            DiagramElementGroup de = innerDiagram
                    .getType()
                    .getSemanticController()
                    .createInstance(innerDiagram, Constraint.class, new Point(),
                            constraint);
            de.putToCompartment();
        }
    }
}
