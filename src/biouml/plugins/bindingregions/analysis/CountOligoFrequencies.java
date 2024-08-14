package biouml.plugins.bindingregions.analysis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.Olig;
import biouml.plugins.bindingregions.utils.TrackInfo;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 * Extracted from BindingRegions/mode15
 * 15. Count olig frequencies in binding regions
 */
public class CountOligoFrequencies extends AnalysisMethodSupport<CountOligoFrequencies.CountOligoFrequenciesParameters>
{
    public CountOligoFrequencies(DataCollection<?> origin, String name)
    {
        super(origin, name, new CountOligoFrequenciesParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        DataElementPath pathToOutputs = parameters.getOutputPath();
        DataElementPath pathToSingleTrack = parameters.getTrackPath();

        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));

        log.info("Count olig frequencies in binding regions");
        int oligLength = parameters.getOligLength();

        int minimalLengthOfSequenceRegion = 300;
        writeOligsFrequenciesInBindingRegionsIntoTables(pathToSingleTrack, pathToOutputs, oligLength, minimalLengthOfSequenceRegion);

        return pathToOutputs.getDataCollection();
    }

    private void writeOligsFrequenciesInBindingRegionsIntoTables(DataElementPath pathToSingleTrack, final DataElementPath pathToOutputs, final int oligLength, final int minimalLengthOfSequenceRegion) throws Exception
    {
        log.info("Read binding regions from "+pathToSingleTrack.getName());
        final Map<String, List<BindingRegion>> allBindingRegions = BindingRegion.readBindingRegionsFromTrack(pathToSingleTrack);
        jobControl.setPreparedness(5);
        Set<String> distinctTfClasses = BindingRegion.getDistinctTfClasses(allBindingRegions);
        final DataElementPath pathToSequences = TrackInfo.getPathToSequences(pathToSingleTrack);
        jobControl.pushProgress(10, 100);
        jobControl.forCollection(distinctTfClasses, tfClass -> {
            try
            {
                DataElementPath dep = pathToOutputs.getChildPath(tfClass);
                if( dep.exists() )
                {
                    log.info("tfClass = " + tfClass + " - exists; skipping");
                    return true;
                }
                log.info("tfClass = " + tfClass);
                Map<String, List<BindingRegion>> selectedBindingRegions = filterBindingRegionsByTfClass(allBindingRegions, tfClass);
                jobControl.setPreparedness(20);
//                    Object[] objects = CountOligoFrequencies.Olig.getOligFrequencies(selectedBindingRegions, pathToSequences, minimalLengthOfSequenceRegion, oligLength);
                Object[] objects = Olig.getOligFrequencies(selectedBindingRegions, pathToSequences, minimalLengthOfSequenceRegion, oligLength);
                jobControl.setPreparedness(50);
                Integer i = (Integer)objects[0];
                int numberOfUsedBindingRegions = i;
                int[] frequencies = (int[])objects[1];
                TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
                table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
                table.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
                table.getColumnModel().addColumn("olig", String.class);
                table.getColumnModel().addColumn("complementOlig", String.class);
                table.getColumnModel().addColumn("oligFrequency", Integer.class);
                table.getColumnModel().addColumn("oligRelativeFrequency", Double.class);
                int iRow = 0;
                for( int hash = 0; hash < frequencies.length; hash++ )
                {
                    Float freq;
                    if( frequencies[hash] == 0 ) continue;
                    if( numberOfUsedBindingRegions > 0 )
                        freq = (float)frequencies[hash] / (float)numberOfUsedBindingRegions;
                    else
                        freq = (float)0;
//                      CountOligoFrequencies.Olig olig = new CountOligoFrequencies.Olig(hash, oligLength);
//                      CountOligoFrequencies.Olig complementOlig = olig.getComplementOlig();
                    Olig olig = new Olig(hash, oligLength);
                    Olig complementOlig = olig.getComplementOlig();
                    String stringOlig = olig.toString();
                    String stringComplementOlig = complementOlig.toString();
                    TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {stringOlig, stringComplementOlig, frequencies[hash], freq}, true);
                }
                table.finalizeAddition();
                CollectionFactoryUtils.save(table);
                return true;
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
        });
        jobControl.popProgress();
    }

    private static Map<String, List<BindingRegion>> filterBindingRegionsByTfClass(Map<String, List<BindingRegion>> allBindingRegions, String givenTfClass)
    {
        return EntryStream.of(allBindingRegions)
                .mapValues(brs -> StreamEx.of(brs).filter(br -> br.getTfClass().equals(givenTfClass)).toList())
                .removeValues(List::isEmpty).toMap();
    }

    public static class CountOligoFrequenciesParameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath;
        private int oligLength = 6;
        private DataElementPath outputPath;

        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_MERGED)
        public DataElementPath getTrackPath()
        {
            return trackPath;
        }
        public void setTrackPath(DataElementPath trackPath)
        {
            Object oldValue = this.trackPath;
            this.trackPath = trackPath;
            firePropertyChange("trackPath", oldValue, trackPath);
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

        @PropertyName(MessageBundle.PN_OLIG_LENGTH)
        @PropertyDescription(MessageBundle.PD_OLIG_LENGTH)
        public int getOligLength()
        {
            return oligLength;
        }
        public void setOligLength(int oligLength)
        {
            Object oldValue = this.oligLength;
            this.oligLength = oligLength;
            firePropertyChange("oligLength", oldValue, oligLength);
        }

    }

    public static class CountOligoFrequenciesParametersBeanInfo extends BeanInfoEx2<CountOligoFrequenciesParameters>
    {
        public CountOligoFrequenciesParametersBeanInfo()
        {
            super(CountOligoFrequenciesParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            add("oligLength");
            property( "outputPath" ).outputElement( FolderCollection.class ).auto( "$trackPath$ olig counts" ).add();
        }
    }
}
