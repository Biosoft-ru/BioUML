/* $Id$ */
package biouml.plugins.gtrd.utils;
//01.04.22
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.gtrd.utils.FunSiteUtils.CombinedSites;
import biouml.plugins.gtrd.utils.FunSiteUtils.QualityControlSites;
import biouml.plugins.machinelearning.distribution_mixture.NormalMixture;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.ModelApplication.DivideSampleByClassification;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.PopulationSize;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample.DensityEstimation;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.standard.type.Species;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.chart.Chart;

/**
 * @author yura
 *
 */
public class CistromUtils
{
    /************************** CistromConstructor : start ******************************/
    public static class CistromConstructor
    {
    	// 01.04.22
        //public static final String OPTION_01 = "Meta-clusters construction (METARA is used for all TFs available for given cell line)";
        //public static final String OPTION_02 = "Meta-clusters construction (METARA is used for all TFs available)";
        //public static final String OPTION_03 = "DNase meta-clusters constriction";
    	
        public static final String OPTION_04 = "Selection of the most reliable meta-clusters (based on 2-component normal mixture of RA-scores)";
        public static final String OPTION_05 = "Calculation of site motif scores";
        public static final String OPTION_06 = "Assignment of probabilities to meta-clusters by classification model";

        public static final String NAME_OF_FOLDER_WITH_COMBINED_PEAKS = "Combined_peaks";
        
        // TODO: To create method for automatic calculation of PEAK_CALLER_CHARACTERISTICS and DO_SORT_IN_INCREASING_ORDER!
        public static final String[] PEAK_CALLER_CHARACTERISTICS = {"macs_fold_enrichment", "macs_-10*log10(pvalue)", "macs_FDR(%)", "macs_tags", "pics_score", "sissrs_p-value", "sissrs_fold", "sissrs_tags", "gem_Fold", "gem_P-lg10", "gem_Noise", "gem_P_poiss", "gem_Q_-lg10", "macs2_-log10(pvalue)", "macs2_-log10(qvalue)", "macs2_fold_enrichment", "macs2_pileup", "hotspot2_itemRGB"};
        public static final boolean[] DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER = {false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false};
        
        private String cistromType;
        private String combinedPeakType;
        private String rankAggregationMethod;
        private double fpcmThreshold;
        private int siteNumberThreshold;
        private DataElementPath pathToFolderWithFolders;
        private String[] foldersNames;
        private int minimalLengthOfPeaks;
        private int maximalLengthOfPeaks;
        private DataElementPath pathToOutputFolder;
        private DataElementPath pathToFolderWithCombinedPeaks;
        private Track trackWithSiteMotifs;
        private boolean doAddMotifsIntoFirstRaStep;
        private boolean doAddMotifsIntoSecondRaStep;
        private String tfClass;
        
        // It is constructor for cistrom construction for GTRD tracks (meta-clusters for every available TF-class or meta-clusters for given cell line or DNase meta-clusters).
        
//        public CistromConstructor(String cistromType, Species givenSpecie, String cellLine, String combinedPeakType, boolean doRemoveCellTreatments, String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithFolders, String[] foldersNames, int minimalLengthOfPeaks, int maximalLengthOfPeaks, DataElementPath pathToOutputFolder, AnalysisJobControl jobControl, int from, int to)
//        {
//        	CistromConstructor(cistromType, givenSpecie, cellLine, combinedPeakType, doRemoveCellTreatments, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder, null, false, jobControl, from, to);
//        }
        
