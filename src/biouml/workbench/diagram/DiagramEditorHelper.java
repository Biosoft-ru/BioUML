package biouml.workbench.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionListener;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.workbench.BioUMLApplication;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.undo.DataCollectionUndoListener;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.InsertAction;
import ru.biosoft.graphics.editor.ViewEditorHelper;
import ru.biosoft.graphics.editor.ViewEditorPane;

/** This class is used to provide all necessary changes and validations in diagram model. */
public class DiagramEditorHelper implements ViewEditorHelper, TransactionListener
{
    protected static final Logger log = Logger.getLogger(DiagramEditorHelper.class.getName());

    protected Diagram diagram;
    protected SemanticController semanticController;
    protected ViewEditorPane viewEditor;
    protected DataCollectionUndoListener diagramChangeListener;

    public DiagramEditorHelper(Diagram diagram)
    {
        this.diagram = diagram;
        semanticController = diagram.getType().getSemanticController();
    }

    @Override
    public void register(ViewEditorPane viewEditor)
    {
        this.viewEditor = viewEditor;
        viewEditor.addTransactionListener(this);
    }

    @Override
    public void startTransaction(TransactionEvent te)
    {
        // create special listener that stores all diagram changes event
        // as undoable edit and propagate them to transaction listeners:

        diagramChangeListener = new DataCollectionUndoListener(viewEditor);
        diagram.addDataCollectionListener(diagramChangeListener);
        diagram.addPropertyChangeListener(diagramChangeListener);
    }

    @Override
    public void completeTransaction()
    {
        // remove diagram changes listener
        diagram.removeDataCollectionListener(diagramChangeListener);
        diagram.removePropertyChangeListener(diagramChangeListener);
        diagramChangeListener = null;

        // diagram view will update in DiagramDocument
    }

    //////////////////////////////////////////////
    //  ViewEditorHelper implementation
    //


    /**
     * Should provides necessary changes in view and model if the view can be moved on
     * the specified offset.
     * @returns shift on which the view was moved.
     *
     * @pending find parent compartment if deepaestActive is not DiagramElement
     * Axec: is this even possible?
     * 
     * @pending transaction support and rollback if something break
     */
    @Override
    public Dimension moveView(View view, Dimension offset) throws LoggedException
    {
        Object model = view.getModel();
        if( ! ( model instanceof DiagramElement ) )
            return new Dimension(0, 0);

        DiagramElement de = (DiagramElement)model;
        Compartment parent = de.getCompartment();
        Rectangle rect = view.getBounds();
        
        if( de instanceof Node ) //do not try to move edge into compartment
        {
            Point pt = new Point(rect.x + rect.width / 2 + offset.width, rect.y + +rect.height / 2 + offset.height);
            CompositeView diagramView = viewEditor.getView();
            View parentView = diagramView.getDeepestActive(pt, de.recursiveStream().toArray(), Compartment.class);
            DiagramElement cmpElement = (DiagramElement)parentView.getModel();
            while( ! ( cmpElement instanceof Compartment ) )
            {
                cmpElement = (DiagramElement)cmpElement.getParent();
            }
            parent = (Compartment)cmpElement;
        }
        
        try
        {
            // escape moving parent to child
            if( de instanceof Compartment )
            {
                DataElementPath oldPath = ( (Compartment)de ).getCompletePath();
                DataElementPath newPath = parent.getCompletePath();
                if( newPath.isDescendantOf(oldPath) )
                {
                    if( de.getOrigin() instanceof Compartment )
                    {
                        parent = (Compartment)de.getOrigin();
                    }
                    else
                    {
                        return new Dimension(0, 0);
                    }
                }
            }

            Dimension dimension = semanticController.move(de, parent, offset, rect);
            DiagramElement newDE = parent.get(de.getName());
            if( newDE != null )
                view.setModel( newDE );
            return dimension;
        }
        catch( Throwable t )
        {
            logException( t, de.getName(), parent.getName() );
            throw ExceptionRegistry.translateException( t );
        }
    }

