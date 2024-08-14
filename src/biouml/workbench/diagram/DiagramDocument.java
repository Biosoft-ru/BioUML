package biouml.workbench.diagram;

import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.print.PrintAction;
import com.developmentontheedge.print.PrintPreviewAction;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramFilter;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Module;
import biouml.model.SemanticController;
import biouml.standard.state.State;
import biouml.standard.state.StateUndoManager;
import biouml.workbench.graph.DiagramToGraphTransformer;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.DataElementImportTransferHandler;
import ru.biosoft.access.subaction.DynamicActionFactory;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.DocumentViewAccessProvider;
import ru.biosoft.gui.RedoAction;
import ru.biosoft.gui.SaveDocumentAction;
import ru.biosoft.gui.UndoAction;
import ru.biosoft.gui.ZoomInAction;
import ru.biosoft.gui.ZoomOutAction;

@ClassIcon ( "resources/diagramDocument.gif" )
public class DiagramDocument extends Document implements PropertyChangeListener
{
    protected DocumentViewAccessProvider diagramViewAccessProvider = null;

    public DiagramDocument(Diagram diagram, DocumentViewAccessProvider diagramViewAccessProvider)
    {
        this(diagram, diagramViewAccessProvider, true);
    }

    public DiagramDocument(Diagram diagram)
    {
        this(diagram, DocumentManager.getDocumentViewAccessProvider(), true);
    }

    public DiagramDocument(Diagram diagram, boolean isMutable)
    {
        this(diagram, DocumentManager.getDocumentViewAccessProvider(), isMutable);
    }

    public DiagramDocument(Diagram diagram, DocumentViewAccessProvider diagramViewAccessProvider, boolean isMutable)
    {
        super(diagram);

        this.diagramViewAccessProvider = diagramViewAccessProvider;

        if( diagram != null )
        {
            if( ( diagram.getOrigin() != null ) && ( diagram.getPathLayouter() == null ) )
            {
                DataCollectionInfo info = diagram.getOrigin().getInfo();
                if( info.getProperties() != null )
                {
                    String layouterName = info.getProperty(Diagram.DEFAULT_LAYOUTER);
                    if( layouterName != null )
                    {
                        String pluginNames = info.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
                        try
                        {
                            Class<? extends Layouter> layouterClass = ClassLoading.loadSubClass( layouterName, pluginNames, Layouter.class );
                            boolean notificationEnabled = diagram.isNotificationEnabled();
                            diagram.setNotificationEnabled(false);
                            diagram.setPathLayouter(layouterClass.newInstance());
                            diagram.setNotificationEnabled(notificationEnabled);
                        }
                        catch( Throwable t )
                        {
                            log.log(Level.SEVERE, "Can not load default layouter", t);
                        }
                    }
                }
            }
            DiagramToGraphTransformer.layoutIfNeeded(diagram);

            DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
            Graphics gg = Application.getApplicationFrame().getGraphics();
            gg.translate(X_VIEWPANE_OFFSET, Y_VIEWPANE_OFFSET);
            CompositeView diagramView = builder.createDiagramView(diagram, gg);
            DiagramFilter[] filterList = diagram.getFilterList();
            for( DiagramFilter filter : filterList )
            {
                if( filter != null && filter.isEnabled() )
                    filter.apply( diagram );
            }

            // create view pane
            //if( !diagram.getOrigin().isMutable() )
            //    viewPane = new ViewPane();
            //else
            if( !isMutable )
                viewPane = new ViewPane();

            else
            {
                viewPane = new ViewEditorPane( new DiagramEditorHelper( diagram ) );
                ((ViewEditorPane)viewPane).addTransactionListener(undoManager);
                ( (ViewEditorPane)viewPane ).getPanel().setTransferHandler(
                        new DataElementImportTransferHandler( (path, point) -> {
                            Diagram diagram1 = getDiagram();
                            DiagramEditorHelper helper = new DiagramEditorHelper( diagram1 );
                            SemanticController semanticController = diagram1.getType().getSemanticController();
                            Compartment parent = (Compartment)helper.getOrigin( point );
                            if( parent == null )
                                parent = diagram1;
                            try
                            {
                                DiagramElementGroup diagramElements = semanticController.addInstanceFromElement( parent,
                                        path.getDataElement(), point, (ViewEditorPane)viewPane );
                                if( diagramElements.getElement() == null )
                                    throw new Exception( "Element is not supported" );
                            }
                            catch( Exception e )
                            {
                                log.log( Level.SEVERE, e.getMessage(), e );
                                JOptionPane.showMessageDialog( null,
                                        "Cannot add element '" + path + "' of type " + DataCollectionUtils.getElementType( path )
                                                + ": " + e.getMessage() );
                                return false;
                            }
                            return true;
                        } ) );
            }
            viewPane.setView(diagramView);
            viewPane.addViewPaneListener(diagramViewAccessProvider.getDocumentViewListener());


            viewPane.addFocusListener(new FocusAdapter()
            {
                @Override
                public void focusGained(FocusEvent e)
                {
                    DocumentManager.setActiveDocument(DiagramDocument.this, viewPane);
                }
            });

            viewPane.setGridOptions(diagram.getViewOptions().getGridOptions());

            if( diagram.getLabelLayouter() != null )
            {
                DiagramToGraphTransformer.layoutLabels(diagram);
                update();
            }

            viewPane.setPopupActionsProvider(new PopupActionsProvider());

            diagram.addPropertyChangeListener(this);

            //temporary lock for KEGG
            //refreshEdges();
        }
    }

