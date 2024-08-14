package biouml.plugins.kegg;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

public class KeggPathwayDiagramType extends PathwayDiagramType
{
    public KeggPathwayDiagramType()
    {
        //super(new MessageBundle().getString("CN_PATHWAY_DIAGRAM"));
    }

    /** @pending provide special class for each molecule type */
    @Override
    public Class[] getNodeTypes()
    {
        return new Class[] {Protein.class, Substance.class, Reaction.class, Stub.Note.class};
    }

    @Override
    public Class[] getEdgeTypes()
    {
        return new Class[] {SemanticRelation.class, Stub.NoteLink.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new KeggPathwayDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public DiagramFilter getDiagramFilter(Diagram diagram)
    {
        return null; //new GeneNetDiagramFilter(diagram);
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new KeggPathwaySemanticController();

        return semanticController;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return false;
    }
}