    private void logException(Throwable t, String deName, String parentName)
    {
        String message = null;
        if( t instanceof LoggedException )
            message = t.getMessage();
        else if( DefaultSemanticController.ERROR_NODE_IS_DUPLICATED.equals( t.getMessage() ) )
            message = BioUMLApplication.getMessageBundle().getResourceString( "MESSAGE_NODE_ALREADY_EXIST" );
        else
            message = BioUMLApplication.getMessageBundle().getResourceString( "CANNOT_MOVE_NODE_INTO_PARENT" );

        message = MessageFormat.format( message, new Object[] {deName, parentName} );
        log.log( Level.SEVERE, "Moving view error: " + message, t );

        String title = BioUMLApplication.getMessageBundle().getResourceString( "INFO_TITLE" );
        if( Application.getApplicationFrame() != null )
            ApplicationUtils.messageBox( title, message );
    }

    /**
     * Should provides necessary changes in view and model if the view can be resized.
     * @returns new view size.
     */
    @Override
    public Dimension resizeView(View view, Dimension size) throws LoggedException
    {
        return resizeView( view, size, new Dimension( 0, 0 ) );
    }


    /**
     * resize view and move Node to new location
     * allows to resize up and left without moving inner nodes
     */
    @Override
    public Dimension resizeView(View view, Dimension size, Dimension offset) throws LoggedException
    {
        Object model = view.getModel();
        Rectangle rect = view.getBounds();

        if( model instanceof DiagramElement )
        {
            //TODO: move actual resize to semantic controller and remove resize from here
            size = semanticController.resize( (DiagramElement)model, size, offset );
        }

        Dimension newSize = new Dimension( rect.width + size.width, rect.height + size.height );

        if( model instanceof Node )
        {
            //TODO: think about synchronizing of shape size and viewBounds
            //we take shapeSize here because we should resize core shape for compartments and nodes with variables
            //in some cases shapeSize differs from viewBounds for nodes without children, this may lead to incorrect resizing 
            Dimension shapeSize = ( (Node)model ).getShapeSize();
            if( shapeSize.width > 0 || shapeSize.height > 0 )
                newSize = new Dimension( shapeSize.width + size.width, shapeSize.height + size.height );
            ( (Node)model ).setShapeSize( newSize );

            if( offset.width != 0 || offset.height != 0 )
            {
                Point location = ( (Node)model ).getLocation();
                location.translate( offset.width, offset.height );
                ( (Node)model ).setLocation( location );
            }

            if( model instanceof Compartment )
            {
                Compartment compartment = (Compartment)model;
                diagram.getType().getDiagramViewBuilder().createDiagramView( diagram, ApplicationUtils.getGraphics() );
                viewEditor.setView( (CompositeView)diagram.getView() );

                moveCollection( compartment );
            }

            for( Edge edge : ( (Node)model ).getEdges() )
            {
                semanticController.recalculateEdgePath( edge );
            }
        }

        return newSize;
    }

    private void moveCollection(Compartment compartment) throws LoggedException
    {
        Iterator<DiagramElement> iter = compartment.iterator();
        List<DiagramElement> children = new ArrayList<>();
        while( iter.hasNext() )
        {
            children.add(iter.next());
        }

        for( DiagramElement de : children )
        {
            if( compartment.contains(de) )
            {
                moveView(de.getView(), new Dimension(0, 0));
            }
        }
    }

    /** @returns whether a specified view can be inserted to other view. */
    @Override
    public boolean canAccept(CompositeView composite, View view)
    {
        boolean canAccept = false;
        Object modelToAccept = composite.getModel();
        Object modelToDrop = view.getModel();
        if( modelToAccept instanceof Edge )
        {
            return true;
        }
        if( modelToAccept instanceof Compartment && modelToDrop instanceof DiagramElement && modelToAccept != modelToDrop )
        {
            if( ( (DiagramElement)modelToDrop ).getOrigin() == modelToAccept )
            {
                return true;
            }
            else
            {
                canAccept = semanticController.canAccept((Compartment)modelToAccept, (DiagramElement)modelToDrop);
            }
        }
        return canAccept;
    }


    /** @returns whether a specified view can be resized. */
    @Override
    public boolean isResizable(View view)
    {
        boolean isResizable = false;
        Object model = view.getModel();
        if( model instanceof DiagramElement && semanticController != null )
        {
            isResizable = semanticController.isResizable((DiagramElement)model);
        }
        return isResizable;
    }

