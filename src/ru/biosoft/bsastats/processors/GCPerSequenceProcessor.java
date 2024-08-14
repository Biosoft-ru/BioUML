package ru.biosoft.bsastats.processors;

import java.awt.Color;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("GC content per sequence")
@PropertyDescription("Draws a distribution of GC content among reads")
public class GCPerSequenceProcessor extends AbstractStatisticsProcessor
{
    double[] gcCounts = new double[101];
    int totalCount = 0;
    double sumGC = 0;
    double sumSqGC = 0;
    
    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        int gcCount = getGCcount(sequence);
        double rLength = 1.0/sequence.length;
        double count = gcCount*100.0*rLength;
        totalCount++;
        sumGC+=count;
        sumSqGC+=count*count;
        double minCount = (gcCount-0.5)*100.0*rLength;
        double maxCount = (gcCount+0.5)*100.0*rLength;
        for(int i = (int)Math.round(minCount); i<=(int)Math.round(maxCount); i++)
        {
            if(i>100) break;
            if(i<0) continue;
            double increment = 1 - Math.max(0, minCount - ( i - 0.5 )) - Math.max(0, i + 0.5 - maxCount);
            gcCounts[i]+=increment/(maxCount-minCount);
        }
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        double[][] dataReal = new double[101][];
        for(int i=0; i<=100; i++)
        {
            dataReal[i] = new double[] {i, gcCounts[i]};
        }
        ChartSeries seriesReal = new ChartSeries(dataReal);
        seriesReal.setLabel("GC count per read");

        double meanGC = sumGC/totalCount;
        double sdGC2 = sumSqGC/totalCount-sumGC/totalCount*sumGC/totalCount;
        double[][] dataNormal = new double[101][];
        double devSum = 0;
        for(int i=0; i<=100; i++)
        {
            dataNormal[i] = new double[] {i, totalCount*Math.exp(-(i-meanGC)*(i-meanGC)/(sdGC2*2))/(Math.sqrt(2*sdGC2*Math.PI))};
            devSum+=Math.abs(dataNormal[i][1]-dataReal[i][1]);
        }
        devSum/=totalCount;
        if(devSum > 0.30) raiseError();
        else if(devSum > 0.15) raiseWarning();
        ChartSeries seriesNormal = new ChartSeries(dataNormal);
        seriesNormal.setLabel("Normal distribution");
        seriesNormal.setColor(Color.BLUE);
        
        Chart chart = new Chart();
        chart.addSeries(seriesReal);
        chart.addSeries(seriesNormal);
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Mean GC content (%)");
        options.getXAxis().setMin(0.0);
        options.getXAxis().setMax(100.0);
        options.getYAxis().setLabel("Number of sequences");
        chart.setOptions(options);
        
        ChartDataElement image = new ChartDataElement(getName()+" chart", resultsFolder, chart);
        resultsFolder.put(image);
    }

    @Override
    public String[] getReportItemNames()
    {
        return new String[] {getName()+" chart"};
    }

    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception
    {
        copyGraphs(inputReports, outputReport, "gc_per_sequence");
    }
}

