package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import biouml.model.Module;
import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.SiteModelTypesSelector;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

/**
 * @author yura
 *
 */
public class ROCcurvesInOverlappedChipSeqPeaks extends AnalysisMethodSupport<ROCcurvesInOverlappedChipSeqPeaks.ROCcurvesInOverlappedChipSeqPeaksParameters>
{
    public ROCcurvesInOverlappedChipSeqPeaks(DataCollection<?> origin, String name)
    {
        super(origin, name, new ROCcurvesInOverlappedChipSeqPeaksParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Create ROC-curves on subsets of overlapped ChIP-Seq peaks of given TF-class");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToInputTracks = parameters.getChipSeqTracksPath();
        Species givenSpecie = parameters.getSpecies();
        // only tfClass with >= 2 tracks can be selected !!!
        String tfClassWithName = parameters.getTfClassWithName();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        boolean isAroundSummit = parameters.isAroundSummit();
        String[] siteModelTypes = parameters.getSiteModelTypes();
        final DataElementPath pathToOutputs = parameters.getOutputPath();
        boolean areBothStrands = true;
        
        // 1.
        log.info("Create site models and select appropiate ChIP-Seq peaks tracks");
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        SiteModelsComparison smc = new SiteModelsComparison(siteModelTypes, matrix);
        String[] array = tfClassWithName.split(BindingRegion.SEPARATOR_BETWEEN_TF_CLASS_AND_ITS_NAME);
        String tfClass = array[0];
        List<TrackInfo> trackInfos = TrackInfo.getTracksInfo(pathToInputTracks, givenSpecie, tfClass, null);
        log.info("There are " + trackInfos.size() + " peak sets for TF-class " + tfClass);
        if( trackInfos.size() < 2 ) return null;
        List<Track> tracks = new ArrayList<>();
        for( TrackInfo trackInfo : trackInfos )
            tracks.add(pathToInputTracks.getChildPath(trackInfo.getTrackName()).getDataElement(Track.class));
        Object[] characteristicNamesAndSummitIndicator = getCharacteristicNamesAndSummitIndicator(tracks);
        if( isAroundSummit )
            isAroundSummit = isAroundSummit && (boolean)characteristicNamesAndSummitIndicator[1];
        String[] characteristicNames = (String[])characteristicNamesAndSummitIndicator[0];
        characteristicNames = (String[])ArrayUtils.removeElement(characteristicNames, ChipSeqPeak.SUMMIT);
        characteristicNames = (String[])ArrayUtils.add(characteristicNames, characteristicNames.length, ChipSeqPeak.LENGTH);
        jobControl.setPreparedness(10);
        if( jobControl.isStopped() ) return null;
        
        // 2.
        log.info("Create ROC-curves and calculate AUCs");
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        for( int i = 0; i < tracks.size(); i++ )
        {
            Track track = tracks.get(i);
            List<Track> subList = StreamEx.of(tracks).without( track ).toList();
            Map<String, Map<String, List<ChipSeqPeak>>> allPeaks = getNumberOfOverlapsAndPeaks(track, subList);
            String tablesName = isAroundSummit ? "_" + track.getName() + "_summit" : track.getName();
            SiteModelsComparisonUtils.writeROCcurvesAndAUCsForGroupedPeaks(characteristicNames, allPeaks, isAroundSummit, pathToSequences, areBothStrands, minimalLengthOfSequenceRegion, smc, pathToOutputs, tablesName, jobControl, 10 + 90 * i / tracks.size(), 10 + 90 * (i + 1) / tracks.size());
        }
        return pathToOutputs.getDataCollection();
    }

    private Object[] getCharacteristicNamesAndSummitIndicator(List<Track> tracksWithPeaks)
    {
        Set<String> allNegative = StreamEx.of( tracksWithPeaks ).map( ChipSeqPeak::readChromosomeAndPeaks )
            .flatMap( ChipSeqPeak::negativeCharacteristicNames ).toSet();
        String[] resultList = ChipSeqPeak.characteristics().remove( allNegative::contains ).toArray( String[]::new );
        boolean isSummitExist = ChipSeqPeak.isSummitExist( resultList );
        return new Object[]{resultList, isSummitExist};
    }

    private Map<String, Map<String, List<ChipSeqPeak>>> getNumberOfOverlapsAndPeaks(Track track, List<Track> tracks) throws Exception
    {
        Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = ChipSeqPeak.readChromosomeAndPeaks(track);
        return StreamEx.ofValues(chromosomeAndPeaks).flatMap( List::stream )
            .peek( peak -> peak.increaseNumberOfOverlaps( tracks ) )
            .groupingBy( peak -> "Number of overlaps = " + peak.getNumberOfOverlaps(),
                    Collectors.groupingBy( ChipSeqPeak::getChromosome ));
    }

    public static class ROCcurvesInOverlappedChipSeqPeaksParameters extends AbstractFiveSiteModelsParameters
    {
        private DataElementPath chipSeqTracksPath;
        private String tfClassWithName; // only tfClass with >= 2 tracks can be selected !!!
        
        @PropertyName(MessageBundle.PN_CHIPSEQ_FOLDER)
        @PropertyDescription(MessageBundle.PD_CHIPSEQ_FOLDER)
        public DataElementPath getChipSeqTracksPath()
        {
            return chipSeqTracksPath;
        }

        public void setChipSeqTracksPath(DataElementPath chipSeqTracksPath)
        {
            Object oldValue = this.chipSeqTracksPath;
            this.chipSeqTracksPath = chipSeqTracksPath;
            firePropertyChange("chipSeqTracksPath", oldValue, chipSeqTracksPath);
        }

        @PropertyName(MessageBundle.PN_TF_CLASS)
        @PropertyDescription(MessageBundle.PD_TF_CLASS)
        public String getTfClassWithName()
        {
            return tfClassWithName;
        }
        public void setTfClassWithName(String tfClassWithName)
        {
            Object oldValue = this.tfClassWithName;
            this.tfClassWithName = tfClassWithName;
            firePropertyChange("tfClassWithName", oldValue, tfClassWithName);
        }
    }

    // only tfClasses with >= 2 tracks are selected !!!
    private static StreamEx<String> getTfClassesWithNamesFromGTRD(DataElementPath pathToInputTracks, String givenSpecie)
    {
        DataCollection<DataElement> dataCollection = pathToInputTracks.getDataCollection();
        DataElementPath path = Module.getModulePath(dataCollection);
        DataCollection<ChIPseqExperiment> dc = path.getChildPath("Data", "experiments").getDataCollection(ChIPseqExperiment.class);
        return StreamEx.of( dc.stream() ).filter( exp -> exp.getSpecie().getLatinName().equals( givenSpecie ) )
            .mapToEntry( ChIPseqExperiment::getTfClassId, ChIPseqExperiment::getTfTitle )
            .nonNullKeys().nonNullValues()
            .join( BindingRegion.SEPARATOR_BETWEEN_TF_CLASS_AND_ITS_NAME )
            .distinct( 2 );
    }

    public static class ROCcurvesInOverlappedChipSeqPeaksParametersBeanInfo extends BeanInfoEx2<ROCcurvesInOverlappedChipSeqPeaksParameters>
    {
        public ROCcurvesInOverlappedChipSeqPeaksParametersBeanInfo()
        {
            super(ROCcurvesInOverlappedChipSeqPeaksParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInputChild("chipSeqTracksPath", beanClass, Track.class));
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            property("tfClassWithName").tags( bean -> {
                DataElementPath pathToInputTracks = bean.getChipSeqTracksPath();
                Species givenSpecie = bean.getSpecies();
                return getTfClassesWithNamesFromGTRD(pathToInputTracks, givenSpecie.getLatinName()).sorted();
            }).add();
            add(DataElementPathEditor.registerInput("matrixPath", beanClass, FrequencyMatrix.class));
            add("minRegionLength");
            add("aroundSummit");
            add("siteModelTypes", SiteModelTypesSelector.class);
            // add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class), "$trackPath$ summary"));
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class));

        }
    }
}