         ////////////////////////////// new
//        public CistromConstructor(String cistromType, Species givenSpecie, String cellLine, String combinedPeakType, boolean doRemoveCellTreatments, String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithFolders, String[] foldersNames, int minimalLengthOfPeaks, int maximalLengthOfPeaks, DataElementPath pathToOutputFolder, Track trackWithSiteMotifs, String tfClass, boolean doAddMotifsIntoFirstRaStep, boolean doAddMotifsIntoSecondRaStep, AnalysisJobControl jobControl, int from, int to)
//        {
//            // 0. Initialize some Class fields.
//            this.cistromType = cistromType;
//            this.combinedPeakType = combinedPeakType;
//            this.rankAggregationMethod = rankAggregationMethod;
//            this.fpcmThreshold = fpcmThreshold;
//            this.siteNumberThreshold = siteNumberThreshold;
//            this.pathToFolderWithFolders = pathToFolderWithFolders;
//            this.foldersNames = foldersNames;
//            this.minimalLengthOfPeaks = minimalLengthOfPeaks;
//            this.maximalLengthOfPeaks = maximalLengthOfPeaks;
//            this.pathToOutputFolder = pathToOutputFolder;
//            this.trackWithSiteMotifs = trackWithSiteMotifs;
//            this.doAddMotifsIntoFirstRaStep = doAddMotifsIntoFirstRaStep;
//            this.doAddMotifsIntoSecondRaStep = doAddMotifsIntoSecondRaStep;
//            this.tfClass = tfClass;
//            
//            // 1. Identify  trackInfos and distinctObjects = {distinctTfClasses or distinctCellLines}.
//            TrackInfo[] trackInfos = TrackInfo.getTracksInfo(this.pathToFolderWithFolders, this.foldersNames, givenSpecie, null, cellLine, null);
//             
//
//            // TODO:
//            log.info("trackInfos.length = " + trackInfos.length);
//            for( int i = 0; i < trackInfos.length; i++ )
//            {
//                String specie = trackInfos[i].getSpecie(), cellLine_ = trackInfos[i].getCellLine();
//                log.info("i = " + i + " specie = " + specie + " cellLine_ = " + cellLine_);
//            }
//
//            if( this.cistromType.equals(OPTION_01) || this.cistromType.equals(OPTION_02) )
//                trackInfos = TrackInfo.removeTrackInfosWithoutTfClasses(trackInfos);
//            if( doRemoveCellTreatments )
//            {
//                for( TrackInfo ti : trackInfos )
//                    ti.replaceTreatment();
//                trackInfos = TrackInfo.removeTrackInfosWithCellTreatments(trackInfos);
//            }
//            String[] distinctObjects = null;
//            switch( this.cistromType )
//            {
//                case OPTION_01:
//                case OPTION_02: if( this.tfClass == null ) distinctObjects = TrackInfo.getDistinctTfClasses(trackInfos);
//                				else  distinctObjects = new String[]{this.tfClass}; break;
//                case OPTION_03: distinctObjects = TrackInfo.getDistinctCellLines(trackInfos); break;
//            }
//            log.info("number of distinct object (for example, TF-classes or cell lines) = " + distinctObjects.length);
//            
//            // 2. Remove analyzed distinctTfClasses.
//            if( pathToOutputFolder.exists() )
//            {
//                String[] fileNames = pathToOutputFolder.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
//                if( fileNames.length > 0 )
//                {
//                    List<String> list = new ArrayList<>();
//                    for( String s : distinctObjects )
//                        if( ! ArrayUtils.contains(fileNames, s) )
//                            list.add(s);
//                    distinctObjects = list.toArray(new String[0]);
//                }
//            }
//            
//            // 3. Identify combined sites for distinct objects.
//            pathToFolderWithCombinedPeaks = this.pathToOutputFolder.getChildPath(NAME_OF_FOLDER_WITH_COMBINED_PEAKS);
//            DataCollectionUtils.createFoldersForPath(pathToFolderWithCombinedPeaks.getChildPath(""));
//            int difference = to - from;
//            for( int i = 0; i < distinctObjects.length; i++ )
//            {
//                if( jobControl != null )
//                    jobControl.setPreparedness(from + (i + 1) * difference / distinctObjects.length);
//                
//                // 3.1. Calculate RA-scores for each trackName for peak callers.
//                String[] trackNames = null;
//                switch( this.cistromType )
//                {
//                    case OPTION_01:
//                    case OPTION_02: trackNames = TrackInfo.getTrackNames(trackInfos, distinctObjects[i], null, null); break;
//                    case OPTION_03: trackNames = TrackInfo.getTrackNames(trackInfos, null, null, distinctObjects[i]); break;
//                }
//                log.info("*** i = " + i + " distinct object = " + distinctObjects[i] + " number of tracks = " + trackNames.length + " ***");
//                trackNames = implementTwoStepRankAggregationForPeakCallers(trackNames, false);
//                if( trackNames.length < 1 ) continue;
//                
//                // 3.2. Calculate combinedSites and RA-scores for given distinct object.
//                log.info("*** Calculation of combined sites and RA-scores for given distinct object. ***");
//                int size = implementRankAggregationForTracks(trackNames, distinctObjects[i]);
//                TableAndFileUtils.addRowToTable(new double[]{(double)trackNames.length, (double)size}, null, distinctObjects[i], new String[]{"Number_of_combined_tracks", "Number_of_combined_sites"}, pathToOutputFolder, "summary");
//            }
//        }

////////////////////////////////////////// old
        public CistromConstructor(String cistromType, Species givenSpecie, String cellLine, String combinedPeakType, boolean doRemoveCellTreatments, String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithFolders, String[] foldersNames, int minimalLengthOfPeaks, int maximalLengthOfPeaks, DataElementPath pathToOutputFolder, Track trackWithSiteMotifs, String tfClass, boolean doAddMotifsIntoFirstRaStep, boolean doAddMotifsIntoSecondRaStep, AnalysisJobControl jobControl, int from, int to)
        {
            // 0. Initialize some Class fields.
            this.cistromType = cistromType;
            this.combinedPeakType = combinedPeakType;
            this.rankAggregationMethod = rankAggregationMethod;
            this.fpcmThreshold = fpcmThreshold;
            this.siteNumberThreshold = siteNumberThreshold;
            this.pathToFolderWithFolders = pathToFolderWithFolders;
            this.foldersNames = foldersNames;
            this.minimalLengthOfPeaks = minimalLengthOfPeaks;
            this.maximalLengthOfPeaks = maximalLengthOfPeaks;
            this.pathToOutputFolder = pathToOutputFolder;
            this.trackWithSiteMotifs = trackWithSiteMotifs;
            this.doAddMotifsIntoFirstRaStep = doAddMotifsIntoFirstRaStep;
            this.doAddMotifsIntoSecondRaStep = doAddMotifsIntoSecondRaStep;
            this.tfClass = tfClass;
            
            // 1. Identify  trackInfos and distinctObjects = {distinctTfClasses or distinctCellLines}.
            TrackInfo[] trackInfos = TrackInfo.getTracksInfo(this.pathToFolderWithFolders, this.foldersNames, givenSpecie, null, cellLine, null);
             

            // TODO:
            log.info("trackInfos.length = " + trackInfos.length);
            for( int i = 0; i < trackInfos.length; i++ )
            {
                String specie = trackInfos[i].getSpecie(), cellLine_ = trackInfos[i].getCellLine();
                log.info("i = " + i + " specie = " + specie + " cellLine_ = " + cellLine_);
            }

            if( this.cistromType.equals(MetaClusterConsrtruction.OPTION_01) || this.cistromType.equals(MetaClusterConsrtruction.OPTION_02) )
                trackInfos = TrackInfo.removeTrackInfosWithoutTfClasses(trackInfos);
            if( doRemoveCellTreatments )
            {
                for( TrackInfo ti : trackInfos )
                    ti.replaceTreatment();
                trackInfos = TrackInfo.removeTrackInfosWithCellTreatments(trackInfos);
            }
            String[] distinctObjects = null;
            
            // 01.04.22
            switch( this.cistromType )
            {
                case MetaClusterConsrtruction.OPTION_01:
                case MetaClusterConsrtruction.OPTION_02: if( this.tfClass == null ) distinctObjects = TrackInfo.getDistinctTfClasses(trackInfos);
                				else  distinctObjects = new String[]{this.tfClass}; break;
                case MetaClusterConsrtruction.OPTION_03: distinctObjects = TrackInfo.getDistinctCellLines(trackInfos); break;
            }
            log.info("number of distinct object (for example, TF-classes or cell lines) = " + distinctObjects.length);
            
            // 2. Remove analyzed distinctTfClasses.
            if( pathToOutputFolder.exists() )
            {
                String[] fileNames = pathToOutputFolder.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
                if( fileNames.length > 0 )
                {
                    List<String> list = new ArrayList<>();
                    for( String s : distinctObjects )
                        if( ! ArrayUtils.contains(fileNames, s) )
                            list.add(s);
                    distinctObjects = list.toArray(new String[0]);
                }
            }
            
            // 3. Identify combined sites for distinct objects.
            pathToFolderWithCombinedPeaks = this.pathToOutputFolder.getChildPath(NAME_OF_FOLDER_WITH_COMBINED_PEAKS);
            DataCollectionUtils.createFoldersForPath(pathToFolderWithCombinedPeaks.getChildPath(""));
            int difference = to - from;
            for( int i = 0; i < distinctObjects.length; i++ )
            {
                if( jobControl != null )
                    jobControl.setPreparedness(from + (i + 1) * difference / distinctObjects.length);
                
                // 3.1. Calculate RA-scores for each trackName for peak callers.
                String[] trackNames = null;
                
                // 01.04.22
                switch( this.cistromType )
                {
                    case MetaClusterConsrtruction.OPTION_01:
                    case MetaClusterConsrtruction.OPTION_02: trackNames = TrackInfo.getTrackNames(trackInfos, distinctObjects[i], null, null); break;
                    case MetaClusterConsrtruction.OPTION_03: trackNames = TrackInfo.getTrackNames(trackInfos, null, null, distinctObjects[i]); break;
                }
                log.info("*** i = " + i + " distinct object = " + distinctObjects[i] + " number of tracks = " + trackNames.length + " ***");
                trackNames = implementTwoStepRankAggregationForPeakCallers(trackNames, false);
                if( trackNames.length < 1 ) continue;
                
                // 3.2. Calculate combinedSites and RA-scores for given distinct object.
                log.info("*** Calculation of combined sites and RA-scores for given distinct object. ***");
                int size = implementRankAggregationForTracks(trackNames, distinctObjects[i]);
                TableAndFileUtils.addRowToTable(new double[]{(double)trackNames.length, (double)size}, null, distinctObjects[i], new String[]{"Number_of_combined_tracks", "Number_of_combined_sites"}, pathToOutputFolder, "summary");
            }
        }
/////////////////////////////////////////        
        
        
        // Constructor for METARA/IMETARA-methods.
        // nameOfResultedTrackInInitialStep = {May be the name of tTfClass or  name of cellLine}.
//        public CistromConstructor(String cistromType, String combinedPeakType, String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithFolders, String[] foldersNames, String[] trackNames, String nameOfResultedTrack, int minimalLengthOfPeaks, int maximalLengthOfPeaks, DataElementPath pathToOutputFolder)
//        {
//            // 0. Initialize some Class fields.
//            this.cistromType = cistromType;
//            this.combinedPeakType = combinedPeakType;
//            this.rankAggregationMethod = rankAggregationMethod;
//            this.fpcmThreshold = fpcmThreshold;
//            this.siteNumberThreshold = siteNumberThreshold;
//            this.pathToFolderWithFolders = pathToFolderWithFolders;
//            this.foldersNames = foldersNames;
//            this.minimalLengthOfPeaks = minimalLengthOfPeaks;
//            this.maximalLengthOfPeaks = maximalLengthOfPeaks;
//            this.pathToOutputFolder = pathToOutputFolder;
//            
//            pathToFolderWithCombinedPeaks = this.pathToOutputFolder.getChildPath(NAME_OF_FOLDER_WITH_COMBINED_PEAKS);
//            DataCollectionUtils.createFoldersForPath(pathToFolderWithCombinedPeaks.getChildPath(""));
//            //1. Calculate RA-scores for each trackName for peak callers.
//            log.info("*** name of resulted track = " + nameOfResultedTrack + " number of tracks = " + trackNames.length + " ***");
//            trackNames = implementTwoStepRankAggregationForPeakCallers(trackNames, false);
//            if( trackNames.length < 1 ) return;
//            
//            //2. Calculate combinedSites and RA-scores for given tracks.
//            log.info("*** Calculation of combined sites and RA-scores for given distinct object. ***");
//            int size = implementRankAggregationForTracks(trackNames, nameOfResultedTrack);
//            // TableAndFileUtils.addRowToTable(new double[]{(double)trackNames.length, (double)size}, null, objectName, new String[]{"Number_of_combined_tracks", "Number_of_combined_sites"}, pathToOutputFolder, "summary");
//        }

////////////////////////////////////////// old
        // Constructor for METARA/IMETARA-methods.
        // nameOfResultedTrackInInitialStep = {May be the name of tTfClass or  name of cellLine}.
        public CistromConstructor(String cistromType, String combinedPeakType, String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithFolders, String[] foldersNames, String[] trackNames, String nameOfResultedTrack, int minimalLengthOfPeaks, int maximalLengthOfPeaks, DataElementPath pathToOutputFolder)
        {
            // 0. Initialize some Class fields.
            this.cistromType = cistromType;
            this.combinedPeakType = combinedPeakType;
            this.rankAggregationMethod = rankAggregationMethod;
            this.fpcmThreshold = fpcmThreshold;
            this.siteNumberThreshold = siteNumberThreshold;
            this.pathToFolderWithFolders = pathToFolderWithFolders;
            this.foldersNames = foldersNames;
            this.minimalLengthOfPeaks = minimalLengthOfPeaks;
            this.maximalLengthOfPeaks = maximalLengthOfPeaks;
            this.pathToOutputFolder = pathToOutputFolder;
            
            pathToFolderWithCombinedPeaks = this.pathToOutputFolder.getChildPath(NAME_OF_FOLDER_WITH_COMBINED_PEAKS);
            DataCollectionUtils.createFoldersForPath(pathToFolderWithCombinedPeaks.getChildPath(""));
            //1. Calculate RA-scores for each trackName for peak callers.
            log.info("*** name of resulted track = " + nameOfResultedTrack + " number of tracks = " + trackNames.length + " ***");
            trackNames = implementTwoStepRankAggregationForPeakCallers(trackNames, false);
            if( trackNames.length < 1 ) return;
            
            //2. Calculate combinedSites and RA-scores for given tracks.
            log.info("*** Calculation of combined sites and RA-scores for given distinct object. ***");
            int size = implementRankAggregationForTracks(trackNames, nameOfResultedTrack);
            // TableAndFileUtils.addRowToTable(new double[]{(double)trackNames.length, (double)size}, null, objectName, new String[]{"Number_of_combined_tracks", "Number_of_combined_sites"}, pathToOutputFolder, "summary");
        }

