package biouml.plugins.gtrd.utils;

// 04.04.22

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.gtrd.utils.FunSiteUtils.CombinedSites;
import biouml.plugins.gtrd.utils.FunSiteUtils.QualityControlSites;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.StatUtils.PopulationSize;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.standard.type.Species;

public class MetaClusterConsrtruction
{
	// 01.04.22
    public static final String OPTION_01 = "Meta-clusters construction (METARA is used for all TFs available for given cell line)";
    public static final String OPTION_02 = "Meta-clusters construction (METARA is used for all TFs available)";
    public static final String OPTION_03 = "DNase meta-clusters constriction";
    public static final String NAME_OF_FOLDER_WITH_COMBINED_PEAKS = "Combined_peaks";

    // TODO: To create method for automatic calculation of PEAK_CALLER_CHARACTERISTICS and DO_SORT_IN_INCREASING_ORDER!
    public static final String[] PEAK_CALLER_CHARACTERISTICS = {"macs_fold_enrichment", "macs_-10*log10(pvalue)", "macs_FDR(%)", "macs_tags", "pics_score", "sissrs_p-value", "sissrs_fold", "sissrs_tags", "gem_Fold", "gem_P-lg10", "gem_Noise", "gem_P_poiss", "gem_Q_-lg10", "macs2_-log10(pvalue)", "macs2_-log10(qvalue)", "macs2_fold_enrichment", "macs2_pileup", "hotspot2_itemRGB"};
    public static final boolean[] DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER = {false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false};


	// 01.04.22
    public static class Metara
    {
    	private String cistromType = OPTION_02;
    	private Species givenSpecie = transformToGtrdSpecies("Homo sapiens");
    	private String cellLine = null;
    	private String combinedPeakType = CombinedSites.SITE_TYPE_OVERLAPPED;
    	
    	// 03.04.22
    	// private boolean doRemoveCellTreatments = false;
    	private String cellLineStatus = TrackInfo.CELL_LINE_STATUS_ALL;
    	
    	private String rankAggregationMethod = RankAggregation.METHOD_AR_MEAN;
    	private double fpcmThreshold = 3.0;
    	private int siteNumberThreshold = 2000;
    	private DataElementPath pathToFolderWithFolders;
    	private String[] foldersNames = new String[]{"gem", "macs2", "pics", "sissrs"};
    	private int minimalLengthOfPeaks = 30;
    	private int maximalLengthOfPeaks = 1000000;
    	private String tfClassificationType;
    	private String tfClassOrUniprotId = null;
    	private Track trackWithSiteMotifs = null;
    	private boolean doAddMotifsIntoFirstRaStep = false;
    	private boolean doAddMotifsIntoSecondRaStep = false;
    	private DataElementPath pathToOutputFolder;
    	private DataElementPath pathToFolderWithCombinedPeaks;

