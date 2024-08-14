package biouml.plugins.sbgn.extension;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.Type;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;

@PropertyName("SBML model in SBGN notation extended")
@PropertyDescription("SBML (SBGN) model extended")
public class SbgnExDiagramType extends SbgnDiagramType
{
    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {Stub.NoteLink.class, Type.TYPE_PORTLINK, SemanticRelation.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new SbgnExDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new SbgnExSemanticController();

        return semanticController;
    }
    
    @Override
    public boolean isGeneralPurpose()
    {
        return false;
    }
}
