package biouml.plugins.research.workflow.engine;

import java.awt.Point;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.plugins.research.BaseResearchSemanticController;

public class TextScriptParameters implements InitialElementProperties
{
    private Node node;

    public TextScriptParameters()
    {
        this(null);
    }

    public TextScriptParameters(Node n)
    {
        this.node = n;
    }

    public String getScript()
    {
        return (String)node.getAttributes().getValue(ScriptElement.SCRIPT_SOURCE);
    }

    public void setScript(String script)
    {
        node.getAttributes().setValue(ScriptElement.SCRIPT_SOURCE, script);
    }

    public Node getNode()
    {
        return node;
    }

    public String getScriptType()
    {
        return (String)node.getAttributes().getValue( ScriptElement.SCRIPT_TYPE );
    }

    public void setScriptType(String scriptType)
    {
        node.getAttributes().setValue( ScriptElement.SCRIPT_TYPE, scriptType );
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        BaseResearchSemanticController semanticController = (BaseResearchSemanticController)Diagram.getDiagram(parent).getType().getSemanticController();
        Compartment node = semanticController.createScriptNode(parent, getScript(), getScriptType());
        if( node != null && semanticController.canAccept(parent, node) )
        {
            viewPane.add(node, location);
        }
        return new DiagramElementGroup( node );
    }
}