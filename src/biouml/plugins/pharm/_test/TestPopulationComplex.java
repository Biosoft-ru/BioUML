package biouml.plugins.pharm._test;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

public class TestPopulationComplex extends AbstractBioUMLTest implements PatientProcessor
{
    public TestPopulationComplex(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestPopulationComplex.class.getName());
        suite.addTest(new TestPopulationComplex("simpleTest"));
        return suite;
    }

    //STRUCTURAL MODEL
    private String diagramName = "agent";//Complex model new";
    private String diagramCollection = "data/Collaboration/Ilya/Data/Diagrams/CVS models/Fedors Disser/";//"data/Collaboration (git)/Cardiovascular system/Complex model/";

    //OUTPUT PATH
    private boolean appendToExisting = true;//if true then new patients will be appended to existing in output folder otherwise output folder will be cleaned
//    private String resultFolder = "/Results_Ovch_1312/9";
//    private String initialDataPath = "Ovcharenko_init/1.txt";
    
    private String resultFolder = "/Results_260221/";
    private String initialDataPath =null;//"/Init_Kamanov/8.txt";
    
    
    private String observedResult = resultFolder + "/Observed.txt";
    private String parametersResult = resultFolder + "/Parameters.txt";
    private String fullResult = resultFolder + "/All.txt";

    //POPULATION PARAMETRES
//	private String[] modelParameters = { "Heart\\Heart_Stress", "Heart\\VO_2_pre", "Heart\\Y_ALVL0",
//			"Heart\\w_AL0", "Heart\\w_AR0", "Heart\\AO_2", "Heart\\G_AL0",
//			"Heart\\Heart_Baro", "Heart\\Heart_Base", "Kidney\\A_AT1_ea", "Kidney\\B_AT1_ea", "Kidney\\R_ea_0",
//			"Kidney\\V_ecf2", "Kidney\\n_eta_pt" };
	
//    private String[] modelParameters = {"Heart\\A_3","Heart\\A_12", "Heart\\AO_2", "Heart\\G_AL0", "Heart\\Heart_Base", "Heart\\Heart_Stress", "Heart\\VO_2_pre",
//            "Heart\\K_L0", "Heart\\K_R0", "Heart\\RO_20", "Heart\\w_VR0", "Kidney\\A_AT1_ea", "Kidney\\C_AT1_ea", "Kidney\\R_ea_0",
//            "Heart\\Y_ALVL0", "Kidney\\B_AT1_ea", "Kidney\\N_nephrons", "Heart\\Y_HRAR", "Heart\\FS_threshold0"};
    
//    private String[] modelParameters = {"kidney/R_aa_0", "kidney/P_B", "kidney/N_rsna", "kidney/N_rs", "kidney/h_ANGIV", "kidney/C_GP_auto",
//            "kidney/C_AT1_preglom", "kidney/c_ACE2", "kidney/A_al_DT", "heart/Y_ARVR0", "heart/Y_ALVL0", "heart/w_VL0", "heart/w_HR0",
//            "heart/w_AR0", "heart/w_AL0", "heart/RO_20", "heart/K_VRHL", "heart/K_L0", "heart/Hematocrit",
//            "heart/Heart_Stress", "heart/Heart_Oxygen", "heart/Heart_Base", "heart/G_HR", "heart/G_HL", "heart/G_AR0",
//            "heart/FS_threshold0", "heart/A_8", "heart/A_5", "heart/A_18", "heart/A_16", "heart/A_15", "heart/A_13"};

    private String[] modelParameters = {"heart/Y_ALVL0", "heart/VO_2_pre", "kidney/R_ea_0", "kidney/P_go", "kidney/P_B", "kidney/N_rsna",
            "kidney/N_nephrons", "kidney/n_eta_pt", "kidney/n_eta_cd", "kidney/n_eps_dt", "kidney/ksi_map", "kidney/K_f",
            "heart/Heart_Base", "heart/G_AL0", "kidney/Fi_sodin", "kidney/C_GP_auto", "kidney/B_al_DT", "kidney/B_al_CD", "kidney/A_al_DT",
            "kidney/A_al_CD"};

    
  //Abstract data
    private String[] observedNames = { "heart/P_D", "heart/P_S"};
    private double[] mean = new double[] {100, 160};
//    private double[][] sd = 
//  	      { { 123.21, 120.49 },
//  			{ 120.49, 282.24 }};
  private double[][] sd = 
  { { 121, 119.68 },
    { 119.68, 289 }};
    
