package biouml.plugins.biopax;

import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.diagram.PathwaySemanticController;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.DNA;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Substance;

public class BioPAXDiagramType extends DiagramTypeSupport
{
    @Override
    public Class<?>[] getNodeTypes()
    {
        return new Class[] {Concept.class, Substance.class, Complex.class, RNA.class, DNA.class, Protein.class, Reaction.class};
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
            diagramViewBuilder = new BioPAXDiagramViewBuilder();

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
