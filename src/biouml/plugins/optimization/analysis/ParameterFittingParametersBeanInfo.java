package biouml.plugins.optimization.analysis;

import biouml.model.Diagram;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
public class ParameterFittingParametersBeanInfo extends BeanInfoEx2<ParameterFittingParameters>
{
    public ParameterFittingParametersBeanInfo()
    {
        super(ParameterFittingParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("diagramPath").inputElement(Diagram.class).add();
        property("experimentPath").inputElement(TableDataCollection.class).add();
        addWithTags("regime", ParameterFittingParameters.getAvailableRegimes());
        add(ColumnNameSelector.registerNumericSelector("dataColumn", beanClass, "experimentPath"));
        add("algorithm");
        property("outputDiagram").outputElement( Diagram.class).add();
        add("engineWrapper"); 
    }
}
