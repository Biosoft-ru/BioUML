package biouml.plugins.gtrd._test;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

public class ImportUniprobeTest extends TestCase
{
    static final String UNIPROBE_DIR = "/mnt/disk2/tmp/All_PWMs";
    static final String DATA_DIRECTORY = "../data/";
    
    //Table from http://thebrain.bwh.harvard.edu/pbms/webworks_pub/browse.php
    static final String UNIPROBE_INFO_FILE = UNIPROBE_DIR + "/info";
    
    static class UniprobeInfo
    {
        String protein;
        String uniprobeID;
        String domain;
        String species;
        String description;
        String publication;
        
        public static UniprobeInfo parse(String str)
        {
            UniprobeInfo info = new UniprobeInfo();
            String[] fields = str.split("\t");
            info.protein = fields[0];
            info.uniprobeID = fields[1];
            info.domain = fields[2];
            info.species = fields[3];
            info.description = fields[4];
            info.publication = fields[5];
            return info;
        }
    }
    
    public List<UniprobeInfo> getUniprobeInfo() throws Exception
    {
        List<UniprobeInfo> result = new ArrayList<>();
        try(BufferedReader reader = ApplicationUtils.asciiReader( UNIPROBE_INFO_FILE ))
        {
            reader.readLine();
            String line;
            while((line = reader.readLine()) != null)
            {
                UniprobeInfo info = UniprobeInfo.parse(line);
                if(info.species.equals("Homo sapiens") || info.species.equals("Mus musculus"))
                    result.add(info);
            }
        }
        return result;
    }
    
    
    public void testImportUniprobe() throws Exception
    {
        CollectionFactory.createRepository(DATA_DIRECTORY);
        DataCollection<?> parent = DataElementPath.create("databases/GTRD/matrices/uniprobe").getDataCollection();
        
        List<UniprobeInfo> infos = getUniprobeInfo();
        for(UniprobeInfo info :infos)
            importUniprobe(parent, info);
    }
    
    private void importUniprobe(DataCollection parent, UniprobeInfo info) throws Exception
    {
        File file = getMatrixFile(info);
        FrequencyMatrix matrix = parseUniprobeMatrix(parent, info.uniprobeID, file);
        parent.put(matrix);
    }
    
    static final Map<String, String> publicationToDir = new HashMap<>();
    
    static {
        publicationToDir.put("Berger et al., Cell 2008", "Cell08");
        publicationToDir.put("Badis et al., Science 2009", "SCI09");
        publicationToDir.put("Wei et al., EMBO J 2010", "EMBO10");
        publicationToDir.put("Berger et al., Nat Biotech 2006", "NBT06");
        publicationToDir.put("Scharer et al., Cancer Res 2009", "CR09");
        
    }
    
    private File getMatrixFile(UniprobeInfo info)
    {
        File dir = new File(UNIPROBE_DIR, publicationToDir.get(info.publication));
        String factorName = info.protein;
        File[] files = dir.listFiles();
        if(files == null)
            throw new IllegalArgumentException("Cannot read directory "+dir);
        for(File file : files)
        {
            String fileName = file.getName();
            if(fileName.contains("_RC") || fileName.contains("_secondary"))
                continue;
            String factor = fileName.substring(0, fileName.lastIndexOf('.'));
            int end = factor.indexOf('_');
            if(end == -1)
                end = factor.length();
            factor = factor.substring(0, end);
            if(factor.equals(factorName))
                return file;
        }
        throw new IllegalArgumentException("File for " + factorName + " not found in " + dir.getName());
    }
    
    private FrequencyMatrix parseUniprobeMatrix(DataCollection<?> parent, String name, File file) throws Exception
    {
        String lineA = null;
        String lineC = null;
        String lineG = null;
        String lineT = null;
        try(BufferedReader reader = ApplicationUtils.asciiReader( file ))
        {
            String line;
            while((line = reader.readLine()) != null)
            {
                if(line.startsWith("A:\t"))
                    lineA = line;
                else if(line.startsWith("C:\t"))
                    lineC = line;
                else if(line.startsWith("G:\t"))
                    lineG = line;
                else if(line.startsWith("T:\t"))
                    lineT = line;
            }
        }
        
        double[] rowA = parseMatrixLine(lineA);
        double[] rowC = parseMatrixLine(lineC);
        double[] rowG = parseMatrixLine(lineG);
        double[] rowT = parseMatrixLine(lineT);
        
        double[][] weights = new double[rowA.length][];
        for(int i = 0; i < rowA.length; i++)
            weights[i] = new double[] {rowA[i], rowC[i], rowG[i], rowT[i]};
        
        FrequencyMatrix matrix = new FrequencyMatrix(parent, name, Nucleotide15LetterAlphabet.getInstance(), null, weights, false);
        return matrix;
    }
    
    private double[] parseMatrixLine(String line)
    {
        String[] fields = line.split("\t");
        double[] result = new double[fields.length-1];
        for(int i = 1; i < fields.length; i++)
            result[i - 1] = Double.parseDouble(fields[i]);
        return result;
    }
}
