package ru.biosoft.bsastats.processors;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.IntArray;

/**
 * @author lan
 *
 */
public abstract class AbstractContentPerBaseProcessor extends AbstractStatisticsProcessor
{
    protected static class CountEntry
    {
        public IntArray counts;
        public LetterCounter counter;
        public Color color;
        public String name;
        
        public CountEntry(String name, LetterCounter counter, Color color)
        {
            this.name = name;
            this.counter = counter;
            this.color = color;
            this.counts = new IntArray();
        }
    }
    protected List<CountEntry> entries = new ArrayList<>();
    protected IntArray totalCounts = new IntArray();

    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        for(int i=entries.size()-1; i>=0; i--)
        {
            CountEntry entry = entries.get(i);
            entry.counter.countPerBase(sequence, entry.counts);
        }
        totalCounts.growTo(sequence.length);
        int[] data = totalCounts.data();
        for(int i=sequence.length-1; i>=0; i--) data[i]++;
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Position in read (bp)");
        options.getXAxis().setMin(1.0);
        options.getXAxis().setMax((double)totalCounts.size());
        options.getYAxis().setLabel("%");
        chart.setOptions(options);

        for(CountEntry entry: entries)
        {
            double[][] data = new double[totalCounts.size()][];
            IntArray curCounts = entry.counts;
            for(int i=0; i<totalCounts.size(); i++)
            {
                data[i] = new double[] {i+1, curCounts.get(i)*100.0/totalCounts.get(i)};
            }
            ChartSeries series = new ChartSeries(data);
            series.setLabel("% "+entry.name);
            series.setColor(entry.color);
            chart.addSeries(series);
        }
        
        ChartDataElement image = new ChartDataElement(getName()+" chart", resultsFolder, chart);
        resultsFolder.put(image);

        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(resultsFolder, getName());
        for(CountEntry entry: entries)
        {
            table.getColumnModel().addColumn(entry.name + " (count)", Integer.class);
            table.getColumnModel().addColumn(entry.name + " (%)", Double.class);
        }
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        for(int i=0; i<totalCounts.size(); i++)
        {
            Object[] values = new Object[entries.size()*2];
            int col=0;
            for(CountEntry entry: entries)
            {
                int count = entry.counts.get(i);
                values[col++] = count;
                values[col++] = count*100.0/totalCounts.get(i);
            }
            TableDataCollectionUtils.addRow(table, String.valueOf(i+1), values, true);
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
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception
    {
        boolean headerAdded = false;
        for(CountEntry entry: entries)
        {
            Chart chart = new Chart();
            ChartOptions options = new ChartOptions();
            options.getXAxis().setLabel("Position in read(bp)");
            options.getYAxis().setLabel("%");
            double maxY = 0;
            options.getXAxis().setMin(1.0);
            options.getYAxis().setMin(0.0);
            chart.setOptions(options);
            for(DataElementPath path: inputReports)
            {
                TableDataCollection table = path.getChildPath(getName()).optDataElement(TableDataCollection.class);
                if(table == null) continue;
                double[][] data = new double[table.getSize()][];
                for(int i=0; i<table.getSize(); i++)
                {
                    double value = ((Number)table.get(String.valueOf(i+1)).getValue(entry.name+" (%)")).doubleValue();
                    data[i] = new double[] {i+1, value};
                    if(value > maxY) maxY = value;
                }
                ChartSeries series = new ChartSeries(data);
                series.setLabel(path.getName());
                series.setColor(ColorUtils.getDefaultColor(chart.getSeriesCount()));
                chart.addSeries(series);
            }
            maxY = Math.min(100.0, maxY*1.01);
            options.getYAxis().setMax(maxY);
            if(chart.getSeriesCount() > 0)
            {
                if(!headerAdded)
                {
                    outputReport.addSubHeader(getName());
                    headerAdded = true;
                }
                if(entries.size() > 1)
                {
                    outputReport.addSubSubHeader(entry.name+" content");
                }
                outputReport.addImage(chart.getImage(800, 500), entry.name+"content", getName());
            }
        }
    }
}