    // //////////////////////////////////////////////////////////////////////////
    // Properties
    //

    public Diagram getDiagram()
    {
        applyEditorChanges();
        return (Diagram)getModel();
    }

    @Override
    public String getDisplayName()
    {
        Diagram diagram = getDiagram();
        Module module = Module.optModule(diagram);
        if( module != null )
        {
            return module.getName() + " : " + diagram.getName();
        }
        return diagram.getName();
    }

    @Override
    public List<DataElement> getSelectedItems()
    {
        List<DataElement> results = new ArrayList<>();
        Object[] objs = viewPane.getSelectionManager().getSelectedModels();
        for (Object obj: objs)
            if (obj instanceof DataElement)
        results.add( (DataElement)obj );
        return results;
    }

    private static boolean actionInitialized = false;
    @Override
    public Action[] getActions(ActionType actionType)
    {
        Action[] actions = getActionsByStaticWay(actionType);
        if(actionType == ActionType.TOOLBAR_ACTION)
        {
            return StreamEx.of( actions ).append( (Action)null ).append( DynamicActionFactory.getEnabledActions( getDiagram() ) )
                    .toArray( Action[]::new );
        }
        return actions;
    }
    public static Action[] getActionsByStaticWay(ActionType actionType)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;

            ActionInitializer initializer = new ActionInitializer(biouml.workbench.resources.MessageBundle.class);
            
            Action action = new ViewOptionsAction(true);
            actionManager.addAction(ViewOptionsAction.KEY, action);
            initializer.initAction(action, ViewOptionsAction.KEY);

            //toolbar actions
            action = new UndoAction(true);
            actionManager.addAction(UndoAction.KEY, action);
            initializer.initAction(action, UndoAction.KEY);

            action = new RedoAction(true);
            actionManager.addAction(RedoAction.KEY, action);
            initializer.initAction(action, RedoAction.KEY);

            action = new ZoomInAction(true);
            actionManager.addAction(ZoomInAction.KEY, action);
            initializer.initAction(action, ZoomInAction.KEY);

            action = new ZoomOutAction(true);
            actionManager.addAction(ZoomOutAction.KEY, action);
            initializer.initAction(action, ZoomOutAction.KEY);

