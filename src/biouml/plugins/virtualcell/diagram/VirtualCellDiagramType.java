package biouml.plugins.virtualcell.diagram;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;

@PropertyName ( "Virtual cell models" )
@PropertyDescription ( "Virtual cell models." )
public class VirtualCellDiagramType extends DiagramTypeSupport
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram( origin, diagramName, kernel );
        diagram.setRole( new EModel(diagram) );
        return diagram;
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {"Pool", "Translation", "ProteinDegradation", "Population", "Metabolism", "Transcription"};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {"Connection"};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new VirtualCellDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new VirtualCellDiagramSemanticController();
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
        return new VirtualCellDiagramWriter();
    }

    @Override
    public DiagramXmlReader getDiagramReader()
    {
        return new VirtualCellDiagramReader();
    }
}
