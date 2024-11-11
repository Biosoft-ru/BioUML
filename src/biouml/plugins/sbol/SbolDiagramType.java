package biouml.plugins.sbol;

import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;

public class SbolDiagramType extends DiagramTypeSupport
{

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Backbone.class};
    }
    
    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if ( diagramViewBuilder == null )
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

}
