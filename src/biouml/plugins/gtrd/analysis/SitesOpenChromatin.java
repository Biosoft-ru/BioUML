package biouml.plugins.gtrd.analysis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.access.GTRDStatsAccess;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;


public class SitesOpenChromatin extends AnalysisMethodSupport<SitesOpenChromatin.SitesOpenChromatinParameters>
{
	private static DataElementPath pathToOutputFolder;
	private String [] namesOfTracks;
	private String [][] dataForMatrix;
	volatile double[] massiveWithCounts;
	private int threadsNumber;
	private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	private int percentNumber;
	private String[] peaksNames;
	
	public SitesOpenChromatin(DataCollection<?> origin, String name)
	{
		super(origin, name, new SitesOpenChromatinParameters());
	}
	@Override
	public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception 
	{	
		pathToOutputFolder = parameters.getPathToOutputFolder();
		boolean isTrackOrFile = parameters.getTrackOrFile();
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
		//SqlTrack inputTrack = isTrackOrFile ? parameters.getInputChipTrack().getDataElement( SqlTrack.class ): null;
		SqlTrack inputDnaseTrack = parameters.getInputDnaseTrack().getDataElement( SqlTrack.class );
		threadsNumber = parameters.getThreadsNumber();
		TrackInfoTable tableWithTrackInfo = new TrackInfoTable();
		ChromatinInfoTable tableWithChromInfo = new ChromatinInfoTable();
		List<SqlTrack> inputChipTracks = new ArrayList<>();
		
		if (!isTrackOrFile)
		{
			SqlTrack inputTrack = parameters.getInputChipTrack().getDataElement( SqlTrack.class );
			inputChipTracks.add(inputTrack);
		}
		
		else
		{
			//DataCollection<SqlTrack> trackCollection = parameters.getPathToFolderWithChipFolders().optDataCollection(SqlTrack.class);
			peaksNames = parameters.getFoldersNames();
			ru.biosoft.access.core.DataElementPath pathwayToTracks = parameters.getPathToFolderWithChipFolders();
			for(int k = 0; k < peaksNames.length; k++)
			{	
				ru.biosoft.access.core.DataElementPath pathwayToTrack  = DataElementPath.create(pathwayToTracks + "/" + peaksNames[k]);
				inputChipTracks.add(pathwayToTrack.getDataElement(SqlTrack.class));
			}
		}
		
		namesOfTracks = new String[inputChipTracks.size()];
		dataForMatrix = new String[inputChipTracks.size()][1];
		log.info(formatter.format(new Date()) + "getting all sites");
		
		jobControl.setPreparedness(jobControl.getPreparedness() + 20);
		for (int j =0; j < inputChipTracks.size(); j++)
		{
			if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
			massiveWithCounts = new double[2];
			SqlTrack inputChipTrack = inputChipTracks.get(j);
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
					percentNumber = (int)80/threadsNumber;
				}
				
				else
				{
					mediaMassive.addAll(preSites.subList(prevIndex, currIndex));
					percentNumber = (int)inputChipTracks.size()/threadsNumber;
				}
				prevIndex = currIndex;
				allSites.add(mediaMassive);
			}
			
			
			
			if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
			TaskPool.getInstance().iterate(allSites, 
					new GettingSitesIntersectionsIterations(inputChipTrack, inputDnaseTrack), threadsNumber);
			
