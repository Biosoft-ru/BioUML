package biouml.standard.diagram;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.standard.type.Cell;
import biouml.standard.type.Concept;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class SemanticNetworkDiagramType extends PathwayDiagramType
{
    @Override
    public Class[] getNodeTypes()
    {
        return new Class[] {Cell.class, Concept.class, Gene.class, RNA.class, Protein.class, Substance.class, Stub.Note.class};
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
            diagramViewBuilder = new SemanticNetworkDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public DiagramFilter getDiagramFilter(Diagram diagram)
    {
        return null;
    }

    public static class SemanticNetworkDiagramViewBuilder extends PathwayDiagramViewBuilder
    {
        @Override
        public DiagramViewOptions createDefaultDiagramViewOptions()
        {
            return new SemanticNetworkDiagramViewOptions(null);
        }
    }

    /**
     * We have defined SemanticNetworkDiagramViewOptions to have an ability specify
     * corresponding bean info. This bean info can customize view of
     * SemanticNetworkDiagramViewOptions in property inspector.
     */
    @PropertyName ( "View options" )
    public static class SemanticNetworkDiagramViewOptions extends PathwayDiagramViewOptions
    {
        public SemanticNetworkDiagramViewOptions(Option parent)
        {
            super(parent);
        }
    }

    public static class SemanticNetworkDiagramViewOptionsBeanInfo extends PathwayDiagramViewOptionsBeanInfo
    {
        public SemanticNetworkDiagramViewOptionsBeanInfo()
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