//    private String[] individualParameters = { };
//    private double[] individualValues = { };
//    private double[] error = new double[]{40, 40};
    
  //Kamanov data
//    private String[] observedNames = { "Heart\\P_D", "Heart\\P_S", "Heart\\Heart_Rate"};
//    private double[] mean = new double[] {95, 148, 52};
//    private double[][] sd = 
//	      { { 123.21, 120.49, -6.682 },
//			{ 120.49, 282.24, 5.74 },
//			{ -6.682, 5.74, 114.49}};
//			
//    private String[] individualParameters = { "Heart/m", "Kidney/m", "Heart/He", "Heart/Hematocrit"};
//    private double[] individualValues = { 94.0, 94.0, 138, 40};
    
//	private String[] observedNames = { "Heart\\P_D", "Heart\\P_S", "Heart\\Heart_Rate", "Heart\\CO" };

    //DESIRED DISTRIBUTION
	//Ovch data
//    private double[] mean = new double[] {90, 160, 70, 80};
    //Uvarovskaya data
//	private double[] mean = new double[] {95, 148, 52};
	
    
    
//	private double[][] sd = 
//	      { { 123.21, 120.49, -6.682, 70 },
//			{ 120.49, 282.24, 5.74, 60 },
//			{ -6.682, 5.74, 114.49, 6},
//			{70, 60, 6, 100}};
    
    
    	
//	private String[] individualParameters = { "Heart/m", "Kidney/m", "Heart/He", "Heart/Hematocrit"};
			//"Kidney/C_K", "Kidney/C_sod"};//, "Heart/He" };
//	private double[] individualValues = { 70.0, 70.0, 4.5, 143, 49.4, 160};//, 160};//, 49.4};

	//Kamanov data
//    private String[] individualParameters = { "Heart/m", "Kidney/m", "Heart/He", "Heart/Hematocrit"};
//	private double[] individualValues = { 94.0, 94.0, 138, 40};
//	private String[] observedNames = { "Heart\\P_S", "Heart\\P_D",  "Heart\\Heart_Rate" };
//	private double[] mean = new double[] {148, 95, 52};
//	 private double[][] sd = 
//		      { { 205.6649306, 114.7118056,-2.416319444 },
//				{ 114.7118056, 143.2830556, 29.59430556 },
//				{ -2.416319444, 29.59430556,  114.6999306}};
//    private double[] error = new double[]{10, 10, 10};
    
    //Uvarovskaya data
//    private String[] individualParameters = { "Heart/m", "Kidney/m", "Heart/He", "Heart/Hematocrit"};
//	private double[] individualValues = { 70.0, 70.0, 160, 49.4};
//	private String[] observedNames = { "Heart\\P_S", "Heart\\P_D",  "Heart\\Heart_Rate", "Heart\\CO", "Heart\\V_HL_KD" };
//	private double[] mean = new double[] {152, 100, 70, 41, 58.6};
//    
//    private double[] error = new double[]{10, 10, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 60};
//	 
//	private double[][] sd = {
//			{ 205.6649306, 114.7118056, -2.416319444, 44.940625, 85.71331338 },
//			{ 114.7118056, 143.2830556, 29.59430556, 31.40208333, 63.55855343 },
//			{ -2.416319444, 29.59430556, 114.6999306, -12.11145833, -14.3025035 },
//			{ 44.940625, 31.40208333, 63.55855343, 111.7927083, 207.453071 },
//			{ 85.71331338, 63.55855343,	-14.3025035,	207.453071,	552.2018241}};

	
	//Ovch data
//	вес 73 кг
//	АД 160/90 
//	ЧСС 72 уд/мин
//	УО 79 мл
//	Фракция выброса 76%
//	Конечно-диастолический объем 104 мл
//	Гемоглобин 141 г/л
//	Гематокрит 43%
	
//	private String[] individualParameters = { "Heart/m", "Kidney/m", "Heart/He", "Heart/Hematocrit"};
//	private double[] individualValues = { 73.0, 73.0, 141, 43};
//	private String[] observedNames = { "Heart\\P_S", "Heart\\P_D",  "Heart\\Heart_Rate", "Heart\\CO", "Heart\\V_HL_KD" };
//	private double[] mean = new double[] {160, 90, 72, 79, 104};
	
    
    //Tkachuk Data
