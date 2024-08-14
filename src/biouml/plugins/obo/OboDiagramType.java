package biouml.plugins.obo;

import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.diagram.PathwaySemanticController;
import biouml.standard.type.Concept;
import biouml.standard.type.SemanticRelation;

public class OboDiagramType extends DiagramTypeSupport
{
    @Override
    public Class<?>[] getNodeTypes()
    {
        return new Class[] {Concept.class};
    }

    @Override
    public Class<?>[] getEdgeTypes()
    {
        return new Class[] {SemanticRelation.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if (diagramViewBuilder == null)
            diagramViewBuilder = new OboDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if (semanticController == null)
            semanticController = new PathwaySemanticController();

        return semanticController;
    }
}
