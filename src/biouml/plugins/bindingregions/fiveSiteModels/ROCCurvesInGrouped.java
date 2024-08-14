package biouml.plugins.bindingregions.fiveSiteModels;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.SiteModelTypesSelector;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;

/**
 * @author lan
 * ROC-curves: sorted and grouped ChIP-Seq peaks are used for curve creation; summit(yes/no)
 */
public class ROCCurvesInGrouped extends AnalysisMethodSupport<ROCCurvesInGrouped.ROCCurvesInGroupedParameters>
{
    public ROCCurvesInGrouped(DataCollection<?> origin, String name)
    {
        super(origin, name, new ROCCurvesInGroupedParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkGreater("groupsNumber", 2);
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("ROC-curves: sorted and grouped ChIP-Seq peaks are used for curve creation; summit(yes/no)");
        final DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        final DataElementPath pathToChipSeqTrack = parameters.getTrackPath();
        final int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        boolean isAroundSummit = parameters.isAroundSummit();
        final int numberOfGroups = Math.max(2, parameters.getGroupsNumber());
        String[] siteModelTypes = parameters.getSiteModelTypes();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        final DataElementPath pathToOutputs = parameters.getOutputPath();
        boolean areBothStrands = true;

        // 1.
        log.info("Preliminary step: read ChIP-Seq peaks and create site models");
        final Track track = pathToChipSeqTrack.getDataElement(Track.class);
        final FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        final SiteModelsComparison smc = new SiteModelsComparison(siteModelTypes, matrix);
        final Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = ChipSeqPeak.readChromosomeAndPeaks(track);
        String[] characteristicNames = ChipSeqPeak.getPositiveCharacteristicNames(chromosomeAndPeaks);
        final boolean isActuallyAroundSummit = isAroundSummit && ChipSeqPeak.isSummitExist(characteristicNames);
        characteristicNames = (String[])ArrayUtils.removeElement(characteristicNames, ChipSeqPeak.SUMMIT);
        characteristicNames = (String[])ArrayUtils.add(characteristicNames, ChipSeqPeak.LENGTH);
        final String[] finalCharacteristicNames = characteristicNames;
        if( characteristicNames == null || characteristicNames.length == 0 )
        {
            log.warning("No characteristics found! Result is not created!");
            return null;
        }
        jobControl.setPreparedness(5);
        if(jobControl.isStopped()) return null;

        // 2.
        log.info("ROC-curve creaion and AUCs calculation");
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        jobControl.pushProgress(5, 100);
        jobControl.forCollection(Arrays.asList(characteristicNames), characteristicName -> {
            try
            {
                log.info("Characteristic: " + characteristicName);
                Map<String, Map<String, List<ChipSeqPeak>>> nameOfGroupAndPeaksOfGroup = ChipSeqPeak.getGroupedPeaks(numberOfGroups, characteristicName, chromosomeAndPeaks);
                if( nameOfGroupAndPeaksOfGroup == null ) return false;
                String commonSubNameOfTables = "_for_" + matrix.getName() + "_in_" + (new TrackInfo(track)).getTfClass() + "_" + pathToChipSeqTrack.getName() + "_groupedBy_" + characteristicName;
                if( isActuallyAroundSummit )
                    commonSubNameOfTables += "_summit_" + minimalLengthOfSequenceRegion;
                SiteModelsComparisonUtils.writeROCcurvesAndAUCsForGroupedPeaks(finalCharacteristicNames, nameOfGroupAndPeaksOfGroup, isActuallyAroundSummit, pathToSequences, areBothStrands, minimalLengthOfSequenceRegion, smc, pathToOutputs, commonSubNameOfTables, null, 0, 0);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
            return true;
        });
        jobControl.popProgress();
        return pathToOutputs.getDataCollection();
    }

    public static class ROCCurvesInGroupedParameters extends AbstractFiveSiteModelsParameters
    {
        private int groupsNumber = 2;

        @PropertyName(MessageBundle.PN_NUMBER_GROUPS)
        @PropertyDescription(MessageBundle.PD_NUMBER_GROUPS)
        public int getGroupsNumber()
        {
            return groupsNumber;
        }
        public void setGroupsNumber(int groupsNumber)
        {
            Object oldValue = this.groupsNumber;
            this.groupsNumber = groupsNumber;
            firePropertyChange("groupsNumber", oldValue, groupsNumber);
        }
    }

    public static class ROCCurvesInGroupedParametersBeanInfo extends BeanInfoEx2<ROCCurvesInGroupedParameters>
    {
        public ROCCurvesInGroupedParametersBeanInfo()
        {
            super(ROCCurvesInGroupedParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            property( "trackPath" ).inputElement( Track.class ).add();
            add("aroundSummit");
            add("minRegionLength");
            add("groupsNumber");
            add("siteModelTypes", SiteModelTypesSelector.class);
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).add();
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
