package biouml.plugins.sbml.composite;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.plugins.sbml.SbmlEModel;
import biouml.standard.type.Specie;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Compartment;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;

public class SbmlCompositeDiagramType extends DiagramTypeSupport
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram( origin, diagramName, kernel );
        diagram.setRole( new SbmlEModel( diagram ) );
        return diagram;
    }

    @Override
    public Class[] getNodeTypes()
    {
        return new Class[] {Compartment.class, Specie.class, Reaction.class, Event.class, Equation.class, Function.class, SubDiagram.class,
                Stub.ContactConnectionPort.class};
    }

    @Override
    public Class[] getEdgeTypes()
    {
        return new Class[] {Stub.UndirectedConnection.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new SbmlCompositeDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new SbmlCompositeSemanticController();

        return semanticController;
    }

    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return edge.nodes().noneMatch( Util::isReaction );
    }
}
