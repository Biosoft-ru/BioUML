package biouml.plugins.gtrd.analysis;

import java.beans.PropertyDescriptor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.access.GTRDStatsAccess;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class ProfilesStats extends AnalysisMethodSupport<ProfilesStats.ProfilesStatsParameters>
{
    private static PropertyDescriptor MAX_PROFILE_HEIGHT_PD = StaticDescriptor.create( "maxProfileHeight" );
    private static DataElementPath pathToOutputFolder;
    private static String prefix;
	private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	volatile List<double[]> lengthAndHeight;
    private int percentNumbers;
    private String[][] dataForMatrix;
    private String[] namesOfTracks;
    private boolean heightOrNot;
	public ProfilesStats(DataCollection<?> origin, String name)
	{
		super(origin, name, new ProfilesStatsParameters());
	}
	
	@Override
    public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception 
	{
		pathToOutputFolder = parameters.getPathToOutputFolder();
		heightOrNot = parameters.isAddHight();
		prefix = parameters.getPrefix().equals("") ? "" : parameters.getPrefix() + "_";
		List<SqlTrack> inputChipTracks = new ArrayList<>();
		boolean fileOrFolder = parameters.isTrackOrFolder();
		TrackInfoTable tableWithTrackInfo = new TrackInfoTable();
		
		if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
		
		if(!fileOrFolder) inputChipTracks.add(parameters.getInputTrack().getDataElement( SqlTrack.class ));
		
		else
		{	
			ru.biosoft.access.core.DataElementPath pathwayToTracks = parameters.getPathToFolderWithFolders();
			if(parameters.getAllFolder())
			{
				DataCollection<DataElement> folderWithTracks = pathwayToTracks.getDataCollection(DataElement.class);
				for(DataElement track : folderWithTracks)
				{
					String trackName = track.getName();
					ru.biosoft.access.core.DataElementPath tracksPlace = DataElementPath.create(pathwayToTracks + "/" + trackName); 
					inputChipTracks.add(tracksPlace.getDataElement(SqlTrack.class));
				}
			}
			else
			{
				String[] peaksNames = parameters.getFoldersNames();
				for(int k = 0; k < peaksNames.length; k++)
				{	
					ru.biosoft.access.core.DataElementPath pathwayToTrack  = DataElementPath.create(pathwayToTracks + "/" + peaksNames[k]);
					inputChipTracks.add(pathwayToTrack.getDataElement(SqlTrack.class));
				}
			}
		
		}
		
		dataForMatrix = new String[inputChipTracks.size()][1];
		namesOfTracks = new String[inputChipTracks.size()];
		for(int t =0; t < inputChipTracks.size(); t++)
		{	
			lengthAndHeight = new ArrayList<>();
			SqlTrack inputTrack = inputChipTracks.get(t);
			try
			{
				if(inputTrack.getSize() != 0)
				{
					ru.biosoft.access.core.DataElementPath genomePath = TrackUtils.getTrackSequencesPath( inputTrack );
					//log.info(formatter.format(new Date()) + "getting all sites");
					jobControl.setPreparedness(10);
					if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
					percentNumbers = (int)70/parameters.getThreadsNumber()*(1/inputChipTracks.size());
					TaskPool.getInstance().iterate(genomePath.getChildren(), 
							new GettingPropertiesIteration(inputTrack), parameters.getThreadsNumber());
					if(!lengthAndHeight.isEmpty())
					{
						double[] length = new double[lengthAndHeight.size()];
						double[] height = new double[lengthAndHeight.size()];
						double[] values;
						double totalLenght = 0;
						double minLength = lengthAndHeight.get(0)[0];
						double maxLength = length[0];
						
						for(int i = 0; i < lengthAndHeight.size(); i++)
						{
							values = lengthAndHeight.get(i);
							length[i] = values[0];
							if (heightOrNot)
								height[i] = values[1];
							totalLenght = totalLenght + (int)length[i];
							if (minLength > length[i]) 
							{
								minLength = (int)length[i];
							}
							else if(maxLength < length[i]) 
							{
								maxLength = (int)length[i];
							}
						}
						String medianLength = String.format("%.3g",totalLenght/length.length);
						int size = length.length;
						String[] tracStat = new String[6];
						tracStat[0] = tableWithTrackInfo.getTrackInfoId(inputTrack.getCompletePath().toString());
						tracStat[1] = size + "";
						tracStat[2] = (int)totalLenght + "";
						tracStat[3] = (int)minLength + "";
						tracStat[4] = (int)maxLength + "";
						tracStat[5] = medianLength;
						dataForMatrix[t] = tracStat;
						String  trackName = inputTrack.getName(); 
						namesOfTracks[t] = trackName;
						if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
						
						boolean graphOrNot = parameters.isAddGraph();
						
						if (graphOrNot) 
						{
							boolean lenghtOrNot = parameters.isAddLengthAndHight();
							log.info(formatter.format(new Date()) + "getting lengthCurve");
							double[][][] lengthCurve= getCurve(length);		
							log.info(formatter.format(new Date()) + "creating the charts");
							Chart chart = ChartUtils.createChart(lengthCurve[0], lengthCurve[1], new String[]{"DHSs' length destribution", "50%", "95%", "99%"}, 
									null, null, null, null, "Length, bp", "Quantity", false);
							DataCollection<DataElement> parent  = pathToOutputFolder.getDataCollection();
							ChartDataElement chartDE = new ChartDataElement(prefix + "Length_distribution_of_" + trackName, parent, chart);
							parent.put(chartDE);
							
							if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
							
							if(heightOrNot)
							{   
								log.info(formatter.format(new Date()) + "getting heightCurve");
								double[][][] heightCurve= getCurve(height);
								chart = ChartUtils.createChart(heightCurve[0], heightCurve[1], new String[]{"DHSs' profile height destribution", "50%", "95%", "99%"}, 
										null, null, null, null, "Height, number of overlapped sites", "Quantity", false);
								chartDE = new ChartDataElement(prefix + "Height_distribution_of_" + trackName, parent, chart);
								parent.put(chartDE);
							}
							if(lenghtOrNot)
							{
								log.info(formatter.format(new Date()) + "getting heightCurve");
								chart = ChartUtils.createChart(null, null, null, length, height, null, null, "site's length", "site's height", false);
								chartDE = new ChartDataElement(prefix + "the_dependence_of_the_height_of_the_site_from_its_length_of_" + trackName, parent, chart);
								parent.put(chartDE);
				
							}
						}
					}
					else
					{
						dataForMatrix[t] = new String[]{tableWithTrackInfo.getTrackInfoId(inputTrack.getCompletePath().toString()),"0","0","0","0","0"};
						namesOfTracks[t] = inputTrack.getName(); 
					}
						
				}
				else
				{
					dataForMatrix[t] = new String[]{tableWithTrackInfo.getTrackInfoId(inputTrack.getCompletePath().toString()), "0","0","0","0","0"};
					namesOfTracks[t] = inputTrack.getName(); 
				}
			}
			catch (IndexOutOfBoundsException ex)
			{
				log.info(inputTrack.getName() + "_" + t);
				throw ex;
			}
		}
		jobControl.setPreparedness(90);
		
		DataMatrixString tracStatMatrix = new DataMatrixString(namesOfTracks, new String[]{"track_info_id","size", "total_length", 
				"minimal_lenght", "maximal_lenght", "median_length"}, dataForMatrix);
		
		if(parameters.isRecordToGtrdf())
			if(parameters.isWriteNewMatrixToGTRD())
				GTRDStatsAccess.writeTableFromDataMatrix(tracStatMatrix, parameters.getPrefix() + "_scores","track_name", 
						new String[] {"VARCHAR(255)","INTEGER","INTEGER", "INTEGER", "INTEGER","FLOAT"}, "gtrd_id", true, true);
			else
				GTRDStatsAccess.writeTableFromDataMatrix(tracStatMatrix, parameters.getNameOfExistedTable(),"track_name", 
						new String[] {"VARCHAR(255)","INTEGER","INTEGER", "INTEGER", "INTEGER","FLOAT"}, "gtrd_id", false, false);
				
		else
			tracStatMatrix.writeDataMatrixString(false, pathToOutputFolder,parameters.getPrefix() + "_Scores_" + "of_statistics", log);
		
		return pathToOutputFolder.getDataCollection();
	}

	class GettingPropertiesIteration implements Iteration<ru.biosoft.access.core.DataElementPath>
	{
		SqlTrack inputTrack;
		GettingPropertiesIteration(SqlTrack inputTrack)
		{
			this.inputTrack = inputTrack;
		}
		
		@Override
		public boolean run(DataElementPath element) 
		{
			try
			{
				//log.info(formatter.format(new Date()) + Thread.currentThread().getId() + " thread started");
				Sequence chrSeq = element.getDataElement( AnnotatedSequence.class ).getSequence();
				DataCollection<Site> sites = inputTrack.getSites( element.toString(), 0, chrSeq.getLength() + chrSeq.getStart() );
				if(sites.getSize() !=0)
				{
					List<double[]> results = new ArrayList<>();
					double height, length;
					for( Site site : sites )
					{
						length = (double) site.getLength();
						if(heightOrNot)
						{
							height = Double.parseDouble((site.getProperties().getProperty(MAX_PROFILE_HEIGHT_PD.getName()).getValue().toString()));
							results.add( new double[] {length, height} );
						}
						else
							results.add(new double[] {length});
					}
	                synchronized(lengthAndHeight)
	                {
	                    for(double[] result : results)
	                    	lengthAndHeight.add( result );
	                }
	                synchronized (jobControl) 
	                {
	                	jobControl.setPreparedness(jobControl.getPreparedness() + percentNumbers);
	                }
				}
				
				//log.info(formatter.format(new Date()) + Thread.currentThread().getId() + "_thread finished");
			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}
	
	private double[][][] getCurve(double[] arrays) {
		double counter;
		double[] array = arrays.clone();
		UtilsForArray.sortInAscendingOrder(array);
		LinkedList<Double> xValues = new LinkedList<>();
		LinkedList<Double> yValues = new LinkedList<>();
		double d = 1; //100.0 / (double) array.length;
		
		counter = d;
		double maxY = 0;
		int quantity = 0;
		int quantity50 = (int) ( 0.5 * array.length );
		int quantity95 = (int) ( 0.95 * array.length );
		int quantity99 = (int) ( 0.99 * array.length );
		double[][][] addLines = new double[2][3][2];
		for(int j = 0; j < array.length; j++)
		{
			quantity++;
			if(quantity == quantity50) 
			{
				addLines[0][0][0] = array[j];
				addLines[0][0][1] = array[j];
			}
			if(quantity == quantity95) 
			{
				addLines[0][1][0] = array[j];
				addLines[0][1][1] = array[j];
			}
			if(quantity == quantity99) 
			{
				addLines[0][2][0] = array[j];
				addLines[0][2][1] = array[j];
			}
			while( (j + 1) < array.length && array[j] == array[j + 1] )
			{
				counter += d;
				j++;
				quantity++;
				if(quantity == quantity50) 
				{
					addLines[0][0][0] = array[j];
					addLines[0][0][1] = array[j];
				}
				if(quantity == quantity95) 
				{
					addLines[0][1][0] = array[j];
					addLines[0][1][1] = array[j];
				}
				if(quantity == quantity99) 
				{
					addLines[0][2][0] = array[j];
					addLines[0][2][1] = array[j];
				}
			}
			xValues.add(array[j]);
			yValues.add(counter);
			if(counter > maxY) maxY = counter;
			counter = d;
		}
		
		double[][][] result = new double[2][4][yValues.size()];
		for(int i = 0; i < addLines[1].length; i++)
		{
			addLines[1][i][0] = 0.0;
			addLines[1][i][1] = maxY;
			result[0][i + 1] = addLines[0][i];
			result[1][i + 1] = addLines[1][i];
		}
		
		for(int i = 0; i < xValues.size(); i++)
		{
			result[0][0][i] = xValues.get(i);
			result[1][0][i] = yValues.get(i);
		}
		
		return result;
	}
	
	 public static class FoldersNamesSelector extends GenericMultiSelectEditor
	    {
	        @Override
	        protected String[] getAvailableValues()
	        {
	            try
	            {
	                DataCollection<DataElement> folders = ((ProfilesStatsParameters)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
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
	
	
	public static class ProfilesStatsParameters extends AbstractAnalysisParameters
	{
		private DataElementPath inputTrack, pathToOutputFolder;
		private String prefix = "test";
		private int threadsNumber = 1;
		private boolean addHight = true;
		private boolean addGraph = true;
		private boolean addLengthAndHight = true;
		private DataElementPath pathToFolderWithFolders;
        private String[] foldersNames;
        private boolean trackOrFolder = true;
        private boolean notFolder = false;
        private boolean allFolder = false;
        private boolean isSelectorHidden = false;
        private boolean recordToGtrdf = false;
        private boolean hiddenRecordToGTRD = true;
        private boolean writeNewMatrixToGTRD = false;
        private boolean writeToExistedTable = false;
        private boolean hiddenNameOfExistedTable = true;
        private String  nameOfExistedTable;
        private boolean hiddenOutputFolder = false;

		ProfilesStatsParameters()
		{}
		
		
		
        public boolean isHiddenOutputFolder() {
			return hiddenOutputFolder;
		}



		public void setHiddenOutputFolder(boolean hiddenOutputFolder) {
			this.hiddenOutputFolder = hiddenOutputFolder;
		}



		public void setSelectorHidden(boolean isSelectorHidden) {
			this.isSelectorHidden = isSelectorHidden;
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

		
		public boolean getIsSelectorHidden() 
		{
			return isSelectorHidden;
		}

		public void setIsSelectorHidden(boolean isSelectorHidden) 
		{
			Object oldValue = this.isSelectorHidden;
			this.isSelectorHidden = isSelectorHidden;
			firePropertyChange("*", oldValue, isSelectorHidden);
		}


		@PropertyName("count all files in folder")
		@PropertyDescription("count all files in folder")
		public boolean getAllFolder() {
			return allFolder;
		}

		public void setAllFolder(boolean allFolder) 
		{
			Object oldValue = this.allFolder;
			this.allFolder = allFolder;
			setIsSelectorHidden(allFolder);
			firePropertyChange("*", oldValue, allFolder);
		}

		
		@PropertyName("Load tracks from folder")
		@PropertyDescription("Take tracks from folder")
		public boolean isTrackOrFolder() 
		{
			return trackOrFolder;
		}
		public void setTrackOrFolder(boolean trackOrFolder) 
		{
			Object oldValue = this.trackOrFolder;
			this.trackOrFolder = trackOrFolder;
			setIsSelectorHidden(!trackOrFolder);
			setNotFolder(!trackOrFolder);
			firePropertyChange("*", oldValue, trackOrFolder);
		}
		
		public boolean getNotFolder() 
		{
			return notFolder;
		}
		public void setNotFolder(boolean notFolder) 
		{
			Object oldValue = this.notFolder;
			this.notFolder = notFolder;
			firePropertyChange("*", oldValue, notFolder);
		}
		
		@PropertyName("add the lenght/hight graph")
		@PropertyDescription("drawing thr graphic between hight and lenght")
		public boolean isAddLengthAndHight() {
			return addLengthAndHight;
		}
		public void setAddLengthAndHight(boolean addLengthAndHight) {
			this.addLengthAndHight = addLengthAndHight;
		}
		@PropertyName("add the graphics")
		@PropertyDescription("drawing graphics or not")
		public boolean isAddGraph() {
			return addGraph;
		}
		public void setAddGraph(boolean addGraph) {
			this.addGraph = addGraph;
			setAddHight(addGraph);
			setAddLengthAndHight(addGraph);
		}
		@PropertyName("add the hight in analysis")
		@PropertyDescription("adding or not the hight in analysis")
		public boolean isAddHight() {
			return addHight;
		}

		public void setAddHight(boolean addHight) {
			this.addHight = addHight;
			if(!isAddHight())
				setAddLengthAndHight(false);
		}
		
		@PropertyName("Input SQL Track")
        @PropertyDescription("Input SQL Track")
		public DataElementPath getInputTrack() 
		{
			return inputTrack;
		}
		
		public void setInputTrack(DataElementPath inputTrack) 
		{
			this.inputTrack = inputTrack;
		}

		@PropertyName("Number of threads")
        @PropertyDescription("Number of threads")
		public int getThreadsNumber() 
		{
			return threadsNumber;
		}

		public void setThreadsNumber(int threadsNumber) 
		{
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
		
        @PropertyName("Path to folder with tracks")
        @PropertyDescription("Path to folder with tracks")
        public DataElementPath getPathToFolderWithFolders()
        {
            return pathToFolderWithFolders;
        }
        public void setPathToFolderWithFolders(DataElementPath pathToFolderWithFolders)
        {
            Object oldValue = this.pathToFolderWithFolders;
            this.pathToFolderWithFolders = pathToFolderWithFolders;
            firePropertyChange("*", oldValue, pathToFolderWithFolders);
        }

        @PropertyName("Track's names in folder")
        @PropertyDescription("Track's names in folder")
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
		
        @PropertyName("prefix")
        @PropertyDescription("write prefix for output file name")
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
	public static class ProfilesStatsParametersBeanInfo extends BeanInfoEx2<ProfilesStatsParameters>
	{
		public ProfilesStatsParametersBeanInfo()
        {
            super(ProfilesStatsParameters.class);
        }
		
		@Override
		protected void initProperties() throws Exception
		{
			property("TrackOrFolder").add();
			property("allFolder").hidden("getNotFolder").add();
			property("inputTrack").inputElement( SqlTrack.class ).hidden("isTrackOrFolder").add();
			property(DataElementPathEditor.registerInput("pathToFolderWithFolders", beanClass, DataCollection.class)).hidden("getNotFolder").add();
			property("foldersNames").editor(FoldersNamesSelector.class).hidden("getIsSelectorHidden").add();
			property("prefix").add();
			property("threadsNumber").add();
			property("addGraph").add();
			property("addHight").add();
			property("addLengthAndHight").add();
			property("recordToGtrdf").add();
            property("writeNewMatrixToGTRD").hidden("isHiddenRecordToGTRD").add();
            property("writeToExistedTable").hidden("isHiddenRecordToGTRD").add();
            property("nameOfExistedTable").hidden("isHiddenNameOfExistedTable").add();
			property("pathToOutputFolder").inputElement( FolderCollection.class ).hidden("isHiddenOutputFolder").add();
		}
	}
}
