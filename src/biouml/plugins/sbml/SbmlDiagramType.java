package biouml.plugins.sbml;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.Compartment;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;


public class SbmlDiagramType extends DiagramTypeSupport
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        diagram.setRole( new SbmlEModel( diagram ) );
        PathwaySimulationDiagramType.DiagramPropertyChangeListener listener = new PathwaySimulationDiagramType.DiagramPropertyChangeListener(diagram);
        diagram.getViewOptions().addPropertyChangeListener(listener);
        return diagram;
    }
    
    @Override
    public Class[] getNodeTypes()
    {
        return new Class[] { Compartment.class, Specie.class, Reaction.class };
    }
    
    @Override
    public Class[] getEdgeTypes()
    {
        return new Class[] { };
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if (diagramViewBuilder == null)
            diagramViewBuilder = new SbmlDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if (semanticController == null)
            semanticController = new SbmlSemanticController();

        return semanticController;
    }
}
