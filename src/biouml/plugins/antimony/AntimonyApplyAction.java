package biouml.plugins.antimony;

import java.util.List;
import java.util.stream.Collectors;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.access.subaction.DynamicAction;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@SuppressWarnings ( "serial" )
public class AntimonyApplyAction extends BackgroundDynamicAction
{
    public AntimonyApplyAction()
    {
        setNumSelected( DynamicAction.SELECTED_ZERO_OR_ANY );
    }
    @Override
    public void validateParameters(Object model, List<DataElement> selectedItems)
    {
        if( !isApplicable( model ) )
            throw new ParameterNotAcceptableException( "Document", String.valueOf( model ) );
    }
    
    @Override
    public boolean isApplicable(Object object)
    {
        return object instanceof Diagram && AntimonyUtility.checkDiagramType((Diagram)object);
    }

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    Diagram diagram = (Diagram)model;
                    Antimony antimony = new Antimony( diagram );
                    Diagram changedDiagram = antimony.generateDiagram( AntimonyUtility.getAntimonyAttribute( diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR ) );
                    applyAntimony( diagram, changedDiagram );
                    if( diagram.getAttributes().getValue( AntimonyDiagramListener.LISTENER_ATTR ) instanceof AntimonyDiagramListener )
                        ( (AntimonyDiagramListener)diagram.getAttributes()
                                .getValue( AntimonyDiagramListener.LISTENER_ATTR ) ).antimony = antimony;
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
        
    }
    public static void applyAntimony(Diagram diagram, Diagram changedDiagram) throws Exception
    {
        EModel emodel = diagram.getRole( EModel.class );
        EModel changedEmodel = changedDiagram.getRole( EModel.class );
        DataCollection<Variable> variables = emodel.getVariables();
        
        boolean notificationEnabled = diagram.isNotificationEnabled();
        boolean propagationEnabled = diagram.isPropagationEnabled();
        boolean emodelNotificationEnabled = variables.isNotificationEnabled();
        boolean emodelPropagationEnabled = variables.isPropagationEnabled();

        diagram.setNotificationEnabled( false );
        diagram.setPropagationEnabled( false );
        variables.setNotificationEnabled( false );
        variables.setPropagationEnabled( false );

        for( DiagramElement de : diagram.recursiveStream().remove( de -> de instanceof Diagram ).toSet() )
            diagram.remove( de.getName() );

        for( Variable v : variables.stream().filter( v -> ! ( v instanceof VariableRole ) ).collect( Collectors.toSet() ) )
            variables.remove( v.getName() );

        for( Variable v : changedEmodel.getVariables().stream().filter( v -> ! ( v instanceof VariableRole ) )
                .collect( Collectors.toSet() ) )
        {
            Variable var = new Variable( v.getName(), emodel, variables );
            var.setComment( v.getComment() );
            var.setConstant( v.isConstant() );
            var.setInitialValue( v.getInitialValue() );
            var.setUnits( v.getUnits() );
            for( DynamicProperty dp : v.getAttributes() )
                var.getAttributes().add( new DynamicProperty( dp.getName(), dp.getType(), dp.getValue() ) );

            emodel.put( var );
        }

        changedDiagram.recursiveStream().remove( de -> de instanceof Diagram ).select( Node.class )
                .forEachOrdered( n -> copyDiagramElement( diagram, n ) );

        changedDiagram.recursiveStream().select( Edge.class ).forEach( e -> copyDiagramElement( diagram, e ) );

        variables.setPropagationEnabled( emodelPropagationEnabled );
        variables.setNotificationEnabled( emodelNotificationEnabled );
        diagram.setPropagationEnabled( propagationEnabled );
        diagram.setNotificationEnabled( notificationEnabled );
    }

    private static void copyDiagramElement(Diagram diagram, DiagramElement de)
    {
        Compartment parent = (Compartment)diagram.findNode( de.getCompartment().getName() );
        if( parent == null )
            parent = diagram;
        parent.put( de.clone( parent, de.getName() ) );
    }
}
