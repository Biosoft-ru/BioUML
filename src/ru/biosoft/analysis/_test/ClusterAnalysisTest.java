package ru.biosoft.analysis._test;

import java.util.HashMap;

import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.ClusterAnalysis;
import ru.biosoft.analysis.ClusterAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.columnbeans.ColumnGroup;

/**
 * cluster analysis test
 * @author axec
 *
 */
public class ClusterAnalysisTest extends AbstractBioUMLTest
{

    public static final String OUTPUT_COLLECTION = "data/Data";

    HashMap<String, String> inputToOutputMap = new HashMap<>();

    public ClusterAnalysisTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(ClusterAnalysisTest.class.getName());
        suite.addTest(new ClusterAnalysisTest("test"));
        return suite;
    }


    public void test() throws Exception
    {
        CollectionFactory.createRepository("../data");
        test("Hartigan-Wong");
        test("Forgy");
        test("Lloyd");
        test("MacQueen");
    }
    
    private TableDataCollection generateTableDataCollectionForTest() throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(DataElementPath.create(OUTPUT_COLLECTION, "table"));
        table.getColumnModel().addColumn("column1", Integer.class);
        table.getColumnModel().addColumn("column2", Integer.class);
        table.getColumnModel().addColumn("column3", Integer.class);
        table.getColumnModel().addColumn("column4", Integer.class);
        TableDataCollectionUtils.addRow(table, "row1", new Object[] {1, -1, 5, 20});
        TableDataCollectionUtils.addRow(table, "row2", new Object[] {100, -100, 104, -100});
        TableDataCollectionUtils.addRow(table, "row3", new Object[] { -9, -9, -5, -8});
        TableDataCollectionUtils.addRow(table, "row4", new Object[] {80, -80, 60, -60});
        table.getOrigin().put(table);
        return table;
    }
   
    
    
    public void test(String method) throws Exception
    {
        ClusterAnalysisParameters parameters = prepareParameters(method);
        ClusterAnalysis analysis = new ClusterAnalysis(null, "annotate");
        analysis.setParameters(parameters);
        TableDataCollection result = analysis.justAnalyze();
       
        Object[] clusters = new Object[4];
        int i = 0;
        for (String key: result.getNameList())
        {
            clusters[i] = TableDataCollectionUtils.getRowValues(result, key)[0];
            System.out.println(clusters[i]);
            i++;
        }
        
        assertEquals(clusters[0], clusters[2]);
        assertEquals(clusters[1], clusters[3]);
    }

   

    private ClusterAnalysisParameters prepareParameters(String method) throws Exception
    {
        String repositoryPath = "../data/test/ru/biosoft/analysis/data";
        CollectionFactory.createRepository(repositoryPath);
        DataCollection collection = CollectionFactory.getDataCollection(OUTPUT_COLLECTION);
        ClusterAnalysisParameters parameters = new ClusterAnalysisParameters();
        parameters.setMethod(method);
        ColumnGroup group = new ColumnGroup(parameters);
        group.setTable(generateTableDataCollectionForTest());
        group.setColumns(new Column[] {new Column(group, "column1"), new Column(group, "column2"),
                new Column(group, "column3"), new Column(group, "column4")});
        parameters.setExperimentData(group);

        parameters.setOutputTablePath(DataElementPath.create(OUTPUT_COLLECTION+"/test"));

        return parameters;
    }



}