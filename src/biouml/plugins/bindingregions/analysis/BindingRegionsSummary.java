package biouml.plugins.bindingregions.analysis;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.gtrd.TrackSqlTransformer;

import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * @author lan
 * Extracted from BindingRegions/mode3
 * "3. Summary of binding regions, density of overlaps (creation of tables: 'summaryOfBindingRegions', 'densityOfBindingRegionOverlaps', 'chart_densityOfBindingRegionOverlaps')"
 */
public class BindingRegionsSummary extends AnalysisMethodSupport<BindingRegionsSummary.BindingRegionsSummaryParameters>
{
    public BindingRegionsSummary(DataCollection<?> origin, String name)
    {
        super(origin, name, new BindingRegionsSummaryParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Create summary on binding regions");
        DataElementPath pathToOutputs = parameters.getOutputPath();
        DataElementPath pathToSingleTrack = parameters.getTrackPath();

        // Read tfClassAndTfName in track
        Track track = pathToSingleTrack.getDataElement(Track.class);
        Map<String, String> tfClassAndTfName = BindingRegion.getDistinctTfClassesAndNamesFromTrack(track);
        log.info("Read binding regions in track and create initial data sets");
        
        // Create initial maps
        Map<String, Integer> tfClassAndNumberOfMergedTracks = new HashMap<>();
        TObjectLongMap<String> tfClassAndNumberOfBindingRegions = new TObjectLongHashMap<>();
        TObjectLongMap<String> tfClassAndSumOfLengths = new TObjectLongHashMap<>();
        TObjectLongMap<String> tfClassAndSumOfNumberOfOverlaps = new TObjectLongHashMap<>();
        DataCollection<Site> sites = track.getAllSites();
        int iJobControl = 0;
        for( Site site: sites )
        {
            DynamicPropertySet properties = site.getProperties();
            String tfClass = properties.getValueAsString(TrackSqlTransformer.TF_CLASS_ID_PROPERTY);
            if( tfClass == null )
                throw new IllegalArgumentException("Invalid track supplied");
            tfClassAndNumberOfBindingRegions.adjustOrPutValue(tfClass, 1, 1);
            Integer numberOfMergedTracks = (Integer)properties.getValue(BindingRegion.NUMBER_OF_MERGED_TRACKS);
            tfClassAndNumberOfMergedTracks.put(tfClass, numberOfMergedTracks);
            int length = site.getLength();
            tfClassAndSumOfLengths.adjustOrPutValue(tfClass, length, length);
            Integer numberOfOverlaps = (Integer)properties.getValue(BindingRegion.NUMBER_OF_OVERLAPS);
            tfClassAndSumOfNumberOfOverlaps.adjustOrPutValue(tfClass, numberOfOverlaps, numberOfOverlaps);
            jobControl.setPreparedness( 100 * (++iJobControl) / sites.getSize());
            if(jobControl.isStopped()) return null;
        }
        // Create and write chart and table
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        createAndWriteCharts(tfClassAndNumberOfBindingRegions, tfClassAndSumOfLengths, tfClassAndSumOfNumberOfOverlaps, pathToOutputs, "_charts_densities");
        createAndWriteTableWithSummary(tfClassAndTfName, tfClassAndNumberOfBindingRegions, tfClassAndSumOfLengths, tfClassAndSumOfNumberOfOverlaps, tfClassAndNumberOfMergedTracks, pathToOutputs, "summaryOnBindingRegions");
        return pathToOutputs.getDataCollection();
    }
    
    private static void createAndWriteCharts(TObjectLongMap<String> tfClassAndNumberOfBindingRegions, TObjectLongMap<String> tfClassAndSumOfLengths, TObjectLongMap<String> tfClassAndSumOfNumberOfOverlaps, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        Map<String, Chart> charts = new HashMap<>();
        double[] sample1 = new double[tfClassAndSumOfLengths.size()];
        int i = 0;
        for( String tfClass : tfClassAndSumOfLengths.keySet() )
            sample1[i++] = (double)tfClassAndSumOfLengths.get(tfClass) / (double)tfClassAndNumberOfBindingRegions.get(tfClass);
        List<double[]> curve1 = Stat.getEmpiricalDensitySmoothedByEpanechninkov(sample1, 0.1 * Stat.mean(sample1), true);
        Chart chart1 = TableUtils.createChart(curve1.get(0), curve1.get(1), "Mean length of binding regions", "Probability", null, Color.BLUE);
        charts.put("Mean length of binding regions", chart1);
        double[] sample2 = new double[tfClassAndSumOfNumberOfOverlaps.size()];
        i = 0;
        for( String tfClass : tfClassAndSumOfNumberOfOverlaps.keySet() )
            sample2[i++] = (double)tfClassAndSumOfNumberOfOverlaps.get(tfClass) / (double)tfClassAndNumberOfBindingRegions.get(tfClass);
        List<double[]> curve2 = Stat.getEmpiricalDensitySmoothedByEpanechninkov(sample2, 0.1 * Stat.mean(sample2), true);
        Chart chart2 = TableUtils.createChart(curve2.get(0), curve2.get(1), "Mean number of overlaps", "Probability", null, Color.BLUE);
        charts.put("Mean number of overlaps", chart2);
        TableUtils.writeChartsIntoTable(charts, "chart", pathToOutputs.getChildPath(tableName));
    }
    
    private static void createAndWriteTableWithSummary(Map<String, String> tfClassAndTfName, TObjectLongMap<String> tfClassAndNumberOfBindingRegions, TObjectLongMap<String> tfClassAndSumOfLengths, TObjectLongMap<String> tfClassAndSumOfNumberOfOverlaps, Map<String, Integer> tfClassAndNumberOfMergedTracks, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        int m = 4; // m = number of characteristics considered
        int ii = 0;
        for( Integer number : tfClassAndNumberOfMergedTracks.values() )
            ii += number;
        tfClassAndNumberOfMergedTracks.put("all TF-classes", ii);
        TObjectLongMap<String> map = null;
        for( int j = 0; j < m - 1; j++ )
        {
            switch( j )
            {
                case 0: map = tfClassAndNumberOfBindingRegions; break;
                case 1: map = tfClassAndSumOfLengths; break;
                case 2: map = tfClassAndSumOfNumberOfOverlaps; break;
            }
            long sum = 0;
            for( long number : map.values() )
                sum += number;
            map.adjustOrPutValue("all TF-classes", sum, sum);
        }
        String[] namesOfColumns = new String[]{"Number of merged tracks", "Number of binding regions", "Mean length of binding regions", "Mean number of overlaps"};
        String[] namesOfRows = new String[tfClassAndNumberOfMergedTracks.size()];
        double[][] data = new double[tfClassAndNumberOfMergedTracks.size()][m];
        int j = -1;
        for( Map.Entry<String, Integer> entry : tfClassAndNumberOfMergedTracks.entrySet() )
        {
            String tfClass = entry.getKey();
            data[++j][0] = entry.getValue();
            data[j][1] = tfClassAndNumberOfBindingRegions.get(tfClass);
            data[j][2] = tfClassAndSumOfLengths.get(tfClass) / data[j][1];
            data[j][3] = tfClassAndSumOfNumberOfOverlaps.get(tfClass) / data[j][1];
            namesOfRows[j] = tfClass;
            if( tfClassAndTfName.get(tfClass) != null )
                namesOfRows[j] += "_" + tfClassAndTfName.get(tfClass);
        }
        TableUtils.writeDoubleTable(data, namesOfRows, namesOfColumns, pathToOutputs, tableName);
    }

    public static class BindingRegionsSummaryParameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath;
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
    }

    public static class BindingRegionsSummaryParametersBeanInfo extends BeanInfoEx2<BindingRegionsSummaryParameters>
    {
        public BindingRegionsSummaryParametersBeanInfo()
        {
            super(BindingRegionsSummaryParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            property( "outputPath" ).outputElement( FolderCollection.class ).auto( "$trackPath$ summary" ).add();
        }
    }
}
