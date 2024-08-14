package biouml.plugins.gtrd.analysis;

import java.beans.PropertyDescriptor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.DNaseExperiment;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;

public class DnaseProfileCreation extends AnalysisMethodSupport<DnaseProfileCreation.DnaseProfileCreationParameters>
{
	private PropertyDescriptor PROFILE_PD = StaticDescriptor.create("profile");
	private PropertyDescriptor MAX_PROFILE_HEIGHT_PD = StaticDescriptor.create( "maxProfileHeight" );
//	private PropertyDescriptor NUMBER_OF_TRACKS_PD = StaticDescriptor.create( "numberOfTracks" );
	
	private int threadsNumber;
	volatile SqlTrack resultTrack;

	volatile Map<String, short[]> coverageByChr = new HashMap<>();
	volatile double maxProfile;
	volatile float numberOfTracks;
	private Date date;
	private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"); 

	public DnaseProfileCreation(DataCollection<?> origin, String name)
	{
		super(origin, name, new DnaseProfileCreationParameters());
	}

	@Override
	public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception {
		threadsNumber = parameters.getThreadsNumber();

		DataCollection<DNaseExperiment> expCollection = parameters.getPathToExperiments().optDataCollection(DNaseExperiment.class);
        HashMap<String, List<DNaseExperiment>> expsGrouped = new HashMap<>();
		if(parameters.getByCells())
		{
			HashMap<String, List<DNaseExperiment>> expsGroupedByCell = getExpsGroupedByCell(expCollection);
			expsGrouped.putAll(expsGroupedByCell);
		}
		if(parameters.getBySpecies())
		{
			HashMap<String, List<DNaseExperiment>> expsGroupedBySpecies = getExpsGroupedBySpecies(expCollection);
			expsGrouped.putAll(expsGroupedBySpecies);
		}
		if(parameters.getOnlyHuman())
		{
			expsGrouped = getExpsGroupedBySpecies(expCollection, "Human");
		}
		if(!parameters.getByCells() && !parameters.getBySpecies() && !parameters.getOnlyHuman())
		{
			throw new IllegalArgumentException("Type of experiments is not selected");
		}

		String peakCaller = parameters.getPeakCaller();
		ru.biosoft.access.core.DataElementPath outputDir = parameters.getPathToOutputDir();
		List<SqlTrack> allMergedTracks = new ArrayList<>();
		log.info(expsGrouped.size() + " groups of DNase-seq experiments");

		int counter = 1;
		for(Entry<String, List<DNaseExperiment>> expsGroup : expsGrouped.entrySet())
		{

			List<SqlTrack> peaks = new ArrayList<>(); 
			for(DNaseExperiment exp : expsGroup.getValue())
				for(DataElementPath peaksPath : exp.getPeaksByPeakCaller( peakCaller ))
					if(peaksPath.exists())
						peaks.add(peaksPath.getDataElement(SqlTrack.class));

			if(peaks.size() == 0)
				continue;

			numberOfTracks = peaks.size();
			ru.biosoft.access.core.DataElementPath genomePath = TrackUtils.getTrackSequencesPath( peaks.get( 0 ) );
			
			for(DataElementPath chrPath : genomePath.getChildren())
			{
				Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
				coverageByChr.put( chrPath.getName(), new short[chrSeq.getLength()] );
			}

			String title;
			if( expsGroup.getKey().matches(".*\\d.*") )
				title = expsGroup.getValue().get(0).getSpecie().getCommonName() + "_" + expsGroup.getValue().get(0).getCell().getTitle();
			else 
				title = "all";
			date = new Date();
			log.info(formatter.format(date) + ": " + counter++ + ". " + title);
			String trackName = peakCaller + "_" + expsGroup.getKey() + "_" + title;
			ru.biosoft.access.core.DataElementPath resultTrackPath = DataElementPath.create( outputDir.getDataCollection(), trackName);
			resultTrack = SqlTrack.createTrack(resultTrackPath, peaks.get( 0 )); // merged track with profile

			List<List<SqlTrack>> groupedTracks = new ArrayList<>();
			int groupSize;
			int tmpThreadsNumber;
	
			if(peaks.size() < threadsNumber)
			{
				groupSize = peaks.size();
				tmpThreadsNumber = 1;
			}
			else
			{
				groupSize = (int) peaks.size() / threadsNumber;
				tmpThreadsNumber = threadsNumber;
			}

			int prevIndex = 0;
			int currIndex = 0;
			for(int i = 1; i <= tmpThreadsNumber; i++)
			{
				currIndex = i * groupSize;
				if(i == tmpThreadsNumber)
					groupedTracks.add( peaks.subList(prevIndex, peaks.size()) );
				else
					groupedTracks.add(peaks.subList(prevIndex, currIndex));
				prevIndex = currIndex;
			}
			
			log.info("Number of tracks: " + peaks.size());
			log.info("Number of groups: " + groupedTracks.size());

			date = new Date();
			log.info(formatter.format(date) + ": SiteProfileIteration in progress");
			TaskPool.getInstance().iterate(groupedTracks, new SiteProfileIteration(), tmpThreadsNumber);
			
			date = new Date();
			log.info(formatter.format(date) + ": ProfileSplitterIteration in progress");
			TaskPool.getInstance().iterate(genomePath.getChildren(), new ProfileSplitterIteration(), threadsNumber);
			
//			resultTrack.getDynamicProperties().add(new DynamicProperty(NUMBER_OF_TRACKS_PD, Float.class, numberOfTracks));
//			resultTrack.getDynamicProperties().add(new DynamicProperty(MAX_PROFILE_HEIGHT_PD, Float.class, maxProfile));
			resultTrack.finalizeAddition();
			CollectionFactoryUtils.save(resultTrack);
			allMergedTracks.add(resultTrack);
			
		}

		return null;
	}

