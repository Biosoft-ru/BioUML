package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.HashMap;

import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
@PropertyName ( "Call properties" )
@PropertyDescription ( "Call properties." )
public class CallProperties extends Option implements InitialElementProperties
{
    protected String name = "call_1";
    protected String alias = "call_1";


    protected String taskRef = "";
    protected String[] availableTasks = new String[0];
    public CallProperties(Diagram diagram)
    {
        availableTasks = StreamEx.of( WorkflowUtil.getTasks( diagram ) ).map( task -> task.getName() ).toArray( String[]::new );
        taskRef = availableTasks.length == 0? "": availableTasks[0];
    }

    @PropertyName ( "Name" )
    @PropertyDescription ( "Name" )
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        String oldValue = this.name;
        this.name = name;
        this.firePropertyChange( "name", oldValue, name );
    }
    
    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    @PropertyName ( "Called task" )
    @PropertyDescription ( "Called task." )
    public String getTaskRef()
    {
        return taskRef;
    }

    public void setTaskRef(String taskRef)
    {
        String oldValue = this.taskRef;
        this.taskRef = taskRef;
        this.firePropertyChange( "taskRef", oldValue, taskRef );
    }

    public StreamEx<String> getAvailableTasks()
    {
        return StreamEx.of(availableTasks);
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty task name!" );
        Diagram diagram = Diagram.getDiagram( parent );
        
        String name = WDLSemanticController.uniqName( parent, this.name );
        Compartment compartment = new Compartment( parent, name, new Stub( null, name, WDLConstants.CALL_TYPE ) );
        compartment.setNotificationEnabled( false );

        WorkflowUtil.setCallName( compartment, taskRef );
        WorkflowUtil.setAlias( compartment, alias );
        WorkflowUtil.setTaskRef( compartment, taskRef );
        
        Compartment c = WorkflowUtil.findTask( taskRef, diagram );
        for( Node input : WorkflowUtil.getInputs( c ) )
        {
            Node callInput = input.clone( compartment, input.getName() );
            compartment.put( callInput );
        }
        
        for( Node output : WorkflowUtil.getOutputs( c ) )
        {
            Node callOutput = output.clone( compartment, output.getName() );
            compartment.put( callOutput );
        }

        compartment.setShapeSize( new Dimension( 200, 50 ) );
        compartment.getAttributes().add( DPSUtils.createHiddenReadOnly( "innerNodesPortFinder", Boolean.class, true ) );

        compartment.setNotificationEnabled( true );
       
        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept( parent, compartment ) )
            return new DiagramElementGroup();

        return new DiagramElementGroup( compartment );
    }

}