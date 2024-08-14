package biouml.standard.diagram;

import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.SemanticController;
import biouml.standard.type.Compartment;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class GeneNetworkDiagramType extends PathwayDiagramType
{
    @Override
    public Class[] getNodeTypes()
    {
        return new Class[] {Compartment.class, Gene.class, RNA.class, Protein.class, Substance.class, Reaction.class, Stub.Note.class};
    }

    @Override
    public Class[] getEdgeTypes()
    {
        return new Class[] {SpecieReference.class, Stub.NoteLink.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new GeneNetworkDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new PathwaySemanticController();

        return semanticController;
    }

    public static class GeneNetworkDiagramViewBuilder extends PathwayDiagramViewBuilder
    {
        @Override
        public DiagramViewOptions createDefaultDiagramViewOptions()
        {
            return new GeneNetworkDiagramViewOptions(null);
        }
    }

    /**
     * We have redefined ViewOptions to have an ability specify
     * corresponding bean info. This bean info can customize view of
     * MetabolicPathwayDiagramViewOptions in property inspector.
     */
    @PropertyName ( "View options" )
    public static class GeneNetworkDiagramViewOptions extends PathwayDiagramViewOptions
    {
        public GeneNetworkDiagramViewOptions(Option parent)
        {
            super(parent);
        }
    }

    public static class GeneNetworkDiagramViewOptionsBeanInfo extends PathwayDiagramViewOptionsBeanInfo
    {
        public GeneNetworkDiagramViewOptionsBeanInfo()
        {
            super(GeneNetworkDiagramViewOptions.class);
        }
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return false;
    }
}


