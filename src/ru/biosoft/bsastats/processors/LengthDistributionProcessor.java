package ru.biosoft.bsastats.processors;

import java.io.IOException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.graphics.chart.AxisOptions.Transform;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.IntArray;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("Sequence length distribution")
@PropertyDescription("Calculates distribution of read lengths and outputs them as the table and as the chart")
public class LengthDistributionProcessor extends AbstractStatisticsProcessor
{
    private final IntArray lengths = new IntArray();
    private int totalCount = 0;

    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        totalCount++;
        int length = sequence.length;
        lengths.growTo(length+1);
        lengths.data()[length]++;
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        int min = 0;
        int max = lengths.size()-1;
        while(min < max && lengths.get(min) == 0) min++;
        if(min != max) raiseWarning();
        if(min == 0) raiseError();
        Transform transform = Transform.LOGARITHM;
        if(max-min < 2)
        {
            min--;
            max++;
            lengths.growTo(max+1);
            transform = Transform.LINEAR;
        }
        
        double[][] data = new double[max-min+1][];
        
        for(int i=min; i<=max; i++)
        {
            data[i-min] = new double[] {i, lengths.get(i)};
        }
        
        ChartSeries series = new ChartSeries(data);
        series.setLabel("Number of sequences with given length");
        
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Sequence length (bp)");
        options.getXAxis().setMin((double)min);
        options.getXAxis().setMax((double)max);
        options.getYAxis().setLabel("Count");
        options.getYAxis().setTransform(transform);
        
        Chart chart = new Chart();
        chart.addSeries(series);
        chart.setOptions(options);
        
        ChartDataElement image = new ChartDataElement(getName()+" chart", resultsFolder, chart);
        resultsFolder.put(image);
        
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(resultsFolder, getName());
        table.getColumnModel().addColumn("Count", Integer.class);
        table.getColumnModel().addColumn("% of total", Double.class);
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        for(int i=min; i<=max; i++)
        {
            int count = lengths.get(i);
            TableDataCollectionUtils.addRow(table, String.valueOf(i), new Object[] {count, count*100.0/totalCount}, true);
        }
        table.finalizeAddition();
        resultsFolder.put(table);
    }

    @Override
    public String[] getReportItemNames()
    {
        return new String[] {getName()+" chart"};
    }

    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws IOException, RepositoryException
    {
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Sequence length (bp)");
        options.getYAxis().setLabel("% of sequences");
        options.getYAxis().setMax(100.0);
        chart.setOptions(options);
        for(DataElementPath path: inputReports)
        {
            TableDataCollection table = path.getChildPath(getName()).optDataElement(TableDataCollection.class);
            if(table == null) continue;
            double[][] data = new double[table.getSize()][];
            int i=0;
            for(RowDataElement rde: table)
            {
                data[i++] = new double[] {Integer.parseInt(rde.getName()), ((Number)rde.getValue("% of total")).doubleValue()};
            }
            ChartSeries series = new ChartSeries(data);
            series.setLabel(path.getName());
            series.setColor(ColorUtils.getDefaultColor(chart.getSeriesCount()));
            chart.addSeries(series);
        }
        if(chart.getSeriesCount() > 0)
        {
            outputReport.addSubHeader(getName());
            outputReport.addImage(chart.getImage(800, 500), "seqlength", getName());
        }
    }
}
