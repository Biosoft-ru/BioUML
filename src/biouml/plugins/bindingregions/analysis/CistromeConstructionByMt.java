package biouml.plugins.bindingregions.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.FunSiteUtils;
import biouml.plugins.gtrd.utils.FunSiteUtils.CombinedSites;
import biouml.plugins.gtrd.utils.FunSiteUtils.QualityControlSites;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.PopulationSize;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.standard.type.Species;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

public class CistromeConstructionByMt extends AnalysisMethodSupport<CistromeConstructionByMt.CistromeConstructionByMtParameters>
{
	public static final String[] PEAK_CALLER_CHARACTERISTICS = {"macs_fold_enrichment", "macs_-10*log10(pvalue)", "macs_FDR(%)", "macs_tags", "pics_score", "sissrs_p-value", "sissrs_fold", "sissrs_tags", "gem_Fold", "gem_P-lg10", "gem_Noise", "gem_P_poiss", "gem_Q_-lg10", "macs2_-log10(pvalue)", "macs2_-log10(qvalue)", "macs2_fold_enrichment", "macs2_pileup", "hotspot2_itemRGB"};
	public static final boolean[] DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER = {false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false};

	private String combinedPeakType = CombinedSites.SITE_TYPE_OVERLAPPED;
	private String rankAggregationMethod = RankAggregation.METHOD_AR_MEAN;
	String[] peakcallers = new String[] {"macs2", "gem", "pics", "sissrs"};
	
	private DataElementPath pathToMasterTrack;
	private DataElementPath pathToOutputDir;
	private int siteNumberThreshold;
    private int minimalLengthOfPeaks;
    private int maximalLengthOfPeaks;
    private boolean doPerformQualityControl;
    private double fpcmThreshold;
    private DataElementPath chipSeqPeaks;
    private DataElementPath pathToDirWithCombinedPeaks;
    private Species givenSpecies;
    private Map<String, Integer> chromSizes;
    
	public CistromeConstructionByMt(DataCollection<?> origin, String name)
	{
		super(origin, name, new CistromeConstructionByMtParameters());
	}

	@Override
	public DataCollection<?> justAnalyzeAndPut() throws Exception
	{
		this.pathToOutputDir = parameters.getPathToOutputDir();
		this.pathToDirWithCombinedPeaks = this.pathToOutputDir.getChildPath("Combined_peaks");
		this.siteNumberThreshold = parameters.getSiteNumberThreshold();
		this.minimalLengthOfPeaks = parameters.getMinimalLengthOfPeaks();
		this.maximalLengthOfPeaks = parameters.getMaximalLengthOfPeaks();
		this.doPerformQualityControl = parameters.getDoPerformQualityControl();
		this.fpcmThreshold = doPerformQualityControl ? parameters.getFpcmThreshold() : Double.NaN;;
		this.chipSeqPeaks = parameters.getChipSeqPeaks();		
		this.pathToMasterTrack = parameters.getPathToMasterTrack();
		
		MasterTrack masterTrack = pathToMasterTrack.getDataElement( MasterTrack.class );
		this.givenSpecies = parameters.getSpecies();
		this.chromSizes = masterTrack.getChromSizes();

		// Calculate RA-scores for each trackName for peak callers.
		Metadata metadata = masterTrack.getMetadata();
		List<ChIPseqExperiment> chipExps = new ArrayList<ChIPseqExperiment>(metadata.chipSeqExperiments.values());
		String tfUniprotId = chipExps.get(0).getTfUniprotId();
		String[] trackNames = implementTwoStepRankAggregationForPeakCallers(chipExps, true);
		if( trackNames.length < 1 ) 
			return null;

		// Calculate combinedSites and RA-scores for given distinct object.
		log.info("*** Calculation of combined sites and RA-scores for given distinct object. ***");
		int size = implementRankAggregationForTracks(trackNames, tfUniprotId);
		TableAndFileUtils.addRowToTable(new double[]{(double)trackNames.length, (double)size}, null, tfUniprotId, new String[]{"Number_of_combined_tracks", "Number_of_combined_sites"}, pathToOutputDir, "summary");

		return null;
	}

