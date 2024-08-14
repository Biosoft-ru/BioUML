package ru.biosoft.bsastats.processors;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.IntArray;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("Quality per base")
@PropertyDescription("Distribution of phred quality score along the bases")
public class QualityPerBaseProcessor extends AbstractStatisticsProcessor
{
    private static final int[] levels = new int[] {20,28,30};
    private List<int[]> positionToQuality = new ArrayList<>();
    private int maxQuality = 0;
    private boolean valid = true;
    
    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        if(!valid ) return;
        if(qualities == null)
        {
            log.warning("No phred quality information: Quality per base processor is disabled");
            valid = false;
            return;
        }
        while(qualities.length>positionToQuality.size())
        {
            positionToQuality.add(new int[255]);
        }
        
        for(int i=0; i<qualities.length; i++)
        {
            maxQuality = Math.max(maxQuality, qualities[i]);
            positionToQuality.get(i)[qualities[i]]++;
        }
    }
    
    private static class MyBoxAndWhiskerXYDataset extends DefaultBoxAndWhiskerXYDataset
    {
        private static final long serialVersionUID = 1L;
        private IntArray positions = new IntArray();
        private static final Date STUB_DATE = new Date(0);

        public MyBoxAndWhiskerXYDataset(Comparable<?> seriesKey)
        {
            super(seriesKey);
        }
        
        public void add(int x, double mean, double q1, double q3, double minRegular, double maxRegular)
        {
            BoxAndWhiskerItem item = new BoxAndWhiskerItem(null, mean, q1, q3, minRegular, maxRegular, minRegular, maxRegular, Collections.EMPTY_LIST);
            positions.add(x);
            add(STUB_DATE, item);
        }

        @Override
        public Number getX(int series, int item)
        {
            return positions.get(item);
        }
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        if(!valid) return;
        int maxPos = positionToQuality.size();
        double[] means = new double[maxPos];
        int[] quantiles = new int [] {10,25,50,75,90};
        int[][] quantValues = new int[maxPos][];
        
        int max90PercentQuantileValue = 0;
        
        for(int i=0; i<maxPos; i++)
        {
            int[] qualities = positionToQuality.get(i);
            int total = 0;
            long sumQualities = 0;
            for(int quality = 0; quality < 255; quality++)
            {
                total+=qualities[quality];
                sumQualities += quality*((long)qualities[quality]);
            }
            means[i] = ((double)sumQualities)/total;
            quantValues[i] = new int[quantiles.length];
            int subTotal = 0;
            int curQuantile = 0;
            for(int quality = 0; quality < 255; quality++)
            {
                subTotal+=qualities[quality];
                while(subTotal*100.0/total>quantiles[curQuantile])
                {
                    quantValues[i][curQuantile++] = quality;
                    if(curQuantile==quantiles.length) break;
                }
                if(curQuantile==quantiles.length) break;
            }
            if(max90PercentQuantileValue < quantValues[i][4]) max90PercentQuantileValue = quantValues[i][4];
        }
        
        XYSeries meanSeries = new XYSeries("Mean value");
        for(int i=0; i<maxPos; i++)
        {
            meanSeries.add(i+1, means[i]);
        }
        XYSeriesCollection meanDataset = new XYSeriesCollection(meanSeries);
        JFreeChart chart = ChartFactory.createXYLineChart("", "", "", meanDataset, PlotOrientation.VERTICAL, false, true,
                false);
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot xyPlot = chart.getXYPlot();
        xyPlot.setBackgroundPaint(Color.WHITE);
        NumberAxis xAxis = new NumberAxis("Position in read (bp)");
        xAxis.setRange(0.5, maxPos+0.5);
        xyPlot.setDomainAxis(xAxis);
        xyPlot.setDomainGridlinePaint(Color.GRAY);
        xyPlot.setRangeGridlinePaint(Color.GRAY);
        NumberAxis yAxis = new NumberAxis("Quality");
        int upperBound = max90PercentQuantileValue < levels[2] ? levels[2] : max90PercentQuantileValue;
        yAxis.setRange(0, upperBound + 5);
        xyPlot.setRangeAxis(yAxis);
        xyPlot.getRenderer(0).setSeriesPaint(0, Color.BLUE);
        
        MyBoxAndWhiskerXYDataset boxAndWhisker = new MyBoxAndWhiskerXYDataset("10% - 25% - 50% - 75% - 90%");
        for(int i=0; i<maxPos; i++)
        {
            boxAndWhisker.add(i+1, quantValues[i][2], quantValues[i][1], quantValues[i][3], quantValues[i][0], quantValues[i][4]);
        }
        xyPlot.setDataset(1, boxAndWhisker);
        XYBoxAndWhiskerRenderer boxAndWhiskerRenderer = new XYBoxAndWhiskerRenderer();
        boxAndWhiskerRenderer.setDefaultOutlinePaint(Color.GRAY);
        xyPlot.setRenderer(1, boxAndWhiskerRenderer);

        createBackground(xyPlot);
        BufferedImage baseChart = chart.createBufferedImage((int) ( 100*Math.sqrt(maxPos) ), 400);
        BufferedImage legend = ImageIO.read(QualityPerBaseProcessor.class.getResourceAsStream("resources/box and whisker legend.gif"));
        BufferedImage resultingImage = new BufferedImage(Math.max(baseChart.getWidth(), legend.getWidth()), baseChart.getHeight()+legend.getHeight(), baseChart.getType());
        resultingImage.getGraphics().drawImage(baseChart, 0, 0, Color.WHITE, null);
        resultingImage.getGraphics().drawImage(legend, 0, baseChart.getHeight(), Color.WHITE, null);
        ImageDataElement image = new ImageDataElement(getName()+" chart", resultsFolder, resultingImage);
        resultsFolder.put(image);

        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(resultsFolder, getName());
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        table.getColumnModel().addColumn("Mean value", Double.class);
        for(int quantile: quantiles)
        {
            table.getColumnModel().addColumn(quantile+" %", Integer.class);
        }
        for(int i=0; i<maxPos; i++)
        {
            Object[] values = new Object[quantiles.length+1];
            values[0] = means[i];
            for(int quantile = 0; quantile < quantiles.length; quantile++)
            {
                values[quantile+1] = quantValues[i][quantile];
            }
            if(quantValues[i][1] < 10 || quantValues[i][2] < 25) raiseWarning();
            if(quantValues[i][1] < 5 || quantValues[i][2] < 20) raiseError();
            TableDataCollectionUtils.addRow(table, String.valueOf(i+1), values, true);
        }
        table.finalizeAddition();
        
        resultsFolder.put(table);
    }

    protected void createBackground(XYPlot xyPlot)
    {
        int maxPos = positionToQuality.size();
        XYSeries redArea = new XYSeries("Poor quality");
        redArea.add(0.5, levels[0]);
        redArea.add(maxPos+0.5, levels[0]);
        xyPlot.setDataset(2, new XYSeriesCollection(redArea));
        XYSeries orangeArea = new XYSeries("Reasonable quality");
        orangeArea.add(0.5, levels[1]);
        orangeArea.add(maxPos+0.5, levels[1]);
        xyPlot.setDataset(3, new XYSeriesCollection(orangeArea));
        XYSeries greenArea = new XYSeries("Good quality");
        greenArea.add(0.5, maxQuality);
        greenArea.add(maxPos+0.5, maxQuality);
        xyPlot.setDataset(4, new XYSeriesCollection(greenArea));
        XYStepAreaRenderer backgroundRenderer = new XYStepAreaRenderer();
        backgroundRenderer.setSeriesPaint(0, new Color(255,200,200,128));
        xyPlot.setRenderer(2, backgroundRenderer);
        backgroundRenderer = new XYStepAreaRenderer();
        backgroundRenderer.setSeriesPaint(0, new Color(240,230,150,128));
        backgroundRenderer.setRangeBase(levels[0]);
        xyPlot.setRenderer(3, backgroundRenderer);
        backgroundRenderer = new XYStepAreaRenderer();
        backgroundRenderer.setSeriesPaint(0, new Color(200,240,200,128));
        backgroundRenderer.setRangeBase(levels[1]);
        xyPlot.setRenderer(4, backgroundRenderer);
    }

    @Override
    public String[] getReportItemNames()
    {
        if(!valid) return null;
        return new String[] {getName()+" chart"};
    }

    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception
    {
        copyGraphs(inputReports, outputReport, "quality_per_base");
    }
}
