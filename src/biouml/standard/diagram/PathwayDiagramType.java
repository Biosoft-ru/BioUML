package biouml.standard.diagram;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.filter.BiopolimerDiagramFilter;
import biouml.standard.type.Cell;
import biouml.standard.type.Compartment;
import biouml.standard.type.Concept;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

/**
 * This is basic class for formal description of biological pathways.
 */
public class PathwayDiagramType extends DiagramTypeSupport
{
    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] { Cell.class, Compartment.class,
                             Concept.class,
                             Gene.class, RNA.class, Protein.class, Substance.class, Reaction.class,
                             Stub.Note.class };
    }

    @Override
    public Class<?>[] getEdgeTypes()
    {
        return new Class[] { SemanticRelation.class, Stub.NoteLink.class };
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if (diagramViewBuilder == null)
            diagramViewBuilder = new PathwayDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public DiagramFilter getDiagramFilter(Diagram diagram)
    {
        return new BiopolimerDiagramFilter(diagram);
    }

    @Override
    public SemanticController getSemanticController()
    {
        if (semanticController == null)
            semanticController = new PathwaySemanticController();

        return semanticController;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return false;
    }
}
