package biouml.plugins.hemodynamics;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.SemanticController;
import biouml.model.dynamics.Equation;
import biouml.standard.type.Base;
import biouml.standard.type.Type;

/**
 * @author Ilya
 */
@PropertyName("Arterial tree")
@PropertyDescription("Model of blood flow in arterial tree.")
public class HemodynamicsDiagramType extends DiagramTypeSupport
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        diagram.setRole( new HemodynamicsEModel( diagram ) );
        return diagram;
    }
    
    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {HemodynamicsType.BIFURCATION, HemodynamicsType.CONTROL_POINT, Equation.class, Type.TYPE_PORT};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if (diagramViewBuilder == null)
            diagramViewBuilder = new HemodynamicsDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if (semanticController == null)
            semanticController = new HemodynamicsSemanticController();

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
