
package biouml.plugins.research.workflow.engine;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import biouml.standard.simulation.ScriptDataGenerator;
import biouml.standard.simulation.SimulationDataGenerator;
import biouml.standard.simulation.SimulationResult;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 * @author anna
 *
 */
public class DataGeneratorElement extends WorkflowElement implements PropertyChangeListener
{
    private SimulationDataGenerator dataGenerator = null;
    private String variables;
    /**
     * @param statusProperty
     */
    public DataGeneratorElement(String name, DynamicPropertySet attr, DynamicProperty statusProperty)
    {
        super(statusProperty);
        if( attr.getProperty(ScriptDataGenerator.SCRIPT_PROPERTY) != null )
        {
            dataGenerator = new ScriptDataGenerator(null, name, attr.getValue(ScriptDataGenerator.SCRIPT_PROPERTY).toString());
            Object parameters = attr.getValue(ScriptDataGenerator.PARAMETERS_PROPERTY);
            if( parameters instanceof String[] )
            {
                for( String paramStr : (String[])parameters )
                {
                    int index = paramStr.indexOf(";");
                    if( index > -1 )
                    {
                        String paramName = paramStr.substring(0, index);
                        ( (ScriptDataGenerator)dataGenerator ).addParameter(paramName, paramStr.substring(index));
                    }
                }
            }
            variables = (String)attr.getValue(ScriptDataGenerator.VARIABLES_PROPERTY);
        }
    }

    @Override
    public boolean isComplete()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void startElementExecution(JobControlListener listener)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        Object source = evt.getSource();
        if( source instanceof SimulationResult )
        {
            if( evt.getPropertyName().equals("start") )
            {
                SimulationResult result = (SimulationResult)source;
                try
                {
                    ( (ScriptDataGenerator)dataGenerator ).addVariablesFromString(variables, result);
                }
                catch( Exception e )
                {
                }
            }
        }
        //propagate propertyChange to other listeners
        for( WorkflowElement pcl : listeners )
        {
            if( pcl instanceof PropertyChangeListener )
                ( (PropertyChangeListener)pcl ).propertyChange(new PropertyChangeEvent(dataGenerator, evt.getPropertyName(), evt
                        .getOldValue(), evt.getNewValue()));
        }
    }

}
