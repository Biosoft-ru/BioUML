package biouml.plugins.pharm._test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;

public class TestSimulateComplex2 extends AbstractBioUMLTest
{
    public TestSimulateComplex2(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestSimulateComplex2.class.getName());
        suite.addTest(new TestSimulateComplex2("test"));
        return suite;
    }


    private Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection("data/Collaboration (git)/Cardiovascular system/Complex model/");
        SessionCache sessionCache = SessionCacheManager.getSessionCache();
        String completeName = DataElementPath.create(collection, "Complex model new Aliskiren").toString();
        sessionCache.removeObject(completeName);
        collection.release("Complex model new Aliskiren");
        DataElement de = collection.get("Complex model new Aliskiren");
        return (Diagram)de;
    }

    public void test() throws Exception
    {        
        Treatment t = new Treatment(getDiagram(), "/Results/All.txt", "/Results/Treated/");
        Treatment t2 = new Treatment(getDiagram(), "/Results/All.txt", "/Results/Treated2/");
        t.start();
        t2.start();
        t.join();
        t2.join();
    }

    public class Treatment extends Thread implements ResultListener
    {
        private SimulationEngine engine;
        private Model model;

        public Treatment(Diagram diagram, String inputFile, String output) throws Exception
        {
            engine = DiagramUtility.getPreferredEngine(diagram);
            engine.setDiagram(diagram);
            model = engine.createModel();

            this.inputFileName = inputFile;
            resultFolder = output;
            outputFileName = resultFolder + "Treated.txt";
            statisticsFileName = resultFolder + "Statistics.txt";
            historyPSFileName = resultFolder + "history PS.txt";
            historyPDFileName = resultFolder + "history PD.txt";
            historyHRFileName = resultFolder + "history HR.txt";
            resultInfo = resultFolder + "INFO.txt";
        }

        private String inputFileName;// = "/Results/All.txt";
        private String resultFolder;// = "/Results/Treated/";
        private String outputFileName;// = resultFolder + "Treated.txt";
        private String statisticsFileName;// = resultFolder + "Statistics.txt";
        private String historyPSFileName;// = resultFolder + "history PS.txt";
        private String historyPDFileName;// = resultFolder + "history PD.txt";
        private String historyHRFileName;// = resultFolder + "history HR.txt";
        private String resultInfo;//= resultFolder + "INFO.txt";

        private String[] selected = new String[] {"Heart\\P_S", "Heart\\P_D", "Heart\\Heart_Rate"};

        private int findAlreadyProcessed() throws Exception
        {
            return ApplicationUtils.readAsList(getTestFile(resultInfo)).size();
        }

        @Override
        public void run()
        {
            try
            {
                getTestFile(resultFolder).mkdirs();
                getTestFile(resultInfo).createNewFile();

                List<String> patientsData = ApplicationUtils.readAsList(getTestFile(inputFileName));

                String[] header = patientsData.get(0).split("\t"); //variable names in file

                Map<String, Integer> mapping = engine.getVarPathIndexMapping(); //mapping for new model

                model.init();
                iPS = mapping.get(selected[0]);
                iPD = mapping.get(selected[1]);
                iHR = mapping.get(selected[2]);
                double[] initialValues = model.getCurrentValues();

                int alreadyProcessed = findAlreadyProcessed();
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

                        int index = mapping.get(header[j]);
                        modelValues[index] = patientsValues[j];
                    }
                    model.setCurrentValues(modelValues);

                    double timeStart = System.currentTimeMillis();

                    engine.simulate(model, new ResultListener[] {this});

                    String err = engine.getSimulator().getProfile().getErrorMessage();
                    if( err != null )
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
                    double[] newValues = model.getCurrentValues();

                    try (BufferedWriter statisticsWriter = ApplicationUtils.utfAppender(getTestFile(statisticsFileName)))
                    {
                        double[] selectedOld = StreamEx.of(selected).mapToDouble(s -> modelValues[mapping.get(s)]).toArray();
                        double[] selectedNew = StreamEx.of(selected).mapToDouble(s -> newValues[mapping.get(s)]).toArray();
                        statisticsWriter.append("\n" + DoubleStreamEx.of(selectedOld).append(selectedNew).prepend(ID).joining("\t"));
                    }

                    try (BufferedWriter resultWriter = ApplicationUtils.utfAppender(getTestFile(outputFileName)))
                    {
                        resultWriter.append("\n" + DoubleStreamEx.of(newValues).prepend(ID).joining("\t"));
                    }

                    try (BufferedWriter psWriter = ApplicationUtils.utfAppender(getTestFile(this.historyPSFileName));
                            BufferedWriter pdWriter = ApplicationUtils.utfAppender(getTestFile(this.historyPDFileName));
                            BufferedWriter hrWriter = ApplicationUtils.utfAppender(getTestFile(this.historyHRFileName)))
                    {
                        psWriter.append("\n" + StreamEx.of(historyPS).prepend(ID).joining("\t"));
                        pdWriter.append("\n" + StreamEx.of(historyPD).prepend(ID).joining("\t"));
                        hrWriter.append("\n" + StreamEx.of(historyHR).prepend(ID).joining("\t"));
                    }

                    System.out.println(i + " / " + total + " Patient done.");

                }
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }

        private void filterPopulation() throws IOException
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

        private void joinPopulation() throws IOException
        {
            String inputFolder = "Generated Population/A 300";
            String resultFolder = "Generated Population/A 300/Joined";
            File input = getTestFile(inputFolder);

            getTestFile(resultFolder).delete();
            getTestFile(resultFolder).mkdirs();
            Map<String, File> resultFiles = new HashMap<>();
            for( File f : input.listFiles() )
            {
                File[] dataFiles = f.listFiles();

                for( File dataFile : dataFiles )
                {
                    String name = dataFile.getName();
                    File resultFile = resultFiles.computeIfAbsent(name, n -> getTestFile(resultFolder + "/" + n));

                    try (BufferedWriter bw = ApplicationUtils.utfAppender(resultFile);)
                    {
                        List<String> data = ApplicationUtils.readAsList(dataFile);

                        for( String s : data )
                            bw.append(s + "\n");
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

            String[] fileNames = new String[] {"P1.txt", "P2.txt", "P3.txt", "P4.txt", "P5.txt", "P6.txt", "P7.txt", "P8.txt", "P9.txt",
                    "P10.txt"};
            for( String fileName : fileNames )
            {
                List<String> lines = ApplicationUtils.readAsList(getTestFile(inputFolder + "/" + fileName));
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
                        bw.append("\n" + newS);

                    }

                    index += ( lines.size() - 1 );
                }
            }
        }

        @Override
        public void start(Object model)
        {
            historyPS = new ArrayList<>();
            historyPD = new ArrayList<>();
            historyHR = new ArrayList<>();
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
}
