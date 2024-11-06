package biouml.plugins.sbol;

import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;

public class SbolDiagramType extends DiagramTypeSupport
{

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if ( diagramViewBuilder == null )
            diagramViewBuilder = new SbolDiagramViewBuilder();
        return diagramViewBuilder;
    }

}
