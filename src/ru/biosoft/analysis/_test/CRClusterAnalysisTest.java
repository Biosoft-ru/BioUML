package ru.biosoft.analysis._test;

import java.util.HashMap;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.analysis.ClusterAnalysis;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * cluster analysis test
 * @author axec
 *
 */
public class CRClusterAnalysisTest extends TestCase
{

    HashMap<String, String> inputToOutputMap = new HashMap<>();

    public CRClusterAnalysisTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(CRClusterAnalysisTest.class.getName());
        suite.addTest(new CRClusterAnalysisTest("test"));
        return suite;
    }


    public void test() throws Exception
    {

    }
 
    
    public void test(String method) throws Exception
    {
//        ClusterAnalysisParameters parameters = prepareParameters(method);
        ClusterAnalysis analysis = new ClusterAnalysis(null, "annotate");
//        analysis.setParameters(parameters);
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

   

//    private ClusterAnalysisParameters prepareParameters(String method) throws Exception
//    {
////        String repositoryPath = "../data_resources";
////        DataCollection repository = CollectionFactory.createRepository(repositoryPath);
////        TransformedDataCollection microarray = (TransformedDataCollection)CollectionFactory
////        .getDataElement("data/microarray/");
////        DataCollection collection = CollectionFactory.getDataElement("test.fac", microarray);
////        ClusterAnalysisParameters parameters = new ClusterAnalysisParameters();
////        parameters.setMethod(method);
////        ColumnsFromTable group = new ColumnsFromTable(parameters, "columns");
////        group.setTable(generateTableDataCollectionForTest());
////        group.setColumns(new ColumnWithNewName[] {new ColumnWithNewName(group, "column1"), new ColumnWithNewName(group, "column2"),
////                new ColumnWithNewName(group, "column3"), new ColumnWithNewName(group, "column4")});
////        parameters.setColumnsGroup(group);
//
//        parameters.setOutputCollection(collection);
//        parameters.setOutputName("test");
//
//        return parameters;
//    }



}