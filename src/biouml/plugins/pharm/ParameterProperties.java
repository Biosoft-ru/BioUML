package biouml.plugins.pharm;

import java.awt.Dimension;
import java.awt.Point;
import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.diagram.PortProperties;
import biouml.standard.type.Stub;

public class ParameterProperties implements InitialElementProperties
{
    public static final String PARAMETER_TYPE = "Parameter";
    public static final String OBSERVED_TYPE = "Observed";
    public static final String DOSE_TYPE = "Dose";

    public String[] getAvailableTypes()
    {
        return new String[] {PARAMETER_TYPE, OBSERVED_TYPE, DOSE_TYPE};
    }
    String[] availableNames;
    public ParameterProperties(Compartment c)
    {

        if( ! ( c instanceof StructuralModel ) )
            throw new IllegalArgumentException( "Only structural model can be parent for parameter" );

        availableNames = PortProperties.getParameters( ( (StructuralModel)c ).getDiagram() );
        if( availableNames.length > 0 )
            this.parameterName = availableNames[0];
    }

    private String parameterName;

    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( parameterName.isEmpty() )
            throw new IllegalArgumentException( "Please specify node name" );

        String name = parameterName.startsWith( "$" ) ? parameterName.substring( 1 ) : parameterName;
        if( c.get( name ) != null )
        {
            throw new IllegalArgumentException( "Parameter with this name alreay exists." );
        }

        Node node = new Node( c, new Stub( null, name, Type.TYPE_PORT ) );
        node.setTitle( parameterName );
        DynamicProperty dp = new DynamicProperty( "type", String.class, this.type ) ;
        dp.setReadOnly( true );
        node.getAttributes().add(dp );
        dp = new DynamicProperty( "parameterName", Integer.class, parameterName ) ;
        dp.setReadOnly(true);
        node.getAttributes().add(dp );
        SemanticController semanticController = Diagram.getDiagram( c ).getType().getSemanticController();
        if( semanticController.canAccept( c, node ) )
        {
            viewPane.add( node, location );
        }
        if (c instanceof StructuralModel)
        {
            CompositeSemanticController.movePortToEdge( node, c, new Dimension(0,0), false );
        }
        return new DiagramElementGroup( node );
    }

    @PropertyName ( "Name" )
    public String getParameterName()
    {
        return parameterName;
    }

    public void setParameterName(String name)
    {
        this.parameterName = name;
    }

    public String[] getAvailableNames()
    {
        return availableNames;
    }

    private String type = PARAMETER_TYPE;
    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

}
