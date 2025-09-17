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
import ru.biosoft.access.core.DataCollection;

@PropertyName ( "Physicell model" )
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
        return new Object[] {PhysicellConstants.TYPE_CELL_DEFINITION, PhysicellConstants.TYPE_SUBSTRATE, PhysicellConstants.TYPE_EVENT,
                PhysicellConstants.TYPE_NOTE};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {PhysicellConstants.TYPE_SECRETION, PhysicellConstants.TYPE_CHEMOTAXIS, PhysicellConstants.TYPE_INTERACTION,
                PhysicellConstants.TYPE_TRANSFORMATION, PhysicellConstants.TYPE_NOTE_LINK};
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
    
    @Override
    public String getTitle()
    {
        return "Physicell model";
    }

    @Override //TODO" for some reason description is not read correctly from annotation
    public String getDescription()
    {
         return "Multicellular agent-based spatial model using Physicell formalism.";
    }
}