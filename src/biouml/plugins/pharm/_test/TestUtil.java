package biouml.plugins.pharm._test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysis.Stat;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.ExProperties;

public class TestUtil
{

    public static Diagram getExampleDiagram(String name) throws Exception
    {
        return getDiagram(DATA_RESOURCES_REPOSITORY, EXAMPLE_DIAGRAMS_COLLECTION, name);
    }

    public static TableDataCollection getExampleTable(String name) throws Exception
    {
        return getTable(DATA_RESOURCES_REPOSITORY, EXAMPLE_TABLES_COLLECTION, name);
    }

    public static Diagram getDiagram(String repositoryPath, String collectionName, String name) throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository("../data");
        CollectionFactory.createRepository(repositoryPath);
        DataCollection collection1 = CollectionFactory.getDataCollection(collectionName);
        DataElement de = collection1.get(name);
        return (Diagram)de;
    }

    public static TableDataCollection getTable(String repositoryPath, String collectionName, String tableName) throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository(repositoryPath);
        DataCollection collection1 = CollectionFactory.getDataCollection(collectionName);
        DataElement de = collection1.get(tableName);
        return (TableDataCollection)de;
    }

    public static DataCollection createRepository(String path) throws Exception
    {
        File f = new File(path, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
        Properties propRepository = new ExProperties(f);
        return CollectionFactory.createCollection(null, propRepository);
    }

    public static final String DATA_RESOURCES_REPOSITORY = "../data_resources";
    public static final String EXAMPLE_DIAGRAMS_COLLECTION = "data/Examples/Population Models/Data/Diagrams";
    public static final String EXAMPLE_TABLES_COLLECTION = "data/Examples/Population Models/Data/Tables";


    public static void calc(String fileName, String drugName) throws IOException
    {
        List<String> list = ApplicationUtils.readAsList(new File(fileName));

        List<Double> ps = new ArrayList<>();
        List<Double> pd = new ArrayList<>();
        for( int i = 1; i < list.size(); i++ )
        {
            String[] vals = list.get(i).split("\t");
            ps.add(Double.parseDouble(vals[1]));
            pd.add(Double.parseDouble(vals[2]));
        }

        double[] psArray = DoubleStreamEx.of(ps).toArray();
        double[] pdArray = DoubleStreamEx.of(pd).toArray();
        System.out.println(drugName);
        System.out.println("PS:\t" + Stat.mean(psArray) + "\t" + Math.sqrt(Stat.covariance(psArray, psArray)));
        System.out.println("PD:\t" + Stat.mean(pdArray) + "\t" + Math.sqrt(Stat.covariance(pdArray, pdArray)));
    }

    public static void main(String ... args) throws Exception
    {


        //        String heart = "C:/Users/Ilya/Desktop/data/Kamanov_heart_ss.txt";
        //        String kidney = "C:/Users/Ilya/Desktop/data/Kamanov_kidney_ss.txt";
        //        String result = "C:/Users/Ilya/Desktop/data/Kamanov_ss.txt";
        //        transformTables(heart, kidney, result);

        //        String heart = "C:/Users/Ilya/Desktop/data/Uvarovskaya_heart_ss.txt";
        //        String kidney = "C:/Users/Ilya/Desktop/data/Uvarovskaya_kidney_ss.txt";
        //        String result = "C:/Users/Ilya/Desktop/data/Uvarovskaya_ss.txt";
        //        transformTables(heart, kidney, result);

        //        String baseFolder = "C:/Users/Ilya/Desktop/Results//";//"C:/Users/Ilya/Desktop/Results/";
        //        generateStatistics("Aliskiren_150", baseFolder+"Results_Sokolov_0501/All.txt", baseFolder+"Results_Sokolov_0501/Aliskiren_150/Statistics.txt");
        //        generateStatistics("Amlodipine_5", baseFolder+"Results_Sokolov_0501/All.txt", baseFolder+"Results_Sokolov_0501/Amlodipine_5/Statistics.txt");
        //        generateStatistics("Losartan_50", baseFolder+"Results_Sokolov_0501/All.txt", baseFolder+"Results_Sokolov_0501/Losartan_50/Statistics.txt");
        //        generateStatistics("Diroton", baseFolder+"Results_Sokolov_0501/All.txt", baseFolder+"Results_Sokolov_0501/Diroton/Statistics.txt");
        //        generateStatistics("Bisoprolol", baseFolder+"Results_Sokolov_0501/All.txt", baseFolder+"Results_Sokolov_0501/Bisoprolol/Statistics.txt");

        //        String folder = "C:/Users/Ilya/Desktop/Final/Nazaruk";
        String folder2 = "C:/Users/Ilya/Desktop/Final/Ovcharenko";
        String folder3 = "C:/Users/Ilya/Desktop/Final/Tkachuk";
        String folder4 = "C:/Users/Ilya/Desktop/Final/Nazaruk";

        //        String folder = "C:/Users/Ilya/Desktop/Best";
        //        String folder = "C:/Results_Abstract/";
        //        String folder = "C:/Users/Ilya/Desktop/Results/Results_UV_2012";

        //        String folder = "C:/Results_UV_2012/";
        //        String folder = "C:/Users/Ilya/Desktop/Final results/Results_Kamanov_2412";
        //        mergeResults(folder,"Placebo", "Treated");
        //        mergeResults(folder,"Aliskiren 3 75", "Treated");
        //        mergeResults(folder,"Aliskiren 75", "Treated");
        //        mergeResults(folder,"Aliskiren 150", "Treated");
        //        mergeResults(folder,"Aliskiren 300", "Treated");
        //                mergeResults(folder,"Amlodipine new", "Treated", "Amlodipine 5mg");
        //                mergeResults(folder,"Enalapril new", "Treated", "Enalapril 20mg");
        //
        //                mergeResults(folder,"Amlodipine new", "Statistics", "Amlodipine 5mg stat");
        //                mergeResults(folder,"Enalapril new", "Statistics", "Enalapril 20mg stat");

        //        mergeResults("Bisoprolol 0.6", "Statistics");
        //        mergeResults("Bisoprolol 0.8", "Statistics");
        //        mergeResults(folder,"Bisoprolol 0.98", "Treated");
        //        mergeResults(folder,"Enalapril", "Treated");
        //        mergeResults(folder,"Losartan", "Treated");
        //        mergeResults(folder,"Losartan 50", "Treated", "Losartan 50");
        //          mergeResults(folder,"Losartan 50", "Treated", "Losartan longer");
        mergeInitial(folder2, "All");
        mergeInitial(folder3, "All");
        mergeInitial(folder4, "All");

    }

    public static void mergeInitial(String folder, String fileName) throws IOException
    {
        String excludeFile = null;//"C:/Users/Ilya/git/biouml/test/biouml.plugins.pharm/Results_UV_2012/Indices.txt";//"C:/Results_Abstract/Exclude.txt";
        //        String[] paths = new String[] {
        //                folder+"/1/"+fileName+".txt",
        //                folder+"/2/"+fileName+".txt",
        //                folder+"/3/"+fileName+".txt",
        //                folder+"/6/"+fileName+".txt",
        //                folder+"/9/"+fileName+".txt",
        //                };
        String[] paths = new String[] {folder + "/1/" + fileName + ".txt", folder + "/2/" + fileName + ".txt",
                folder + "/3/" + fileName + ".txt",
                //              folder+"/4/"+fileName+".txt",
                //              folder+"/5/"+fileName+".txt",
                //                folder + "/6/" + fileName + ".txt",
                //              folder+"/7/"+fileName+".txt",
                //              folder+"/8/"+fileName+".txt",
                //                folder + "/9/" + fileName + ".txt",
                //              folder+"/9/"+fileName+".txt",
                //              folder+"/5/"+fileName+".txt",
                //              folder+"/6/"+fileName+".txt",
                //              folder+"/7/"+fileName+".txt",
        };

        Set<Double> exclusions = excludeFile != null && new File(excludeFile).exists()
                ? StreamEx.of(ApplicationUtils.readAsList(new File(excludeFile))).map(s -> Double.parseDouble(s)).toSet() : new HashSet<>();

        String resultPath = folder + "/" + fileName + ".txt";

        mergeResults(paths, resultPath, exclusions);
    }

    public static void mergeResults(String folder, String drugName, String fileName, String result) throws IOException
    {
        String excludeFile = null;//"C:/Users/Ilya/git/biouml/test/biouml.plugins.pharm/Results_UV_2012/Indices.txt";//folder+"/Exclude.txt";

        Set<Double> exclusions = excludeFile != null && new File(excludeFile).exists()
                ? StreamEx.of(ApplicationUtils.readAsList(new File(excludeFile))).map(s -> Double.parseDouble(s)).toSet() : new HashSet<>();

        String[] paths = new String[] {folder + "/1/" + drugName + "/" + fileName + ".txt",
                folder + "/2/" + drugName + "/" + fileName + ".txt", folder + "/3/" + drugName + "/" + fileName + ".txt",
                folder + "/4/" + drugName + "/" + fileName + ".txt", folder + "/5/" + drugName + "/" + fileName + ".txt",
                //                folder + "/6/" + drugName + "/" + fileName + ".txt",
                //                folder + "/9/" + drugName + "/" + fileName + ".txt",
                //                folder+"/7/"+drugName+"/"+fileName+".txt",
                //                folder+"/9/"+drugName+"/"+fileName+".txt",
                //                folder+"/5/"+drugName+"/"+fileName+".txt",
                //                folder+"/7/"+drugName+"/"+fileName+".txt",
                //                folder+"/8/"+drugName+"/"+fileName+".txt",
                //                folder+"/9/"+drugName+"/"+fileName+".txt"
        };


        //                "C:/Results_Abstract/1/"+drugName+"/"+fileName+".txt",
        //                "C:/Results_Abstract/2/"+drugName+"/"+fileName+".txt",
        //                "C:/Results_Abstract/3/"+drugName+"/"+fileName+".txt",
        //                "C:/Results_Abstract/4/"+drugName+"/"+fileName+".txt"};

        String resultPath = folder + "/" + result + ".txt";

        mergeResults(paths, resultPath, exclusions);
        calc(resultPath, drugName);
    }

    public static void mergeResults(String[] paths, String resultPath, Set<Double> exclusions) throws IOException
    {
        try (BufferedWriter bw = ApplicationUtils.asciiWriter(resultPath))
        {
            boolean headerDone = false;

            for( int i = 0; i < paths.length; i++ )
            {

                try
                {
                    File f = new File(paths[i]);
                    if( !f.exists() )
                        continue;
                    BufferedReader br = ApplicationUtils.asciiReader(paths[i]);
                    String header = br.readLine();
                    if( !headerDone )
                    {
                        bw.write(header + "\n");
                        headerDone = true;
                    }
                    String line = br.readLine();
                    while( line != null )
                    {
                        String[] vals = line.split("\t");

                        String indexString = vals[0];

                        double index = Double.parseDouble(indexString);
                        double newIndex = index;//i * 10000 + index;

                        vals[0] = String.valueOf(newIndex);

                        if( !exclusions.contains(newIndex) )
                        {
                            String newLine = StreamEx.of(vals).joining("\t");
                            bw.write(newLine + "\n");
                        }
                        line = br.readLine();
                    }
                    br.close();

                }
                catch( Exception ex )
                {
                    ex.printStackTrace();
                }

            }
        }
    }


    public static void generateStatistics(String drug, String before, String after)// throws Exception
    {
        try
        {
            File beforeFile = new File(before);
            File afterFile = new File(after);

            List<String> beforeData = ApplicationUtils.readAsList(beforeFile);
            List<String> afterData = ApplicationUtils.readAsList(afterFile);

            double badEffect = 100 - ( ( afterData.size() - 1 ) / (double) ( beforeData.size() - 1 ) * 100 );

            double[] ps = StreamEx.of(afterData).skip(1).mapToDouble(s -> Double.parseDouble(s.split("\t")[1])).toArray();
            double[] pd = StreamEx.of(afterData).skip(1).mapToDouble(s -> Double.parseDouble(s.split("\t")[2])).toArray();
            double[] psDelta = StreamEx.of(afterData).skip(1).mapToDouble(s -> Double.parseDouble(s.split("\t")[4])).toArray();
            double[] pdDelta = StreamEx.of(afterData).skip(1).mapToDouble(s -> Double.parseDouble(s.split("\t")[5])).toArray();

            double poorEffect = 0;
            for( int i = 0; i < ps.length; i++ )
            {
                if( ps[i] - psDelta[i] < 10 || pd[i] - pdDelta[i] < 10 )
                {
                    poorEffect++;
                }
            }

            poorEffect = poorEffect / ( beforeData.size() - 1 ) * 100;

            System.out.println(drug + "\t" + stats(ps) + "\t" + stats(psDelta) + "\t" + stats(pd) + "\t" + stats(pdDelta) + "\t"
                    + poorEffect + "\t" + badEffect);
        }
        catch( Exception ex )
        {

        }
    }

    public static String stats(double[] val)
    {
        return format(Stat.mean(val)) + "±" + format(Math.sqrt(Stat.variance(val)));
    }

    public static String format(double val)
    {
        return String.valueOf(Math.round(val * 100) / 100.0);
    }

    public static void transformTables(String heartFile, String kidneyFile, String resultFile) throws Exception
    {
        List<String> heart = ApplicationUtils.readAsList(new File(heartFile));
        List<String> kidney = ApplicationUtils.readAsList(new File(kidneyFile));

        List<String> names = new ArrayList();
        List<String> vals = new ArrayList();

        for( String s : heart )
        {
            String[] arr = s.split("\t");
            String newKey = "Heart/" + arr[0];
            names.add(newKey);
            vals.add(arr[1]);
        }

        for( String s : kidney )
        {
            String[] arr = s.split("\t");
            String newKey = "Kidney/" + arr[0];
            names.add(newKey);
            vals.add(arr[1]);
        }

        try (BufferedWriter bw = ApplicationUtils.utfWriter(resultFile))
        {
            bw.write(StreamEx.of(names).prepend("ID").joining("\t") + "\n");
            bw.write(StreamEx.of(vals).prepend("1").joining("\t") + "\n");
        }
    }
}
