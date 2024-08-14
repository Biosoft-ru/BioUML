/* $Id$ */

package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.gtrd.utils.CistromUtils.CistromConstructor;
import biouml.plugins.gtrd.utils.EnsemblUtils;
import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.FunSiteUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils.SiteModelComposed;
import biouml.plugins.gtrd.utils.SiteUtils;
import biouml.plugins.machinelearning.classification_models.ClassificationModel;
import biouml.plugins.machinelearning.distribution_mixture.NormalMixture;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrix.DataMatrixConstructor;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixStringConstructor;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.NormalDistribution;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class CistromConstructionAdvanced extends AnalysisMethodSupport<CistromConstructionAdvanced.CistromConstructionAdvancedParameters>
{
    public static final String OPTION_01 = CistromConstructor.OPTION_04;
    public static final String OPTION_02 = CistromConstructor.OPTION_05;
    public static final String OPTION_03 = CistromConstructor.OPTION_06;
    
    public static final String BEST_SCORE = "Best_score";
    
    public CistromConstructionAdvanced(DataCollection<?> origin, String name)
    {
         super(origin, name, new CistromConstructionAdvancedParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut()
    {
        log.info(" **************************************************************************************************************");
        log.info(" *    Cistrom construction advanced. There are 3 modes :                                                      *");
        log.info(" * 1. Selection of the most reliable meta-clusters (based on 2-component normal mixture of RA-scores)         *");
        log.info(" * 2. Extended meta-clusters construction (Rank-aggregation for rank aggregation scores and site motif scores *");
        log.info(" * 3. Assignment of probabilities to meta-clusters by classification model                                    *");        
        log.info(" **************************************************************************************************************");

        String option = parameters.getOption();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        
        switch( option )
        {
            case OPTION_01 : log.info("Selected option : " + OPTION_01);
                             DataElementPath pathToFolderWithTracks = parameters.getParametersForOption01().getPathToFolderWithTracks();
                             String[] trackNames = parameters.getParametersForOption01().getTrackNames();
                             double proportionForRemoving = parameters.getParametersForOption01().getProportionForRemoving();
                             implementOption01(pathToFolderWithTracks, trackNames, proportionForRemoving, pathToOutputFolder);
                             break;
            case OPTION_02 : log.info("Selected option : " + OPTION_02);
                             pathToFolderWithTracks = parameters.getParametersForOption02().getPathToFolderWithTracks();
                             trackNames = parameters.getParametersForOption02().getTrackNames();
                             DataElementPath pathToSequences = parameters.getParametersForOption02().getDbSelector().getSequenceCollectionPath();
                             DataElementPath pathToFolderWithSiteModels = parameters.getParametersForOption02().getPathToFolderWithSiteModels();
                             implementOption02(pathToFolderWithTracks, trackNames, pathToFolderWithSiteModels, pathToSequences, pathToOutputFolder);
                             break;
            case OPTION_03 : log.info("Selected option : " + OPTION_03);
                             pathToFolderWithTracks = parameters.getParametersForOption03().getPathToFolderWithTracks();
                             trackNames = parameters.getParametersForOption03().getTrackNames();
                             double proportionForTopSites = parameters.getParametersForOption03().getProportionForTopSites();
                             implementOption03(pathToFolderWithTracks, trackNames, proportionForTopSites, pathToOutputFolder);
                             break;
        }
        return pathToOutputFolder.getDataCollection();
    }
    
    /**********************************************************************/
    /****************************** OPTION_01 *****************************/
    /**********************************************************************/

    private void implementOption01(DataElementPath pathToFolderWithTracks, String[] trackNames, double proportionForRemoving, DataElementPath pathToOutputFolder)
    {
        String summaryTableName = "Summary";
        String[] columnNames = new String[]{"p1", "mean1", "sigma1", "p2", "mean2", "sigma2", "RA_threshold", "Number_of_initial_sites", "Percentage_of_removed_sites"};
        
        // 1. Remove names of treated tracks.
        DataElementPath pathToTableWithSummary = pathToOutputFolder.getChildPath(summaryTableName);
        String[] trackNamesUntreated = trackNames;
        if( pathToTableWithSummary.exists() )
        {
            String[] trackNamesInTable = TableAndFileUtils.getRowNamesInTable(pathToTableWithSummary);
            List<String> list = new ArrayList<>();
            for( int i = 0; i < trackNames.length; i++ )
                if( ! ArrayUtils.contains(trackNamesInTable, trackNames[i]) )
                    list.add(trackNames[i]);
            trackNamesUntreated = list.toArray(new String[0]);
        }
        log.info("number of untreated tracks = " + trackNamesUntreated.length);

        // 2. Treatment of tracks (with meta-clusters).
        double quantile = NormalDistribution.getStandartNormalQuantile(proportionForRemoving, 0.0001, 100);
        log.info("quantile of standard normal distribution = " + quantile);

        for( int i = 0; i < trackNamesUntreated.length; i++ )
        {
            // 2.1. Identification of 2-component normal mixture.
            log.info("i = " + i + " trackNamesUntreated[i] = " + trackNamesUntreated[i]);
            DataElementPath pathToTrack = pathToFolderWithTracks.getChildPath(trackNamesUntreated[i]);
            Track track = pathToTrack.getDataElement(Track.class);
            double[] raScores = getRaScores(track);
            log.info("number of sites = " + raScores.length);
            NormalMixture normalMixture = new NormalMixture(raScores, 2, null,null, 300);
            DataMatrix dm = normalMixture.getParametersOfComponents();
            if( dm.getSize() < 2 )
            {
                TableAndFileUtils.addRowToTable(UtilsForArray.getConstantArray(columnNames.length, Double.NaN), null, trackNamesUntreated[i], columnNames, pathToOutputFolder, summaryTableName);
                continue;
            }
            // 2.2. Calculate RA-thresholds and percentage of removed sites (meta-clusters).
            double[] means = dm.getColumn("Mean value");
            String[] rowNames = dm.getRowNames();
            double[] row = means[0] < means[1] ? ArrayUtils.addAll(dm.getRow(rowNames[0]), dm.getRow(rowNames[1])) : ArrayUtils.addAll(dm.getRow(rowNames[1]), dm.getRow(rowNames[0]));
            double threshold = row[4] - quantile * row[5]; 
            int count = 0;
            for( double x : raScores )
                if( x >= threshold )
                    count++;
            double percentageOfRemovedSites = (double)count / (double)raScores.length;
            log.info("threshold = " + threshold + " percentageOfRemovedSites = " + percentageOfRemovedSites);
            row = ArrayUtils.addAll(row, new double[]{threshold, (double)raScores.length, percentageOfRemovedSites});
            TableAndFileUtils.addRowToTable(row, null, trackNamesUntreated[i], columnNames, pathToOutputFolder, summaryTableName);
            
            // 2.3. Save the most reliable sites (meta-clusters).
            saveMostReliableSites(pathToTrack, threshold, pathToOutputFolder, trackNamesUntreated[i]);
        }
    }
    
    private static void saveMostReliableSites(DataElementPath pathToTrack, double raThreshold, DataElementPath pathToOutputFolder, String outputTrackName)
    {
        Track track = pathToTrack.getDataElement(Track.class);
        SqlTrack outputTrack = SqlTrack.createTrack(pathToOutputFolder.getChildPath(outputTrackName), null);
        String[] propertiesNames = new String[]{RankAggregation.RA_SCORE};
        for( Site site : track.getAllSites() )
            if( SiteUtils.getProperties(site, propertiesNames)[0] < raThreshold )
                outputTrack.addSite(site);
        outputTrack.finalizeAddition();
        CollectionFactoryUtils.save(track);
    }
    
    // TODO: To move to 'SiteUtils' class.
    private double[] getRaScores(Track track)
    {
        String[] propertiesNames = new String[]{RankAggregation.RA_SCORE};
        DataCollection<Site> dc = track.getAllSites();
        int n = dc.getSize(), index = 0;
        double[] result = new double[n];
        for( Site site : dc )
            result[index++] = SiteUtils.getProperties(site, propertiesNames)[0];
        return result;
    }
    
    /**********************************************************************/
    /****************************** OPTION_02 ******************************/
    /**********************************************************************/

    // Old version
    // trackNamesInput = uniprot_IDs
    private void implementOption02(DataElementPath pathToFolderWithTracks, String[] trackNamesInput, DataElementPath pathToFolderWithSiteModels, DataElementPath pathToSequences, DataElementPath pathToOutputFolder)
    {
        // 1.
        DataMatrixString dms = SiteModelUtils.getSiteModelNamesAndUniprotIds(pathToFolderWithSiteModels);
        String[] siteModelNames = dms.getRowNames(), uniprotIdsForSiteModels = dms.getColumn(0);
        
        // 2. DataMatrixStringConstructor 
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(new String[]{"Site_model_name"});
        for( String name : trackNamesInput )
        {
            int index = ArrayUtils.indexOf(uniprotIdsForSiteModels, name);
            if( index >= 0 )
                dmsc.addRow(name, new String[]{siteModelNames[index]});
        }
        dms = dmsc.getDataMatrixString();
        String[] trackNames = dms.getRowNames();
        siteModelNames = dms.getColumn(0);
        for( int i = 0; i < trackNames.length; i++ )
            log.info("i = " + i + " trackNames = " + trackNames[i] + " siteModelNames = " + siteModelNames[i]);
        
        // 3.
        
        // TODO: temporary!
//        trackNames = new String[]{"O43524"};
//        siteModelNames = new String[]{"FOXO3_HUMAN.H11MO.0.B"};
        
        for( int i = 0; i < trackNames.length; i++ )
        {
            SiteModel siteModel = pathToFolderWithSiteModels.getChildPath(siteModelNames[i]).getDataElement(SiteModel.class);
            SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
            addSiteMotifScoresToTrack(pathToFolderWithTracks, trackNames[i], smc, pathToSequences, pathToOutputFolder);
        }
    }
    
    // New version: under construction
    // trackNamesInput = TF-Classes
//    private void implementOption02(DataElementPath pathToFolderWithTracks, String[] trackNamesInput, DataElementPath pathToFolderWithSiteModels, DataElementPath pathToSequences, DataElementPath pathToOutputFolder)
//    {
//        // 1.
//        DataMatrixString dms = SiteModelUtils.getSiteModelNamesAndUniprotIds(pathToFolderWithSiteModels);
//        String[] siteModelNames = dms.getRowNames(), uniprotIdsForSiteModels = dms.getColumn(0);
//        
//        // 2. DataMatrixStringConstructor 
//        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(new String[]{"Site_model_name"});
//        for( String name : trackNamesInput )
//        {
//            int index = ArrayUtils.indexOf(uniprotIdsForSiteModels, name);
//            if( index >= 0 )
//                dmsc.addRow(name, new String[]{siteModelNames[index]});
//        }
//        dms = dmsc.getDataMatrixString();
//        String[] trackNames = dms.getRowNames();
//        siteModelNames = dms.getColumn(0);
//        for( int i = 0; i < trackNames.length; i++ )
//            log.info("i = " + i + " trackNames = " + trackNames[i] + " siteModelNames = " + siteModelNames[i]);
//        
//        // 3.
//        
//        // TODO: temporary!
////        trackNames = new String[]{"O43524"};
////        siteModelNames = new String[]{"FOXO3_HUMAN.H11MO.0.B"};
//        
//        for( int i = 0; i < trackNames.length; i++ )
//        {
//            SiteModel siteModel = pathToFolderWithSiteModels.getChildPath(siteModelNames[i]).getDataElement(SiteModel.class);
//            SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
//            addSiteMotifScoresToTrack(pathToFolderWithTracks, trackNames[i], smc, pathToSequences, pathToOutputFolder);
//        }
//    }
    
    private void addSiteMotifScoresToTrack(DataElementPath pathToFolderWithTracks, String trackNameInput, SiteModelComposed siteModelComposed, DataElementPath pathToSequences, DataElementPath pathToOutputFolder)
    {
        int lengthOfSequenceRegion = 50;
        String[] chromosomeNamesAvailable = EnsemblUtils.getStandardSequencesNames(pathToSequences);
        DataElementPath pathToTrack = pathToFolderWithTracks.getChildPath(trackNameInput);
        Track track = pathToTrack.getDataElement(Track.class);
        SqlTrack outputTrack = SqlTrack.createTrack(pathToOutputFolder.getChildPath(trackNameInput), null);
        for( Site site : track.getAllSites() )
        {
            if( ! ArrayUtils.contains(chromosomeNamesAvailable, site.getSequence().getName()) ) continue;
            String chromosomeName = site.getSequence().getName();
            Interval coordinates = site.getInterval();
            Sequence fullChromosome = pathToSequences.getChildPath(chromosomeName).getDataElement(AnnotatedSequence.class).getSequence();
            Sequence sequence = getLinearSequenceWithGivenLength(fullChromosome, lengthOfSequenceRegion, coordinates);
            Site bestSite = siteModelComposed.findBestSite(sequence);
            if( bestSite == null ) continue;
            double bestScore = bestSite.getScore();
            
            // temp
            log.info("bestScore = " + bestScore);

            DynamicPropertySet dps = site.getProperties();
            dps.add(new DynamicProperty(BEST_SCORE, String.class, Double.toString(bestScore)));
            outputTrack.addSite(site);
        }
        outputTrack.finalizeAddition();
        CollectionFactoryUtils.save(track);
    }
    
    // TODO: This is modification of funSite.getSequenceRegionWithGivenLength();
    // To refact both this methods to re-use the single method!!!!
    private static Sequence getLinearSequenceWithGivenLength(Sequence fullChromosome, int lengthOfSequenceRegion, Interval coordinates)
    {
        int center = coordinates.getCenter(), leftPosition = center - lengthOfSequenceRegion / 2;
        if(leftPosition < 1 )
            leftPosition = 1;
        int rightPosition = leftPosition + lengthOfSequenceRegion - 1;
        if( rightPosition >= fullChromosome.getLength() )
        {
            rightPosition = fullChromosome.getLength() - 1;
            leftPosition = rightPosition - lengthOfSequenceRegion + 1;
        }
        return new LinearSequence(new SequenceRegion(fullChromosome, new Interval(leftPosition, rightPosition), false, false));
    }
    
    /**********************************************************************/
    /****************************** OPTION_03 *****************************/
    /**********************************************************************/
    private void implementOption03(DataElementPath pathToFolderWithTracks, String[] trackNames, double proportionForTopSites, DataElementPath pathToOutputFolder)
    {
        for( int i = 0; i < trackNames.length; i++ )
        {
            // 1. Create classification model.
            DataElementPath pathToTrack = pathToFolderWithTracks.getChildPath(trackNames[i]);
            FunSite[] funSites = getFunSites(pathToTrack, new String[]{RankAggregation.RA_SCORE, BEST_SCORE});
            int numberOfBestSites = (int)(proportionForTopSites * (double)funSites.length);
            Object[] objects = getDataMatrixAndResponse(funSites, trackNames[i], numberOfBestSites);
            DataMatrixString dms = (DataMatrixString)objects[1];
            DataMatrix dm = (DataMatrix)objects[0];
            dm.writeDataMatrix(false, dms, pathToOutputFolder, "new_data_matrix", log);
            int interceptIndex = dm.getColumnNames().length;
            dm.addColumn(ModelUtils.INTERCEPT, UtilsForArray.getConstantArray(dm.getSize(), 1.0), interceptIndex);
            Object[] additionalInputParameters = new Object[]{300, 0.01, MatrixUtils.DEFAULT_MAX_NUMBER_OF_ROTATIONS, MatrixUtils.DEFAULT_EPS_FOR_ROTATIONS};
            ClassificationModel classificationModel = ClassificationModel.createModel(ClassificationModel.CLASSIFICATION_5_LRM, dms.getColumnNames()[0], dms.getColumn(0), dm, additionalInputParameters, true);
            classificationModel.saveModel(pathToOutputFolder);
        }
    }
    
    private Object[] getDataMatrixAndResponse(FunSite[] funSites, String trackName, int numberOfBestSites)
    {
        // 1.
        DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
        
        log.info("funSites.length = " + funSites.length);
        log.info("dm.length = " + dm.getSize());
        String[] columnNames = dm.getColumnNames();
        for( String s : columnNames )
            log.info("columnNames = " + s);
        double[] raScores = dm.getColumn(RankAggregation.RA_SCORE + "_" + trackName), scoresClone = raScores.clone();
        double [] bestScores = dm.getColumn(BEST_SCORE + "_" + trackName);
        UtilsForArray.sortInAscendingOrder(scoresClone);
        double thresholdMin = scoresClone[numberOfBestSites - 1], thresholdMax = scoresClone[funSites.length - numberOfBestSites];
        
        // 2.
        log.info("thresholdMin = " + thresholdMin + " thresholdMax = " + thresholdMax);
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{RankAggregation.RA_SCORE,
                RankAggregation.RA_SCORE + "_squared",
                BEST_SCORE, BEST_SCORE + "_squared", "interaction_of_" + RankAggregation.RA_SCORE + "_and_" + BEST_SCORE});
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(new String[]{"type of meta-cluster"});
        for( int index = 0, i = 0; i < funSites.length; i++ )
        {
            if( raScores[i] > thresholdMin && raScores[i] < thresholdMax ) continue;
            String rowName = "site_" + Integer.toString(index++);
            dmc.addRow(rowName , new double[]{raScores[i], raScores[i] * raScores[i], bestScores[i], bestScores[i] * bestScores[i], raScores[i] * bestScores[i]});
            if( raScores[i] <= thresholdMin )
                dmsc.addRow(rowName, new String[]{"reliable meta-cluster"});
            if( raScores[i] >= thresholdMax )
                dmsc.addRow(rowName, new String[]{"not reliable meta-cluster"});
        }
        return new Object[]{dmc.getDataMatrix(), dmsc.getDataMatrixString()};
    }

    private static FunSite[] getFunSites(DataElementPath pathToTrack, String[] propertiesNames)
    {
        Track track = pathToTrack.getDataElement(Track.class);
        Map<String, List<FunSite>> sites = FunSiteUtils.readSitesInTrack(track, 1, Integer.MAX_VALUE, propertiesNames, track.getName());
        return FunSiteUtils.transformToArray(sites);
    }


    /************************************************************************/
    /************************** Utils for AnalysisMethodSupport *************/
    /************************************************************************/
    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Please, select option (i.e. select the concrete session of given analysis).";
        
        public static final String PN_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with tracks";
        public static final String PD_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with tracks";
        
        public static final String PN_PATH_TO_FOLDER_WITH_SITE_MODELS = "Path to folder with site models";
        public static final String PD_PATH_TO_FOLDER_WITH_SITE_MODELS = "Path to folder with site models";
        
        public static final String PN_TRACK_NAMES = "Track names";
        public static final String PD_TRACK_NAMES = "Please, select track names";
        
        public static final String PN_DB_SELECTOR = "Sequences collection";
        public static final String PD_DB_SELECTOR = "Select a source of nucleotide sequences";
        
        public static final String PN_PROPORTION_FOR_REMOVING = "Proportion for removing";
        public static final String PD_PROPORTION_FOR_REMOVING = "Proportion for sites that will be classified as False Positives for further removing";
        
        public static final String PN_PROPORTION_FOR_TOP_SITES = "Proportion for top sites";
        public static final String PD_PROPORTION_FOR_TOP_SITES = "Proportion for top sites (meta-clusters for training classification model";
        
        public static final String PN_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        public static final String PD_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        
        public static final String PN_PARAMETERS_FOR_OPTION_02 = "Parameters for OPTION_02";
        public static final String PD_PARAMETERS_FOR_OPTION_02 = "Parameters for OPTION_02";
        
        public static final String PN_PARAMETERS_FOR_OPTION_03 = "Parameters for OPTION_03";
        public static final String PD_PARAMETERS_FOR_OPTION_03 = "Parameters for OPTION_03";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }
    
    public static class AllParameters extends AbstractAnalysisParameters
    {
        private String option = OPTION_01;
        private DataElementPath pathToFolderWithTracks;
        private DataElementPath pathToFolderWithSiteModels;
        private String[] trackNames;
        private BasicGenomeSelector dbSelector;
        private DataElementPath pathToOutputFolder;
        
        public AllParameters()
        {
            setDbSelector(new BasicGenomeSelector());
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
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_TRACKS)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_TRACKS)
        public DataElementPath getPathToFolderWithTracks()
        {
            return pathToFolderWithTracks;
        }
        public void setPathToFolderWithTracks(DataElementPath pathToFolderWithTracks)
        {
            Object oldValue = this.pathToFolderWithTracks;
            this.pathToFolderWithTracks = pathToFolderWithTracks;
            firePropertyChange("pathToFolderWithTracks", oldValue, pathToFolderWithTracks);
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
        
        @PropertyName(MessageBundle.PN_TRACK_NAMES)
        @PropertyDescription(MessageBundle.PD_TRACK_NAMES)
        public String[] getTrackNames()
        {
            return trackNames;
        }
        public void setTrackNames(String[] trackNames)
        {
            Object oldValue = this.trackNames;
            this.trackNames = trackNames;
            firePropertyChange("trackNames", oldValue, trackNames);
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
    
    public static class ParametersForOption01 extends AllParameters
    {
        private double proportionForRemoving = 0.95;
        
        @PropertyName(MessageBundle.PN_PROPORTION_FOR_REMOVING)
        @PropertyDescription(MessageBundle.PD_PROPORTION_FOR_REMOVING)
        public double getProportionForRemoving()
        {
            return proportionForRemoving;
        }
        public void setProportionForRemoving(double proportionForRemoving)
        {
            Object oldValue = this.proportionForRemoving;
            this.proportionForRemoving = proportionForRemoving;
            firePropertyChange("proportionForRemoving", oldValue, proportionForRemoving);
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
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTracks", beanClass, Track.class, true));
            add("trackNames", TrackNamesSelectorForOption01.class);
            add("proportionForRemoving");
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
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTracks", beanClass, Track.class, true));
            add("trackNames", TrackNamesSelectorForOption02.class);
            add(DataElementPathEditor.registerInputChild("pathToFolderWithSiteModels", beanClass, SiteModel.class, true));
            add("dbSelector");
        }
    }
    
    public static class ParametersForOption03 extends AllParameters
    {
        private double proportionForTopSites = 0.10;
        
        @PropertyName(MessageBundle.PN_PROPORTION_FOR_TOP_SITES)
        @PropertyDescription(MessageBundle.PD_PROPORTION_FOR_TOP_SITES)
        public double getProportionForTopSites()
        {
            return proportionForTopSites;
        }
        public void setProportionForTopSites(double proportionForTopSites)
        {
            Object oldValue = this.proportionForTopSites;
            this.proportionForTopSites = proportionForTopSites;
            firePropertyChange("proportionForTopSites", oldValue, proportionForTopSites);
        }
    }
    
    public static class ParametersForOption03BeanInfo extends BeanInfoEx2<ParametersForOption03>
    {
        public ParametersForOption03BeanInfo()
        {
            super(ParametersForOption03.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTracks", beanClass, Track.class, true));
            add("trackNames", TrackNamesSelectorForOption03.class);
            add("proportionForTopSites");
        }
    }
    
    public static class CistromConstructionAdvancedParameters extends AllParameters
    {
        ParametersForOption01 parametersForOption01;
        ParametersForOption02 parametersForOption02;
        ParametersForOption03 parametersForOption03;

        public CistromConstructionAdvancedParameters()
        {
            setParametersForOption01(new ParametersForOption01());
            setParametersForOption02(new ParametersForOption02());
            setParametersForOption03(new ParametersForOption03());
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
            this.parametersForOption01.setParent(this);
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
            this.parametersForOption02.setParent(this);
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
            this.parametersForOption03.setParent(this);
            firePropertyChange("parametersForOption03", oldValue, parametersForOption03);
        }
        
        public boolean areParametersForOption01Hidden()
        {
            return ! getOption().equals(OPTION_01);
        }
        
        public boolean areParametersForOption02Hidden()
        {
            return ! getOption().equals(OPTION_02);
        }
        
        public boolean areParametersForOption03Hidden()
        {
            return ! getOption().equals(OPTION_03);
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
        return new String[]{OPTION_01, OPTION_02, OPTION_03};
    }
    
    public static class TrackNamesSelectorForOption01 extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> tracks = ((ParametersForOption01)getBean()).getPathToFolderWithTracks().getDataCollection(DataElement.class);
                String[] trackNames = tracks.getNameList().toArray(new String[0]);
                Arrays.sort(trackNames, String.CASE_INSENSITIVE_ORDER);
                return trackNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select folder with tracks)"};
            }
            catch( Exception e )
            {
                return new String[]{"(folder doesn't contain the tracks)"};
            }
        }
    }
    
    public static class TrackNamesSelectorForOption02 extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> tracks = ((ParametersForOption02)getBean()).getPathToFolderWithTracks().getDataCollection(DataElement.class);
                String[] trackNames = tracks.getNameList().toArray(new String[0]);
                Arrays.sort(trackNames, String.CASE_INSENSITIVE_ORDER);
                return trackNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select folder with tracks)"};
            }
            catch( Exception e )
            {
                return new String[]{"(folder doesn't contain the tracks)"};
            }
        }
    }
    
    public static class TrackNamesSelectorForOption03 extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> tracks = ((ParametersForOption03)getBean()).getPathToFolderWithTracks().getDataCollection(DataElement.class);
                String[] trackNames = tracks.getNameList().toArray(new String[0]);
                Arrays.sort(trackNames, String.CASE_INSENSITIVE_ORDER);
                return trackNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select folder with tracks)"};
            }
            catch( Exception e )
            {
                return new String[]{"(folder doesn't contain the tracks)"};
            }
        }
    }

    public static class CistromConstructionAdvancedParametersBeanInfo extends BeanInfoEx2<CistromConstructionAdvancedParameters>
    {
        public CistromConstructionAdvancedParametersBeanInfo()
        {
            super(CistromConstructionAdvancedParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("option", beanClass), OptionSelector.class);
            addHidden("parametersForOption01", "areParametersForOption01Hidden");
            addHidden("parametersForOption02", "areParametersForOption02Hidden");
            addHidden("parametersForOption03", "areParametersForOption03Hidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}