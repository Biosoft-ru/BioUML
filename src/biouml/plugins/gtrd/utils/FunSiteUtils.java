/* $Id$ */

package biouml.plugins.gtrd.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.GEMPeak;
import biouml.plugins.gtrd.master.sites.chipseq.MACS2ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.PICSPeak;
import biouml.plugins.gtrd.master.sites.chipseq.SISSRSPeak;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.StatUtils.PopulationSize;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.standard.type.Species;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.bigbed.AutoSql;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.TextUtil;

/**
 * @author yura
 *
 */
public class FunSiteUtils
{
    /************************* CombinedSites : start *****************************/
    // Combined Sites must be merged sites or overlapped sites only!!!.
    public static class CombinedSites
    {
        public static final String SITE_TYPE_MERGED = "Merged sites";
        public static final String SITE_TYPE_OVERLAPPED = "Overlapped sites";
        public static final String COMBINED_FREQUENCY = "Frequency";
        
        private FunSite[] combinedSites;
        private String siteType; // site type must bee SITE_TYPE_MERGED or SITE_TYPE_OVERLAPPED only!!!
        
        public CombinedSites(FunSite[] combinedSites, String siteType)
        {
            this.combinedSites = combinedSites;
            this.siteType = siteType;
        }

        // O.K. It is tested.
//        public CombinedSites(String siteType, DataElementPath[] pathsToDataSets, String[] dataSetNames, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
//        {
//            this(readSitesAndProduceCombinedSites(siteType, pathsToDataSets, dataSetNames, minimalLengthOfSite, maximalLengthOfSite, doFromDataMatrixToDataMatrices), siteType);
//        }
        
//////////////////////////////////////////old
        // O.K. It is tested.
        public CombinedSites(String siteType, DataElementPath[] pathsToDataSets, String[] dataSetNames, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
        {
            this(readSitesAndProduceCombinedSites(siteType, pathsToDataSets, dataSetNames, minimalLengthOfSite, maximalLengthOfSite, doFromDataMatrixToDataMatrices), siteType);
        }


        // O.K. It is tested.
//        public CombinedSites(String siteType, DataElementPath pathToFolderWithFolders, String[] foldersNames, String trackName, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
//        {
//            this(siteType, getPaths(pathToFolderWithFolders, foldersNames, trackName), foldersNames, minimalLengthOfSite, maximalLengthOfSite, doFromDataMatrixToDataMatrices);
//        }

////////////////////////////////////////// old
        // O.K. It is tested.
        public CombinedSites(String siteType, DataElementPath pathToFolderWithFolders, String[] foldersNames, String trackName, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
        {
            this(siteType, getPaths(pathToFolderWithFolders, foldersNames, trackName), foldersNames, minimalLengthOfSite, maximalLengthOfSite, doFromDataMatrixToDataMatrices);
        }



        // O.K. It is tested.
        public CombinedSites(String siteType, DataElementPath pathToFolder, String[] fileNames, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
        {
            this(siteType, getPaths(pathToFolder, fileNames), fileNames, minimalLengthOfSite, maximalLengthOfSite, doFromDataMatrixToDataMatrices);
        }
        
//        public CombinedSites(String siteType, Map<String, List<FunSite>> funSites, boolean doFromDataMatrixToDataMatrices)
//        {
//            this(produceCombinedSites(funSites, siteType, doFromDataMatrixToDataMatrices), siteType);
//        }
        
        public FunSite[] getCombinedSites()
        {
            return combinedSites;
        }
        
        // old;
        //private static FunSite[] readSitesAndProduceCombinedSites(String siteType, DataElementPath[] pathsToDataSets, String[] dataSetNames, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
        // new; For memory optimization in METARA.
        //public static FunSite[] produceCombinedSitesMemoryOptimal(DataElementPath[]pathsToTracks, String siteType, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
        

        
        // Read sites in tracks or BED files and produce combinedSites.
        // old version
        
//        private static FunSite[] readSitesAndProduceCombinedSites(String siteType, DataElementPath[] pathsToDataSets, String[] dataSetNames, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
//        {
//            Map<String, List<FunSite>> allSites = readSitesInTracksOrBedFiles(pathsToDataSets, dataSetNames, minimalLengthOfSite, maximalLengthOfSite);
//            return produceCombinedSites(allSites, siteType, doFromDataMatrixToDataMatrices);
//        }
        
        // new version: memory Optimal	!!!!!!!
//        private static FunSite[] readSitesAndProduceCombinedSites(String siteType, DataElementPath[] pathsToTracks, String[] dataSetNames, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
//        {
//            //Map<String, List<FunSite>> allSites = readSitesInTracksOrBedFiles(pathsToDataSets, dataSetNames, minimalLengthOfSite, maximalLengthOfSite);
//            //return produceCombinedSites(allSites, siteType, doFromDataMatrixToDataMatrices);
//            return produceCombinedSitesMemoryOptimal(pathsToTracks, siteType, minimalLengthOfSite, maximalLengthOfSite, doFromDataMatrixToDataMatrices);
//        }
        
//////////////////////////////// old
        // Read sites in tracks or BED files and produce combinedSites.
        private static FunSite[] readSitesAndProduceCombinedSites(String siteType, DataElementPath[] pathsToDataSets, String[] dataSetNames, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
        {
            Map<String, List<FunSite>> allSites = readSitesInTracksOrBedFiles(pathsToDataSets, dataSetNames, minimalLengthOfSite, maximalLengthOfSite);
            return produceCombinedSites(allSites, siteType, doFromDataMatrixToDataMatrices);
        }
        
        private static Map<String, List<FunSite>> readSitesInTracksOrBedFiles(DataElementPath[] pathsToDataSets, String[] dataSetNames, int minimalLengthOfSite, int maximalLengthOfSite)
        {
            Map<String, List<FunSite>> allSites = new HashMap<>();
            for( int i = 0; i < pathsToDataSets.length; i++ )
            {
                if( ! pathsToDataSets[i].exists() ) continue;
                DataElement de = pathsToDataSets[i].getDataElement();
                Map<String, List<FunSite>> sites;
                if(de instanceof BigBedTrack)
                	sites = BigBedTrackUtils.readSitesInBigBedTrack((BigBedTrack<FunSite>) de,
                			minimalLengthOfSite, maximalLengthOfSite, dataSetNames[i]);
                else
                	sites = de instanceof Track ? readSitesWithPropertiesInTrack((Track)de, minimalLengthOfSite, maximalLengthOfSite, dataSetNames[i])
                                                                       : readSitesInBedFile(pathsToDataSets[i], 0, 1, 2, null, null, dataSetNames[i], minimalLengthOfSite, maximalLengthOfSite);
                if( sites == null ) continue;
                allSites = getUnion(allSites, sites);
            }
            return allSites;
        }


        // For memory optimization in METARA.
        //private static FunSite[] produceCombinedSitesMemoryOptimal(DataElementPath[]pathsToTracks, String siteType, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices, String rowName)
        private static FunSite[] produceCombinedSitesMemoryOptimal(DataElementPath[] pathsToTracks, String siteType, int minimalLengthOfSite, int maximalLengthOfSite, boolean doFromDataMatrixToDataMatrices)
        {
        	List<FunSite> allCombinedSites = new ArrayList<>();
        	String[] distinctChromosomeNames = getDistinctChromosomeNames(pathsToTracks);
        	for( String chromosomeName : distinctChromosomeNames )
        	{
            	List<FunSite> allSitesInChromosome = new ArrayList<>();
            	for( DataElementPath dep : pathsToTracks )
            		// String rowName = dep.getName() Is it correct???
            		allSitesInChromosome.addAll(readSitesWithPropertiesInTrack(chromosomeName, dep.getDataElement(Track.class), minimalLengthOfSite, maximalLengthOfSite, dep.getName()));
                Collections.sort(allSitesInChromosome);
                if( doFromDataMatrixToDataMatrices )
                	for( FunSite funSite : allSitesInChromosome )
                		funSite.fromDataMatrixToDataMatrices();
                allCombinedSites.addAll(produceCombinedSitesInWholeChromosome(allSitesInChromosome, siteType));
        	}
        	FunSite[] result = allCombinedSites.toArray(new FunSite[0]);
        	calculateAverages(result);
        	return result;
        }
        
        private static String[] getDistinctChromosomeNames(DataElementPath[]pathsToTracks)
        {
        	Set<String> set = new HashSet<>();
        	for( DataElementPath dep : pathsToTracks )
        		for( Site site : dep.getDataElement(Track.class).getAllSites() )
        			set.add(site.getSequence().getName());
        	return set.toArray(new String[0]);
        }

//        public static FunSite[] produceCombinedSites(Map<String, List<FunSite>> allSites, String siteType, boolean doFromDataMatrixToDataMatrices)
//        {
//            ListUtil.sortAll(allSites);
//            if( doFromDataMatrixToDataMatrices )
//                fromDataMatrixToDataMatrices(allSites);
//            FunSite[] result = transformToArray(produceCombinedSites(allSites, siteType));
//            calculateAverages(result);
//            return result;
//        }

//////////////////////////// old
        public static FunSite[] produceCombinedSites(Map<String, List<FunSite>> allSites, String siteType, boolean doFromDataMatrixToDataMatrices)
        {
        	if( allSites.isEmpty() )
        		return new FunSite[]{};
        	ListUtil.sortAll(allSites);
            if( doFromDataMatrixToDataMatrices )
                fromDataMatrixToDataMatrices(allSites);
            FunSite[] result = transformToArray(produceCombinedSites(allSites, siteType));
            calculateAverages(result);
            return result;
        }

        
        // Input allSites must be sorted by their start positions.
        private static Map<String, List<FunSite>> produceCombinedSites(Map<String, List<FunSite>> allSites, String siteType)
        {
            Map<String, List<FunSite>> result = new HashMap<>();
            for( Entry<String, List<FunSite>> entry : allSites.entrySet() )
                result.put(entry.getKey(), produceCombinedSitesInWholeChromosome(entry.getValue(), siteType));
            return result;
        }
        
        // 1. It is the extension of the the correct modification of method List<CisModule> cisModules = CisModule.getCisModules1()
        // 2. Input sitesInWholeChromosome must be sorted by their start positions.
        private static List<FunSite> produceCombinedSitesInWholeChromosome(List<FunSite> sitesInWholeChromosome, String siteType)
        {
            if( sitesInWholeChromosome.size() <= 1 ) return sitesInWholeChromosome;
            List<FunSite> result = new ArrayList<>();
            int i = 0;
            for( ; i < sitesInWholeChromosome.size() - 1; i++ )
            {
                FunSite fs1 = sitesInWholeChromosome.get(i);
                int finishPosition = fs1.getFinishPosition();
                //List<FunSite> funSitesOverlapped = Arrays.asList(new FunSite[]{fs1}); // TODO: in this list impossible to add()
                List<FunSite> funSitesOverlapped = new ArrayList<>();
                funSitesOverlapped.add(fs1);
                for( int ii = i + 1; ii < sitesInWholeChromosome.size(); ii++ )
                {
                    FunSite fs2 = sitesInWholeChromosome.get(ii);
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
                    result.addAll(siteType.equals(SITE_TYPE_MERGED) ? mergeSites(funSitesOverlapped) : overlapSites(funSitesOverlapped));
                    i += funSitesOverlapped.size() - 1;
                }
            }
            if( i < sitesInWholeChromosome.size() )
                result.add(sitesInWholeChromosome.get(i - 1));
            return result;
        }

        private static List<FunSite> overlapSites(List<FunSite> funSites)
        {
            if( funSites.size() == 1 ) return funSites;
            List<FunSite> result = new ArrayList<>(), list = new ArrayList<>();
            for( FunSite fs : funSites )
                list.add(fs);
            FunSite funSite = list.get(0);
            String chromosomeName = funSite.getChromosomeName();
            int strand = funSite.getStrand();
            while( ! list.isEmpty() )
            {
                if( list.size() == 1 )
                {
                    result.add(list.get(0));
                    return result;
                }
                List<FunSite> overlap = new ArrayList<>();
                funSite = list.get(0);
                overlap.add(funSite);
                Interval overlapCoordinates = funSite.getCoordinates();
                for( int i = 1; i < list.size(); i++ )
                {
                    FunSite fs = list.get(i);
                    Interval coordinates = fs.getCoordinates();
                    Interval interval = coordinates.intersect(overlapCoordinates);

                    // Current site intersect the overlap : recalculate overlapCoordinates and overlap.
                    if( interval != null )
                    {
                        overlapCoordinates = interval;
                        overlap.add(fs);
                        if( i < list.size() - 1 ) continue;
                        result.add(mergeOverlap(chromosomeName, overlapCoordinates, strand, overlap));
                        list = new ArrayList<>();
                        break;
                    }
 
                    // Current site does not intersect the overlap : remove some funSites and to merge the overlap.
                    for( int j = i - 1; j >= 0; j-- )
                    {
                        int boundary = fs.getStartPosition();
                        if( list.get(j).getFinishPosition() < boundary )
                            list.remove(j);
                    }
                    result.add(mergeOverlap(chromosomeName, overlapCoordinates, strand, overlap));
                    break;
                }
            }
            return result;
        }

        private static FunSite mergeOverlap(String chromosomeName, Interval overlapCoordinates, int strand, List<FunSite> overlap)
        {
        	Sequence sequence = overlap.get(0).getSequence();
            if( overlap.get(0).getDataMatrices() != null )
            {
                DataMatrix[] dms = getAllDataMatrices(overlap), dataMatrices = new DataMatrix[dms.length];
                for( int i = 0; i < dms.length; i++ )
                    dataMatrices[i] = dms[i].getSemiClone();
                return new FunSite(chromosomeName, overlapCoordinates, strand, dataMatrices, sequence);
            }
            DataMatrix[] dms = new DataMatrix[overlap.size()];
            for( int i = 0; i < overlap.size(); i++ )
                dms[i] = overlap.get(i).getDataMatrix();
            return new FunSite(chromosomeName, overlapCoordinates, strand, DataMatrix.concatinateDataMatricesRowWise(dms), sequence);
        }
        
        private static List<FunSite> mergeSites(List<FunSite> funSites)
        {
            // 1. Calculate chromosomeName, strand, from, to.
            if( funSites.size() == 1 ) return funSites;
            FunSite funSite = funSites.get(0);
            Sequence sequence = funSite.getSequence();
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
                return Arrays.asList(new FunSite[]{new FunSite(chromosomeName, new Interval(from, to), strand, dataMatrix, sequence)});
            }
            return Arrays.asList(new FunSite[]{new FunSite(chromosomeName, new Interval(from, to), strand, getAllDataMatrices(funSites), sequence)});
        }
        
        // TODO: May be to move to FunSiteUtils ??? 
        private static DataMatrix[] getAllDataMatrices(List<FunSite> funSites)
        {
            List<DataMatrix> list = new ArrayList<>();
            for( FunSite fs : funSites )
                for( DataMatrix dm : fs.getDataMatrices() )
                    list.add(dm);
            return list.toArray(new DataMatrix[0]);
        }
        
        private static ru.biosoft.access.core.DataElementPath[] getPaths(DataElementPath pathToFolder, String[] fileNames)
        {
            ru.biosoft.access.core.DataElementPath[] paths = new ru.biosoft.access.core.DataElementPath[fileNames.length];
            for( int i = 0; i < fileNames.length; i++ )
                paths[i] = pathToFolder.getChildPath(fileNames[i]);
            return paths;
        }
        
        private static ru.biosoft.access.core.DataElementPath[] getPaths(DataElementPath pathToFolderWithFolders, String[] foldersNames, String trackName)
        {
            ru.biosoft.access.core.DataElementPath[] paths = new ru.biosoft.access.core.DataElementPath[foldersNames.length];
            for( int i = 0; i < foldersNames.length; i++ )
                paths[i] = pathToFolderWithFolders.getChildPath(foldersNames[i]).getChildPath(trackName);
            return paths;
        }
        
        // TODO: Attention: The input funSites1 (i.e. its  List<FunSite>) will be changed!
        private static Map<String, List<FunSite>> getUnion(Map<String, List<FunSite>> funSites1, Map<String, List<FunSite>> funSites2)
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
        
        public static String[] getAvailableSiteTypes()
        {
            return new String[]{SITE_TYPE_MERGED, SITE_TYPE_OVERLAPPED};
        }
        
        private static void calculateAverages(FunSite[] funSites)
        {
            if( funSites[0].getDataMatrices() != null )
                calculateAveragesOfMatricesThatHaveSameRowAndColumnNames(funSites);
            else
                calculateAveragesOfRowsThatHaveSameRowNames(funSites);
        }
        
        private static void calculateAveragesOfMatricesThatHaveSameRowAndColumnNames(FunSite[] funSites)
        {
            if( funSites[0].getDataMatrices() == null ) return;
            for( FunSite fs : funSites )
                fs.calculateAveragesOfMatricesThatHaveSameRowAndColumnNames();
        }
        
        private static void calculateAveragesOfRowsThatHaveSameRowNames(FunSite[] funSites)
        {
            if( funSites[0].getDataMatrix() == null ) return;
            for( FunSite fs : funSites )
                fs.getDataMatrix().calculateAveragesOfRowsThatHaveSameRowNames();
        }
    }
    /*********************** CombinedSites : end  *****************************/
    
    /*********************** TranscriptionStartSites : start ******************/
    public static class TranscriptionStartSites
    {
        private FunSite[] tsss;
        
        public TranscriptionStartSites(FunSite[] tsss)
        {
            this.tsss = tsss;
        }
        
        public TranscriptionStartSites(DataElementPath pathToFileWithSites, DataElementPath pathToFileWithScores, String nameOfColumnWithChromosomeNames, String nameOfColumnWithStrands, String nameOfColumnWithTsss, String[] namesOfColumnsWithScores)
        {
            // 1. Define chromosomeNames, positions, strands and scores/
            DataMatrixString dms = new DataMatrixString(pathToFileWithSites, new String[]{nameOfColumnWithChromosomeNames});
            String[] chromosomeNames = dms.getColumn(0);
            Object[] objects = TableAndFileUtils.readMatrixOrSubmatixInFile(pathToFileWithSites, new String[]{nameOfColumnWithTsss, nameOfColumnWithStrands}, TableAndFileUtils.INT_TYPE);
            int[] positions = MatrixUtils.getColumn((int[][])objects[2], 0);
            int[] strands = MatrixUtils.getColumn((int[][])objects[2], 1);
            DataMatrix dm = new DataMatrix(pathToFileWithScores, namesOfColumnsWithScores);
            
            // 2. Define tsss.
            tsss = new FunSite[dm.getSize()];
            for( int i = 0; i < tsss.length; i++ )
                tsss[i] = new FunSite(chromosomeNames[i], new Interval(positions[i], positions[i]), strands[i], dm.getRow(i));
        }
        
        public FunSite[] getTsss()
        {
            return tsss;
        }
        
        private void identifyNearestTsss(FunSite[] array, FunSite[] arrayAdditional, int strand)
        {
            if( array.length < 2 || arrayAdditional.length < 2 ) return;
            int indexForNearest = 0;
            for( int i = 0; i < array.length; i++ )
            {
                if( array[i].getStrand() != strand ) continue;
                int start = array[i].getStartPosition(), distanceMin = Integer.MAX_VALUE;
                for( int j = indexForNearest; j < arrayAdditional.length; j++ )
                {
                    if( arrayAdditional[j].getStrand() != strand ) continue;
                    int startAdditional = arrayAdditional[j].getStartPosition(), distance = Math.abs(start - startAdditional);
                    if( distance > distanceMin ) break;
                    indexForNearest = j;
                    distanceMin = distance;
                }
                array[i].setObjects(new Object[]{arrayAdditional[indexForNearest]});
            }
        }
        
        public void identifyNearestTsss(FunSite[] tsssAdditional)
        {
            Map<String, List<FunSite>> map = transformToMap(tsss), mapAdditional = transformToMap(tsssAdditional);
            ListUtil.sortAll(map);
            ListUtil.sortAll(mapAdditional);
            for( Entry<String, List<FunSite>> entry : map.entrySet() )
            {
                FunSite[] array = entry.getValue().toArray(new FunSite[0]), arrayAdditional =  mapAdditional.get(entry.getKey()).toArray(new FunSite[0]);
                for( int strand : new int[]{StrandType.STRAND_PLUS, StrandType.STRAND_MINUS} )
                    identifyNearestTsss(array, arrayAdditional, strand);
            }
        }
    }
    /*********************** TranscriptionStartSites : end ******************/
    
    /*********************** QualityControlSites : start ********************/
    public static class QualityControlSites
    {
        // distinctRowNames = foldersNames or trackNames or fileNames.
        // combinedFunSites = mergedFunSites or overlappedFunSites
        public static DataMatrix calculateQualityMetrics(FunSite[] combinedFunSites, String[] distinctRowNames, double fpcmThreshold)
        {
            // 1. Preliminary calculations and calculation of FPCM.
            int[] pivotalFrequencies = calculatePivotalFrequencies(combinedFunSites, distinctRowNames.length);
            double fpcm = PopulationSize.getFpcm(pivotalFrequencies[0], pivotalFrequencies[1], pivotalFrequencies[2]);
            double fpcm2 = PopulationSize.getFpcm2(pivotalFrequencies[1], pivotalFrequencies[2], pivotalFrequencies[3]);
            double[] fncms = new double[distinctRowNames.length];
            String[] columnNames = new String[distinctRowNames.length];
            for( int i = 0; i < distinctRowNames.length; i++ )
                columnNames[i] = "FNCM_" + distinctRowNames[i];
            columnNames = (String[])ArrayUtils.addAll(new String[]{"FPCM"}, columnNames);
            columnNames = (String[])ArrayUtils.addAll(columnNames, new String[]{"FPCM2", "Estimated_number_of_sites"});
            double populationSize = Double.NaN;
                
            // 2. Calculate FNCMs and populationSize.
            if( fpcm < fpcmThreshold )
            {
                double[] populationSizes = PopulationSize.getPopulationSizes(pivotalFrequencies, combinedFunSites.length, 20);
                populationSize = UtilsForArray.doContainNan(populationSizes) ? Double.NaN : PrimitiveOperations.getAverage(populationSizes);
                int[] sitesCounts = getCountsForGivenRowNames(combinedFunSites, distinctRowNames);
                for( int j = 0; j < distinctRowNames.length; j++ )
                    fncms[j] = Double.isNaN(populationSize) ? Double.NaN : Math.min(1.0, (double)sitesCounts[j] / populationSize);
            }
            else
            {
                FunSite[] funSitesWithoutOrphans = removeOrphans(combinedFunSites);
                int[] sitesCounts = getCountsForGivenRowNames(funSitesWithoutOrphans, distinctRowNames);
                Object[] objects = getPopulationSizeChapmanBased(funSitesWithoutOrphans, distinctRowNames);
                populationSize = ((double[])objects[1])[0];
                for( int j = 0; j < distinctRowNames.length; j++ )
                    fncms[j] = Math.min(1.0, (double)sitesCounts[j] / populationSize);
            }
            
            // 3. Output results.
            double[] output = new double[]{fpcm};
            output = ArrayUtils.addAll(output, fncms);
            output = ArrayUtils.addAll(output, new double[]{fpcm2, populationSize});
            return new DataMatrix(new String[]{"values"}, columnNames, new double[][]{output});
        }
        
        // TODO: To remove log.info()
        // Output: Object[] objects; objects[0] = double[] sample; objects[1] = double[] meanAndSigma; 
        private static Object[]  getPopulationSizeChapmanBased(FunSite[] funSites, String[] rowNames)
        {
            int combinationsNumber = rowNames.length * (rowNames.length - 1) / 2;
            double[] populationSizes = new double[combinationsNumber];
            int index = 0;
            for( int j = 0; j < rowNames.length - 1; j++ )
                for( int jj = j + 1; jj < rowNames.length; jj++ )
                {
                    int[] counts = getCountsForTwoGivenRowNames(funSites, rowNames[j], rowNames[jj]);
                    populationSizes[index++] = PopulationSize.getPopulationSizeChapman(counts[0], counts[1], counts[2]);
                    log.info("names = " + rowNames[j] + " " + rowNames[jj] + " population size = " + populationSizes[index - 1]);
                }
            return UnivariateSample.checkForOutlier(populationSizes);
        }
        
        // In particular, if numberOfDistinctRowNames = numberOfPeakFinders then 
        // Output : int[] freq : freq[i] = number of funSites that composed by exactly (i + 1) peak finders, i = 0 ,..., numberOfPeakFinders - 1
        public static int[] calculatePivotalFrequencies(FunSite[] funSites, int numberOfDistinctRowNames)
        {
            int[] freq = new int[numberOfDistinctRowNames + 1];
            for( FunSite fs : funSites )
                freq[fs.getDistinctRowNames().length]++;
            return ArrayUtils.remove(freq, 0);
        }
        
        public static FunSite[] removeOrphans(FunSite[] funSites)
        {
            List<FunSite> result = new ArrayList<>();
            for( FunSite fs : funSites )
                if( ! fs.isOrphan() )
                    result.add(fs);
            return result.toArray(new FunSite[0]);
        }
        
        private static int[] getCountsForGivenRowNames(FunSite[] funSites, String[] givenRowNames)
        {
            int[] counts = new int[givenRowNames.length];
            for( FunSite funSite : funSites )
                for( int i = 0; i < givenRowNames.length; i++ )
                    if( funSite.doContainGivenRowName(givenRowNames[i]) )
                        counts[i]++;
            return counts;
        }
        
        private static int[] getCountsForTwoGivenRowNames(FunSite[] funSites, String givenRowName1, String givenRowName2)
        {
            int count1 = 0, count2 = 0, count12 = 0;
            for( FunSite funSite : funSites )
            {
                if( funSite.doContainGivenRowName(givenRowName1) )
                {
                    count1++;
                    if( funSite.doContainGivenRowName(givenRowName2) )
                    {
                        count2++;
                        count12++;
                    }
                }
                else if( funSite.doContainGivenRowName(givenRowName2) )
                    count2++;
            }
            return new int[]{count1, count2, count12};
        }
        
        // TODO: To refactor.
        // distinctRowNames = foldersNames or trackNames or fileNames.
        public static DataMatrix chiSquaredTestForZeroTruncatedPoisson(FunSite[] funSites, String[] distinctRowNames)
        {
            // 1. Preliminary calculations.
            int maxNumberOfIterations = 20;
            double epsilon =- 1.0e-5;
            int[] pivotalFrequencies = calculatePivotalFrequencies(funSites, distinctRowNames.length);
            double populationSizeInitialApproximation = PopulationSize.getPopulationSizeAndSigmaChao(pivotalFrequencies[0], pivotalFrequencies[1], funSites.length)[0];
            
            // TODO: Parts 2 and 3 are the parts of PopulationSize.getPopulationSizeAndSigmaMaximumLikelihood().
            // 2. Calculation of initial approximation for lambda.
            double sum = 0.0, n = 0.0;
            for( int i = 0; i < pivotalFrequencies.length; i++ )
            {
                sum += (double)(i + 1) * (double)pivotalFrequencies[i];
                n += (double)pivotalFrequencies[i];
            }
            double lambda = - Math.log(1.0 - n / populationSizeInitialApproximation);

            // 3. Newton's method for calculation of lambda. 
            for( int i = 0; i < maxNumberOfIterations; i++ )
            {
                double x = Math.exp(-lambda), xx = 1.0 - x, y = sum / lambda, yy = n / xx, delta = (y - yy) / (- y / lambda + yy * x / xx); 
                if( Math.abs(delta) < epsilon ) break;
                lambda -= delta;
            }
            
            // TODO: To transform it to individual method PearsonChiSquaredTest() 
            // 4. Calculate Pearson's chi-squared test.
            double x = Math.exp(-lambda), statistic = 0.0;
            for( int i = 1; i <= pivotalFrequencies.length; i++ )
            {
                x *= lambda / (double)i;
                double probability = x, y = (double)pivotalFrequencies[i - 1];
                statistic += y * y / probability;
            }
            statistic = statistic / (double)funSites.length - (double)funSites.length;
            double pvalue = 0.0;
            try
            {
                // TODO: Replace Stat.chiDistribution()
                pvalue = 1.0 - Stat.chiDistribution(statistic, (double)(pivotalFrequencies.length - 1));
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            double statisticLg = statistic <= 0.0 ? Double.NaN : Math.log10(statistic);
            
            // 5. Calculate statistic2.
            double xx = Math.exp(-lambda), statistic2 = pivotalFrequencies.length < 3 ? Double.NaN : 0.0;
            if( pivotalFrequencies.length >= 3 )
                for( int i = 1; i <= 3; i++ )
                {
                    xx *= lambda / (double)i;
                    double probabilityEstimated = xx, probabilityObserved = (double)pivotalFrequencies[i - 1] / (double)funSites.length;
                    statistic2 += Math.abs(probabilityEstimated - probabilityObserved);
                }
            double statistic2Lg = pivotalFrequencies.length < 3 ? Double.NaN : Math.log10(statistic2);
            
            // 6. Create and return the resulted DataMatrix.
            String[] columnNames = new String[]{"chi_squared_statistic", "chi_squared_statistic_lg", "p_value", "statistic2", "statistic2Lg"};
            return new DataMatrix("values", columnNames, new double[]{statistic, statisticLg, pvalue, statistic2, statistic2Lg});
        }
    }
    /*********************** QualityControlSites : end ********************/
    
    // For memory optimization in METARA.
    // Read FunSites of 2-nd type.
    public static List<FunSite> readSitesWithPropertiesInTrack(String chromosomeName, Track track, int minimalLengthOfSite, int maximalLengthOfSite, String rowName)
    {
        String[] propertiesNames = SiteUtils.getAvailablePropertiesNames(track);
        if( propertiesNames == null ) return null;

        // TODO: To change these hard codes.
        // It is necessary to check correctly digital properties
        if( ArrayUtils.contains(propertiesNames, "name") )
            propertiesNames = (String[])ArrayUtils.removeElement(propertiesNames, "name");
        propertiesNames = (String[])ArrayUtils.add(propertiesNames, propertiesNames.length, SiteUtils.SITE_LENGTH);
        return readSitesInTrack(track, chromosomeName, minimalLengthOfSite, maximalLengthOfSite, propertiesNames, rowName);
    }

    // For memory optimization in METARA.
    // Read FunSites of 2-nd type.
    public static List<FunSite> readSitesInTrack(Track track, String chromosomeName, int minimalLengthOfSite, int maximalLengthOfSite, String[] propertiesNames, String rowName)
    {
        List<FunSite> result = new ArrayList<>();
        String[] rowNameAsArray = propertiesNames == null ? null : new String[]{rowName};
        for( Site site : track.getSites(chromosomeName, 0, Integer.MAX_VALUE) )
        {
            Interval coordinates = site.getInterval();
            coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
            double[] properties = propertiesNames == null ? null : SiteUtils.getProperties(site, propertiesNames);
            DataMatrix dataMatrix = propertiesNames == null ? null : new DataMatrix(rowNameAsArray, propertiesNames, new double[][]{properties});
            result.add(new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix));
        }
        return result;
    }

    // Read FunSites of 2-nd type.
    public static Map<String, List<FunSite>> readSitesWithPropertiesInTrack(Track track, int minimalLengthOfSite, int maximalLengthOfSite, String rowName)
    {
        String[] propertiesNames = SiteUtils.getAvailablePropertiesNames(track);
        if( propertiesNames == null ) return null;

        // TODO: To change these hard codes.
        // It is necessary to check correctly digital properties
        if( ArrayUtils.contains(propertiesNames, "name") )
            propertiesNames = (String[])ArrayUtils.removeElement(propertiesNames, "name");
        propertiesNames = (String[])ArrayUtils.add(propertiesNames, propertiesNames.length, SiteUtils.SITE_LENGTH);
        return readSitesInTrack(track, minimalLengthOfSite, maximalLengthOfSite, propertiesNames, rowName);
    }
    
    // 10.03.22
    public static Map<String, List<FunSite>> readFunSitesOfSecondTypeInTrack(Track track, int minimalLengthOfSite, int maximalLengthOfSite, String[] propertiesNames, String rowName)
    {
        Map<String, List<FunSite>> result = new HashMap<>();
        String[] rowNameAsArray = new String[]{rowName};
        for( Site site : track.getAllSites() )
        {
            String chromosomeName = site.getSequence().getName();
            Interval coordinates = site.getInterval();
            coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
            double[] properties = SiteUtils.getProperties(site, propertiesNames);
            DataMatrix dataMatrix = propertiesNames == null ? null : new DataMatrix(rowNameAsArray, propertiesNames, new double[][]{properties});
            result.computeIfAbsent(chromosomeName, key -> new ArrayList<>()).add(new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix));
        }
        return result;
    }

    
    // Read FunSites of 2-nd type.
    // It didn't work correctly???.
    // 10.03.22
    public static Map<String, List<FunSite>> readSitesInTrack(Track track, int minimalLengthOfSite, int maximalLengthOfSite, String[] propertiesNames, String rowName)
    {
        Map<String, List<FunSite>> result = new HashMap<>();
        String[] rowNameAsArray = propertiesNames == null ? null : new String[]{rowName};
        for( Site site : track.getAllSites() )
        {
            String chromosomeName = site.getSequence().getName();
            Interval coordinates = site.getInterval();
            coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
            double[] properties = propertiesNames == null ? null : SiteUtils.getProperties(site, propertiesNames);
            DataMatrix dataMatrix = propertiesNames == null ? null : new DataMatrix(rowNameAsArray, propertiesNames, new double[][]{properties});
            result.computeIfAbsent(chromosomeName, key -> new ArrayList<>()).add(new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix));
        }
        return result;
    }
    
