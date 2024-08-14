package biouml.plugins.pharm._test;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysis.Stat;

public class TestAnalyzePopulation extends AbstractBioUMLTest
{
    public TestAnalyzePopulation(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestAnalyzePopulation.class.getName());
        suite.addTest(new TestAnalyzePopulation("test"));
        return suite;
    }


    private Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection<?> collection = CollectionFactory
                .getDataCollection( "data/Collaboration (git)/Cardiovascular system/Complex model/" );
        DataElement de = collection.get("Complex model new Losartan");
        return (Diagram)de;
    }
//
//    private String initialFile = "C://Users//Ilya//Desktop//POpulation results//Treatment 17 October//All.txt";
//    private String treatedFile = "C://Users//Ilya//Desktop//POpulation results//Treatment 17 October//Enalapril//Treated.txt";;

//    private String resultFile = "";

    private String[] targetNames = {"Heart\\P_S"};

    private String[] factorNames = {"Heart\\A_3", "Heart\\AO_2", "Heart\\Heart_Base", "Heart\\Heart_Stress", "Heart\\VO_2_pre",
            "Heart\\K_L0", "Heart\\K_R0", "Heart\\RO_20", "Heart\\w_VR0", "Kidney\\A_AT1_ea", "Kidney\\n_eta_pt", "Kidney\\R_ea_0",
            "Heart\\Y_ALVL0", "Kidney\\N_rsna", "Kidney\\B_AT1_ea", "Kidney\\N_nephrons"};

    public void test() throws Exception
    {
        String baseFolder = "C://Users//Ilya//Desktop//POpulation results//Treatment 17 October//";//Enalapril//Treated.txt"
        String input = baseFolder+"All.txt";
        test(input, baseFolder+"Enalapril//Treated.txt", baseFolder+"Enalapril//Correlations.txt", "Enalapril");
        test(input, baseFolder+"Losartan//Treated.txt", baseFolder+"Losartan//Correlations.txt", "Losartan");
        test(input, baseFolder+"Bisoprolol//Treated.txt", baseFolder+"Bisoprolol//Correlations.txt", "Bisoprolol");
        test(input, baseFolder+"Amlodipine//Treated.txt", baseFolder+"Amlodipine//Correlations.txt", "Amlodipine");
        test(input, baseFolder+"Thiazide//Treated.txt", baseFolder+"Thiazide//Correlations.txt", "Thiazide");
        test(input, baseFolder+"EdarbiCLOR//Treated.txt", baseFolder+"EdarbiCLOR//Correlations.txt", "EdarbiCLOR");
    }
    
    public void test(String initialPath, String treatedPath, String outputPath, String drug) throws Exception
    {
        Map<String, String[]> initialData = StreamEx.of(ApplicationUtils.readAsList(new File(initialPath))).map(s -> s.split("\t"))
                .toMap(arr -> arr[0], arr -> arr);
        Map<String, String[]> treatedData = StreamEx.of(ApplicationUtils.readAsList(new File(treatedPath))).map(s -> s.split("\t"))
                .toMap(arr -> arr[0], arr -> arr);

        String[] initialParams = initialData.get("ID");
        String[] treatedParams = treatedData.get("ID");

        Set<String> factors = StreamEx.of(initialData.get("ID")).without("ID").toSet();//StreamEx.of(factorNames).toSet();

        Set<String> targets = StreamEx.of(targetNames).toSet();
        
        Map<String, Integer> initialMapping = new HashMap<>();
        Map<String, Integer> treatedMapping = new HashMap<>();

        Map<String, List<Double>> initialCollected = new HashMap<>();
        //        Map<String, List<Double>> treatedCollected = new HashMap<>();

        Map<String, List<Double>> targetCollected = new HashMap<>();


        for( int i = 0; i < initialParams.length; i++ )
            if( factors.contains(initialParams[i]) || targets.contains(initialParams[i]))
                initialMapping.put(initialParams[i], i);

        for( int i = 0; i < treatedParams.length; i++ )
            if( factors.contains(treatedParams[i]) || targets.contains(treatedParams[i]) )
                treatedMapping.put(treatedParams[i], i);


        for( Entry<String, String[]> e : initialData.entrySet() )
        {
            if( e.getKey().equals("ID") )
                continue;

            String[] initial = e.getValue();
            String[] treated = treatedData.get(e.getKey());
            
            if (treated == null)
                continue;

            for( String s : factors )
            {
                initialCollected.computeIfAbsent( s, key -> new ArrayList<>() )
                        .add( Double.parseDouble( initial[initialMapping.get( s )] ) );
                //                treatedCollected.computeIfAbsent(s, key -> new ArrayList<Double>()).add(Double.parseDouble(treated[treatedMapping.get(s)]));
            }

            for( String s : targetNames )
            {
                targetCollected.computeIfAbsent( s, key -> new ArrayList<>() )
                        .add(Double.parseDouble(initial[initialMapping.get(s)]) - Double.parseDouble(treated[treatedMapping.get(s)]));
            }

        }

//        Map<String, Double> correlations = new HashMap<>();
//        new File(resultFile;
        System.out.println(drug+"\n");
        try (BufferedWriter bw = ApplicationUtils.utfWriter(new File(outputPath)))
        {
            for( String target : targetNames )
            {
                for( String factor : factors )
                {
                    double corr = Stat.pearsonCorrelation(DoubleStreamEx.of(initialCollected.get(factor)).toArray(),
                            DoubleStreamEx.of(targetCollected.get(target)).toArray());
                    
                    if (Math.abs(corr) > 0.3 )
                    {
                        bw.write(factor + "\t" + corr+"\n");
                        System.out.println(factor + "\t" + corr);
                    }
//                        System.out.println(factor + "\t" + corr);
//                    correlations.put(factor, corr);
                }
            }
        }
        System.out.println("\n");
    }
}
