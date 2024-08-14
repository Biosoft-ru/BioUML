package biouml.plugins.modelreduction;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;
import biouml.plugins.simulation.SimulationEngine;

import com.developmentontheedge.beans.BeanInfoConstants;

public class SteadyStateTaskParametersBeanInfo extends BeanInfoEx2<SteadyStateTaskParameters>
{
    public SteadyStateTaskParametersBeanInfo()
    {
        super(SteadyStateTaskParameters.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("absoluteTolerance").numberFormat( BeanInfoConstants.NUMBER_FORMAT_NONE ).add();
        property("relativeTolerance").numberFormat( BeanInfoConstants.NUMBER_FORMAT_NONE ).add();
        property("validationSize").add();
        property("startSearchTime").numberFormat( BeanInfoConstants.NUMBER_FORMAT_NONE ).add();
        property("variableNames").canBeNull().editor( VariableEditor.class ).add();
        property("engineWrapper").add();
    }

    public static class VariableEditor extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            SimulationEngine engine = ( (SteadyStateTaskParameters)getBean() ).getSimulationEngine();
            if( engine != null && engine.getDiagram() != null && engine.getVariableNames() != null )
                return engine.getVariableNames();
            else
                return new String[0];
        }
    }
}
