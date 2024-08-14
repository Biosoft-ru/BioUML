package biouml.plugins.gtrd.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.gtrd.analysis.QualityControlAnalysis.AllParameters;
import biouml.plugins.gtrd.utils.EnsemblUtils;
import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.FunSiteUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils.RocCurve;
import biouml.plugins.gtrd.utils.SiteModelUtils.SiteModelComposed;
import biouml.plugins.gtrd.utils.SiteUtils;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.SqlTrack.SitesCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.bean.BeanInfoEx2;

public class AUCEstimationForTrack extends AnalysisMethodSupport<AUCEstimationForTrack.AUCEstimationForTrackParameters>
{

    public AUCEstimationForTrack(DataCollection<?> origin, String name)
    {
        super(origin, name, new AUCEstimationForTrackParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut()
    {

    	HashMap<String, FunSite[]> funSitesGroups = new HashMap<String, FunSite[]>();
    	
    	ru.biosoft.access.core.DataElementPath siteModelPath = parameters.getSiteModelPath();
    	ru.biosoft.access.core.DataElementPath pathToTrack = parameters.getPathToTrack();
    	ru.biosoft.access.core.DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
    	ru.biosoft.access.core.DataElementPath pathToOutputFolder = parameters.pathToOutputFolder;
    	String prefix = parameters.getPrefix();
    	int numberOfGroups = parameters.getNumberOfGroups();
    	int groupSize = parameters.getGroupSize();
    	int w = 100;
    	int minimalLengthOfSite = parameters.getMinimalLengthOfSite();
    	int maximalLengthOfSite = parameters.getMaximalLengthOfSite();
    	
    	if( parameters.isDoSplitClustersByRaScore() )
    	{
    		HashMap<Integer, ArrayList<Site>> SitesGroups = new HashMap<Integer, ArrayList<Site>>();
    		SqlTrack sqlTrack = pathToTrack.getDataElement(SqlTrack.class);
        	ArrayList<Site> allSites = new ArrayList<Site>();
        	double minRaScore = 100.0;
        	double maxRaScore = 0.0;
        	ArrayList<Site> allSitesonTrackFiltered = removeUnusualChromosomes(pathToSequences, sqlTrack.getAllSites());
        	
        	for( Site site : allSitesonTrackFiltered )
            {
                double raScore = (double) Double.parseDouble(site.getProperties().getProperty("RA-score").getValue().toString());
                if(raScore > maxRaScore) maxRaScore = raScore;
                if(raScore < minRaScore) minRaScore = raScore;
                allSites.add(site);
            }
            
        	for(int i = 0; i < numberOfGroups; i++)
        		SitesGroups.put( (Integer) i, new ArrayList<Site>() );
        	
        	double groupDelta = ( maxRaScore - minRaScore ) / numberOfGroups;
        	for( Site site : allSites )
        	{
        		double raScore = (double) Double.parseDouble(site.getProperties().getProperty("RA-score").getValue().toString());
        		if( raScore == maxRaScore )
        			SitesGroups.get( numberOfGroups - 1 ).add( site );
        		else
        		{
        			Integer group = (int) Math.floor( (raScore - minRaScore) / groupDelta );
        			SitesGroups.get( group ).add( site );
        		}
        	}
        	
        	Random random = new Random();
        	for(int i = 0; i < numberOfGroups; i++)
        	{
        		double lowBound = minRaScore + (i * groupDelta);    		
        		double highBound;
        		if( i == (numberOfGroups - 1) )
        			highBound = maxRaScore;
        		else
        			highBound = minRaScore + ((i + 1) * groupDelta);
        		
        		String groupName = lowBound + "-" + highBound;
        		int size = SitesGroups.get(i).size();
        		FunSite[] subgroupFunSites;
        		double sumOfRaScores = 0;
        		if(size == 0) continue;
        		if(size <= groupSize)
        		{
        			subgroupFunSites = new FunSite[size];
        			int index = 0;
        			for(Site site : SitesGroups.get(i))
        			{
        				String chromosomeName = site.getSequence().getName();
        	            Interval coordinates = site.getInterval();
        	            DataMatrix dataMatrix = null;
        	            subgroupFunSites[index++] = new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix);
        	            
        	            double raScore = (double) Double.parseDouble(site.getProperties().getProperty("RA-score").getValue().toString());
        	            log.info(groupName + " " + i + " RA-score: " + raScore);
        	            sumOfRaScores += raScore;
        			}
        			log.info("> " + groupName + " mean RA-score: " + (sumOfRaScores / size));

        		}
        		else
        		{
        			subgroupFunSites = new FunSite[groupSize];
        			HashSet<Integer> usedIndexes = new HashSet<Integer>();
        			for(int j = 0; j < groupSize; j++)
        			{
        				int index = random.nextInt(size);
        				while(usedIndexes.contains(index))
        					index = random.nextInt(size);
        				usedIndexes.add(index);
        				Site site = SitesGroups.get(i).get(index);
        	            String chromosomeName = site.getSequence().getName();
        	            Interval coordinates = site.getInterval();
        	            DataMatrix dataMatrix = null;
        	            subgroupFunSites[j] = new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix);
        	            
        	            double raScore = (double) Double.parseDouble(site.getProperties().getProperty("RA-score").getValue().toString());
        	            log.info(groupName + " " + i + " RA-score: " + raScore);
        	            sumOfRaScores += raScore;
        			}
        			log.info("> " + groupName + " mean RA-score: " + (sumOfRaScores / groupSize));
        		}
        		funSitesGroups.put(groupName, subgroupFunSites);
        	}
    	}
    	else
    	{
    		HashMap<Integer, ArrayList<String>> SitesGroups = new HashMap<Integer, ArrayList<String>>();
    		SqlTrack sqlTrack = pathToTrack.getDataElement(SqlTrack.class);
    		SitesCollection sortableSiteCollection = (SitesCollection) sqlTrack.getAllSites();
    		HashMap<String, Site> allSites = new HashMap<String, Site>();
    		List<String> sortedNameList = sortableSiteCollection.getSortedNameList("Property: RA-score", true);

    		for( Site site : sortableSiteCollection )
    			allSites.put(site.getName(), site);

    		for(int i = 0; i < numberOfGroups; i++)
    			SitesGroups.put( (Integer) i, new ArrayList<String>() );
    		int groupDelta = (int) Math.floor( allSites.size() / numberOfGroups);

    		int groupIndex = 0;
    		int index = 0;
    		ArrayList<String> sortedSitesGroup = new ArrayList<String>();
    		for (String siteName : sortedNameList) {
    			if( groupIndex != (numberOfGroups - 1) )
    				if( (groupIndex + 1) * groupDelta <= index  )
    				{
    					SitesGroups.put(groupIndex, sortedSitesGroup);
    					sortedSitesGroup = new ArrayList<String>();
    					groupIndex++;
    				}
    			sortedSitesGroup.add(siteName);
    			index++;
    		}
    		SitesGroups.put(numberOfGroups - 1, sortedSitesGroup);

    		Random random = new Random();
    		for(int i = 0; i < numberOfGroups; i++)
    		{
    			ArrayList<String> subgroupSiteNameList = SitesGroups.get(i);
    			int size = subgroupSiteNameList.size();
    			Site lowestSite = allSites.get(subgroupSiteNameList.get(0));
    			Site highestSite = allSites.get(subgroupSiteNameList.get(size - 1));
    			double lowBound = (double) Double.parseDouble(lowestSite.getProperties().getProperty("RA-score").getValue().toString());    		
    			double highBound = (double) Double.parseDouble(highestSite.getProperties().getProperty("RA-score").getValue().toString());

    			String groupName = lowBound + "-" + highBound;
    			FunSite[] subgroupFunSites;
    			double sumOfRaScores = 0;
    			if(size == 0) continue;

    			//            SqlTrack track = SqlTrack.createTrack(pathToOutputFolder.getChildPath("track_subgroup_" + i), null, null);

    			if(size <= groupSize)
    			{
    				subgroupFunSites = new FunSite[size];
    				int indexSite = 0;
    				for(String siteName : subgroupSiteNameList)
    				{
    					Site site = allSites.get(siteName);
    					//    				track.addSite(site);
    					String chromosomeName = site.getSequence().getName();
    					Interval coordinates = site.getInterval();
    					coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
    					DataMatrix dataMatrix = null;
    					subgroupFunSites[indexSite++] = new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix);

    					double raScore = (double) Double.parseDouble(site.getProperties().getProperty("RA-score").getValue().toString());
    					sumOfRaScores += raScore;
    				}
    				log.info("> " + groupName + " mean RA-score: " + (sumOfRaScores / size));

    			}
    			else
    			{
    				subgroupFunSites = new FunSite[groupSize];
    				HashSet<Integer> usedIndexes = new HashSet<Integer>();
    				for(int j = 0; j < groupSize; j++)
    				{
    					int indexSite = random.nextInt(size);
    					while(usedIndexes.contains(indexSite))
    						indexSite = random.nextInt(size);
    					usedIndexes.add(indexSite);
    					Site site = allSites.get(subgroupSiteNameList.get(indexSite));
    					//    				track.addSite(site);
    					String chromosomeName = site.getSequence().getName();
    					Interval coordinates = site.getInterval();
    					coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
    					DataMatrix dataMatrix = null;
    					subgroupFunSites[j] = new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix);

    					double raScore = (double) Double.parseDouble(site.getProperties().getProperty("RA-score").getValue().toString());
    					sumOfRaScores += raScore;
    				}
    				log.info("> " + groupName + " mean RA-score: " + (sumOfRaScores / groupSize));
    			}
    			funSitesGroups.put(groupName, subgroupFunSites);
    			//            track.finalizeAddition();
    			//            CollectionFactory.save(track);
    		}
    	}
    	SiteModel siteModel = siteModelPath.getDataElement(SiteModel.class);
        int lengthOfSequenceRegion = w + siteModel.getLength();
        log.info("ROC-curve are producing");
    	
        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
        double[][] xValuesForCurves = new double[numberOfGroups][], yValuesForCurves = new double[numberOfGroups][];
        double[] aucs = new double[numberOfGroups];
        double[] sizes = new double[numberOfGroups];
        
    	String[] groupNames = new String[numberOfGroups];
        int i = 0;
        for( Map.Entry<String, FunSite[]> entry : funSitesGroups.entrySet())
        {
        	FunSite[] funSites = entry.getValue();
        	funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
        	Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
            RocCurve rocCurve = new RocCurve(smc, sequences, 10, 0);
            double[][] curve  = rocCurve.getRocCurve();
            xValuesForCurves[i] = curve[0];
            yValuesForCurves[i] = curve[1];
            aucs[i] = rocCurve.getAuc();
            sizes[i] = funSites.length;
            log.info("AUC = " + aucs[i] + " with size: " + sizes[i]);
            groupNames[i] = entry.getKey();
            i++;
        }
        
        DataMatrix dm = new DataMatrix(groupNames, "AUC", aucs);
        dm.writeDataMatrix(false, pathToOutputFolder, prefix + "_AUCs_tab", log);
        dm = new DataMatrix(groupNames, "AUC", sizes);
        dm.writeDataMatrix(false, pathToOutputFolder, prefix + "_sizes_tab", log);
        
        Chart chart = ChartUtils.createChart(xValuesForCurves, yValuesForCurves, groupNames, null, null, null, null, "Specificity", "Sensitivity", true);
        DataCollection<DataElement> parent  = pathToOutputFolder.getDataCollection();
        ChartDataElement chartDE = new ChartDataElement(prefix + "_ROC-curves", parent, chart);
        parent.put(chartDE);
		return null;
    }
    
    public static ArrayList<Site> removeUnusualChromosomes(DataElementPath pathToSequences, DataCollection<Site> sites)
    {
        ArrayList<Site> result = new ArrayList<>();
        String[] chromosomeNamesAvailable = EnsemblUtils.getStandardSequencesNames(pathToSequences);
        for( Site s : sites )
            if( ArrayUtils.contains(chromosomeNamesAvailable, s.getOriginalSequence().getName()) )
                result.add(s);
        return result;
    }
    
    public static ArrayList<String> sitesWithUnusualChromosomes(DataElementPath pathToSequences, DataCollection<Site> sites)
    {
        ArrayList<String> result = new ArrayList<>();
        String[] chromosomeNamesAvailable = EnsemblUtils.getStandardSequencesNames(pathToSequences);
        for( Site s : sites )
            if( ArrayUtils.contains(chromosomeNamesAvailable, s.getOriginalSequence().getName()) )
                result.add(s.getName());
        return result;
    }
    
    public static class AUCEstimationForTrackParametersBeanInfo extends BeanInfoEx2<AUCEstimationForTrackParameters>
    {
        public AUCEstimationForTrackParametersBeanInfo()
        {
            super(AUCEstimationForTrackParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInput("pathToTrack", beanClass, Track.class));
            add(DataElementPathEditor.registerInput("siteModelPath", beanClass, SiteModel.class));
            add("numberOfGroups");
            add("groupSize");
            add("minimalLengthOfSite");
            add("maximalLengthOfSite");
            add("doSplitClustersByRaScore");
            add("prefix");
            property("pathToOutputFolder").inputElement( FolderCollection.class ).add();
        }

    }
    
	public static class AUCEstimationForTrackParameters extends AllParameters
	{
    	ru.biosoft.access.core.DataElementPath pathToTrack, siteModelPath, pathToOutputFolder;
        private BasicGenomeSelector dbSelector;
		String prefix;
    	int numberOfGroups = 8;
    	int groupSize = 5000;
		int minimalLengthOfSite = 20;
    	int maximalLengthOfSite = 300;
    	boolean doSplitClustersByRaScore = true;

		public AUCEstimationForTrackParameters()
    	{}
		
		public boolean isDoSplitClustersByRaScore() {
			return doSplitClustersByRaScore;
		}
		
		public void setDoSplitClustersByRaScore(boolean doSplitClustersByRaScore) {
			this.doSplitClustersByRaScore = doSplitClustersByRaScore;
		}
    	
    	public int getMinimalLengthOfSite() {
    		return minimalLengthOfSite;
    	}
    	
    	public void setMinimalLengthOfSite(int minimalLengthOfSite) {
    		this.minimalLengthOfSite = minimalLengthOfSite;
    	}
    	
    	public int getMaximalLengthOfSite() {
    		return maximalLengthOfSite;
    	}
    	
    	public void setMaximalLengthOfSite(int maximalLengthOfSite) {
    		this.maximalLengthOfSite = maximalLengthOfSite;
    	}
    	
		public DataElementPath getSiteModelPath() {
			return siteModelPath;
		}

		public void setSiteModelPath(DataElementPath siteModelPath) {
			this.siteModelPath = siteModelPath;
		}
		
		public DataElementPath getPathToTrack() {
			return pathToTrack;
		}

		public void setPathToTrack(DataElementPath pathToTrack) {
			this.pathToTrack = pathToTrack;
		}

		public DataElementPath getPathToOutputFolder() {
			return pathToOutputFolder;
		}

		public void setPathToOutputFolder(DataElementPath pathToOutputFolder) {
			this.pathToOutputFolder = pathToOutputFolder;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public int getNumberOfGroups() {
			return numberOfGroups;
		}

		public void setNumberOfGroups(int numberOfGroups) {
			this.numberOfGroups = numberOfGroups;
		}

		public int getGroupSize() {
			return groupSize;
		}

		public void setGroupSize(int groupSize) {
			this.groupSize = groupSize;
		}
		
        public BasicGenomeSelector getDbSelector()
        {
            return dbSelector;
        }
        public void setDbSelector(BasicGenomeSelector dbSelector)
        {
            Object oldValue = this.dbSelector;
            this.dbSelector = dbSelector;
            dbSelector.setParent(this);
            firePropertyChange("dbSelector", oldValue, dbSelector);
        }
        
	}
}