        private int implementRankAggregationForTracks(String[] trackNames, String nameOfResultedTrack)
        {
            if( trackNames == null || trackNames.length < 1 ) return 0;
            if( trackNames.length == 1 )
            {
                Track track = pathToFolderWithCombinedPeaks.getChildPath(trackNames[0]).getDataElement(Track.class);
                Map<String, List<FunSite>> sites = FunSiteUtils.readSitesInTrack(track, 0, Integer.MAX_VALUE, new String[]{RankAggregation.RA_SCORE}, trackNames[0]);
                FunSite[] funSites = FunSiteUtils.transformToArray(sites);
                DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
                String[] names = dm.getColumnNames();
                String name = null;
                for( String str : names )
                    if( str.contains(RankAggregation.RA_SCORE) )
                        name = str;
                double[] scores = dm.getColumn(name);
                Object[] objects = new Object[]{funSites[0].getDistinctRowNames()};
                for( FunSite fs : funSites )
                    fs.setObjects(objects);
                FunSiteUtils.writeSitesToSqlTrack(funSites, RankAggregation.RA_SCORE, scores, pathToOutputFolder, nameOfResultedTrack);
                return funSites.length;
            }
            CombinedSites combinedSites = new CombinedSites(combinedPeakType, pathToFolderWithCombinedPeaks, trackNames, minimalLengthOfPeaks, maximalLengthOfPeaks, false);
            FunSite[] funSites = combinedSites.getCombinedSites();
            double[] scores = calculateRankAggregationScores(funSites);
            for( FunSite fs : funSites )
                fs.setObjects(new Object[]{fs.getDistinctRowNames()});

            // TODO: check mergeNearestRegions()
            funSites = mergeNearestRegions(funSites, scores);
            scores = calculateRankAggregationScores(funSites);
            FunSiteUtils.writeSitesToSqlTrack(funSites, RankAggregation.RA_SCORE, scores, pathToOutputFolder, nameOfResultedTrack);
            return funSites.length;
        }
        
        private double[] calculateRankAggregationScores(FunSite[] funSites)
        {
            DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
            String[] columnNames = dm.getColumnNames();
            for( int j = columnNames.length - 1; j >= 0; j-- )
                if( ! columnNames[j].contains(RankAggregation.RA_SCORE) )
                    dm.removeColumn(columnNames[j]);
            RankAggregation ra = new RankAggregation(dm, UtilsForArray.getConstantArray(dm.getColumnNames().length, true));
            return ra.getScoresTransformed(rankAggregationMethod, null, null, null);
        }

//        private String[] implementTwoStepRankAggregationForPeakCallers(String[] trackNames, boolean doIncludeCombinedFrequency)
//        {
//            List<String> listOfNamesOfSavedTracks = new ArrayList<>();
//            for( int j = 0; j < trackNames.length; j++ )
//            {
//                log.info(" j = " + j + " trackName = " + trackNames[j]);
//                if( implementTwoStepRankAggregationForPeakCallers(trackNames[j], doIncludeCombinedFrequency) )
//                    listOfNamesOfSavedTracks.add(trackNames[j]);
//            }
//            return listOfNamesOfSavedTracks.toArray(new String[0]);
//        }

/////////////////////////////////////// old
        private String[] implementTwoStepRankAggregationForPeakCallers(String[] trackNames, boolean doIncludeCombinedFrequency)
        {
            List<String> listOfNamesOfSavedTracks = new ArrayList<>();
            for( int j = 0; j < trackNames.length; j++ )
            {
                log.info(" j = " + j + " trackName = " + trackNames[j]);
                if( implementTwoStepRankAggregationForPeakCallers(trackNames[j], doIncludeCombinedFrequency) )
                    listOfNamesOfSavedTracks.add(trackNames[j]);
            }
            return listOfNamesOfSavedTracks.toArray(new String[0]);
        }
        
//        private boolean implementTwoStepRankAggregationForPeakCallers(String trackName, boolean doIncludeCombinedFrequency)
//        {
//        	//int wForMotifs = 20;
//        	
//            // 1.1 Calculate ranks for whole data matrix.
//            Object[] objects = getCombinedSitesAndDataMatrixFromPeakCallers(trackName, doIncludeCombinedFrequency);
//            if( objects == null ) return false;
//            FunSite[] funSites = (FunSite[])objects[0];
//            DataMatrix dm = (DataMatrix)objects[1];
//            if( dm.getSize() < siteNumberThreshold ) return false;
//            
//            // 1.2. 
//            if ( doAddMotifsIntoFirstRaStep )
//            {
//            	double[] motifScores = new double[funSites.length];
//            	String[] propertiesNames = new String[]{"score"};
//            	double minScore = Double.MAX_VALUE;
//            	DataCollection<Site> sites = trackWithSiteMotifs.getAllSites();
//            	for( Site site : sites )
//                	minScore = Math.min(minScore, SiteUtils.getProperties(site, propertiesNames)[0]);
//                for( int i = 0; i < funSites.length; i++ )
//                {
//                	DataCollection<Site> dc = trackWithSiteMotifs.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
//                	if( dc.isEmpty() )
//                		motifScores[i] = minScore;
//                	else
//                	{
//                		double maxScore = 0.0;
//                		for( Site sitesLocal : dc )
//                    		motifScores[i] = Math.max(maxScore, SiteUtils.getProperties(sitesLocal, propertiesNames)[0]);
//                	}
//                }
//                dm.addColumn("motif_score", motifScores, dm.getColumnNames().length);
//            }
//
//            // 1.3.
//            boolean[] doSortInIncreasingOrder = (boolean[])objects[2];
//            doSortInIncreasingOrder = ArrayUtils.add(doSortInIncreasingOrder, false);
//            RankAggregation ra = new RankAggregation(dm, doSortInIncreasingOrder);
//            dm = ra.getRanks();
//
//            // 2. Calculate dataMatrix with RA-scores at 1-st step.
//            DataMatrix dataMatrix = null;
//            String[] rowNames = dm.getRowNames(), columnNames = dm.getColumnNames(), headers = new String[columnNames.length];
//            for( int j = 0; j < headers.length; j++ )
//                headers[j] = columnNames[j].split("_")[0];
//            for( String folderName : foldersNames )
//            {
//                List<String> list = new ArrayList<>();
//                for( int j = 0; j < headers.length; j++ )
//                    if( folderName.equals(headers[j]) )
//                        list.add(columnNames[j]);
//                if( list.size() < 1 ) continue;
//                DataMatrix subRanks = dm.getSubDataMatrixColumnWise(list.toArray(new String[0]));
//                if( subRanks.getColumnNames().length == 1 )
//                {
//                    double[] scores = subRanks.getColumn(subRanks.getColumnNames()[0]);
//                    DataMatrix dmNew = new DataMatrix(subRanks.getRowNames(), folderName + "_" + RankAggregation.RA_SCORE, scores);
//                    dataMatrix = dataMatrix == null ? dmNew : DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dataMatrix, dmNew});
//                    continue;
//                }
//                ra = new RankAggregation(subRanks);
//                double[] scores = ra.getScores(rankAggregationMethod, null, null, null);
//                DataMatrix dmNew = new DataMatrix(rowNames, folderName + "_" + RankAggregation.RA_SCORE, scores);
//                dataMatrix = dataMatrix == null ? dmNew : DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dataMatrix, dmNew});
//            }
//            
//            // 3. Calculate RA-scores at 2-nd step.
//            if ( doAddMotifsIntoSecondRaStep )
//            {
//            	double[] motifScores = new double[funSites.length];
//            	String[] propertiesNames = new String[]{"score"};
//            	double minScore = Double.MAX_VALUE;
//            	DataCollection<Site> sites = trackWithSiteMotifs.getAllSites();
//            	for( Site site : sites )
//                	minScore = Math.min(minScore, SiteUtils.getProperties(site, propertiesNames)[0]);
//                for( int i = 0; i < funSites.length; i++ )
//                {
//                	DataCollection<Site> dc = trackWithSiteMotifs.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
//                	if( dc.isEmpty() )
//                		motifScores[i] = minScore;
//                	else
//                	{
//                		double maxScore = 0.0;
//                		for( Site sitesLocal : dc )
//                    		motifScores[i] = Math.max(maxScore, SiteUtils.getProperties(sitesLocal, propertiesNames)[0]);
//                	}
//                }
//                dataMatrix.addColumn("motif_score", motifScores, dataMatrix.getColumnNames().length);
//            }
//            ra = new RankAggregation(dataMatrix, UtilsForArray.getConstantArray(dataMatrix.getColumnNames().length, true));
//            double[] scores = ra.getScoresTransformed(rankAggregationMethod, null, null, null);
//            
//            
//            /// TODO: temp
//            log.info(" size of funSites = " + funSites.length + " scores.length = " + scores.length);
//            ///
//            
//            FunSiteUtils.writeSitesToSqlTrack(funSites, RankAggregation.RA_SCORE, scores, pathToFolderWithCombinedPeaks, trackName);
//            return true;
//        }

/////////////////////////////////// old
        private boolean implementTwoStepRankAggregationForPeakCallers(String trackName, boolean doIncludeCombinedFrequency)
        {
        	//int wForMotifs = 20;
        	
            // 1.1 Calculate ranks for whole data matrix.
            Object[] objects = getCombinedSitesAndDataMatrixFromPeakCallers(trackName, doIncludeCombinedFrequency);
            if( objects == null ) return false;
            FunSite[] funSites = (FunSite[])objects[0];
            DataMatrix dm = (DataMatrix)objects[1];
            if( dm.getSize() < siteNumberThreshold ) return false;
            
            // 1.2. 
            if ( doAddMotifsIntoFirstRaStep )
            {
            	double[] motifScores = new double[funSites.length];
            	String[] propertiesNames = new String[]{"score"};
            	double minScore = Double.MAX_VALUE;
            	DataCollection<Site> sites = trackWithSiteMotifs.getAllSites();
            	for( Site site : sites )
                	minScore = Math.min(minScore, SiteUtils.getProperties(site, propertiesNames)[0]);
                for( int i = 0; i < funSites.length; i++ )
                {
                	DataCollection<Site> dc = trackWithSiteMotifs.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
                	if( dc.isEmpty() )
                		motifScores[i] = minScore;
                	else
                	{
                		double maxScore = 0.0;
                		for( Site sitesLocal : dc )
                    		motifScores[i] = Math.max(maxScore, SiteUtils.getProperties(sitesLocal, propertiesNames)[0]);
                	}
                }
                dm.addColumn("motif_score", motifScores, dm.getColumnNames().length);
            }

            // 1.3.
            boolean[] doSortInIncreasingOrder = (boolean[])objects[2];
            doSortInIncreasingOrder = ArrayUtils.add(doSortInIncreasingOrder, false);
            RankAggregation ra = new RankAggregation(dm, doSortInIncreasingOrder);
            dm = ra.getRanks();

            // 2. Calculate dataMatrix with RA-scores at 1-st step.
            DataMatrix dataMatrix = null;
            String[] rowNames = dm.getRowNames(), columnNames = dm.getColumnNames(), headers = new String[columnNames.length];
            for( int j = 0; j < headers.length; j++ )
                headers[j] = columnNames[j].split("_")[0];
            for( String folderName : foldersNames )
            {
                List<String> list = new ArrayList<>();
                for( int j = 0; j < headers.length; j++ )
                    if( folderName.equals(headers[j]) )
                        list.add(columnNames[j]);
                if( list.size() < 1 ) continue;
                DataMatrix subRanks = dm.getSubDataMatrixColumnWise(list.toArray(new String[0]));
                if( subRanks.getColumnNames().length == 1 )
                {
                    double[] scores = subRanks.getColumn(subRanks.getColumnNames()[0]);
                    DataMatrix dmNew = new DataMatrix(subRanks.getRowNames(), folderName + "_" + RankAggregation.RA_SCORE, scores);
                    dataMatrix = dataMatrix == null ? dmNew : DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dataMatrix, dmNew});
                    continue;
                }
                ra = new RankAggregation(subRanks);
                double[] scores = ra.getScores(rankAggregationMethod, null, null, null);
                DataMatrix dmNew = new DataMatrix(rowNames, folderName + "_" + RankAggregation.RA_SCORE, scores);
                dataMatrix = dataMatrix == null ? dmNew : DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dataMatrix, dmNew});
            }
            
            // 3. Calculate RA-scores at 2-nd step.
            if ( doAddMotifsIntoSecondRaStep )
            {
            	double[] motifScores = new double[funSites.length];
            	String[] propertiesNames = new String[]{"score"};
            	double minScore = Double.MAX_VALUE;
            	DataCollection<Site> sites = trackWithSiteMotifs.getAllSites();
            	for( Site site : sites )
                	minScore = Math.min(minScore, SiteUtils.getProperties(site, propertiesNames)[0]);
                for( int i = 0; i < funSites.length; i++ )
                {
                	DataCollection<Site> dc = trackWithSiteMotifs.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
                	if( dc.isEmpty() )
                		motifScores[i] = minScore;
                	else
                	{
                		double maxScore = 0.0;
                		for( Site sitesLocal : dc )
                    		motifScores[i] = Math.max(maxScore, SiteUtils.getProperties(sitesLocal, propertiesNames)[0]);
                	}
                }
                dataMatrix.addColumn("motif_score", motifScores, dataMatrix.getColumnNames().length);
            }
            ra = new RankAggregation(dataMatrix, UtilsForArray.getConstantArray(dataMatrix.getColumnNames().length, true));
            double[] scores = ra.getScoresTransformed(rankAggregationMethod, null, null, null);
            
            
            /// TODO: temp
            log.info(" size of funSites = " + funSites.length + " scores.length = " + scores.length);
            ///
            
            FunSiteUtils.writeSitesToSqlTrack(funSites, RankAggregation.RA_SCORE, scores, pathToFolderWithCombinedPeaks, trackName);
            return true;
        }

