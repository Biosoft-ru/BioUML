package biouml.standard.diagram;

import java.awt.Point;

import javax.swing.JOptionPane;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.standard.type.Stub;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class SimpleEdgePane extends SemanticRelationPane
{
    protected static final Logger log = Logger.getLogger(SimpleEdgePane.class.getName());

    protected Stub edgeStub;

    protected String edgeName;
    protected String edgeType;
    protected DynamicPropertySet initAttributes;
    protected Edge newEdge;

    public SimpleEdgePane(Module module, ViewEditorPane viewEditor, String edgeName, String edgeType, DynamicPropertySet initAttributes)
    {
        super(module, viewEditor);
        this.edgeName = edgeName;
        this.edgeType = edgeType;
        this.initAttributes = initAttributes;
    }

    @Override
    protected boolean createRelation()
    {
        try
        {
            Stub edgeStub = new Stub(null, edgeName, edgeType);

            this.edgeStub = edgeStub;
            return true;
        }
        catch( Throwable t )
        {
            JOptionPane.showMessageDialog(this, "Exception: " + t, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Creates Edge for the simple edge and adds it on the diagram. */
    @Override
    protected void createEdge()
    {
        try
        {
            Node inNode = (Node)inSelector.getDiagramElement();
            Node outNode = (Node)outSelector.getDiagramElement();

            Compartment origin = Node.findCommonOrigin(inNode, outNode);
            newEdge = new Edge(origin, edgeStub.getName(), edgeStub, inNode, outNode);

            if( initAttributes != null )
            {
                for(DynamicProperty property: initAttributes)
                {
                    newEdge.getAttributes().add(property);
                }
            }

            Diagram diagram = Diagram.getDiagram(origin);
            SemanticController semanticController = diagram.getType().getSemanticController();
            if( semanticController.canAccept(origin, newEdge) )
            {
                if( semanticController instanceof XmlDiagramSemanticController )
                {
                    Edge validatedEdge = (Edge) ( (XmlDiagramSemanticController)semanticController ).getPrototype().validate(origin,
                            newEdge);
                    viewEditor.add(validatedEdge, new Point(0, 0));
                }
                else
                {
                    viewEditor.add(newEdge, new Point(0, 0));
                }
            }
            else
            {
                JOptionPane.showMessageDialog(null, "can't create edge with '" + inNode.getKernel().getType() + "' input and '"
                        + outNode.getKernel().getType() + "' output");
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not create Edge for relation '" + edgeStub.getName() + "'", t);
        }
    }

    @Override
    public void release()
    {
        viewEditor.setSelectionEnabled(true);
        viewEditor.removeViewPaneListener(adapter);
    }

    @Override
    public void okPressed()
    {
        createEdge();
        release();
    }

    @Override
    protected void cancelPressed()
    {
        release();
    }
}
