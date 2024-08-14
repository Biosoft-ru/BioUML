/* $Id$ */

// 02.04.22

package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.utils.CistromUtils.CistromConstructor;
import biouml.plugins.gtrd.utils.EnsemblUtils;
import biouml.plugins.gtrd.utils.FunSiteUtils.CombinedSites;
import biouml.plugins.gtrd.utils.MetaClusterConsrtruction;
import biouml.plugins.gtrd.utils.MetaClusterConsrtruction.Metara;
import biouml.plugins.gtrd.utils.TrackInfo;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class CistromConstruction extends AnalysisMethodSupport<CistromConstruction.CistromConstructionParameters>
{
    public CistromConstruction(DataCollection<?> origin, String name)
    {
         super(origin, name, new CistromConstructionParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut()
    {
        log.info(" ********************************************************************************************");
        log.info(" *    Cistrom construction. There are 3 modes :                                             *");
        log.info(" * 1. Meta-clusters construction (METARA is used for all TFs available for given cell line) *");
        log.info(" * 2. Meta-clusters construction (METARA is used for all TFs available)                     *");
        log.info(" * 3. DNase meta-clusters constriction                                                      *");
        log.info(" ********************************************************************************************");
        
        String option = parameters.getOption();
        
        // 31.03.22
        String tfClassificationType = parameters.getTfClassificationType();
        String cellLineStatus = parameters.getCellLineStatus();
        Species givenSpecie = parameters.getSpecies();
        DataElementPath pathToFolderWithFolders = parameters.getPathToFolderWithFolders();
        String[] foldersNames = parameters.getFoldersNames();
        String combinedPeakType = parameters.getCombinedPeakType();
        int minimalLengthOfPeaks = parameters.getMinimalLengthOfPeaks();
        int maximalLengthOfPeaks = parameters.getMaximalLengthOfPeaks();
        boolean doPerformQualityControl = parameters.getDoPerformQualityControl();
        double fpcmThreshold = parameters.getFpcmThreshold();
        fpcmThreshold = doPerformQualityControl ? fpcmThreshold : Double.NaN;
        int siteNumberThreshold = parameters.getSiteNumberThreshold();
        String rankAggregationMethod = parameters.getRankAggregationMethod();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        switch( option )
        {
        	// 01.04.22
        	//case CistromConstructor.OPTION_01 : log.info("Selected option : " + CistromConstructor.OPTION_01);
            case MetaClusterConsrtruction.OPTION_01 : log.info("Selected option : " + MetaClusterConsrtruction.OPTION_01);
            										  String cellLine = parameters.getParametersForOption01().getCellLine();
            										  log.info("cellLine = " + cellLine);
            										  boolean doRemoveCellTreatments = parameters.getParametersForOption01().getDoRemoveCellTreatments();
            										  new CistromConstructor(option, givenSpecie, cellLine, combinedPeakType, doRemoveCellTreatments, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder, null, null, false, false, jobControl, 0, 100);
            										  break;
            // TODO !!!!!!!!!!!!!! It is main version !!!


            										  
            // 04.04.22
            // case CistromConstructor.OPTION_02 : log.info("Selected option : " + CistromConstructor.OPTION_02);
            case MetaClusterConsrtruction.OPTION_02 : log.info("Selected option : " + MetaClusterConsrtruction.OPTION_02);
//            										  new CistromConstructor(option, givenSpecie, null, combinedPeakType,
//            										  false, rankAggregationMethod, fpcmThreshold, siteNumberThreshold,
//            										  pathToFolderWithFolders, foldersNames, minimalLengthOfPeaks,
//            										  maximalLengthOfPeaks, pathToOutputFolder, null, null, false, false, jobControl, 0, 100);
//            										  break;
            										  String tfClass = null;
            										  cellLine = null;
            										  new Metara(option, givenSpecie, cellLine, cellLineStatus, combinedPeakType,
            									    			 rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders,
            									    			 foldersNames, minimalLengthOfPeaks, maximalLengthOfPeaks, tfClassificationType, tfClass,
            									    			 null, false, false, pathToOutputFolder, jobControl, 0, 100); break;
            case MetaClusterConsrtruction.OPTION_03 : log.info("Selected option : " + MetaClusterConsrtruction.OPTION_03);
                                                new CistromConstructor(option, givenSpecie, null, combinedPeakType, false, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder, null, null, false, false, jobControl, 0, 100);
                                                break;
        }
        return pathToOutputFolder.getDataCollection();
    }
    
    /************************************************************************/
    /************************** Utils for AnalysisMethodSupport *************/
    /************************************************************************/
    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Please, select option (i.e. select the concrete session of given analysis).";
        
        // 30.03.22
        public static final String PN_TF_CLASSIFICATION_TYPE = "TF-classification type";
        public static final String PD_TF_CLASSIFICATION_TYPE = "Please, select TF-classification type";
        
        // 31.03.22
        public static final String PN_CELL_LINE_STATUS = "Cell line status";
        public static final String PD_CELL_LINE_STATUS = "Please, select cell line status";
        
        public static final String PN_SPECIES = "Species";
        public static final String PD_SPECIES = "Please, select a taxonomical species";
        
        public static final String PN_PATH_TO_FOLDER_WITH_CELL_LINES = "Path to folder with cell lines";
        public static final String PD_PATH_TO_FOLDER_WITH_CELL_LINES = "Path to folder with GTRD cell lines";
        
        public static final String PN_PATH_TO_FOLDER_WITH_FOLDERS = "Path to folder with folders";
        public static final String PD_PATH_TO_FOLDER_WITH_FOLDERS = "Path to folder with folders (Each internal folder contains ChIP-seq tracks with peaks)";
        
        public static final String PN_FOLDERS_NAMES = "Folders names";
        public static final String PD_FOLDERS_NAMES = "Please, select folders names";
        
        public static final String PN_COMBINED_PEAK_TYPE = "Type of combined peaks";
        public static final String PD_COMBINED_PEAK_TYPE = "Please, select type of combined peaks";

        public static final String PN_MINIMAL_LENGTH_OF_PEAKS = "Minimal length of peaks";
        public static final String PD_MINIMAL_LENGTH_OF_PEAKS = "All peaks that are shorter than this threshold will be prolongated";
        
        public static final String PN_MAXIMAL_LENGTH_OF_PEAKS = "Maximal length of peaks";
        public static final String PD_MAXIMAL_LENGTH_OF_PEAKS = "All peaks that are longer than this threshold will be truncated";
        
        public static final String PN_DO_PERFORM_QUALITY_CONTROL = "Do perform quality control?";
        public static final String PD_DO_PERFORM_QUALITY_CONTROL = "Do perform quality control?";
        
        public static final String PN_DO_REMOVE_CELL_TREATMENTS = "Do remove cell treatments?";
        public static final String PD_DO_REMOVE_CELL_TREATMENTS = "Do remove tracks when cell lines were treated?";
        
        public static final String PN_FPCM_THRESHOLD = "FPCM threshold";
        public static final String PD_FPCM_THRESHOLD = "FPCM threshold";
        
        public static final String PN_SITE_NUMBER_THRESHOLD = "Site number threshold";
        public static final String PD_SITE_NUMBER_THRESHOLD = "If size of a data set is less than this threshold, then data set will be excluded from rank aggregation";
        
        public static final String PN_RANK_AGGREGATION_METHOD = "Rank aggregation method";
        public static final String PD_RANK_AGGREGATION_METHOD = "Please, select rank aggregation method";
        
        public static final String PN_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        public static final String PD_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        
        public static final String PN_CELL_LINE = "Cell line";
        public static final String PD_CELL_LINE = "Please, select cell line.";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }
    
    public static class AllParameters extends AbstractAnalysisParameters
    {
    	// 01.04.22
        //private String option = CistromConstructor.OPTION_02;
    	private String option = MetaClusterConsrtruction.OPTION_02;
    	
        private Species species = Species.getDefaultSpecies(null);
        
        // 30.03.22
        private String tfClassificationType = EnsemblUtils.TF_CLASSIFICATION_TYPE_UNIPROT;
        private String cellLineStatus = TrackInfo.CELL_LINE_STATUS_ALL;

        private DataElementPath pathToFolderWithFolders;
        private String[] foldersNames;
        private String combinedPeakType = CombinedSites.SITE_TYPE_OVERLAPPED;
        private int minimalLengthOfPeaks = 20;
        private int maximalLengthOfPeaks = 1000000;
        private boolean doPerformQualityControl = true;
        private double fpcmThreshold = 3.0;
        private int siteNumberThreshold = 2000;
        private String rankAggregationMethod = RankAggregation.METHOD_AR_MEAN;
        private DataElementPath pathToOutputFolder;
        
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

        // 30.03.22
        @PropertyName(MessageBundle.PN_TF_CLASSIFICATION_TYPE)
        @PropertyDescription(MessageBundle.PD_TF_CLASSIFICATION_TYPE)
        public String getTfClassificationType()
        {
            return tfClassificationType;
        }
        // 30.03.22
        public void setTfClassificationType(String tfClassificationType)
        {
            Object oldValue = this.tfClassificationType;
            this.tfClassificationType = tfClassificationType;
            firePropertyChange("*", oldValue, tfClassificationType);
        }

        // 31.03.22
        @PropertyName(MessageBundle.PN_CELL_LINE_STATUS)
        @PropertyDescription(MessageBundle.PD_CELL_LINE_STATUS)
        public String getCellLineStatus()
        {
            return cellLineStatus;
        }
        // 31.03.22
        public void setCellLineStatus(String cellLineStatus)
        {
            Object oldValue = this.cellLineStatus;
            this.cellLineStatus = cellLineStatus;
            firePropertyChange("*", oldValue, cellLineStatus);
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
        
        @PropertyName(MessageBundle.PN_COMBINED_PEAK_TYPE)
        @PropertyDescription(MessageBundle.PD_COMBINED_PEAK_TYPE)
        public String getCombinedPeakType()
        {
            return combinedPeakType;
        }
        public void setCombinedPeakType(String combinedPeakType)
        {
            Object oldValue = this.combinedPeakType;
            this.combinedPeakType = combinedPeakType;
            firePropertyChange("combinedPeakType", oldValue, combinedPeakType);
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

        @PropertyName(MessageBundle.PN_RANK_AGGREGATION_METHOD)
        @PropertyDescription(MessageBundle.PD_RANK_AGGREGATION_METHOD)
        public String getRankAggregationMethod()
        {
            return rankAggregationMethod;
        }
        public void setRankAggregationMethod(String rankAggregationMethod)
        {
            Object oldValue = this.rankAggregationMethod;
            this.rankAggregationMethod = rankAggregationMethod;
            firePropertyChange("rankAggregationMethod", oldValue, rankAggregationMethod);
        }

        @PropertyName (MessageBundle.PN_PATH_TO_OUTPUT_FOLDER)
        @PropertyDescription ( MessageBundle.PD_PATH_TO_OUTPUT_FOLDER )
        public DataElementPath getPathToOutputFolder()
        {
            return pathToOutputFolder;
        }
        public void setPathToOutputFolder(DataElementPath pathToOutputFolder)
        {
            Object oldValue = this.pathToOutputFolder;
            this.pathToOutputFolder = pathToOutputFolder;
            firePropertyChange( "pathToOutputFolder", oldValue, pathToOutputFolder);
        }
    }

    public static class ParametersForOption01 extends OptionEx
    {
        private String cellLine;
        private boolean doRemoveCellTreatments = true;
        private DataElementPath pathToFolderWithCellLines;

        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_CELL_LINES)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_CELL_LINES)
        public DataElementPath getPathToFolderWithCellLines()
        {
            return pathToFolderWithCellLines;
        }
        public void setPathToFolderWithCellLines(DataElementPath pathToFolderWithCellLines)
        {
            Object oldValue = this.pathToFolderWithCellLines;
            this.pathToFolderWithCellLines = pathToFolderWithCellLines;
            firePropertyChange("*", oldValue, pathToFolderWithCellLines);
        }

        @PropertyName(MessageBundle.PN_CELL_LINE)
        @PropertyDescription(MessageBundle.PD_CELL_LINE)
        public String getCellLine()
        {
            return cellLine;
        }
        public void setCellLine(String cellLine)
        {
            Object oldValue = this.cellLine;
            this.cellLine = cellLine;
            firePropertyChange("cellLine", oldValue, cellLine);
        }
        
        @PropertyName(MessageBundle.PN_DO_REMOVE_CELL_TREATMENTS)
        @PropertyDescription( MessageBundle.PD_DO_REMOVE_CELL_TREATMENTS)
        public boolean getDoRemoveCellTreatments()
        {
            return doRemoveCellTreatments;
        }
        public void setDoRemoveCellTreatments(boolean doRemoveCellTreatments)
        {
            Object oldValue = this.doRemoveCellTreatments;
            this.doRemoveCellTreatments = doRemoveCellTreatments;
            firePropertyChange("doRemoveCellTreatments", oldValue, doRemoveCellTreatments);
        }
    }
    
    public static class ParametersForOption01BeanInfo extends BeanInfoEx2<ParametersForOption01>
    {
        public ParametersForOption01BeanInfo()
        {
            super(ParametersForOption01.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToFolderWithCellLines", beanClass, CellLine.class, true));
            add(new PropertyDescriptorEx("cellLine", beanClass), CellLineSelector.class);
            add("doRemoveCellTreatments");
        }
    }
    
    public static class CistromConstructionParameters extends AllParameters
    {
        ParametersForOption01 parametersForOption01;

        public CistromConstructionParameters()
        {
            setParametersForOption01(new ParametersForOption01());
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
            this.parametersForOption01.setParent( this );
            firePropertyChange("parametersForOption01", oldValue, parametersForOption01);
        }
        
        public boolean areParametersForOption01Hidden()
        {
        	// 01.4.22
            //return ! getOption().equals(CistromConstructor.OPTION_01);
        	return ! getOption().equals(MetaClusterConsrtruction.OPTION_01);
        }
        
        public boolean isFpcmThresholdHidden()
        {
            return ! getDoPerformQualityControl();
        }
        
        // 30.03.22
        public boolean isTfClassificationTypeHidden()
        {
        	String option = getOption();
        	// 01.04.22
            //return ! option.equals(CistromConstructor.OPTION_01) & ! option.equals(CistromConstructor.OPTION_02);
        	return ! option.equals(MetaClusterConsrtruction.OPTION_01) & ! option.equals(MetaClusterConsrtruction.OPTION_02);
        }
        
        // 31.03.22
        public boolean isCellLineStatusHidden()
        {
        	String option = getOption();
        	// 01.04.22
            return ! option.equals(MetaClusterConsrtruction.OPTION_01) & ! option.equals(MetaClusterConsrtruction.OPTION_02);
        }
    }
    
    public static class OptionSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return getAvailableOptions();
        }
    }
    
    private static String[] getAvailableOptions()
    {
    	// 01.04.22
        //return new String[]{CistromConstructor.OPTION_01, CistromConstructor.OPTION_02, CistromConstructor.OPTION_03};
    	return new String[]{MetaClusterConsrtruction.OPTION_01, MetaClusterConsrtruction.OPTION_02, MetaClusterConsrtruction.OPTION_03};
    }
    
    // 30.03.22
    public static class TfClassificationTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return getAvailableTfClassificationType();
        }
    }

    // 30.03.22
    private static String[] getAvailableTfClassificationType()
    {
        return new String[]{EnsemblUtils.TF_CLASSIFICATION_TYPE_UNIPROT, EnsemblUtils.TF_CLASSIFICATION_TYPE_TF_CLASS};
    }
    
    // 31.03.22
    public static class CellLineStatusSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return TrackInfo.getAvailableCellLineStatus();
        }
    }

    // 01.04.22