    public static Map<String, List<FunSite>> readSitesInTrack(Track track, int minimalLengthOfSite, int maximalLengthOfSite)
    {
    	return readSitesInTrack(track, minimalLengthOfSite, maximalLengthOfSite, null, null);
    }
    
    public static FunSite[] getFunSites(DataElementPath pathToTrack)
    {
        Track track = pathToTrack.getDataElement(Track.class);
        Map<String, List<FunSite>> sites = readSitesInTrack(track, 1, Integer.MAX_VALUE, new String[]{RankAggregation.RA_SCORE}, track.getName());
        return FunSiteUtils.transformToArray(sites);
    }

    
    public static class BigBedTrackUtils
    {
    	// currently reads only gem, macs2, pics and sissrs bigBeds
    	public static Map<String, List<FunSite>> readSitesInChIPseqBigBedTrack(Track track, String rowName, int minimalLengthOfSite, int maximalLengthOfSite) throws IOException
    	{
    		// TODO: add throwable
    		if( !(track instanceof BigBedTrack) )
    			return null;

    		BigBedTrack<? extends ChIPSeqPeak> bigBedTrack = (BigBedTrack<? extends ChIPSeqPeak>) track;
    		Map<String, List<FunSite>> result = new HashMap<>();
    		for( String chromosomeName : bigBedTrack.getChromosomes() )
    		{
    			List<FunSite> funSites = new ArrayList<>();
    			List<? extends ChIPSeqPeak> chrPeaks = bigBedTrack.query( chromosomeName );
    			for( ChIPSeqPeak peak : chrPeaks )
    			{
    				int start = peak.getFrom();
    				int end = peak.getTo();
    				Interval coordinates = new Interval(start, end);
    				if( minimalLengthOfSite > 0 || maximalLengthOfSite > 0 )
    					coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);

    				double[] propertyValues = new double[0];
    				String[] propertyNames = new String[0];
    				if(peak instanceof GEMPeak)
    				{
    					GEMPeak gemPeak = (GEMPeak) peak;
    					propertyValues = new double[5];
    					propertyNames = GEMPeak.FIELDS;
    					propertyValues[0] = gemPeak.getFold();
    					propertyValues[1] = gemPeak.getPMLog10();
    					propertyValues[2] = gemPeak.getNoise();
    					propertyValues[3] = gemPeak.getPPoiss();
    					propertyValues[4] = gemPeak.getQMLog10();

    				}
    				else if(peak instanceof MACS2ChIPSeqPeak)
    				{
    					MACS2ChIPSeqPeak macs2Peak = (MACS2ChIPSeqPeak) peak;
    					propertyValues = new double[4];
    					propertyNames = MACS2ChIPSeqPeak.FIELDS;
    					propertyValues[1] = macs2Peak.getMLog10PValue();
    					propertyValues[2] = macs2Peak.getMLog10QValue();
    					propertyValues[0] = macs2Peak.getFoldEnrichment();
    					propertyValues[3] = macs2Peak.getPileup();
    				}
    				else if(peak instanceof PICSPeak)
    				{
    					PICSPeak picsPeak = (PICSPeak) peak;
    					propertyValues = new double[1];
    					propertyNames = PICSPeak.FIELDS;
    					propertyValues[0] = picsPeak.getPicsScore();
    				}
    				else if(peak instanceof SISSRSPeak)
    				{
    					SISSRSPeak sissrsPeak = (SISSRSPeak) peak;
    					propertyValues = new double[3];
    					propertyNames = SISSRSPeak.FIELDS;
    					propertyValues[0] = sissrsPeak.getPValue();
    					propertyValues[1] = sissrsPeak.getFold();
    					propertyValues[2] = sissrsPeak.getNumTags();
    				}
    				else
    				{
    					//TODO: add default converter
    				}

    				propertyValues = ArrayUtils.addAll(propertyValues, new double[]{(double)coordinates.getLength()});
    				propertyNames = (String[]) ArrayUtils.addAll(propertyNames, new String[] {SiteUtils.SITE_LENGTH});

    				DataMatrix dataMatrix = new DataMatrix(new String[]{rowName}, propertyNames, new double[][]{propertyValues});
    				funSites.add(new FunSite(chromosomeName, coordinates, 0, dataMatrix, peak.getOriginalSequence()));
    			}
    			result.computeIfAbsent(chromosomeName, key -> new ArrayList<>()).addAll(funSites);
    		}
    		return result;
    	}
    	
