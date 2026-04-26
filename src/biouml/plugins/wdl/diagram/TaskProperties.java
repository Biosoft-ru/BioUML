package biouml.plugins.wdl.diagram;

import java.awt.Color;
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
import biouml.model.SemanticController;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.model.ExpressionInfo;
import biouml.standard.type.Stub;
import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
@PropertyName ( "Task properties" )
@PropertyDescription ( "Task properties." )
public class TaskProperties extends Option implements InitialElementProperties
{

    protected String name = "task_1";
    protected String command = "";

    public TaskProperties()
    {

    }

    @PropertyName ( "Name" )
    @PropertyDescription ( "Name" )
    public String getName()
    {
        return name;
    }

    //TODO: validate name
    public void setName(String name)
    {
        String oldValue = this.name;
        this.name = name;
        this.firePropertyChange( "name", oldValue, name );
    }

    @PropertyName ( "Command" )
    @PropertyDescription ( "Command" )
    public String getCommand()
    {
        return command;
    }

    public void setCommand(String command)
    {
        String oldValue = this.command;
        this.command = command;
        this.firePropertyChange( "command", oldValue, command );
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty task name!" );

        String name = WDLSemanticController.uniqName( parent, this.name );
        Compartment compartment = new Compartment( parent, name, new Stub( null, name, WDLConstants.TASK_TYPE ) );
        compartment.setNotificationEnabled( false );

        WorkflowUtil.setBeforeCommand( compartment, new ExpressionInfo[0] );
        WorkflowUtil.setCommand( compartment, command );
        WorkflowUtil.setRuntime( compartment, new HashMap<>() );
        compartment.setShapeSize( new Dimension( 200, 50 ) );

        compartment.getAttributes().add( DPSUtils.createHiddenReadOnly(  "innerNodesPortFinder", Boolean.class, true ) );
        compartment.setNotificationEnabled( true );
        Diagram diagram = Diagram.getDiagram(parent);
        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept(parent, compartment) )
            return new DiagramElementGroup();

        return new DiagramElementGroup( compartment );
    }

}