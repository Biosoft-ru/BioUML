package ru.biosoft.analysis._test;

import static ru.biosoft.table.TableDataCollectionUtils.parseStringToColumnNames;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.FoldChange;
import ru.biosoft.analysis.FoldChangeParameters;
import ru.biosoft.analysis.Stat;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;

public class FoldChangeTest extends TestCase
{
    public FoldChangeTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(FoldChangeTest.class.getName());
        suite.addTest(new FoldChangeTest("testFoldChange"));
        return suite;
    }

    private static DataCollection collection;
    private static String experimentName = "foldchangeTestExperiment";
    private static String controlName = "foldChangeTestControl";
    private static String resultName = "foldChangeTestResult";
    private static Double[] experiment = new Double[] {1090.09, 9991.88, 3451.3452, 0.001};
    private static Double[] control = new Double[] {90.0, 10000.0, 2.0, 21.0, 10.0};

    private static double accuracy = 1;

    public static void testFoldChange() throws Exception
    {
        initTestCollection();

        double[] result = test("Average all", "non-logarithmic", "log10", "1-4", "1-5");
        for( int i = 0; i < result.length; i++ )
            testEquality("Average all, log10", result[i], Math.log10(Stat.mean(experiment) / Stat.mean(control)), accuracy);

        result = test("Average Experiment", "non-logarithmic", "non-logarithmic", "1-4", "1-5");
        for( int i = 0; i < result.length; i++ )
            testEquality("Average Experiment", result[i], Stat.mean(experiment) / control[i], accuracy);

        result = test("Average Control", "non-logarithmic", "non-logarithmic", "1-4", "1-5");
        for( int i = 1; i < result.length; i++ )
            testEquality("Average Control", result[i], experiment[i] / Stat.mean(control), accuracy);

        result = test("One experiment to one control", "non-logarithmic", "log10", "1-4", "1-3,5");
        for( int i = 0; i < 3; i++ )
            assertEquals("One experiment to one control, log10", result[i], Math.log10(experiment[i] / control[i]), accuracy);
        testEquality("One experiment to one control, log10", result[3], Math.log10(experiment[3] / control[4]), accuracy);

        result = test("Each experiment to each control", "non-logarithmic", "non-logarithmic", "1-2", "1-3");
        for( int i = 0; i < 2; i++ )
            for( int j = 0; j < 3; j++ )
                testEquality("Each experiment to each control", result[i + j * 2], experiment[i] / control[j], accuracy);

        result = test("One experiment to one control", "log10", "non-logarithmic", "1-4", "2-5");
        for( int i = 0; i < result.length; i++ )
            testEquality("One experiment to one control, input log10", result[i], ( Math.pow(10,experiment[i]) / Math.pow(10,control[i + 1]) ), accuracy);

        deleteTables();
    }

    private static void testEquality(String str, double actual, double expected, double accuracy)
    {
        if(Double.isNaN(actual) && Double.isNaN(expected)) return;
        assertEquals(str, expected, actual, accuracy * Math.abs(expected));
    }

    public static double[] test(String type, String inputBase, String outputBase, String experiment, String control) throws Exception
    {
        initTestCollection();

        FoldChange analysis = new FoldChange(null, "");
        FoldChangeParameters parameters = new FoldChangeParameters();
        parameters.setType(type);
        parameters.setExpertMode(true);
        parameters.setInputLogarithmBase(inputBase);
        parameters.setOutputLogarithmBase(outputBase);

        TableDataCollection experimentTable = createExperiment();
        TableDataCollection controlTable = createControl();

        String[] experimentNames = parseStringToColumnNames(experiment, experimentTable);

        String[] controlNames = parseStringToColumnNames(control, controlTable);

        parameters.setExperimentData(new ColumnGroup(parameters, experimentNames, DataElementPath.create(experimentTable)));
        parameters.setControlData(new ColumnGroup(parameters, controlNames, DataElementPath.create(controlTable)));
        parameters.setOutputTablePath(DataElementPath.create(collection, resultName));
        analysis.setParameters(parameters);
        TableDataCollection result = analysis.justAnalyze();

        return TableDataCollectionUtils.getDoubleRow(result, "row");
    }

    public static void initTestCollection() throws Exception
    {
        String repositoryPath = "../data/test/ru/biosoft/analysis/data";
        CollectionFactory.createRepository(repositoryPath);
        collection = CollectionFactory.getDataCollection("data/Data");
    }


    public static TableDataCollection createExperiment() throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(collection, experimentName);
        for( int i = 0; i < experiment.length; i++ )
            table.getColumnModel().addColumn(String.valueOf(i), Double.class);
        Double[] row = new Double[experiment.length];
        System.arraycopy(experiment, 0, row, 0, experiment.length);
        TableDataCollectionUtils.addRow(table, "row", row);
        collection.put(table);
        return table;
    }

    public static TableDataCollection createControl() throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(collection, controlName);

        for( int i = 0; i < control.length; i++ )
            table.getColumnModel().addColumn(String.valueOf(i), Double.class);
        Double[] row = new Double[control.length];
        System.arraycopy(control, 0, row, 0, control.length);
        TableDataCollectionUtils.addRow(table, "row", row);
        collection.put(table);
        return table;
    }

    public static void deleteTables() throws Exception
    {
        collection.remove(experimentName);
        collection.remove(controlName);
        collection.remove(resultName);
    }

}