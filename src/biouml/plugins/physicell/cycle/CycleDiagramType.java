package biouml.plugins.physicell.cycle;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;

@PropertyName ( "Cell Cycle model" )
@PropertyDescription ( "Cell Cycle model." )
public class CycleDiagramType extends DiagramTypeSupport
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram( origin, diagramName, kernel );
        diagram.setRole( new CycleEModel( diagram ) );
        return diagram;
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {CycleConstants.TYPE_PHASE};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {CycleConstants.TYPE_TRANSITION};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new CycleDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new CycleDiagramSemanticController();
        return semanticController;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }
}