//  private String[] individualParameters = { "Heart/m", "Kidney/m", "Heart/He", "Heart/Hematocrit"};
//	private double[] individualValues = { 108, 108, 173, 55};
//	private String[] observedNames = { "Heart\\P_S", "Heart\\P_D",  "Heart\\Heart_Rate", "Heart\\CO", "Heart\\V_HL_KD" };
//	private double[] mean = new double[] {180, 129, 78, 81, 121};
//  
//  private double[] error = new double[]{25, 15, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 60};
//	 
//	private double[][] sd = {
//			{ 205.6649306, 114.7118056, -2.416319444, 44.940625, 85.71331338 },
//			{ 114.7118056, 143.2830556, 29.59430556, 31.40208333, 63.55855343 },
//			{ -2.416319444, 29.59430556, 114.6999306, -12.11145833, -14.3025035 },
//			{ 44.940625, 31.40208333, 63.55855343, 111.7927083, 207.453071 },
//			{ 85.71331338, 63.55855343,	-14.3025035,	207.453071,	552.2018241}};
//    Рост 167 см, вес 108 кг
//    АД 180/129
//    ЧСС 78 уд/мин
//    Ударный объем 81 мл
//    Фракция выброса 67%
//    Конечно-диастолический объем 121 мл
//    Глюкоза 5.02 ммоль/л
//    Гемоглобин 173 г/л
//    Гематокрит 55 %


	//Sokolov Data
//	Вес 74 кг, Рост 165 см
//	АД 118 / 67 (периодически повышалось до 168/91)
//	ЧСС 54 уд/мин
//	Гемоглобин 150 г/л
//	Гематокрит 48.2
//	Холестерин    (в норме)
//	 private String[] individualParameters = { "Heart/m", "Kidney/m", "Heart/He", "Heart/Hematocrit"};
//		private double[] individualValues = { 74, 74, 150, 48.2};
//		private String[] observedNames = { "Heart\\P_S", "Heart\\P_D",  "Heart\\Heart_Rate" };
//		private double[] mean = new double[] {118, 67, 54};
//	  
//	  private double[] error = new double[]{15, 15, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 60};
//		 
//		private double[][] sd = {
//				{ 205.6649306, 114.7118056, -2.416319444 },
//				{ 114.7118056, 143.2830556, 29.59430556 },
//				{ -2.416319444, 29.59430556, 114.6999306 }};
    
//    Рост 176 см, вес 105 кг
//    АД 135/88 (до 191/111)
//    ЧСС 58 уд/мин
//    Глюкоза 5.73 ммоль/л
//    Холестерин ЛПВП (понижен)
//    Натрий 138 ммоль/л
//    Калий 4.2 ммоль/л
//    Гематокрит 39.1%
//    Гемоглобин 126 г/л
    private String[] individualParameters = {};//"Heart/m", "Kidney/m", "Heart/He", "Heart/Hematocrit"};
    private double[] individualValues = {105, 105, 126, 39.1};
//    private String[] observedNames = {"heart/P_S", "heart/P_D", "heart/Heart_Rate"};
//    private double[] mean = new double[] {118, 67, 54};

    private double[] error = new double[] {15, 15, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 60};

