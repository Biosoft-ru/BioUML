package biouml.plugins.bindingregions.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.SampleComparison;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.TableUtils;

/**
 * @author lan
 * ChIP-Seq peak characteristics: distribution of each characteristic and correlations between them
 */
public class ChIPSeqCharacteristicsDistribution extends AnalysisMethodSupport<ChIPSeqCharacteristicsDistribution.ChIPSeqCharacteristicsDistributionParameters>
{
    public ChIPSeqCharacteristicsDistribution(DataCollection<?> origin, String name)
    {
        super(origin, name, new ChIPSeqCharacteristicsDistributionParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("ChIP-Seq peak characteristics: distribution of each characteristic and correlations between them");
        DataElementPath pathToOutputs = parameters.getOutputPath();
        DataElementPath pathToSingleTrack = parameters.getTrackPath();

        // 1.
        log.info("Read ChIP-Seq peaks from track and form the array of peak characteristics");
        Track track = pathToSingleTrack.getDataElement(Track.class);
        Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = ChipSeqPeak.readChromosomeAndPeaks(track);
        jobControl.setPreparedness(10);
        if( jobControl.isStopped() ) return null;
        String[] characteristicsNames = ChipSeqPeak.getPositiveCharacteristicNames(chromosomeAndPeaks);
        if( characteristicsNames == null || characteristicsNames.length == 0 )
            throw new IllegalArgumentException("Invalid track supplied. Make sure this track was created by MACS or SISSRs peak detection algorithms.");
        else if( ! ArrayUtils.contains(characteristicsNames, ChipSeqPeak.SUMMIT) )
            characteristicsNames = (String[])ArrayUtils.add(characteristicsNames, ChipSeqPeak.LENGTH);
        else
            for( int i = 0; i < characteristicsNames.length; i++ )
                if( characteristicsNames[i].equals(ChipSeqPeak.SUMMIT) )
                    characteristicsNames[i] = ChipSeqPeak.LENGTH;
        
        // 2.
        log.info("Create peak characteristics samples");
        Map<String, double[]> nameAndSample = StreamEx.of(characteristicsNames)
                .mapToEntry(name -> ChipSeqPeak.getValuesOfGivenCharacteristic(chromosomeAndPeaks, name))
                .nonNullValues().filterValues(sample -> sample.length >= 2).toSortedMap();
        SampleComparison sc = new SampleComparison(nameAndSample, "is not used");
        jobControl.setPreparedness(20);
        if( jobControl.isStopped() ) return null;

        // 3.
        log.info("Create charts and tables and write them");
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        String trackName = pathToSingleTrack.getName();
        // sc.writeIndividualChartsWithSmoothedDensities(true, null, 1, null, pathToOutputs.getChildPath(trackName + "_densities_charts"));
        SampleComparison.writeIndividualChartsWithSmoothedDensities(nameAndSample, true, null, DensityEstimation.WINDOW_WIDTH_01, null, pathToOutputs.getChildPath(trackName + "_densities_charts"));
        jobControl.setPreparedness(60);
        if( jobControl.isStopped() ) return null;
        sc.writeTableWithMeanAndSigma(pathToOutputs, trackName + "_summary");
        sc.writeTableWithPearsonCorrelationMatrix(pathToOutputs, trackName + "_correlationMatrix");
        sc.writeTableWithAllClouds(pathToOutputs, trackName + "_clouds_charts");
        jobControl.setPreparedness(80);
        if( jobControl.isStopped() ) return null;
        Chart chart = getSummitLocationAlongSequences(chromosomeAndPeaks);
        if( chart != null )
            TableUtils.addChartToTable("Summit locations along sequences", chart, pathToOutputs.getChildPath(trackName + "_summitLocation_chart"));
        jobControl.setPreparedness(100);
        return pathToSingleTrack.getDataCollection();
    }

    private Chart getSummitLocationAlongSequences(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks)
    {
        if( ! ChipSeqPeak.isSummitExist(chromosomeAndPeaks) ) return null;
        Map<String, double[]> map = Collections.singletonMap( "", StreamEx.ofValues( chromosomeAndPeaks ).flatMap( List::stream )
                .mapToDouble( peak -> 100.0 * peak.getSummit() / peak.getLengthOfPeak() ).toArray() );
        SampleComparison sc = new SampleComparison(map, "Normalized positions of summits (in %)");
        return sc.chartWithSmoothedDensities(false, null, DensityEstimation.WINDOW_WIDTH_04, 5.0);
    }
    
    public static class ChIPSeqCharacteristicsDistributionParameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath;
        private DataElementPath outputPath;
        
        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_PEAK_FINDER)
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
    }

    public static class ChIPSeqCharacteristicsDistributionParametersBeanInfo
            extends BeanInfoEx2<ChIPSeqCharacteristicsDistributionParameters>
    {
        public ChIPSeqCharacteristicsDistributionParametersBeanInfo()
        {
            super(ChIPSeqCharacteristicsDistributionParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            property( "outputPath" ).inputElement( FolderCollection.class ).auto( "" ).add();
        }
    }
}
