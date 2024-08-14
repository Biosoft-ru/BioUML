/* $Id$ */

package biouml.plugins.bindingregions.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.bindingregions.utils.StatUtil.PopulationSize;
import biouml.plugins.bindingregions.utils.TableUtils.FileUtils;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ListUtil;

/**
 * @author yura
 *
 */

//There are to types of FunSite:
//1-st type: it contains DataMatrix[] without DataMatrix; it is for merged sites obtained by several peak callers in the same experiment;
//2-st type: it contains DataMatrix without DataMatrix[]; it is for merged sites obtained by same peak caller in distinct experiments;
//Each type of Funsite is constructed by it's specific constructor.

public class FunSite implements Comparable<FunSite>
{
    // it is copied to SiteUtils
    public static String SITE_LENGTH = "Length";

    // it is copied
    public static String CHROMOSOME = "Chromosome";
    public static String START = "Start";
    public static String END = "End";
    public static String STRAND = "Strand";
    public static String NUMBER_OF_INITIAL_SITES = "Number_of_initial_sites";

    // it is copied
    private String chromosomeName;
    private Interval coordinates;
    private int strand;
    private DataMatrix dataMatrix;
    private DataMatrix[] dataMatrices;

    // it is copied
    public FunSite(String chromosomeName, Interval coordinates, int strand, DataMatrix dataMatrix, DataMatrix[] dataMatrices)
    {
        this.chromosomeName = chromosomeName;
        this.coordinates = coordinates;
        this.strand = strand;
        this.dataMatrix = dataMatrix;
        this.dataMatrices = dataMatrices;
    }

    // it is copied
    // Constructor for 1-st site type.
    public FunSite(String chromosomeName, Interval coordinates, int strand, DataMatrix[] dataMatrices)
    {
        this(chromosomeName, coordinates, strand, null, dataMatrices);
    }

    // it is copied
    // Constructor for 2-nd site type.
    public FunSite(String chromosomeName, Interval coordinates, int strand, DataMatrix dataMatrix)
    {
        this(chromosomeName, coordinates, strand, dataMatrix, null);
    }
    
    // it is copied
    @Override
    public int compareTo(FunSite o)
    {
        return coordinates.compareTo(o.coordinates);
    }

    // it is copied
    public String getChromosomeName()
    {
        return chromosomeName;
    }

    // it is copied
    public int getStrand()
    {
        return strand;
    }

    // it is copied
    public int getStartPosition()
    {
        return coordinates.getFrom();
    }

    // it is copied
    public int getFinishPosition()
    {
        return coordinates.getTo();
    }

    // it is copied
    public int getLength()
    {
        return coordinates.getLength();
    }

    // it is copied
    public DataMatrix getDataMatrix()
    {
        return dataMatrix;
    }

    // it is copied
    public DataMatrix[] getDataMatrices()
    {
        return dataMatrices;
    }

    // it is copied
    public void fromDataMatrixToDataMatrices()
    {
        this.dataMatrices = new DataMatrix[]{dataMatrix};
        this.dataMatrix = null;
    }
    
    //
    // it is copied
    public void calculateAveragesOfMatricesThatHaveSameRowAndColumnNames()
    {
        dataMatrices = DataMatrix.calculateAveragesOfMatricesThatHaveSameRowAndColumnNames(dataMatrices);
    }
    
    public Sequence getSequenceRegion(Sequence fullChromosome, int minimalLengthOfSequenceRegion)
    {
        Interval interval = new Interval(getStartPosition(), getFinishPosition());
        if( interval.getLength() < minimalLengthOfSequenceRegion )
            interval = interval.zoomToLength(minimalLengthOfSequenceRegion).fit(fullChromosome.getInterval());
        return new SequenceRegion(fullChromosome, interval, false, false);
    }
    
    // It is copied
    public Sequence getSequenceRegionWithGivenLength(Sequence fullChromosome, int lengthOfSequenceRegion)
    {
        int center = coordinates.getCenter(), leftPosition = center - lengthOfSequenceRegion / 2;
        if(leftPosition < 1 )
            leftPosition = 1;
        int rightPosition = leftPosition + lengthOfSequenceRegion - 1;
        if( rightPosition >= fullChromosome.getLength() )
        {
            rightPosition = fullChromosome.getLength() - 1;
            leftPosition = rightPosition - lengthOfSequenceRegion + 1;
        }
        return new SequenceRegion(fullChromosome, new Interval(leftPosition, rightPosition), false, false);
    }


    //////////////////////////////////////////////////////////////////////////////

