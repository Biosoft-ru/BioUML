package biouml.plugins.pharm.analysis;

import biouml.model.Diagram;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;


public class PopulationSamplingParametersBeanInfo extends BeanInfoEx2<PopulationSamplingParameters>
{
    public PopulationSamplingParametersBeanInfo()
    {
        super( PopulationSamplingParameters.class );
    }
    
    @Override
    public void initProperties() throws Exception
    {    
        add(DataElementPathEditor.registerInput("diagramPath", beanClass, Diagram.class));
        add(DataElementPathEditor.registerInput("experimentalDataPath", beanClass, TableDataCollection.class));
        add(DataElementPathEditor.registerInput("initialDataPath", beanClass, TableDataCollection.class));
        property( "resultPath" ).outputElement(TableDataCollection.class ).add();
        add("populationSize");
        add("preliminarySteps");
        add("acceptanceRate");
        add("seed");
        addHidden("observedVariables", VariableEditor.class, "isDiagramNotSet");
        addHidden("estimatedVariables", VariableEditor.class, "isDiagramNotSet");
        add("engineWrapper");      
    }
    
    public static class VariableEditor extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ((PopulationSamplingParameters)getBean()).getAvailableVariables().toArray(String[]::new);
        }
    }
}