//        private Object[] getCombinedSitesAndDataMatrixFromPeakCallers(String trackName, boolean doIncludeCombinedFrequency)
//        {
//            // 1. Definition of columnNamesSelected and doSortInIncreasingOrder.
//            String[] columnNamesSelected = doIncludeCombinedFrequency ? PEAK_CALLER_CHARACTERISTICS : (String[])ArrayUtils.add(PEAK_CALLER_CHARACTERISTICS, "Combined_frequency");
//            boolean[] doSortInIncreasingOrder = doIncludeCombinedFrequency ? DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER : ArrayUtils.add(DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER, false);
//        
//            // 2. Create combined sites.
//            
////            /// TODO: temp for test: start;
////            log.info(" ??? pathToFolderWithFolders = " + pathToFolderWithFolders.toString() + " trackName = " + trackName);
////            for( int i = 0; i < foldersNames.length; i++ )
////            {
////                log.info(" ??? i = " + i + " foldersNames[i] = " + foldersNames[i]);
////                SqlTrack track = SqlTrack.createTrack(pathToOutputFolder.getChildPath(trackName), null);
////                log.info(" ??? track_size = " + track.getSize());
////            }
////            /// TODO: temp for test: start;
//            CombinedSites combinedSites = new CombinedSites(combinedPeakType, pathToFolderWithFolders, foldersNames, trackName, minimalLengthOfPeaks, maximalLengthOfPeaks, true);
//            FunSite[] funSites = combinedSites.getCombinedSites();
//            
//            
//            /*********************************************************************************************/
////            log.info(" ********* TEST1 **********");
////            log.info(" combinedPeakType = " + combinedPeakType);
////            log.info(" pathToFolderWithFolders = " + pathToFolderWithFolders);
////            for( int ii = 0; ii < foldersNames.length; ii++)
////                log.info(" foldersNames = " + foldersNames[ii]);
////            log.info(" trackName = " + trackName);
////            log.info(" funSites.length = " + funSites.length);
////            for( int ii = 0; ii < Math.min(100, funSites.length); ii++)
////            {
////                log.info(" ii) = " + ii + " funSite[ii] : chr = " + funSites[ii].getChromosomeName() + " positions = " + funSites[ii].getStartPosition() + " " + funSites[ii].getFinishPosition());
////            	DataMatrix dm = funSites[ii].getDataMatrix();
////            	if( dm == null ) log.info(" ii) = " + ii + " dm == null");
////            	else
////            		DataMatrix.printDataMatrix(dm);
////            	DataMatrix[] dms = funSites[ii].getDataMatrices();
////            	if( dms == null ) log.info(" ii) = " + ii + " dms == null");
////            	else for( int jj = 0; jj < dms.length; jj++ )
////                {
////                	log.info(" jj = " + jj + " dataMatrix :");
////            		DataMatrix.printDataMatrix(dms[jj]);
////                }
////            }
////            log.info(" ********* TEST1 **********");
//            /************************************************************************************************/
//
//
//            if( funSites.length < siteNumberThreshold ) return null;
//            
//            // 3. Perform quality control.
//            if( ! Double.isNaN(fpcmThreshold) )
//            {
//                int[] pivotalFrequencies = QualityControlSites.calculatePivotalFrequencies(funSites, foldersNames.length);
//                double fpcm = PopulationSize.getFpcm(pivotalFrequencies[0], pivotalFrequencies[1], pivotalFrequencies[2]);
//                if( fpcm > fpcmThreshold )
//                    funSites = QualityControlSites.removeOrphans(funSites);
//                if( funSites.length < siteNumberThreshold ) return null;
//            }
//            
//            /////////////////////////// For refactoring METARA
//            // 4. Remove degenerated columns.
//            //DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
//            DataMatrix dm = FunSiteUtils.getDataMatrixGeneralizedFromChipSeq(funSites);
//            
//            
///******************* Temporary: start!!!! *****************/
//            log.info(" ...... Outputs for getDataMatrixGeneralizedFromChipSeq() .....");
//            int k = 100;
//            log.info(" ...... Outputs : dataMatrix dm : only " + k + " rows .....");
//            DataMatrix.printDataMatrix(dm, 100);
///******************* Temporary: end !!!! *****************/
//
//            
//            
//            double[][] matrix = dm.getMatrix();
//            for( int jj = matrix[0].length - 1; jj >= 0; jj-- )
//            {
//                double[] x = UtilsGeneral.getDistinctValues(MatrixUtils.getColumn(matrix, jj));
//                if( x.length < 2 )
//                    dm.removeColumn(jj);
//            }
//            
//            // 5. Remove unavailable columns.
//            String[] names = dm.getColumnNames();
//            List<String> listStr = new ArrayList<>();
//            for( String s : columnNamesSelected )
//                if( ArrayUtils.indexOf(names, s) >= 0 )
//                    listStr.add(s);
//            String[] columnNamesNew = listStr.toArray(new String[0]);
//            boolean[] doSortInIncreasingOrderNew = new boolean[columnNamesNew.length];
//            for( int jj = 0; jj < columnNamesNew.length; jj++ )
//                doSortInIncreasingOrderNew[jj] = doSortInIncreasingOrder[ArrayUtils.indexOf(columnNamesSelected, columnNamesNew[jj])];
//            dm = dm.getSubDataMatrixColumnWise(columnNamesNew);
//            
//            
///******************* Temporary: start!!!! *****************/
//            log.info(" ...... Outputs for getCombinedSitesAndDataMatrixFromPeakCallers() .....");
//            int dim = 100;
//            log.info(" ...... Outputs : dataMatrix dm : only " + dim + " rows .....");
//            DataMatrix.printDataMatrix(dm, 100);
//            log.info(" ...... Outputs : doSortInIncreasingOrderNew .....");
//            for( int i = 0; i < doSortInIncreasingOrderNew.length; i++ )
//                log.info(" i = " + i + " doSortInIncreasingOrderNew[i] = " + Boolean.toString(doSortInIncreasingOrderNew[i]));
//            log.info(" ...... Outputs : funSites .....");
//            log.info(" dim(funSites) = " + funSites.length);
///******************* Temporary: end !!!! *****************/
//            
//            
//            return new Object[]{funSites, dm, doSortInIncreasingOrderNew};
//        }
//

