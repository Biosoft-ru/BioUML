package biouml.plugins.sbol;

import javax.annotation.Nonnull;

import org.sbolstandard.core2.SBOLDocument;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.SemanticController;
import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.DPSUtils;

/**
 * SBOL Diagram type
 */
@PropertyName("SBOL model")
public class SbolDiagramType extends DiagramTypeSupport
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram( origin, diagramName, kernel );
        SBOLDocument doc = new SBOLDocument();
        doc.setDefaultURIprefix( SbolConstants.HTTPS_BIOUML_ORG );
        doc.createModuleDefinition( "Main_module", "1" );
        diagram.getAttributes().add( DPSUtils.createHiddenReadOnly( SbolConstants.SBOL_DOCUMENT_PROPERTY, SBOLDocument.class, doc ) );
        return diagram;
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Backbone.class, SequenceFeature.class, MolecularSpecies.class, InteractionProperties.class};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {ParticipationProperties.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new SbolDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new SbolDiagramSemanticController();

        return semanticController;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }
    
    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return true;
    }
}