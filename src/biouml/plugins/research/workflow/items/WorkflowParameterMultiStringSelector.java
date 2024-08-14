package biouml.plugins.research.workflow.items;

import ru.biosoft.workbench.editors.GenericMultiSelectEditor;
import biouml.model.Node;

public class WorkflowParameterMultiStringSelector extends GenericMultiSelectEditor
{
    @Override
    protected Object[] getAvailableValues()
    {
        try
        {
            Node n = ((Node)getDescriptor().getValue("node"));
            String mode = (String)getDescriptor().getValue("mode");
            return WorkflowParameter.getDropDownValues( n, mode );
        }
        catch( Exception e )
        {
        }
        return new String[0];
    }
}