    	// Read FunSites of 2-nd type from bigBed.
    	public static Map<String, List<FunSite>> readSitesInBigBedTrack(BigBedTrack<FunSite> bbTrack, int minimalLengthOfSite, int maximalLengthOfSite, String rowName)
    	{
    		
    		Map<String, List<FunSite>> result = new HashMap<>();
    		for( String chromosomeName : bbTrack.getChromosomes() )
    		{
    			List<FunSite> sites;
    			try {
    				sites = bbTrack.query( chromosomeName );

    				for( FunSite site : sites )
    				{
    					Interval coordinates = site.getCoordinates();
    					coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
    					site.getDataMatrix().setRowNames(new String[]{rowName});
    					result.computeIfAbsent(chromosomeName, key -> new ArrayList<>()).add(new FunSite(chromosomeName, coordinates, 
    							site.getStrand(), site.getDataMatrix()));
    				}
    			} catch (IOException e) {
    				log.warning("Can't query chromosome name: " + chromosomeName );
    				log.warning(e.getMessage());
    			}
    		}
    		return result;
    		
    	}
    	
        public static void writeSitesToBigBedTrack(FunSite[] funSites, String[] propertyNames,
        		DataElementPath pathToOutputFolder, String trackName, Species species, Map<String, Integer> chromSizes) throws Exception
        {
        	DataElementPath outPath = DataElementPath.create( pathToOutputFolder + "/" + trackName );
        	Properties props = new Properties();
        	DataElementPath sequencesPath = TrackUtils.getPrimarySequencesPath( TrackUtils.getEnsemblPath( species ) );
            props.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, sequencesPath.toString() );
            props.setProperty( BigBedTrack.PROP_CONVERTER_CLASS, BedEntryToFunSite.class.getName() );
            BigBedTrack<FunSite> bbTrack = BigBedTrack.create( outPath, props ); 
                       
