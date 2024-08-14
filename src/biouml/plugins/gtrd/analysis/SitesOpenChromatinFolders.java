package biouml.plugins.gtrd.analysis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.access.GTRDStatsAccess;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import java.util.HashMap;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SitesOpenChromatinFolders extends AnalysisMethodSupport<SitesOpenChromatinFolders.SitesOpenChromatinFoldersParameters>
{
	private static DataElementPath pathToOutputFolder;
	private List<String> namesOfTracks;
	private List<String[]> dataForMatrix;
	volatile double[] massiveWithCounts;
	private int threadsNumber;
	private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	private int percentNumber;

	public SitesOpenChromatinFolders(DataCollection<?> origin, String name)
	{
		super(origin, name, new SitesOpenChromatinFoldersParameters());
	}
	@Override
	public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception 
	{	
		pathToOutputFolder = parameters.getPathToOutputFolder();
		if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
		threadsNumber = parameters.getThreadsNumber(); 
		DataCollection<ChIPseqExperiment> inputChipTracks = parameters.getPathToFolderWithChipFolders().getDataCollection(ChIPseqExperiment.class);
		ru.biosoft.access.core.DataElementPath pathwayToDnase = parameters.getPathToFolderWithDnaseFolders();
		DataCollection<DataElement> dnaseCollection = pathwayToDnase.getDataCollection(DataElement.class);
		TrackInfoTable tableWithTrackInfo = new TrackInfoTable();
		ChromatinInfoTable tableWithChromInfo = new ChromatinInfoTable();
		HashMap<String,SqlTrack> dnaseHashMap = getDnaseHashMap(dnaseCollection, pathwayToDnase, log);
		
		log.info(formatter.format(new Date()) + "getting all sites");
		
		if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
		List<ChIPseqExperiment> expNotControls = new ArrayList<>();
		int j = 0;
		
		for(ChIPseqExperiment exper : inputChipTracks)
		{
			if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
			log.info(formatter.format(new Date()) + "_" +j + "_experiments_from_Experiments_folder" + 
			inputChipTracks.getSize() + "_are taken");
			j++;
			if(!exper.isControlExperiment())
			{
				expNotControls.add(exper);
			}
		}
		
		namesOfTracks = new ArrayList<>();
		dataForMatrix = new ArrayList<>();
		log.info(formatter.format(new Date()) + "_start analysis");
		
		jobControl.setPreparedness(jobControl.getPreparedness() + 20);
		for (ChIPseqExperiment e : expNotControls)
		{
			String expCellId = e.getCell().getName();
			String peakId = e.getPeak().getName();
			
			//-2 if no Dnase for cell line, -1 if no peakcaller 
			if(!dnaseHashMap.containsKey(expCellId))
			{
				
				//dataForMatrix.add(new String[]{"-2","-2"});
				//namesOfTracks.add(peakId);
				continue;
			}

			for(String peakType : new String[] {"macs", "pics", "sissrs", "gem"})
			{
				
				massiveWithCounts = new double[2];
				ru.biosoft.access.core.DataElementPath peakPath = parameters.getGtrdPath().getChildPath( "Data", "peaks", peakType, peakId );

				if(!peakPath.exists())
				{
					//dataForMatrix.add(new String[]{"-1","-1"});
					//namesOfTracks.add(peakId + "_" + peakType);
					continue;
				}

				else
				{
					SqlTrack inputChipTrack = peakPath.getDataElement(SqlTrack.class);
					DataCollection<Site> chSites = inputChipTrack.getAllSites();
					List<List<Site>> allSites = new ArrayList<>();
					List<Site> preSites = new ArrayList<>();

					for (Site ssite : chSites)
					{
						preSites.add(ssite);
					}

					int groupSize;

					if(preSites.size()<= threadsNumber)
					{
						groupSize = preSites.size();
						threadsNumber = 1;
					}

					else
					{
						groupSize = (int) preSites.size()/threadsNumber;
					}

					int prevIndex = 0;
					int currIndex = 0;

					for(int i = 1; i <= threadsNumber; i++)
					{
						currIndex = i * groupSize;
						List<Site> mediaMassive = new ArrayList<>();
						if(i == threadsNumber)
						{
							mediaMassive.addAll(preSites.subList(prevIndex, preSites.size()));
							percentNumber = (int)80/threadsNumber*(1/expNotControls.size());
						}

						else
						{
							mediaMassive.addAll(preSites.subList(prevIndex, currIndex));
							percentNumber = (int)expNotControls.size()/threadsNumber*(1/expNotControls.size());
						}
						prevIndex = currIndex;
						allSites.add(mediaMassive);
					}



					log.info(formatter.format(new Date()) + " finished taking sites to array");
					if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
					TaskPool.getInstance().iterate(allSites, 
							new GettingSitesIntersectionsIterations(inputChipTrack, dnaseHashMap.get(expCellId)), threadsNumber);

					String percentOfSites;
					if (massiveWithCounts[0] == 0.0)
							percentOfSites = 0.0 + "";
					else
					{
						percentOfSites = String.format("%.3g",(massiveWithCounts[0]/preSites.size())*100);
					}
					dataForMatrix.add(new String[] {tableWithTrackInfo.getTrackInfoId(inputChipTrack.getCompletePath().toString()), 
	        	           tableWithChromInfo.getChromatinInfoId(dnaseHashMap.get(expCellId).getCompletePath().toString()), massiveWithCounts[0] + "", percentOfSites});
					namesOfTracks.add(peakId + "_" + peakType);
				}
				jobControl.setPreparedness(jobControl.getPreparedness() + 60/inputChipTracks.getSize());
			}
		}
		
		String[] massiveWithNames = new String[namesOfTracks.size()];
		String[][]massiveWithDataForMatrix = new String[dataForMatrix.size()][1];
		for (int p =0; p < namesOfTracks.size(); p++)
		{
			massiveWithNames[p] = namesOfTracks.get(p);
			massiveWithDataForMatrix[p] = dataForMatrix.get(p);
		}
		
		
		DataMatrixString statMatrix = new DataMatrixString(massiveWithNames, new String[]{"track_info_id", "chromatin_info_id","overlapped_number","overlapped_percents"}, massiveWithDataForMatrix);
		
		if(parameters.isRecordToGtrdf())
			if(parameters.isWriteNewMatrixToGTRD())
				GTRDStatsAccess.writeTableFromDataMatrix(statMatrix,parameters.getPrefix() + "_scores","track_name", 
						new String[] {"VARCHAR(255)","VARCHAR(255)","FLOAT", "FLOAT"}, "gtrd_id", true, true);
			else
				GTRDStatsAccess.writeTableFromDataMatrix(statMatrix, parameters.getNameOfExistedTable(),"track_name", 
						new String[] {"VARCHAR(255)","VARCHAR(255)","FLOAT", "FLOAT"}, "gtrd_id", false, false);
				
		else
			statMatrix.writeDataMatrixString(false, pathToOutputFolder,parameters.getPrefix() + "_intersectionNum", log);
		return pathToOutputFolder.getDataCollection();
	}
	class GettingSitesIntersectionsIterations implements Iteration<List<Site>>
	{		
		SqlTrack chInputTrack;
		SqlTrack dnInputTrack;

		GettingSitesIntersectionsIterations(SqlTrack chInputTrack, SqlTrack dnInputTrack)
		{
			this.chInputTrack = chInputTrack;
			this.dnInputTrack = dnInputTrack;
		}

		@Override
		public boolean run(List<Site> chSites) 
		{
			try
			{
				String chrName;
				double counter = 0;

				for(Site site:chSites) 
				{ 
					chrName = site.getSequence().getName();
					DataCollection<Site> findsite = dnInputTrack.getSites(chrName,site.getStart(), site.getTo());
					if (!findsite.isEmpty()) 
					{ counter++; 
					}

				}

				synchronized(massiveWithCounts)
				{

					massiveWithCounts[0] += counter;
				}

			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}

	private static HashMap<String,SqlTrack> getDnaseHashMap(DataCollection<DataElement> dnaseDataCollection, 
			ru.biosoft.access.core.DataElementPath pathwayToCollection, java.util.logging.Logger log)
	{
		HashMap<String,SqlTrack> dnaseHashMap = new HashMap<>(); 
		int j = 0;
		for (DataElement elm : dnaseDataCollection)
		{
			String trackName = elm.getName();
			String[] preCellId = trackName.split("_");
			String cellId = preCellId[1];
			ru.biosoft.access.core.DataElementPath pathToDnaseTrack = DataElementPath.create(pathwayToCollection + "/" + trackName);
			SqlTrack dnaseTrack = pathToDnaseTrack.getDataElement(SqlTrack.class);
			dnaseHashMap.put(cellId, dnaseTrack);
			log.info(formatter.format(new Date()) + "_" + j + "_Dnase tracks from_" 
			+ dnaseDataCollection.getSize() + "_are taken");
			j++;
		}
		return(dnaseHashMap);
	}

	public static class SitesOpenChromatinFoldersParameters extends AbstractAnalysisParameters
	{	
		private DataElementPath pathToFolderWithChipFolders;
		private DataElementPath pathToFolderWithDnaseFolders;
		private DataElementPath pathToOutputFolder;
		private DataElementPath gtrdPath = DataElementPath.create( "databases/GTRD" );
		private int threadsNumber = 1;
		private String prefix = "test";
		private boolean recordToGtrdf = false;
        private boolean hiddenRecordToGTRD = true;
        private boolean writeNewMatrixToGTRD = false;
        private boolean writeToExistedTable = false;
        private boolean hiddenNameOfExistedTable = true;
        private String  nameOfExistedTable;
        private boolean hiddenOutputFolder = false;

		SitesOpenChromatinFoldersParameters()
		{}
		
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
		public boolean isWriteToExistedTable() 
		{
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


		public String getNameOfExistedTable() 
		{
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
			if(!recordToGtrdf)
			{
				setWriteNewMatrixToGTRD(false);
				setWriteToExistedTable(false);
			}
				
			firePropertyChange("*", oldValue, recordToGtrdf);

		}

		public DataElementPath getGtrdPath()
		{
			return gtrdPath;
		}
		public void setGtrdPath(DataElementPath gtrdPath)
		{
			Object oldValue = this.gtrdPath;
			this.gtrdPath = gtrdPath;
			firePropertyChange( "gtrdPath", oldValue, gtrdPath );
		}


		@PropertyName("Path to ChipTracks")
		@PropertyDescription("Path to ChipTracks")

		public DataElementPath getPathToFolderWithChipFolders()
		{
			return pathToFolderWithChipFolders;
		}
		public void setPathToFolderWithChipFolders(DataElementPath pathToFolderWithChipFolders)
		{
			Object oldValue = this.pathToFolderWithChipFolders;
			this.pathToFolderWithChipFolders = pathToFolderWithChipFolders;
			firePropertyChange("pathToFolderWithChipFolders", oldValue, pathToFolderWithChipFolders);
		}


		@PropertyName("Dnase tracks")
		@PropertyDescription("Path to Dnase tracks")
		public DataElementPath getPathToFolderWithDnaseFolders()
		{
			return pathToFolderWithDnaseFolders;
		}
		public void setPathToFolderWithDnaseFolders(DataElementPath pathToFolderWithDnaseFolders)
		{
			Object oldValue = this.pathToFolderWithDnaseFolders;
			this.pathToFolderWithDnaseFolders = pathToFolderWithDnaseFolders;
			firePropertyChange("pathToFolderWithDnaseFolders", oldValue, pathToFolderWithDnaseFolders);
		}

		@PropertyName("Number of threads")
		@PropertyDescription("Number of threads")
		public int getThreadsNumber() {
			return threadsNumber;
		}

		public void setThreadsNumber(int threadsNumber) {
			if (threadsNumber <= 20 && threadsNumber >0)
			{
				this.threadsNumber = threadsNumber;
			}
			else if(threadsNumber <= 0)
			{
				this.threadsNumber = 1;
			}
			else
			{
				this.threadsNumber = 20;
			}
		}

		@PropertyName("prefix")
        @PropertyDescription("write prefix of output file")
		public String getPrefix() 
		{
			return prefix;
		}

		public void setPrefix(String prefix) 
		{
			this.prefix = prefix;
		}
		
		@PropertyName("path to output folder")
        @PropertyDescription("choose folder for output file")
		public DataElementPath getPathToOutputFolder()
		{
			return pathToOutputFolder;
		}

		public void setPathToOutputFolder(DataElementPath pathToOutputFolder)
		{
			Object oldValue = this.pathToOutputFolder;
			this.pathToOutputFolder = pathToOutputFolder;
			firePropertyChange("pathToOutputFolder", oldValue, pathToOutputFolder);
		}

	}
	public static class SitesOpenChromatinFoldersParametersBeanInfo extends BeanInfoEx2<SitesOpenChromatinFoldersParameters>
	{
		public SitesOpenChromatinFoldersParametersBeanInfo()
		{
			super(SitesOpenChromatinFoldersParameters.class);
		}

		@Override
		protected void initProperties() throws Exception
		{

			property("pathToFolderWithChipFolders").inputElement(SqlDataCollection.class).add();
			property("pathToFolderWithDnaseFolders").inputElement(FolderCollection.class).add();
			property("prefix").add();
			property("threadsNumber").add();
			property("recordToGtrdf").add();
            property("writeNewMatrixToGTRD").hidden("isHiddenRecordToGTRD").add();
            property("writeToExistedTable").hidden("isHiddenRecordToGTRD").add();
            property("nameOfExistedTable").hidden("isHiddenNameOfExistedTable").add();
            property("pathToOutputFolder").inputElement( FolderCollection.class ).hidden("isHiddenOutputFolder").add();
		}
	}
}

