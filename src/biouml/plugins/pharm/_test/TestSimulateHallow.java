package biouml.plugins.pharm._test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.ode.OdeModel;
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
import ru.biosoft.analysis.Stat;

public class TestSimulateHallow extends AbstractBioUMLTest
{
    public TestSimulateHallow(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestSimulateHallow.class.getName());
        suite.addTest(new TestSimulateHallow("test"));
        return suite;
    }


    private static Diagram getDiagram(String diagramName) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection("data/Collaboration (git)/Cardiovascular system/Complex model/");
        DataElement de = collection.get(diagramName);
        return (Diagram)de;
    }

    private static int limit = 5000;
    private boolean overwrite = true;

    private static final String[] selected = new String[] {"P_ma2"};

    private static int findAlreadyProcessed(String resultInfo) throws Exception
    {
        return ApplicationUtils.readAsList(getTestFile(resultInfo)).size();
    }
    
    public void test() throws Exception
    {
        String source = "Results Hallow Population 13 nov C_at fixed constr";
        
      Treatment t = new Treatment("Complex model Hallow plain", source+"/All.txt", source+"/Prestanse 0.96", true);
      t.start();
      t.join();
////
//      Treatment t2 = new Treatment("Complex model new Enalapril", "Results 15 October/All.txt", "Results 15 October/Enalapril", true);
//      t2.start();
//      t2.join();
//
//      Treatment t3 = new Treatment("Complex model new Bisoprolol", "Results 15 October/All.txt", "Results 15 October/Bisoprolol", false);
//      t3.start();
//      t3.join();

//      Treatment t4 = new Treatment("Complex model new Amlodipine", "Results 15 October/All.txt", "Results 15 October/Amlodipine", false);
//      t4.start();
//      t4.join();
//
//      Treatment t5 = new Treatment("Complex model new EdarbiCLOR", "Results 15 October/All.txt", "Results 15 October/EdarbiCLOR fixed", false);
//      t5.start();
//      t5.join();
////
//        Treatment t6 = new Treatment("Complex model new Prestance", "Population 18 October/All.txt", "Population 18 October/TEST", false);
//        t6.start();
//        t6.join();
//
//      Treatment t7 = new Treatment("Complex model new Thiazide", "Results 15 October/All.txt", "Results 15 October/Thiazide", false);
//      t7.start();
//      t7.join();
    }

    
    private static class Treatment extends Thread
    {
        private String inputFileName;
        private String resultFolder;
        private Diagram d;
        private boolean overwrite;
        
        public Treatment(String diagramName, String inputFileName, String resultFolder, boolean overwrite) throws Exception
        {
            this.overwrite = overwrite;
            this.inputFileName = inputFileName;
            this.resultFolder = resultFolder;
            this.d = getDiagram(diagramName);
        }
        
         @Override
         public void run()
         {
             String outputFileName = resultFolder + "/Treated.txt";
             String statisticsFileName = resultFolder + "/Statistics.txt";
             String historyPmaFileName = resultFolder + "/history Pma.txt";
             String resultInfo = resultFolder + "/INFO.txt";
             
//            TestPopulationComplex.prepare(d);

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
            engine.setCompletionTime(1E9);
            engine.setTimeIncrement(1E8);
            engine.setLogLevel( Level.SEVERE );
            
                
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

                List<Double> pressureDrop = new ArrayList<>();
            
            int counter = 0;
            int total = patientsData.size() - 1;
            for( int i = 1 + alreadyProcessed; i < patientsData.size(); i++ )
            {
                if (counter++ >= limit)
                    break;
                String data = patientsData.get(i);
                double[] patientsValues = StreamEx.of(data.split("\t")).mapToDouble(s -> Double.parseDouble(s)).toArray();

                double[] modelValues = initialValues.clone();
                double ID = Double.NaN;
                for( int j = 0; j < header.length; j++ )
                {

                    if( header[j].contains("time") ) //avoid initial time setting
                        continue;

                    if( header[j].equals("ID") )
                    {
                        ID = patientsValues[j];
                        continue;
                    }

                    if (mapping.containsKey(header[j]))
                    {
                    int index = mapping.get(header[j]);
                    modelValues[index] = patientsValues[j];
                    }
                }

                model.init();
                model.setCurrentValues(modelValues);

                double timeStart = System.currentTimeMillis();
                HistoryListener listener = new HistoryListener(StreamEx.of(selected).mapToInt(s->mapping.get(s)).toArray());
                
                double[] before = StreamEx.of(selected).mapToDouble(s->modelValues[mapping.get(s)]).toArray();
                System.out.println("Patient "+ID+": "+DoubleStreamEx.of(before).joining(" "));
                engine.simulate(model, new ResultListener[] {listener});
                double[] newValues = model.getCurrentValues();
                double[] after = StreamEx.of(selected).mapToDouble(s->newValues[mapping.get(s)]).toArray();
                double[] diff = new double[before.length];
                for (int j=0; j<before.length; j++)
                    diff[j] = before[j] - after[j];
                
                pressureDrop.add(diff[0]);
                
                System.out.println("Patient "+ID+": "+DoubleStreamEx.of(diff).joining(" "));
                
                
                boolean constraintViolated = ((OdeModel)model).isConstraintViolated();
                
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
                    statisticsWriter.append("\n" + DoubleStreamEx.of(selectedOld).append(selectedNew).append(selectedDiff).prepend(ID).joining("\t"));
                }

                try (BufferedWriter resultWriter = ApplicationUtils.utfAppender(getTestFile(outputFileName)))
                {
                    resultWriter.append("\n" + DoubleStreamEx.of(newValues).prepend(ID).joining("\t"));
                }

                try (BufferedWriter pmaWriter = ApplicationUtils.utfAppender(getTestFile(historyPmaFileName)))
                {
                    pmaWriter.append("\n" + StreamEx.of(listener.historyPma).prepend(ID).joining("\t"));
                }

                System.out.println(i + " / " + total + " Patient done.");

            }
            
            System.out.println(Stat.mean(pressureDrop));
            System.out.println(Math.sqrt(Stat.variance(DoubleStreamEx.of(pressureDrop).toArray())));
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
            iPma = selected[0];
            historyPma = new ArrayList<>();
        }
        
        @Override
        public void start(Object model)
        {
            this.model = (Model) model;
        }

        List<Double> historyPma;

        int iPma;

        @Override
        public void add(double t, double[] y) throws Exception
        {
            double[] values = model.getCurrentValues();
            historyPma.add(values[iPma]);
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
