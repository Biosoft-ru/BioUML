package biouml.plugins.gtrd.utils;
//24.03.22
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.StatUtils.SimilaritiesAndDissimilarities.SimilaritiesForBinaryData;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.ListUtil;

public class TrackUtils
{
	public static void comparisonOfTwoTracksByOverlapsFrequencies(DataElementPath pathToFirstTrack, DataElementPath pathToSecondTrack)
	{
    	log.info("*******************************************************************");
    	log.info("***** pathToFirstTrack = " + pathToFirstTrack.toString() + " *****");
    	log.info("***** pathToSecondTrack = " + pathToSecondTrack.toString() + " *****");
    	log.info("*******************************************************************");

		int[] frequecies = getOverlapsFrequencies(pathToFirstTrack, pathToSecondTrack);
    	log.info("overlaps frequencies : n01 = " + frequecies[0] + " n10 = " + frequecies[1] + " n11InFirstTrack = " + frequecies[2] + " n11InSecondTrack = " + frequecies[3] + " n1 = " + frequecies[4] + " n2 = " + frequecies[5]);

    	// 1. Frequency n11InFirstTrack is used.
    	log.info("----- Frequency n11InFirstTrack is used -----");
    	Object[] objects = SimilaritiesForBinaryData.getAllSimilarityMeasuresWhenN00IsAbsent(frequecies[0], frequecies[1], frequecies[2]);
    	String[] measuresNames = (String[])objects[0];
    	double[] measures = (double[])objects[1];
    	for( int i = 0; i < measuresNames.length; i++ )
        	log.info("measuresName = " + measuresNames[i] + " similarity measure = " + measures[i]);
    	
    	// 2. Frequency n11InSecondTrack is used.
    	log.info("----- Frequency n11InSecondTrack is used -----");
    	objects = SimilaritiesForBinaryData.getAllSimilarityMeasuresWhenN00IsAbsent(frequecies[0], frequecies[1], frequecies[3]);
    	measuresNames = (String[])objects[0];
    	measures = (double[])objects[1];
    	for( int i = 0; i < measuresNames.length; i++ )
        	log.info("measuresName = " + measuresNames[i] + " similarity measure = " + measures[i]);
	}

    // It returns track sizes (n1 and n2) and 3 values of contingency table {n01, n10, n11InFirstTrack, n11InSecondTrack). 
	private static int[] getOverlapsFrequencies(DataElementPath pathToFirstTrack, DataElementPath pathToSecondTrack)
	{
		Track trackFirst = (Track)pathToFirstTrack.getDataElement(), trackSecond = (Track)pathToSecondTrack.getDataElement();
		int n01 = 0, n10 = 0, n11InFirstTrack = 0, n11InSecondTrack = 0;
		// 1. Calculation of n1, n10 and n11InFirstTrack.
		DataCollection<Site> dc = trackFirst.getAllSites();
		int n1 = dc.getSize();
		for( Site site : dc )
        {
            //String chromosomeName = site.getSequence().getName();
            Interval coordinates = site.getInterval();
            if( trackSecond.getSites(site.getSequence().getName(), coordinates.getFrom(), coordinates.getTo()).isEmpty() )
            	n10++;
            else
            	n11InFirstTrack++;;
        }
		
		// 2. Calculation of n2, n01 and n11Second.
		dc = trackSecond.getAllSites();
		int n2 = dc.getSize();
		for( Site site : dc )
        {
            Interval coordinates = site.getInterval();
            if( trackFirst.getSites(site.getSequence().getName(), coordinates.getFrom(), coordinates.getTo()).isEmpty() )
            	n01++;
            else
            	n11InSecondTrack++;
        }
		return new int[]{n01, n10, n11InFirstTrack, n11InSecondTrack, n1, n2};
	}
	
