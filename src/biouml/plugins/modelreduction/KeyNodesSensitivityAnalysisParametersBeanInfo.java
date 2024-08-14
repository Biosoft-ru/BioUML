package biouml.plugins.modelreduction;

import biouml.model.Diagram;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.columnbeans.ColumnNamesSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

public class KeyNodesSensitivityAnalysisParametersBeanInfo extends BeanInfoEx2<KeyNodesSensitivityAnalysisParameters>
{

    public KeyNodesSensitivityAnalysisParametersBeanInfo()
    {
        super(KeyNodesSensitivityAnalysisParameters.class);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        property("input").inputElement(Diagram.class).add();
        property("tableData").inputElement(TableDataCollection.class).add();
        property("keyNodes").inputElement(TableDataCollection.class).canBeNull().add();
        addWithTags("type", b->b.getAvailableTypes());      
        add(ColumnNameSelector.registerSelector("nameColumn", beanClass, "tableData"));
        add(ColumnNameSelector.registerNumericSelector("timeColumn", beanClass, "tableData"));       
        add(ColumnNamesSelector.registerNumericSelector("dataColumns", beanClass, "tableData"));
        add("absoluteTolerance");
        add("relativeTolerance");
        addWithTags("steadyStateVariables", b->b.getAvailableSteadyStateVariables());
        property("result").outputElement(TableDataCollection.class).add();
        add("engineWrapper");
    }
}
