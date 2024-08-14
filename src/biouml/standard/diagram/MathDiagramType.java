package biouml.standard.diagram;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.SemanticController;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.State;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.Transition;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

/**
 * MathDiagramType is pure equation type with autogenerating dependencies
 */
@PropertyName("Mathematical model")
@PropertyDescription("Model consisting of ODE, algebraic equations and events. No entitites or reactions between them are used.")
public class MathDiagramType extends PathwaySimulationDiagramType
{

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        diagram.getViewOptions().setDependencyEdges(true);
        return diagram;
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Type.TYPE_BLOCK, Stub.Note.class, Stub.OutputConnectionPort.class, Stub.InputConnectionPort.class,
                Stub.ContactConnectionPort.class, Event.class, Equation.class, Function.class, Constraint.class, State.class, TableElement.class};
    }

    @Override
    public Class[] getEdgeTypes()
    {
        return new Class[] {Stub.NoteLink.class, Transition.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new MathDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new MathDiagramSemanticController();

        return semanticController;
    }
    
    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return true;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }
}