	public static void mergeOverlappedSites(DataElementPath pathToFolder, String nameOfInputTrack, AnalysisJobControl jobControl, int fromJobControl, int toJobControl)
	{
		DataElementPath pathToInputTrack = pathToFolder.getChildPath(nameOfInputTrack);
		DataElementPath pathToOutputTrack = pathToFolder.getChildPath(nameOfInputTrack + "_merged_");

		// 1. Read FunSite in input track.
		Track inputTrack = (Track)pathToInputTrack.getDataElement();
		SqlTrack resultedTrack = SqlTrack.createTrack(pathToOutputTrack, null);
        Map<String, List<FunSite>> allOverlappedSites = new HashMap<>();
        DataMatrix dataMatrix = null;
    	log.info("Read sites in track");
		for( Site site : inputTrack.getAllSites() )
		{
			String chromosomeName = site.getSequence().getName();
            Interval coordinates = site.getInterval();
			FunSite funSite = new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix);
			funSite.setObjects(new Object[]{site});
			allOverlappedSites.computeIfAbsent(chromosomeName, key -> new ArrayList<>()).add(funSite);
		}
    	log.info("O.K.1");
		
		// 2. Select a single site (with maximal frequency) from set of overlapped sites and write it.
    	log.info("*** all overlapped sites are sorting ***");
		ListUtil.sortAll(allOverlappedSites);
    	log.info("*** all overlapped sites were sorted ***");
        int index = 0, difference = toJobControl - fromJobControl; 
        List<FunSite> list = new ArrayList<>();
        for( Entry<String, List<FunSite>> entry : allOverlappedSites.entrySet() )
        {
            if( jobControl != null )
                jobControl.setPreparedness(fromJobControl + (++index) * difference / allOverlappedSites.size());
            FunSite[] funSites = entry.getValue().toArray(new FunSite[0]);
            for( int i = 0; i < funSites.length; i++ )
            {
            	if( list.isEmpty() )
            	{
            		list.add(funSites[i]);
            		continue;
            	}
            	Interval coordinates = funSites[i].getCoordinates(), coordinatesPrevious = funSites[i - 1].getCoordinates();
            	if( coordinates.intersects(coordinatesPrevious) )
            		list.add(funSites[i]);
            	else
            	{
            		selectSiteAndWriteItToTrack(list, resultedTrack);
            		list.add(funSites[i]);
            	}
            }
        	if( ! list.isEmpty() )
        		selectSiteAndWriteItToTrack(list, resultedTrack);
        }
        jobControl.setPreparedness(toJobControl);
		resultedTrack.finalizeAddition();
		CollectionFactory.save(resultedTrack);
	}
	
	private static void selectSiteAndWriteItToTrack(List<FunSite> list, SqlTrack track)
	{
    	log.info("dim(list) = " + list.size());
		double frequencySelected = -1.0;
		Site siteSelected = null;
		for( FunSite funSite : list )
		{
			Site site = (Site)funSite.getObjects()[0];
			DynamicPropertySet dpsFirst = site.getProperties();
			double frequency = Double.parseDouble(dpsFirst.getValueAsString("Frequency"));
			if( frequency > frequencySelected )
			{
				frequencySelected = frequency;
				siteSelected = site;
			}
		}
		track.addSite(siteSelected);
		list.clear();
	}
	