    // TODO: To transform it writeToTtableInformationFromDataMatrices(Funsite[] funSites)
    public static void calculateAndWriteFunSitesIntoTables(DataElementPath pathToFolderWithFolders, String[] foldersNames, String[] distinctTracksNames, int minimalLengthOfSite, int maximalLengthOfSite, DataElementPath pathToOutputFolder, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        // 1. Calculate propertiesNamesAll and indexes (indexes of all starts of propertiesNames in propertiesNamesAll)
        String[] propertiesNamesAll = null;
        int[] indexes = new int[foldersNames.length];
        indexes[0] = 0;
        for( int i = 0; i < foldersNames.length; i++ )
        {
            String[] propertiesNames = getPropertiesNames(pathToFolderWithFolders.getChildPath(foldersNames[i]));
            for( int j = 0; j < propertiesNames.length; j++ )
                propertiesNames[j] += "_" + foldersNames[i];
            propertiesNamesAll = i == 0 ? propertiesNames : (String[])ArrayUtils.addAll(propertiesNamesAll, propertiesNames);
            if( i < foldersNames.length - 1 )
                indexes[i + 1] = indexes[i] + propertiesNames.length;
        }
        
        // 2. Calculate and write FunSites into tables.
        int difference = to - from;
        for( int i = 0; i < distinctTracksNames.length; i++ )
        {
            log.info("i = " + i + " track name = " + distinctTracksNames[i]);
            if( jobControl != null )
                jobControl.setPreparedness(from + i * difference / distinctTracksNames.length);
            Map<String, List<FunSite>> mergedSites = getMergedSites(pathToFolderWithFolders, foldersNames,  distinctTracksNames[i], minimalLengthOfSite, maximalLengthOfSite);
            FunSite[] funSites = transformToArray(mergedSites);

            // temp !!!!
//            log.info("size of merged funsites = i = " + funSites.length);
//            for( FunSite fs : funSites )
//                printFunSite(fs);
            // temp !!!!

            // calculateAveragesOfMatricesThatHaveSameRowAndColumnNames(funSites);
            writeFunSitesIntoTable(propertiesNamesAll, indexes, foldersNames, funSites, pathToOutputFolder, distinctTracksNames[i]);
        }
    }

