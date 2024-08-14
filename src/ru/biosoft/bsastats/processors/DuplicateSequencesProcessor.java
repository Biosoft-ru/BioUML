package ru.biosoft.bsastats.processors;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableInt;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.graphics.chart.AxisOptions.Transform;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ColorUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("Duplicate sequences")
@PropertyDescription("Calculate the rate of sequences duplication: " +
        "how many sequences occurs 2, 3 and so on times relative to unique sequences. " +
        "This statistic is based on the first "+DuplicateSequencesProcessor.MAX_COUNT+" reads")
public class DuplicateSequencesProcessor extends AbstractStatisticsProcessor
{
    public final static int MAX_COUNT = 200000;
    private int totalCount = 0;
    private int totalCountAtLimit = 0;
    private final Map<PackedRead, MutableInt> dups = new HashMap<>();

    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        totalCount++;
        int length = sequence.length;
        if(length > 75) length = 50;
        PackedRead packedRead = new PackedRead(sequence, length);
        MutableInt count = dups.get(packedRead);
        if(count != null)
            count.increment();
        if(dups.size() < MAX_COUNT)
        {
            totalCountAtLimit++;
            if(count == null)
                dups.put(packedRead, new MutableInt(1));
        }
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        int[] dupCounts = new int[20];
        int[] dupCountsTotal = new int[20];
        int totalValues = 0;
        for(MutableInt dupCount: dups.values())
        {
            totalValues+=dupCount.intValue();
            dupCounts[Math.min(dupCount.intValue(), dupCounts.length)-1]++;
            dupCountsTotal[Math.min(dupCount.intValue(), dupCounts.length)-1]+=dupCount.intValue();
        }
        double dupRatio = (totalCount-(double)dupCounts[0]*totalCount/totalCountAtLimit)*100/totalCount;
        if(dupRatio > 50) raiseError();
        else if(dupRatio > 20) raiseWarning();
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Duplicates count (total: "+String.format("%.2f", dupRatio)+"%)");
        options.getXAxis().setMin(0.5);
        options.getXAxis().setMax(dupCounts.length+0.5);
        options.getYAxis().setLabel("Count");
        options.getYAxis().setTransform(Transform.LOGARITHM);
        chart.setOptions(options);
        
        double[][] data = new double[20][];
        for(int i=0; i<dupCounts.length; i++)
        {
            data[i] = new double[] {i+1, dupCounts[i]};
        }
        ChartSeries series = new ChartSeries(data);
        series.getLines().setShow(false);
        series.getBars().setShow(true);
        series.getBars().setWidth(0.7);
        series.setColor(new Color(40,40,255,200));
        chart.addSeries(series);
        
        ChartDataElement image = new ChartDataElement(getName()+" chart", resultsFolder, chart);
        resultsFolder.put(image);
        
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(resultsFolder, getName());
        table.getColumnModel().addColumn("Duplicates count", Integer.class);
        table.getColumnModel().addColumn("Duplicate groups count", Integer.class);
        table.getColumnModel().addColumn("Percentage relative to unique", Double.class);
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        for(int i=0; i<20; i++)
        {
            TableDataCollectionUtils.addRow(table, String.valueOf(i + 1) + ( i == 19 ? "+" : "" ), new Object[] {dupCountsTotal[i], dupCounts[i],
                    dupCounts[i] * 100.0 / dupCounts[0]}, true);
        }
        table.finalizeAddition();
        table.getOrigin().put(table);
    }

    @Override
    public String[] getReportItemNames()
    {
        return new String[] {getName()+" chart"};
    }

    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception
    {
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Duplicates count");
        options.getXAxis().setMin(0.5);
        options.getXAxis().setMax(20.5);
        options.getYAxis().setLabel("Count");
        options.getYAxis().setTransform(Transform.LOGARITHM);
        chart.setOptions(options);
        for(DataElementPath path: inputReports)
        {
            TableDataCollection table = path.getChildPath(getName()).optDataElement(TableDataCollection.class);
            if(table == null) continue;
            double[][] data = new double[table.getSize()][];
            for(int i=0; i<table.getSize(); i++)
            {
                data[i] = new double[] {
                        i + 1,
                        ( (Number)table.get(String.valueOf(i + 1) + ( i == table.getSize() - 1 ? "+" : "" )).getValue(
                                "Duplicate groups count") ).doubleValue()};
            }
            ChartSeries series = new ChartSeries(data);
            series.setLabel(path.getName());
            series.setColor(ColorUtils.getDefaultColor(chart.getSeriesCount()));
            chart.addSeries(series);
        }
        if(chart.getSeriesCount() > 0)
        {
            outputReport.addSubHeader(getName());
            outputReport.addImage(chart.getImage(800, 500), "seqdups", getName());
        }
    }
}
