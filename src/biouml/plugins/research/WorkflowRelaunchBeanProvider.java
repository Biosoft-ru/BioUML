package biouml.plugins.research;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Diagram;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class WorkflowRelaunchBeanProvider implements BeanProvider
{

    @Override
    public Object getBean(String path)
    {
        try
        {
            DataCollection<?> dc = CollectionFactory.getDataCollection( path );
            if(dc == null) return null;
            Diagram diagram = (Diagram)CollectionFactory.getDataCollection(dc.getInfo().getProperty("workflow_path"));
            DynamicPropertySet workflowParameters = WorkflowItemFactory.getWorkflowParameters(diagram);
            String properties = dc.getInfo().getProperty("workflow_properties");
            if(properties == null) return null;
            DynamicPropertySet dps = TextUtil.readDPSFromJSON(properties);
            BeanUtil.copyBean(dps, workflowParameters);
            return workflowParameters;
        }
        catch( Exception e )
        {
        }
        return null;
    }

}