/////////////////////////////////////////////// old        
        private Object[] getCombinedSitesAndDataMatrixFromPeakCallers(String trackName, boolean doIncludeCombinedFrequency)
        {
            // 1. Definition of columnNamesSelected and doSortInIncreasingOrder.
            String[] columnNamesSelected = doIncludeCombinedFrequency ? PEAK_CALLER_CHARACTERISTICS : (String[])ArrayUtils.add(PEAK_CALLER_CHARACTERISTICS, "Combined_frequency");
            boolean[] doSortInIncreasingOrder = doIncludeCombinedFrequency ? DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER : ArrayUtils.add(DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER, false);
        
            // 2. Create combined sites.
            CombinedSites combinedSites = new CombinedSites(combinedPeakType, pathToFolderWithFolders, foldersNames, trackName, minimalLengthOfPeaks, maximalLengthOfPeaks, true);
            FunSite[] funSites = combinedSites.getCombinedSites();
            if( funSites.length < siteNumberThreshold ) return null;
            
            // 3. Perform quality control.
            if( ! Double.isNaN(fpcmThreshold) )
            {
                int[] pivotalFrequencies = QualityControlSites.calculatePivotalFrequencies(funSites, foldersNames.length);
                double fpcm = PopulationSize.getFpcm(pivotalFrequencies[0], pivotalFrequencies[1], pivotalFrequencies[2]);
                if( fpcm > fpcmThreshold )
                    funSites = QualityControlSites.removeOrphans(funSites);
                if( funSites.length < siteNumberThreshold ) return null;
            }
            
            // 4. Remove degenerated columns.
            DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
            double[][] matrix = dm.getMatrix();
            for( int jj = matrix[0].length - 1; jj >= 0; jj-- )
            {
                double[] x = UtilsGeneral.getDistinctValues(MatrixUtils.getColumn(matrix, jj));
                if( x.length < 2 )
                    dm.removeColumn(jj);
            }
            
            // 5. Remove unavailable columns.
            String[] names = dm.getColumnNames();
            List<String> listStr = new ArrayList<>();
            for( String s : columnNamesSelected )
                if( ArrayUtils.indexOf(names, s) >= 0 )
                    listStr.add(s);
            String[] columnNamesNew = listStr.toArray(new String[0]);
            boolean[] doSortInIncreasingOrderNew = new boolean[columnNamesNew.length];
            for( int jj = 0; jj < columnNamesNew.length; jj++ )
                doSortInIncreasingOrderNew[jj] = doSortInIncreasingOrder[ArrayUtils.indexOf(columnNamesSelected, columnNamesNew[jj])];
            dm = dm.getSubDataMatrixColumnWise(columnNamesNew);
            return new Object[]{funSites, dm, doSortInIncreasingOrderNew};
        }
        
        
        // new : every FunSite contains DataMatrix
        // Input funSites must be sorted within each chromosome!!!
        private static FunSite[] mergeNearestRegions(FunSite[] funSites, double[] scores)
        {
            int maximalLength = 100, maximalDistance = 21;
            List<FunSite> result = new ArrayList<>(), listForMerging = new ArrayList<>();
            List<Double> scoresForMerging = new ArrayList<>();
            for( int i = 0; i < funSites.length; i++ )
            {
                if( listForMerging.isEmpty() )
                {
                    if( funSites[i].getLength() > maximalLength )
                        result.add(funSites[i]);
                    else
                        addFunSiteAndScore(funSites[i], scores[i], listForMerging, scoresForMerging);
                    continue;
                }
                
                FunSite fs = listForMerging.get(listForMerging.size() - 1);
                if( ! fs.getChromosomeName().equals(funSites[i].getChromosomeName()) || funSites[i].getLength() > maximalLength || funSites[i].getStartPosition() - fs.getFinishPosition() > maximalDistance )
                {
                    result.add(mergeAndClear(listForMerging, scoresForMerging));
                    if( funSites[i].getLength() > maximalLength )
                        result.add(funSites[i]);
                    else
                        addFunSiteAndScore(funSites[i], scores[i], listForMerging, scoresForMerging);
                }
                else
                    addFunSiteAndScore(funSites[i], scores[i], listForMerging, scoresForMerging);
            }
            if( ! listForMerging.isEmpty() )
                result.add(mergeAndClear(listForMerging, scoresForMerging));
            return result.toArray(new FunSite[0]);
        }
        
        
        
        private static void addFunSiteAndScore(FunSite funSite, double score, List<FunSite> funSites, List<Double> scores)
        {
            funSites.add(funSite);
            scores.add(score);
        }
        
        private static FunSite mergeAndClear(List<FunSite> listForMerging,  List<Double> scoresForMerging)
        {
            FunSite result = listForMerging.get(0);
            Set<String> set = result.getObjects() == null ? null : new HashSet<>();
            if( listForMerging.size() > 1 )
            {
                int index = (int)PrimitiveOperations.getMin(UtilsGeneral.fromListToArray(scoresForMerging))[0];
                FunSite fs = listForMerging.get(index);
                result = new FunSite(fs.getChromosomeName(), new Interval(result.getStartPosition(), listForMerging.get(listForMerging.size() - 1).getFinishPosition()), fs.getStrand(), fs.getDataMatrix());
                if( set != null )
                {
                    for( FunSite funSite : listForMerging )
                        for( String string : (String[])funSite.getObjects()[0] )
                            set.add(string);
                    result.setObjects(new Object[]{set.toArray(new String[0])});
                }
            }
            listForMerging.clear();
            scoresForMerging.clear();
            return result;
        }
        
