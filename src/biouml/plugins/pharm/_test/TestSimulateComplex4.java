package biouml.plugins.pharm._test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.pharm.prognostic.TreatmentSystemParameters;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.ode.OdeModel;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestSimulateComplex4 extends AbstractBioUMLTest
{
    public TestSimulateComplex4(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestSimulateComplex4.class.getName());
        suite.addTest(new TestSimulateComplex4("test"));
        return suite;
    }

//    private static String patientIndex = "71.0";
    private boolean overwrite = true;  

    private static final String[] selected = new String[] {"heart/P_S", "heart/P_D", "heart/V", "heart/Heart_Rate"};

    private static int findAlreadyProcessed(String resultInfo) throws Exception
    {
        return ApplicationUtils.readAsList(getTestFile(resultInfo)).size();
    }
    
    public void filterInvalid(String inputFileName, String outputFileName) throws Exception
    {
    	File input = getTestFile(inputFileName);
    	File output = getTestFile("/Results260221/All_Filtered.txt");
    	Set<String> negativeParameters = new HashSet<>();
    	negativeParameters.add("heart\\V_HL_Change");
    	negativeParameters.add("heart\\V_HR_Change");
    	negativeParameters.add("heart\\V_AL_Change");
    	negativeParameters.add("heart\\V_AR_Change");
    	negativeParameters.add("heart\\V_VL_Change");
    	negativeParameters.add("heart\\V_VR_Change");
    	negativeParameters.add("heart\\DTS_L");
    	negativeParameters.add("heart\\DTS_R");
    	negativeParameters.add("kidney\\delta_baro");
    	negativeParameters.add("kidney\\delta_ra");
    	filterPopulation(input, output, negativeParameters);
    }
    
    public void test() throws Exception
    {
        testDrug(TreatmentSystemParameters.AMLODIPINE_5_MG, "/71/Amlo");
//        testDrug(TreatmentSystemParameters.AMLODIPINE_10_MG, "/71/Amlo_71");
//        testDrug(TreatmentSystemParameters.LOSARTAN_100_MG, "/71/Los100_71");        
//        testDrug(TreatmentSystemParameters.ALISKIREN_150_MG, "/71/Alis_150_71");
//        testDrug(TreatmentSystemParameters.ENALAPRIL_20_MG, "/71/Enal_20");
    }
    
    private void testDrug(String drug, String name) throws Exception
    {
        String path = "N/";
        Treatment t = new Treatment(drug, path+"All.txt", path+name, true);
        t.start();
        t.join();
    }

    public static Diagram getDiagram(String diagramPath, String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(diagramPath);
        DataElement de = collection.get(name);
        return (Diagram)de;
    }
    
    private static class Treatment extends Thread
    {
        private String inputFileName;
        private String resultFolder;
        private Diagram d;
        private boolean overwrite;
        private String drug;
        
    	public Treatment(String drug, String inputFileName, String resultFolder, boolean overwrite) throws Exception
    	{
    		this.overwrite = overwrite;
    		this.inputFileName = inputFileName;
    		this.resultFolder = resultFolder;
            this.d = getDiagram("data/Collaboration/Ilya/Data/Diagrams/CVS models/Fedors Disser/March/", "agent");
            this.drug = drug;
    	}
    	
    	 @Override
         public void run()
         {
    		 String outputFileName = resultFolder + "/Treated.txt";
             String statisticsFileName = resultFolder + "/Statistics.txt";
             String historyPSFileName = resultFolder + "/history PS.txt";
             String historyPDFileName = resultFolder + "/history PD.txt";
             String historyHRFileName = resultFolder + "/history HR.txt";
             String resultInfo = resultFolder + "/INFO.txt";
             
			TestPopulationComplex.prepare(d);

			try 
			{
				for (Equation eq : d.getRole(EModel.class).getInitialAssignments())
					d.getType().getSemanticController().remove(eq.getDiagramElement());

				for (SubDiagram subDiagram : Util.getSubDiagrams(d)) 
				{
					Diagram diagram = subDiagram.getDiagram();
					for (Equation eq : diagram.getRole(EModel.class).getInitialAssignments())
						diagram.getType().getSemanticController().remove(eq.getDiagramElement());
				}
			} 
			catch (Exception ex) 
			{
				ex.printStackTrace();
				return;
			}
			try 
			{
				getTestFile(resultFolder).mkdirs();
				getTestFile(resultInfo).createNewFile();

            SimulationEngine engine = DiagramUtility.getPreferredEngine(d);
            engine.setDiagram(d);
            engine.setCompletionTime(2E6);
//            engine.setTimeIncrement(1000);
            engine.setLogLevel( Level.SEVERE );
            
            for (AgentSimulationEngineWrapper innerEngine: ((AgentModelSimulationEngine)engine).getEngines())
            {
            	JVodeSolver solver = (JVodeSolver)innerEngine.getSolver();//new JVodeSolver();
            	solver.getOptions().setDetectIncorrectNumbers(true);
//            	solver.getOptions().setAtol(1E-6);
//            	solver.getOptions().setRtol(1E-6);
//            	innerEngine.setSolver(solver);
            }
            	
            Model model = engine.createModel();

            List<String> patientsData = ApplicationUtils.readAsList(getTestFile(inputFileName));

            String[] header = patientsData.get(0).split("\t"); //variable names in file

            Map<String, Integer> mapping = engine.getVarPathIndexMapping(); //mapping for new model

            model.init();

            double[] initialValues = model.getCurrentValues();           	
            
            int alreadyProcessed = overwrite? 0: findAlreadyProcessed(resultInfo);
            //writing headers
            if( alreadyProcessed == 0 )
            {
                try (BufferedWriter resultWriter = ApplicationUtils.utfAppender(getTestFile(outputFileName)))
                {
                    Map<Integer, String> inverted = EntryStream.of(mapping).invert().toSortedMap();
                    String newHeader = StreamEx.of(inverted.values()).prepend("ID").joining("\t");
                    resultWriter.append(newHeader);
                }

                try (BufferedWriter statisticsWriter = ApplicationUtils.utfAppender(getTestFile(statisticsFileName)))
                {
                    statisticsWriter.append(StreamEx.of(selected).map(s -> "Old " + s).append(selected).prepend("ID").joining("\t"));
                }
            }

            int total = patientsData.size() - 1;
            for( int i = 1 + alreadyProcessed; i < patientsData.size(); i++ )
            {
            	
                String data = patientsData.get(i);
                String[] row = data.split("\t");
                double[] patientsValues = StreamEx.of(row).skip(1).mapToDouble(s -> Double.parseDouble(s)).toArray();

                double[] modelValues = initialValues.clone();
                String ID = row[0];
                for( int j = 1; j < header.length; j++ )
                {

                    if( header[j].contains("time") ) //avoid initial time setting
                        continue;

//                    if( header[j].equals("ID") )
//                    {
//                        ID = patientsValues[j];
//                        continue;
//                    }

                    if (mapping.containsKey(header[j]))
                    {
                        
                    int index = mapping.get(header[j]);
                    modelValues[index] = patientsValues[j-1];
//                    System.out.println( header[j] +" : "+modelValues[index] );
                    }
                }
                    boolean drugFound = false;
					String drugName = TreatmentSystemParameters.getParameter(drug);
					if (mapping.containsKey("kidney/" + drugName)) 
					{
						int index = mapping.get("kidney/" + drugName);
						modelValues[index] = 1;
						drugFound = true;
					} 
					if (mapping.containsKey("heart/" + drugName)) 
					{
					    int index = mapping.get("heart/" + drugName);
                        modelValues[index] = 1;
                        drugFound = true;
					}
					if(!drugFound)
					{
					    System.out.println("Drug parameter not found: "+drugName);
					}
//                if (ID < patientIndex)
//                	continue;
//                if (!ID.equals( patientIndex))
//            		continue;
                
//                for (int j=0; j<header.length; j++)
//                {
//                	if (patientsValues[j] < 0 )
//                		System.out.println(header[j] +" = "+patientsValues[j]);
//                }
//					int[] ind = StreamEx.of(selected).mapToInt(s -> mapping.get(s)).toArray();
//
//					System.out.println(ID + "\t" + modelValues[ind[0]] + "\t" + modelValues[ind[1]]);
//					if (Math.abs(modelValues[ind[0]] - 160) > 10 || Math.abs(modelValues[ind[1]] - 90) > 10)
//						continue;
////						else
//							System.out.println(
//									"Patient " + ID + " is ok!: " + modelValues[ind[0]] + " / " + modelValues[ind[1]]);
						
					
					model.init();
                model.setCurrentValues(modelValues);

//                Map<Integer, String> inverted = EntryStream.of(mapping).invert().toSortedMap();
//                String newHeader = StreamEx.of(inverted.values()).joining("\t");
//                double[] curValues = model.getCurrentValues();
//                System.out.println( newHeader );
//                System.out.print( DoubleStreamEx.of( modelValues ).joining( "\t" ) );
                double timeStart = System.currentTimeMillis();
                HistoryListener listener = new HistoryListener(StreamEx.of(selected).mapToInt(s->mapping.get(s)).toArray());
                
                double[] before = StreamEx.of(selected).mapToDouble(s->modelValues[mapping.get(s)]).toArray();
                System.out.println("Patient "+ID+": "+DoubleStreamEx.of(before).joining(" "));
                
                
//                List<ResultListener> listeners = new ArrayList<ResultListener>();
//                listeners.add(listener);
//                StreamEx.of(engine.getPlots()).forEach(p->listeners.add(new ResultPlotPane(engine, null, p)));
                
//                ResultListener[] listenerArray = new ResultListener[listeners.size()+1];
//                for (int j=0; j<listeners.size(); j++)
//                	listenerArray[j] = listeners.get(j);
//                listenerArray[listeners.size()] = listener;
                
                engine.simulate(model, new ResultListener[]{listener});
                double[] newValues = model.getCurrentValues();
                double[] after = StreamEx.of(selected).mapToDouble(s->newValues[mapping.get(s)]).toArray();
                double[] diff = new double[before.length]; 
                for (int j=0; j<before.length; j++)
                	diff[j] = before[j] - after[j]; 
                System.out.println("Patient "+ID+": "+DoubleStreamEx.of(diff).joining(" "));
                
                
                boolean constraintViolated = false;
                for (SimulationAgent agent: ((AgentBasedModel)model).getAgents())
                {
                	if (agent instanceof ModelAgent)
                	if (((OdeModel)((ModelAgent)agent).getModel()).isConstraintViolated())
                		constraintViolated = true;
                }
                
                String err = engine.getSimulator().getProfile().getErrorMessage();
                if( err != null || constraintViolated)
                {
                    System.out.println("Patient " + ID + " Failed");
                    System.out.println(System.currentTimeMillis() - timeStart);
                    try (BufferedWriter infoWriter = ApplicationUtils.utfAppender(getTestFile(resultInfo)))
                    {
                        infoWriter.append("\n" + ID + "\tFailed");
                    }
                    continue;
                }
                else
                {
                    try (BufferedWriter infoWriter = ApplicationUtils.utfAppender(getTestFile(resultInfo)))
                    {
                        infoWriter.append("\n" + ID + "\tOK");
                    }
                }
               
                try (BufferedWriter statisticsWriter = ApplicationUtils.utfAppender(getTestFile(statisticsFileName)))
                {
                    double[] selectedOld = StreamEx.of(selected).mapToDouble(s -> modelValues[mapping.get(s)]).toArray();
                    double[] selectedNew = StreamEx.of(selected).mapToDouble(s -> newValues[mapping.get(s)]).toArray();
                    double[] selectedDiff = StreamEx.of(selected).mapToDouble(s -> (modelValues[mapping.get(s)] - newValues[mapping.get(s)])).toArray();
                    statisticsWriter.append("\n" + DoubleStreamEx.of(selectedOld).append(selectedNew).append(selectedDiff).mapToObj(s->Double.toString(s)).prepend(ID).joining("\t"));
                }

                try (BufferedWriter resultWriter = ApplicationUtils.utfAppender(getTestFile(outputFileName)))
                {
                    resultWriter.append("\n" + DoubleStreamEx.of(newValues).mapToObj(s->Double.toString(s)).prepend(ID).joining("\t"));
                }

                try (BufferedWriter psWriter = ApplicationUtils.utfAppender(getTestFile(historyPSFileName));
                        BufferedWriter pdWriter = ApplicationUtils.utfAppender(getTestFile(historyPDFileName));
                        BufferedWriter hrWriter = ApplicationUtils.utfAppender(getTestFile(historyHRFileName)))
                {
                    psWriter.append("\n" + StreamEx.of(listener.historyPS).map(s->s.toString()).prepend(ID).joining("\t"));
                    pdWriter.append("\n" + StreamEx.of(listener.historyPD).map(s->s.toString()).prepend(ID).joining("\t"));
                    hrWriter.append("\n" + StreamEx.of(listener.historyHR).map(s->s.toString()).prepend(ID).joining("\t"));
                }

                System.out.println(i + " / " + total + " Patient done.");

            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
    }
    
    private static void filterPopulation() throws IOException
    {
        String inputFolder = "C:/Users/Ilya/Desktop/Generated population/Unfiltered";
        String result = "C:/Users/Ilya/Desktop/Generated population/Population joined.txt";
        File input = new File(inputFolder);

        new File(result).delete();
        try (BufferedWriter bw = ApplicationUtils.utfWriter(result))
        {
            for( File inputFile : input.listFiles() )
            {
                List<String> lines = ApplicationUtils.readAsList(inputFile);

                for( int i = 1; i < lines.size(); i = i + 5 )
                    bw.append(lines.get(i) + "\n");
            }
        }
    }
    
    public void test2() throws IOException
    {
        joinPopulation();
    }
    
    private void joinPopulation() throws IOException
    {
        String inputFolder = "Generated Population/Am5";
        String resultFolder = "Generated Population/Am5/Joined";
        File input = getTestFile(inputFolder);

        getTestFile(resultFolder).delete();
        getTestFile(resultFolder).mkdirs();
        Map<String, File> resultFiles = new HashMap<>();
        for (File f: input.listFiles())
        {
            File[] dataFiles = f.listFiles();
            
            for (File dataFile: dataFiles)
            {
                String name = dataFile.getName();
                File resultFile = resultFiles.computeIfAbsent(name, n -> getTestFile(resultFolder+"/"+n));

                try (BufferedWriter bw = ApplicationUtils.utfAppender(resultFile);)
                {
                    List<String> data = ApplicationUtils.readAsList(dataFile);

                    for( String s : data )
                        bw.append(s+"\n");
                }
            }
        }
    }
       

    private void renumeratePopulation() throws IOException
    {
        String inputFolder = "Generated Population/Before";
        String resultFolder = "Generated Population/Renumerated";
        File input = getTestFile(inputFolder);

        int index = 0;
        getTestFile(resultFolder).mkdirs();
        
        String[] fileNames = new String[]{"P1.txt", "P2.txt", "P3.txt", "P4.txt", "P5.txt", "P6.txt", "P7.txt", "P8.txt", "P9.txt", "P10.txt"};
        for( String fileName : fileNames )
        {
            List<String> lines = ApplicationUtils.readAsList(getTestFile(inputFolder+"/"+fileName));
            File result = getTestFile(resultFolder + "/" + fileName);

            try (BufferedWriter bw = ApplicationUtils.utfAppender(result);)
            {

                bw.append(lines.get(0));
                for( int i = 1; i < lines.size(); i++ )
                {
                    String[] components = lines.get(i).split("\t");
                    int ID = Integer.parseInt(components[0]);
                    int newID = ID + index;
                    String newS = StreamEx.of(components).skip(1).prepend(String.valueOf(newID)).joining("\t");
                    bw.append("\n"+newS);

                }

                index += ( lines.size() - 1 );
            }
        }
    }
    
	private static class HistoryListener implements ResultListener 
	{
		Model model;

		public HistoryListener(int[] selected)
		{
			iPS = selected[0];
			iPD = selected[1];
			iHR = selected[2];
			historyPS = new ArrayList<>();
			historyPD = new ArrayList<>();
			historyHR = new ArrayList<>();
		}

		@Override
		public void start(Object model)
		{
			this.model = (Model) model;
		}

		List<Double> historyPS;
		List<Double> historyPD;
		List<Double> historyHR;

		int iPS;
		int iPD;
		int iHR;

		@Override
		public void add(double t, double[] y) throws Exception 
		{
			double[] values = model.getCurrentValues();
			historyPS.add(values[iPS]);
			historyPD.add(values[iPD]);
			historyHR.add(values[iHR]);
		}
	}
   
    
    public static void filterPopulation(File input, File output, Set<String> negativeParameters) throws IOException
    {
        List<String> data = ApplicationUtils.readAsList(input);
        
        String header = data.get(0);
        String[] parameterNames = header.split("\t");       
        
		try (BufferedWriter bw = ApplicationUtils.utfWriter(output)) 
		{
			bw.write(header+"\n");

			for (int i = 1; i < data.size(); i++) 
			{
				String line = data.get(i);
				String[] parameterValues = line.split("\t");

				boolean isValid = true;
				for (int j = 0; j < parameterValues.length; j++) 
				{
					double parameterValue = Double.parseDouble(parameterValues[j]);
					if (parameterValue < 0 && !negativeParameters.contains(parameterNames[j])) 
					{
						isValid = false;
						System.out.println("Patient " + parameterValues[0] + " is invalid because of "
								+ parameterNames[j] + " value: " + parameterValue);
						break;
					}
				}
				if (isValid)
					bw.write(line + "\n");
			}
		}
    }
}