            action = new FitToScreenAction( true );
            actionManager.addAction( FitToScreenAction.KEY, action );
            initializer.initAction( action, FitToScreenAction.KEY );
        }
        if( actionType == ActionType.MENU_ACTION )
        {
            return new Action[] {};
        }
        else if( actionType == ActionType.TOOLBAR_ACTION )
        {
            Action undoAction = actionManager.getAction(UndoAction.KEY);
            Action redoAction = actionManager.getAction(RedoAction.KEY);
            Action zoomInAction = actionManager.getAction(ZoomInAction.KEY);
            Action zoomOutAction = actionManager.getAction(ZoomOutAction.KEY);
            Action fitToScreenAction = actionManager.getAction( FitToScreenAction.KEY );
            Action convertDiagramAction = actionManager.getAction(ConvertDiagramAction.KEY);
            Action viewOptionsAction = actionManager.getAction(ViewOptionsAction.KEY);
            return new Action[] {undoAction, redoAction, zoomInAction, zoomOutAction, fitToScreenAction, null, convertDiagramAction, null,
                    viewOptionsAction};
        }
        else if( actionType == ActionType.ENABLED_ACTIONS )
        {
            Action printAction = actionManager.getAction(PrintAction.KEY);
            Action printPreviewAction = actionManager.getAction(PrintPreviewAction.KEY);

            return new Action[] {printAction, printPreviewAction};
        }
        return null;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Life cycle events
    //

    @Override
    public void setActive(boolean isActive)
    {
        super.setActive(isActive);

        if( isActive )
            diagramViewAccessProvider.updateSelection(getViewPane());
    }

    @Override
    public void save()
    {
        Diagram diagram = getDiagram();
        if( !ModelDefinition.isDefindInModelDefinition(diagram) )
            try
            {
                State currentState = diagram.getCurrentState();
                diagram.restore();
                CollectionFactoryUtils.save(diagram);
                if( currentState != null )
                {
                    diagram.setStateEditingMode(currentState);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Saving diagram error", e);
            }

        super.save();
    }

    @Override
    public void close()
    {
        Diagram diagram = getDiagram();
        diagram.removePropertyChangeListener(this);

        viewPane.removeViewPaneListener(diagramViewAccessProvider.getDocumentViewListener());
        if( viewPane instanceof ViewEditorPane )
            ( (ViewEditorPane)viewPane ).removeTransactionListener(undoManager);

        if( diagram.getOrigin() != null )
        {
            diagram.getOrigin().release(diagram.getName());
        }

        super.close();
    }

    // //////////////////////////////////////////////////////////////////////////
    // Update issues

    @Override
    protected synchronized void doUpdate()
    {
        //long start = System.currentTimeMillis();
        final Diagram diagram = getDiagram();
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();

        Graphics gg = Application.getApplicationFrame().getGraphics();
        gg.translate(X_VIEWPANE_OFFSET, Y_VIEWPANE_OFFSET);
        builder.createDiagramView(diagram, gg);

        DiagramFilter[] filterList = diagram.getFilterList();
        for( DiagramFilter filter : filterList )
        {
            if( filter != null && filter.isEnabled() )
                filter.apply( diagram );
        }

        CompositeView diagramView = (CompositeView)diagram.getView();
        viewPane.setView(diagramView);

        //System.out.println("Diagram update, time=" + ( System.currentTimeMillis() - start ));
    }
    @Override
    public void updateViewPane()
    {
        if( viewPane instanceof ViewEditorPane )
        {
            Diagram diagram = getDiagram();
            ( (ViewEditorPane)viewPane ).fillToolbar(new DiagramEditorHelper(diagram));
        }
    }

    /**
     * @todo filtering events for diagram update
     * @pending if filter is changed we should only reapply the filter and not
     *          rebuild the diagram
     */
    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if( log.isLoggable( Level.FINE ) )
            log.log(Level.FINE, "Property changed: " + pce.getPropertyName() + ", propagated by " + pce.getPropagationId());

        if( pce.getPropagationId() instanceof DiagramFilter )
        {
            Diagram diagram = getDiagram();
            DiagramFilter filter = diagram.getFilter();
            if( filter != null )
            {
                filter.restoreView(diagram);

                if( filter.isEnabled() )
                    filter.apply(diagram);
            }

            viewPane.repaint();
            return;
        }

        // do not repaint during transaction
        // because of selection is missing in this case
        if( canUpdate() )
            update();

        if( Document.getActiveDocument() == this )
        {
            if( isMutable() )
            {
                Application.getActionManager().enableActions( true, SaveDocumentAction.KEY );
            }
        }
    }

    public boolean canUpdate()
    {
        Diagram diagram = (Diagram)getModel();
        if( diagram.getCurrentState() != null )
        {
            StateUndoManager stUndoManager = diagram.getCurrentState().getStateUndoManager();
            if( stUndoManager.isRedo() || stUndoManager.isUndo() )
                return false;
        }

        return !undoManager.hasTransaction() && !undoManager.isUndo() && !undoManager.isRedo();
    }

    @Override
    public boolean isMutable()
    {
        Diagram diagram = (Diagram)getModel();
        if( ( diagram != null ) && ( diagram.getOrigin() != null ) )
        {
            return diagram.getOrigin().isMutable();
        }
        return true;
    }

    /**
     * Updates edges points
     */
    public void refreshEdges()
    {
        Diagram diagram = getDiagram();
        boolean listenerEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled(false);
        updateEdgeConnections(diagram, diagram);
        diagram.setNotificationEnabled(listenerEnabled);
        update();
    }

    /**
     * Refresh edge start and finish points using SemanticController.move method
     */
    protected void updateEdgeConnections(Diagram diagram, Compartment compartment)
    {
        compartment.recursiveStream().select( Edge.class ).forEach( diagram.getType().getSemanticController()::recalculateEdgePath );
    }

    static void updateDiagram(ViewPane viewPane, DiagramElement de)
    {
        Diagram diagram = Diagram.getDiagram(de);
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();

        Graphics gg = Application.getApplicationFrame().getGraphics();
        gg.translate(X_VIEWPANE_OFFSET, Y_VIEWPANE_OFFSET);
        builder.createDiagramView(diagram, gg);

        DiagramFilter[] filterList = diagram.getFilterList();
        for( DiagramFilter filter : filterList )
        {
            if( filter != null && filter.isEnabled() )
                filter.apply( diagram );
        }

        CompositeView diagramView = (CompositeView)diagram.getView();
        viewPane.setView(diagramView);
    }

    /**
     * Method returns view pane actually containing diagram
     * It can be different from viewPane (see CompositeDiagramDocument})
     */
    public ViewPane getDiagramViewPane()
    {
        return viewPane;
    }
    
    // ///////////////////////////////////////////////////////////////////////////////////////////
    // Refactor or remove
    //
    public static final int X_VIEWPANE_OFFSET = 10;

    public static final int Y_VIEWPANE_OFFSET = 10;
}
