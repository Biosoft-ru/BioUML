package biouml.plugins.gtrd.analysis;

import java.beans.PropertyDescriptor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import ru.biosoft.bsa.MergedTrack;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.UnionTrack;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;

public class DnaseProfileCreationOld extends AnalysisMethodSupport<DnaseProfileCreationOld.DnaseProfileCreationOldParameters>
{
	private PropertyDescriptor PROFILE_PD = StaticDescriptor.create("profile");
	private PropertyDescriptor MAX_PROFILE_HEIGHT_PD = StaticDescriptor.create( "maxProfileHeight" );
	private int threadsNumber;
	volatile SqlTrack resultTrack;

	public DnaseProfileCreationOld(DataCollection<?> origin, String name)
	{
		super(origin, name, new DnaseProfileCreationOldParameters());
	}

	@Override
	public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception {
		threadsNumber = parameters.getThreadsNumber();
		Date date;
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

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

		ru.biosoft.access.core.DataElementPath outputDir = parameters.getPathToOutputDir();
		List<SqlTrack> allMergedTracks = new ArrayList<>();
		log.info(expsGrouped.size() + " groups of DNase-seq experiments");

		int counter = 1;
		for(Entry<String, List<DNaseExperiment>> expsGroup : expsGrouped.entrySet())
		{
			List<Track> macsPeaks = new ArrayList<>();
			for(DNaseExperiment exp : expsGroup.getValue())
				for(DataElementPath peaksPath : exp.getMacsPeaks())
				{
					if(peaksPath.exists())
						macsPeaks.add(peaksPath.getDataElement(Track.class));
				}

			if(macsPeaks.size() == 0)
				continue;

			ru.biosoft.access.core.DataElementPath genomePath = TrackUtils.getTrackSequencesPath( macsPeaks.get( 0 ) );
			UnionTrack unionTrack = new UnionTrack("DNase_" + expsGroup.getKey(), macsPeaks.get(0).getOrigin(), macsPeaks);
			MergedTrack mergedTrack = new MergedTrack(unionTrack);

			String cellTitle = expsGroup.getValue().get(0).getCell().getTitle();
			date = new Date();
			log.info(formatter.format(date) + ": " + counter++ + ". " + cellTitle);
			ru.biosoft.access.core.DataElementPath resultTrackPath = DataElementPath.create( outputDir.getDataCollection(), expsGroup.getKey() + "_" + cellTitle + "_track");
			resultTrack = SqlTrack.createTrack(resultTrackPath, macsPeaks.get( 0 )); // merged track with profile

			date = new Date();
			log.info(formatter.format(date) + ": SiteProfileIteration in progress");
			TaskPool.getInstance().iterate(genomePath.getChildren(), new SiteProfileIteration(mergedTrack, unionTrack), threadsNumber);

			resultTrack.finalizeAddition();
			CollectionFactoryUtils.save(resultTrack);
			allMergedTracks.add(resultTrack);
		}

		/*double[][] distances = new double[allMergedTracks.size()][allMergedTracks.size()];
		for(int i = 0; i < allMergedTracks.size(); i++)
		{
			for(int j = 0; j < allMergedTracks.size(); j++)
			{
				if(i == j)
				{
					distances[i][j] = 0.0;
					break;
				}
				distances[i][j] = getDistance(allMergedTracks.get(i), allMergedTracks.get(j));
			}
		}
		 */
		return null;
	}

	class SiteProfileIteration implements Iteration<ru.biosoft.access.core.DataElementPath>
	{
		MergedTrack mergedTrack;
		UnionTrack unionTrack;

		SiteProfileIteration(MergedTrack mergedTrack, UnionTrack unionTrack)
		{
			this.mergedTrack = mergedTrack;
			this.unionTrack = unionTrack;
		}

		@Override
		public boolean run(DataElementPath element) {
			try
			{
				Sequence chrSeq = element.getDataElement( AnnotatedSequence.class ).getSequence();
				DataCollection<Site> sites = mergedTrack.getSites( element.toString(), 0, chrSeq.getLength() + chrSeq.getStart() );
				List<Site> results = new ArrayList<>();
				for( Site s : sites )
				{
					results.add( computeCoverage(s, unionTrack) );
				}
				synchronized(resultTrack)
				{
					for(Site result : results)
						resultTrack.addSite( result );
				}

			}
			catch (Exception e)
			{
				return false;
			}
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

	private Site computeCoverage(Site region, Track track) throws Exception
	{
		double[] profile = new double[region.getLength()];

		int regionFrom = region.getFrom();
		int regionTo = region.getTo();

		DataCollection<Site> sites = track.getSites(DataElementPath.create(region.getSequence()).toString(), regionFrom, regionTo);

		for( Site site : sites )
		{
			int from = site.getFrom();
			int to = site.getTo();

			from -= region.getFrom();
			to -= region.getFrom();

			if( from < 0 )
				from = 0;
			if( to >= region.getLength() )
				to = region.getLength() - 1;

			for( int pos = from; pos <= to; pos++ )
				profile[pos]++;
		}

		double max = 0;
		for(double x : profile)
			if(x > max)
				max = x;

		region.getProperties().add(new DynamicProperty( PROFILE_PD, double[].class, profile ));
		region.getProperties().add( new DynamicProperty( MAX_PROFILE_HEIGHT_PD, Double.class, max ) );

		return region;
	}

	public static class DnaseProfileCreationOldParameters extends AbstractAnalysisParameters
	{
		private DataElementPath pathToExperiments;
		private DataElementPath pathToOutputDir;
		private int binSize;
		private int threadsNumber = 1;
		private boolean bySpecies = false;
		private boolean byCells = false;
		private boolean onlyHuman = false;

		DnaseProfileCreationOldParameters()
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
	}

	public static class DnaseProfileCreationOldParametersBeanInfo extends BeanInfoEx2<DnaseProfileCreationOldParameters>
	{
		public DnaseProfileCreationOldParametersBeanInfo()
		{
			super(DnaseProfileCreationOldParameters.class);
		}

		@Override
		protected void initProperties() throws Exception
		{
			property("pathToExperiments").add();
			property("threadsNumber").add();
			property("onlyHuman").add();
			property("byCells").add();
			property("bySpecies").add();
			property("pathToOutputDir").add();
		}
	}
}