			String percentOfSites = String.format("%.3g",(massiveWithCounts[0]/preSites.size())*100);
			dataForMatrix[j] = 	new String[] {tableWithTrackInfo.getTrackInfoId(inputChipTrack.getCompletePath().toString()), 
        	           tableWithChromInfo.getChromatinInfoId(inputDnaseTrack.getCompletePath().toString()), massiveWithCounts[0] + "", percentOfSites};
			namesOfTracks[j] = inputChipTrack.getName();
			jobControl.setPreparedness(jobControl.getPreparedness() + 60/inputChipTracks.size());
		}
		
		DataMatrixString statMatrix = new DataMatrixString(namesOfTracks, new String[]{"track_info_id", "chromatin_info_id","overlapped_number","overlapped_percents"}, dataForMatrix);
		
		if(parameters.isRecordToGtrdf())
			if(parameters.isWriteNewMatrixToGTRD())
				GTRDStatsAccess.writeTableFromDataMatrix(statMatrix,parameters.getPrefix() + "_scores", "track_name",
						new String[] {"VARCHAR(255)","VARCHAR(255)","FLOAT", "FLOAT"}, "gtrd_id", true, true);
			else
				GTRDStatsAccess.writeTableFromDataMatrix(statMatrix,parameters.getNameOfExistedTable(),"track_name", 
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
				log.info(formatter.format(new Date()) + Thread.currentThread().getId() + " thread started");
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
				log.info(formatter.format(new Date()) + Thread.currentThread().getId() + " thread finished");
			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}
	
	 public static class FoldersNamesSelector extends GenericMultiSelectEditor
	    {
	        @Override
	        protected String[] getAvailableValues()
	        {
	            try
	            {
	                DataCollection<DataElement> folders = ((SitesOpenChromatinParameters)getBean()).getPathToFolderWithChipFolders().getDataCollection(DataElement.class);
	                String[] foldersNames = folders.getNameList().toArray(new String[0]);
	                Arrays.sort(foldersNames, String.CASE_INSENSITIVE_ORDER);
	                return foldersNames;
	            }
	            catch( RepositoryException e )
	            {
	                return new String[]{"(please select folder with folders)"};
	            }
	            catch( Exception e )
	            {
	                return new String[]{"(folder doesn't contain the folders)"};
	            }
	        }
	    }
	 
	public static class SitesOpenChromatinParameters extends AbstractAnalysisParameters
	{	
		private String[] foldersNames;
		private DataElementPath pathToFolderWithChipFolders;
		//private DataElementPath pathToFolderWithDnaseFolders;
		private DataElementPath inputChipTrack, pathToOutputFolder;
		private DataElementPath inputDnaseTrack;
		private int threadsNumber = 1;
		private String prefix = "test";
		private boolean trackOrFile = true;
		private boolean chooseFile = false;
        private boolean recordToGtrdf = false;
        private boolean hiddenRecordToGTRD = true;
        private boolean writeNewMatrixToGTRD = false;
        private boolean writeToExistedTable = false;
        private boolean hiddenNameOfExistedTable = true;
        private String  nameOfExistedTable;
        private boolean hiddenOutputFolder = false;
		
		

		SitesOpenChromatinParameters()
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
		public boolean isWriteNewMatrixToGTRD() 
		{
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
			if(!recordToGtrdf)
			{
				setWriteNewMatrixToGTRD(false);
				setWriteToExistedTable(false);
			}
				
			firePropertyChange("*", oldValue, recordToGtrdf);

		}

		public boolean getChooseFile() 
		{
			return chooseFile;
		}

		public void setChooseFile(boolean chooseFile) 
		{
			Object oldValue = this.chooseFile;
			this.chooseFile = chooseFile;
			firePropertyChange("*", oldValue, chooseFile);
		}
		
		
		@PropertyName("More then 1 track")
		@PropertyDescription("Choose if you need more then 1 track")
		public boolean getTrackOrFile() 
		{
			return trackOrFile;
		}

		public void setTrackOrFile(boolean trackOrFile) 
		{
			Object oldValue = this.trackOrFile;
			this.trackOrFile = trackOrFile;
			setChooseFile(!trackOrFile);
			firePropertyChange("*", oldValue, trackOrFile);
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
		
		@PropertyName("Number of threads")
        @PropertyDescription("Number of threads")
		public int getThreadsNumber() {
			return threadsNumber;
		}
		
		@PropertyName("Folder names")
        @PropertyDescription("Folder names")
        public String[] getFoldersNames()
        {
            return foldersNames;
        }
        public void setFoldersNames(String[] foldersNames)
        {
            Object oldValue = this.foldersNames;
            this.foldersNames = foldersNames;
            firePropertyChange("foldersNames", oldValue, foldersNames);
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

		
		@PropertyName("Input SQL Dnase Track")
		@PropertyDescription("Input Dnase ChIP Track")
		public DataElementPath getInputDnaseTrack() 
		{
			return inputDnaseTrack;
		}
		
		public void setInputDnaseTrack(DataElementPath inputDnaseTrack) 
		{
			this.inputDnaseTrack = inputDnaseTrack;
		}

		@PropertyName("Input SQL ChIP Track")
		@PropertyDescription("Input SQL ChIP Track")
		public DataElementPath getInputChipTrack() {
			return inputChipTrack;
		}
		public void setInputChipTrack(DataElementPath inputTrack) 
		{
			this.inputChipTrack = inputTrack;
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
		
		@PropertyName("path to output file")
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
	public static class SitesOpenChromatinParametersBeanInfo extends BeanInfoEx2<SitesOpenChromatinParameters>
	{
		public SitesOpenChromatinParametersBeanInfo()
		{
			super(SitesOpenChromatinParameters.class);
		}

		@Override
		protected void initProperties() throws Exception
		{
			
			property("inputChipTrack").inputElement( SqlTrack.class ).hidden("getTrackOrFile").add();
			property("inputDnaseTrack").inputElement( SqlTrack.class ).add();
			property(DataElementPathEditor.registerInput("pathToFolderWithChipFolders", beanClass, FolderCollection.class)).hidden("getChooseFile").add();
			property("foldersNames").editor(FoldersNamesSelector.class).hidden("getChooseFile").add();
			property("prefix").add();
			property("threadsNumber").add();
			property("trackOrFile").add();
			property("recordToGtrdf").add();
            property("writeNewMatrixToGTRD").hidden("isHiddenRecordToGTRD").add();
            property("writeToExistedTable").hidden("isHiddenRecordToGTRD").add();
            property("nameOfExistedTable").hidden("isHiddenNameOfExistedTable").add();
            property("pathToOutputFolder").inputElement( FolderCollection.class ).hidden("isHiddenOutputFolder").add();
		}
	}
}
