package ru.biosoft.analysis._test;

import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.HypergeometricAnalysis;
import ru.biosoft.analysis.HypergeometricAnalysisParameters;
import ru.biosoft.analysis.Util.MatrixElementsStatistics;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;

public class HypergeometricAnalysisTest extends AbstractBioUMLTest
{
    public HypergeometricAnalysisTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(HypergeometricAnalysisTest.class.getName());
        suite.addTest(new HypergeometricAnalysisTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        testStatistics();
        double accuracy = 0;

        TableDataCollection test = createTableDataCollection();

        String[] columns = TableDataCollectionUtils.parseStringToColumnNames("1-22", test);

        HypergeometricAnalysis analysis = new HypergeometricAnalysis(null, "hypergeometric");
        HypergeometricAnalysisParameters parameters = new HypergeometricAnalysisParameters();
        parameters.setBv(1.0);
        parameters.setExperimentData(new ColumnGroup(parameters, columns, DataElementPath.create( test)));
        parameters.setPvalue(2.0);
        parameters.setOutputTablePath(DataElementPath.create(OUTPUT_COLLECTION+"/test"));

        analysis.setParameters(parameters);

        TableDataCollection result = analysis.justAnalyze();

        double[] exactAnswers = new double[] { -10.850836119485804, 12.711318327838889, 0};
        double[] answers = new double[3];
        answers[0] = Double.parseDouble(TableDataCollectionUtils.getRowValues(result, "21652")[0].toString());
        answers[1] = Double.parseDouble(TableDataCollectionUtils.getRowValues(result, "754479")[0].toString());
        answers[2] = Double.parseDouble(TableDataCollectionUtils.getRowValues(result, "test")[0].toString());

        if( ( answers[0] - exactAnswers[0] ) > accuracy )
            throw new Exception("down regulated gene was found with error: " + ( answers[0] - exactAnswers[0] ));
        if( ( answers[1] - exactAnswers[1] ) > accuracy )
            throw new Exception("up regulated gene was found with error: " + ( answers[1] - exactAnswers[1] ));
        if( ( answers[2] - exactAnswers[2] ) > accuracy )
            throw new Exception("not regulated gene was found with error: " + ( answers[2] - exactAnswers[2] ));
                for( int i = 0; i < 3; i++ )
                    System.out.println(i + "=> " + answers[i]);
    }

    public static final String OUTPUT_COLLECTION = "data/Data";

    public TableDataCollection createTableDataCollection() throws Exception
    {
        String repositoryPath = "../data/test/ru/biosoft/analysis/data";
        DataCollection repo = CollectionFactory.createRepository( repositoryPath );
        assertNotNull( "Repository is null", repo );
        assertEquals( "Repo name", "data", repo.getName() );
        DataCollection collection = CollectionFactory.getDataCollection(OUTPUT_COLLECTION);
        assertNotNull( "Collection is null", collection );
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(collection, "HypergeometricAnalysisTestInput");

        for( int i = 1; i <= 22; i++ )
            table.getColumnModel().addColumn(String.valueOf(i), String.class);

        TableDataCollectionUtils.addRow(table, "21652", new Object[] {4.13, 0.25, 3.20, 2.37, 0.42, 0.44, 0.37, 0.53, 0.65, 0.32, 0.68,
                0.41, 0.67, 0.76, 0.60, 0.72, 0.53, 0.37, 0.49, 0.38, 3.35, 1.66});
        TableDataCollectionUtils.addRow(table, "754479", new Object[] {3.11, 2.72, 5.53, 5.00, 1.84, 2.89, 3.14, 5.63, 7.40, 3.47, 4.40,
                3.97, 1.15, 2.91, 4.61, 2.96, 3.67, 1.71, 3.83, 1.47, 2.67, 2.29});
        TableDataCollectionUtils.addRow(table, "test", new Object[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
        
        collection.put( table );
        return table;
    }
    
    public void testStatistics() throws Exception
    {
        double[][] matrix = new double[][]{{1,2},{},{5,6,10,0,1,2},{2,1},{0,0,0,10}};
        MatrixElementsStatistics statistics = new MatrixElementsStatistics(matrix);
        System.out.println("Done");
    }
}
