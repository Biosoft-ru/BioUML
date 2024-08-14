package biouml.standard.diagram;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

@PropertyName("Composite model")
@PropertyDescription("Hierarchic model which can contain ODE (with events) models as interconnected parts. Simulation is conducted by \"flattening\" model to ODE.")
public class CompositeDiagramType extends PathwaySimulationDiagramType
{

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {SubDiagram.class, Type.TYPE_BLOCK, Stub.Note.class, Event.class, Equation.class, Function.class,
                Stub.OutputConnectionPort.class, Stub.InputConnectionPort.class, Stub.ContactConnectionPort.class, Stub.Bus.class,
                Stub.SwitchElement.class, Stub.Constant.class, Type.TYPE_TABLE};
    }

    @Override
    public Class[] getEdgeTypes()
    {
        return new Class[] {Stub.DirectedConnection.class, Stub.UndirectedConnection.class, Stub.NoteLink.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new CompositeDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new CompositeSemanticController();

        return semanticController;
    }

    @Override
    public boolean needLayout(Node node)
    {
        return ( node instanceof Compartment ) && ! ( node instanceof SubDiagram );
    }
    
    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return edge.nodes().noneMatch( Util::isReaction );
    }
    
    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }
}