	class SiteProfileIteration implements Iteration<List<SqlTrack>>
	{

		SiteProfileIteration()
		{}

		@Override
		public boolean run(List<SqlTrack> tracks) 
		{
			try
			{
				for(SqlTrack sqlTrack : tracks)
				{
					if(sqlTrack.getName().contains("DPEAKS002059"))
						log.info("---> " + sqlTrack.getName());
					else if (sqlTrack.getName().contains("DPEAKS002062"))
						log.info("---> " + sqlTrack.getName());
					
					DataCollection<Site> sites = sqlTrack.getAllSites();
					String chrName;
					for( Site site : sites )
					{
						chrName = site.getSequence().getName();
						if(coverageByChr.containsKey(chrName))
						{
							coverageByChr.get(chrName)[site.getFrom()]++;
							if((site.getTo() + 1) < coverageByChr.get(chrName).length)
								coverageByChr.get(chrName)[site.getTo() + 1]--; //exclusive site end
							else
								coverageByChr.get(chrName)[coverageByChr.get(chrName).length - 1]--;
						}
					}
				}
				return true;
			}
			catch (Exception e)
			{
				log.info(e.getMessage());
				for(int i = 0; i < e.getStackTrace().length; i++)
					log.info(e.getStackTrace()[i].toString());
				log.info("Exception");
				return false;
			}
		}
	}
	
	class ProfileSplitterIteration implements Iteration<ru.biosoft.access.core.DataElementPath>
	{
		@Override
		public boolean run(DataElementPath element)
		{
//			log.info(formatter.format(new Date()) + " : " + Thread.currentThread().getName() + " is running");
			Sequence chrSeq = element.getDataElement( AnnotatedSequence.class ).getSequence();
			short[] coverage = coverageByChr.get( element.getName() );
			List<Site> resultSites = new ArrayList<>();
			boolean insideTheRegion = false;
			int from = 0;
			int to = 0;
			short height = 0;
			for(int i = 0; i < coverage.length; i++)
			{
				//trimming 0 height profile and split regions
				if(coverage[i] != 0)
				{
					height += coverage[i];
				}
				coverage[i] = height;
				
				if( !insideTheRegion && coverage[i] != 0 )
				{
					from = i;
					insideTheRegion = true;
				}
				
				if( insideTheRegion && ( coverage[i] == 0 || i == (coverage.length - 1) ) )
				{
					to = --i;
					SiteImpl newSite = new SiteImpl(null, chrSeq.getName(), from, to - from + 1,
							Site.STRAND_NOT_APPLICABLE, chrSeq);

					if(newSite.getLength() > 0)
					{
					double[] subProfile = new double[newSite.getLength()];
					for(int j = 0; j < newSite.getLength(); j++)
						subProfile[j] = coverage[j + from];

					newSite.getProperties().add( new DynamicProperty( PROFILE_PD, double[].class, subProfile ));
					double currentMaxProfile = getMaxHeight(subProfile);
					newSite.getProperties().add( new DynamicProperty( MAX_PROFILE_HEIGHT_PD, Float.class, currentMaxProfile ) );
					if(currentMaxProfile > 0)
						resultSites.add( newSite );
//					if(currentMaxProfile > maxProfile)
//						maxProfile = currentMaxProfile;
					}
					insideTheRegion = false;
				}
			}

//			log.info(formatter.format(new Date()) + " : " + Thread.currentThread().getName() + " is writing");
			synchronized(resultTrack)
			{
				for(Site result : resultSites)
					resultTrack.addSite( result );
			}

			log.info(formatter.format(new Date()) + " : " + Thread.currentThread().getName() + " finished");
			return true;
		}
	}

	public static double getDistance(SqlTrack firstTrack, SqlTrack secondTrack)
	{
		double distance = 0.0;

		return distance;
	}

	public static HashMap<String, List<DNaseExperiment>> getExpsGroupedByCell(DataCollection<DNaseExperiment> expCollection)
	{
		HashMap<String, List<DNaseExperiment>> expsGroupedByCell = new HashMap<>();
		for(DNaseExperiment exp : expCollection)
		{
			String cellName = exp.getCell().getName();
			if(!expsGroupedByCell.containsKey(cellName))
			{
				List<DNaseExperiment> newList = new ArrayList<>();
				expsGroupedByCell.put(cellName, newList);
			}
			expsGroupedByCell.get(cellName).add(exp);
		}
		return expsGroupedByCell;
	}

