package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.eclipsesource.json.JsonObject;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.CisModule;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gap;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;
import biouml.plugins.gtrd.TrackSqlTransformer;
import biouml.standard.type.Species;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

/**
 * @author lan
 */
public class MergeCellLineBindingRegions extends AnalysisMethodSupport<MergeCellLineBindingRegions.MergeCellLineBindingRegionsParameters>
{
    public MergeCellLineBindingRegions(DataCollection<?> origin, String name)
    {
        super(origin, name, new MergeCellLineBindingRegionsParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        final DataElementPath pathToInputChipSeqTracks = parameters.getChipSeqTracksPath();
        final Species givenSpecie = parameters.getSpecies();
        final DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToTableWithCellLineSynonyms = parameters.getCellLineSynonymsPath();
        DataElementPath pathToTableWithChromosomeGaps = parameters.getChromosomeGapsPath();
        final DataElementPath pathToOutputs = parameters.getOutputPath();
        // Identification of information about ChIP-Seq tracks
        log.info("Reading tracks info");
//      final List<TrackInfo> trackInfos = TrackInfo.getTracksInfo(pathToInputChipSeqTracks, givenSpecie, pathToSequences);
        final List<TrackInfo> trackInfos = TrackInfo.getTracksInfo(pathToInputChipSeqTracks, givenSpecie);
        log.info(" number of tracks = " + trackInfos.size());
        log.info("Correct cell lines via synonyms table");
        Map<String, String> cellLineAndSynonymCellLine = TableUtils.readGivenColumnInStringTable(pathToTableWithCellLineSynonyms, MessageBundle.CELL_LINE_SYNONYM_COLUMN);
        if( cellLineAndSynonymCellLine != null )
            TrackInfo.changeSynonymCellLines(trackInfos, cellLineAndSynonymCellLine);
        log.info("Reading distinct cell lines");
        List<String> distinctCellLines = TrackInfo.getDistinctCellLines(trackInfos);
        distinctCellLines.add(BindingRegion.ALL_CELL_LINES);
        log.info("Reading chromosome gaps");
        final Map<String, List<Gap>> chromosomeNameAndGaps = EnsemblUtils.readChromosomeNameAndGapsInTable(pathToTableWithChromosomeGaps);
        if( chromosomeNameAndGaps.isEmpty() )
            log.warning("No gaps found: check gaps table");
        jobControl.setPreparedness(20);
        if(jobControl.isStopped())
        {
            return null;
        }
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        log.info("Processing cell lines");
        jobControl.pushProgress(20, 100);
        jobControl.forCollection(distinctCellLines, cellLine -> {
            try
            {
                List<TrackInfo> cellSpecificTrackInfos;
                if( cellLine.equals(BindingRegion.ALL_CELL_LINES) )
                    cellSpecificTrackInfos = trackInfos;
                else
                    cellSpecificTrackInfos = TrackInfo.getTrackInfosWithGivenCellLine(trackInfos, cellLine);
                log.info("cellLine = " + cellLine + " ; number of tracks = " + cellSpecificTrackInfos.size());
                for( TrackInfo ti : cellSpecificTrackInfos )
                    log.info("track name = " + ti.getTrackName());
                TObjectIntMap<String> distinctTfClassAndNumberOfTracks = getDistinctTfClassAndNumberOfTracks(cellSpecificTrackInfos);
                String trackName = cellLine.replaceAll("/", "|");
                writeMergedBindingRegionsIntoTrack(givenSpecie, cellLine, pathToSequences, cellSpecificTrackInfos, pathToInputChipSeqTracks, chromosomeNameAndGaps, distinctTfClassAndNumberOfTracks, pathToOutputs, "merged_BRs_in_" + trackName);
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

    private static TObjectIntMap<String> getDistinctTfClassAndNumberOfTracks(List<TrackInfo> trackInfos)
    {
        TObjectIntMap<String> result = new TObjectIntHashMap<>();
        for( TrackInfo trackInfo : trackInfos )
            result.adjustOrPutValue(trackInfo.getTfClass(), 1, 1);
        return result;
    }

    TObjectLongMap<String> tfClassAndNumberOfBindingRegions = new TObjectLongHashMap<>();


    /***
     * Create merged binding regions for all available (through trackInfos) tfClasses for which there exist tracks of Chip-Seq peaks
     * @param chromosome
     * @param trackInfos
     * @param pathToChipSeqPeaks
     * @return
     * @throws Exception
     */
    private static List<BindingRegion> getAllBindingRegions(String chromosome, List<TrackInfo> trackInfos, DataElementPath pathToChipSeqPeaks) throws Exception
    {
        List<BindingRegion> result = new ArrayList<>();
        List<String> distinctTfClasses = TrackInfo.getDistinctTfClasses(trackInfos);
        for( String tfClass : distinctTfClasses )
        {
            List<Track> tracks = new ArrayList<>();
            for( TrackInfo ti : trackInfos )
            {
                if( ti.getTfClass().equals(tfClass) )
                {
//                  Track t = pathToChipSeqPeaks.getDataCollection(Track.class).get(ti.getTrackName());
                    Track t = (Track)pathToChipSeqPeaks.getDataCollection().get(ti.getTrackName());
                    tracks.add(t);
                }
            }
            List<BindingRegion> list = BindingRegion.getMergedBindingRegionsFromTracksWithPeaks(tracks, chromosome, tfClass);
            if( ! list.isEmpty() )
                result.addAll(list);
        }
        return result;
    }

    private static void removeAllBindingRegionsThatOverlappedWithGaps(List<BindingRegion> bindingRegions, List<Gap> gaps)
    {
        if(gaps == null) return;
        Iterator<BindingRegion> iter = bindingRegions.iterator();
        while( iter.hasNext() )
        {
            BindingRegion bindingRegion = iter.next();
            for( Gap gap : gaps )
                if( gap.getInterval().intersects(bindingRegion.getInterval()) )
                {
                    iter.remove();
                    break;
                }
        }
    }

    // all binding regions have to be sorted (in increasing order of start positions)
    private static void calculateNumbersOfOverlaps(List<BindingRegion> bindingRegions)
    {
        for( int i = 0; i < bindingRegions.size() - 1; i++ )
        {
            BindingRegion br1 = bindingRegions.get(i);
            for( int ii = i + 1; ii < bindingRegions.size(); ii++ )
            {
                BindingRegion br2 = bindingRegions.get(ii);
                if( br1.getFinishPosition() < br2.getStartPosition() ) break;
                br1.increaseNumberOfOverlaps();
                br2.increaseNumberOfOverlaps();
            }
        }
    }

    private static void writeMergedBindingRegionsIntoTrack(Species givenSpecie, String cellLine, DataElementPath pathToSequences, List<TrackInfo> trackInfos, DataElementPath pathToChipSeqPeaks, Map<String, List<Gap>> chromosomeNameAndGaps, TObjectIntMap<String> distinctTfClassAndNumberOfTracks, DataElementPath pathToOutputs, String trackName) throws Exception
    {
        SqlTrack track = SqlTrack.createTrack(pathToOutputs.getChildPath(trackName), null);
        for( Sequence sequence : EnsemblUtils.getSequences(pathToSequences) )
        {
            String chromosome = sequence.getName();
            List<BindingRegion> bindingRegions = getAllBindingRegions(chromosome, trackInfos, pathToChipSeqPeaks);
            if( bindingRegions == null || bindingRegions.isEmpty() ) continue;
            if( chromosomeNameAndGaps != null && ! chromosomeNameAndGaps.isEmpty() )
            {
                List<Gap> gaps = chromosomeNameAndGaps.get(chromosome);
                removeAllBindingRegionsThatOverlappedWithGaps(bindingRegions, gaps);
            }
            Collections.sort(bindingRegions);
            calculateNumbersOfOverlaps(bindingRegions);
            for( BindingRegion br : bindingRegions )
            {
                int length = br.getLengthOfBindingRegion();
                if( length <= 0 ) continue;
                String tfClass = br.getTfClass();
                Site site = new SiteImpl(track, tfClass, tfClass, Site.BASIS_PREDICTED, br.getStartPosition(), length, Site.PRECISION_NOT_KNOWN, Site.STRAND_BOTH, sequence, null);
                DynamicPropertySet dps = site.getProperties();
                dps.add(new DynamicProperty(BindingRegion.NUMBER_OF_OVERLAPS, Integer.class, br.getNumberOfOverlap()));
                dps.add(new DynamicProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY, String.class, tfClass));
                int numberOfMergedTracks = distinctTfClassAndNumberOfTracks.get(tfClass);
                dps.add(new DynamicProperty(BindingRegion.NUMBER_OF_MERGED_TRACKS, Integer.class, numberOfMergedTracks));
                track.addSite(site);
            }
        }
        track.finalizeAddition();
        track.getInfo().getProperties().setProperty(TrackSqlTransformer.SPECIE_PROPERTY, givenSpecie.getLatinName());
        track.getInfo().getProperties().setProperty(Track.SEQUENCES_COLLECTION_PROPERTY, pathToSequences.toString());
        track.getInfo().getProperties().setProperty(TrackSqlTransformer.CELL_LINE_PROPERTY, cellLine);
        Map<String, String> tfClassAndTfName = TrackInfo.getDistinctTfClassAndTfName(trackInfos);
        JsonObject object = JsonUtils.fromMap( tfClassAndTfName );
        track.getInfo().getProperties().setProperty(CisModule.DISTINCT_TFCLASSES_AND_NAMES, object.toString());
        CollectionFactoryUtils.save(track);
    }

    public static class MergeCellLineBindingRegionsParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private DataElementPath chipSeqTracksPath, cellLineSynonymsPath, chromosomeGapsPath;
        private Species species = Species.getDefaultSpecies(null);
        private DataElementPath outputPath;

        public MergeCellLineBindingRegionsParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }

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

        @PropertyName(MessageBundle.PN_CELLLINE_SYNONYMS_TABLE)
        @PropertyDescription(MessageBundle.PD_CELLLINE_SYNONYMS_TABLE)
        public DataElementPath getCellLineSynonymsPath()
        {
            return cellLineSynonymsPath;
        }

        public void setCellLineSynonymsPath(DataElementPath cellLineSynonymsPath)
        {
            Object oldValue = this.cellLineSynonymsPath;
            this.cellLineSynonymsPath = cellLineSynonymsPath;
            firePropertyChange("cellLineSynonymsPath", oldValue, cellLineSynonymsPath);
        }

        @PropertyName(MessageBundle.PN_CHROMOSOME_GAPS_TABLE)
        @PropertyDescription(MessageBundle.PD_CHROMOSOME_GAPS_TABLE)
        public DataElementPath getChromosomeGapsPath()
        {
            return chromosomeGapsPath;
        }
        public void setChromosomeGapsPath(DataElementPath chromosomeGapsPath)
        {
            Object oldValue = this.chromosomeGapsPath;
            this.chromosomeGapsPath = chromosomeGapsPath;
            firePropertyChange("chromosomeGapsPath", oldValue, chromosomeGapsPath);
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
    }

    public static class MergeCellLineBindingRegionsParametersBeanInfo extends BeanInfoEx
    {
        public MergeCellLineBindingRegionsParametersBeanInfo()
        {
            super( MergeCellLineBindingRegionsParameters.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInputChild("chipSeqTracksPath", beanClass, Track.class));
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            add(DataElementPathEditor.registerInput("cellLineSynonymsPath", beanClass, TableDataCollection.class, true));
            add(DataElementPathEditor.registerInput("chromosomeGapsPath", beanClass, TableDataCollection.class));
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class));
        }
    }
}
