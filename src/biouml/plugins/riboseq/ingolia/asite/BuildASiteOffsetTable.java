package biouml.plugins.riboseq.ingolia.asite;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import biouml.plugins.riboseq.transcripts.Transcript;
import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.graphics.chart.AxisOptions;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class BuildASiteOffsetTable extends AnalysisMethodSupport<BuildASiteOffsetTableParameters>
{
    public BuildASiteOffsetTable(DataCollection<?> origin, String name)
    {
        super( origin, name, new BuildASiteOffsetTableParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 3 );
        final List<BAMTrack> bamTrackList = Arrays.asList( parameters.getBAMTracks() );
        jobControl.popProgress();
        jobControl.pushProgress( 3, 10 );
        final List<Transcript> transcriptList = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );
        jobControl.popProgress();

        final ASiteOffsetBuilderParameters builderParameters = new ASiteOffsetBuilderParameters( parameters );
        final ASiteOffsetBuilder aSiteOffsetBuilder = new ASiteOffsetBuilder( builderParameters );

        jobControl.pushProgress( 10, 90 );
        aSiteOffsetBuilder.computeTableFromBamTracks( bamTrackList, transcriptList, jobControl);
        jobControl.popProgress();

        jobControl.pushProgress( 90, 100 );
        final TableDataCollection aSiteOffsetTableDC = createASiteOffsetTableDataCollection(
                aSiteOffsetBuilder.getASiteOffsetTable(),
                aSiteOffsetBuilder.getASiteOffsetCounterMap() );
        jobControl.popProgress();
        return aSiteOffsetTableDC;
    }

    private TableDataCollection createASiteOffsetTableDataCollection(Map<Integer, Integer> aSiteOffsetMap, Map
            <Integer, Map<Integer, Integer>> aSiteOffsetCounterMap)
    {
        final DataElementPath outputDataElementPath = parameters.getASiteOffsetTable();
        final TableDataCollection aSiteOffsetTable = TableDataCollectionUtils.createTableDataCollection( outputDataElementPath );

        final ColumnModel columnModel = aSiteOffsetTable.getColumnModel();
        columnModel.addColumn( "Length", Integer.class );
        columnModel.addColumn( "Offset", Integer.class );
        columnModel.addColumn( "Trust", String.class );
        columnModel.addColumn( "Histogram", Chart.class );

        int i = 0;
        for( Map.Entry<Integer, Integer> entry : aSiteOffsetMap.entrySet() )
        {
            final Integer lengthKey = entry.getKey();
            final int modeOffset = entry.getValue();
            final Map<Integer, Integer> offsetCounterMap = aSiteOffsetCounterMap.get( lengthKey );

            final Chart histogramChart = createHistogramChart();
            fillHistogram( histogramChart, offsetCounterMap );

            final boolean trust = decisionTrust( offsetCounterMap, modeOffset );
            final String valueOfTrust = String.valueOf( trust );

            final Object[] rowData = {lengthKey, modeOffset, valueOfTrust, histogramChart};
            TableDataCollectionUtils.addRow( aSiteOffsetTable, Integer.toString( i++ ), rowData );
        }

        aSiteOffsetTable.finalizeAddition();
        outputDataElementPath.save( aSiteOffsetTable );

        return aSiteOffsetTable;
    }

    private void fillHistogram(Chart chart, Map<Integer, Integer> offsetCounterMap)
    {
        double[][] values = EntryStream.of(offsetCounterMap)
                .map(entry -> new double[] { entry.getKey(), entry.getValue() }).toArray(double[][]::new);

        chart.getSeries( 0 ).setData( values );
    }

    private Chart createHistogramChart()
    {
        final Chart chart = new Chart();
        final ChartOptions chartOptions = new ChartOptions();
        final AxisOptions xAxis = new AxisOptions();

        final AxisOptions yAxis = new AxisOptions();
        chartOptions.setXAxis( xAxis );
        chartOptions.setYAxis( yAxis );

        final ChartSeries series = new ChartSeries();

        // to histogram style
        series.getLines().setShow( false );
        series.getBars().setShow( true );
        series.getBars().setWidth( 1.0 );

        chart.addSeries( series );

        return chart;
    }

    private boolean decisionTrust(Map<Integer, Integer> offsetCounterMap, int modeOffset)
    {
        final int lowLimitCounts = 100;
        final double trustBoundary = 0.5;
        final int sizeLocality = 3;

        int modeOffsetLocalityValue = 0;
        int sumValues = 0;
        for( final Map.Entry<Integer, Integer> entry : offsetCounterMap.entrySet() )
        {
            final Integer key = entry.getKey();
            final Integer value = entry.getValue();
            if( modeOffset - sizeLocality <= key && key <= modeOffset + sizeLocality )
            {
                modeOffsetLocalityValue += value;
            }

            sumValues += value;
        }

        if( sumValues < lowLimitCounts )
        {
            return false;
        }

        final double percent = ((double) modeOffsetLocalityValue) / sumValues;
        return percent >= trustBoundary;
    }
}