//        public static String[] getAvailableOptions()
//        {
//            return new String[]{OPTION_01, OPTION_02, OPTION_03, OPTION_04};
//        }
    }
    /************************** CistromConstructor : end ******************************/
    
    /************************** CistromConstructorTemp : start ******************************/
    public static class CistromConstructorTemp
    {
        public static final String OPTION_01 = "Cistrom construction for given cell line";
        public static final String OPTION_02 = "Meta-clusters constriction (i.e. cistrom construction for every available TF-class)";
        public static final String OPTION_03 = "DNase meta-clusters constriction";

        public static final String OPTION_04 = "Selection of the most reliable meta-clusters (based on 2-component normal mixture of RA-scores)";
        public static final String OPTION_05 = "Calculation of site motif scores";
        public static final String OPTION_06 = "Assignment of probabilities to meta-clusters by classification model";

        public static final String NAME_OF_FOLDER_WITH_COMBINED_PEAKS = "Combined_peaks";
        
        // TODO: To create method for automatic calculation of PEAK_CALLER_CHARACTERISTICS and DO_SORT_IN_INCREASING_ORDER!
        public static final String[] PEAK_CALLER_CHARACTERISTICS = {"macs_fold_enrichment", "macs_-10*log10(pvalue)", "macs_FDR(%)", "macs_tags", "pics_score", "sissrs_p-value", "sissrs_fold", "sissrs_tags", "gem_Fold", "gem_P-lg10", "gem_Noise", "gem_P_poiss", "gem_Q_-lg10", "macs2_-log10(pvalue)", "macs2_-log10(qvalue)", "macs2_fold_enrichment", "macs2_pileup", "hotspot2_itemRGB"};
        public static final boolean[] DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER = {false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false};
        
        private String cistromType;
        private String combinedPeakType;
        private String rankAggregationMethod;
        private double fpcmThreshold;
        private int siteNumberThreshold;
        private DataElementPath pathToFolderWithFolders;
        private String[] foldersNames;
        private int minimalLengthOfPeaks;
        private int maximalLengthOfPeaks;
        private DataElementPath pathToOutputFolder;
        private DataElementPath pathToFolderWithCombinedPeaks;
        private Track trackWithSiteMotifs;
        private boolean doAddMotifsIntoFirstRaStep;
        private boolean doAddMotifsIntoSecondRaStep;
        private String tfClass;
        
        // It is constructor for cistrom construction for GTRD tracks (meta-clusters for every available TF-class or meta-clusters for given cell line or DNase meta-clusters).
        
//        public CistromConstructor(String cistromType, Species givenSpecie, String cellLine, String combinedPeakType, boolean doRemoveCellTreatments, String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithFolders, String[] foldersNames, int minimalLengthOfPeaks, int maximalLengthOfPeaks, DataElementPath pathToOutputFolder, AnalysisJobControl jobControl, int from, int to)
//        {
//        	CistromConstructor(cistromType, givenSpecie, cellLine, combinedPeakType, doRemoveCellTreatments, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder, null, false, jobControl, from, to);
//        }
        
        public CistromConstructorTemp(String cistromType, Species givenSpecie, String cellLine, String combinedPeakType, boolean doRemoveCellTreatments, String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithFolders, String[] foldersNames, int minimalLengthOfPeaks, int maximalLengthOfPeaks, DataElementPath pathToOutputFolder, Track trackWithSiteMotifs, String tfClass, boolean doAddMotifsIntoFirstRaStep, boolean doAddMotifsIntoSecondRaStep, AnalysisJobControl jobControl, int from, int to)
        {
            // 0. Initialize some Class fields.
            this.cistromType = cistromType;
            this.combinedPeakType = combinedPeakType;
            this.rankAggregationMethod = rankAggregationMethod;
            this.fpcmThreshold = fpcmThreshold;
            this.siteNumberThreshold = siteNumberThreshold;
            this.pathToFolderWithFolders = pathToFolderWithFolders;
            this.foldersNames = foldersNames;
            this.minimalLengthOfPeaks = minimalLengthOfPeaks;
            this.maximalLengthOfPeaks = maximalLengthOfPeaks;
            this.pathToOutputFolder = pathToOutputFolder;
            this.trackWithSiteMotifs = trackWithSiteMotifs;
            this.doAddMotifsIntoFirstRaStep = doAddMotifsIntoFirstRaStep;
            this.doAddMotifsIntoSecondRaStep = doAddMotifsIntoSecondRaStep;
            this.tfClass = tfClass;
            
            // 1. Identify  trackInfos and distinctObjects = {distinctTfClasses or distinctCellLines}.
            ///////TrackInfo[] trackInfos = TrackInfo.getTracksInfo(this.pathToFolderWithFolders, this.foldersNames, givenSpecie, null, cellLine, null);
             

            // TODO:
//            log.info("trackInfos.length = " + trackInfos.length);
//            for( int i = 0; i < trackInfos.length; i++ )
//            {
//                String specie = trackInfos[i].getSpecie(), cellLine_ = trackInfos[i].getCellLine();
//                log.info("i = " + i + " specie = " + specie + " cellLine_ = " + cellLine_);
//            }

//            if( this.cistromType.equals(OPTION_01) || this.cistromType.equals(OPTION_02) )
//                trackInfos = TrackInfo.removeTrackInfosWithoutTfClasses(trackInfos);
//            if( doRemoveCellTreatments )
//            {
//                for( TrackInfo ti : trackInfos )
//                    ti.replaceTreatment();
//                trackInfos = TrackInfo.removeTrackInfosWithCellTreatments(trackInfos);
//            }
            String[] distinctObjects = null;
            switch( this.cistromType )
            {
                case OPTION_01:
                case OPTION_02: //if( tfClass == null ) distinctObjects = TrackInfo.getDistinctTfClasses(trackInfos);
                				/**else***/  distinctObjects = new String[]{tfClass}; break;
                case OPTION_03: distinctObjects = null; //TrackInfo.getDistinctCellLines(trackInfos); break;
            }
            log.info("number of distinct object (for example, TF-classes or cell lines) = " + distinctObjects.length);
            
            // 2. Remove analyzed distinctTfClasses.
            if( pathToOutputFolder.exists() )
            {
                String[] fileNames = pathToOutputFolder.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
                if( fileNames.length > 0 )
                {
                    List<String> list = new ArrayList<>();
                    for( String s : distinctObjects )
                        if( ! ArrayUtils.contains(fileNames, s) )
                            list.add(s);
                    distinctObjects = list.toArray(new String[0]);
                }
            }
            
            // 3. Identify combined sites for distinct objects.
            pathToFolderWithCombinedPeaks = this.pathToOutputFolder.getChildPath(NAME_OF_FOLDER_WITH_COMBINED_PEAKS);
            DataCollectionUtils.createFoldersForPath(pathToFolderWithCombinedPeaks.getChildPath(""));
            int difference = to - from;
            for( int i = 0; i < distinctObjects.length; i++ )
            {
                if( jobControl != null )
                    jobControl.setPreparedness(from + (i + 1) * difference / distinctObjects.length);
                
                // 3.1. Calculate RA-scores for each trackName for peak callers.
                String[] trackNames = null;
                switch( this.cistromType )
                {
                    case OPTION_01:
                    case OPTION_02: //trackNames = TrackInfo.getTrackNames(trackInfos, distinctObjects[i], null, null); break;
                    	trackNames = new String[]{"PEAKS034449", "PEAKS041179"};
//                    case OPTION_03: trackNames = TrackInfo.getTrackNames(trackInfos, null, null, distinctObjects[i]); break;
                }
                log.info("*** i = " + i + " distinct object = " + distinctObjects[i] + " number of tracks = " + trackNames.length + " ***");
                trackNames = implementTwoStepRankAggregationForPeakCallers(trackNames, false);
                if( trackNames.length < 1 ) continue;
                
                // 3.2. Calculate combinedSites and RA-scores for given distinct object.
                log.info("*** Calculation of combined sites and RA-scores for given distinct object. ***");
                int size = implementRankAggregationForTracks(trackNames, distinctObjects[i]);
                TableAndFileUtils.addRowToTable(new double[]{(double)trackNames.length, (double)size}, null, distinctObjects[i], new String[]{"Number_of_combined_tracks", "Number_of_combined_sites"}, pathToOutputFolder, "summary");
            }
        }

        // TODO: Under construction !!!!!!!!!!!!!!!!
        // It is constructor for cistrom construction for single TF when names of tracks (obtained by single peak caller) are given.
//        public CistromConstructor(String combinedPeakType, String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithTracks, String[] tracksNames, String tfName, int minimalLengthOfPeaks, int maximalLengthOfPeaks, DataElementPath pathToOutputFolder, AnalysisJobControl jobControl, int from, int to)
//        {
//            // 0. Initialize some Class fields.
//            this.combinedPeakType = combinedPeakType;
//            this.rankAggregationMethod = rankAggregationMethod;
//            this.fpcmThreshold = fpcmThreshold;
//            this.siteNumberThreshold = siteNumberThreshold;
//            this.minimalLengthOfPeaks = minimalLengthOfPeaks;
//            this.maximalLengthOfPeaks = maximalLengthOfPeaks;
//            this.pathToOutputFolder = pathToOutputFolder;
//            
//            //log.info("number of distinct object (for example, TF-classes or cell lines) = " + distinctObjects.length);
//            
//            // 1. Identify combined sites for given tracks.
//            pathToFolderWithCombinedPeaks = this.pathToOutputFolder.getChildPath(NAME_OF_FOLDER_WITH_COMBINED_PEAKS);
//            DataCollectionUtils.createFoldersForPath(pathToFolderWithCombinedPeaks.getChildPath(""));
//            int difference = to - from;
//            for( int i = 0; i < tracksNames.length; i++ )
//            {
//                if( jobControl != null )
//                    jobControl.setPreparedness(from + (i + 1) * difference / tracksNames.length);
//                
//                // 1.1. Calculate RA-scores for each trackName for peak callers.
//                // log.info("*** i = " + i + " distinct object = " + distinctObjects[i] + " number of tracks = " + trackNames.length + " ***");
//                
//                // TODO: Stop here !!!!!!!
//                tracksNames = implementTwoStepRankAggregationForPeakCallers(tracksNames, false);
//                //if( tracksNames.length < 1 ) continue;
//                
//                // 1.2. Calculate combinedSites and RA-scores for given distinct object.
//                log.info("*** Calculation of combined sites and RA-scores for given distinct object. ***");
//                int size = implementRankAggregationForTracks(tracksNames, tfName);
//            }
//        }

        private int implementRankAggregationForTracks(String[] trackNames, String nameOfResultedTrack)
        {
            if( trackNames == null || trackNames.length < 1 ) return 0;
            if( trackNames.length == 1 )
            {
                Track track = pathToFolderWithCombinedPeaks.getChildPath(trackNames[0]).getDataElement(Track.class);
                Map<String, List<FunSite>> sites = FunSiteUtils.readSitesInTrack(track, 0, Integer.MAX_VALUE, new String[]{RankAggregation.RA_SCORE}, trackNames[0]);
                FunSite[] funSites = FunSiteUtils.transformToArray(sites);
                DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
                String[] names = dm.getColumnNames();
                String name = null;
                for( String str : names )
                    if( str.contains(RankAggregation.RA_SCORE) )
                        name = str;
                double[] scores = dm.getColumn(name);
                Object[] objects = new Object[]{funSites[0].getDistinctRowNames()};
                for( FunSite fs : funSites )
                    fs.setObjects(objects);
                FunSiteUtils.writeSitesToSqlTrack(funSites, RankAggregation.RA_SCORE, scores, pathToOutputFolder, nameOfResultedTrack);
                return funSites.length;
            }
            CombinedSites combinedSites = new CombinedSites(combinedPeakType, pathToFolderWithCombinedPeaks, trackNames, minimalLengthOfPeaks, maximalLengthOfPeaks, false);
            FunSite[] funSites = combinedSites.getCombinedSites();
            double[] scores = calculateRankAggregationScores(funSites);
            for( FunSite fs : funSites )
                fs.setObjects(new Object[]{fs.getDistinctRowNames()});

            // TODO: check mergeNearestRegions()
            funSites = mergeNearestRegions(funSites, scores);
            scores = calculateRankAggregationScores(funSites);
            FunSiteUtils.writeSitesToSqlTrack(funSites, RankAggregation.RA_SCORE, scores, pathToOutputFolder, nameOfResultedTrack);
            return funSites.length;
        }
        
        private double[] calculateRankAggregationScores(FunSite[] funSites)
        {
            DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
            String[] columnNames = dm.getColumnNames();
            for( int j = columnNames.length - 1; j >= 0; j-- )
                if( ! columnNames[j].contains(RankAggregation.RA_SCORE) )
                    dm.removeColumn(columnNames[j]);
            RankAggregation ra = new RankAggregation(dm, UtilsForArray.getConstantArray(dm.getColumnNames().length, true));
            return ra.getScoresTransformed(rankAggregationMethod, null, null, null);
        }

        private String[] implementTwoStepRankAggregationForPeakCallers(String[] trackNames, boolean doIncludeCombinedFrequency)
        {
            List<String> listOfNamesOfSavedTracks = new ArrayList<>();
            for( int j = 0; j < trackNames.length; j++ )
            {
                log.info(" j = " + j + " trackName = " + trackNames[j]);
                if( implementTwoStepRankAggregationForPeakCallers(trackNames[j], doIncludeCombinedFrequency) )
                    listOfNamesOfSavedTracks.add(trackNames[j]);
            }
            return listOfNamesOfSavedTracks.toArray(new String[0]);
        }
        
        private boolean implementTwoStepRankAggregationForPeakCallers(String trackName, boolean doIncludeCombinedFrequency)
        {
            // 1.1 Calculate ranks for whole data matrix.
            Object[] objects = getCombinedSitesAndDataMatrixFromPeakCallers(trackName, doIncludeCombinedFrequency);
            if( objects == null ) return false;
            FunSite[] funSites = (FunSite[])objects[0];
            DataMatrix dm = (DataMatrix)objects[1];
            if( dm.getSize() < siteNumberThreshold ) return false;
            
            // 1.2. 
            if ( doAddMotifsIntoFirstRaStep )
            {
            	double[] motifScores = new double[funSites.length];
            	String[] propertiesNames = new String[]{"score"};
            	double minScore = Double.MAX_VALUE, maxScore = 0.0;
            	DataCollection<Site> sites = trackWithSiteMotifs.getAllSites();
            	for( Site site : sites )
                	minScore = Math.min(minScore, SiteUtils.getProperties(site, propertiesNames)[0]);
                for( int i = 0; i < funSites.length; i++ )
                {
                	DataCollection<Site> dc = trackWithSiteMotifs.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
                	if( dc.isEmpty() )
                		motifScores[i] = minScore;
                	else
                    	for( Site sitesLocal : dc )
                    		motifScores[i] = Math.min(maxScore, SiteUtils.getProperties(sitesLocal, propertiesNames)[0]);
                }
                dm.addColumn("motif_score", motifScores, dm.getColumnNames().length);
            }

            // 1.3.
            boolean[] doSortInIncreasingOrder = (boolean[])objects[2];
            doSortInIncreasingOrder = ArrayUtils.add(doSortInIncreasingOrder, false); 
            
            RankAggregation ra = new RankAggregation(dm, doSortInIncreasingOrder);
            dm = ra.getRanks();

            // 2. Calculate dataMatrix with RA-scores at 1-st step.
            DataMatrix dataMatrix = null;
            String[] rowNames = dm.getRowNames(), columnNames = dm.getColumnNames(), headers = new String[columnNames.length];
            for( int j = 0; j < headers.length; j++ )
                headers[j] = columnNames[j].split("_")[0];
            for( String folderName : foldersNames )
            {
                List<String> list = new ArrayList<>();
                for( int j = 0; j < headers.length; j++ )
                    if( folderName.equals(headers[j]) )
                        list.add(columnNames[j]);
                if( list.size() < 1 ) continue;
                DataMatrix subRanks = dm.getSubDataMatrixColumnWise(list.toArray(new String[0]));
                if( subRanks.getColumnNames().length == 1 )
                {
                    double[] scores = subRanks.getColumn(subRanks.getColumnNames()[0]);
                    DataMatrix dmNew = new DataMatrix(subRanks.getRowNames(), folderName + "_" + RankAggregation.RA_SCORE, scores);
                    dataMatrix = dataMatrix == null ? dmNew : DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dataMatrix, dmNew});
                    continue;
                }
                ra = new RankAggregation(subRanks);
                double[] scores = ra.getScores(rankAggregationMethod, null, null, null);
                DataMatrix dmNew = new DataMatrix(rowNames, folderName + "_" + RankAggregation.RA_SCORE, scores);
                dataMatrix = dataMatrix == null ? dmNew : DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dataMatrix, dmNew});
            }
            
            // 3. Calculate RA-scores at 2-nd step.
            
            if ( doAddMotifsIntoSecondRaStep )
            {
            	double[] motifScores = new double[funSites.length];
            	String[] propertiesNames = new String[]{"score"};
            	double minScore = Double.MAX_VALUE, maxScore = 0.0;
            	DataCollection<Site> sites = trackWithSiteMotifs.getAllSites();
            	for( Site site : sites )
                	minScore = Math.min(minScore, SiteUtils.getProperties(site, propertiesNames)[0]);
                for( int i = 0; i < funSites.length; i++ )
                {
                	DataCollection<Site> dc = trackWithSiteMotifs.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
                	if( dc.isEmpty() )
                		motifScores[i] = minScore;
                	else
                    	for( Site sitesLocal : dc )
                    		motifScores[i] = Math.min(maxScore, SiteUtils.getProperties(sitesLocal, propertiesNames)[0]);
                }
                dm.addColumn("motif_score", motifScores, dm.getColumnNames().length);
            }
                
            ra = new RankAggregation(dataMatrix, UtilsForArray.getConstantArray(dataMatrix.getColumnNames().length, true));
            double[] scores = ra.getScoresTransformed(rankAggregationMethod, null, null, null);
            FunSiteUtils.writeSitesToSqlTrack(funSites, RankAggregation.RA_SCORE, scores, pathToFolderWithCombinedPeaks, trackName);
            return true;
        }

        private Object[] getCombinedSitesAndDataMatrixFromPeakCallers(String trackName, boolean doIncludeCombinedFrequency)
        {
            // 1. Definition of columnNamesSelected and doSortInIncreasingOrder.
            String[] columnNamesSelected = doIncludeCombinedFrequency ? PEAK_CALLER_CHARACTERISTICS : (String[])ArrayUtils.add(PEAK_CALLER_CHARACTERISTICS, "Combined_frequency");
            boolean[] doSortInIncreasingOrder = doIncludeCombinedFrequency ? DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER : ArrayUtils.add(DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER, false);
        
            // 2. Create combined sites.
            CombinedSites combinedSites = new CombinedSites(combinedPeakType, pathToFolderWithFolders, foldersNames, trackName, minimalLengthOfPeaks, maximalLengthOfPeaks, true);
            FunSite[] funSites = combinedSites.getCombinedSites();
            if( funSites.length < siteNumberThreshold ) return null;
            
            // 3. Perform quality control.
            if( ! Double.isNaN(fpcmThreshold) )
            {
                int[] pivotalFrequencies = QualityControlSites.calculatePivotalFrequencies(funSites, foldersNames.length);
                double fpcm = PopulationSize.getFpcm(pivotalFrequencies[0], pivotalFrequencies[1], pivotalFrequencies[2]);
                if( fpcm > fpcmThreshold )
                    funSites = QualityControlSites.removeOrphans(funSites);
                if( funSites.length < siteNumberThreshold ) return null;
            }
            
            // 4. Remove degenerated columns.
            DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
            double[][] matrix = dm.getMatrix();
            for( int jj = matrix[0].length - 1; jj >= 0; jj-- )
            {
                double[] x = UtilsGeneral.getDistinctValues(MatrixUtils.getColumn(matrix, jj));
                if( x.length < 2 )
                    dm.removeColumn(jj);
            }
            
            // 5. Remove unavailable columns.
            String[] names = dm.getColumnNames();
            List<String> listStr = new ArrayList<>();
            for( String s : columnNamesSelected )
                if( ArrayUtils.indexOf(names, s) >= 0 )
                    listStr.add(s);
            String[] columnNamesNew = listStr.toArray(new String[0]);
            boolean[] doSortInIncreasingOrderNew = new boolean[columnNamesNew.length];
            for( int jj = 0; jj < columnNamesNew.length; jj++ )
                doSortInIncreasingOrderNew[jj] = doSortInIncreasingOrder[ArrayUtils.indexOf(columnNamesSelected, columnNamesNew[jj])];
            dm = dm.getSubDataMatrixColumnWise(columnNamesNew);
            return new Object[]{funSites, dm, doSortInIncreasingOrderNew};
        }
        
        // new : every FunSite contains DataMatrix
        // Input funSites must be sorted within each chromosome!!!
        private static FunSite[] mergeNearestRegions(FunSite[] funSites, double[] scores)
        {
            int maximalLength = 100, maximalDistance = 21;
            List<FunSite> result = new ArrayList<>(), listForMerging = new ArrayList<>();
            List<Double> scoresForMerging = new ArrayList<>();
            for( int i = 0; i < funSites.length; i++ )
            {
                if( listForMerging.isEmpty() )
                {
                    if( funSites[i].getLength() > maximalLength )
                        result.add(funSites[i]);
                    else
                        addFunSiteAndScore(funSites[i], scores[i], listForMerging, scoresForMerging);
                    continue;
                }
                
                FunSite fs = listForMerging.get(listForMerging.size() - 1);
                if( ! fs.getChromosomeName().equals(funSites[i].getChromosomeName()) || funSites[i].getLength() > maximalLength || funSites[i].getStartPosition() - fs.getFinishPosition() > maximalDistance )
                {
                    result.add(mergeAndClear(listForMerging, scoresForMerging));
                    if( funSites[i].getLength() > maximalLength )
                        result.add(funSites[i]);
                    else
                        addFunSiteAndScore(funSites[i], scores[i], listForMerging, scoresForMerging);
                }
                else
                    addFunSiteAndScore(funSites[i], scores[i], listForMerging, scoresForMerging);
            }
            if( ! listForMerging.isEmpty() )
                result.add(mergeAndClear(listForMerging, scoresForMerging));
            return result.toArray(new FunSite[0]);
        }
        
        private static void addFunSiteAndScore(FunSite funSite, double score, List<FunSite> funSites, List<Double> scores)
        {
            funSites.add(funSite);
            scores.add(score);
        }
        
        private static FunSite mergeAndClear(List<FunSite> listForMerging,  List<Double> scoresForMerging)
        {
            FunSite result = listForMerging.get(0);
            Set<String> set = result.getObjects() == null ? null : new HashSet<>();
            if( listForMerging.size() > 1 )
            {
                int index = (int)PrimitiveOperations.getMin(UtilsGeneral.fromListToArray(scoresForMerging))[0];
                FunSite fs = listForMerging.get(index);
                result = new FunSite(fs.getChromosomeName(), new Interval(result.getStartPosition(), listForMerging.get(listForMerging.size() - 1).getFinishPosition()), fs.getStrand(), fs.getDataMatrix());
                if( set != null )
                {
                    for( FunSite funSite : listForMerging )
                        for( String string : (String[])funSite.getObjects()[0] )
                            set.add(string);
                    result.setObjects(new Object[]{set.toArray(new String[0])});
                }
            }
            listForMerging.clear();
            scoresForMerging.clear();
            return result;
        }
        
