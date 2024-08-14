package biouml.plugins.agentmodeling;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

@PropertyName ( "Agent-based model" )
@PropertyDescription ( "Hierarchic model which can contain modules with different formalisms and types (ODE models, PDE models, scripts, special modules)." )
public class AgentModelDiagramType extends CompositeDiagramType
{

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram( origin, diagramName, kernel );
        diagram.setRole( new AgentEModel( diagram ) );
        return diagram;
    }

    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {SubDiagram.class, Type.TYPE_BLOCK, Stub.Note.class, Event.class, Equation.class, Function.class,
                Constraint.class, Stub.Bus.class, Stub.SwitchElement.class, Stub.Constant.class, Stub.AveragerElement.class,
                ScriptAgent.class, Stub.InputConnectionPort.class, Stub.OutputConnectionPort.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new AgentModelDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new AgentModelSemanticController();

        return semanticController;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }

    @Override
    public String getTitle()
    {
        return "Agent-based model";
    }

    @Override
    public AgentModelDiagramXmlReader getDiagramReader()
    {
        return new AgentModelDiagramXmlReader();
    }
}
