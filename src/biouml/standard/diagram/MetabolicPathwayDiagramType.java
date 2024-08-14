package biouml.standard.diagram;

import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.SemanticController;
import biouml.standard.type.Compartment;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class MetabolicPathwayDiagramType extends PathwayDiagramType
{
    @Override
    public Class[] getNodeTypes()
    {
        return new Class[] {Compartment.class, Protein.class, Substance.class, Reaction.class, Stub.Note.class};
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
            diagramViewBuilder = new MetabolicPathwayDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new PathwaySemanticController();

        return semanticController;
    }


    public static class MetabolicPathwayDiagramViewBuilder extends PathwayDiagramViewBuilder
    {
        @Override
        public DiagramViewOptions createDefaultDiagramViewOptions()
        {
            return new MetabolicPathwayDiagramViewOptions(null);
        }
    }

    /**
     * We have redefined ViewOptions to have an ability specify
     * corresponding bean info. This bean info can customize view of
     * MetabolicPathwayDiagramViewOptions in property inspector.
     */
    @PropertyName ( "View options" )
    public static class MetabolicPathwayDiagramViewOptions extends PathwayDiagramViewOptions
    {
        public MetabolicPathwayDiagramViewOptions(Option parent)
        {
            super(parent);
        }
    }

    public static class MetabolicPathwayDiagramViewOptionsBeanInfo extends PathwayDiagramViewOptionsBeanInfo
    {
        public MetabolicPathwayDiagramViewOptionsBeanInfo()
        {
            super();
        }
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return false;
    }
}
