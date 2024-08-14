/* $Id$ */

package biouml.plugins.bindingregions.analysis;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.gtrd.utils.EnsemblUtils;
import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.FunSiteUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils.SiteModelComposed;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import ru.biosoft.access.core.DataCollection;
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
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author yura
 *
 */
public class SitePredictionInGenome extends AnalysisMethodSupport<SitePredictionInGenome.SitePredictionInGenomeParameters>
{
    private static final String WHOLE_GENOME = "Whole genome";
    private static final String CHROMOSOME_FRAGMENT = "Chromosome fragment";
    private static final String CHIP_SEQ_PEAKS = "ChIP-Seq peaks from given track";
    
    private static final String GIVEN_SITE_MODEL = "Given site model";
    
    public SitePredictionInGenome(DataCollection<?> origin, String name)
    {
        super( origin, name, new SitePredictionInGenomeParameters() );
    }
    
    @Override
    public Track justAnalyzeAndPut()
    {
        log.info("**************************************************************");
        log.info("* Prediction of TF-bindig sites for given TF in              *");
        log.info("*     whole genome                                           *");
        log.info("*     chromosome fragment                                    *");
        log.info("*     genome framents (ChIP-Seq peaks).                      *");
        log.info("* TF-binding sites can be predicted by distinct site models. *");
        log.info("**************************************************************");

        String sequenceSetType = parameters.getSequenceSetType();
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        String chromosomeName = parameters.getChromosomeName();
        int startPosition = parameters.getStartPosition();
        int finishPosition = parameters.getFinishPosition();
        String siteName = parameters.getSiteName();
        PredictionModel[] predictionModels = parameters.getPredictionModels();
        String trackName = parameters.getTrackName();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        DataElementPath pathToTrack = parameters.getPathToTrack();
        
        // TODO: temporary: Transform some BED-files into tracks.
//        if( siteName.equals("ERalpha") )
//        {
//            ExploratoryAnalysisUtil.fromBedFileToTrack(DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C02_DATA_GSE110502/Ctcf"), "site", DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C02_DATA_GSE110502"), "Ctcf_");
//            ExploratoryAnalysisUtil.fromBedFileToTrack(DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C02_DATA_GSE110502/Era"), "site", DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C02_DATA_GSE110502"), "Era_");
//            ExploratoryAnalysisUtil.fromBedFileToTrack(DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C02_DATA_GSE110502/Foxa1"), "site", DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C02_DATA_GSE110502"), "Foxa1_");
//            log.info("*** Transformation of BED-file to tracks is finished  ***");
//        }
        
        // 1. Create sequence set.
        Sequence[] sequences = null;
        switch( sequenceSetType )
        {
            case WHOLE_GENOME        : sequences = EnsemblUtils.getAnnotatedSequences(pathToSequences); break;
            case CHROMOSOME_FRAGMENT : sequences = new Sequence[]{EnsemblUtils.getSequenceRegion(pathToSequences, chromosomeName, startPosition, finishPosition - startPosition + 1)}; break;
            case CHIP_SEQ_PEAKS      : sequences = getSequenceRegions(pathToTrack, pathToSequences);
        }
        
        // 2. Create siteModelComposed
        SiteModel[] siteModels = new SiteModel[predictionModels.length];
        String[] siteModelNames = new String[predictionModels.length];
        for( int i = 0; i < predictionModels.length; i++ )
        {
            siteModelNames[i] = predictionModels[i].getModelName();
            String siteType = predictionModels[i].getSiteType();
            if( siteType.equals(GIVEN_SITE_MODEL) )
            {
                siteModels[i] = predictionModels[i].getModelPath().getDataElement(SiteModel.class);
                continue;
            }
            int window = siteType.equals(SiteModelUtils.IPS_MODEL) || siteType.equals(SiteModelUtils.LOG_IPS_MODEL) ? predictionModels[i].getWindow() : 0;
            double threshold = predictionModels[i].getThreshold();
            FrequencyMatrix frequencyMatrix = predictionModels[i].getMatrixPath().getDataElement(FrequencyMatrix.class);
            siteModels[i] = SiteModelUtils.createSiteModel(siteType, siteModelNames[i], frequencyMatrix, threshold, window);
        }
        
        // 3. Predict sites and write them to track. 
        SiteModelComposed siteModelComposed = new SiteModelComposed(siteModels, siteModelNames, siteName, true);
        Track track = siteModelComposed.findAllSites(sequences, pathToSequences, pathToOutputFolder, trackName, jobControl, 0, 100);
        return track;
    }
    
