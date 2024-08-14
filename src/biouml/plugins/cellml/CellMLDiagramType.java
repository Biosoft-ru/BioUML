package biouml.plugins.cellml;

import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;

public class CellMLDiagramType extends DiagramTypeSupport
{
    @Override
    public Class<?>[] getNodeTypes()
    {
        return new Class[] { }; //Compartment.class, Specie.class, Reaction.class };
    }

    @Override
    public Class<?>[] getEdgeTypes()
    {
        return new Class[] { }; //SpecieReference.class };
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if (diagramViewBuilder == null)
            diagramViewBuilder = new CellMLDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if (semanticController == null)
            semanticController = new CellMLSemanticController();

        return semanticController;
    }
}
