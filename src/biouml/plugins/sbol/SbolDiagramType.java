package biouml.plugins.sbol;

import javax.annotation.Nonnull;

import org.sbolstandard.core2.SBOLDocument;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;

public class SbolDiagramType extends DiagramTypeSupport
{

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram( origin, diagramName, kernel );
        SBOLDocument doc = new SBOLDocument();
        diagram.getAttributes().add( new DynamicProperty(SbolUtil.SBOL_DOCUMENT_PROPERTY, SBOLDocument.class, doc) );//.getValue(SbolUtil.SBOL_DOCUMENT_PROPERTY);
        return diagram;
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Backbone.class};
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

}