	public static HashMap<String, List<DNaseExperiment>> getExpsGroupedBySpecies(DataCollection<DNaseExperiment> expCollection)
	{
		HashMap<String, List<DNaseExperiment>> expsGroupedBySpecies = new HashMap<>();
		for(DNaseExperiment exp : expCollection)
		{
			String speciesName = exp.getSpecie().getCommonName();
			if(!expsGroupedBySpecies.containsKey(speciesName))
			{
				List<DNaseExperiment> newList = new ArrayList<>();
				expsGroupedBySpecies.put(speciesName, newList);
			}
			expsGroupedBySpecies.get(speciesName).add(exp);
		}
		return expsGroupedBySpecies;
	}

	public static HashMap<String, List<DNaseExperiment>> getExpsGroupedBySpecies(DataCollection<DNaseExperiment> expCollection, String species)
	{
		HashMap<String, List<DNaseExperiment>> expsGroupedBySpecies = new HashMap<>();
		for(DNaseExperiment exp : expCollection)
		{
			String speciesName = exp.getSpecie().getCommonName();
			if(!speciesName.equals(species))
				continue;
			if(!expsGroupedBySpecies.containsKey(speciesName))
			{
				List<DNaseExperiment> newList = new ArrayList<>();
				expsGroupedBySpecies.put(speciesName, newList);
			}
			expsGroupedBySpecies.get(speciesName).add(exp);
		}
		return expsGroupedBySpecies;
	}

	public static class DnaseProfileCreationParameters extends AbstractAnalysisParameters
	{
		private DataElementPath pathToExperiments;
		private DataElementPath pathToOutputDir;
		private String peakCaller = "macs2";
		private int binSize;
		private int threadsNumber = 1;
		private boolean bySpecies = false;
		private boolean byCells = false;
		private boolean onlyHuman = false;

		DnaseProfileCreationParameters()
		{
			pathToExperiments = DataElementPath.create("databases/GTRD/Data/DNase experiments");
		}

		public DataElementPath getPathToExperiments() {
			return pathToExperiments;
		}

		@PropertyName("Directory with experiments")
		@PropertyDescription("Directory with experiments")
		public void setPathToExperiments(DataElementPath pathToExperiments) {
			this.pathToExperiments = pathToExperiments;
		}

		@PropertyName("Output Directory")
		@PropertyDescription("Output Directory")
		public DataElementPath getPathToOutputDir() {
			return pathToOutputDir;
		}
		public void setPathToOutputDir(DataElementPath pathToOutputDir) {
			this.pathToOutputDir = pathToOutputDir;
		}

		public int getThreadsNumber()
		{
			return threadsNumber;
		}

		@PropertyName("Number of threads")
		@PropertyDescription("Number of threads")
		public void setThreadsNumber(int threadsNumber)
		{
			this.threadsNumber = threadsNumber;
		}

		public int getBinSize() {
			return binSize;
		}

		public void setBinSize(int binSize) {
			this.binSize = binSize;
		}

		public boolean getBySpecies() {
			return bySpecies;
		}

		public void setBySpecies(boolean bySpecies) {
			Object oldValue = this.bySpecies;
			this.bySpecies = bySpecies;
			firePropertyChange("*", oldValue, bySpecies);
			if(bySpecies)
				setOnlyHuman(false);
		}

		public boolean getByCells() {
			return byCells;
		}

		public void setByCells(boolean byCells) {
			Object oldValue = this.byCells;
			this.byCells = byCells;
			firePropertyChange("*", oldValue, byCells);
			if(byCells)
				setOnlyHuman(false);
		}

		public boolean getOnlyHuman() {
			return onlyHuman;

		}

		public void setOnlyHuman(boolean onlyHuman) {
			Object oldValue = this.onlyHuman;
			this.onlyHuman = onlyHuman;
			firePropertyChange("*", oldValue, onlyHuman);
			if(onlyHuman)
			{
				setByCells(false);
				setBySpecies(false);
			}
		}

		public String getPeakCaller() {
			return peakCaller;
		}

		public void setPeakCaller(String peakCaller) {
			Object oldValue = this.peakCaller;
			this.peakCaller = peakCaller;
			firePropertyChange("*", oldValue, peakCaller);
		}
	}

	public static class DnaseProfileCreationParametersBeanInfo extends BeanInfoEx2<DnaseProfileCreationParameters>
	{
		public DnaseProfileCreationParametersBeanInfo()
		{
			super(DnaseProfileCreationParameters.class);
		}

		@Override
		protected void initProperties() throws Exception
		{
			property("pathToExperiments").add();
			property("threadsNumber").add();
			property("peakCaller").tags( "macs2", "hotspot2" ).add();
			property("onlyHuman").add();
			property("byCells").add();
			property("bySpecies").add();
			property("pathToOutputDir").add();
		}
	}

	private static double getMaxHeight(double[] profile)
	{
		double max = -1;
		for(int i = 0; i < profile.length; i++)
		{
			if(profile[i] > max) max = profile[i];
		}
		return max;
	}
}