//    private static String[] getAvailableCellLineStatus()
//    {
//        return new String[]{CELL_LINE_STATUS_UNTREATED, CELL_LINE_STATUS_TREATED, CELL_LINE_STATUS_ALL};
//    }
    
    public static class FoldersNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> folders = ((CistromConstructionParameters)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
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
    
    public static class CombinedPeakTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return CombinedSites.getAvailableSiteTypes();
        }
    }
    
    public static class CellLineSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
              // old version
//            ParametersForOption01 par = (ParametersForOption01)getBean();
//            CistromConstructionParameters parentParams = (CistromConstructionParameters)par.getParent();
//            DataElementPath pathToFolderWithFolders = parentParams.getPathToFolderWithFolders();
//            if( pathToFolderWithFolders == null ) return null;
//            String[] foldersNames = parentParams.getFoldersNames();
//            Species givenSpecie = parentParams.getSpecies();
//            TrackInfo[] trackInfos = TrackInfo.removeTrackInfosWithoutTfClasses(TrackInfo.getTracksInfo(pathToFolderWithFolders, foldersNames, givenSpecie, null, null, null));
//            String[] distinctCellLines = TrackInfo.getDistinctCellLines(trackInfos);
//            Arrays.sort(distinctCellLines, String.CASE_INSENSITIVE_ORDER);
//            return distinctCellLines;
            
            // new version
            ParametersForOption01 par = (ParametersForOption01)getBean();
            CistromConstructionParameters parentParams = (CistromConstructionParameters)par.getParent();
            DataElementPath pathToFolderWithCellLines = par.getPathToFolderWithCellLines();
            String givenSpecieName = parentParams.getSpecies().getLatinName();
            DataCollection<CellLine> cellLines = pathToFolderWithCellLines.getDataCollection(CellLine.class);
            List<String> list = new ArrayList<>();
            for( CellLine cellLine : cellLines )
                if( cellLine.getSpecies().getLatinName().equals(givenSpecieName) )
                    list.add(cellLine.getTitle());
            String[] distinctCellLines = list.toArray(new String[0]);
            Arrays.sort(distinctCellLines, String.CASE_INSENSITIVE_ORDER);
            return distinctCellLines;
        }
    }
    
    public static class RankAggregationMethodSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return RankAggregation.getAvailableMethodNames();
        }
    }
    
    public static class CistromConstructionParametersBeanInfo extends BeanInfoEx2<CistromConstructionParameters>
    {
        public CistromConstructionParametersBeanInfo()
        {
            super(CistromConstructionParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("option", beanClass), OptionSelector.class);
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            
            // 30.03.22
            addHidden(new PropertyDescriptorEx("tfClassificationType", beanClass), TfClassificationTypeSelector.class, "isTfClassificationTypeHidden");
            addHidden(new PropertyDescriptorEx("cellLineStatus", beanClass), CellLineStatusSelector.class, "isCellLineStatusHidden");
            
            add(DataElementPathEditor.registerInput("pathToFolderWithFolders", beanClass, FolderCollection.class));
            add("foldersNames", FoldersNamesSelector.class);
            add(new PropertyDescriptorEx("combinedPeakType", beanClass), CombinedPeakTypeSelector.class);
            add("minimalLengthOfPeaks");
            add("maximalLengthOfPeaks");
            add("doPerformQualityControl");
            addHidden("fpcmThreshold", "isFpcmThresholdHidden");
            add("siteNumberThreshold");
            add(new PropertyDescriptorEx("rankAggregationMethod", beanClass), RankAggregationMethodSelector.class);
            addHidden("parametersForOption01", "areParametersForOption01Hidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}