//    public static Sequence[] getLinearSequencesMostReliableWithGivenLength(FunSite[] funSites, DataElementPath pathToFolderWithSequences, int numberOfMostReliableSites, int lengthOfSequenceRegion)
//    {
//        funSites = FunSiteUtils.removeUnusualChromosomes(pathToFolderWithSequences, funSites);
//        //funSites[0].getSequenceRegionWithGivenLength(fullChromosome, lengthOfSequenceRegion)
//        double[] scores = new double[funSites.length];
//        for( int i = 0; i < scores.length; i++ )
//        	scores[i] = (funSites[i].getDataMatrix().getColumn(RankAggregation.RA_SCORE))[0];
//        int[] positions = Util.sortHeap(scores);
//        FunSite[] funSitesReliable = null;
//        
//        log.info("positions.length = " + positions.length);
//        log.info("scores.length = " + scores.length);
//        log.info("funSites.length = " + funSites.length);
//        
//        if( numberOfMostReliableSites >= funSites.length ) funSitesReliable = funSites;
//        else
//        {
//        	// TODO temporary def for numberOfMostReliableSites
//        	numberOfMostReliableSites = 100;
//        	funSitesReliable = new FunSite[numberOfMostReliableSites];
//           
//        	//for( int i = 0; i < positions.length; i++ )
//        	for( int i = 0; i < numberOfMostReliableSites; i++ )
//            	// funSitesReliable[i] = funSites[positions[i]];
//            	funSitesReliable[i] = funSites[i];
//            
//            /****************************** test *************/
//            log.info("******** Test for ordering *******");
//            for( int i = 0; i < numberOfMostReliableSites; i++ )
//            {
//                log.info("test: i = " + i);
//            	DataMatrix.printDataMatrix(funSitesReliable[i].getDataMatrix());
//            }
//            log.info("******** Test for ordering *******");
//            /****************************** test *************/
//            
//        }
//        return FunSiteUtils.getLinearSequencesWithGivenLength(funSitesReliable, pathToFolderWithSequences, lengthOfSequenceRegion);
//    }
    
    public static FunSite[] readBestFunSitesInTrack(DataElementPath pathToTrack, int numberOfBestSites, int minimalLengthOfSite, int maximalLengthOfSite)
    {
        // 1. Identify RA-scores[].
        Track track = pathToTrack.getDataElement(Track.class);
        String[] propertiesNames = new String[]{RankAggregation.RA_SCORE}; 
        Map<String, List<FunSite>> sites = FunSiteUtils.readFunSitesOfSecondTypeInTrack(track, minimalLengthOfSite, maximalLengthOfSite, propertiesNames, "site");
        FunSite[] funSites = FunSiteUtils.transformToArray(sites);

        
        // TODO: 1-st test; temp -------------------------------------------
//        log.info("******** Test No 1: funSites.length = " + funSites.length);
//        for( int i = 0; i < 5; i++ )
//        {
//            log.info("******** Test: i = " + i);
//            DataMatrix dm = funSites[i].getDataMatrix();
//            if( dm == null ) log.info("******** Test: DataMatrix == null");
//            DataMatrix[] dms = funSites[i].getDataMatrices();
//            if( dms == null ) log.info("******** Test: dms == null");
//            else log.info("******** Test: number of datamatrices = " + dms.length);
//            DataMatrix.printDataMatrix(dm);
//        }
//        DataMatrix dm = funSites[0].getDataMatrix();
//        double[] column = dm.getColumn(RankAggregation.RA_SCORE);
//        if( column == null ) log.info("******** Test: column == null");
//        log.info("******** column[0] = " + column[0]);
        // 1-st test; temp -------------------------------------------

        
        double[] scores = FunSiteUtils.getRaScores(funSites);

        // 2. Identify threshold of scores
        UtilsForArray.sortInAscendingOrder(scores);
        int number = Math.min(numberOfBestSites, scores.length);
        double threshold = scores[number - 1];
        
        // 3. Select the best sites.
        List<FunSite> list = new ArrayList<>();
        for( int i = 0; i < funSites.length; i++ )
        {
        	double score = (funSites[i].getDataMatrix().getColumn(RankAggregation.RA_SCORE))[0];
            if( score <= threshold )
                list.add(funSites[i]);
            if( list.size() >= number ) break;
        }
        log.info("******** Test No 2: funSites.length = " + funSites.length + " threshold = " + threshold + " list.size() = " + list.size());
        return list.toArray(new FunSite[0]);
    }
	
	private static Logger log = Logger.getLogger(TrackUtils.class.getName());
}