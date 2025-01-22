package biouml.plugins.physicell;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.type.Base;
import biouml.standard.type.Type;
import ru.biosoft.access.core.DataCollection;

@PropertyName ( "Physicell models" )
@PropertyDescription ( "Multicellular agent models." )
public class PhysicellDiagramType extends DiagramTypeSupport
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram( origin, diagramName, kernel );
        diagram.setRole( new MulticellEModel( diagram ) );
        return diagram;
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {PhysicellConstants.TYPE_CELL_DEFINITION, PhysicellConstants.TYPE_SUBSTRATE, PhysicellConstants.TYPE_EVENT, Type.TYPE_NOTE};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {PhysicellConstants.TYPE_SECRETION, PhysicellConstants.TYPE_CHEMOTAXIS, PhysicellConstants.TYPE_INTERACTION,
                PhysicellConstants.TYPE_TRANSFORMATION};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new PhysicellDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new PhysicellDiagramSemanticController();
        return semanticController;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }

    @Override
    public DiagramXmlWriter getDiagramWriter()
    {
        return new PhysicellDiagramWriter();
    }

    @Override
    public DiagramXmlReader getDiagramReader()
    {
        return new PhysicellDiagramReader();
    }
}