    	// 01.04.22
    	public Metara(String cistromType, Species givenSpecie, String cellLine, String cellLineStatus, String combinedPeakType,
    			String rankAggregationMethod, double fpcmThreshold, int siteNumberThreshold, DataElementPath pathToFolderWithFolders,
    			String[] foldersNames, int minimalLengthOfPeaks, int maximalLengthOfPeaks, String tfClassificationType, String tfClassOrUniprotId,
    			Track trackWithSiteMotifs, boolean doAddMotifsIntoFirstRaStep, boolean doAddMotifsIntoSecondRaStep,
    			DataElementPath pathToOutputFolder, AnalysisJobControl jobControl, int from, int to)
        {
            this.cistromType = cistromType;
            this.combinedPeakType = combinedPeakType;
            this.cellLine = cellLine;
            this.cellLineStatus = cellLineStatus;
            this.rankAggregationMethod = rankAggregationMethod;
            this.fpcmThreshold = fpcmThreshold;
            this.siteNumberThreshold = siteNumberThreshold;
            this.pathToFolderWithFolders = pathToFolderWithFolders;
            this.foldersNames = foldersNames;
            this.minimalLengthOfPeaks = minimalLengthOfPeaks;
            this.maximalLengthOfPeaks = maximalLengthOfPeaks;
            this.tfClassificationType = tfClassificationType;
            this.tfClassOrUniprotId = tfClassOrUniprotId;
            this.trackWithSiteMotifs = trackWithSiteMotifs;
            this.doAddMotifsIntoFirstRaStep = doAddMotifsIntoFirstRaStep;
            this.doAddMotifsIntoSecondRaStep = doAddMotifsIntoSecondRaStep;
            this.pathToOutputFolder = pathToOutputFolder;
        	this.pathToFolderWithCombinedPeaks = this.pathToOutputFolder.getChildPath(NAME_OF_FOLDER_WITH_COMBINED_PEAKS);

            // 1. Identify  trackInfos.
            TrackInfo[] trackInfos = TrackInfo.getTracksInfo(this.pathToFolderWithFolders, this.foldersNames, givenSpecie, null, this.cellLine, null);
            
            // 05.04.22
            if( this.cistromType.equals(OPTION_01) || this.cistromType.equals(OPTION_02) )
            {
            	if( tfClassificationType.equals(EnsemblUtils.TF_CLASSIFICATION_TYPE_TF_CLASS) )
            		trackInfos = TrackInfo.removeTrackInfosWithoutTfClasses(trackInfos);
            	trackInfos = TrackInfo.selectTrackInfosWithGivenCellTreatmentStatus(trackInfos, this.cellLineStatus);
            }

            // TODO: temporary
            log.info("trackInfos.length = " + trackInfos.length);
            for( int i = 0; i < trackInfos.length; i++ )
            {
            	//03.04.22
                String specie = trackInfos[i].getSpecie(), cellLine_ = trackInfos[i].getCellLine(), treatment = trackInfos[i].getTreatment(), tfClass_ = trackInfos[i].getTfClass(), uniprotID_ = trackInfos[i].getUniprotId();
                log.info("i = " + i + " specie = " + specie + " cellLine_ = " + cellLine_ + " treatment = " + "<" + treatment + ">" + " isCellLineTreated = " + trackInfos[i].isCellLineTreated() + " tfClass_ = " + tfClass_ + " uniprotID_ = " + uniprotID_);
            }
            
            // 2. Identify distinctObjects = {distinct TfClassOrUniprotIds or distinct CellLines}.
            String[] distinctObjects = null;
            // 04.04.22
            switch( this.cistromType )
            {
                case OPTION_01:
                case OPTION_02: if( this.tfClassOrUniprotId == null ) distinctObjects = TrackInfo.getDistinctTfClassesOrUniprotIds(trackInfos, this.tfClassificationType);
                				else distinctObjects = new String[]{this.tfClassOrUniprotId}; break;
                case OPTION_03: distinctObjects = TrackInfo.getDistinctCellLines(trackInfos); break;
            }
            
            // TODO" temp
            log.info("number of distinct objects (for example, TfClassOrUniprotIds or cell lines) = " + distinctObjects.length);
            
            // 3. Remove analyzed distinctTfClasses.
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
            
            // 4. Identify combined sites for distinct objects where distinctObjects = {distinctTfClasses or distinctCellLines}.
            DataCollectionUtils.createFoldersForPath(pathToFolderWithCombinedPeaks.getChildPath(""));
            int difference = to - from;
            for( int i = 0; i < distinctObjects.length; i++ )
            {
                if( jobControl != null )
                    jobControl.setPreparedness(from + (i + 1) * difference / distinctObjects.length);
                
                // 4.1. Calculate RA-scores for each trackName for peak callers.
                String[] trackNames = null;
                switch( this.cistromType )
                {
                    case OPTION_01 :

                    // 05.04.22
                    // case OPTION_02 : trackNames = TrackInfo.getTrackNames(trackInfos, distinctObjects[i], null, null); break;
                    case OPTION_02 : trackNames = TrackInfo.getTrackNames(trackInfos, tfClassificationType, distinctObjects[i], null, null); break;
                    
                    // 01.04.22 Under construction
                    // case OPTION_03 : trackNames = TrackInfo.getTrackNames(trackInfos, null, null, distinctObjects[i]); break;
                    case OPTION_03 : trackNames = null; break;
                }
                
                log.info("*** i = " + i + " distinct object = " + distinctObjects[i] + " number of tracks = " + trackNames.length + " ***");
                log.info("*** trackNames : ");
                for( int ii = 0; ii < trackNames.length; ii++ )
                    log.info("*** ii = " + ii + " trackNames[ii] = " + trackNames[ii]);
                
                trackNames = implementTwoStepRankAggregationForPeakCallers(trackNames, false);
                if( trackNames.length < 1 ) continue;
                
                // 4.2. Calculate combinedSites and RA-scores for given distinct object.
                log.info("*** Calculation of combined sites and RA-scores for given distinct object. ***");
                int size = implementRankAggregationForTracks(trackNames, distinctObjects[i]);
                TableAndFileUtils.addRowToTable(new double[]{(double)trackNames.length, (double)size}, null, distinctObjects[i], new String[]{"Number_of_combined_tracks", "Number_of_combined_sites"}, pathToOutputFolder, "summary");
            }
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
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// TODO: temp!!!
	// TODO: jobControl, from, to  - to remove or to use this input parameters.
//	CistromConstructor cc1 = new CistromConstructor(cistromType, givenSpecie, cellLine, combinedPeakType,
//			doRemoveCellTreatments, rankAggregationMethod, fpcmThreshold, siteNumberThreshold,
//    		pathToFolderWithFolders, foldersNames, minimalLengthOfPeaks, maximalLengthOfPeaks,
//    		pathToOutputFolder, trackWithSiteMotifs, tfClass, doAddMotifsIntoFirstRaStep,
//    		doAddMotifsIntoSecondRaStep, jobControl, 0, 100);

    
	// several tracks for treatment of ESR1 by METARA.
	private DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD_20.06/Data/peaks");
	private DataElementPath pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment");
	String[] trackNames = new String[]{"PEAKS033070", "PEAKS033084", "PEAKS033093"};
	String nameOfResultedTrack = "Meta_clusters_for_all_peaks";

    /********* Move to GTRD utils? ******/
    // from ExploratoryAnalysisUtil.ThirdArticleOnCistrom
    // Example speciesInLatin = "Homo sapiens";
    public static Species transformToGtrdSpecies(String speciesInLatin, DataElementPath pathToGtrdSpecies)
    {
        Species givenSpecie = null;
        for( Species species : pathToGtrdSpecies.getDataCollection(Species.class) )
            if( species.getLatinName().equals(speciesInLatin) )
            	return species;
        return givenSpecie;
    }

    // from ExploratoryAnalysisUtil.ThirdArticleOnCistrom
    /********* Move to GTRD utils? ******/
    // Example speciesInLatin = "Homo sapiens";
    public static Species transformToGtrdSpecies(String speciesInLatin)
    {
       return transformToGtrdSpecies(speciesInLatin, DataElementPath.create("databases/Utils/Species"));
    }

    static Logger log = Logger.getLogger(MetaClusterConsrtruction.class.getName());
}
