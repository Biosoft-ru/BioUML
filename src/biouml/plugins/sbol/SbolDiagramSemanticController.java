package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.standard.type.Base;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

public class SbolDiagramSemanticController extends DefaultSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( isNotificationEnabled );

        try
        {
            Object properties = getPropertiesByType( parent, type, pt );
            if( properties instanceof InitialElementProperties )
            {
                if( new PropertiesDialog( Application.getApplicationFrame(), "New element", properties ).doModal() )
                    ( (InitialElementProperties)properties ).createElements( parent, pt, viewEditor );
                return DiagramElementGroup.EMPTY_EG;
            }
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Error during element creation", ex );
            return DiagramElementGroup.EMPTY_EG;
        }
        finally
        {
            parent.setNotificationEnabled( isNotificationEnabled );
        }
        return super.createInstance( parent, type, pt, viewEditor );
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        Base deBase = de.getKernel();
        Base compartmentBase = compartment.getKernel();

        if( deBase instanceof SequenceFeature )
        {
            return compartmentBase instanceof Backbone;
        }
        else if( deBase instanceof Backbone )
        {
            return compartment instanceof Diagram;
        }
        else if( deBase instanceof MolecularSpecies )
        {
            return compartment instanceof Diagram;
        }

        return false;
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        try
        {
            if( type instanceof Class )
            {
                return ( (Class)type ).getConstructor().newInstance();
            }
        }
        catch( Exception ex )
        {

        }
        return null;
    }

    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        return false;
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        return de;
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if ( de instanceof Node )
        {
            if ( de.getKernel() instanceof Backbone )
                return super.move(de, newParent, offset, oldBounds);
            Node node = (Node) de;
            Compartment parent = (Compartment) node.getOrigin();
            if ( parent.getKernel() instanceof Backbone )
            {
                //do not move outside parent backbone
                if ( newParent != parent )
                {
                    return new Dimension(0, 0);
                }
                Point location = node.getLocation();
                int oldX = location.x;
                int width = (int) (Math.ceil(((Node) de).getShapeSize().getWidth()));

                location.translate(offset.width, 0);
                int newX = location.x;
                int dx = parent.stream().mapToInt(n -> {
                    if ( n instanceof Node )
                    {
                        Point nlocation = ((Node) n).getLocation();
                        int nX = nlocation.x;
                        int nW = (int) (Math.ceil(((Node) n).getShapeSize().getWidth()));

                        if ( oldX > newX && nX <= newX && nX + nW >= newX )
                            return oldX - nX - nW + 1;
                        else if ( oldX < newX && nX <= newX && nX + nW >= newX )
                            return nX + nW + 1 - width - oldX;
                        return 0;
                    }
                    return 0;
                }).max().orElse(0);
                int shift = oldX > newX ? -dx : dx;
                if ( dx == 0 )
                    return new Dimension(0, 0);
                parent.stream().forEach(n -> {
                    if ( n instanceof Node )
                    {
                        Point nlocation = ((Node) n).getLocation();
                        int nX = nlocation.x;

                        if ( nX == oldX )
                        {
                            nlocation.translate(shift, 0);
                            ((Node) n).setLocation(nlocation);
                        }
                        else if ( oldX > newX && nX > newX && nX < oldX + width )
                        {
                            nlocation.translate(width, 0);
                            ((Node) n).setLocation(nlocation);
                        }
                        else if ( oldX < newX && nX > oldX && nX < newX )
                        {
                            nlocation.translate(-width, 0);
                            ((Node) n).setLocation(nlocation);
                        }
                        //TODO: move edges
                        //TODO: change "precedes" property
                    }
                });
                return new Dimension(dx, 0);
            }
            else
            {
                if ( newParent != parent )
                {
                    return new Dimension(0, 0);
                }
                return super.move(de, newParent, offset, oldBounds);
            }
        }
        else
            return super.move(de, newParent, offset, oldBounds);
    }

    @Override
    public void moveInCompartment(Node de, Compartment movingCompartment, Dimension offset)
    {
        // TODO Auto-generated method stub
        super.moveInCompartment(de, movingCompartment, offset);
    }
}