            AutoSql autosql = getAutoSql(funSites[0], trackName, trackName, propertyNames);
            
            bbTrack.write(Arrays.asList(funSites), chromSizes, 3 + propertyNames.length, 3, autosql.toString());
            outPath.save( bbTrack );
        }
        
        private static AutoSql getAutoSql(FunSite fs, String trackName, String description, String[] propertyNames)
        {
        	AutoSql autosql = new AutoSql();
            autosql.name = "\"" + trackName + "\"";
            autosql.description = description;
            autosql.columns.add(new AutoSql.Column("string", "chrom", "chrom"));
            autosql.columns.add(new AutoSql.Column("uint", "chromStart", "start"));
            autosql.columns.add(new AutoSql.Column("uint", "chromEnd", "end"));
            // special symbols and - should be removed from property names
            for(int i = 0; i < propertyNames.length; i++)
            {
            	Object value = fs.getObjects()[i];
            	autosql.columns.add(new AutoSql.Column(convertJavaToAutoSqlType(value), propertyNames[i], propertyNames[i]));
            }
            return autosql;
        }
        
        private static String convertJavaToAutoSqlType(Object value)
        {
            if(value instanceof Integer)
                return "int";
            if(value instanceof Double || value instanceof Float)
                return "float";
            if(value instanceof String)
                return "string";
            return "string";
        }
        
    }
    
    private static void fromDataMatrixToDataMatrices(Map<String, List<FunSite>> funsites)
    {
        for( Entry<String, List<FunSite>> entry : funsites.entrySet() )
            for( FunSite funSite : entry.getValue() )
                funSite.fromDataMatrixToDataMatrices();
    }
    
    public static int getNumberOfSites(Map<String, List<FunSite>> sites)
    {
        int n = 0;
        for( Entry<String, List<FunSite>> entry : sites.entrySet() )
            n += entry.getValue().size();
        return n;
    }
    
    public static FunSite[] transformToArray(Map<String, List<FunSite>> sites)
    {
        int n = getNumberOfSites(sites), index = 0;
        FunSite[] result = new FunSite[n];
        for( Entry<String, List<FunSite>> entry : sites.entrySet() )
            for( FunSite funSite : entry.getValue() )
                result[index++] = funSite;
        return result;
    }
    
    public static double[] getRaScores(FunSite[] funSites)
    {
    	//24.03.22
        double[] scores = new double[funSites.length];
        for( int i = 0; i < funSites.length; i++ )
        	scores[i] = funSites[i].getDataMatrix().getColumn(RankAggregation.RA_SCORE)[0];
        return scores;
    }

    public static Map<String, List<FunSite>> transformToMap(FunSite[] funSites)
    {
        Map<String, List<FunSite>> result = new HashMap<>();
        for( FunSite funSite : funSites )
            result.computeIfAbsent(funSite.getChromosomeName(), key -> new ArrayList<>()).add(funSite);
        return result;
    }
    
    public static Sequence[] removeOrphanSequences(FunSite[] funSites, Sequence[] sequences)
    {
        List<Sequence> result = new ArrayList<>();
        for( int i = 0; i < funSites.length; i++ )
            if( ! funSites[i].isOrphan() )
                result.add(sequences[i]);
        return result.toArray(new Sequence[0]);
    }
    
    public static String writeSitesToString(FunSite[] funSites)
    {
        StringBuilder builder = new StringBuilder(); 
        int type = funSites[0].getDataMatrix() == null ? 1 : 2;
        builder.append("size\t").append(String.valueOf(funSites.length)).append("\ttype\t").append(String.valueOf(type));
        for( FunSite fs : funSites )
            builder.append("\n").append(fs.toString());
        return builder.toString();
    }
    
    public static void writeSitesToBedFile(FunSite[] funSites, DataElementPath pathToOutputFolder, String fileName)
    {
        StringBuilder builder = new StringBuilder();
        for( FunSite fs : funSites )
            builder.append("chr").append(fs.getChromosomeName()).append("\t").append(String.valueOf(fs.getStartPosition())).append("\t").append(String.valueOf(fs.getFinishPosition())).append("\n");
        String string = builder.toString();

        TableAndFileUtils.writeStringToFile(string, pathToOutputFolder, fileName, log);
    }
    
    public static void writeSitesToSqlTrack(FunSite[] funSites, String scoreName, double[] scores, DataElementPath pathToOutputFolder, String trackName)
    {
        SqlTrack track = SqlTrack.createTrack(pathToOutputFolder.getChildPath(trackName), null);
        if( scoreName == null )
            for( FunSite fs : funSites )
                track.addSite(fs.toSite(null, 0.0));
        else
            for( int i = 0; i < funSites.length; i++ )
                track.addSite(funSites[i].toSite(scoreName, scores[i]));
        track.finalizeAddition();
        CollectionFactoryUtils.save(track);
    }

    public static FunSite[] readSitesInLines(String[] lines)
    {
        String[] tokens = TextUtil.split(lines[0], '\t');
        int n = Integer.parseInt(tokens[1]), type = Integer.parseInt(tokens[3]), index = 0;
        FunSite[] result = new FunSite[n];
        for( int i = 0; i < n; i++ )
        {
            tokens = TextUtil.split(lines[++index], '\t');
            String chromosomeName = tokens[1];
            Interval coordinates = new Interval(Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4]));
            int strand = Integer.parseInt(tokens[5]);
            tokens = TextUtil.split(lines[++index], '\t');
            int numberOfDataMatrices = Integer.parseInt(tokens[1]);
            DataMatrix[] dms = new DataMatrix[numberOfDataMatrices];
            for( int ii = 0; ii < numberOfDataMatrices; ii++ )
            {
                tokens = TextUtil.split(lines[++index], '\t');
                int rowNumber = Integer.parseInt(tokens[1]), columnNumber = Integer.parseInt(tokens[2]);
                String[] rowNames = new String[rowNumber], columnNames = new String[columnNumber];
                double[][] matrix = new double[rowNumber][columnNumber];
                tokens = TextUtil.split(lines[++index], '\t');
                for( int j = 0; j < columnNumber; j++ )
                    columnNames[j] = tokens[j + 1];
                for( int j = 0; j < rowNumber; j++ )
                {
                    tokens = TextUtil.split(lines[++index], '\t');
                    rowNames[j] = tokens[0];
                    for( int jj = 0; jj < columnNumber; jj++ )
                        matrix[j][jj] = Double.parseDouble(tokens[jj + 1]);
                }
                dms[ii] = new DataMatrix(rowNames, columnNames, matrix);
            }
            result[i] = type == 1 ? new FunSite(chromosomeName, coordinates, strand, dms) : new FunSite(chromosomeName, coordinates, strand, dms[0]);  
        }
        return result;
    }
    
    public static Map<String, List<FunSite>> readSitesInBedFile(DataElementPath pathToFile, int columnIndexForChromosome, int columnIndexForStartPosition, int columnIndexForEndPosition, int[] columIndcesForDoubleValues, String[] columnNamesForDoubleValues, String rowName, int minimalLengthOfSite, int maximalLengthOfSite)
    {
        Map<String, List<FunSite>> result = new HashMap<>();
        String[] names = columnNamesForDoubleValues == null ? new String[]{SiteUtils.SITE_LENGTH} : (String[])ArrayUtils.add(columnNamesForDoubleValues, columnNamesForDoubleValues.length, SiteUtils.SITE_LENGTH);
        String[] lines = TableAndFileUtils.readLinesInFile(pathToFile);
        for( String line : lines )
        {
            String[] tokens = TextUtil.split(line, '\t');
            if( tokens.length < 3 || tokens[columnIndexForChromosome].length() < 3 ) break;
            String chromosomeName = tokens[columnIndexForChromosome].substring(3, tokens[columnIndexForChromosome].length());
            int start = Integer.parseInt(tokens[columnIndexForStartPosition]), end = Integer.parseInt(tokens[columnIndexForEndPosition]);
            Interval coordinates = new Interval(start, end);
            double[] vector = new double[]{(double)coordinates.getLength()};
            if( columIndcesForDoubleValues != null )
            {
                double[] array = new double[columIndcesForDoubleValues.length];
                for( int j = 0; j < columIndcesForDoubleValues.length; j++ )
                    array[j] = Double.parseDouble(tokens[columIndcesForDoubleValues[j]]);
                vector = ArrayUtils.addAll(array, vector);
            }
            if( minimalLengthOfSite > 0 || maximalLengthOfSite > 0 )
                coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
            DataMatrix dataMatrix = new DataMatrix(new String[]{rowName}, names, new double[][]{vector});
            result.computeIfAbsent(chromosomeName, key -> new ArrayList<>()).add(new FunSite(chromosomeName, coordinates, 0, dataMatrix));
        }
        return result;
    }
    
    public static Map<String, List<FunSite>> readSitesInBedFile(DataElementPath pathToFile, int columnIndexForChromosome, int columnIndexForStartPosition, int columnIndexForEndPosition, int[] columIndcesForDoubleValues, String[] columnNamesForDoubleValues, String rowName)
    {
        return readSitesInBedFile(pathToFile, columnIndexForChromosome, columnIndexForStartPosition, columnIndexForEndPosition, columIndcesForDoubleValues, columnNamesForDoubleValues, rowName, 0, 0);
    }

    public static DataMatrix getDataMatrixGeneralizedFromChipSeq(FunSite[] funSites)
    {
        // 1. Calculate rowNames and columnNames for resulted DataMatrix.
    	log.info(" funSites.length = " + funSites.length);
        if( funSites == null || funSites.length == 0 ) return null;
        String[] rowNames = new String[funSites.length];
        for( int i = 0; i < rowNames.length; i++ )
            rowNames[i] = "S_" + Integer.toString(i);
        String[] columnNames = getDistinctColumnNames(funSites);
        int index = ArrayUtils.indexOf(columnNames, "Length");
        if( index >= 0 )
        	columnNames = (String[])ArrayUtils.remove(columnNames, index);
        
        // Remove CombinedSites.COMBINED_FREQUENCY !!!
        //columnNames = (String[]) ArrayUtils.add(columnNames, CombinedSites.COMBINED_FREQUENCY);
        
        // 2. Calculate matrix[][].
        double[][] matrix = new double[funSites.length][];
        for( int i = 0; i < funSites.length; i++ )
        {
        	double[] row = UtilsForArray.getConstantArray(columnNames.length, Double.NaN);
        	DataMatrix[] dataMatrices = funSites[i].getDataMatrices();
        	log.info(" i = " + i + " percentage = " + 100.0 * (double)i / (double)funSites.length + " number of data matrices = " + dataMatrices.length);
        	for( DataMatrix dataMatrix : dataMatrices )
        	{
        		String[] columnNamesTemporary = dataMatrix.getColumnNames();
        		double[] values = dataMatrix.getMatrix()[0];
                for( int j = 0; j < columnNamesTemporary.length; j++ )
                	if( ! columnNamesTemporary[j].equals("Length") )
                	{
                    	int ind = ArrayUtils.indexOf(columnNames, columnNamesTemporary[j]);
                    	row[ind] = values[j];
                    	
///**********************************************************/                	
//log.info(" j = " + j + " ind = " + ind + " columnNamesTemporary[j] = " + columnNamesTemporary[j]);
//for( int jj = 0; jj < columnNamesTemporary.length; jj++ )
//log.info(" jj, columnNamesTemporary[jj] = " + jj + " " + columnNamesTemporary[jj]);
//for( int jj = 0; jj < columnNames.length; jj++ )
//log.info(" jj, columnNames[jj] = " + jj + " " + columnNames[jj]);
///************************************************************/

                	}
        	}
        	
            // Remove CombinedSites.COMBINED_FREQUENCY !!!
        	// row[row.length - 1] = (double)dataMatrices.length;
        	
        	matrix[i] = row;
       	
        	
//            /*********************************************************/
//        	if( i < 100 )
//        		for( DataMatrix dataMatrix : dataMatrices )
//        		{
//        			log.info(" *** dataMatrix in (" + i + ")");
//            		DataMatrix.printDataMatrix(dataMatrix);
//        		}
//        	/********************************************************/
       	
        }
        return new DataMatrix(rowNames, columnNames, matrix);
    }

    public static String[] getDistinctRowNames(FunSite[] funSites)
    {
        Set<String> set = new HashSet<>();
        for( FunSite fs : funSites )
            for( String s : fs.getDistinctRowNames() )
                set.add(s);
        return set.toArray(new String[0]);
    }
    
    public static String[] getDistinctColumnNames(FunSite[] funSites)
    {
        Set<String> set = new HashSet<>();
        for( FunSite fs : funSites )
            for( String s : fs.getDistinctColumnNames() )
                set.add(s);
        return set.toArray(new String[0]);
    }
    
    // TODO It is copy from archive