//    private double[][] sd = {{205.6649306, 114.7118056, -2.416319444}, {114.7118056, 143.2830556, 29.59430556},
//            { -2.416319444, 29.59430556, 114.6999306 }};
    
    //ALGORITHM PARAMETERS
    int acceptanceRate = 5;//how many patients should be skiped (e.g. each 5th patient will be added to population)
    int populationSize = 100000; //desired population size
    int preliminarySteps = 10; //how many patients will be skipped at start (to "forget" about initial values
    double atol = 100; //absolute tolerance for steady state
    //here we set atol = 10 to guarantee that simulation will be stopped at model time = startSearchTime
    double startSearchTime = 2E6; //start model time for steady state detection
    int validationSize = 1; //number of consequent time points at which model should demonstrate steady state
    
    private boolean debug = true;
    
    //TECHNICAL
    private boolean started = false;
    //id of first patient
    private int id = 121;
    private PopulationSampling analysis;
    
    public void simpleTest() throws Exception
    {
        Diagram d = getDiagram();
        prepare(d);
        
        
        for (int i=0; i<individualParameters.length;i++)
        {
        	String paramPath = individualParameters[i];
        	String[] path = paramPath.split("/");
        	String module = path[0];
        	String name = path[1];
        	SubDiagram subDiagram = (SubDiagram)d.get(module);
        	subDiagram.getDiagram().getRole(EModel.class).getVariable(name).setInitialValue(individualValues[i]);
        	
		}
		// here we retrieve values of parameters from diagram and adjust initial
		// values and steps for algorithm for each parameter
		TableDataCollection initialData = prepareInitialData(d, modelParameters,
				initialDataPath != null ? readValues(initialDataPath, modelParameters) : null);

		analysis = new PopulationSampling(null, "");
        analysis.setDebug(debug);
        analysis.setObservedDistribution(mean, sd);
        analysis.setAcceptedError(mean, error);
        
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

        //set this test as processor to write data about patients on the fly
        analysis.setPatientProcessor(this);

        double startTime = System.currentTimeMillis();
        analysis.justAnalyze();
        System.out.println("Elapsed time: " + ( System.currentTimeMillis() - startTime ) / 1000);
    }

    private TableDataCollection prepareInitialData(Diagram d, String[] modelParameters, double[] values)
    {
        double[][] initialData = new double[modelParameters.length][];

        for( int i = 0; i < modelParameters.length; i++ )
        {
            Diagram parameterDiagram = d;
            String parameterName = modelParameters[i];
            if( modelParameters[i].contains("/") )
            {
                String subDiagramName = modelParameters[i].substring(0, modelParameters[i].lastIndexOf("/"));
                parameterDiagram = ( (SubDiagram)d.get(subDiagramName) ).getDiagram();
                parameterName = modelParameters[i].substring(modelParameters[i].lastIndexOf("/") + 1, modelParameters[i].length());

            }
            EModel emodel = parameterDiagram.getRole(EModel.class);
            if (!emodel.containsVariable(parameterName))
            	System.out.println(parameterName+" not found");
            double val = values != null? values[i]: emodel.getVariable(parameterName).getInitialValue();
            initialData[i] = new double[] {val, val / 30, 0, val * 2};
        }

        return TableDataCollectionUtils.createTable("init", initialData, new String[] {"Mean", "Variance", "Min", "Max"}, modelParameters);
    }
    
    
    public double[] readValues(String filePath, String[] names) throws IOException
    {
    	File f = getTestFile(filePath);
    	List<String> list = ApplicationUtils.readAsList(f);
    	String[] header = list.get(0).split("\t");
    	double[] values = StreamEx.of(list.get(1).split("\t")).mapToDouble(s->Double.parseDouble(s)).toArray();
    	
    	double[] result = new double[names.length];
    	for (int j=0; j<names.length; j++)
    	{
    		for (int i=0; i<header.length; i++)
    		{
    			if (header[i].equals(names[j]))
    				result[j] = values[i];
    		}
    	}
    	
    	return result;
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
    	variables.add("Fi_cd_sodreab > 0");
    	variables.add("Fi_co > 0");
    	variables.add("Fi_dt_sod > 0");
    	variables.add("Fi_dt_sodreab > 0");
    	variables.add("Fi_filsod > 0");
//    	variables.add("Fi_u_sod");
    	variables.add("Fi_gfilt > 0");
    	variables.add("Fi_md_sod3 > 0");
    	variables.add("Fi_pt_sodreab > 0");	
    	variables.add("Fi_rb > 0");
    	variables.add("Fi_sodin > 0");
    	variables.add("Fi_t_wreab > 0");
    	addConstraints(diagram, "Kidney", variables);
    	
    	Set<String> variables2 = new HashSet<>();
    	variables2.add("V_HL > o");
//    	variables2.add("P_S");
//    	variables2.add("P_D");
    	addConstraints(diagram, "Heart", variables2);
    }
    
    
	private static void addConstraints(Diagram diagram, String path, Set<String> formulas)
	{
		SubDiagram subDiagram = Util.getSubDiagram(diagram, path);
		if (subDiagram == null)
			return;
		Diagram innerDiagram = Util.getSubDiagram(diagram, path).getDiagram();
		for (String formula : formulas) 
		{
			Constraint constraint = new Constraint(null, formula, formula +" is false");
			DiagramElementGroup de = innerDiagram
					.getType()
					.getSemanticController()
					.createInstance(innerDiagram, Constraint.class, new Point(),
							constraint);
			de.putToCompartment();
		}
	}
	
	public static Map<String,String[]> negatives = new HashMap<>();
	{
		negatives.put("Heart", new String[] { "DTS_L", "DTS_R", "V_AL_Change",
				"V_HL_Change", "V_HR_Change" });
		negatives.put("Kidney", new String[]{"delta_ra","delta_baro"});
	}
	

}
