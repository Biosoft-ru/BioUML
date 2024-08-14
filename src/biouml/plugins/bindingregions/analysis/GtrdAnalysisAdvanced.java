/* $Id$ */

package biouml.plugins.bindingregions.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.gtrd.utils.CistromUtils.CistromConstructorExploratory;
import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.TrackInfo;
import biouml.plugins.gtrd.utils.FunSiteUtils.CombinedSites;
import biouml.plugins.machinelearning.utils.ClusterUtils.AgglomerativeHierarchicalClustering;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrix.DataMatrixConstructor;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixStringConstructor;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class GtrdAnalysisAdvanced extends AnalysisMethodSupport<GtrdAnalysisAdvanced.GtrdAnalysisAdvancedParameters>
{
    public static final String OPTION_01 = "OPTION_01 : Summary on ChIP-Seq tracks";
    public static final String OPTION_02 = "OPTION_02 : Hierarchical clustering of tracks from peack callers";
    public static final String OPTION_03 = "OPTION_03 : Hierarchical clustering of tracks from peack callers";
//    public static final String OPTION_04 = "OPTION_04 : Correspondence between GTRD tracks and HOCOMOCO matrices";
//    public static final String OPTION_05 = "OPTION_05 : Read merged FunSites and produce ROC-curves and AUCs";
//    public static final String OPTION_06 = "OPTION_06 : Read merged FunSites and write average peak caller properties into table";
//    public static final String OPTION_07 = "OPTION_07 : Determine information about control in ChIP-seq tracks";
//    public static final String OPTION_08 = "OPTION_08 : Read combined (merged or overlapped) FunSites and write them into tracks or tables";
//    public static final String OPTION_09 = "OPTION_09 : Cistrom construction";

    public GtrdAnalysisAdvanced(DataCollection<?> origin, String name)
    {
        super(origin, name, new GtrdAnalysisAdvancedParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut()
    {
        String option = parameters.getOption();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        switch( option )
        {
            case OPTION_01 : log.info("OPTION_01 : Summary on ChIP-Seq tracks");
                             Species givenSpecie = parameters.getParametersForOption01().getSpecies();
                             DataElementPath pathToFolderWithFolders = parameters.getParametersForOption01().getPathToFolderWithFolders();
                             String[] foldersNames = parameters.getParametersForOption01().getFoldersNames();
                             // boolean doAddFrequencyMatices = parameters.getParametersForOption01().getDoAddFrequencyMatices();
                             DataElementPath pathToFolderWithMatrices = parameters.getParametersForOption01().getPathToFolderWithMatrices();
                             implementOption01(givenSpecie, pathToFolderWithFolders, foldersNames, pathToFolderWithMatrices, pathToOutputFolder);
                             break;
//            case OPTION_02 : log.info("OPTION_02 : Hierarchical clustering of tracks from peack callers");
//                             // TODO: temp
//                             String combinedSiteType = CombinedSites.SITE_TYPE_OVERLAPPED;
//                             pathToFolderWithFolders = parameters.getParametersForOption02().getPathToFolderWithFolders();
//                             foldersNames = parameters.getParametersForOption02().getFoldersNames();
//                             String[] trackNames = new String[]{"PEAKS033364", "PEAKS040188", "PEAKS040190"};
//                             int minimalLengthOfSite = 20;
//                             int maximalLengthOfSite = 1000000;
//                             String methodName = RankAggregation.METHOD_AR_MEAN;
//                             //implementAgglomerativeHierarchicalClustering(combinedSiteType, pathToFolderWithFolders, foldersNames, trackNames, minimalLengthOfSite, maximalLengthOfSite, true, methodName, pathToOutputFolder);
//                             break;
            case OPTION_03 : log.info("OPTION_03 : Hierarchical clustering of tracks from peack callers");
                             // TODO: temp
                             // Agglomerative hierarchical clustering
                             String combinedSiteType = CombinedSites.SITE_TYPE_OVERLAPPED;
                             pathToFolderWithFolders = parameters.getParametersForOption03().getPathToFolderWithFolders();
                             foldersNames = parameters.getParametersForOption03().getFoldersNames();
                             String[] trackNames = new String[]{"PEAKS033364", "PEAKS040188", "PEAKS040190"};
                             int minimalLengthOfSite = 20, maximalLengthOfSite = 1000000;
                             String methodName = RankAggregation.METHOD_AR_MEAN;
                             //implementAgglomerativeHierarchicalClustering(combinedSiteType, pathToFolderWithFolders, foldersNames, trackNames, minimalLengthOfSite, maximalLengthOfSite, true, methodName, pathToOutputFolder);
                             
                             // TODO: temp
//                           DataElementPath path = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/GTRD_38/Answer04_quality_control_article/DATA03_new/quality_control_metrics_and_control");
//                           ExploratoryAnalysisUtil.getPriorities(path, pathToOutputFolder, "priorities_all");
//                           path = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/GTRD_38/Answer04_quality_control_article/DATA03_new/quality_control_metrics_and_control__Yes");
//                           ExploratoryAnalysisUtil.getPriorities(path, pathToOutputFolder, "priorities_with_control");
//                           path = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/GTRD_38/Answer04_quality_control_article/DATA03_new/quality_control_metrics_and_control__No");
//                           ExploratoryAnalysisUtil.getPriorities(path, pathToOutputFolder, "priorities_without_control");
                             break;

//                             DataElementPath pathToFolderWithFiles = parameters.parametersForOption03.getPathToFolderWithFiles();
//                             implementOption03(pathToFolderWithFiles, pathToOutputFolder);
//                             break;
//            case OPTION_04 : log.info("OPTION_04 : Correspondence between GTRD tracks and HOCOMOCO matrices");
//                             givenSpecie = parameters.getParametersForOption04().getSpecies();
//                             pathToFolderWithFolders = parameters.getParametersForOption04().getPathToFolderWithFolders();
//                             foldersNames = parameters.getParametersForOption04().getFoldersNames();
//                             pathToFolderWithMatrices = parameters.getParametersForOption04().getPathToFolderWithMatrices();
//                             implementOption04(pathToFolderWithFolders, givenSpecie, foldersNames, pathToFolderWithMatrices, pathToOutputFolder);
//                             break;
        }
        return pathToOutputFolder.getDataCollection();
    }
    
        /**************************************************************/
    
//    private void implementAgglomerativeHierarchicalClustering(String combinedSiteType, DataElementPath pathToFolderWithFolders, String[] foldersNames, String[] trackNames, int minimalLengthOfSite, int maximalLengthOfSite, boolean doIncludeCombinedFrequency, String methodName, DataElementPath pathToOutputFolder)
//    {
//        for( int i = 0; i < trackNames.length; i++ )
//        {
//            Object[] objects = CistromConstructorExploratory.getCombinedSitesAndDataMatrixFromPeakCallers(combinedSiteType, pathToFolderWithFolders, foldersNames, trackNames[i], minimalLengthOfSite, maximalLengthOfSite, doIncludeCombinedFrequency, methodName);
//            if( objects == null ) continue;
//            log.info("**************** _i_ = " + i + " trackNames[i] = " + trackNames[i] + " size of combined sites set = " + ((FunSite[])objects[0]).length);
//            DataMatrix dm = (DataMatrix)objects[1];
//            boolean[] doSortInIncreasingOrder = (boolean[])objects[2];
//            dm.writeDataMatrix(false, pathToOutputFolder, trackNames[i] + "_data_matrix", log);
//            
//            // Feature selection by AgglomerativeHierarchicalClustering.
//            objects = MatrixUtils.getRanksWithTieCorrections(dm.getMatrix(), doSortInIncreasingOrder);
//            double[][] ranks = (double[][])objects[0];
//            double[] tieCorrections2 = (double[])objects[2];
//            DataMatrix dataMatrix = new DataMatrix(dm.getRowNames(), dm.getColumnNames(), ranks);
//            dataMatrix.writeDataMatrix(false, pathToOutputFolder, trackNames[i] + "_ranks", log);
//            dataMatrix.transpose();
//            AgglomerativeHierarchicalClustering ahc = new AgglomerativeHierarchicalClustering(dataMatrix, tieCorrections2);
//            ahc.saveHistory(pathToOutputFolder, trackNames[i] + "_history");
//            int clustersNumber = doIncludeCombinedFrequency ? 1 + foldersNames.length : foldersNames.length;
//            ahc.saveClusters(clustersNumber, false, pathToOutputFolder, trackNames[i] + "_clusters_content");
//        }
//    }
//    
    /*****************************************************************************************/

    private void implementOption01(Species givenSpecie, DataElementPath pathToFolderWithFolders, String[] foldersNames, DataElementPath pathToFolderWithMatrices, DataElementPath pathToOutputFolder)
    {
        String specieName = givenSpecie.getLatinName();
        Set<String> distinctTracksNames = new HashSet<>();
        String[] names = new String[foldersNames.length];
        for( int i = 0; i < foldersNames.length; i++ )
            names[i] = "Number_of_sites_" + foldersNames[i];
        DataMatrixConstructor dmc = new DataMatrixConstructor(names);
        String[] ss = new String[]{"TF-class", "TF-name", "Uniprot_ID", "Cell_line", "Cell_line_treatment", "is_cell_line_treated", "Control_ID", "Do_control_exist", "Antibody"};
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(ss);
        
        // 1. Calculate output DataMatrix dm and DataMatrixString dms.
        for( int i = 0; i < foldersNames.length; i++ )
            for( ru.biosoft.access.core.DataElement de : pathToFolderWithFolders.getChildPath(foldersNames[i]).getDataCollection(DataElement.class) )
            {
                String name = de.getName();
                if( distinctTracksNames.contains(name) ) continue;
                TrackInfo ti = new TrackInfo((Track)de);
                if( ! specieName.equals(ti.getSpecie()) ) continue;
                
                // TODO: temp
                log.info("track name  = " + name + " cell = " + ti.getCellLine() + " tfClass = " + ti.getTfClass() + " tfName = " + ti.getTfName() + " treatment = " + ti.getTreatment() + " uniprotid = " + ti.getUniprotId());
                
                ti.replaceTreatment();
                distinctTracksNames.add(name);
                double[] numbersOfSites = new double[foldersNames.length];
                numbersOfSites[i] = (double)ti.getNumberOfSites();
                for( int j = i + 1; j < foldersNames.length; j++ )
                {
                    DataElementPath path = pathToFolderWithFolders.getChildPath(foldersNames[j]).getChildPath(name);
                    if( path.exists() )
                        numbersOfSites[j] = path.getDataElement(Track.class).getAllSites().getSize();
                }
                dmc.addRow(name, numbersOfSites);
                String treatment = ti.getTreatment(), isTreated = treatment == null || treatment.equals("") ? "No" : "Yes";
                ti.replaceTreatment();
                String controlId = ti.getControlId(), doControlExist = controlId == null || controlId.equals("") ? "No" : "Yes";
                dmsc.addRow(name, new String[]{ti.getTfClass(), ti.getTfName(), ti.getUniprotId(), ti.getCellLine(), treatment, isTreated, controlId, doControlExist, ti.getAntibody()});
            }
        DataMatrix dm = dmc.getDataMatrix();
        DataMatrixString dms = dmsc.getDataMatrixString();
        
        // 2. Add corresponding matrix names.
        if( pathToFolderWithMatrices != null )
        {
            DataCollection<DataElement> frequencyMatrices = pathToFolderWithMatrices.getDataCollection(DataElement.class);
            int n = frequencyMatrices.getSize(), index = 0;
            String[] matrixNames = new String[n], uniprotIdsForMatrices = new String[n];
            for( ru.biosoft.access.core.DataElement de : frequencyMatrices )
            {
                if( ! (de instanceof FrequencyMatrix) ) return;
                FrequencyMatrix frequencyMatrix = (FrequencyMatrix)de;
                matrixNames[index] = frequencyMatrix.getName();
                uniprotIdsForMatrices[index++] = frequencyMatrix.getBindingElement().getFactors()[0].getName();
            }
            String[] uniprotIds = dms.getColumn("Uniprot_ID"), matNames = UtilsForArray.getConstantArray(uniprotIds.length, "");
            for( int i = 0; i < uniprotIds.length; i++ )
            {
                index = ArrayUtils.indexOf(uniprotIdsForMatrices, uniprotIds[i]);
                if( index >= 0 )
                    matNames[i] = matrixNames[index];
            }
            dms.addColumn("Matrix_name", matNames, dms.getColumnNames().length);
        }
        dm.writeDataMatrix(false, dms, pathToOutputFolder, "summary_on_chip_seq_tracks", log);
    }
    
//  public static class TEfromTwoTablesParameters extends AbstractMrnaAnalysisParameters
//  {
//      public boolean areColumnNamesWithStartCodonPositionsAndTranscriptNamesHidden()
//      {
//          return ! ((RiboSeqAndMrnaFeaturesFormingParameters)getParent()).getDataSetName().equals(ParticularRiboSeq.TWO_TABLES_FOR_TIE);
//      }
//  }

    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Select option (Select the concrete session of given analysis).";
        
        public static final String PN_DB_SELECTOR = "Sequences collection";
        public static final String PD_DB_SELECTOR = "Select a source of nucleotide sequences";
        
        public static final String PN_SPECIES = "Species";
        public static final String PD_SPECIES = "Select a taxonomical species";
        
        public static final String PN_PATH_TO_FOLDER_WITH_FOLDERS = "Path to folder with folders";
        public static final String PD_PATH_TO_FOLDER_WITH_FOLDERS = "Path to folder with folders (that contain (ChIP-seq) tracks)";
        
        public static final String PN_FOLDERS_NAMES = "Folders names";
        public static final String PD_FOLDERS_NAMES = "Select folders names";
         
//        public static final String PN_DO_ADD_FREQUENCY_MATRICES = "Do add frequency matices";
//        public static final String PD_DO_ADD_FREQUENCY_MATRICES = "Do add information about HOKOMOKO frequency matices?";
        
        public static final String PN_PATH_TO_FOLDER_WITH_MATRICES = "Path to folder with matrices";
        public static final String PD_PATH_TO_FOLDER_WITH_MATRICES = "Path to folder with frequency matrices";

        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";

        public static final String PN_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        public static final String PD_PARAMETERS_FOR_OPTION_01 = "Please, determine parameters for OPTION_01";
        
        public static final String PN_PARAMETERS_FOR_OPTION_02 = "Parameters for OPTION_2";
        public static final String PD_PARAMETERS_FOR_OPTION_02 = "Please, determine parameters for OPTION_02";
        
        public static final String PN_PARAMETERS_FOR_OPTION_03 = "Parameters for OPTION_3";
        public static final String PD_PARAMETERS_FOR_OPTION_03 = "Please, determine parameters for OPTION_03";
        
        /***********************************************************************/

//        public static final String PN_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with tracks";
//        public static final String PD_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with (ChIP-seq) tracks";
//        
//        public static final String PN_PATH_TO_FOLDER_WITH_FILES = "Path to folder with files";
//        public static final String PD_PATH_TO_FOLDER_WITH_FILES = "Path to folder with files";
//        
//        public static final String PN_PATH_TO_FOLDER_WITH_SITE_MODELS = "Path to folder with site models";
//        public static final String PD_PATH_TO_FOLDER_WITH_SITE_MODELS = "Path to folder with site models";
//        
//        public static final String PN_PATH_TO_INPUT_TABLE = "Path to input table";
//        public static final String PD_PATH_TO_INPUT_TABLE = "Path to input table";
//        
//        public static final String PN_COMBINED_SITE_TYPE = "Combined site type";
//        public static final String PD_COMBINED_SITE_TYPE = "Select combined site type";
//        
//        public static final String PN_MINIMAL_LENGTH_OF_SITE = "Minimal length of site";
//        public static final String PD_MINIMAL_LENGTH_OF_SITE = "Minimal length of site (or binding region)";
//        
//        public static final String PN_MAXIMAL_LENGTH_OF_SITE = "Maximal length of site";
//        public static final String PD_MAXIMAL_LENGTH_OF_SITE = "Maximal length of site (or binding region)";
//        
//        public static final String PN_DO_REMOVE_ORPHANS = "Do remove orphans";
//        public static final String PD_DO_REMOVE_ORPHANS = "Do remove orphans";
//
//        public static final String PN_DO_TRACK = "Do track (or table)?";
//        public static final String PD_DO_TRACK = "Do write sites into track (or table)?";
//        
//        public static final String PN_METHOD_NAME = "Method name";
//        public static final String PD_METHOD_NAME = "Select method name";
//        
//        public static final String PN_TF_CLASS = "TF-class";
//        public static final String PD_TF_CLASS = "TF-class";
//        
//        public static final String PN_PARAMETERS_FOR_OPTION_04 = "Parameters for OPTION_04";
//        public static final String PD_PARAMETERS_FOR_OPTION_04 = "Please, determine parameters for OPTION_04";
//        
//        public static final String PN_PARAMETERS_FOR_OPTION_05 = "Parameters for OPTION_05";
//        public static final String PD_PARAMETERS_FOR_OPTION_05 = "Please, determine parameters for OPTION_05";
//        
//        public static final String PN_PARAMETERS_FOR_OPTION_06 = "Parameters for OPTION_06";
//        public static final String PD_PARAMETERS_FOR_OPTION_06 = "Please, determine parameters for OPTION_06";
//        
//        public static final String PN_PARAMETERS_FOR_OPTION_07 = "Parameters for OPTION_07";
//        public static final String PD_PARAMETERS_FOR_OPTION_07 = "Please, determine parameters for OPTION_07";
//        
//        public static final String PN_PARAMETERS_FOR_OPTION_08 = "Parameters for OPTION_08";
//        public static final String PD_PARAMETERS_FOR_OPTION_08 = "Please, determine parameters for OPTION_08";
//        
//        public static final String PN_PARAMETERS_FOR_OPTION_09 = "Parameters for OPTION_09";
//        public static final String PD_PARAMETERS_FOR_OPTION_09 = "Please, determine parameters for OPTION_09";
    }
    
    public static class AllParameters extends AbstractAnalysisParameters
    {
        private String option = OPTION_01;
        private BasicGenomeSelector dbSelector;
        private Species species = Species.getDefaultSpecies(null);
        private DataElementPath pathToFolderWithFolders;
        private String[] foldersNames;
        // private boolean doAddFrequencyMatices = true;
        private DataElementPath pathToFolderWithMatrices;
        private DataElementPath pathToOutputFolder;

        /***************************************************************/
        
//        private String combinedSiteType = CombinedSites.SITE_TYPE_MERGED;
//        private DataElementPath pathToFolderWithTracks;
//        private DataElementPath pathToFolderWithFiles;
//        private DataElementPath pathToFolderWithSiteModels;
//        private DataElementPath pathToInputTable;
//        private int minimalLengthOfSite = 20;
//        private int maximalLengthOfSite = 300;
//        private boolean doRemoveOrphans = true;
//        private boolean doTrack = false;
//        private String methodName = RankAggregation.METHOD_AR_MEAN;
//        private String tfClass;
        
        public AllParameters()
        {
            setDbSelector(new BasicGenomeSelector());
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
        
        @PropertyName(MessageBundle.PN_OPTION)
        @PropertyDescription(MessageBundle.PD_OPTION)
        public String getOption()
        {
            return option;
        }
        public void setOption(String option)
        {
            Object oldValue = this.option;
            this.option = option;
            firePropertyChange("*", oldValue, option);
        }
        
        @PropertyName(MessageBundle.PN_SPECIES)
        @PropertyDescription(MessageBundle.PD_SPECIES)
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
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_FOLDERS)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_FOLDERS)
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
        
        @PropertyName(MessageBundle.PN_FOLDERS_NAMES)
        @PropertyDescription(MessageBundle.PD_FOLDERS_NAMES)
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
        
//        @PropertyName(MessageBundle.PN_DO_ADD_FREQUENCY_MATRICES)
//        @PropertyDescription(MessageBundle.PD_DO_ADD_FREQUENCY_MATRICES)
//        public boolean getDoAddFrequencyMatices()
//        {
//            return doAddFrequencyMatices;
//        }
//        public void setDoAddFrequencyMatices(boolean doAddFrequencyMatices)
//        {
//            Object oldValue = this.doAddFrequencyMatices;
//            this.doAddFrequencyMatices = doAddFrequencyMatices;
//            // firePropertyChange("doAddFrequencyMatices", oldValue, doAddFrequencyMatices);
//            firePropertyChange("*", oldValue, doAddFrequencyMatices);
//        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_MATRICES)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_MATRICES)
        public DataElementPath getPathToFolderWithMatrices()
        {
            return pathToFolderWithMatrices;
        }
        public void setPathToFolderWithMatrices(DataElementPath pathToFolderWithMatrices)
        {
            Object oldValue = this.pathToFolderWithMatrices;
            this.pathToFolderWithMatrices = pathToFolderWithMatrices;
            firePropertyChange("pathToFolderWithMatrices", oldValue, pathToFolderWithMatrices);
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
        
        /***************************************/
        
//        @PropertyName(MessageBundle.PN_COMBINED_SITE_TYPE)
//        @PropertyDescription(MessageBundle.PD_COMBINED_SITE_TYPE)
//        public String getCombinedSiteType()
//        {
//            return combinedSiteType;
//        }
//        public void setCombinedSiteType(String combinedSiteType)
//        {
//            Object oldValue = this.combinedSiteType;
//            this.combinedSiteType = combinedSiteType;
//            firePropertyChange("combinedSiteType", oldValue, combinedSiteType);
//        }
//        
//        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_TRACKS)
//        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_TRACKS)
//        public DataElementPath getPathToFolderWithTracks()
//        {
//            return pathToFolderWithTracks;
//        }
//        public void setPathToFolderWithTracks(DataElementPath pathToFolderWithTracks)
//        {
//            Object oldValue = this.pathToFolderWithTracks;
//            this.pathToFolderWithTracks = pathToFolderWithTracks;
//            firePropertyChange("pathToFolderWithTracks", oldValue, pathToFolderWithTracks);
//        }
//        
//        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_FILES)
//        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_FILES)
//        public DataElementPath getPathToFolderWithFiles()
//        {
//            return pathToFolderWithFiles;
//        }
//        public void setPathToFolderWithFiles(DataElementPath pathToFolderWithFiles)
//        {
//            Object oldValue = this.pathToFolderWithFiles;
//            this.pathToFolderWithFiles = pathToFolderWithFiles;
//            firePropertyChange("pathToFolderWithFiles", oldValue, pathToFolderWithFiles);
//        }
//        
//        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_SITE_MODELS)
//        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_SITE_MODELS)
//        public DataElementPath getPathToFolderWithSiteModels()
//        {
//            return pathToFolderWithSiteModels;
//        }
//        public void setPathToFolderWithSiteModels(DataElementPath pathToFolderWithSiteModels)
//        {
//            Object oldValue = this.pathToFolderWithSiteModels;
//            this.pathToFolderWithSiteModels = pathToFolderWithSiteModels;
//            firePropertyChange("pathToFolderWithSiteModels", oldValue, pathToFolderWithSiteModels);
//        }
//
//        @PropertyName(MessageBundle.PN_PATH_TO_INPUT_TABLE)
//        @PropertyDescription(MessageBundle.PD_PATH_TO_INPUT_TABLE)
//        public DataElementPath getPathToInputTable()
//        {
//            return pathToInputTable;
//        }
//        public void setPathToInputTable(DataElementPath pathToInputTable)
//        {
//            Object oldValue = this.pathToInputTable;
//            this.pathToInputTable = pathToInputTable;
//            firePropertyChange("pathToInputTable", oldValue, pathToInputTable);
//        }
//        
//        @PropertyName(MessageBundle.PN_MINIMAL_LENGTH_OF_SITE)
//        @PropertyDescription(MessageBundle.PD_MINIMAL_LENGTH_OF_SITE)
//        public int getMinimalLengthOfSite()
//        {
//            return minimalLengthOfSite;
//        }
//        public void setMinimalLengthOfSite(int minimalLengthOfSite)
//        {
//            Object oldValue = this.minimalLengthOfSite;
//            this.minimalLengthOfSite = minimalLengthOfSite;
//            firePropertyChange("minimalLengthOfSite", oldValue, minimalLengthOfSite);
//        }
//        
//        @PropertyName(MessageBundle.PN_MAXIMAL_LENGTH_OF_SITE)
//        @PropertyDescription(MessageBundle.PD_MAXIMAL_LENGTH_OF_SITE)
//        public int getMaximalLengthOfSite()
//        {
//            return maximalLengthOfSite;
//        }
//        public void setMaximalLengthOfSite(int maximalLengthOfSite)
//        {
//            Object oldValue = this.maximalLengthOfSite;
//            this.maximalLengthOfSite = maximalLengthOfSite;
//            firePropertyChange("maximalLengthOfSite", oldValue, maximalLengthOfSite);
//        }
//        
//        @PropertyName(MessageBundle.PN_DO_REMOVE_ORPHANS)
//        @PropertyDescription(MessageBundle.PD_DO_REMOVE_ORPHANS)
//        public boolean getDoRemoveOrphans()
//        {
//            return doRemoveOrphans;
//        }
//        public void setDoRemoveOrphans(boolean doRemoveOrphans)
//        {
//            Object oldValue = this.doRemoveOrphans;
//            this.doRemoveOrphans = doRemoveOrphans;
//            firePropertyChange("doRemoveOrphans", oldValue, doRemoveOrphans);
//        }
//        
//        @PropertyName(MessageBundle.PN_DO_TRACK)
//        @PropertyDescription(MessageBundle.PD_DO_TRACK)
//        public boolean getDoTrack()
//        {
//            return doTrack;
//        }
//        public void setDoTrack(boolean doTrack)
//        {
//            Object oldValue = this.doTrack;
//            this.doTrack = doTrack;
//            firePropertyChange("doTrack", oldValue, doTrack);
//        }
//        
//        @PropertyName(MessageBundle.PN_METHOD_NAME)
//        @PropertyDescription(MessageBundle.PD_METHOD_NAME)
//        public String getMethodName()
//        {
//            return methodName;
//        }
//        public void setMethodName(String methodName)
//        {
//            Object oldValue = this.methodName;
//            this.methodName = methodName;
//            firePropertyChange("methodName", oldValue, methodName);
//        }
//        
//        @PropertyName(MessageBundle.PN_TF_CLASS)
//        @PropertyDescription(MessageBundle.PN_TF_CLASS)
//        public String getTfClass()
//        {
//            return tfClass;
//        }
//        public void setTfClass(String tfClass)
//        {
//            Object oldValue = this.tfClass;
//            this.tfClass = tfClass;
//            firePropertyChange("tfClass", oldValue, tfClass);
//        }
    }

    
    public static class ParametersForOption01 extends AllParameters
    {}
    
    public static class ParametersForOption01BeanInfo extends BeanInfoEx2<ParametersForOption01>
    {
        public ParametersForOption01BeanInfo()
        {
            super(ParametersForOption01.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            add(DataElementPathEditor.registerOutput("pathToFolderWithFolders", beanClass, FolderCollection.class, true));
            add("foldersNames", FoldersNamesSelectorForOption01.class);
            //add("doAddFrequencyMatices");
            add(DataElementPathEditor.registerInputChild("pathToFolderWithMatrices", beanClass, FrequencyMatrix.class, true));
        }
    }

    public static class ParametersForOption02 extends AllParameters
    {}
    
    public static class ParametersForOption02BeanInfo extends BeanInfoEx2<ParametersForOption02>
    {
        public ParametersForOption02BeanInfo()
        {
            super(ParametersForOption02.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerOutput("pathToFolderWithFolders", beanClass, FolderCollection.class, true));
            add("foldersNames", FoldersNamesSelectorForOption01.class);
            //add(DataElementPathEditor.registerInputChild("pathToFolderWithFiles", beanClass, DataCollection.class, true));

        }
    }
    
    public static class ParametersForOption03 extends AllParameters
    {}
    
    public static class ParametersForOption03BeanInfo extends BeanInfoEx2<ParametersForOption03>
    {
        public ParametersForOption03BeanInfo()
        {
            super(ParametersForOption03.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerOutput("pathToFolderWithFolders", beanClass, FolderCollection.class, true));
            add("foldersNames", FoldersNamesSelectorForOption01.class);
            //add(DataElementPathEditor.registerInputChild("pathToFolderWithFiles", beanClass, DataCollection.class, true));
        }
    }
    
//    public static class ParametersForOption04 extends AllParameters
//    {}
//    
//    public static class ParametersForOption04BeanInfo extends BeanInfoEx2<ParametersForOption04>
//    {
//        public ParametersForOption04BeanInfo()
//        {
//            super(ParametersForOption04.class);
//        }
//        
//        @Override
//        protected void initProperties() throws Exception
//        {
//            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
//            add(DataElementPathEditor.registerOutput("pathToFolderWithFolders", beanClass, FolderCollection.class, true));
//            add("foldersNames", FoldersNamesSelectorForOption04.class);
//            add(DataElementPathEditor.registerInputChild("pathToFolderWithMatrices", beanClass, FrequencyMatrix.class, true));
//        }
//    }
//    
//    public static class ParametersForOption05 extends AllParameters
//    {}
//    
//    public static class ParametersForOption05BeanInfo extends BeanInfoEx2<ParametersForOption05>
//    {
//        public ParametersForOption05BeanInfo()
//        {
//            super(ParametersForOption05.class);
//        }
//        
//        @Override
//        protected void initProperties() throws Exception
//        {
//            add("dbSelector");
//            add(DataElementPathEditor.registerInputChild("pathToFolderWithFiles", beanClass, DataCollection.class, true));
//            add(DataElementPathEditor.registerInputChild("pathToFolderWithMatrices", beanClass, FrequencyMatrix.class, true));
//            add(DataElementPathEditor.registerInputChild("pathToFolderWithSiteModels", beanClass, SiteModel.class, true));
//            add(DataElementPathEditor.registerInput("pathToInputTable", beanClass, TableDataCollection.class, true));
//        }
//    }
//    
//    public static class ParametersForOption06 extends AllParameters
//    {}
//    
//    public static class ParametersForOption06BeanInfo extends BeanInfoEx2<ParametersForOption06>
//    {
//        public ParametersForOption06BeanInfo()
//        {
//            super(ParametersForOption06.class);
//        }
//        
//        @Override
//        protected void initProperties() throws Exception
//        {
//            add(DataElementPathEditor.registerInputChild("pathToFolderWithFiles", beanClass, DataCollection.class, true));
//            add("doRemoveOrphans");
//            add(DataElementPathEditor.registerInput("pathToInputTable", beanClass, TableDataCollection.class, true));
//        }
//    }
//    
//    public static class ParametersForOption07 extends AllParameters
//    {}
//    
//    public static class ParametersForOption07BeanInfo extends BeanInfoEx2<ParametersForOption07>
//    {
//        public ParametersForOption07BeanInfo()
//        {
//            super(ParametersForOption07.class);
//        }
//        
//        @Override
//        protected void initProperties() throws Exception
//        {
//            add(DataElementPathEditor.registerOutput("pathToFolderWithFolders", beanClass, FolderCollection.class, true));
//            add("foldersNames", FoldersNamesSelectorForOption07.class);
//            add(DataElementPathEditor.registerInput("pathToInputTable", beanClass, TableDataCollection.class, true));
//        }
//    }
//    
//    public static class ParametersForOption08 extends AllParameters
//    {}
//    
//    public static class ParametersForOption08BeanInfo extends BeanInfoEx2<ParametersForOption08>
//    {
//        public ParametersForOption08BeanInfo()
//        {
//            super(ParametersForOption08.class);
//        }
//        
//        @Override
//        protected void initProperties() throws Exception
//        {
//            add(DataElementPathEditor.registerInputChild("pathToFolderWithFiles", beanClass, DataCollection.class, true));
//            add("doTrack");
//        }
//    }
//    
//    public static class ParametersForOption09 extends AllParameters
//    {}
//    
//    public static class ParametersForOption09BeanInfo extends BeanInfoEx2<ParametersForOption09>
//    {
//        public ParametersForOption09BeanInfo()
//        {
//            super(ParametersForOption09.class);
//        }
//        
//        @Override
//        protected void initProperties() throws Exception
//        {
//            add(new PropertyDescriptorEx("combinedSiteType", beanClass), CombinedSiteTypeSelector.class);
//            add("tfClass");
//            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
//            add(DataElementPathEditor.registerOutput("pathToFolderWithFolders", beanClass, FolderCollection.class, true));
//            add("foldersNames", FoldersNamesSelectorForOption09.class);
//            add("minimalLengthOfSite");
//            add("maximalLengthOfSite");
//            add(new PropertyDescriptorEx("methodName", beanClass), MethodNameSelector.class);
//        }
//    }

    public static String[] getAvailableOptions()
    {
        return new String[]{OPTION_01, OPTION_02, OPTION_03};
    }
    
    public static class GtrdAnalysisAdvancedParameters extends AllParameters
    {
        ParametersForOption01 parametersForOption01;
        ParametersForOption02 parametersForOption02;
        ParametersForOption03 parametersForOption03;
//        ParametersForOption04 parametersForOption04;
//        ParametersForOption05 parametersForOption05;
//        ParametersForOption06 parametersForOption06;
//        ParametersForOption07 parametersForOption07;
//        ParametersForOption08 parametersForOption08;
//        ParametersForOption09 parametersForOption09;
       
        public GtrdAnalysisAdvancedParameters()
        {
            setParametersForOption01(new ParametersForOption01());
            setParametersForOption02(new ParametersForOption02());
            setParametersForOption03(new ParametersForOption03());
//            setParametersForOption04(new ParametersForOption04());
//            setParametersForOption05(new ParametersForOption05());
//            setParametersForOption06(new ParametersForOption06());
//            setParametersForOption07(new ParametersForOption07());
//            setParametersForOption08(new ParametersForOption08());
//            setParametersForOption09(new ParametersForOption09());
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_01)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_01)
        public ParametersForOption01 getParametersForOption01()
        {
            return parametersForOption01;
        }
        public void setParametersForOption01(ParametersForOption01 parametersForOption01)
        {
            Object oldValue = this.parametersForOption01;
            this.parametersForOption01 = parametersForOption01;
            firePropertyChange("parametersForOption01", oldValue, parametersForOption01);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_02)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_02)
        public ParametersForOption02 getParametersForOption02()
        {
            return parametersForOption02;
        }
        public void setParametersForOption02(ParametersForOption02 parametersForOption02)
        {
            Object oldValue = this.parametersForOption02;
            this.parametersForOption02 = parametersForOption02;
            firePropertyChange("parametersForOption02", oldValue, parametersForOption02);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_03)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_03)
        public ParametersForOption03 getParametersForOption03()
        {
            return parametersForOption03;
        }
        public void setParametersForOption03(ParametersForOption03 parametersForOption03)
        {
            Object oldValue = this.parametersForOption03;
            this.parametersForOption03 = parametersForOption03;
            firePropertyChange("parametersForOption03", oldValue, parametersForOption03);
        }
        
//        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_04)
//        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_04)
//        public ParametersForOption04 getParametersForOption04()
//        {
//            return parametersForOption04;
//        }
//        public void setParametersForOption04(ParametersForOption04 parametersForOption04)
//        {
//            Object oldValue = this.parametersForOption04;
//            this.parametersForOption04 = parametersForOption04;
//            firePropertyChange("parametersForOption04", oldValue, parametersForOption04);
//        }
//        
//        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_05)
//        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_05)
//        public ParametersForOption05 getParametersForOption05()
//        {
//            return parametersForOption05;
//        }
//        public void setParametersForOption05(ParametersForOption05 parametersForOption05)
//        {
//            Object oldValue = this.parametersForOption05;
//            this.parametersForOption05 = parametersForOption05;
//            firePropertyChange("parametersForOption05", oldValue, parametersForOption05);
//        }
//        
//        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_06)
//        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_06)
//        public ParametersForOption06 getParametersForOption06()
//        {
//            return parametersForOption06;
//        }
//        public void setParametersForOption06(ParametersForOption06 parametersForOption06)
//        {
//            Object oldValue = this.parametersForOption06;
//            this.parametersForOption06 = parametersForOption06;
//            firePropertyChange("parametersForOption06", oldValue, parametersForOption06);
//        }
//        
//        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_07)
//        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_07)
//        public ParametersForOption07 getParametersForOption07()
//        {
//            return parametersForOption07;
//        }
//        public void setParametersForOption07(ParametersForOption07 parametersForOption07)
//        {
//            Object oldValue = this.parametersForOption07;
//            this.parametersForOption07 = parametersForOption07;
//            firePropertyChange("parametersForOption07", oldValue, parametersForOption07);
//        }
//        
//        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_08)
//        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_08)
//        public ParametersForOption08 getParametersForOption08()
//        {
//            return parametersForOption08;
//        }
//        public void setParametersForOption08(ParametersForOption08 parametersForOption08)
//        {
//            Object oldValue = this.parametersForOption08;
//            this.parametersForOption08 = parametersForOption08;
//            firePropertyChange("parametersForOption08", oldValue, parametersForOption08);
//        }
//        
//        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_09)
//        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_09)
//        public ParametersForOption09 getParametersForOption09()
//        {
//            return parametersForOption09;
//        }
//        public void setParametersForOption09(ParametersForOption09 parametersForOption09)
//        {
//            Object oldValue = this.parametersForOption09;
//            this.parametersForOption09 = parametersForOption09;
//            firePropertyChange("parametersForOption09", oldValue, parametersForOption09);
//        }
        
        public boolean isParametersForOption01Hidden()
        {
            return( ! getOption().equals(OPTION_01) );
        }
        
        public boolean isParametersForOption02Hidden()
        {
            return( ! getOption().equals(OPTION_02) );
        }
        
        public boolean isParametersForOption03Hidden()
        {
            return( ! getOption().equals(OPTION_03) );
        }
        
//        public boolean isParametersForOption04Hidden()
//        {
//            return( ! getOption().equals(OPTION_04) );
//        }
//        
//        public boolean isParametersForOption05Hidden()
//        {
//            return( ! getOption().equals(OPTION_05) );
//        }
//        
//        public boolean isParametersForOption06Hidden()
//        {
//            return( ! getOption().equals(OPTION_06) );
//        }
//        
//        public boolean isParametersForOption07Hidden()
//        {
//            return( ! getOption().equals(OPTION_07) );
//        }
//        
//        public boolean isParametersForOption08Hidden()
//        {
//            return( ! getOption().equals(OPTION_08) );
//        }
//        
//        public boolean isParametersForOption09Hidden()
//        {
//            return( ! getOption().equals(OPTION_09) );
//        }
    }
    
    public static class OptionSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return getAvailableOptions();
        }
    }
    
    public static class MethodNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return RankAggregation.getAvailableMethodNames();
        }
    }
    
    public static class CombinedSiteTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return CombinedSites.getAvailableSiteTypes();
        }
    }
    
    public static class FoldersNamesSelectorForOption01 extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> folders = ((ParametersForOption01)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
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
    
//    public static class FoldersNamesSelectorForOption04 extends GenericMultiSelectEditor
//    {
//        @Override
//        protected String[] getAvailableValues()
//        {
//            try
//            {
//                DataCollection<DataElement> folders = ((ParametersForOption04)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
//                String[] foldersNames = folders.getNameList().toArray(new String[0]);
//                Arrays.sort(foldersNames, String.CASE_INSENSITIVE_ORDER);
//                return foldersNames;
//            }
//            catch( RepositoryException e )
//            {
//                return new String[]{"(please select folder with folders)"};
//            }
//            catch( Exception e )
//            {
//                return new String[]{"(folder doesn't contain the folders)"};
//            }
//        }
//    }
    
//    public static class FoldersNamesSelectorForOption07 extends GenericMultiSelectEditor
//    {
//        @Override
//        protected String[] getAvailableValues()
//        {
//            try
//            {
//                DataCollection<DataElement> folders = ((ParametersForOption07)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
//                String[] foldersNames = folders.getNameList().toArray(new String[0]);
//                Arrays.sort(foldersNames, String.CASE_INSENSITIVE_ORDER);
//                return foldersNames;
//            }
//            catch( RepositoryException e )
//            {
//                return new String[]{"(please select folder with folders)"};
//            }
//            catch( Exception e )
//            {
//                return new String[]{"(folder doesn't contain the folders)"};
//            }
//        }
//    }
    
//    public static class FoldersNamesSelectorForOption09 extends GenericMultiSelectEditor
//    {
//        @Override
//        protected String[] getAvailableValues()
//        {
//            try
//            {
//                DataCollection<DataElement> folders = ((ParametersForOption09)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
//                String[] foldersNames = folders.getNameList().toArray(new String[0]);
//                Arrays.sort(foldersNames, String.CASE_INSENSITIVE_ORDER);
//                return foldersNames;
//            }
//            catch( RepositoryException e )
//            {
//                return new String[]{"(please select folder with folders)"};
//            }
//            catch( Exception e )
//            {
//                return new String[]{"(folder doesn't contain the folders)"};
//            }
//        }
//    }

    public static class GtrdAnalysisAdvancedParametersBeanInfo extends BeanInfoEx2<GtrdAnalysisAdvancedParameters>
    {
        public GtrdAnalysisAdvancedParametersBeanInfo()
        {
            super(GtrdAnalysisAdvancedParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("option", beanClass), OptionSelector.class);
            addHidden("parametersForOption01", "isParametersForOption01Hidden");
            addHidden("parametersForOption02", "isParametersForOption02Hidden");
            addHidden("parametersForOption03", "isParametersForOption03Hidden");
//            addHidden("parametersForOption04", "isParametersForOption04Hidden");
//            addHidden("parametersForOption05", "isParametersForOption05Hidden");
//            addHidden("parametersForOption06", "isParametersForOption06Hidden");
//            addHidden("parametersForOption07", "isParametersForOption07Hidden");
//            addHidden("parametersForOption08", "isParametersForOption08Hidden");
//            addHidden("parametersForOption09", "isParametersForOption09Hidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}