//    public static DataMatrix getDataMatrixGeneralized(FunSite[] funSites)
//    {
//        // 1. Initialize columnNames and rowNames.
//        if( funSites == null || funSites.length == 0 ) return null;
//        String lastColumnName = "Combined_frequency"; // "Number of merged  or overlapped sites";
//        String[] columnNames = new String[0], rowNames = new String[funSites.length];
//        double[][] matrix = new double[funSites.length][];
//        for( int i = 0; i < rowNames.length; i++ )
//            rowNames[i] = "S_" + Integer.toString(i);
//        
//        // 2. Calculate data matrix when  FunSites are of 2-nd type.
//        if( funSites[0].getDataMatrix() != null )
//        {
//            String[] distinctRowNames = getDistinctRowNames(funSites);
//            columnNames = funSites[0].getDataMatrix().getColumnNames();
//            int index = 0, nn = distinctRowNames.length * columnNames.length;
//            String[] columnNamesNew = new String[nn];
//            for( int i = 0; i < columnNames.length; i++ )
//                for( int j = 0; j < distinctRowNames.length; j++ )
//                    columnNamesNew[index++] = columnNames[i] + "_" + distinctRowNames[j];
//            //columnNames = (String[])ArrayUtils.add(columnNames, lastColumnName);
//            for( int ii = 0; ii < funSites.length; ii++ )
//            {
//                double[][] mat = funSites[ii].getDataMatrix().getMatrix();
//                String[] rowNamesParticular = funSites[ii].getDataMatrix().getRowNames();
//                matrix[ii] = UtilsForArray.getConstantArray(nn, Double.NaN);
//                for( int i = 0; i < columnNames.length; i++ )
//                    for( int j = 0; j < rowNamesParticular.length; j++ )
//                    {
//                        String name = columnNames[i] + "_" + rowNamesParticular[j];
//                        int indx = ArrayUtils.indexOf(columnNamesNew, name);
//                        matrix[ii][indx] = mat[j][i];
//                    }
//            }
//            return new DataMatrix(rowNames, columnNamesNew, matrix);
//        }
//        
//        // 3. Calculate distinctRowNames, indices and columnNames.
//        String[] distinctRowNames = new String[0];
//        int[] indices = new int[0];
//        int index = 0;
//        for( FunSite fs : funSites )
//            for( DataMatrix dm : fs.getDataMatrices() )
//            {
//                String name = dm.getRowNames()[0];
//                if( ArrayUtils.contains(distinctRowNames, name) ) continue;
//                distinctRowNames = (String[])ArrayUtils.add(distinctRowNames, name);
//                String[] array = dm.getColumnNames();
//                for( int i = 0; i < array.length; i ++ )
//                    array[i] = name + "_" + array[i];
//                columnNames = (String[])ArrayUtils.addAll(columnNames, array);
//                indices = ArrayUtils.add(indices, index);
//                index += array.length;
//            }
//        columnNames = (String[])ArrayUtils.add(columnNames, lastColumnName);
//
//        
///******************** TEST2 ********************/
//      log.info(" ****** Distinct vectors *******");
//      for( int i = 0; i < distinctRowNames.length; i++ )
//          log.info(" [i] = " + i + " distinctRowNames[i] = " + distinctRowNames[i]);
//      for( int i = 0; i < indices.length; i++ )
//          log.info(" [i] = " + i + " indices[i] = " + indices[i]);
//      for( int i = 0; i < columnNames.length; i++ )
//          log.info(" [i] = " + i + " columnNames[i] = " + columnNames[i]);
//      log.info(" ****** Distinct vectors *******");
///******************** TEST2 ********************/
//      
//      
//
///******************* test *********************/
//      for( int ii = 0; ii < Math.min(100, funSites.length); ii++)
//      {
//      	log.info(" ii) = " + ii + " funSite[ii] : chr = " + funSites[ii].getChromosomeName() + " positions = " + funSites[ii].getStartPosition() + " " + funSites[ii].getFinishPosition());
//      	DataMatrix dm = funSites[ii].getDataMatrix();
//      	if( dm == null ) log.info(" ii) = " + ii + " dm == null");
//      	else
//      		DataMatrix.printDataMatrix(dm);
//      	DataMatrix[] dms = funSites[ii].getDataMatrices();
//      	if( dms == null ) log.info(" ii) = " + ii + " dms == null");
//      	else for( int jj = 0; jj < dms.length; jj++ )
//          {
//          	log.info(" jj = " + jj + " dataMatrix :");
//      		DataMatrix.printDataMatrix(dms[jj]);
//          }
//      }
//      log.info(" ********* TEST2 **********");
///************************************************************************************************/
//        
//      log.info(" funSites.length = " + funSites.length);
//        // 4. Calculate data matrix when  FunSites are of 1-st type.
//        for( int i = 0; i < funSites.length; i++ )
//        {
//            double[] row = UtilsForArray.getConstantArray(columnNames.length, Double.NaN);
//            DataMatrix[] dataMatrices = funSites[i].getDataMatrices();
//            
//
///************************************/            
//          log.info("");
//          log.info(" ********** i) = " + i + " funSite[ii] : chr = " + funSites[i].getChromosomeName() + " positions = " + funSites[i].getStartPosition() + " " + funSites[i].getFinishPosition());
//          log.info(" number of dataMatrices = " + dataMatrices.length);
//      	if( dataMatrices == null ) log.info(" i) = " + i + " DataMatrix[] == null !!!");
//      	else for( int jj = 0; jj < dataMatrices.length; jj++ )
//      	{
//      		log.info(" jj = " + jj + " dataMatrix :");
//      		DataMatrix.printDataMatrix(dataMatrices[jj]);
//      	}
///************************************/            
//          
//            
//            
//            
//            row[row.length - 1] = dataMatrices.length;
//            for( DataMatrix dm : dataMatrices )
//            {
//                String name = dm.getRowNames()[0];
//                index = ArrayUtils.indexOf(distinctRowNames, name);
//                double[][] mat = dm.getMatrix();
//
//	
//log.info(" dim(mat[0]) = " + mat[0].length + " dim(row) = " + row.length + " dim(indices) = " + indices.length + " index = " + index + " indices[index] = " + indices[index]);
//
//
//                
//                UtilsForArray.copyIntoArray(mat[0], row, indices[index]);
//            }
//            matrix[i] = row;
//        }
//        return new DataMatrix(rowNames, columnNames, matrix);
//    }