//        public static String[] getAvailableOptions()
//        {
//            return new String[]{OPTION_01, OPTION_02, OPTION_03, OPTION_04};
//        }
    }
    /************************** CistromConstructorTemp : end ******************************/
    
    
    /************************** CistromConstructorExploratory : start *****************/
    // It contains exploratory methods for definition of threshold for rank aggregation scores.
    public static class CistromConstructorExploratory
    {
        // TODO: To create method to calculate PEAK_CALLER_CHARACTERISTICS and DO_SORT_IN_INCREASING_ORDER automatically!
//        public static final String[] PEAK_CALLER_CHARACTERISTICS = {"macs_fold_enrichment", "macs_-10*log10(pvalue)", "macs_FDR(%)", "macs_tags", "pics_score", "sissrs_p-value", "sissrs_fold", "sissrs_tags", "gem_Fold", "gem_P-lg10", "gem_Noise", "gem_P_poiss", "gem_Q_-lg10", "macs2_-log10(pvalue)", "macs2_-log10(qvalue)", "macs2_fold_enrichment", "macs2_pileup"};
//        public static final boolean[] DO_SORT_IN_INCREASING_ORDER = {false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false};
//        
//        public static Object[] getCombinedSitesAndDataMatrixFromPeakCallers(String combinedSiteType, DataElementPath pathToFolderWithFolders, String[] foldersNames, String trackName, int minimalLengthOfSite, int maximalLengthOfSite, boolean doIncludeCombinedFrequency, String methodName)
//        {
//            // 1. Definition of columnNamesSelected and doSortInIncreasingOrder.
//            String[] columnNamesSelected = doIncludeCombinedFrequency ? PEAK_CALLER_CHARACTERISTICS : (String[])ArrayUtils.add(PEAK_CALLER_CHARACTERISTICS, "Combined_frequency");
//            boolean[] doSortInIncreasingOrder = doIncludeCombinedFrequency ? DO_SORT_IN_INCREASING_ORDER : ArrayUtils.add(DO_SORT_IN_INCREASING_ORDER, false);
//        
//            // 2. Create combined sites.
//            CombinedSites combinedSites = new CombinedSites(combinedSiteType, pathToFolderWithFolders, foldersNames, trackName, minimalLengthOfSite, maximalLengthOfSite, true);
//            FunSite[] funSites = combinedSites.getCombinedSites();
//            if( funSites.length <= 10 ) return null;
//            
//            // 3. Remove degenerated columns.
//            DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
//            double[][] matrix = dm.getMatrix();
//            for( int jj = matrix[0].length - 1; jj >= 0; jj-- )
//            {
//                double[] x = UtilsGeneral.getDistinctValues(MatrixUtils.getColumn(matrix, jj));
//                if( x.length < 2 )
//                    dm.removeColumn(jj);
//            }
//            
//            // 4. Remove unavailable columns.
//            String[] names = dm.getColumnNames();
//            List<String> listStr = new ArrayList<>();
//            for( String s : columnNamesSelected )
//                if( ArrayUtils.indexOf(names, s) >= 0 )
//                    listStr.add(s);
//            String[] columnNamesNew = listStr.toArray(new String[0]);
//            boolean[] doSortInIncreasingOrderNew = new boolean[columnNamesNew.length];
//            for( int jj = 0; jj < columnNamesNew.length; jj++ )
//                doSortInIncreasingOrderNew[jj] = doSortInIncreasingOrder[ArrayUtils.indexOf(columnNamesSelected, columnNamesNew[jj])];
//            dm = dm.getSubDataMatrixColumnWise(columnNamesNew);
//            return new Object[]{funSites, dm, doSortInIncreasingOrderNew};
//        }
        
        // Filtration of sites by normal mixture.
//        private static Object[] implementSiteFiltration(FunSite[] funSites, double[] scores)
//        {
//            NormalMixture normalMixture = new NormalMixture(scores, 2, null,null, 300);
//            DataMatrix dm = normalMixture.getParametersOfComponents();
//            double[] means = dm.getColumn("Mean value"), sigmas = dm.getColumn("Sigma");
//            if( means.length < 2 ) return new Object[]{funSites, scores};
//            int index = (int)PrimitiveOperations.getMax(means)[0];
//            double threshold = means[index] - 3.0 * sigmas[index];
//            List<FunSite> funSitesNew = new ArrayList<>();
//            List<Double> scoresNew = new ArrayList<>();
//            for( int i = 0; i < scores.length; i++ )
//                if( scores[i] <= threshold )
//                {
//                    funSitesNew.add(funSites[i]);
//                    scoresNew.add(scores[i]);
//                }
//            return new Object[]{funSitesNew.toArray(new FunSite[0]), UtilsGeneral.fromListToArray(scoresNew)};
//        }

        // TODO: To change and to move to appropriate Class.
        public static void selectBestSites(DataElementPath pathToFolderWithTracks, String inputTrackName, int numberOfBestSites, DataElementPath pathToOutputFolder, String outputTrackName)
        {
            // 1. Read RA-scores and calculate threshold.
            Track track = pathToFolderWithTracks.getChildPath(inputTrackName).getDataElement(Track.class);
            DataCollection<Site> sites = track.getAllSites();
            int n = sites.getSize(), i = 0;
            if( n <= numberOfBestSites ) return;
            String[] propertiesNames = new String[]{RankAggregation.RA_SCORE};
            double[] scores = new double[n];
            for( Site site : sites )
                scores[i++] = SiteUtils.getProperties(site, propertiesNames)[0];
            UtilsForArray.sortInAscendingOrder(scores);
            double threshold = scores[numberOfBestSites - 1];
            
            // 2. Select best sites and write them into output track.
            SqlTrack outputTrack = SqlTrack.createTrack(pathToOutputFolder.getChildPath(outputTrackName), null);
            for( Site site : sites )
                if( SiteUtils.getProperties(site, propertiesNames)[0] <= threshold )
                    outputTrack.addSite(site);
            outputTrack.finalizeAddition();
            CollectionFactoryUtils.save(track);
        }
        
        private static void treatRankAggregationScores(String rankAggregationMethodName, RankAggregation ra, double[] scores, DataElementPath pathToOutputFolder, String subName)
        {
            double[] scoresRandom = ra.getRandomScoresTransformed(rankAggregationMethodName, 1, null, null, null);
            UtilsForArray.sortInAscendingOrder(scoresRandom);
            DataMatrix dm = ra.getRanks();
            double[][] matrix = MatrixUtils.getProductOfMatrixAndScalar(dm.getMatrix(), 1.0 / (double)dm.getSize());
            dm = new DataMatrix(dm.getRowNames(), dm.getColumnNames(), matrix);
            treatRankAggregationScores(dm, scores, scoresRandom, pathToOutputFolder, subName);
        }
        
        private static void treatRankAggregationScores(DataMatrix dataMatrix, double[] scores, double[] scoresRandom, DataElementPath pathToOutputFolder, String subName)
        {
            Chart chart = DensityEstimation.createChartWithSmoothedDensities(new double[][]{scores, scoresRandom}, new String[]{RankAggregation.RA_SCORE, "Random score"}, "Score", true, null, DensityEstimation.WINDOW_WIDTH_01, null);
            TableAndFileUtils.addChartToTable("chart with RA-scores", chart, pathToOutputFolder.getChildPath(subName + "_chart_RA_scores"));
            NormalMixture normalMixture = new NormalMixture(scores, 2, null,null, 300);
            DataMatrix dm = normalMixture.getParametersOfComponents();
            dm.writeDataMatrix(false, pathToOutputFolder, subName + "_mixture_parameters", log);
            chart = normalMixture.createChartWithDensities(RankAggregation.RA_SCORE);
            TableAndFileUtils.addChartToTable("chart with RA-scores", chart, pathToOutputFolder.getChildPath(subName + "_chart_mixture"));
            if( scoresRandom.length > 100 )
            {
                UtilsForArray.sortInAscendingOrder(scoresRandom);
                dm = estimatePvalues(scores, scoresRandom);
                dm.writeDataMatrix(false, pathToOutputFolder, subName + "_pvalues", log);
                dm = DivideSampleByClassification.thresholdDetermination(dataMatrix, scores);
                dm.writeDataMatrix(false, pathToOutputFolder, subName + "_scores_divisions", log);
            }
        }
        
        // TODO: To move to appropriate Class???
        // scoresRandom must be sorted!
        private static DataMatrix estimatePvalues(double[] scores, double[] scoresRandom)
        {
            double pvalue = 0.1;
            List<double[]> matrix = new ArrayList<>();
            List<String> rowNames = new ArrayList<>();
            for( int i = 0; i < 5; i++ )
            {
                pvalue *= 0.1;
                int k = (int)((double)scoresRandom.length * pvalue + 0.5);
                if( k <= 0 ) break;
                int n = PrimitiveOperations.countSmallValues(scores, scoresRandom[k]);
                matrix.add(new double[]{pvalue, scoresRandom[k], 100.0 * (double)n / (double)scores.length});
                rowNames.add(Integer.toString(i));
            }
            return new DataMatrix(rowNames.toArray(new String[0]), new String[]{"p-value", "score", "percentage_of_less_scores"}, matrix.toArray(new double[matrix.size()][]));
        }
        
    // Map<String, List<FunSite>> map = FunSiteUtils.transformToMap(funSites);
    // ListUtil.sortAll(map);
        
        // new : every FunSite contains DataMatrix
        // Input funSites must be sorted within each chromosome!!!
    }
    /************************** CistromConstructorExploratory : end *****************/
    
    static Logger log = Logger.getLogger(CistromUtils.class.getName());
}
