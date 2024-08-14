package biouml.plugins.modelreduction._test;

import java.util.HashMap;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.modelreduction.KeyNodesSensitivityAnalysis;
import biouml.plugins.modelreduction.KeyNodesSensitivityAnalysisParameters;
import junit.framework.TestSuite;
import one.util.streamex.EntryStream;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;

public class TestKeyNodesSensitivity extends AbstractBioUMLTest
{
    public TestKeyNodesSensitivity(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestKeyNodesSensitivity.class.getName());
        suite.addTest(new TestKeyNodesSensitivity("test"));
        return suite;
    }

    private final static double ERROR = 1E-5;

    public void test() throws Exception
    {
        TableDataCollection result = new StandardTableDataCollection(null, "result");
        TableDataCollection table = getTable();
        Diagram diagram = getDiagram("test");
        KeyNodesSensitivityAnalysis analysis = new KeyNodesSensitivityAnalysis(null, "");
        analysis.setDebug(true);
        analysis.getParameters().setTable(table);
        analysis.getParameters().setInput(DataElementPath.create(diagram));
        analysis.getParameters().setType(KeyNodesSensitivityAnalysisParameters.TYPE_STEADY_STATE);
        analysis.getParameters().setNameColumn("name");
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime(100000);
        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(1);
        analysis.justAnalyze(result);
        Map<String, Double> calculated = new HashMap<>();
        result.stream().forEach(row -> calculated.put(row.getName(), Double.parseDouble(row.getValueAsString("Score"))));
        out(calculated);
        assertEquals(getResult(), calculated, ERROR);
    }

    protected Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository("../data");
        DataCollection<?> db = CollectionFactory.getDataCollection("databases/SBML modules");
        DataCollection<Diagram> diagrams = ( (Module)db ).getDiagrams();
        return diagrams.get(name);
    }

    protected TableDataCollection getTable() throws Exception
    {
        TableDataCollection data = new StandardTableDataCollection(null, "table");
        data.getColumnModel().addColumn("name", DataType.Text);
        RowDataElement r1 = new RowDataElement("r1", data);
        r1.setValues(new String[] {"f"});
        data.addRow(r1);
        RowDataElement r2 = new RowDataElement("r2", data);
        r2.setValues(new String[] {"d"});
        data.addRow(r2);
        RowDataElement r3 = new RowDataElement("r3", data);
        r3.setValues(new String[] {"e"});
        data.addRow(r3);
        return data;
    }

    protected void out(Map<String, Double> map)
    {
        EntryStream.of(map).forEach(e -> System.out.println(e.getKey() + " : " + e.getValue()));
    }

    protected void assertEquals(Map<String, Double> expected, Map<String, Double> calculated, double error)
    {
        EntryStream.of(calculated).forEach(e -> assertEquals(expected.get(e.getKey()) , e.getValue(), error));
    }

    protected Map<String, Double> getResult()
    {
        Map<String, Double> result = new HashMap<>();
        result.put("a", 2.369485);
        result.put("b", 0.588235);
        result.put("c", 1.78125);
        result.put("g", 0.58823);
        result.put("h", 0.63051);
        return result;
    }
}