    // These input FunSites are used for analysis of technical replicas (tracks are created by the distinct peak finders on the same alignment)
    // Each FunSite contains dataMatrices (without dataMatrx).
    public static void writeFunSitesIntoTable(String[] propertiesNamesAll, int[] indexes, String[] foldersNames, FunSite[] funSites, DataElementPath pathToOutputFolder, String tableName) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputFolder.getChildPath(tableName));
        table.getColumnModel().addColumn(CHROMOSOME, String.class);
        table.getColumnModel().addColumn(START, Integer.class);
        table.getColumnModel().addColumn(END, Integer.class);
        table.getColumnModel().addColumn(STRAND, Integer.class);
        table.getColumnModel().addColumn(NUMBER_OF_INITIAL_SITES, Double.class);
        for( String s : propertiesNamesAll )
            table.getColumnModel().addColumn(s, String.class);
        for( int i = 0; i < funSites.length; i ++ )
        {
            Object[] row = new Object[]{funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition(), funSites[i].getStrand(), (double)funSites[i].getDataMatrices().length};
            Object[] propertiesValues = new Object[propertiesNamesAll.length];
            for( int j = 0; j < propertiesValues.length; j++ )
                propertiesValues[j] = Double.NaN;
            DataMatrix[] dataMatrices = funSites[i].getDataMatrices();
            for( DataMatrix dataMatrix : dataMatrices )
            {
                String rowName = dataMatrix.getRowNames()[0];
                int index = ArrayUtils.indexOf(foldersNames, rowName);
                if( index < 0 ) throw new Exception("Some data matrix contains unexpected row name");
                int startPosition = indexes[index];
                double[][] matrix = dataMatrix.getMatrix();
                for( int j = 0; j < matrix[0].length; j++ )
                    propertiesValues[startPosition + j] = matrix[0][j];
            }
            row = ArrayUtils.addAll(row, propertiesValues);
            TableDataCollectionUtils.addRow(table, "S_" + Integer.toString(i), row, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
    
    public static FunSite[] readFunSitesInFileBedLike(DataElementPath pathToFile, String columnNameForChromosome, String columnNameForStartPosition, String columnNameForEndPosition, String[] columNamesForDoubleValues) throws IOException
    {
        String[][] matrix = FileUtils.readTabDelimitedFile(pathToFile);
        if( matrix.length < 2 ) return null;
        FunSite[] result = new FunSite[matrix.length - 1];
        int[] starts = new int[result.length], ends = new int[result.length];
        int indexChromosome = ArrayUtils.indexOf(matrix[0], columnNameForChromosome), indexStart = ArrayUtils.indexOf(matrix[0], columnNameForStartPosition), indexEnd = ArrayUtils.indexOf(matrix[0], columnNameForEndPosition);
        if( indexChromosome < 0 || indexStart < 0 || indexEnd < 0 ) return null;
        int[] indicesForDouble = new int[columNamesForDoubleValues.length];
        for( int i = 0; i < columNamesForDoubleValues.length; i++ )
        {
            int index = ArrayUtils.indexOf(matrix[0], columNamesForDoubleValues[i]);
            if( index < 0 ) return null;
            indicesForDouble[i] = index;
        }
        String[] chromosomeNames = (String[])ArrayUtils.remove(MatrixUtils.getColumn(matrix, indexChromosome), 0);
        String[] startsString = (String[])ArrayUtils.remove(MatrixUtils.getColumn(matrix, indexStart), 0);
        String[] endsString = (String[])ArrayUtils.remove(MatrixUtils.getColumn(matrix, indexEnd), 0);
        double[][] doubleValues = new double[result.length][];
        for( int i = 0; i < result.length; i++ )
        {
            chromosomeNames[i] = chromosomeNames[i].substring(3, chromosomeNames[i].length());
            starts[i] = Integer.parseInt(startsString[i]);
            ends[i] = Integer.parseInt(endsString[i]);
            double[] vector = new double[indicesForDouble.length];
            for(  int j = 0; j < vector.length; j++ )
                vector[j] = Double.parseDouble(matrix[i + 1][indicesForDouble[j]]);
            doubleValues[i] = vector;
        }
        for( int i = 0; i < result.length; i++ )
        {
            DataMatrix dataMatrix = new DataMatrix(new String[]{"FS_" + Integer.toString(i)}, columNamesForDoubleValues, new double[][]{doubleValues[i]});
            result[i] = new FunSite(chromosomeNames[i], new Interval(starts[i]), ends[i], dataMatrix);
        }
        return result;
    }
    
    public static FunSite[] readFunSitesInTable(DataElementPath pathToTable)
    {
        TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        String[] chromosomeNames = TableUtils.readGivenColumnInStringTable(table, CHROMOSOME);
        int[] starts = TableUtils.readGivenColumnInIntegerTable(table, START);
        int[] ends = TableUtils.readGivenColumnInIntegerTable(table, END);
        int[] strands = TableUtils.readGivenColumnInIntegerTable(table, STRAND);
        String[] columnNames = TableUtils.getColumnNamesInTable(pathToTable);
        for( String s : new String[]{CHROMOSOME, START, END, STRAND} )
            columnNames = (String[])ArrayUtils.removeElement(columnNames, s);
        Object[] objects = TableUtils.readDataSubMatrix(table, columnNames);
        String[] rowNames = (String[])objects[0];
        double[][] doubleData = (double[][])objects[1];
        FunSite[] funSites = new FunSite[rowNames.length];
        for( int i = 0; i < rowNames.length; i++ )
            funSites[i] = new FunSite(chromosomeNames[i], new Interval(starts[i], ends[i]), strands[i], new DataMatrix(new String[]{rowNames[i]}, columnNames, new double[][]{doubleData[i]}));
        return funSites;
    }
    
    
    // it is coped and modified!!!
    public static FunSite[] removeOrphans(FunSite[] funSites)
    {
        List<FunSite> result = new ArrayList<>();
        DataMatrix dataMatrix = funSites[0].getDataMatrix();
        if( dataMatrix != null )
        {
            int index = ArrayUtils.indexOf(dataMatrix.getColumnNames(), NUMBER_OF_INITIAL_SITES);
            if( index < 0 ) return funSites;
            for( FunSite fs :  funSites )
                if( fs.getDataMatrix().getMatrix()[0][index] > 1.001 )
                    result.add(fs);
        }
        else
        {
            for( FunSite fs :  funSites )
                if( fs.getDataMatrices().length > 1 )
                    result.add(fs);
        }
        return result.toArray(new FunSite[0]);
    }

//    // it is copied
//    public static void calculateAveragesOfMatricesThatHaveSameRowAndColumnNames(FunSite[] funSites)
//    {
//        for( FunSite fs : funSites )
//            fs.calculateAveragesOfMatricesThatHaveSameRowAndColumnNames();
//    }
    
    public static DataMatrix getTracksPropertiesMeans(DataElementPath pathToFolderWithFolders, String[] foldersNames, String[] distinctTracksNames)
    {
        DataMatrix result = null;
        for( int i = 0; i < foldersNames.length; i++ )
        {
            log.info("treatment of foldersNames = " + foldersNames[i]);
            DataElementPath pathToFolderWithTracks = pathToFolderWithFolders.getChildPath(foldersNames[i]);
            DataMatrix dataMatrix = getTracksPropertiesMeans(pathToFolderWithTracks, distinctTracksNames);
            String[] columnNames = dataMatrix.getColumnNames();
            for( int j = 0; j < columnNames.length; j++ )
                columnNames[j] += "_" + foldersNames[i];
            if( result == null )
                result = dataMatrix;
            else
                result.addAnotherDataMatrixColumnWise(dataMatrix);
        }
        return result;
    }
    
    
    public static double[] getPopulationSizesAndSigmas(int[] freq, int numberOfFunSites, int maxNumberOfIterations)
    {
        double[] result = PopulationSize.getPopulationSizeAndSigmaChao(freq[0], freq[1], numberOfFunSites);
        result = ArrayUtils.addAll(result, PopulationSize.getPopulationSizeAndSigmaLanumteangBohning(freq[0], freq[1], freq[2], numberOfFunSites));
        result = ArrayUtils.addAll(result, PopulationSize.getPopulationSizeAndSigmaZelterman(freq[0], freq[1], numberOfFunSites));
        return ArrayUtils.addAll(result, PopulationSize.getPopulationSizeAndSigmaMaximumLikelihood(freq, result[0], maxNumberOfIterations, 1.0e-5));
    }

    public static Map<Integer, double[][]> getNumberOfOverlapsAndMatrices(String trackName, FunSite[] funSites)
    {
        Map<Integer, List<double[]>> preResult = new HashMap<>();
        for( FunSite funSite : funSites )
        {
            DataMatrix dataMatrix = funSite.getDataMatrix();
            String[] trackNames = dataMatrix.getRowNames();
            int i = ArrayUtils.indexOf(trackNames, trackName);
            if( i < 0 ) continue;
            double[][] matrix = dataMatrix.getMatrix();
            int numberOfOverlaps = trackNames.length - 1;
            List<double[]> list = preResult.get(numberOfOverlaps);
            if( list == null )
                list = new ArrayList<>();
            list.add(matrix[i]);
            preResult.put(numberOfOverlaps, list);
        }
        Map<Integer, double[][]> result = new HashMap<>();
        for( Entry<Integer, List<double[]>>  entry : preResult.entrySet() )
        {
            List<double[]> list = entry.getValue();
            result.put(entry.getKey(), list.toArray(new double[list.size()][]));
        }
        return result;
    }
    
    public static DataMatrix getMeansAndSigmasOfProperties(String trackName, FunSite[] funSites)
    {
        Map<Integer, double[][]> numberOfOverlapsAndMatrices = getNumberOfOverlapsAndMatrices(trackName, funSites);
        
        // 1. Define matrixOutput and rowNames.
        DataMatrix meansAndSigmasDataMatrix = null;
        double[][] matrixOutput = new double[numberOfOverlapsAndMatrices.size()][];
        String[] rowNames = new String[numberOfOverlapsAndMatrices.size()];
        int index = 0;
        for( Entry<Integer, double[][]> entry : numberOfOverlapsAndMatrices.entrySet() )
        {
            double[][] matrix = entry.getValue();
            DataMatrix dm = new DataMatrix(null, null, matrix);
            meansAndSigmasDataMatrix = dm.getMeansAndSigmas();
            double[][] meansAndSigmasValues = meansAndSigmasDataMatrix.getMatrix();
            int numberOfOverlaps = entry.getKey();
            rowNames[index] = trackName + "_number_of_overlaps_" + Integer.toString(numberOfOverlaps);
            double[] row = new double[]{matrix.length, numberOfOverlaps}, rowTemp = new double[2 * matrix[0].length];
            for( int i = 0; i < matrix[0].length; i++ )
            {
                rowTemp[2 * i] = meansAndSigmasValues[0][i];
                rowTemp[2 * i + 1] = meansAndSigmasValues[1][i];
            }
            matrixOutput[index++] = ArrayUtils.addAll(row, rowTemp);
        }
        
        // 2. Define columnNames.
        String[] names = funSites[0].getDataMatrix().getColumnNames(), columnNamesTemp = new String[2 * names.length], meanAndSigmaNames = meansAndSigmasDataMatrix.getRowNames();
        for( int i = 0; i < names.length; i++ )
        {
            columnNamesTemp[2 * i] = names[i] + "_" + meanAndSigmaNames[0];
            columnNamesTemp[2 * i + 1] = names[i] + "_" + meanAndSigmaNames[1];
        }
        return new DataMatrix(rowNames, (String[])ArrayUtils.addAll(new String[]{"size", "number of overlaps"}, columnNamesTemp), matrixOutput);
    }
    
    // it is copied
    public static FunSite[] transformToArray(Map<String, List<FunSite>> sites)
    {
        int n = getNumberOfSites(sites), index = 0;
        FunSite[] result = new FunSite[n];
        for( Entry<String, List<FunSite>> entry : sites.entrySet() )
            for( FunSite funSite : entry.getValue() )
                result[index++] = funSite;
        return result;
    }
    
    // it is copied
    public static Map<String, List<FunSite>> transformToMap(FunSite[] funSites)
    {
        Map<String, List<FunSite>> result = new HashMap<>();
        for( FunSite funSite : funSites )
            result.computeIfAbsent(funSite.getChromosomeName(), key -> new ArrayList<>()).add(funSite);
        return result;
    }

    // to test it: I changed it!!!
    public static Map<String, List<FunSite>> removeUnusualChromosomes(DataElementPath pathToSequences, Map<String, List<FunSite>> funSites)
    {
        Map<String, List<FunSite>> result = new HashMap<>();
        String[] chromosomeNamesAvailable = EnsemblUtils.getStandardSequencesNames(pathToSequences);
        for( Entry<String, List<FunSite>> entry : funSites.entrySet() )
        {
            String chromosomeName = entry.getKey();
            if( ArrayUtils.contains(chromosomeNamesAvailable, chromosomeName) )
                result.put(chromosomeName, entry.getValue());
        }
        return result;
    }
    
    public static Sequence[] getLinearSequences(Map<String, List<FunSite>> chromosomeNameAndFunSites, DataElementPath pathToSequences, int minimalLengthOfSequenceRegion)
    {
        List<Sequence> result = new ArrayList<>();
        for( Entry<String, List<FunSite>> entry : chromosomeNameAndFunSites.entrySet() )
        {
            Sequence fullChromosome = pathToSequences.getChildPath(entry.getKey()).getDataElement(AnnotatedSequence.class).getSequence();
            for( FunSite funSite : entry.getValue() )
                result.add(new LinearSequence(funSite.getSequenceRegion(fullChromosome, minimalLengthOfSequenceRegion)));
        }
        return result.toArray(new Sequence[result.size()]);
    }

    // It is copied
    public static Sequence[] getLinearSequencesWithGivenLength(Map<String, List<FunSite>> chromosomeNameAndFunSites, DataElementPath pathToSequences, int lengthOfSequenceRegion)
    {
        List<Sequence> result = new ArrayList<>();
        for( Entry<String, List<FunSite>> entry : chromosomeNameAndFunSites.entrySet() )
        {
            Sequence fullChromosome = pathToSequences.getChildPath(entry.getKey()).getDataElement(AnnotatedSequence.class).getSequence();
            for( FunSite funSite : entry.getValue() )
                result.add(new LinearSequence(funSite.getSequenceRegionWithGivenLength(fullChromosome, lengthOfSequenceRegion)));
        }
        return result.toArray(new Sequence[result.size()]);
    }

    public static Sequence[] getRandomLinearSequences(Map<String, List<FunSite>> chromosomeNameAndFunSites, DataElementPath pathToSequences, int minimalLengthOfSequenceRegion, int seed)
    {
        List<Sequence> result = new ArrayList<>();
        Random randomNumberGenerator = new Random(seed);
        for( Entry<String, List<FunSite>> entry : chromosomeNameAndFunSites.entrySet() )
        {
            Sequence fullChromosome = pathToSequences.getChildPath(entry.getKey()).getDataElement(AnnotatedSequence.class).getSequence();
            int chromosomeLength = fullChromosome.getLength();
            for( FunSite funSite : entry.getValue() )
            {
                int length = funSite.getLength(), boundary = chromosomeLength - Math.max(minimalLengthOfSequenceRegion, length);
                int start = randomNumberGenerator.nextInt(boundary);
                FunSite randomSite = new FunSite(funSite.getChromosomeName(), new Interval(start, start + length - 1), 0, null, null);
                result.add(new LinearSequence(randomSite.getSequenceRegion(fullChromosome, minimalLengthOfSequenceRegion)));
            }
        }
        return result.toArray(new Sequence[result.size()]);
    }

    public static Sequence[] getRandomLinearSequencesWithGivenLength(Map<String, List<FunSite>> chromosomeNameAndFunSites, DataElementPath pathToSequences, int lengthOfSequenceRegion, int seed)
    {
        List<Sequence> result = new ArrayList<>();
        Random randomNumberGenerator = new Random(seed);
        for( Entry<String, List<FunSite>> entry : chromosomeNameAndFunSites.entrySet() )
        {
            Sequence fullChromosome = pathToSequences.getChildPath(entry.getKey()).getDataElement(AnnotatedSequence.class).getSequence();
            int chromosomeLength = fullChromosome.getLength();
            for( FunSite funSite : entry.getValue() )
            {
                int length = funSite.getLength(), boundary = chromosomeLength - Math.max(lengthOfSequenceRegion, length);
                int start = randomNumberGenerator.nextInt(boundary);
                FunSite randomSite = new FunSite(funSite.getChromosomeName(), new Interval(start, start + length - 1), 0, null, null);
                result.add(new LinearSequence(randomSite.getSequenceRegionWithGivenLength(fullChromosome, lengthOfSequenceRegion)));
            }
        }
        return result.toArray(new Sequence[result.size()]);
    }
    
    // it is copied
    public static int getNumberOfSites(Map<String, List<FunSite>> sites)
    {
        int n = 0;
        for( Entry<String, List<FunSite>> entry : sites.entrySet() )
            n += entry.getValue().size();
        return n;
    }

    // it is copied
    // Create FunSites of 2-nd type.
    public static Map<String, List<FunSite>> getMergedSites(DataElementPath pathToFolderWithTracks, String[] tracksNames, int minimalLengthOfSite, int maximalLengthOfSite)
    {
        Map<String, List<FunSite>> allSites = new HashMap<>();
        for( int i = 0; i < tracksNames.length; i++ )
        {
            Track track = pathToFolderWithTracks.getChildPath(tracksNames[i]).getDataElement(Track.class);
            Map<String, List<FunSite>> sites = readSitesWithPropertiesInTrack(track, minimalLengthOfSite, maximalLengthOfSite, track.getName());
            if( sites != null )
                allSites = getUnion(allSites, sites);
        }
        ListUtil.sortAll(allSites);
        return getMergedSites(allSites);
    }
    
    // it is copied
    // Create FunSites of 1-st type.
    public static Map<String, List<FunSite>> getMergedSites(DataElementPath pathToFolderWithFolders, String[] foldersNames, String trackName, int minimalLengthOfSite, int maximalLengthOfSite)
    {
        // 1. Read FunSites in tracks; Each Funsite is site of 2-nd type (i.e. contains dataMatrix).
        Map<String, List<FunSite>> allSites = new HashMap<>();
        for( int i = 0; i < foldersNames.length; i++ )
        {
            DataElementPath pathToTrack = pathToFolderWithFolders.getChildPath(foldersNames[i]).getChildPath(trackName);
            if( ! pathToTrack.exists() ) continue;
            Map<String, List<FunSite>> sites = readSitesWithPropertiesInTrack(pathToTrack.getDataElement(Track.class), minimalLengthOfSite, maximalLengthOfSite, foldersNames[i]);
            if( sites == null ) continue;
            allSites = getUnion(allSites, sites);
        }
        
        // 2. Transform FunSites: from dataMatrix to dataMatrices. Calculate merged FunSites (with dataMatrices).
        ListUtil.sortAll(allSites);
        fromDataMatrixToDataMatrices(allSites);
        return getMergedSites(allSites);
    }

    // it is copied
    public static void fromDataMatrixToDataMatrices(Map<String, List<FunSite>> funsites)
    {
        for( Entry<String, List<FunSite>> entry : funsites.entrySet() )
            for( FunSite funSite : entry.getValue() )
                funSite.fromDataMatrixToDataMatrices();
    }
    
    public static void printFunSite(FunSite s)
    {
        DataMatrix dm = s.getDataMatrix();
        if( dm != null )
        {
            double[][] mat = dm.getMatrix();
            String ss = "";
            for( String a : dm.getRowNames() )
                ss += a + "_";
            log.info("chr = " + s.getChromosomeName() + " start = " + s.getStartPosition() + " finish = " + s.getFinishPosition() + " names = " + ss + " dim = " + mat.length + " x " + mat[0].length);
        }
        else
        {
            DataMatrix[] dms = s.getDataMatrices();
            log.info("chr = " + s.getChromosomeName() + " start = " + s.getStartPosition() + " finish = " + s.getFinishPosition());
            for( DataMatrix m : dms )
            {
                String ss = "";
                for( String a : m.getColumnNames() )
                    ss += a + " ";
                log.info(" rowName = " + m.getRowNames()[0] + " columnNames = " + ss);
            }
        }
    }
    
    // it is copied
    public static Map<String, List<FunSite>> getMergedSites(Map<String, List<FunSite>> allSites)
    {
        Map<String, List<FunSite>> result = new HashMap<>();
        for( Entry<String, List<FunSite>> entry : allSites.entrySet() )
            result.put(entry.getKey(), getMergedSites(entry.getValue()));
        return result;
    }

    // it is copied
    // 1. It is the correct modification of method List<CisModule> cisModules = CisModule.getCisModules1()
    // 2. Input funSites must be sorted.
    public static List<FunSite> getMergedSites(List<FunSite> funSites)
    {
        List<FunSite> result = new ArrayList<>();
        for( int i = 0; i < funSites.size() - 1; i++ )
        {
            FunSite fs1 = funSites.get(i);
            int finishPosition = fs1.getFinishPosition();
            List<FunSite> funSitesOverlapped = new ArrayList<>();
            funSitesOverlapped.add(fs1);
            for( int ii = i + 1; ii < funSites.size(); ii++ )
            {
                FunSite fs2 = funSites.get(ii);
                if( finishPosition < fs2.getStartPosition() )
                    break;
                else
                {
                    finishPosition = Math.max(finishPosition, fs2.getFinishPosition());
                    funSitesOverlapped.add(fs2);
                }
            }
            if( funSitesOverlapped.size() > 0 )
            {
                result.add(getfunSiteMerged(funSitesOverlapped));
                i += funSitesOverlapped.size() - 1;
            }
        }
        return result;
    }

    // it is copied
    // For biological replicas all sites have the same properties (columnNames are the same) and dataMatrix != null
    // For technical replicas dataMatrices != null
    private static FunSite getfunSiteMerged(List<FunSite> funSites)
    {
        // 1. Calculate chromosomeName, strand, from, to.
        if( funSites.size() == 1 ) return funSites.get(0);
        FunSite funSite = funSites.get(0);
        String chromosomeName = funSite.getChromosomeName();
        int strand = funSite.getStrand(), from = funSite.getStartPosition(), to = funSite.getFinishPosition();
        for( int i = 1; i < funSites.size(); i++ )
        {
            FunSite fs = funSites.get(i);
            from = Math.min(from, fs.getStartPosition());
            to = Math.max(to, fs.getFinishPosition());
        }
        
        // 2a. Calculate merged dataMatrix.
        DataMatrix dataMatrix = funSite.getDataMatrix();
        if( dataMatrix != null )
        {
            for( int i = 1; i < funSites.size(); i++ )
                dataMatrix.addAnotherDataMatrixRowWise(funSites.get(i).getDataMatrix());
            return new FunSite(chromosomeName, new Interval(from, to), strand, dataMatrix);
        }
        
        // 2b. Calculate merged dataMatrices.
        List<DataMatrix> list = new ArrayList<>();
        for( FunSite fs : funSites )
            for( DataMatrix dm : fs.getDataMatrices() )
                list.add(dm);
        list.toArray(new DataMatrix[0]);
        return new FunSite(chromosomeName, new Interval(from, to), strand, list.toArray(new DataMatrix[0]));
    }
    
    // it is copied
    // TODO: Attention: The input funSites1 (i.e. its  List<FunSite>) will be changed!
    public static Map<String, List<FunSite>> getUnion(Map<String, List<FunSite>> funSites1, Map<String, List<FunSite>> funSites2)
    {
        Map<String, List<FunSite>> funSites = new HashMap<>();
        for( Entry<String, List<FunSite>> entry : funSites1.entrySet() )
            funSites.put(entry.getKey(), entry.getValue());
        for( Entry<String, List<FunSite>> entry : funSites2.entrySet() )
        {
            String chromosomeName = entry.getKey();
            List<FunSite> sites2 = entry.getValue(), sites = funSites.get(chromosomeName);
            if( sites == null )
                sites = sites2;
            else
                sites.addAll(sites2);
            funSites.put(chromosomeName, sites);
        }
        return funSites;
    }
    
    // It is copied
    public static void getInformationAboutControl(DataElementPath pathToFolderWithFolders, String[] foldersNames, String[] distinctTracksNames, DataElementPath pathToOutputFolder, String tableName)
    {
        String[][] matrix = new String[distinctTracksNames.length][];
        for( int i = 0; i < distinctTracksNames.length; i++ )
        {
            String[] matrixRow = new String[]{null, null};
            for( String folderName  : foldersNames )
            {
                DataElementPath pathToTrack = pathToFolderWithFolders.getChildPath(folderName).getChildPath(distinctTracksNames[i]);
                if( pathToTrack.exists() )
                {
                    String id = TrackInfo.getControlId(pathToTrack.getDataElement(Track.class));
                    log.info("i = " + i + " distinctTracksNames = " + distinctTracksNames[i] + " id = " + id);
                    matrixRow = new String[]{id, id != null && ! id.equals("") ? "Yes" : "No"};
                    break;
                }
            }
            matrix[i] = matrixRow;
        }
        TableUtils.writeStringTable(matrix, distinctTracksNames, new String[]{"control_ID", "is_control_ID_exist"}, pathToOutputFolder.getChildPath(tableName));
    }
    
    public static String[] getPropertiesNames(DataElementPath pathToFolderWithTracks)
    {
        DataCollection<DataElement> tracks = pathToFolderWithTracks.getDataCollection(DataElement.class);
        String[] propertiesNames = null, tracksNames = tracks.getNameList().toArray(new String[0]);
        for( String trackName : tracksNames )
        {
            Track track = pathToFolderWithTracks.getChildPath(trackName).getDataElement(Track.class);
            propertiesNames = SequenceSampleUtils.getAvailablePropertiesNames(track);
            if( propertiesNames != null )
            {
                // TODO: To change these hard codes.
                // It is necessary to check correctly digital properties
                if( ArrayUtils.contains(propertiesNames, "name") )
                    propertiesNames = (String[])ArrayUtils.removeElement(propertiesNames, "name");
                break;
            }
        }
        propertiesNames = (String[])ArrayUtils.add(propertiesNames, propertiesNames.length, SITE_LENGTH);
        return propertiesNames;
    }

    public static DataMatrix getTracksPropertiesMeans(DataElementPath pathToFolderWithTracks, String[] distinctTracksNames)
    {
        // 1. Identification of propertiesNames.
        String[] propertiesNames = getPropertiesNames(pathToFolderWithTracks);
        log.info("propertiesNames.length = " + propertiesNames.length);
        
        // 2. Calculate dataMatrix with properties means.
        double[][] matrix = new double[distinctTracksNames.length][];
        for( int i = 0; i < distinctTracksNames.length; i++ )
        {
            log.info("track name = " + distinctTracksNames[i]);
            DataElementPath pathToTrack = pathToFolderWithTracks.getChildPath(distinctTracksNames[i]);
            if( ! pathToTrack.exists() )
            {
                log.info("track das not exist");
                matrix[i] = MatrixUtils.getConstantVector(propertiesNames.length + 1, Double.NaN);
                continue;
            }
            Track track = pathToTrack.getDataElement(Track.class);
            int trackSize = track.getAllSites().getSize();
            if( trackSize > 0 )
                matrix[i] = ArrayUtils.add(getPropertiesMeans(track, propertiesNames), trackSize);
            else
            {
                double[] vector =  MatrixUtils.getConstantVector(propertiesNames.length + 1, Double.NaN);
                vector[propertiesNames.length] = 0.0;
                matrix[i] = vector;
            }
        }
        propertiesNames = (String[])ArrayUtils.add(propertiesNames, propertiesNames.length, "track_size");
        log.info("O.K.");
        return new DataMatrix(distinctTracksNames, propertiesNames, matrix);
    }

    private static double[] getPropertiesMeans(Track track, String[] propertiesNames)
    {
        DataCollection<Site> dc = track.getAllSites();
        double[][] matrix = new double[dc.getSize()][];
        int index = 0;
        for( Site site : dc )
            matrix[index++] = getProperties(site, propertiesNames);
        return MultivariateSample.getMeanVector(matrix);
        
    }

    // it is copied
    // Read FunSites of 2-nd type
    public static Map<String, List<FunSite>> readSitesWithPropertiesInTrack(Track track, int minimalLengthOfSite, int maximalLengthOfSite, String rowName)
    {
        int halfMinimal = minimalLengthOfSite / 2, halfMaximal = maximalLengthOfSite / 2, x = minimalLengthOfSite - 1, xx = maximalLengthOfSite - 1;
        String[] rowNameAsArray = new String[]{rowName}, propertiesNames = SequenceSampleUtils.getAvailablePropertiesNames(track);
        if( propertiesNames == null ) return null;

        // TODO: To change these hard codes.
        // It is necessary to check correctly digital properties
        if( ArrayUtils.contains(propertiesNames, "name") )
            propertiesNames = (String[])ArrayUtils.removeElement(propertiesNames, "name");
        propertiesNames = (String[])ArrayUtils.add(propertiesNames, propertiesNames.length, SITE_LENGTH);
        Map<String, List<FunSite>> funSites = new HashMap<>();
        for( Site site : track.getAllSites() )
        {
            String chromosomeName = site.getSequence().getName();
            Interval coordinates = site.getInterval();
            if( minimalLengthOfSite > 0 && site.getLength() < minimalLengthOfSite )
            {
                int left = Math.max(1, coordinates.getCenter() - halfMinimal);
                coordinates = new Interval(left, left + x);
            }
            else if( maximalLengthOfSite > 0 && site.getLength() > maximalLengthOfSite )
            {
                int left = Math.max(1, coordinates.getCenter() - halfMaximal);
                coordinates = new Interval(left, left + xx);
            }
            double[] properties = getProperties(site, propertiesNames);
            DataMatrix dataMatrix = new DataMatrix(rowNameAsArray, propertiesNames, new double[][]{properties});
            funSites.computeIfAbsent(chromosomeName, key -> new ArrayList<>()).add(new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix));
        }
        return funSites;
    }
    
    // It is copied to SiteUtils.
    private static double[] getProperties(Site site, String[] propertiesNames)
    {
        double[] properties = new double[propertiesNames.length];
        DynamicPropertySet dps = site.getProperties();
        for( int i = 0; i < propertiesNames.length; i++ )
        {
            if( propertiesNames[i].equals(SITE_LENGTH) )
                properties[i] = site.getLength();
            else
            {
                String string = dps.getValueAsString(propertiesNames[i]);
                properties[i] = string != null ? Double.parseDouble(string) : Double.NaN;
            }
        }
        return properties;
    }
    
    private static Logger log = Logger.getLogger(FunSite.class.getName());
}
