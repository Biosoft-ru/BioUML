package biouml.standard.diagram;

import java.awt.Point;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.dynamics.Transition;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class TransitionPane extends SemanticRelationPane
{
    protected static final Logger log = Logger.getLogger(TransitionPane.class.getName());

    public TransitionPane(Module module, ViewEditorPane viewEditor)
    {
        super(module, viewEditor);
    }

    @Override
    protected boolean createRelation()
    {
        //method stub
        return true;
    }

    @Override
    protected void createEdge()
    {
        try
        {
            Node inNode = (Node)inSelector.getDiagramElement();
            Node outNode = (Node)outSelector.getDiagramElement();

            String edgeName = inNode.getName() + " -> " + outNode.getName();
            Edge edge = new Edge(new Stub(null, edgeName, Type.MATH_TRANSITION), inNode, outNode);
            edge.setRole(new Transition(edge));
            viewEditor.add(edge, new Point(0, 0));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not create Edge for transition", t);
        }
    }
}