	private String[] implementTwoStepRankAggregationForPeakCallers(List<ChIPseqExperiment> ChIPseqExps, boolean doIncludeCombinedFrequency) throws Exception
	{
		List<String> listOfNamesOfSavedTracks = new ArrayList<>();
		int counter = 0;
		for( ChIPseqExperiment exp : ChIPseqExps )
		{
			log.info(" counter = " + counter++ + " ChIPseq experiment = " + exp.getName() );
			if(exp.isControlExperiment())
			{
				log.info("Skipping " + " ChIPseq CONTROL experiment: " + exp.getName() );
				continue;
			}
			if( implementTwoStepRankAggregationForPeakCallers(exp, doIncludeCombinedFrequency) )
				listOfNamesOfSavedTracks.add( exp.getPeakId() );
		}
		return listOfNamesOfSavedTracks.toArray(new String[0]);
	}

	private boolean implementTwoStepRankAggregationForPeakCallers(ChIPseqExperiment exp, boolean doIncludeCombinedFrequency) throws Exception
	{
		// 1. Calculate ranks for whole data matrix.
		Object[] objects = getCombinedSitesAndDataMatrixFromChIPseqExp(exp, doIncludeCombinedFrequency);
		if( objects == null ) return false;
		FunSite[] funSites = (FunSite[])objects[0];
		DataMatrix dm = (DataMatrix)objects[1];
		if( dm.getSize() < parameters.siteNumberThreshold ) return false;
		boolean[] doSortInIncreasingOrder = (boolean[])objects[2];
		RankAggregation ra = new RankAggregation(dm, doSortInIncreasingOrder);
		dm = ra.getRanks();

		// 2. Calculate dataMatrix with RA-scores at 1-st step.
		DataMatrix dataMatrix = null;
		String[] rowNames = dm.getRowNames(), columnNames = dm.getColumnNames(), headers = new String[columnNames.length];
		for( int j = 0; j < headers.length; j++ )
			headers[j] = columnNames[j].split("_")[0];
		for( int i =0; i < peakcallers.length; i++ )
		{
			String folderName = peakcallers[i];
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
		ra = new RankAggregation(dataMatrix, UtilsForArray.getConstantArray(dataMatrix.getColumnNames().length, true));
		double[] scores = ra.getScoresTransformed(rankAggregationMethod, null, null, null);
        for( int i = 0; i < funSites.length; i++ )
        {
        	FunSite fs = funSites[i];
        	Object[] properties = new Object[] {fs.getDataMatrices().length, scores[i]};
            fs.setObjects(properties);
        }
        String[] propertyNames = new String[] {"Frequency", RankAggregation.RA_SCORE};
		FunSiteUtils.BigBedTrackUtils.writeSitesToBigBedTrack(funSites, propertyNames, pathToDirWithCombinedPeaks, exp.getPeakId(), givenSpecies, chromSizes );
		return true;
	}
	
	private int implementRankAggregationForTracks(String[] trackNames, String nameOfResultedTrack) throws Exception
    {
        if( trackNames == null || trackNames.length < 1 ) return 0;
        if( trackNames.length == 1 )
        {
        	BigBedTrack<FunSite> bbTrack = pathToDirWithCombinedPeaks.getChildPath(trackNames[0]).getDataElement(BigBedTrack.class);
            Map<String, List<FunSite>> sites = FunSiteUtils.BigBedTrackUtils
            		.readSitesInBigBedTrack(bbTrack, 0, Integer.MAX_VALUE, trackNames[0]); //RankAggregation.RA_SCORE
            FunSite[] funSites = FunSiteUtils.transformToArray(sites);
            DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
            String[] names = dm.getColumnNames();
            String name = null;
            for( String str : names )
                if( str.contains(RankAggregation.RA_SCORE) )
                    name = str;
            double[] scores = dm.getColumn(name);
            for( int i = 0; i < funSites.length; i++ )
            {
            	FunSite fs = funSites[i];
            	String[] peakIds = funSites[i].getDistinctRowNames();
            	Object[] objects = new Object[]{String.join(";", peakIds), scores[i]};
                fs.setObjects(objects);
            }
            String[] propertyNames = new String[] {"peak_ids", RankAggregation.RA_SCORE}; 
            FunSiteUtils.BigBedTrackUtils.writeSitesToBigBedTrack(funSites, propertyNames, pathToOutputDir, nameOfResultedTrack, givenSpecies, chromSizes);
            return funSites.length;
        }
        
        CombinedSites combinedSites = new CombinedSites(combinedPeakType, pathToDirWithCombinedPeaks, trackNames, minimalLengthOfPeaks, maximalLengthOfPeaks, false);
        FunSite[] funSites = combinedSites.getCombinedSites();
        double[] scores = calculateRankAggregationScores(funSites);
        for( FunSite fs : funSites )
            fs.setObjects(new Object[]{fs.getDistinctRowNames()});

        // TODO: check mergeNearestRegions()
        funSites = mergeNearestRegions(funSites, scores);
        scores = calculateRankAggregationScores(funSites);
        for( int i = 0; i < funSites.length; i++ )
        {
        	FunSite fs = funSites[i];
        	String[] peakIds = funSites[i].getDistinctRowNames();
        	Object[] objects = new Object[]{String.join(";", peakIds), scores[i]};
            fs.setObjects(objects);
        }
        String[] propertyNames = new String[] {"peak_ids", RankAggregation.RA_SCORE};
        FunSiteUtils.BigBedTrackUtils.writeSitesToBigBedTrack(funSites, propertyNames, pathToOutputDir, nameOfResultedTrack, givenSpecies, chromSizes);
        return funSites.length;
    }
	
	private Object[] getCombinedSitesAndDataMatrixFromChIPseqExp(ChIPseqExperiment exp, boolean doIncludeCombinedFrequency) throws IOException
	{
		// 1. Definition of columnNamesSelected and doSortInIncreasingOrder.
		String[] columnNamesSelected = doIncludeCombinedFrequency ? PEAK_CALLER_CHARACTERISTICS : (String[])ArrayUtils.add(PEAK_CALLER_CHARACTERISTICS, "Combined_frequency");
		boolean[] doSortInIncreasingOrder = doIncludeCombinedFrequency ? DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER : ArrayUtils.add(DO_SORT_CHARACTERISTICS_IN_INCREASING_ORDER, false);
		
		// 2. Get combined sites.
		Map<String, List<FunSite>> combinedSites = new HashMap<>();
		for(int i = 0; i < peakcallers.length; i++ )
		{
			String peakcaller = peakcallers[i];
			BigBedTrack<? extends ChIPSeqPeak> peaksTrack = chipSeqPeaks.getChildPath( peakcaller.toUpperCase(), exp.getPeakId() + ".bb" )
                    .optDataElement( BigBedTrack.class );

			if( peaksTrack == null )
                continue;
			
			Map<String, List<FunSite>> sitesMap = FunSiteUtils.BigBedTrackUtils.readSitesInChIPseqBigBedTrack(peaksTrack, peakcaller, minimalLengthOfPeaks, maximalLengthOfPeaks);
			// chromSizes keySet instead of sitesMap to filter chrs that are not in the master track
			for(String chr : chromSizes.keySet())
			{
				List<FunSite> sites = sitesMap.get( chr );
				if(sites != null)
					combinedSites.computeIfAbsent(chr, key -> new ArrayList<>()).addAll(sites);
			}
		}
		
		FunSite[] funSites = CombinedSites.produceCombinedSites(combinedSites, combinedPeakType, true);
		if( funSites.length < siteNumberThreshold ) return null;

		// 3. Perform quality control.
		if( ! Double.isNaN(fpcmThreshold) )
		{
			int[] pivotalFrequencies = QualityControlSites.calculatePivotalFrequencies(funSites, peakcallers.length);
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

	// TODO: To optimize this method!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	// added org.apache.commons.lang3.ArrayUtils.removeAll to remove several values at once
	// 10.05.2022
	private double[] calculateRankAggregationScores(FunSite[] funSites)
	{
		DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
		String[] columnNames = dm.getColumnNames();
		ArrayList<String> columnNamesToRemove = new ArrayList<String>();
		for( int j = columnNames.length - 1; j >= 0; j-- )
		{
			if( ! columnNames[j].contains(RankAggregation.RA_SCORE) ) 
			{
				columnNamesToRemove.add(columnNames[j]);
			}
		}
		if(!columnNamesToRemove.isEmpty())
		{
			dm.removeColumns(columnNamesToRemove.toArray(new String[0]));
		}
		RankAggregation ra = new RankAggregation(dm, UtilsForArray.getConstantArray(dm.getColumnNames().length, true));
		return ra.getScoresTransformed(rankAggregationMethod, null, null, null);
	}
	
	// new : every FunSite contains DataMatrix
    // Input funSites must be sorted within each chromosome!!!
    private static FunSite[] mergeNearestRegions(FunSite[] funSites, double[] scores)
    {
        int maximalLength = 100, maximalDistance = 21; // <- max length not from input parameters???
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
    
	public static class CistromeConstructionByMtParametersBeanInfo extends BeanInfoEx2<CistromeConstructionByMtParameters>
	{
		public CistromeConstructionByMtParametersBeanInfo()
		{
			super(CistromeConstructionByMtParameters.class);
		}
		@Override
		protected void initProperties() throws Exception
		{
			add("pathToMasterTrack");
			add("minimalLengthOfPeaks");
            add("maximalLengthOfPeaks");
            add("siteNumberThreshold");
            add("doPerformQualityControl");
            addHidden("fpcmThreshold", "isFpcmThresholdHidden");
			add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
		    add(DataElementPathEditor.registerOutput("pathToOutputDir", beanClass, FolderCollection.class, true));
		}
	}

	public static class CistromeConstructionByMtParameters extends AbstractAnalysisParameters
	{
		private DataElementPath pathToOutputDir;
		private DataElementPath chipSeqPeaks = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/ChIP-seq/Peaks" );
		private int siteNumberThreshold = 2000;
		private Species species = Species.getDefaultSpecies(null);
        private int minimalLengthOfPeaks = 20;
        private int maximalLengthOfPeaks = 1000000;
        private boolean doPerformQualityControl = true;
        private double fpcmThreshold = 3.0;
        private DataElementPath pathToMasterTrack = DataElementPath.create("databases/GTRD/Data/bigBed");
        
    	@PropertyName (MessageBundle.PN_PATH_TO_OUTPUT_FOLDER)
    	@PropertyDescription ( MessageBundle.PD_PATH_TO_OUTPUT_FOLDER )
		public DataElementPath getPathToOutputDir() {
			return pathToOutputDir;
		}
		public void setPathToOutputDir(DataElementPath pathToOutputDir) {
			this.pathToOutputDir = pathToOutputDir;
		}

		@PropertyName("ChIP-seq peaks dir")
		@PropertyDescription("ChIP-seq peaks dir (*.bb-files)")
		public DataElementPath getChipSeqPeaks() {
			return chipSeqPeaks;
		}
		public void setChipSeqPeaks(DataElementPath chipSeqPeaks) {
			this.chipSeqPeaks = chipSeqPeaks;
		}

		@PropertyName("Path to master-track")
		@PropertyDescription("Path to master-track in bigBed format")
		public DataElementPath getPathToMasterTrack()
		{
			return pathToMasterTrack;
		}
		public void setPathToMasterTrack(DataElementPath pathToMasterTrack)
		{
			this.pathToMasterTrack = pathToMasterTrack;
		}
		
		@PropertyName("Species")
		@PropertyDescription("Species")
		public Species getSpecies()
		{
			return species;
		}
		public void setSpecies(Species species)
		{
			Object oldValue = this.species;
			this.species = species;
			firePropertyChange("species", oldValue, species);
		}
		
		 @PropertyName(MessageBundle.PN_MINIMAL_LENGTH_OF_PEAKS)
	        @PropertyDescription(MessageBundle.PD_MINIMAL_LENGTH_OF_PEAKS)
	        public int getMinimalLengthOfPeaks()
	        {
	            return minimalLengthOfPeaks;
	        }
	        public void setMinimalLengthOfPeaks(int minimalLengthOfPeaks)
	        {
	            Object oldValue = this.minimalLengthOfPeaks;
	            this.minimalLengthOfPeaks = minimalLengthOfPeaks;
	            firePropertyChange("minimalLengthOfPeaks", oldValue, minimalLengthOfPeaks);
	        }

	        @PropertyName(MessageBundle.PN_MAXIMAL_LENGTH_OF_PEAKS)
	        @PropertyDescription(MessageBundle.PD_MAXIMAL_LENGTH_OF_PEAKS)
	        public int getMaximalLengthOfPeaks()
	        {
	            return maximalLengthOfPeaks;
	        }
	        public void setMaximalLengthOfPeaks(int maximalLengthOfPeaks)
	        {
	            Object oldValue = this.maximalLengthOfPeaks;
	            this.maximalLengthOfPeaks = maximalLengthOfPeaks;
	            firePropertyChange("maximalLengthOfPeaks", oldValue, maximalLengthOfPeaks);
	        }
	        
	        @PropertyName(MessageBundle.PN_DO_PERFORM_QUALITY_CONTROL)
	        @PropertyDescription(MessageBundle.PD_DO_PERFORM_QUALITY_CONTROL)
	        public boolean getDoPerformQualityControl()
	        {
	            return doPerformQualityControl;
	        }
	        public void setDoPerformQualityControl(boolean doPerformQualityControl)
	        {
	            Object oldValue = this.doPerformQualityControl;
	            this.doPerformQualityControl = doPerformQualityControl;
	            firePropertyChange("*", oldValue, doPerformQualityControl);
	        }

	        @PropertyName(MessageBundle.PN_FPCM_THRESHOLD)
	        @PropertyDescription(MessageBundle.PD_FPCM_THRESHOLD)
	        public double getFpcmThreshold()
	        {
	            return fpcmThreshold;
	        }
	        public void setFpcmThreshold(double fpcmThreshold)
	        {
	            Object oldValue = this.fpcmThreshold;
	            this.fpcmThreshold = fpcmThreshold;
	            firePropertyChange("fpcmThreshold", oldValue, fpcmThreshold);
	        }
	        
	        @PropertyName(MessageBundle.PN_SITE_NUMBER_THRESHOLD)
	        @PropertyDescription(MessageBundle.PD_SITE_NUMBER_THRESHOLD)
	        public int getSiteNumberThreshold()
	        {
	            return siteNumberThreshold;
	        }
	        public void setSiteNumberThreshold(int siteNumberThreshold)
	        {
	            Object oldValue = this.siteNumberThreshold;
	            this.siteNumberThreshold = siteNumberThreshold;
	            firePropertyChange("siteNumberThreshold", oldValue, siteNumberThreshold);
	        }
	        
	        public boolean isFpcmThresholdHidden()
	        {
	            return ! getDoPerformQualityControl();
	        }

	}
	public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Please, select option (i.e. select the concrete session of given analysis).";
        
        public static final String PN_SPECIES = "Species";
        public static final String PD_SPECIES = "Please, select a taxonomical species";
        
        public static final String PN_MINIMAL_LENGTH_OF_PEAKS = "Minimal length of peaks";
        public static final String PD_MINIMAL_LENGTH_OF_PEAKS = "All peaks that are shorter than this threshold will be prolongated";
        
        public static final String PN_MAXIMAL_LENGTH_OF_PEAKS = "Maximal length of peaks";
        public static final String PD_MAXIMAL_LENGTH_OF_PEAKS = "All peaks that are longer than this threshold will be truncated";
        
        public static final String PN_DO_PERFORM_QUALITY_CONTROL = "Do perform quality control?";
        public static final String PD_DO_PERFORM_QUALITY_CONTROL = "Do perform quality control?";
        
        public static final String PN_FPCM_THRESHOLD = "FPCM threshold";
        public static final String PD_FPCM_THRESHOLD = "FPCM threshold";
        
        public static final String PN_SITE_NUMBER_THRESHOLD = "Site number threshold";
        public static final String PD_SITE_NUMBER_THRESHOLD = "If size of a data set is less than this threshold, then data set will be excluded from rank aggregation";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }
}