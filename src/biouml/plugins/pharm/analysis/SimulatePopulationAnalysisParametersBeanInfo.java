package biouml.plugins.pharm.analysis;

import biouml.model.Diagram;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class SimulatePopulationAnalysisParametersBeanInfo extends BeanInfoEx2<SimulatePopulationAnalysisParameters>
{
    public SimulatePopulationAnalysisParametersBeanInfo()
    {
        super( SimulatePopulationAnalysisParameters.class );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("inputDiagramPath", beanClass, Diagram.class));
        add(DataElementPathEditor.registerInput("inputTablePath", beanClass, TableDataCollection.class));
        add("observedVariables", VariableEditor.class);
        add("estimatedVariables", VariableEditor.class);
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class),
                "$inputTablePath/parent/inputTablePath$ simulated"));
    }
    
    public static class VariableEditor extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ((SimulatePopulationAnalysisParameters)getBean()).getAvailableVariables().toArray(String[]::new);
        }
    }
}
