package biouml.plugins.pharm;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.type.Base;

/**
 * 
 * @author Ilya
 *
 */
@PropertyName("Population-based model")
@PropertyDescription("Model contains structural model (e.g. PK-PD model), distribution of its parameters and external events (e.g. drug dosing)")
public class PopulationModelDiagramType extends DiagramTypeSupport
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        diagram.setRole( new PopulationEModel( diagram ) );
        return diagram;
    }
    
    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {PopulationVariable.class, Type.TYPE_ARRAY, Type.TYPE_TABLE_DATA, StructuralModel.class, Type.TYPE_PORT};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {Type.TYPE_DEPENDENCY};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if (diagramViewBuilder == null)
            diagramViewBuilder = new PopulationModelDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if (semanticController == null)
            semanticController = new PopulationModelSemanticController();

        return semanticController;
    }
    
    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }
}