    /**
     * Should provides necessary changes in view and model if the view can removed.
     * @returns whether the view was removed.
     * @pending exception processing
     */
    @Override
    public boolean removeView(View view)
    {
        boolean isRemoved = false;
        Object model = view.getModel();
        if( model instanceof DiagramElement )
        {
            try
            {
                return semanticController.remove((DiagramElement)model);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Error during removing diagram element", t);
            }
        }

        return false;
    }

    /**
     * Should provides necessary changes in view and model if the view can be resized.
     * @returns view for the specified object.
     */
    @Override
    public View add(Object obj, Point location)
    {
        DataCollection parent = getOrigin(location);

        try
        {
            if( parent == null )
            {
                parent = diagram;
            }
            DiagramElement de = (DiagramElement)obj;

            if( de.getOrigin().get(de.getName()) != null )
            {
                String name = DefaultSemanticController.generateUniqueNodeName( (Compartment)de.getOrigin(), de.getName() );
                de = de.clone((Compartment)de.getOrigin(), name);
            }

            if( parent != de.getOrigin() )// && location.getX() == 0 && location.getY() == 0)
            {
                de.getOrigin().put(de);
                if( de instanceof Node )
                {
                    moveNodeToLocation( de, location );
                }
            }
            else
            {
                parent.put(de);
                if( de instanceof Node )
                {
                    moveNodeToLocation( de, location );
                }
            }
            if( de instanceof Edge )
            {
                semanticController.move(de, de.getCompartment(), new Dimension(0, 0), null);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Adding an object to location error", e);
        }
        return null;
    }

    private void moveNodeToLocation(DiagramElement de, Point location) throws Exception
    {
        Rectangle oldBounds = null;
        if( de.getView() != null )
        {
            oldBounds = de.getView().getBounds();
        }
        Point oldLocation = ( (Node)de ).getLocation();
        Dimension offset = null;
        if( oldLocation != null )
            offset = new Dimension( location.x - oldLocation.x, location.y - oldLocation.y );
        else
            offset = new Dimension( location.x, location.y );
        semanticController.move( de, de.getCompartment(), offset, oldBounds );
    }

    @Override
    public boolean addEdit(UndoableEdit ue)
    {
        return true;
        //throw new UnsupportedOperationException("addEdit: Not yet implemented");
    }

    public DataCollection getOrigin(Point pt)
    {
        CompositeView diagramView = (CompositeView)diagram.getView();
        if( diagramView == null )
            return null;
        View parentView = diagramView.getDeepestActive(pt);
        DataCollection parent = null;
        if( parentView != null )
        {
            Object obj = parentView.getModel();
            if( obj instanceof DataCollection )
                parent = (DataCollection)obj;
            else
            {
                DataElement de = (DataElement)obj;
                parent = de.getOrigin();
            }
        }
        return parent;
    }

    @Override
    public Object createObject(Object type, Point pt)
    {
        Compartment parent = (Compartment)getOrigin(pt);
        if( parent == null
                || ( type instanceof Class && ( ( (Class<?>)type ).getName().equals( "biouml.standard.type.Stub$DirectedConnection" )
                        || ( (Class<?>)type ).getName().equals( "biouml.standard.type.Stub$UndirectedConnection" ) ) ) )
            parent = diagram;

        DiagramElement diagramElement = semanticController.createInstance( parent, type, pt, viewEditor ).get( 0 );
        if( diagramElement != null )
        {
            if( !semanticController.canAccept(parent, diagramElement) )
            {
                JOptionPane.showMessageDialog(null, "Can not accept diagram element '" + diagramElement.getName() + "' to compartment '"
                        + parent.getName() + "'");
                return null;
            }
        }
        return diagramElement;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Action issues
    //

    @Override
    public Action[] getActions()
    {
        ActionInitializer initializer = new ActionInitializer(biouml.workbench.resources.MessageBundle.class);
        InsertAction selectAction = new InsertAction();
        initializer.initAction(selectAction, "Select");

        return StreamEx.of( diagram.getType().getNodeTypes(), diagram.getType().getEdgeTypes() )
            .nonNull().flatMap( Arrays::stream )
            .mapToEntry( type -> diagram.getType().getDiagramViewBuilder().getIcon(type) )
            .nonNullValues()
            .mapKeyValue( InsertAction::new )
            .prepend( selectAction )
            .toArray( Action[]::new );
    }

    @Override
    public boolean drawOnFly()
    {
        return diagram.getViewOptions().isDrawOnFly();
    }
}
