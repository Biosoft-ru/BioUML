package biouml.plugins.pharm._test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestPrognosticSystem extends AbstractBioUMLTest
{
    public TestPrognosticSystem(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestPrognosticSystem.class.getName());
        suite.addTest(new TestPrognosticSystem("test"));
        return suite;
    }

    public static String PERINDOPRIL_DRUG = "Perindopril";
    public static String AMLODIPINE_DRUG = "Amlodipine";
    public static String AZILSARTAN_DRUG = "Azilsartan";
    public static String LOSARTAN_DRUG = "Losartan";
    public static String METOPROLOL_DRUG = "Metoprolol";
    public static String PRESTANCE_DRUG = "Prestance";

    public static final String[] AVAILABLE_DRUGS = new String[] {PERINDOPRIL_DRUG, AMLODIPINE_DRUG, AZILSARTAN_DRUG, METOPROLOL_DRUG,
            PRESTANCE_DRUG};


    private static Map<String, String> drugToDiagram = new HashMap<>();

    private static void initDiagrams()
    {
        drugToDiagram.put(PERINDOPRIL_DRUG, "Complex model new Enalapril");
        drugToDiagram.put(AMLODIPINE_DRUG, "Complex model new Amlodipine");
        drugToDiagram.put(AZILSARTAN_DRUG, "Complex model new Azilsartan");
        drugToDiagram.put(LOSARTAN_DRUG, "Complex model new Losartan Simple");
        drugToDiagram.put(METOPROLOL_DRUG, "Complex model new Bisoprolol");
        drugToDiagram.put(PRESTANCE_DRUG, "Complex model new Prestance");
    }

    private String sourcePopulation = "C:/Users/Ilya/git/biouml/test/biouml.plugins.pharm/Results_OV/One.txt";
    private Map<String, Double> patientValues;

    private String resultsFolder = "C:/Users/Ilya/System results/Losartan OV Med/";

    private String[] selectedDrugs = {LOSARTAN_DRUG};

    private String[] observed = {"Heart\\P_S", "Heart\\P_D"};
    private double[] threshold = {30.0, 7.0};
    private double[] reltol = {100, 100}; 
    private double[] paramValues = {160, 90};
    
    private static Map<String, Double> makeMap(String[] keys, double[] values)
    {
        Map<String, Double> result = new HashMap<>();
        for (int i=0; i<keys.length; i++)
            result.put(keys[i], values[i]);
        return result;
    }
    
    public void test() throws Exception
    {
        //1. load and filter population to represent given patient
        new File(resultsFolder).mkdirs();
        String populationPath = resultsFolder + "/Population.txt";
//        
        patientValues = makeMap(observed, paramValues);
        Map<String, Double> thresholds = makeMap(observed, threshold);
        initDiagrams();
        Map<String, Double> relTol = makeMap(observed, reltol);
//        
        preparePopulation(patientValues, relTol, populationPath, sourcePopulation);

        //2. treat filtered population
        for( String drug : selectedDrugs )
        {
            String drugFolder = resultsFolder + "/" + drug;
            String infoPath = drugFolder + "/info.txt";
         
            if( new File(drugFolder).exists() )
            {
                for( File f : new File(drugFolder).listFiles() )
                {
                    f.delete();
                }
            }
            new File(drugFolder).delete();
            new File(drugFolder).mkdirs();
            String diagramName = drugToDiagram.get(drug);
            treatPopulation(populationPath, drugFolder, infoPath, diagramName, drug);          
            collectStatistics(populationPath, drugFolder, infoPath, thresholds, drug);
        }       
    }

    private void collectStatistics(String initialPath, String drugFolder, String infoPath, Map<String, Double> thresholds, String drug)
            throws Exception
    {
//        Set<String> failed = new File(infoPath).exists() ? StreamEx.of(ApplicationUtils.readAsList(new File(infoPath))).toSet()
//                : new HashSet<>();

        String treatedPath = drugFolder+"/Treated.txt";
                
        String initialHeader = ApplicationUtils.utfReader(initialPath).readLine();
        String treatedHeader = ApplicationUtils.utfReader(treatedPath).readLine();

        String[] initialNames = initialHeader.split("\t");
        String[] treatedNames = treatedHeader.split("\t");

        Set<String> observed = thresholds.keySet();
        
        Map<String, Integer> initialObserved = new HashMap<>();
        for( int i = 0; i < initialNames.length; i++ )
        {
            if( observed.contains(initialNames[i]) )
            {
                initialObserved.put(initialNames[i], i);
            }
        }

        Map<String, Integer> treatedObserved = new HashMap<>();
        for( int i = 0; i < treatedNames.length; i++ )
        {
            if( observed.contains(treatedNames[i]) )
            {
                treatedObserved.put(treatedNames[i], i);
            }
        }

        Map<String, String[]> initialData = StreamEx.of(ApplicationUtils.readAsList(new File(initialPath))).skip(1).map(s -> s.split("\t"))
                .toMap(arr -> arr[0], arr -> arr);
        Map<String, String[]> treatedData = StreamEx.of(ApplicationUtils.readAsList(new File(treatedPath))).skip(1).map(s -> s.split("\t"))
                .toMap(arr -> arr[0], arr -> arr);

        int total = initialData.size();
        int treatedNumber = 0;
        int badNumber = 0;
        int failedNumber = 0;
        
        for( Entry<String, String[]> initialEntry : initialData.entrySet() )
        {
            boolean isTreated = true;
            boolean badEffect = false;
            
            String key = initialEntry.getKey();
            if( !treatedData.containsKey(key) )
            {
                failedNumber++;
                continue;
            }
            String[] treated = treatedData.get(key);
            String[] initial = initialEntry.getValue();

            Map<String, Double> initialSelected = StreamEx.of(observed).toMap(s -> s,
                    s -> Double.parseDouble(initial[initialObserved.get(s).intValue()]));
            
            Map<String, Double> treatedSelected = StreamEx.of(observed).toMap(s -> s,
                    s -> Double.parseDouble(treated[treatedObserved.get(s).intValue()]));

            Map<String, Double> dropSelected = StreamEx.of(observed).toMap(s -> s, s -> initialSelected.get(s) - treatedSelected.get(s));

            for( Entry<String, Double> drop : dropSelected.entrySet() )
            {
                if (drop.getValue() < 0 )
                {
                    badEffect = true;
                }
                if( drop.getValue() < thresholds.get(drop.getKey()) )
                {
                    isTreated = false;
                   
                }
                System.out.println(key+"\t"+drop.getValue());
                String paramName = drop.getKey();
                paramName = paramName.substring(paramName.lastIndexOf("\\")+1, paramName.length());
                File paramResult = new File(drugFolder+"/"+paramName);
                double[] treatData = new double[] {initialSelected.get(drop.getKey()), treatedSelected.get(drop.getKey()), drop.getValue()};
                this.writePatient(paramResult, treatData, key);
                
            }
            if( badEffect )
                badNumber++;
            else if( isTreated )
                treatedNumber++;
        }
        
        System.out.println("Treatment by " + drug);
        System.out.println("Total " + total);
        System.out.println("Success rate: " + (double)treatedNumber/total * 100.0+" %" );
        System.out.println("Bad effect rate: " + (double)badNumber/total * 100.0+" %" );
        System.out.println("Simulation failed: " + (double)failedNumber/total * 100.0+" %" );
    }


    private void preparePopulation(Map<String, Double> values, Map<String, Double> relTol, String target, String source)
            throws FileNotFoundException, IOException
    {
        int count = 0;
        new File(target).delete();
        try (BufferedReader br = new BufferedReader(new FileReader(source)); BufferedWriter bw = new BufferedWriter(new FileWriter(target)))
        {   
            String header = br.readLine();

            bw.write(header);
            bw.write("\n");
            
            String[] names = header.split("\t");

            Map<String, Integer> indexToValue = new HashMap<>();
            for( int i = 0; i < names.length; i++ )
            {
                String name = names[i];
                if( values.containsKey(name) )
                {
                    indexToValue.put(name , i);
                }
            }

            String nextLine = br.readLine();

            while( nextLine != null )
            {
                boolean accepted = true;
                

                String[] patientVals = nextLine.split("\t");

                for( Entry<String, Double> entry: values.entrySet())
                {
                    double value = Double.parseDouble(patientVals[indexToValue.get(entry.getKey())]);
                    double reference = entry.getValue();
                    double error = Math.abs(value - reference) / reference;

                    if( error > relTol.get(entry.getKey()) )
                        accepted = false;
                }
                
                if( accepted )
                {
                    bw.write(nextLine);
                    bw.write("\n");
                    count++;
                }
                nextLine = br.readLine();
            }
        }
        System.out.println("Population prepared: "+count);
    }

    private void treatPopulation(String populationPath, String resultPath, String infoPath, String diagramName, String drug)
            throws Exception
    {
        System.out.println("Starting test: "+drug);
        File result = new File(resultPath+"/Treated.txt");
        File info = new File(infoPath);
        Diagram diagram = getDiagram(diagramName);
        removeInitialEquations(diagram);
        removePlots(diagram);
        SimulationEngine engine = DiagramUtility.getPreferredEngine(diagram);
//        engine.setVerbose(false);
        engine.setDiagram(diagram);
        Model model = engine.createModel();
        model.init();

        double[] modelValues = model.getCurrentValues();

        Map<String, Integer> mapping = engine.getVarPathIndexMapping();
        write(result, StreamEx.of(EntryStream.of(mapping).invert().toSortedMap().values()).prepend("ID").joining("\t")+"\n");
        
        try (BufferedReader br = new BufferedReader(new FileReader(populationPath)))
        {
            String header = br.readLine();
            String[] names = header.split("\t");

            String line = br.readLine();

            while( line != null )
            {
                String[] values = line.split("\t");
                double[] doubleVals = StreamEx.of(values).mapToDouble(v -> Double.parseDouble(v)).toArray();

                for( int i = 0; i < names.length; i++ )
                {
                    double paramValue = doubleVals[i];
                    Integer index = mapping.get(names[i]);
                    if( ! names[i].contains("time") && index != null )
                    {
                        modelValues[index] = paramValue;
                    }
                }

                String id = values[0];

                model.setCurrentValues(modelValues);
                engine.setCompletionTime(5E6);
                engine.simulate(model);
               
                String err = engine.getSimulator().getProfile().getErrorMessage();

                if( err != null ) //patient failed
                {
                    System.out.println(id + "\t" + err);
                    write(info, id + "\t" + err + "\n");
                }
                else
                {
                    this.writePatient(result, model.getCurrentState(), id);
                }
                line = br.readLine();
            }
        }

    }

    private void writeHeader(File result, String values) throws IOException
    {
        write(result, StreamEx.of(values).prepend("ID").joining("\t") + "\n");
    }

    private void writePatient(File result, double[] values, String id) throws IOException
    {
        write(result, id +"\t"+DoubleStreamEx.of(values).joining("\t") + "\n");
    }

    private void write(File result, String line) throws IOException
    {
        try (BufferedWriter bw = ApplicationUtils.utfAppender(result))
        {
            bw.append(line);
        }
    }


    private static Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection("data/Collaboration (git)/Cardiovascular system/Complex model/");
        DataElement de = collection.get(name);
        return (Diagram)de;
    }

    public static void removeInitialEquations(Diagram d) throws Exception
    {
        for( Equation eq : d.getRole(EModel.class).getInitialAssignments() )
            d.getType().getSemanticController().remove(eq.getDiagramElement());

        for( SubDiagram subDiagram : Util.getSubDiagrams(d) )
        {
            Diagram diagram = subDiagram.getDiagram();
            for( Equation eq : diagram.getRole(EModel.class).getInitialAssignments() )
                diagram.getType().getSemanticController().remove(eq.getDiagramElement());
        }
    }
    
    public static void removePlots(Diagram d) throws Exception
    {
        for( DiagramElement de: d.recursiveStream().filter(de->Util.isPlot(de)))
            d.getType().getSemanticController().remove(de);

        for( SubDiagram subDiagram : Util.getSubDiagrams(d) )
        {
            Diagram diagram = subDiagram.getDiagram();
            for( DiagramElement de: diagram.recursiveStream().filter(de->Util.isPlot(de)))
                diagram.getType().getSemanticController().remove(de);
        }
    }
}