//////////////////////////////////////////////// copy
    public static DataMatrix getDataMatrixGeneralized(FunSite[] funSites)
    {
        // 1. Initialize columnNames and rowNames.
        if( funSites == null || funSites.length == 0 ) return null;
        String lastColumnName = "Combined_frequency"; // "Number of merged  or overlapped sites";
        String[] columnNames = new String[0], rowNames = new String[funSites.length];
        double[][] matrix = new double[funSites.length][];
        for( int i = 0; i < rowNames.length; i++ )
            rowNames[i] = "S_" + Integer.toString(i);
        
        // 2. Calculate data matrix when  FunSites are of 2-nd type.
        if( funSites[0].getDataMatrix() != null )
        {
            String[] distinctRowNames = getDistinctRowNames(funSites);
            columnNames = funSites[0].getDataMatrix().getColumnNames();
            int index = 0, nn = distinctRowNames.length * columnNames.length;
            String[] columnNamesNew = new String[nn];
            for( int i = 0; i < columnNames.length; i++ )
                for( int j = 0; j < distinctRowNames.length; j++ )
                    columnNamesNew[index++] = columnNames[i] + "_" + distinctRowNames[j];
            //columnNames = (String[])ArrayUtils.add(columnNames, lastColumnName);
            for( int ii = 0; ii < funSites.length; ii++ )
            {
                double[][] mat = funSites[ii].getDataMatrix().getMatrix();
                String[] rowNamesParticular = funSites[ii].getDataMatrix().getRowNames();
                matrix[ii] = UtilsForArray.getConstantArray(nn, Double.NaN);
                for( int i = 0; i < columnNames.length; i++ )
                    for( int j = 0; j < rowNamesParticular.length; j++ )
                    {
                        String name = columnNames[i] + "_" + rowNamesParticular[j];
                        int indx = ArrayUtils.indexOf(columnNamesNew, name);
                        matrix[ii][indx] = mat[j][i];
                    }
            }
            return new DataMatrix(rowNames, columnNamesNew, matrix);
        }
        
        // 3. Calculate distinctRowNames, indices and columnNames.
        String[] distinctRowNames = new String[0];
        int[] indices = new int[0];
        int index = 0;
        for( FunSite fs : funSites )
            for( DataMatrix dm : fs.getDataMatrices() )
            {
                String name = dm.getRowNames()[0];
                if( ArrayUtils.contains(distinctRowNames, name) ) continue;
                distinctRowNames = (String[])ArrayUtils.add(distinctRowNames, name);
                String[] array = dm.getColumnNames();
                for( int i = 0; i < array.length; i ++ )
                    array[i] = name + "_" + array[i];
                columnNames = (String[])ArrayUtils.addAll(columnNames, array);
                indices = ArrayUtils.add(indices, index);
                index += array.length;
            }
        columnNames = (String[])ArrayUtils.add(columnNames, lastColumnName);
        
        // 4. Calculate data matrix when  FunSites are of 1-st type.
        for( int i = 0; i < funSites.length; i++ )
        {
            double[] row = UtilsForArray.getConstantArray(columnNames.length, Double.NaN);
            DataMatrix[] datamatrices = funSites[i].getDataMatrices();
            row[row.length - 1] = datamatrices.length;
            for( DataMatrix dm : datamatrices )
            {
                String name = dm.getRowNames()[0];
                index = ArrayUtils.indexOf(distinctRowNames, name);
                double[][] mat = dm.getMatrix();
                UtilsForArray.copyIntoArray(mat[0], row, indices[index]);
            }
            matrix[i] = row;
        }
        return new DataMatrix(rowNames, columnNames, matrix);
    }
    
    public static Sequence[] getLinearSequencesWithGivenLength(FunSite[] funSites, DataElementPath pathToSequences, int lengthOfSequenceRegion)
    {
        Sequence[] result = new Sequence[funSites.length];
        String oldChromosomeName = funSites[0].getChromosomeName();
        Sequence fullChromosome = pathToSequences.getChildPath(oldChromosomeName).getDataElement(AnnotatedSequence.class).getSequence();
        for( int i = 0; i < funSites.length; i++ )
        {
            String chromosomeName = funSites[i].getChromosomeName();
            if( ! chromosomeName.equals(oldChromosomeName) )
            {
                fullChromosome = pathToSequences.getChildPath(chromosomeName).getDataElement(AnnotatedSequence.class).getSequence();
                oldChromosomeName = chromosomeName;
            }
            result[i] = new LinearSequence(funSites[i].getSequenceRegionWithGivenLength(fullChromosome, lengthOfSequenceRegion));
        }
        return result;
    }
    
