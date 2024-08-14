package biouml.plugins.pharm;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.Application;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.diagram.CreateEdgeDialog;
import biouml.standard.diagram.SimpleEdgePane;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;


/**
 *
 * @author Ilya
 *
 */
public class PopulationModelSemanticController extends DefaultSemanticController
{
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {

        if( Type.TYPE_STOCHASTIC_DEPENDENCY.equals( type ) || Type.TYPE_DEPENDENCY.equals( type ) )
        {
            String name = generateUniqueNodeName( parent, type.toString() );
            CreateEdgeDialog dialog = new CreateEdgeDialog( point, "New edge", new SimpleEdgePane( Module.getModule( parent ), viewEditor,
                    name, type.toString(), null ) );
            dialog.setVisible( true );
            return null;
        }

        try
        {
            Object properties = getPropertiesByType( parent, type, point );

            PropertiesDialog dialog = new PropertiesDialog( Application.getApplicationFrame(), "New element", properties );
            if( dialog.doModal() )
            {
                if( properties instanceof InitialElementProperties )
                    ( (InitialElementProperties)properties ).createElements( parent, point, viewEditor );
                return null;
            }
        }
        catch( Throwable t )
        {

            throw ExceptionRegistry.translateException( t );
        }

        return null;
    }


    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        try
        {
            Object bean = super.getPropertiesByType( compartment, type, point );
            if( bean != null )
                return bean;

            else if (type.equals( StructuralModel.class ))
                return new StructuralModelProperties(Diagram.getDiagram(compartment));

            else if( type.equals( PopulationVariable.class ) )
                return new PopulationVariable(generateUniqueNodeName( compartment, "variable", false ), false );

            if( type.equals( Type.TYPE_ARRAY ) )
                return new ArrayProperties( generateUniqueNodeName( compartment, type.toString(), false  ) );
            else if (type.equals( Type.TYPE_TABLE_DATA))
                return new TableProperties( generateUniqueNodeName( compartment, type.toString(), false  ) );
            else if (type.equals( Type.TYPE_PORT))
                return new ParameterProperties(compartment );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        return null;
    }

    @Override
    public boolean canAccept(Compartment parent, DiagramElement de)
    {
        if( de.getKernel() == null )
            return false;

        if( de instanceof Node )
        {
            Node node = (Node)de;

            if( Util.isVariable( node ) || Util.isArray( node ))
            {
                return parent instanceof Diagram || Util.isArray( parent );
            }
            else if (Type.TYPE_TABLE_DATA.equals( node.getKernel().getType()))
            {
                return parent instanceof Diagram;
            }
            else if (node instanceof StructuralModel)
            {
                return parent instanceof Diagram;
            }
            else if( Util.isPort( node ) )
            {
                return parent instanceof StructuralModel;
            }

        }
        else if( de instanceof Edge )
        {
            Node input = ( (Edge)de ).getInput();
            Node output = ( (Edge)de ).getOutput();

            boolean accept = ( Util.isPort( input ) && Util.isVariable( output ) )
                    || ( Util.isVariable( input ) && ( Util.isPort( output ) || Util.isVariable( output ) ) );
            return accept;
        }

        return false;
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if( offset.width == 0 && offset.height == 0 )
            return offset;
        if( de instanceof Node && Util.isPort( (Node)de ) )
        {
            Compartment compartment = (Compartment)de.getOrigin();
            Dimension newOffset = CompositeSemanticController.movePortToEdge( (Node)de, compartment, offset, false );

            for( Edge edge : ( (Node)de ).getEdges() )
                recalculateEdgePath( edge );
            return newOffset;
        }

        return super.move( de, newParent, offset, oldBounds );
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement)
    {
        Base kernel = de.getKernel();
        if( kernel != null && kernel instanceof Stub && Type.TYPE_STRUCTURAL_MODEL.equals( kernel.getType() ) )
        {
            try
            {
                Node node = (Node)de;
                DataElementPath diagramPath = (DataElementPath)de.getAttributes().getProperty( "DiagramPath" ).getValue();

                DataElement dataElement = diagramPath.getDataElement();

                StructuralModel model = new StructuralModel( compartment, (Diagram)dataElement, de.getName() );
                model.setLocation( node.getLocation() );
                model.setComment( node.getComment() );
                model.setShapeSize( node.getShapeSize() );

                for (DynamicProperty dp: de.getAttributes())
                    model.getAttributes().add( dp );
                return model;
            }
            catch( Exception ex )
            {
                return null;
            }

        }
        else if( kernel != null && kernel instanceof Stub && Type.TYPE_VARIABLE.equals( kernel.getType() ) )
        {
            if (de.getRole() == null)
            {
                DynamicProperty dp = de.getAttributes().getProperty( "populationVariable" );
                PopulationVariable var = (PopulationVariable)dp.getValue();
                de.setRole(var);
                var.setDiagramElement( de );
            }
        }

        return de;
    }


    @Override
    public Dimension resize(DiagramElement de, Dimension sizeChange)
    {
        if( de instanceof StructuralModel )
        {
            Dimension oldSize = de.getView().getBounds().getSize();
            Dimension newSize = new Dimension(oldSize.width + sizeChange.width, oldSize.height + sizeChange.height);
            Rectangle rec = new Rectangle(((Compartment)de).getLocation(), newSize);
            ( (Compartment)de ).setShapeSize(newSize);
            for( Node node : ( (StructuralModel)de ).getNodes() )
            {
                if( Util.isPort( node ) )
                {
                    Point loc = node.getLocation();
                    CompositeSemanticController.movePortToEdge( node, rec, loc, true );
                    node.setLocation( loc );
                    for (Edge e: node.getEdges())
                        recalculateEdgePath( e );
                }
            }
        }

        return sizeChange;
    }
}
