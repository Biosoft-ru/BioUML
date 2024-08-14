/* $Id$ */

package biouml.plugins.gtrd.analysis;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.gtrd.access.GTRDStatsAccess;
import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.FunSiteUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils.SiteModelComposed;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.ChrIntervalMap;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;


public class  SitesMotifsAnalysis  extends AnalysisMethodSupport<SitesMotifsAnalysis.SitesMotifsAnalysisParameters>
{
    volatile double[] arrayWithCounts;
    volatile double numberOfHocoSites;
    public SitesMotifsAnalysis (DataCollection<?> origin, String name)
    {
        super( origin, name, new SitesMotifsAnalysisParameters() );
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut()
            throws RepositoryException, InterruptedException, ExecutionException, ClassNotFoundException, SQLException
    {

        String tableName = parameters.getTrackName();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        DataElementPath pathToTrack = parameters.getPathToTrack();
        DataMatrixString resultTable = null;
        int threadsNumber = parameters.getThreadsNumber();
        TrackInfoTable trackInfoTable = new TrackInfoTable();
        SqlTrack inputTrack = pathToTrack.getDataElement(SqlTrack.class);
        String trackInfoId = trackInfoTable.getTrackInfoId(inputTrack.getCompletePath().toString());
        String typeOfFirsColumn = "none";
        String[] columnsType;
        
        // 1. Create sequence set.
        
        if(!parameters.getTrackOrMatrix())
        {

        	typeOfFirsColumn = "matrix_id";
        	int  minSiteSize = parameters.getMinimlSiteSize();
        	String siteName = parameters.getSiteName();
            SiteModel[] siteModels;
            String[] siteModelNames;
            DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
            Sequence[] sequences = getSequenceRegions(pathToTrack, pathToSequences, minSiteSize);
	        int allSitesInTrack = pathToTrack.getDataElement(SqlTrack.class).getSize();
	        // 2. Create siteModelComposed
	        if(parameters.getOneMatrixOrMore())
	        {	
	        	siteModels = new SiteModel[1];
	        	siteModelNames = new String[1];
	        	siteModelNames[0] = parameters.getModelName();
	        	siteModels[0] = parameters.getModelPath().getDataElement(SiteModel.class);
	        }
	        else
	        {
	        	int i = 0;
	        	HashMap<String,SiteModel> dicWithModels = makeArrayWithModels(parameters.getFoldersNames(),parameters.getHuman());
	        	siteModels = new SiteModel[dicWithModels.size()];
	        	siteModelNames = new String[dicWithModels.size()];
	        	for(Entry<String, SiteModel> elm : dicWithModels.entrySet())
	        	{
	        		siteModelNames[i] = elm.getKey();
	        		siteModels[i] = elm.getValue();
	        		i++;
	        	}
	        }
	        // 3. Predict sites and write them to track.
	        SiteModelComposedHoco siteModelComposed = new SiteModelComposedHoco(siteModels, siteModelNames, siteName, true);
	        
	        jobControl.setPreparedness(jobControl.getPreparedness() + 20);
	        if(!parameters.countCoverOfMotifs)
	        {
	        	resultTable = siteModelComposed.countAllSites(sequences, allSitesInTrack, trackInfoId,jobControl);
	        	columnsType = new String[] {"VARCHAR(255)","FLOAT","FLOAT", "FLOAT"};
	        }

	        else
	        {
	        	ArrayList<ChrIntervalMap<String>> newHocoTracks = siteModelComposed.onlyMakeHocoSites(sequences, pathToSequences, pathToOutputFolder);
	        	String[][] dataForMatrix = new String[newHocoTracks.size()][5];
	        	for(int i= 0; i < newHocoTracks.size(); i++)
	        	{
	        		if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
	        		ChrIntervalMap<String> hocTrack = newHocoTracks.get(i); 
	        		arrayWithCounts = new double[2];
	        		numberOfHocoSites = 0;
	        		TaskPool.getInstance().iterate(inputTrack.getChromosomesPath().getChildren(), 
	    					new GettingMotifsIterations(inputTrack, hocTrack),threadsNumber);
	        		String[] partOfName = siteModelNames[i].split("_");
	        		String pvalue = "none";
	                if(!parameters.getOneMatrixOrMore())
	                	pvalue = partOfName[2];
	        		String percentOfSites = String.format("%.3g",(arrayWithCounts[0]/inputTrack.getSize())*100) + "";
	                String percentOfCover = String.format("%.3g",(arrayWithCounts[1]/numberOfHocoSites)*100) + "";
	                dataForMatrix[i][0] = trackInfoId;
	                dataForMatrix[i][1] = pvalue;
	                dataForMatrix[i][2] = arrayWithCounts[0] + "";
	                dataForMatrix[i][3] = percentOfSites;
	                dataForMatrix[i][4] = percentOfCover;
	                jobControl.setPreparedness(jobControl.getPreparedness() + 60/newHocoTracks.size());
	        	}
	        	columnsType = new String[] {"VARCHAR(255)","FLOAT","FLOAT", "FLOAT","FLOAT"};
	        	resultTable = new DataMatrixString(siteModelNames, new String[]{"track_info_id","p_value","number_of_sites","percent_of_sites","fraction_motif_in_peaks"},dataForMatrix);

	        	
	        }
        }
        
        else
        {
        	if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        	typeOfFirsColumn = "track_name";
        	ru.biosoft.access.core.DataElementPath pathToHocoTrack = parameters.getOnlyTrackl();
            SqlTrack hocoTrack = pathToHocoTrack.getDataElement(SqlTrack.class);
            arrayWithCounts = new double[2];
            TaskPool.getInstance().iterate(inputTrack.getChromosomesPath().getChildren(), 
					new GettingMotifsIterationsTrack(inputTrack, hocoTrack),threadsNumber);
            String percentOfSites = String.format("%.3g",(arrayWithCounts[0]/inputTrack.getSize())*100) + "";
            String percentOfCover = String.format("%.3g",(arrayWithCounts[1]/hocoTrack.getSize())*100) + "";
            jobControl.setPreparedness(jobControl.getPreparedness() + 60);
			resultTable = new DataMatrixString(inputTrack.getName(), new String[]{"track_info_id","number_of_sites","percent_of_sites", "fraction_motif_in_peaks"},
					new String[] {trackInfoId,arrayWithCounts[0] + "",percentOfSites, percentOfCover});
			columnsType = new String[] {"VARCHAR(255)","FLOAT","FLOAT", "FLOAT"};
        }
        
        
        if(parameters.isRecordToGtrdf())
        	if(parameters.isWriteNewMatrixToGTRD())
        		GTRDStatsAccess.writeTableFromDataMatrix(resultTable, tableName, typeOfFirsColumn,columnsType, "gtrd_id", true, true);
        	else
        		GTRDStatsAccess.writeTableFromDataMatrix(resultTable,  parameters.getNameOfExistedTable(), typeOfFirsColumn, columnsType, "gtrd_id", false, false);
        else		
        	resultTable.writeDataMatrixString(false, pathToOutputFolder,tableName + "_result_table", log);
        
        return pathToOutputFolder.getDataCollection();
    }
    
    
    class GettingMotifsIterations implements Iteration<ru.biosoft.access.core.DataElementPath>
	{		
		SqlTrack sqlInputTrack;
		ChrIntervalMap<String> hocoTrack;
		GettingMotifsIterations(SqlTrack sqlInputTrack,ChrIntervalMap<String>  inputHocoTrack)
		{
			this.sqlInputTrack = sqlInputTrack;
			this.hocoTrack = inputHocoTrack;

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
				DataCollection<Site> chipSites = sqlInputTrack.getSites(chrPath.toString(), chrStart, chrEnd);
				double counter = 0;
	        	double coverCount =0;
	        	numberOfHocoSites = numberOfHocoSites + hocoTrack.getIntervals(chrName, chrStart, chrEnd).size();
	        	for(Site site:chipSites) 
				{ 
	        		chrName = site.getSequence().getName();
					Collection<String> findsite = hocoTrack.getIntervals(chrName, site.getFrom(), site.getTo());
					if (!findsite.isEmpty())
					{
						counter++;
						coverCount = findsite.size();
					}
				}
				synchronized(arrayWithCounts)
				{

					arrayWithCounts[0] += counter;
					arrayWithCounts[1] = arrayWithCounts[1] + coverCount;
				}
				synchronized(jobControl)
				{
					//jobControl.setPreparedness(jobControl.getPreparedness() + percentNumber);
				}

			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}
    
    class GettingMotifsIterationsTrack implements Iteration<ru.biosoft.access.core.DataElementPath>
	{		
		SqlTrack sqlInputTrack;
		SqlTrack hocoTrack;
		GettingMotifsIterationsTrack(SqlTrack sqlInputTrack,SqlTrack inputHocoTrack)
		{
			this.sqlInputTrack = sqlInputTrack;
			this.hocoTrack = inputHocoTrack;

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
				DataCollection<Site> chipSites = sqlInputTrack.getSites(chrPath.toString(), chrStart, chrEnd);
				double counter = 0;
	        	double coverCount =0;
	        	for(Site site:chipSites) 
				{ 
	        		chrName = site.getSequence().getName();
					DataCollection<Site> findsite = hocoTrack.getSites(chrName, site.getFrom(), site.getTo());
					if (!findsite.isEmpty())
					{
						counter++;
						coverCount = findsite.getSize();
					}
				}
				synchronized(arrayWithCounts)
				{

					arrayWithCounts[0] += counter;
					arrayWithCounts[1] = arrayWithCounts[1] + coverCount;
				}
				synchronized(jobControl)
				{
					//jobControl.setPreparedness(jobControl.getPreparedness() + percentNumber);
				}

			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}    
    
    
    private static Sequence[] getSequenceRegions(DataElementPath pathToTrack, DataElementPath pathToSequences, int minSiteSize)
    {
        Track track = pathToTrack.getDataElement(Track.class);
        FunSite[] funSites = FunSiteUtils.transformToArray(FunSiteUtils.readSitesInTrack(track, 0, Integer.MAX_VALUE, null, null));
        funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
        return FunSiteUtils.getSequenceRegions(funSites, pathToSequences,minSiteSize);
    }
    
    private static HashMap<String,SiteModel> makeArrayWithModels(String[] listWithNames, boolean spices)
    {
    	HashMap<String,SiteModel> dicWithModels = new HashMap<>();
    	String[] pval = new String[3];
    	pval[0] = "0.0001";
    	pval[1] = "0.0005";
    	pval[2] = "0.001";
    	String organism = "HUMAN";
    	for(int i = 0; i < listWithNames.length;i++)
    	{
    		for(int j = 0; j < pval.length;j++)
    		{
    			if (spices)
    				organism = "MOUSE";
    			SiteModel elm = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_"+organism+"_mono_pval="+ 
    		pval[j] + "/" + listWithNames[i]).getDataElement(SiteModel.class);
    			dicWithModels.put(listWithNames[i] + "_" + pval[j], elm);
    		}
    	}
    	return(dicWithModels);
    }
    
    public static class SiteModelComposedHoco extends SiteModelComposed
    {
        public SiteModelComposedHoco(SiteModel[] siteModels, String[] siteModelNames, String name, boolean areBothStrands)
        {
            super(siteModels, siteModelNames, name, areBothStrands);
        }
        
        public ArrayList<ChrIntervalMap<String>> onlyMakeHocoSites(Sequence[] sequences, DataElementPath pathToSequences,ru.biosoft.access.core.DataElementPath pathToOutputFolder)
        {

            Sequence[] sequencesReversed = areBothStrands ? new Sequence[sequences.length] : null;
            if( areBothStrands )
                 for( int i = 0; i < sequences.length; i++ )
                     sequencesReversed[i] = SequenceRegion.getReversedSequence(sequences[i]);
            
            ArrayList<ChrIntervalMap<String>> arrayWithHocoTracks = new ArrayList<>();
            for( int i = 0; i < siteModels.length; i++ )
            {
                ChrIntervalMap<String> modelAllSites = new ChrIntervalMap<>();
                for( int j = 0; j < sequences.length; j++ )
                {
                    for( int jj = 0; jj < 2; jj++ )
                    {
                        Sequence seq = jj == 0 ? sequences[j] : null;
                        if( jj == 1 )
                        {
                            if( ! areBothStrands ) break;
                            seq = sequencesReversed[j];
                        }
                        try
                        {
                            SequenceRegion regionSeq = (SequenceRegion)seq;
                            siteModels[i].addSitesToIntMap(regionSeq, modelAllSites);
                        }
                        catch( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
                arrayWithHocoTracks.add(modelAllSites);
            }
            return arrayWithHocoTracks;
        }
        
        public DataMatrixString countAllSites(Sequence[] sequences, int allSites, String trackId, AnalysisJobControl jobControl)
        {
            Sequence[] sequencesReversed = areBothStrands ? new Sequence[sequences.length] : null;
            if( areBothStrands )
                for( int i = 0; i < sequences.length; i++ )
                    sequencesReversed[i] = SequenceRegion.getReversedSequence(sequences[i]);
            
            // 1. Find all sites.
            String[][] dataForMatrix = new String[siteModelNames.length][4];
            for( int i = 0; i < siteModels.length; i++ )
            {
                double numberOfallFoundedSites = 0;
                // 1.1 Find all sites for current site model and count them.
                for( int j = 0; j < sequences.length; j++ )
                {
                    for( int jj = 0; jj < 2; jj++ )
                    {
                        Sequence seq = jj == 0 ? sequences[j] : null;
                        if( jj == 1 )
                        {
                            if( ! areBothStrands ) break;
                            seq = sequencesReversed[j];
                        }
                        try
                        {
                            double numberOfSites = siteModels[i].countNumberOfAllSites(seq);
                            numberOfallFoundedSites = numberOfallFoundedSites + numberOfSites;
                        }
                        catch( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
                String percentOfSites = String.format("%.3g",(numberOfallFoundedSites/allSites)*100);
                String[] partOfName = siteModelNames[i].split("_");
                double pvalue = 0;
                if(partOfName.length > 2)
                {
                    if(partOfName[2].equals("0.0005"))
                        pvalue = 0.0005;
                    else if(partOfName[2].equals("0.0001"))
                        pvalue = 0.0001;
                    
                    else if(partOfName[2].equals("0.001"))
                            pvalue = 0.001;
                }
                dataForMatrix[i][0] = trackId;
                dataForMatrix[i][1] = pvalue + "";
                dataForMatrix[i][2] = numberOfallFoundedSites + "";
                dataForMatrix[i][3] = percentOfSites;
                jobControl.setPreparedness(jobControl.getPreparedness() + 60 / siteModels.length);
            }
            
            // 2. build table with data.
            DataMatrixString statMatrix = new DataMatrixString(siteModelNames, new String[]
                    {"track_info_id","p_value","number_of_sites","percent_of_sites"}, dataForMatrix) ;
            return statMatrix;
        }
    }

    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_DB_SELECTOR = "Sequences collection";
        public static final String PD_DB_SELECTOR = "Select a source of nucleotide sequences";
        
        public static final String PN_SEQUENCE_SET_TYPE = "Sequence set type";
        public static final String PD_SEQUENCE_SET_TYPE = "Select type of sequences";
        
        public static final String PN_CHROMOSOME_NAME = "Chromosome name";
        public static final String PD_CHROMOSOME_NAME = "Select chromosome name";
        
        public static final String PN_START_POSITION = "Start position";
        public static final String PD_START_POSITION = "Start position of chromosome fragment";
        
        public static final String PN_FINISH_POSITION = "Finish position";
        public static final String PD_FINISH_POSITION = "Finish position of chromosome fragment";
        
        public static final String PN_PATH_TO_TRACK = "Path to track";
        public static final String PD_PATH_TO_TRACK = "Path to track with ChIP-Seq dataset";

        public static final String PN_SITE_NAME = "Site name";
        public static final String PD_SITE_NAME = "Name of predicted sites";
        
        public static final String PN_PREDICTION_MODELS = "Prediction models";
        public static final String PD_PREDICTION_MODELS = "Define prediction models";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        
        public static final String PN_TRACK_NAME = "The output track name";
        public static final String PD_TRACK_NAME = "The output track name";
    }
    
    
    public static class FoldersNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> folders = ((SitesMotifsAnalysisParameters)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
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
    
    public static class SitesMotifsAnalysisParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private DataElementPath pathToTrack;
        private String siteName;
        private String trackName;
        private DataElementPath pathToOutputFolder;
        private boolean trackOrMatrix = false;
        private boolean hiddenCountMatrix = true;
        private DataElementPath onlyTrackl;
        private String modelName;
        private DataElementPath modelPath;
        private boolean oneMatrixOrMore = false;
        private String[] foldersNames;
        private DataElementPath pathToFolderWithFolders;
        private boolean human = false;
        private boolean checkerForType = false;
        private int minimlSiteSize = 20;
        private boolean hiddenOnlyTrack = true;
        private int threadsNumber = 10;
        private boolean countCoverOfMotifs = false;
        private boolean recordToGtrdf = false;
        private boolean hiddenRecordToGTRD = true;
        private boolean writeNewMatrixToGTRD = false;
        private boolean writeToExistedTable = false;
        private boolean hiddenNameOfExistedTable = true;
        private String  nameOfExistedTable;
        private boolean hiddenOutputFolder = false;

		public SitesMotifsAnalysisParameters()
        {
            setDbSelector(new BasicGenomeSelector());
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
			if(!recordToGtrdf)
			{
				setWriteNewMatrixToGTRD(false);
				setWriteToExistedTable(false);
			}
				
			firePropertyChange("*", oldValue, recordToGtrdf);

		}

		@PropertyName("Number of thread")
        @PropertyDescription("write number of threads (not more than 20)")
		public int getThreadsNumber() {
			return threadsNumber;
		}


		public void setThreadsNumber(int threadsNumber) {
			this.threadsNumber = threadsNumber;
		}


		@PropertyName("Count cover of motifs")
        @PropertyDescription("choose this one, if you need to count cover of motifs in sites")
		public boolean isCountCoverOfMotifs() {
			return countCoverOfMotifs;
		}

		public void setCountCoverOfMotifs(boolean counCoverOfMotifs) {
			this.countCoverOfMotifs = counCoverOfMotifs;
		}

               
        public boolean getHiddenOnlyTrack() {
			return hiddenOnlyTrack;
		}

		public void setHiddenOnlyTrack(boolean hiddenOnlyTrack) {
			this.hiddenOnlyTrack = hiddenOnlyTrack;
		}

		@PropertyName("miniml site size")
        @PropertyDescription("Choose miniml site's size")
        
        public int getMinimlSiteSize()
        {
			return minimlSiteSize;
		}

		public void setMinimlSiteSize(int minimlSiteSize) 
		{
			this.minimlSiteSize = minimlSiteSize;
		}

		public boolean getCheckerForType() 
		{
			return checkerForType;
		}

		public void setCheckerForType(boolean checkerForType) 
		{
			Object oldValue = this.checkerForType;
			this.checkerForType = checkerForType;
			if(checkerForType)
			{
				setHiddenCountMatrix(checkerForType);
				setOneMatrixOrMore(checkerForType);
			}
			firePropertyChange("*", oldValue, checkerForType);
		}
        
        @PropertyName("Use mouse matrix")
        @PropertyDescription("Use mouse HOCOMOCO matrix") 
        
        public boolean getHuman() 
        {
			return human;
		}

		public void setHuman(boolean human) 
		{
			Object oldValue = this.human;
			this.human = human;
			if (!human)
            	this.pathToFolderWithFolders = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.0001");
            else
            	this.pathToFolderWithFolders = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_MOUSE_mono_pval=0.0001");
			firePropertyChange("*", oldValue, human);
		}

		public DataElementPath getPathToFolderWithFolders()
		{
			return pathToFolderWithFolders;
		}
        
        @PropertyName("Use only one model")
        @PropertyDescription("Use only one HOCOMOCO model")
        
        public boolean getOneMatrixOrMore() 
        {
			return oneMatrixOrMore;
		}

		public void setOneMatrixOrMore(boolean oneMatrixOrMore) 
		{
			Object oldValue = this.oneMatrixOrMore;
			this.oneMatrixOrMore = oneMatrixOrMore;
			if(!getCheckerForType())
			{
				setHiddenCountMatrix(!oneMatrixOrMore);
			}
			firePropertyChange("*", oldValue, oneMatrixOrMore);
		}
        
		@PropertyName("model name")
        @PropertyDescription("write name of your model")
		public String getModelName()
        {
            return modelName;
        }
        
        public void setModelName(String modelName)
        {
            Object oldValue = this.modelName;
            this.modelName = modelName;
            firePropertyChange("modelName", oldValue, modelName);
        }
        
        public DataElementPath getModelPath()
        {
            return modelPath;
        }
        
        public void setModelPath(DataElementPath modelPath)
        {
            Object oldValue = this.modelPath;
            this.modelPath = modelPath;
            firePropertyChange("modelPath", oldValue, modelPath);
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
        
		public DataElementPath getOnlyTrackl() 
		{
			return onlyTrackl;
		}


		public void setOnlyTrackl(DataElementPath onlyTrackl) 
		{
			this.onlyTrackl = onlyTrackl;
		}

		public boolean getHiddenCountMatrix() 
		{
			return hiddenCountMatrix;
		}

		public void setHiddenCountMatrix(boolean hiddenCountMatrix) 
		{
			Object oldValue = this.hiddenCountMatrix;
			this.hiddenCountMatrix = hiddenCountMatrix;
			firePropertyChange("*", oldValue, hiddenCountMatrix);
		}

		@PropertyName("Use HOCOMOCO track")
        @PropertyDescription("Choose HOCOMOCO track or use HOCOMOCO model for creating new HOCOMOCO track")        
        public boolean getTrackOrMatrix() 
        {
			return trackOrMatrix;
		}

        public void setTrackOrMatrix(boolean trackOrMatrix) 
        {
        	Object oldValue = this.trackOrMatrix;
			this.trackOrMatrix = trackOrMatrix;
			setCheckerForType(trackOrMatrix);
			setHiddenOnlyTrack(!trackOrMatrix);
			firePropertyChange("*", oldValue, trackOrMatrix);
		}

		@PropertyName(MessageBundle.PN_DB_SELECTOR)
        @PropertyDescription(MessageBundle.PD_DB_SELECTOR)
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
        
        @PropertyName(MessageBundle.PN_PATH_TO_TRACK)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TRACK)
        public DataElementPath getPathToTrack()
        {
            return pathToTrack;
        }
        public void setPathToTrack(DataElementPath pathToTrack)
        {
            Object oldValue = this.pathToTrack;
            this.pathToTrack = pathToTrack;
            firePropertyChange("pathToTrack", oldValue, pathToTrack);
        }
        
        @PropertyName(MessageBundle.PN_SITE_NAME)
        @PropertyDescription(MessageBundle.PD_SITE_NAME)
        public String getSiteName()
        {
            return siteName;
        }
        public void setSiteName(String siteName)
        {
            Object oldValue = this.siteName;
            this.siteName = siteName;
            firePropertyChange("siteName", oldValue, siteName);
        }
        

        @PropertyName("output table name")
        @PropertyDescription("write output table name")
        public String getTrackName()
        {
            return trackName;
        }
        public void setTrackName(String trackName)
        {
            Object oldValue = this.trackName;
            this.trackName = trackName;
            firePropertyChange("trackName", oldValue, trackName);
        }

        @PropertyName(MessageBundle.PN_PATH_TO_OUTPUT_FOLDER)
        @PropertyDescription(MessageBundle.PD_PATH_TO_OUTPUT_FOLDER)
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
    
    public static class SitesMotifsAnalysisParametersBeanInfo extends BeanInfoEx2<SitesMotifsAnalysisParameters>
    {
        public SitesMotifsAnalysisParametersBeanInfo()
        {
            super(SitesMotifsAnalysisParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add("siteName");
            add(DataElementPathEditor.registerInput("pathToTrack", beanClass, Track.class));
            add("trackOrMatrix");
            add("trackName");
            property("recordToGtrdf").add();
            property("writeNewMatrixToGTRD").hidden("isHiddenRecordToGTRD").add();
            property("writeToExistedTable").hidden("isHiddenRecordToGTRD").add();
            property("nameOfExistedTable").hidden("isHiddenNameOfExistedTable").add();
            property("threadsNumber").add();
            property("onlyTrackl").inputElement( SqlTrack.class ).hidden("getHiddenOnlyTrack").add();
            property("oneMatrixOrMore").hidden("getCheckerForType").add();
            property("countCoverOfMotifs").hidden("getCheckerForType").add();
            property("human").hidden("getCheckerForType").add();
            property("minimlSiteSize").hidden("getCheckerForType").add();
            addHidden("modelName","getHiddenCountMatrix");
            property("foldersNames").editor(FoldersNamesSelector.class).hidden("getOneMatrixOrMore").add();
            addHidden(DataElementPathEditor.registerInput("modelPath", beanClass, SiteModel.class),"getHiddenCountMatrix");
            property("pathToOutputFolder").inputElement( FolderCollection.class ).hidden("isHiddenOutputFolder").add();
        }
    }
}

	