//    public static Sequence[] getLinearSequencesWithGivenLength(DataElementPath pathToInputTrack, DataElementPath pathToSequences, int lengthOfSequenceRegion)
//    {
//    	FunSite[] funSites = null;
//    	//Map<String, List<FunSite>> readSitesInTrack(Track track, int minimalLengthOfSite, int maximalLengthOfSite, String[] propertiesNames, String rowName)
//    	
//
//    	
//    	
//    	return getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
//    }

    
    
    public static Sequence[] getSequenceRegions(FunSite[] funSites, DataElementPath pathToSequences)
    {
        Sequence[] result = new Sequence[funSites.length];
        String oldChromosomeName = funSites[0].getChromosomeName();
        Sequence fullChromosome = pathToSequences.getChildPath(oldChromosomeName).getDataElement(AnnotatedSequence.class).getSequence();
        for( int i = 0; i < funSites.length; i++ )
        {
            String chromosomeName = funSites[i].getChromosomeName();
            if( ! chromosomeName.equals(oldChromosomeName) )
            {
                fullChromosome = pathToSequences.getChildPath(chromosomeName).getDataElement(AnnotatedSequence.class).getSequence();
                oldChromosomeName = chromosomeName;
            }
            result[i] = funSites[i].getSequenceRegion(fullChromosome);
        }
        return result;
    }
    public static Sequence[] getSequenceRegions(FunSite[] funSites, DataElementPath pathToSequences, int minimalSiteSize)
    {
        Sequence[] result = new Sequence[funSites.length];
        String oldChromosomeName = funSites[0].getChromosomeName();
        Sequence fullChromosome = pathToSequences.getChildPath(oldChromosomeName).getDataElement(AnnotatedSequence.class).getSequence();
        for( int i = 0; i < funSites.length; i++ )
        {
            String chromosomeName = funSites[i].getChromosomeName();
            if( ! chromosomeName.equals(oldChromosomeName) )
            {
                fullChromosome = pathToSequences.getChildPath(chromosomeName).getDataElement(AnnotatedSequence.class).getSequence();
                oldChromosomeName = chromosomeName;
            }
            if(funSites[i].getLength() < minimalSiteSize)
            {
            	int newStart = funSites[i].getStartPosition() - (int)((minimalSiteSize - funSites[i].getStartPosition())/2); 
            	int newEnd = funSites[i].getFinishPosition() + (minimalSiteSize - (int)((minimalSiteSize 
            			- funSites[i].getStartPosition())/2));
            	if(newStart < 0)
            		newStart = 0;
            	if(newEnd > fullChromosome.getLength())
            		newEnd = fullChromosome.getLength() - 1;
            	DataMatrix dm = null;
            	funSites[i] = new FunSite(chromosomeName, new Interval(newStart, newEnd), funSites[i].getStrand(), dm, null);
            }
            result[i] = funSites[i].getSequenceRegion(fullChromosome);
        }
        return result;
    }

    public static FunSite[] removeUnusualChromosomes(DataElementPath pathToSequences, FunSite[] funSites)
    {
        List<FunSite> result = new ArrayList<>();
        String[] chromosomeNamesAvailable = EnsemblUtils.getStandardSequencesNames(pathToSequences);
        for( FunSite fs : funSites )
            if( ArrayUtils.contains(chromosomeNamesAvailable, fs.getChromosomeName()) )
                result.add(fs);
        return result.toArray(new FunSite[0]);
    }
    
    // TODO: To test it!!!
    // The input 'allSites' must be sorted by 'from'-positions 
    public static FunSite[] getSites(String chromosomeName, int from, int to, Map<String, List<FunSite>> allSites)
    {
    	List<FunSite> result = new ArrayList<>();
    	Interval interval = new Interval(from, to);
    	List<FunSite> funSites = allSites.get(chromosomeName);
    	for( FunSite fs : funSites )
    	{
    		Interval coordinates = fs.getCoordinates();
    		if( coordinates.intersects(interval) )
    			result.add(fs);
    		if( to < fs.getFinishPosition() ) break;
    	}
    	return result.toArray(new FunSite[0]);
    }
    
    private static Logger log = Logger.getLogger(FunSiteUtils.class.getName());
}

