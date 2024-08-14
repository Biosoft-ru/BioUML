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
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ColorUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("Quality per sequence")
@PropertyDescription("Distribution of phred quality score among the sequences")
public class QualityPerSequenceProcessor extends AbstractStatisticsProcessor
{
    boolean valid = true;
    int[] qualitiesMap = new int[255];
    int totalCount = 0;

    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        if(!valid) return;
        if(qualities == null)
        {
            log.warning("No phred quality information: Quality per sequence processor is disabled");
            valid = false;
            return;
        }
        totalCount++;
        int qualitySum = 0;
        for(byte quality: qualities) qualitySum+=quality;
        qualitiesMap[qualitySum/qualities.length]++;
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        if(!valid) return;
        int min = 0;
        int max = qualitiesMap.length-1;
        while(max > 0 && qualitiesMap[max] == 0) max--;
        while(min < max && qualitiesMap[min] == 0) min++;
        
        double[][] data = new double[max-min+1][];
        
        int freqQuality = 0;
        int freqValue = 0;
        for(int i=min; i<=max; i++)
        {
            int value = qualitiesMap[i];
            if(value > freqValue)
            {
                freqValue = value;
                freqQuality = i;
            }
            data[i-min] = new double[] {i, value};
        }
        if(freqQuality < 27) raiseWarning();
        if(freqQuality < 20) raiseError();
        
        ChartSeries series = new ChartSeries(data);
        series.setLabel("Number of sequences with given quality");
        
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Phred score");
        options.getXAxis().setMin((double)min);
        options.getXAxis().setMax((double)max);
        options.getYAxis().setLabel("Count");
        
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
            int count = qualitiesMap[i];
            TableDataCollectionUtils.addRow(table, String.valueOf(i), new Object[] {count, count*100.0/totalCount}, true);
        }
        table.finalizeAddition();
        resultsFolder.put(table);
    }


    @Override
    public String[] getReportItemNames()
    {
        if(!valid) return null;
        return new String[] {getName()+" chart"};
    }

    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws IOException, RepositoryException
    {
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Phred score");
        options.getYAxis().setLabel("% of sequences having given quality");
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
            outputReport.addImage(chart.getImage(800, 500), "seqquality", getName());
        }
    }
}
