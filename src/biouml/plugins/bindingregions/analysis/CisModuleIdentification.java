package biouml.plugins.bindingregions.analysis;

import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.CisModule;
import biouml.plugins.bindingregions.utils.TrackInfo;

/**
 * @author lan
 * Identification of cis-modules1 or cis-modules2
 */
public class CisModuleIdentification extends AnalysisMethodSupport<CisModuleIdentification.CisModuleIdentificationParameters>
{
    public CisModuleIdentification(DataCollection<?> origin, String name)
    {
        super(origin, name, new CisModuleIdentificationParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        final int type = parameters.isType2() ? 2 : 1;
        final int minimalNumberOfOverlaps = parameters.getMinOverlaps();
        final DataElementPath pathToOutputs = parameters.getOutputPath();
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        log.info("Read merged binding regions in sql tracks. Calculate cisModules" + type + " and write them into tracks");
        DataCollection<DataElement> tracks = parameters.getChipSeqTracksPath().getDataCollection();
        jobControl.forCollection(DataCollectionUtils.asCollection(tracks, DataElement.class), de -> {
            try
            {
                if(!(de instanceof Track)) return true;
                Track track = (Track)de;
                log.info("Reading track info: " + track.getName());
                TrackInfo trackInfo = new TrackInfo(track);
                String specie = trackInfo.getSpecie();
                String pathToSequenceCollection = trackInfo.getPathToSequenceCollection();
                String cellLine = trackInfo.getCellLine();
                log.info("Reading binding regions in track = " + track.getName());

                Map<String, String> tfClassAndTfName = BindingRegion.getDistinctTfClassesAndNamesFromTrack(track);
                log.info("Track: specie = " + specie + " cell line = " + cellLine + " number of distinct TF-classes = " + tfClassAndTfName.size());
                if( tfClassAndTfName.size() < minimalNumberOfOverlaps ) return true;
                Map<String, List<CisModule>> allCisModules;
                if( type == 1 )
                {
                    Map<String, List<BindingRegion>> allBindingRegions = BindingRegion.readBindingRegionsFromTrack(track);
                    ListUtil.sortAll(allBindingRegions);
                    allCisModules = CisModule.getCisModules1(allBindingRegions, minimalNumberOfOverlaps);
                }
                else
                    allCisModules = CisModule.getAllCisModules2(track, null, minimalNumberOfOverlaps);
                log.info("Write identified cisModules" + type + " into track");
                String name = "CRMs_" + type + "_in_" + cellLine.replaceAll("/","|");
                CisModule.writeCisModulesIntoTrack(allCisModules, minimalNumberOfOverlaps, specie, cellLine, pathToSequenceCollection, tfClassAndTfName, pathToOutputs, name);
                return true;
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
        });
        return pathToOutputs.getDataCollection();
    }

    public static class CisModuleIdentificationParameters extends AbstractAnalysisParameters
    {
        private DataElementPath chipSeqTracksPath;
        private int minOverlaps = 2;
        private boolean type2 = true;
        private DataElementPath outputPath;

        @PropertyName(MessageBundle.PN_OUTPUT_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_PATH)
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange("outputPath", oldValue, outputPath);
        }

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

        @PropertyName(MessageBundle.PN_MIN_OVERLAPS)
        @PropertyDescription(MessageBundle.PD_MIN_OVERLAPS)
        public int getMinOverlaps()
        {
            return minOverlaps;
        }
        public void setMinOverlaps(int minOverlaps)
        {
            Object oldValue = this.minOverlaps;
            this.minOverlaps = minOverlaps;
            firePropertyChange("minOverlaps", oldValue, minOverlaps);
        }

        @PropertyName(MessageBundle.PN_CIS_MODULE_TYPE_2)
        @PropertyDescription(MessageBundle.PD_CIS_MODULE_TYPE_2)
        public boolean isType2()
        {
            return type2;
        }
        public void setType2(boolean type2)
        {
            Object oldValue = this.type2;
            this.type2 = type2;
            firePropertyChange("type2", oldValue, type2);
        }
    }

    public static class CisModuleIdentificationParametersBeanInfo extends BeanInfoEx2<CisModuleIdentificationParameters>
    {
        public CisModuleIdentificationParametersBeanInfo()
        {
            super(CisModuleIdentificationParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "chipSeqTracksPath" ).inputElement( Track.class ).add();
            add("minOverlaps");
            add("type2");
            property( "outputPath" ).outputElement( FolderCollection.class ).auto( "$chipSeqTracksPath/parent$/CisModules" ).add();
        }
    }
}
