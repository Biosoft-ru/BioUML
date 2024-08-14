package ru.biosoft.bsa._test;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import junit.framework.Assert;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartSeries;

/**
 * @author lan
 *
 */
public class BSATestUtils
{
    public static final String bsaRepositoryPath = "../data/test/ru/biosoft/bsa";
    
    public static DataCollection createRepository() throws Exception
    {
        String path = BSATestUtils.bsaRepositoryPath + "/" + DataCollectionConfigConstants.DEFAULT_CONFIG_FILE;
        File f = new File(path);
        Assert.assertTrue("Can not find export repository config path, absolute path=" + f.getAbsolutePath(), f.exists());
        DataCollection root = CollectionFactory.createRepository(bsaRepositoryPath);
        Assert.assertNotNull("Can not load repository" + f.getAbsolutePath(), root);
        return root;
    }

    /**
     * Tests whether ROC curves in supplied chart conform some basic rules:
     * Chart is not null and contains at least one series
     * In each series data points are located from [0,0] to [1,1] and not decreasing
     * @param rocCurves
     */
    public static void checkROCCurves(Chart rocCurves)
    {
        Assert.assertNotNull(rocCurves);
        Assert.assertTrue(rocCurves.getSeriesCount() > 0);
        for(ChartSeries series: rocCurves)
        {
            checkROCCurve(series);
        }
    }

    public static void checkROCCurve(ChartSeries series)
    {
        double[][] data = series.getData();
        for(double[] point: data)
        {
            Assert.assertTrue(series.getLabel()+":"+point[0]+","+point[1], point[0] >= 0 && point[0] <= 1.000001 && point[1] >= 0 && point[1] <= 1.000001);
        }
        List<double[]> list = StreamEx.of( data )
                .sorted( Comparator.<double[]> comparingDouble( d -> d[0] ).thenComparingDouble( d -> d[1] ) ).toList();
        Assert.assertTrue(series.getLabel(), list.get(0)[0] == 0.0 || list.get(0)[1] == 0.0);
        Assert.assertTrue(series.getLabel(), Math.abs(list.get(list.size()-1)[0]-1.0)<0.000001 || Math.abs(list.get(list.size()-1)[1]-1.0)<0.000001);
        
        for(int i=1; i<list.size(); i++)
        {
            double[] prev = list.get(i-1);
            double[] cur = list.get(i);
            Assert.assertTrue(series.getLabel()+":"+prev[0]+","+prev[1]+"/"+cur[0]+","+cur[1], prev[1]<=cur[1]);
        }
    }
}