    private static Sequence[] getSequenceRegions(DataElementPath pathToTrack, DataElementPath pathToSequences)
    {
        Track track = pathToTrack.getDataElement(Track.class);
        FunSite[] funSites = FunSiteUtils.transformToArray(FunSiteUtils.readSitesInTrack(track, 0, Integer.MAX_VALUE, null, null));
        funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
        return FunSiteUtils.getSequenceRegions(funSites, pathToSequences);
    }
    
    public static String[] getAvailableSequenceSetTypes()
    {
        return new String[]{WHOLE_GENOME, CHROMOSOME_FRAGMENT, CHIP_SEQ_PEAKS};
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
    
    public static class SitePredictionInGenomeParameters extends AbstractAnalysisParameters
    {
        private String sequenceSetType = WHOLE_GENOME;
        private BasicGenomeSelector dbSelector;
        private String chromosomeName = "6";
        private int startPosition = 30445000;
        private int finishPosition = 30890000;
        private DataElementPath pathToTrack;
        private String siteName;
        private PredictionModel[] predictionModels;
        private String trackName;
        private DataElementPath pathToOutputFolder;
        
        public SitePredictionInGenomeParameters()
        {
            setDbSelector(new BasicGenomeSelector());
            setPredictionModels( new PredictionModel[]{new PredictionModel()} );
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
        
        @PropertyName(MessageBundle.PN_SEQUENCE_SET_TYPE)
        @PropertyDescription(MessageBundle.PD_SEQUENCE_SET_TYPE)
        public String getSequenceSetType()
        {
            return sequenceSetType;
        }
        public void setSequenceSetType(String sequenceSetType)
        {
            Object oldValue = this.sequenceSetType;
            this.sequenceSetType = sequenceSetType;
            firePropertyChange("*", oldValue, sequenceSetType);
        }
        
        @PropertyName(MessageBundle.PN_CHROMOSOME_NAME)
        @PropertyDescription(MessageBundle.PD_CHROMOSOME_NAME)
        public String getChromosomeName()
        {
            return chromosomeName;
        }
        public void setChromosomeName(String chromosomeName)
        {
            Object oldValue = this.chromosomeName;
            this.chromosomeName = chromosomeName;
            firePropertyChange("chromosomeName", oldValue, chromosomeName);
        }

        @PropertyName(MessageBundle.PN_START_POSITION)
        @PropertyDescription(MessageBundle.PD_START_POSITION)
        public int getStartPosition()
        {
            return startPosition;
        }
        public void setStartPosition(int startPosition)
        {
            Object oldValue = this.startPosition;
            this.startPosition = startPosition;
            firePropertyChange("startPosition", oldValue, startPosition);
        }
        
        @PropertyName(MessageBundle.PN_FINISH_POSITION)
        @PropertyDescription(MessageBundle.PD_FINISH_POSITION)
        public int getFinishPosition()
        {
            return finishPosition;
        }
        public void setFinishPosition(int finishPosition)
        {
            Object oldValue = this.finishPosition;
            this.finishPosition = finishPosition;
            firePropertyChange("finishPosition", oldValue, finishPosition);
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
        
        @PropertyName(MessageBundle.PN_PREDICTION_MODELS)
        @PropertyDescription(MessageBundle.PD_PREDICTION_MODELS)
        public PredictionModel[] getPredictionModels()
        {
            return predictionModels;
        }
        public void setPredictionModels(PredictionModel[] predictionModels)
        {
            Object oldValue = this.predictionModels;
            for( PredictionModel model : predictionModels )
            {
                model.setParent( this );
            }
            this.predictionModels = predictionModels;
            firePropertyChange("predictionModels", oldValue, predictionModels);
        }

        @PropertyName(MessageBundle.PN_TRACK_NAME)
        @PropertyDescription(MessageBundle.PD_TRACK_NAME)
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
        
        public boolean isChromosomeFragmentHidden()
        {
            return ! getSequenceSetType().equals(CHROMOSOME_FRAGMENT);
        }
        
        public boolean isPathToTrackHidden()
        {
            return ! getSequenceSetType().equals(CHIP_SEQ_PEAKS);
        }
    }
    
    public static class PredictionModel extends OptionEx
    {
        private String modelName;
        private String siteType = SiteModelUtils.IPS_MODEL; // GIVEN_SITE_MODEL;
        private DataElementPath modelPath;
        private DataElementPath matrixPath;
        private double threshold;
        private int window = 100;

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
        
        public String getSiteType()
        {
            return siteType;
        }
        public void setSiteType(String siteType)
        {
            Object oldValue = this.siteType;
            this.siteType = siteType;
            firePropertyChange("*", oldValue, siteType);
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

        public DataElementPath getMatrixPath()
        {
            return matrixPath;
        }
        public void setMatrixPath(DataElementPath matrixPath)
        {
            Object oldValue = this.matrixPath;
            this.matrixPath = matrixPath;
            firePropertyChange("matrixPath", oldValue, matrixPath);
        }

        public double getThreshold()
        {
            return threshold;
        }
        public void setThreshold(double threshold)
        {
            Object oldValue = this.threshold;
            this.threshold = threshold;
            firePropertyChange("threshold", oldValue, threshold);
        }
        
        public int getWindow()
        {
            return window;
        }
        public void setWindow(int window)
        {
            Object oldValue = this.window;
            this.window = window;
            firePropertyChange("window", oldValue, window);
        }
        
        public boolean isModelPathHidden()
        {
            String siteType = getSiteType();
            return ! siteType.equals(GIVEN_SITE_MODEL);
        }
        
        public boolean isMatrixHidden()
        {
            String siteType = getSiteType();
            return siteType.equals(GIVEN_SITE_MODEL);
        }
        
        public boolean isWindowHidden()
        {
            String siteType = getSiteType();
            return (! siteType.equals(SiteModelUtils.IPS_MODEL)) && (! siteType.equals(SiteModelUtils.LOG_IPS_MODEL));
        }
    }
    
    public static class PredictionModelBeanInfo extends BeanInfoEx2<PredictionModel>
    {
        public PredictionModelBeanInfo()
        {
            super(PredictionModel.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("modelName");
            //property(new PropertyDescriptorEx("siteType", beanClass)).editor(SiteTypeSelector.class).structureChanging().add();
            add(new PropertyDescriptorEx("siteType", beanClass), SiteTypeSelector.class);
            addHidden(DataElementPathEditor.registerInput("modelPath", beanClass, SiteModel.class), "isModelPathHidden");
            addHidden(DataElementPathEditor.registerInput("matrixPath", beanClass, FrequencyMatrix.class), "isMatrixHidden");
            addHidden("threshold", "isMatrixHidden");
            addHidden("window", "isWindowHidden");
        }
    }
    
    public static class SequenceSetTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return getAvailableSequenceSetTypes();
        }
    }
    
    public static class SiteTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return (String[])ArrayUtils.addAll(new String[]{GIVEN_SITE_MODEL}, SiteModelUtils.getAvailableSiteModelTypes());
        }
    }
    
    public static class ChromosomeNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            try
            {
                DataElementPath pathToSequences = ((SitePredictionInGenomeParameters)getBean()).getDbSelector().getSequenceCollectionPath();
                String[] chromosomeNames = EnsemblUtils.getStandardSequencesNames(pathToSequences);
                Arrays.sort(chromosomeNames, String.CASE_INSENSITIVE_ORDER);
                return chromosomeNames;
            }
            catch( RepositoryException e )
            {
                return new String[] {"(please select genome)"};
            }
        }
    }
    
    public static class SitePredictionInGenomeParametersBeanInfo extends BeanInfoEx2<SitePredictionInGenomeParameters>
    {
        public SitePredictionInGenomeParametersBeanInfo()
        {
            super(SitePredictionInGenomeParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("sequenceSetType", beanClass), SequenceSetTypeSelector.class);
            add("dbSelector");
            addHidden(new PropertyDescriptorEx("chromosomeName", beanClass), ChromosomeNameSelector.class, "isChromosomeFragmentHidden");
            addHidden("startPosition", "isChromosomeFragmentHidden");
            addHidden("finishPosition", "isChromosomeFragmentHidden");
            // property(DataElementPathEditor.registerInput( "trackPath", beanClass, Track.class)).hidden("isChipSeqTrackHidden").add();
            addHidden(DataElementPathEditor.registerInput("pathToTrack", beanClass, Track.class), "isPathToTrackHidden");
            add("siteName");
            add("predictionModels");
            add("trackName");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
            // add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("pathToOutputTrack", beanClass, Track.class), ""));
        }
    }
}
