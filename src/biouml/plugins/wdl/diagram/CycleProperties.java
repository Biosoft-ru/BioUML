package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;

import ru.biosoft.graphics.editor.ViewEditorPane;

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
import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
@PropertyName ( "Cycle properties" )
@PropertyDescription ( "Cycle properties." )
public class CycleProperties extends Option implements InitialElementProperties
{
    protected String name = "cycle_1";
    protected String variable = "i";

    public CycleProperties()
    {

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
    
    @PropertyName ( "Cycle variable" )
    @PropertyDescription ( "Cycle variable" )
    public String getVariable()
    {
        return variable;
    }

    public void setVariable(String variable)
    {
        String oldValue = this.variable;
        this.variable = variable;
        this.firePropertyChange( "variable", oldValue, variable );
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty conditional block name!" );

        String name = WDLSemanticController.uniqName( parent, this.name );
        Compartment compartment = new Compartment( parent, name, new Stub( null, name, WDLConstants.SCATTER_TYPE ) );
        compartment.setNotificationEnabled(false);
        compartment.setShapeSize( new Dimension( 500, 300 ) );
       
        Node variableNode = new Node( compartment, variable, new Stub( null, variable, WDLConstants.SCATTER_VARIABLE_TYPE ));
        WorkflowUtil.setName( variableNode, variable );
        compartment.put( variableNode );
        
        compartment.setNotificationEnabled(true);
        Diagram diagram = Diagram.getDiagram(parent);
        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept(parent, compartment) )
            return new DiagramElementGroup();

        return new DiagramElementGroup( compartment );
    }
}