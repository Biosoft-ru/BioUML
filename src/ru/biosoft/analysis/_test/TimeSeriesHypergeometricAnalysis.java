package ru.biosoft.analysis._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.HypergeometricAnalysis;
import ru.biosoft.analysis.HypergeometricAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;

public class TimeSeriesHypergeometricAnalysis extends TestCase
{
    private String repositoryPath = "../data_resources";
    private String collectionPath = "data/Collaboration/Ilya/Data/Tables/";
    
    private String tableName = "133A2.0_Selivanova_MCF7";
    
    private String[] control = {"Control_1_12h", "Control_2_12h", "Control_3_12h"};
    
    String[] experimentColumn1 = new String[] {"1uM_RITA_2h", "1uM_RITA_4h", "1uM_RITA_6h", "1uM_RITA_8h", "1uM_RITA_10h", "1uM_RITA_12h",
            "1uM_RITA_16h", "1uM_RITA_20h", "1uM_RITA_24h"};

    String[] experimentColumn2 = new String[] {"0.1uM_RITA_2h", "0.1uM_RITA_4h", "0.1uM_RITA_6h", "0.1uM_RITA_8h", "0.1uM_RITA_10h",
            "0.1uM_RITA_12h", "0.1uM_RITA_16h", "0.1uM_RITA_20h", "0.1uM_RITA_24h"};

    String[] experimentColumn3 = new String[] {"10uM_Nutlin_2h", "10uM_Nutlin_4h", "10uM_Nutlin_6h", "10uM_Nutlin_8h", "10uM_Nutlin_10h",
            "10uM_Nutlin_12h", "10uM_Nutlin_16h", "10uM_Nutlin_20h", "10uM_Nutlin_24h"};

    double pvalue = 0.001;

    public TimeSeriesHypergeometricAnalysis(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TimeSeriesHypergeometricAnalysis.class.getName());

        suite.addTest(new TimeSeriesHypergeometricAnalysis("test"));

        return suite;
    }

    public void test() throws Exception
    {
        
        CollectionFactory.createRepository(repositoryPath);
        DataCollection collection = CollectionFactory.getDataCollection(collectionPath);

        TableDataCollection inputTable = (TableDataCollection)collection.get(tableName);

        collection.put(compareWithControl(inputTable, control, experimentColumn1, "1uM_RITA_Dysregulated", pvalue));
        collection.put(compareWithControl(inputTable, control, experimentColumn2, "0.1uM_RITA_Dysregulated", pvalue));
        collection.put(compareWithControl(inputTable, control, experimentColumn3, "10uM_Nutlin_Dysregulated", pvalue));
    }

    public TableDataCollection compareWithControl(TableDataCollection inputTable, String[] control, String[] experimentSeries, String name,
            double pvalue) throws Exception
    {

        HypergeometricAnalysis analysis = new HypergeometricAnalysis(null, "");
        HypergeometricAnalysisParameters parameters = new HypergeometricAnalysisParameters();
        parameters.setOutputTablePath(DataElementPath.create(collectionPath + name + "_" + pvalue));
        parameters.setPvalue(pvalue);

        DataElementPath path = DataElementPath.create(inputTable);
        parameters.setControlData(new ColumnGroup(parameters, control, path));

        String[] columns1 = TableDataCollectionUtils.parseStringToColumnNames(experimentSeries[0], inputTable);
        parameters.setExperimentData(new ColumnGroup(parameters, columns1, path));
        analysis.setParameters(parameters);
        TableDataCollection t1 = analysis.justAnalyze();

        for( int i = 1; i < experimentSeries.length; i++ )
        {
            columns1 = TableDataCollectionUtils.parseStringToColumnNames(experimentSeries[i], inputTable);
            parameters.setExperimentData(new ColumnGroup(parameters, columns1, path));
            analysis.setParameters(parameters);

            t1 = TableDataCollectionUtils.join(TableDataCollectionUtils.OUTER_JOIN, t1, analysis.justAnalyze(), DataElementPath.create(t1),
                    TableDataCollectionUtils.getColumnNames(t1), new String[] {"Score"}, TableDataCollectionUtils.getColumnNames(t1),
                    new String[] {"Score_" + i});
        }
        return t1;
    }
}
