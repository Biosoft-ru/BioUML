package biouml.plugins.research.workflow.items;

import biouml.model.Node;

import com.developmentontheedge.beans.editors.StringTagEditor;

/**
 * @author lan
 *
 */
public class WorkflowParameterAutoSelector extends StringTagEditor
{
    private static final String[] DEFAULT_OPTIONS = new String[] {"(no options available)"};
    
    @Override
    public String[] getTags()
    {
        String[] result = DEFAULT_OPTIONS;
        try
        {
            Node n = ((Node)getDescriptor().getValue("node"));
            String mode = (String)getDescriptor().getValue("mode");
            result = WorkflowParameter.getDropDownValues( n, mode );
        }
        catch( Exception e )
        {
        }
        return result;
    }
}
