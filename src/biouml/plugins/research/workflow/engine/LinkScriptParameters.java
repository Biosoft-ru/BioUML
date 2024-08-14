package biouml.plugins.research.workflow.engine;

import ru.biosoft.access.core.DataElementPath;
import biouml.model.Node;

public class LinkScriptParameters
{
    private Node node;

    public LinkScriptParameters()
    {
        this(null);
    }
    
    public LinkScriptParameters(Node n)
    {
        this.node = n;
    }
    
    public DataElementPath getScriptPath()
    {
        return DataElementPath.create(node.getAttributes().getValue(ScriptElement.SCRIPT_PATH).toString());
    }
    
    public void setScriptPath(DataElementPath scriptPath)
    {
        node.getAttributes().setValue(ScriptElement.SCRIPT_PATH, scriptPath.toString());
    }
}