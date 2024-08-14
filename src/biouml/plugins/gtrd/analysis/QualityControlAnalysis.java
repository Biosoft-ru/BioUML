/* $Id$ */

package biouml.plugins.gtrd.analysis;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.FunSiteUtils;
import biouml.plugins.gtrd.utils.FunSiteUtils.CombinedSites;
import biouml.plugins.gtrd.utils.FunSiteUtils.QualityControlSites;
import biouml.plugins.gtrd.utils.SiteModelUtils.RocCurve;
import biouml.plugins.gtrd.utils.SiteModelUtils.SiteModelComposed;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class QualityControlAnalysis extends AnalysisMethodSupport<QualityControlAnalysis.QualityControlAnalysisParameters>
{
    public static final String SINGLE_FOLDER = "Datasets (BED-files or tracks) are located in single folder";
    public static final String SEVERAL_FOLDERS = "Datasets are located in several folders";
    
    public QualityControlAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new QualityControlAnalysisParameters());
    }
    @Override
    public DataCollection<?> justAnalyzeAndPut()
    {
        log.info("************************************************************");
        log.info("*                QualityControlAnalysis                    *");
        log.info("* Calculation of quality control metrics FPCM and FNCMs    *");
        log.info("* and AUCs (ROC-curves) for datasets (BED-files or tracks) *");
        log.info("* that are located in                                      *");
        log.info("* a) single folder                                         *");
        log.info("* b) several folders                                       *");
        log.info("************************************************************");
        String dataType = parameters.getDataType();
        String combinedPeakType = parameters.getCombinedPeakType();
        int minimalLengthOfBindingRegion = parameters.getMinimalLengthOfBindingRegion();
        int maximalLengthOfBindingRegion = parameters.getMaximalLengthOfBindingRegion();
        double fpcmThreshold = parameters.getFpcmThreshold();
        boolean doAUCs = parameters.getDoAucs();
        DataElementPath pathToFolderWithSiteModels = doAUCs ? parameters.getPathToFolderWithSiteModels() : null;
        String siteModelName = doAUCs ? parameters.getSiteModelName() : null;
        DataElementPath pathToSequences = doAUCs ? parameters.getDbSelector().getSequenceCollectionPath() : null;
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        
        // 1. Calculate combinedFunSites.
        FunSite[] combinedFunSites = null;
        
        String[] dataSetNames = null;
        switch( dataType )
        {
            case SEVERAL_FOLDERS : PathToDataSet[] pathsToDataSets = parameters.getParametersForSeveralFolders().getPathsToDataSets();
                                   ru.biosoft.access.core.DataElementPath[] paths = new ru.biosoft.access.core.DataElementPath[pathsToDataSets.length];
                                   dataSetNames = new String[pathsToDataSets.length];
                                   for( int i = 0; i < pathsToDataSets.length; i++ )
                                   {
                                       paths[i] = pathsToDataSets[i].getPathToDataSet();
                                       dataSetNames[i] = paths[i].toString();
                                   }
                                   CombinedSites css = new CombinedSites(combinedPeakType, paths, dataSetNames, minimalLengthOfBindingRegion, maximalLengthOfBindingRegion, true);
                                   combinedFunSites = css.getCombinedSites();
                                   break;
            case SINGLE_FOLDER   : DataElementPath pathToFolderWithFiles = parameters.getParametersForSingleFolder().getPathToFolderWithFiles();
                                   String[] fileNames = parameters.getParametersForSingleFolder().getFileNames();
                                   dataSetNames = fileNames;
                                   css = new CombinedSites(combinedPeakType, pathToFolderWithFiles, fileNames, minimalLengthOfBindingRegion, maximalLengthOfBindingRegion, true);
                                   combinedFunSites = css.getCombinedSites();
                                   break;    
            default              : return pathToOutputFolder.getDataCollection();
        }
        
        // 2. Calculate and write output files.
        for( String s : dataSetNames )
            log.info("dataset name = " + s);
        log.info("Number of merged regions = " + combinedFunSites.length);
        FunSiteUtils.writeSitesToBedFile(combinedFunSites, pathToOutputFolder, "all_merged_regions_bed_file");
        FunSiteUtils.writeSitesToSqlTrack(combinedFunSites, null, null, pathToOutputFolder, "all_merged_regions_track");
        FunSite[] funSitesWithoutOrphans = QualityControlSites.removeOrphans(combinedFunSites);
        FunSiteUtils.writeSitesToBedFile(funSitesWithoutOrphans, pathToOutputFolder, "merged_regions_without_orphans_bed_file");
        FunSiteUtils.writeSitesToSqlTrack(funSitesWithoutOrphans, null, null, pathToOutputFolder, "merged_regions_without_orphans_track");
        DataMatrix dm = QualityControlSites.calculateQualityMetrics(combinedFunSites, dataSetNames, fpcmThreshold);
        dm.writeDataMatrix(false, pathToOutputFolder, "quality_metrics", log);
        if( doAUCs )
            getRocCurvesAndAucs(pathToFolderWithSiteModels, siteModelName, combinedFunSites, pathToSequences, pathToOutputFolder);
        return pathToOutputFolder.getDataCollection();
    }
    
    private void getRocCurvesAndAucs(DataElementPath pathToFolderWithSiteModels, String siteModelName, FunSite[] combinedFunSites, DataElementPath pathToSequences, DataElementPath pathToOutputFolder)
    {
        // TODO: To make w as input parameter
        int w = 100;
        FunSite[] funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, combinedFunSites);
        SiteModel siteModel = pathToFolderWithSiteModels.getChildPath(siteModelName).getDataElement(SiteModel.class);
        int lengthOfSequenceRegion = w + siteModel.getLength();
        Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
        log.info("number of sequences = " + sequences.length);
        log.info("ROC-curve are producing");
        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
        double[][] xValuesForCurves = new double[2][], yValuesForCurves = new double[2][];
        double[] aucs = new double[2];
        for( int i = 0; i < 2; i++ )
        {
            if( i == 1 )
                sequences = FunSiteUtils.removeOrphanSequences(funSites, sequences);
            RocCurve rocCurve = new RocCurve(smc, sequences, 10, 0);
            double[][] curve  = rocCurve.getRocCurve();
            xValuesForCurves[i] = curve[0];
            yValuesForCurves[i] = curve[1];
            aucs[i] = rocCurve.getAuc();
            log.info("AUC = " + aucs[i]);
        }
        DataMatrix dm = new DataMatrix(new String[]{"Whole dataset", "Dataset without orphans"}, "AUC", aucs);
        dm.writeDataMatrix(false, pathToOutputFolder, "AUCs", log);
        Chart chart = ChartUtils.createChart(xValuesForCurves, yValuesForCurves, new String[]{"Whole dataset", "Dataset without orphans"}, null, null, null, null, "Specificity", "Sensitivity", true);
        DataCollection<DataElement> parent  = pathToOutputFolder.getDataCollection();
        ChartDataElement chartDE = new ChartDataElement("ROC-curves", parent, chart);
        parent.put(chartDE);
        TableAndFileUtils.addChartToTable("chart with ROC-curve", chart, pathToOutputFolder.getChildPath("_chart_with_ROC_curve"));
    }
    
    // TODO: temp; write sites to BED file
    public void rab(DataElementPath pathToFolderWithFolders, String[] foldersNames, String trackName, DataElementPath pathToOutputFolder)
    {
        for( String folderName : foldersNames )
        {
            log.info("folderName = " + folderName);
            Track track = pathToFolderWithFolders.getChildPath(folderName, trackName).getDataElement(Track.class);
            Map<String, List<FunSite>> map = FunSiteUtils.readSitesWithPropertiesInTrack(track, 0, 100000000, folderName);
            FunSite[] funSites = FunSiteUtils.transformToArray(map);
            FunSiteUtils.writeSitesToBedFile(funSites, pathToOutputFolder, folderName);
        }
    }
    
    public static String[] getAvailableDataTypes()
    {
        return new String[]{SINGLE_FOLDER, SEVERAL_FOLDERS};
    }
    
    /************************************************************************/
    /************************** Utils for AnalysisMethodSupport *************/
    /************************************************************************/
    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_DATA_TYPE = "Data Type";
        public static final String PD_DATA_TYPE = "Select data Type";
        
        public static final String PN_COMBINED_PEAK_TYPE = "Type of combined peaks";
        public static final String PD_COMBINED_PEAK_TYPE = "Type of combined peaks";
        
        public static final String PN_MINIMAL_LENGTH_OF_BINDING_REGION = "Minimal length of binding region";
        public static final String PD_MINIMAL_LENGTH_OF_BINDING_REGION = "Minimal length of binding region";
        
        public static final String PN_MAXIMAL_LENGTH_OF_BINDING_REGION = "Maximal length of binding region";
        public static final String PD_MAXIMAL_LENGTH_OF_BINDING_REGION = "Maximal length of binding region";
        
        public static final String PN_FPCM_THRESHOLD = "FPCM threshold";
        public static final String PD_FPCM_THRESHOLD = "FPCM threshold";

        public static final String PN_DO_CALCULATE_AUCS = "Do calculate AUCs";
        public static final String PD_DO_CALCULATE_AUCS = "Do calculate AUCs and ROC-curves";
        
        public static final String PN_PATH_TO_DATA_SET = "Path to data set";
        public static final String PD_PATH_TO_DATA_SET = "Path to data set";
        
        public static final String PN_PATH_TO_FOLDER_WITH_SITE_MODELS = "Path to folder with site models";
        public static final String PD_PATH_TO_FOLDER_WITH_SITE_MODELS = "Path to folder with site models";
        
        public static final String PN_SITE_MODEL_NAME = "Site model name";
        public static final String PD_SITE_MODEL_NAME = "Please, select site model name";
        
        public static final String PN_DB_SELECTOR = "Sequences collection";
        public static final String PD_DB_SELECTOR = "Select a source of nucleotide sequences";
        
        public static final String PN_PATH_TO_FOLDER = "Path to folder with files or tracks";
        public static final String PD_PATH_TO_FOLDER = "Path to folder with bed-files or tracks";
        
        public static final String PN_PARAMETERS_FOR_SEVERAL_FOLDERS = "Parameters for case of several folders";
        public static final String PD_PARAMETERS_FOR_SEVERAL_FOLDERS = "Please, determine parameters for case of several folders";

        public static final String PN_PARAMETERS_FOR_SINGLE_FOLDER = "Parameters for case of single folder";
        public static final String PD_PARAMETERS_FOR_SINGLE_FOLDER = "Please, determine parameters for case of single folder";

        public static final String PN_FILE_NAMES = "File names";
        public static final String PD_FILE_NAMES = "Select file names";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }
    
    public static class AllParameters extends AbstractAnalysisParameters
    {
        private String dataType = SINGLE_FOLDER;
        private String combinedPeakType = CombinedSites.SITE_TYPE_MERGED;
        private int minimalLengthOfBindingRegion = 20;
        private int maximalLengthOfBindingRegion = 300;
        private double fpcmThreshold = 2.0;
        private boolean doAucs = false;
        private DataElementPath pathToFolderWithSiteModels;
        private String siteModelName;
        private BasicGenomeSelector dbSelector;
        private PathToDataSet[] pathsToDataSets = new PathToDataSet[]{new PathToDataSet()};
        private DataElementPath pathToFolderWithFiles;
        private String[] fileNames;
        private DataElementPath pathToOutputFolder;
        
        public AllParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }

        @PropertyName(MessageBundle.PN_DATA_TYPE)
        @PropertyDescription(MessageBundle.PD_DATA_TYPE)
        public String getDataType()
        {
            return dataType;
        }
        public void setDataType(String dataType)
        {
            Object oldValue = this.dataType;
            this.dataType = dataType;
            firePropertyChange("*", oldValue, dataType);
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
        
        @PropertyName(MessageBundle.PN_MINIMAL_LENGTH_OF_BINDING_REGION)
        @PropertyDescription(MessageBundle.PD_MINIMAL_LENGTH_OF_BINDING_REGION)
        public int getMinimalLengthOfBindingRegion()
        {
            return minimalLengthOfBindingRegion;
        }
        public void setMinimalLengthOfBindingRegion(int minimalLengthOfBindingRegion)
        {
            Object oldValue = this.minimalLengthOfBindingRegion;
            this.minimalLengthOfBindingRegion = minimalLengthOfBindingRegion;
            firePropertyChange("minimalLengthOfBindingRegion", oldValue, minimalLengthOfBindingRegion);
        }
        
        @PropertyName(MessageBundle.PN_MAXIMAL_LENGTH_OF_BINDING_REGION)
        @PropertyDescription(MessageBundle.PD_MAXIMAL_LENGTH_OF_BINDING_REGION)
        public int getMaximalLengthOfBindingRegion()
        {
            return maximalLengthOfBindingRegion;
        }
        public void setMaximalLengthOfBindingRegion(int maximalLengthOfBindingRegion)
        {
            Object oldValue = this.maximalLengthOfBindingRegion;
            this.maximalLengthOfBindingRegion = maximalLengthOfBindingRegion;
            firePropertyChange("maximalLengthOfBindingRegion", oldValue, maximalLengthOfBindingRegion);
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
        
        @PropertyName(MessageBundle.PN_DO_CALCULATE_AUCS)
        @PropertyDescription(MessageBundle.PN_DO_CALCULATE_AUCS)
        public boolean getDoAucs()
        {
            return doAucs;
        }
        public void setDoAucs(boolean doAucs)
        {
            Object oldValue = this.doAucs;
            this.doAucs = doAucs;
            firePropertyChange("*", oldValue, doAucs);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_SITE_MODELS)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_SITE_MODELS)
        public DataElementPath getPathToFolderWithSiteModels()
        {
            return pathToFolderWithSiteModels;
        }
        public void setPathToFolderWithSiteModels(DataElementPath pathToFolderWithSiteModels)
        {
            Object oldValue = this.pathToFolderWithSiteModels;
            this.pathToFolderWithSiteModels = pathToFolderWithSiteModels;
            firePropertyChange("pathToFolderWithSiteModels", oldValue, pathToFolderWithSiteModels);
        }
        
        @PropertyName(MessageBundle.PN_SITE_MODEL_NAME)
        @PropertyDescription(MessageBundle.PD_SITE_MODEL_NAME)
        public String getSiteModelName()
        {
            return siteModelName;
        }
        public void setSiteModelName(String siteModelName)
        {
            Object oldValue = this.siteModelName;
            this.siteModelName = siteModelName;
            firePropertyChange("siteModelName", oldValue, siteModelName);
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
        
        @PropertyName("Paths to data sets")
        public PathToDataSet[] getPathsToDataSets()
        {
            return pathsToDataSets;
        }
        public void setPathsToDataSets(PathToDataSet[] pathsToDataSets)
        {
            Object oldValue = this.pathsToDataSets;
            this.pathsToDataSets = pathsToDataSets;
            firePropertyChange("pathsToDataSets", oldValue, pathsToDataSets);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER)
        public DataElementPath getPathToFolderWithFiles()
        {
            return pathToFolderWithFiles;
        }
        public void setPathToFolderWithFiles(DataElementPath pathToFolderWithFiles)
        {
            Object oldValue = this.pathToFolderWithFiles;
            this.pathToFolderWithFiles = pathToFolderWithFiles;
            firePropertyChange("pathToFolderWithFiles", oldValue, pathToFolderWithFiles);
        }
        
        @PropertyName(MessageBundle.PN_FILE_NAMES)
        @PropertyDescription(MessageBundle.PD_FILE_NAMES)
        public String[] getFileNames()
        {
            return fileNames;
        }
        public void setFileNames(String[] fileNames)
        {
            Object oldValue = this.fileNames;
            this.fileNames = fileNames;
            firePropertyChange("fileNames", oldValue, fileNames);
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

    public static class ParametersForSingleFolder extends AllParameters implements JSONBean
    {}
    
    public static class ParametersForSingleFolderBeanInfo extends BeanInfoEx2<ParametersForSingleFolder>
    {
        public ParametersForSingleFolderBeanInfo()
        {
            super(ParametersForSingleFolder.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToFolderWithFiles", beanClass, DataCollection.class, true));
            add("fileNames", FileNamesSelector.class);
        }
    }
    
    
    public static class ParametersForSeveralFolders extends AllParameters implements JSONBean
    {}
  
  public static class ParametersForSeveralFoldersBeanInfo extends BeanInfoEx2<ParametersForSeveralFolders>
  {
      public ParametersForSeveralFoldersBeanInfo()
      {
          super(ParametersForSeveralFolders.class);
      }
      
      @Override
      protected void initProperties() throws Exception
      {
          add("pathsToDataSets");
      }
  }

    public static class QualityControlAnalysisParameters extends AllParameters
    {
        ParametersForSeveralFolders parametersForSeveralFolders;
        ParametersForSingleFolder parametersForSingleFolder;
        
        public QualityControlAnalysisParameters()
        {
            setParametersForSeveralFolders(new ParametersForSeveralFolders());
            setParametersForSingleFolder(new ParametersForSingleFolder());
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_SEVERAL_FOLDERS)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_SEVERAL_FOLDERS)
        public ParametersForSeveralFolders getParametersForSeveralFolders()
        {
            return parametersForSeveralFolders;
        }
        public void setParametersForSeveralFolders(ParametersForSeveralFolders parametersForSeveralFolders)
        {
            Object oldValue = this.parametersForSeveralFolders;
            this.parametersForSeveralFolders = parametersForSeveralFolders;
            firePropertyChange("parametersForSeveralFolders", oldValue, parametersForSeveralFolders);
        }

        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_SINGLE_FOLDER)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_SINGLE_FOLDER)
        public ParametersForSingleFolder getParametersForSingleFolder()
        {
            return parametersForSingleFolder;
        }
        public void setParametersForSingleFolder(ParametersForSingleFolder parametersForSingleFolder)
        {
            Object oldValue = this.parametersForSingleFolder;
            this.parametersForSingleFolder = parametersForSingleFolder;
            firePropertyChange("parametersForSingleFolder", oldValue, parametersForSingleFolder);
        }
        
        public boolean isPathToFolderWithSiteModelsHidden()
        {
            return( ! getDoAucs() );
        }
        
        public boolean isSiteModelNameHidden()
        {
            return( ! getDoAucs() );
        }
        
        public boolean isDbSelectorHidden()
        {
            return( ! getDoAucs() );
        }
        
        public boolean areParametersForSingleFolderHidden()
        {
            return( ! getDataType().equals(SINGLE_FOLDER) );
        }
        
        public boolean areParametersForSeveralFoldersHidden()
        {
            return( ! getDataType().equals(SEVERAL_FOLDERS) );
        }
    }
    
    public static class PathToDataSet extends OptionEx implements JSONBean
    {
        private DataElementPath pathToDataSet;
        
        @PropertyName(MessageBundle.PN_PATH_TO_DATA_SET)
        @PropertyDescription(MessageBundle.PD_PATH_TO_DATA_SET)
        public DataElementPath getPathToDataSet()
        {
            return pathToDataSet;
        }
        public void setPathToDataSet(DataElementPath pathToDataSet)
        {
            Object oldValue = this.pathToDataSet;
            this.pathToDataSet = pathToDataSet;
            firePropertyChange("pathToDataSet", oldValue, pathToDataSet);
        }
        
        
    }
    
    public static class PathToDataSetBeanInfo extends BeanInfoEx2<PathToDataSet>
    {
        public PathToDataSetBeanInfo()
        {
            super(PathToDataSet.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToDataSet", beanClass, DataElement.class, true));
        }
    }

    public static class DataTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return getAvailableDataTypes();
        }
    }
    
    public static class FileNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> dc = ((ParametersForSingleFolder)getBean()).getPathToFolderWithFiles().getDataCollection(DataElement.class);
                String[] fileNames = dc.getNameList().toArray(new String[0]);
                Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
                return fileNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select folder with files)"};
            }
            catch( Exception e )
            {
                return new String[]{"(folder doesn't contain the files)"};
            }
        }
    }
    
    public static class SiteModelNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            DataCollection<DataElement> dc = ((QualityControlAnalysisParameters)getBean()).getPathToFolderWithSiteModels().getDataCollection(DataElement.class);
            String[] siteModelNames = dc.getNameList().toArray(new String[0]);
            Arrays.sort(siteModelNames, String.CASE_INSENSITIVE_ORDER);
            return siteModelNames;
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
    
    public static class QualityControlAnalysisParametersBeanInfo extends BeanInfoEx2<QualityControlAnalysisParameters>
    {
        public QualityControlAnalysisParametersBeanInfo()
        {
            super(QualityControlAnalysisParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("dataType", beanClass), DataTypeSelector.class);
            add(new PropertyDescriptorEx("combinedPeakType", beanClass), CombinedPeakTypeSelector.class);
            add("minimalLengthOfBindingRegion");
            add("maximalLengthOfBindingRegion");
            add("fpcmThreshold");
            add("doAucs");
            addHidden(DataElementPathEditor.registerInputChild("pathToFolderWithSiteModels", beanClass, SiteModel.class, true), "isPathToFolderWithSiteModelsHidden");
            addHidden("siteModelName", SiteModelNameSelector.class, "isSiteModelNameHidden");
            addHidden("dbSelector", "isDbSelectorHidden");
            addHidden("parametersForSeveralFolders", "areParametersForSeveralFoldersHidden");
            addHidden("parametersForSingleFolder", "areParametersForSingleFolderHidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}
