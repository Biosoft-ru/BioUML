package biouml.plugins.gtrd.analysis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.ensembl.tracks.RepeatTrack;
import biouml.plugins.ensembl.tracks.TranscriptsTrack;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.access.GTRDStatsAccess;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.ChrIntervalMap;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SiteGenomeLocationForFolders extends AnalysisMethodSupport<SiteGenomeLocationForFolders.SiteGenomeLocationForFoldersParameters> 
{
	volatile double[] massiveWithCountTranscripts;
	private int threadsNumber;
	volatile HashMap<String,Double> dicWithRepeatsTypes;
	volatile ChrIntervalMap<String> transcriptGeneratedSites;
	public String[] columnsType;
	
	public SiteGenomeLocationForFolders (DataCollection<?> origin, String name)

	{
		super( origin, name, new SiteGenomeLocationForFoldersParameters() );
	}

	@Override
	public DataCollection<?> justAnalyzeAndPut() throws InterruptedException, ExecutionException, ClassNotFoundException, SQLException
	{

		RepeatTrack repeats = null;
		ArrayList<String> arrayTrackNames = new ArrayList<>();
		ArrayList<double[]> countTransForTrack = new ArrayList<>();
		ArrayList<HashMap<String, Double>> countRepForTrack = new ArrayList<>();
		int upstream = 0;
		int downstream = 0;
		ArrayList<Integer> allSitesInTracks;
		ArrayList<String> tracksInfoId = new ArrayList<>();
		TrackInfoTable tableWithTrackInfo = new TrackInfoTable();
		threadsNumber = parameters.getNumberOfTreads();
		//make arrays for iterations
		//1. massive with repeats
		//2. massive with TSS


		if(parameters.isCountRepeats())
		{
			if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
			repeats = parameters.getEnsembelRepeats().getDataElement(RepeatTrack.class);
		}

		if(parameters.isCountTSS())
		{

			if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
			upstream = parameters.getUpstream();
			downstream = parameters.getDownstream();
			TranscriptsTrack transTrack = parameters.getEnsembelTrastcrips().getDataElement(TranscriptsTrack.class); 
			int numberOfThreads = threadsNumber;
			if (parameters.getEnsembelSeq().getChildren().size() < threadsNumber)
				numberOfThreads = parameters.getEnsembelSeq().getChildren().size();
			transcriptGeneratedSites = new ChrIntervalMap<>(); 
			TaskPool.getInstance().iterate(parameters.getEnsembelSeq().getChildren(), 
					new MakingTrackIterations(transTrack,upstream, downstream), numberOfThreads);
		}
		
		allSitesInTracks = new ArrayList<>();
		List<ChIPseqExperiment> experiments = parsForExperiments(parameters.getPathToFolderWithChipExp(), parameters.getOrganism());
		jobControl.setPreparedness(jobControl.getPreparedness() + 20);
		
		for(ChIPseqExperiment exp : experiments)
		{
			if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
			String peakId = exp.getPeakId();
			
			for(String peakType : new String[] {"macs", "pics", "sissrs", "gem", "macs2"})
			{
				ru.biosoft.access.core.DataElementPath peakPath = DataElementPath.create( "databases/GTRD" ).getChildPath( "Data", "peaks", peakType, peakId );
				if(!peakPath.exists())
					continue;
				else
				{
					
					SqlTrack track = peakPath.getDataElement(SqlTrack.class);
					analyseChipTrack(track, repeats, arrayTrackNames, countTransForTrack, countRepForTrack, allSitesInTracks, 
							tracksInfoId, tableWithTrackInfo, peakType);
				}
			}
			jobControl.setPreparedness(jobControl.getPreparedness() + 60/experiments.size());

		}

		DataMatrixString resulTable = createFinallMatrixFromArrays(arrayTrackNames, countTransForTrack,
				countRepForTrack, parameters.isCountTSS(), parameters.isCountRepeats(), allSitesInTracks,
				tracksInfoId);

		if(parameters.isRecordToGtrdf())
			if(parameters.isWriteNewMatrixToGTRD())
				GTRDStatsAccess.writeTableFromDataMatrix(resulTable, parameters.getOutputName(), "track_name", columnsType, "gtrd_id", true, true);
			else
				GTRDStatsAccess.writeTableFromDataMatrix(resulTable,parameters.getNameOfExistedTable(),"track_name", columnsType, "gtrd_id", false, false);
				
		else
			resulTable.writeDataMatrixString(false, parameters.getPathToOutputFolder(), parameters.getOutputName() + "_result_table", log);
		
		return parameters.getPathToOutputFolder()== null? null : parameters.getPathToOutputFolder().getDataCollection();
	}

//methods for main class
	
	public void analyseChipTrack(SqlTrack track, RepeatTrack repeats,
			ArrayList<String> arrayTrackNames, ArrayList<double[]> countTransForTrack, ArrayList<HashMap<String, Double>> countRepForTrack,
			ArrayList<Integer>allSitesInTracks, ArrayList<String>tracksInfoId, TrackInfoTable tableWithTrackInfo, String trackType) 
            throws ClassNotFoundException, SQLException, RepositoryException, InterruptedException, ExecutionException
	{
		dicWithRepeatsTypes = new HashMap<>();
		String[] repTypes = ((parameters.getEnsembelRepeats().getDataElement(RepeatTrack.class).getAllRepTypes()));
		for(String repTypeName : repTypes)
			dicWithRepeatsTypes.put(repTypeName, 0.0);
		massiveWithCountTranscripts = new double[2];
		
		int numberOfThreads = threadsNumber;
		if(track.getChromosomesPath().getChildren().size() < threadsNumber)
			numberOfThreads = track.getChromosomesPath().getChildren().size();
		if(parameters.isCountTSS())
			TaskPool.getInstance().iterate(track.getChromosomesPath().getChildren(), 
					new GettingTssIterations(track), numberOfThreads);
		if(parameters.isCountRepeats())
			TaskPool.getInstance().iterate(track.getChromosomesPath().getChildren(), 
					new GettingRepeatsIterations(repeats, track), numberOfThreads);


		arrayTrackNames.add(track.getName()+ "_" + trackType);
		double trPercent = 0;
		if(massiveWithCountTranscripts[0] != 0)
			trPercent = (massiveWithCountTranscripts[0]/track.getSize())*100;
		massiveWithCountTranscripts[1] = trPercent;
		countTransForTrack.add(massiveWithCountTranscripts);
		countRepForTrack.add(dicWithRepeatsTypes);
		allSitesInTracks.add(track.getSize());
		tracksInfoId.add(tableWithTrackInfo.getTrackInfoId(track.getCompletePath().toString()));
	}
	
	public  DataMatrixString createFinallMatrixFromArrays(ArrayList<String> arrayTrackNames, 
			ArrayList<double[]> dataForTranscr,ArrayList<HashMap<String, Double>> dataForRepeats, 
			boolean transcripts, boolean repeats, ArrayList<Integer> numberOfSites,ArrayList<String> tracksID)
	{
		columnsType = new String[1 + dataForTranscr.get(0).length*2 + dataForRepeats.get(0).size()*2];
		DataMatrixString result;
		String[] rowsNames = new String[arrayTrackNames.size()];
		String[] columnsNames;
		int numberOfRows = 3;
		if(repeats)
			numberOfRows = (dataForRepeats.get(0).size()*2)+3;
		String [][] columnsValues = new String[arrayTrackNames.size()][numberOfRows];
		columnsNames = new String[(dataForRepeats.get(0).size()*2)+3];
		columnsNames[0] = "track_info_id";
		columnsType[0] = "VARCHAR(255)";
		
		for(int i =0; i < arrayTrackNames.size(); i++)
		{
			rowsNames[i] = arrayTrackNames.get(i);
			columnsValues[i][0] = tracksID.get(i);
			if(transcripts)
			{
				columnsNames[1] = "overlapped_with_tss";
				columnsNames[2] = "percent_of_overlapped_with_tss";
				columnsValues[i][1] = dataForTranscr.get(i)[0] + "";
				columnsValues[i][2] = String.format("%.3g",dataForTranscr.get(i)[1]);
				columnsType[1] = "FLOAT";
				columnsType[2] = "FLOAT";
			}			
			if(repeats)
			{	
				int a = 1;
				if(transcripts)
					a = 3;
				for(Entry<String, Double> repData : dataForRepeats.get(i).entrySet())
				{
					
					String key = repData.getKey();
					columnsNames[a] = "overlapped_with_repeats_type_" + key.replaceAll("\\s+", "_").replace('/', '_').toLowerCase();
					columnsNames[a+1] = "percent_of_overlapped_with_repeats_type_" + key.replaceAll("\\s+", "_").replace('/', '_').toLowerCase();
					double val = repData.getValue();
					String percenRep = 0 + "";
					if(val != 0)
						percenRep = String.format("%.3g",((val/numberOfSites.get(i))*100));
					columnsValues[i][a] = val + "";
					columnsValues[i][a+1] = percenRep;
					columnsType[a] = "FLOAT";
					columnsType[a+1] = "FLOAT";
					a = a +2;
				}
			
			}
			
			
		}
		
		result = new DataMatrixString(rowsNames, columnsNames,columnsValues);

		return result;
	}

	public static List<ChIPseqExperiment> parsForExperiments(DataElementPath pathway, String orgnism)
	{
		DataCollection<ChIPseqExperiment> inputChipTracks = pathway.getDataCollection(ChIPseqExperiment.class);
		List<ChIPseqExperiment> expNotControls = new ArrayList<>();
		for(ChIPseqExperiment exper : inputChipTracks)
		{
			if(!exper.isControlExperiment())
			{
				if(exper.getSpecie().getLatinName().equals(orgnism))
					expNotControls.add(exper);
			}
		}
		return expNotControls;
	}

	class GettingTssIterations implements Iteration<ru.biosoft.access.core.DataElementPath>
	{		
		SqlTrack sqlInputTrack;
		int upstream;
		int downstream;
		GettingTssIterations(SqlTrack sqlInputTrack)
		{
			this.sqlInputTrack = sqlInputTrack;
		}

		@Override
		public boolean run(DataElementPath chrPath) 
		{
			try
			{
				double counter = 0;
				Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
				int chrStart = chrSeq.getStart();
				int chrEnd = chrSeq.getLength();
				String chrName = chrSeq.getName();
				DataCollection<Site> chipSites = sqlInputTrack.getSites(chrPath.toString(), chrStart, chrEnd);
				for(Site site : chipSites)
				{

					int siteStart = site.getFrom();
					int siteEnd = site.getTo();
					if(parameters.isChangeSiteSize())
					{
						int sitesSize = parameters.getSiteSize();
						if(site.getLength() < sitesSize)
						{
							int newBounds = sitesSize - site.getLength();
							int newStart = site.getFrom() - (int)(newBounds/2);
							int newEnd = site.getTo() + (newBounds - (int)(newBounds/2));
							if(newStart < chrStart)
								newStart = chrStart;
							if(newEnd > chrEnd)
								newEnd = chrEnd;
							siteStart = newStart;
							siteEnd = newEnd;
						}
						
					}
					Collection<String> findsite = transcriptGeneratedSites.getIntervals(chrName, siteStart, siteEnd);
					if (!findsite.isEmpty())
						counter++;
				}
				synchronized(massiveWithCountTranscripts)
				{

					massiveWithCountTranscripts[0] += counter;
				}

			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}
	class MakingTrackIterations implements Iteration<ru.biosoft.access.core.DataElementPath>
	{		
		TranscriptsTrack transcriptTrack;
		int upstream;
		int downstream;
		MakingTrackIterations(TranscriptsTrack transcript, int upstream, int downstream)
		{
			this.transcriptTrack = transcript;
			this.upstream = upstream;
			this.downstream = downstream;

		}

		@Override
		public boolean run(DataElementPath chrPath) 
		{
			try
			{
				Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
				int chrStart = chrSeq.getStart();
				int chrEnd = chrSeq.getLength();
				String chrName = chrSeq.getName();
				//IntervalMap<String> trSites = new IntervalMap<String>();
				DataCollection<Site> transcriptsSites = transcriptTrack.getSites(chrPath.toString(),
						chrStart, chrEnd);
				for(Site site:transcriptsSites) 
				{ 
					int newStart = site.getFrom() - upstream;
					int newLength = upstream + downstream;
					if(newStart < 0)
						newStart = 0;
					if((newStart + newLength) > chrSeq.getLength())
						newLength = chrSeq.getLength() - newStart;
			
					synchronized(transcriptGeneratedSites)
					{

						transcriptGeneratedSites.add(chrName, newStart, newStart+ newLength,site.getProperties().getProperty("id").toString());
					}


				}

			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}

	class GettingRepeatsIterations implements Iteration<ru.biosoft.access.core.DataElementPath>
	{		
		RepeatTrack repInputTrack;
		SqlTrack sqlInputTrack;

		GettingRepeatsIterations(RepeatTrack repInput, SqlTrack sqlInput)
		{
			this.repInputTrack = repInput;
			this.sqlInputTrack = sqlInput;
		}

		@Override
		public boolean run(DataElementPath chrPath) 
		{
			try
			{
				HashMap<String,Double> counter = new HashMap<>();
				Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
				int chrStart = chrSeq.getStart();
				int chrEnd = chrSeq.getLength();
				DataCollection<Site> chipSites = sqlInputTrack.getSites(chrPath.toString(), chrStart, chrEnd);
				String chrName = chrSeq.getName(); 

				for(Site site:chipSites) 
				{ 

					int siteStart = site.getStart();
					int siteEnd  = site.getTo();
					if(parameters.isChangeSiteSize())
					{
						int sitesSize = parameters.getSiteSize();
						if(site.getLength() < sitesSize)
						{
							int newBounds = sitesSize - site.getLength();
							int newStart = site.getFrom() - (int)(newBounds/2);
							int newEnd = site.getTo() + (newBounds - (int)(newBounds/2));
							if(newStart < chrStart)
								newStart = chrStart;
							if(newEnd > chrEnd)
								newEnd = chrEnd;
							siteStart = newStart;
							siteEnd = newEnd;
						}
					}
					
					DataCollection<Site> findsite = repInputTrack.getSites(chrPath.toString(),siteStart, siteEnd);
					if (!findsite.isEmpty()) 
					{ 
						ArrayList<String> masWithTypes = new ArrayList<>();
						for(Site elm : findsite)
						{

							String repType = elm.getProperties().getProperty("Type").getValue().toString();
							
							if(masWithTypes.contains(repType))
								continue;
							else
							{
								masWithTypes.add(repType);
								if(dicWithRepeatsTypes.containsKey(repType))
								{
									if(counter.containsKey(repType))
									{
										double oldVal = counter.get(repType);
										counter.put(repType, oldVal + 1);
									}
									else
									{
										counter.put(repType, 1.0);
									}
								}
								else
									continue;
							}
						}
					}
				}

				synchronized(dicWithRepeatsTypes)
				{
					for(String siteType : counter.keySet())
					{
						double chrVal = counter.get(siteType);
						double value = dicWithRepeatsTypes.get(siteType);
						dicWithRepeatsTypes.put(siteType, value + chrVal);
					}
				}

			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}


	public static class OrganismEditor extends StringTagEditor
	{
		@Override
		public String[] getTags()
		{
			ru.biosoft.access.core.DataElementPath pathToSpecies = DataElementPath.create("databases/Utils/Species");
			DataCollection<Species> species = pathToSpecies.getDataCollection(Species.class);
			String [] organisms = new String[species.getSize()];
			int i = 0;
			for (Species sp : species)
			{
				organisms[i] = sp.getLatinName();
				i++;
			}
			return organisms;
			
		}
	}
	
	public static class SiteGenomeLocationForFoldersParameters extends AbstractAnalysisParameters
	{
		private String organism;
		private DataElementPath ensembelTrastcrips;
		private DataElementPath ensembelRepeats;
		private int upstream = 1000;
		private int downstream = 100;
		private boolean countRepeats = true;
		private boolean countTSS = true;
		private int numberOfTreads = 10;
		private DataElementPath pathToOutputFolder;
		private boolean hiddenStreams = false;
		private DataElementPath ensembelSeq;
		private String outputName;
		private boolean changeSiteSize = false;
		private boolean hiddenSiteSizeChange = true;
		private int siteSize = 20;
		private boolean recordToGtrdf = false;
        private boolean hiddenRecordToGTRD = true;
        private boolean writeNewMatrixToGTRD = false;
        private boolean writeToExistedTable = false;
        private boolean hiddenNameOfExistedTable = true;
        private String  nameOfExistedTable;
        private boolean hiddenOutputFolder = false;
        private DataElementPath pathToFolderWithChipExp;
        
		public SiteGenomeLocationForFoldersParameters()
		{

		}
		
		
		@PropertyName("path to input folder")
        @PropertyDescription("path to folder with experiments")
		public DataElementPath getPathToFolderWithChipExp() 
		{
			return pathToFolderWithChipExp;
		}

		public void setPathToFolderWithChipExp(DataElementPath pathToFolderWithChipExp) 
		{
			this.pathToFolderWithChipExp = pathToFolderWithChipExp;
		}



		public boolean isHiddenOutputFolder() {
			return hiddenOutputFolder;
		}



		public void setHiddenOutputFolder(boolean hiddenOutputFolder) {
			this.hiddenOutputFolder = hiddenOutputFolder;
		}

		
        public boolean isHiddenNameOfExistedTable() {
			return hiddenNameOfExistedTable;
		}



		public void setHiddenNameOfExistedTable(boolean hiddenNameOfExistedTable) 
		{
			Object oldValue = this.hiddenNameOfExistedTable;
			this.hiddenNameOfExistedTable = hiddenNameOfExistedTable;
			firePropertyChange("*", oldValue, hiddenNameOfExistedTable);
		}



		public boolean isHiddenRecordToGTRD() {
			return hiddenRecordToGTRD;
		}



		public void setHiddenRecordToGTRD(boolean hiddenRecordToGTRD) 
		{
			Object oldValue = this.hiddenRecordToGTRD;
			this.hiddenRecordToGTRD = hiddenRecordToGTRD;
			firePropertyChange("*", oldValue, hiddenRecordToGTRD);
		}

		@PropertyName("write new table")
        @PropertyDescription("record new table to GTRD database")
		public boolean isWriteNewMatrixToGTRD() {
			return writeNewMatrixToGTRD;
		}

		public void setWriteNewMatrixToGTRD(boolean writeNewMatrixToGTRD) 
		{
			Object oldValue = this.writeNewMatrixToGTRD;
			this.writeNewMatrixToGTRD = writeNewMatrixToGTRD;
			firePropertyChange("*", oldValue, writeNewMatrixToGTRD);
		}


		@PropertyName("write data to existed table")
        @PropertyDescription("record data to existed table in GTRD database")
		public boolean isWriteToExistedTable() {
			return writeToExistedTable;
		}
		public void setWriteToExistedTable(boolean writeToExistedTable) 
		{
			Object oldValue = this.writeToExistedTable;
			this.writeToExistedTable = writeToExistedTable;
			if(isRecordToGtrdf())
				setWriteNewMatrixToGTRD(!writeToExistedTable);
			setHiddenNameOfExistedTable(!writeToExistedTable);
			firePropertyChange("*", oldValue, writeToExistedTable);
		}


		public String getNameOfExistedTable() {
			return nameOfExistedTable;
		}

		@PropertyName("name of existed table")
        @PropertyDescription("name of existed table from GTRD database")
		public void setNameOfExistedTable(String nameOfExistedTable) 
		{
			this.nameOfExistedTable = nameOfExistedTable;
		}


		@PropertyName("Record result to GTRD database")
        @PropertyDescription("Record result to GTRD database")
		public boolean isRecordToGtrdf() {
			return recordToGtrdf;
		}


		public void setRecordToGtrdf(boolean recordToGtrdf) 
		{
			Object oldValue = this.recordToGtrdf;
			this.recordToGtrdf = recordToGtrdf;
			setHiddenRecordToGTRD(!recordToGtrdf);
			setHiddenOutputFolder(recordToGtrdf);
			if(!recordToGtrdf)
			{
				setWriteNewMatrixToGTRD(false);
				setWriteToExistedTable(false);
			}
				
			firePropertyChange("*", oldValue, recordToGtrdf);

		}

		
		
		@PropertyName("Change sites size")
        @PropertyDescription("change miniml size of the sites")
		public boolean isChangeSiteSize() {
			return changeSiteSize;
		}

		public void setChangeSiteSize(boolean changeSiteSize) {
			this.changeSiteSize = changeSiteSize;
			setHiddenSiteSizeChange(!changeSiteSize);
		}



		public boolean isHiddenSiteSizeChange() {
			return hiddenSiteSizeChange;
		}



		public void setHiddenSiteSizeChange(boolean hiddenSiteSizeChange) {
			this.hiddenSiteSizeChange = hiddenSiteSizeChange;
		}

		@PropertyName("write sites size")
        @PropertyDescription("write miniml size of the sites")
		public int getSiteSize() {
			return siteSize;
		}

		public void setSiteSize(int siteSize) {
			this.siteSize = siteSize;
		}

		@PropertyName("output name")
		@PropertyDescription("write name of putput file")
		public String getOutputName() {
			return outputName;
		}

		public void setOutputName(String outputName) {
			this.outputName = outputName;
		}



		public DataElementPath getEnsembelSeq() {
			return ensembelSeq;
		}


		public void setEnsembelSeq(DataElementPath ensembelSeq) {
			this.ensembelSeq = ensembelSeq;
		}


		public boolean isHiddenStreams() {
			return hiddenStreams;
		}



		public void setHiddenStreams(boolean hiddenStreams) {
			this.hiddenStreams = hiddenStreams;

		}

		@PropertyName("path to output folder")
        @PropertyDescription("path to output folder")
		public DataElementPath getPathToOutputFolder() {
			return pathToOutputFolder;
		}

		public void setPathToOutputFolder(DataElementPath pathToOutputFolder) {
			this.pathToOutputFolder = pathToOutputFolder;
		}

		@PropertyName("number of threads")
        @PropertyDescription("number of threads")
		public int getNumberOfTreads() {
			return numberOfTreads;
		}

		public void setNumberOfTreads(int numberOfTreads) {
			this.numberOfTreads = numberOfTreads;
		}

		@PropertyName("Count repeats in track")
		@PropertyDescription("Count repeats in track")

		public boolean isCountRepeats() {
			return countRepeats;
		}
		
		public void setCountRepeats(boolean countRepeats) {
			this.countRepeats = countRepeats;
		}


		@PropertyName("Count TSS in track")
		@PropertyDescription("Count TSS in track") 
		public boolean isCountTSS() {
			return countTSS;
		}

		public void setCountTSS(boolean countTSS) {
			this.countTSS = countTSS;
			setHiddenStreams(!countTSS);
		}


		@PropertyName("Upstream")
		@PropertyDescription("Choose - N nucleotides from TSS")

		public int getUpstream() {
			return upstream;
		}


		public void setUpstream(int upstream) {
			this.upstream = upstream;
		}

		@PropertyName("Downstream")
		@PropertyDescription("Choose + N nucleotides from TSS")

		public int getDownstream() {
			return downstream;
		}

		public void setDownstream(int downstream) {
			this.downstream = downstream;
		}

		@PropertyName("track names")
		@PropertyDescription("track names")

		public DataElementPath getEnsembelTrastcrips() 
		{
			return ensembelTrastcrips;
		}

		public void setEnsembelTrastcrips(DataElementPath ensembelTrastcrips) 
		{
			this.ensembelTrastcrips = ensembelTrastcrips;
		}

		public DataElementPath getEnsembelRepeats() 
		{
			return ensembelRepeats;
		}

		public void setEnsembelRepeats(DataElementPath ensembelRepeats) 
		{
			this.ensembelRepeats = ensembelRepeats;
		}

		@PropertyName("organism")
        @PropertyDescription("choose organism, for wich you will analyze tracks")
		public String getOrganism() 
		{
			return organism;
		}

		public void setOrganism(String organism)
		{
			Object oldValue = this.organism;
			this.organism = organism;
			
			ru.biosoft.access.core.DataElementPath pathToSpecies = DataElementPath.create("databases/Utils/Species");
			DataCollection<Species> species = pathToSpecies.getDataCollection(Species.class);
			for (Species sp : species)
			{
				if(sp.getLatinName().equals(organism))
				{
					setEnsembelRepeats(DataElementPath.create( sp.getAttributes().getProperty("ensemblPath").getValue().toString() + "/Tracks/Repeats" ));
					setEnsembelTrastcrips(DataElementPath.create( sp.getAttributes().getProperty("ensemblPath").getValue().toString() + "/Tracks/Transcripts"));
					setEnsembelSeq(DataElementPath.create(DataElementPath.create(sp.getAttributes().getProperty("ensemblPath").getValue().toString() + "/Sequences").getChildren().toString()));
				}
			}
			firePropertyChange("*", oldValue, organism);
		}
	}



	public static class SiteGenomeLocationForFoldersParametersBeanInfo extends BeanInfoEx2<SiteGenomeLocationForFoldersParameters>
	{
		public SiteGenomeLocationForFoldersParametersBeanInfo()
		{
			super(SiteGenomeLocationForFoldersParameters.class);
		}

		@Override
		protected void initProperties() throws Exception
		{
			add(new PropertyDescriptorEx("organism", beanClass), OrganismEditor.class);
			property("pathToFolderWithChipExp").inputElement(SqlDataCollection.class).add();
			property("changeSiteSize").add();
			property("siteSize").hidden("isHiddenSiteSizeChange").add();
			property("countRepeats").add();
			property("countTSS").add();
			property("upstream").hidden("isHiddenStreams").add();
			property("downstream").hidden("isHiddenStreams").add();
			property("numberOfTreads").add();
			property("outputName").add();
			property("recordToGtrdf").add();
            property("writeNewMatrixToGTRD").hidden("isHiddenRecordToGTRD").add();
            property("writeToExistedTable").hidden("isHiddenRecordToGTRD").add();
            property("nameOfExistedTable").hidden("isHiddenNameOfExistedTable").add();
			property("pathToOutputFolder").inputElement( FolderCollection.class ).hidden("isHiddenOutputFolder").add();

		}
	}
}
