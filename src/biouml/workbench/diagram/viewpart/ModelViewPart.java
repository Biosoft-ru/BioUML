package biouml.workbench.diagram.viewpart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.swing.table.AbstractRowModel;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramFilter;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.DAEModelUtilities;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.EModel.NodeFilter;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.CreateReactionDialog;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.ReactionInitialProperties;
import biouml.standard.diagram.ReactionPane;
import biouml.standard.type.Reaction;
import biouml.workbench.diagram.SetInitialValuesAction;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.support.DataCollectionRowModelAdapter;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.PluggedEditorsTabbedPane;
import ru.biosoft.gui.TabularPropertiesEditor;
import ru.biosoft.gui.ViewPartSupport;

public class ModelViewPart extends ViewPartSupport implements PropertyChangeListener, DataCollectionListener
{
    private static final Logger log = Logger.getLogger( ModelViewPart.class.getName() );
    private JTabbedPane tabbedPane;
    private EModel emodel;
    protected RemoveParametersAction removeUnusedParametersAction;
    protected DetectParametersAction detectParametersAction;
    protected AddParameterAction addParameterAction;
    protected HighlightAction highlightAction;
    protected HighlightOffAction highlightOffAction;
    protected RemoveSelectedParameterAction removeSelectedParameterAction;
    protected AddToPlotAction addToPlotAction;
    protected SetInitialValuesAction setInitialValuesAction;
    protected AddElementAction addElementAction;
    private HighlightFilter highlightFilter = new HighlightFilter( Color.yellow );
    
    public ModelViewPart()
    {
        tabbedPane = new JTabbedPane( SwingConstants.LEFT );
        add( BorderLayout.CENTER, tabbedPane );
    }

    private void update()
    {
        Object parent = getParent().getParent();
        if( parent instanceof PluggedEditorsTabbedPane )
        {
            PluggedEditorsTabbedPane pane = (PluggedEditorsTabbedPane)parent;
            pane.updateActions();

            Component c = tabbedPane.getSelectedComponent();
            if( c instanceof VariablesTab )
            {
                if( ( (VariablesTab)c ).template instanceof VariableRole )
                {
                    addParameterAction.setEnabled( false );
                    removeSelectedParameterAction.setEnabled( false );
                    removeUnusedParametersAction.setEnabled( false );
                }
                else
                {
                    addParameterAction.setEnabled( true );
                    removeSelectedParameterAction.setEnabled( true );
                    removeUnusedParametersAction.setEnabled( true );

                }
            }
        }
    }

    private void initTabbedPane(EModel emodel)
    {
        Diagram diagram = emodel.getDiagramElement();
        tabbedPane.removeAll();
        tabbedPane.addTab( "Variables", new VariablesTab( emodel.getParameters(), new Variable( "template", null, null ) ) );
        tabbedPane.addTab( "Entities", new VariablesTab( emodel.getEntityRoles(), new VariableRole( "template", null, 1.0 ) ) );
        tabbedPane.addTab( "Compartments", new VariablesTab( emodel.getCompartmentRoles(), new VariableRole( "template", null, 1.0 ) ) );
        tabbedPane.addTab( "Reactions", new ReactionsTab( emodel.getDiagramElement(), new ReactionSimple() ) );
        tabbedPane.addTab( "Equations", new MathTab( emodel, new Equation( null ), new RuleFilter() ) );
        tabbedPane.addTab( "Functions", new MathTab( emodel, new Function( null, "function f() = 0" ), null ));
        tabbedPane.addTab( "Events", new MathTab( emodel, new Event( null ), null ));
        tabbedPane.addTab( "Constraints", new MathTab( emodel, new Constraint( null ), null) );
        tabbedPane.addTab( "Ports", null, new PortsTab( emodel.getDiagramElement(), new PortSimple() ) );
        if( DiagramUtility.isComposite( diagram ) )
        {
            tabbedPane.addTab( "Connections", null, new ConnectionsTab( diagram, new ConnectionSimple() ) );
            tabbedPane.addTab( "Submodels", new SubDiagramTab( diagram, new SubDiagramSimple() ) );
            tabbedPane.addTab( "Buses", new MathTab( emodel, new Bus( "", false ), null ) );
        }
        tabbedPane.addTab( "Units", new UnitsEditor( this.model ).getView() );
        tabbedPane.addChangeListener( new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                update();
            }
        } );
        update();
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( emodel != null )
            emodel.removePropertyChangeListener( this );

        if( model != null )
            ( (Diagram)model ).removeDataCollectionListener( this );

        emodel = ( (Diagram)model ).getRole( EModel.class );
        emodel.addPropertyChangeListener( this );
        ( (Diagram)model ).addDataCollectionListener( this );
        emodel.detectVariableTypes();
        initTabbedPane( emodel );
    }

    @Override
    public boolean canExplore(Object model)
    {
        return ( model instanceof Diagram && ( (Diagram)model ).getRole() instanceof EModel );
    }

    @Override
    public Action[] getActions()
    {
        Component c = tabbedPane.getSelectedComponent();
        ActionManager actionManager = Application.getActionManager();
        if( highlightAction == null )
        {
            highlightAction = new HighlightAction();
            actionManager.addAction( HighlightAction.KEY, highlightAction );
            new ActionInitializer( MessageBundle.class ).initAction( highlightAction, HighlightAction.KEY );
        }
        if( highlightOffAction == null )
        {
            highlightOffAction = new HighlightOffAction();
            actionManager.addAction( HighlightAction.KEY, highlightOffAction );
            new ActionInitializer( MessageBundle.class ).initAction( highlightOffAction, HighlightOffAction.KEY );
        }
        if( c instanceof VariablesTab )
        {
            if( removeUnusedParametersAction == null )
            {
                removeUnusedParametersAction = new RemoveParametersAction();
                actionManager.addAction( RemoveParametersAction.KEY, removeUnusedParametersAction );
                new ActionInitializer( MessageBundle.class ).initAction( removeUnusedParametersAction, RemoveParametersAction.KEY );
            }
            if( addParameterAction == null )
            {
                addParameterAction = new AddParameterAction();
                actionManager.addAction( AddParameterAction.KEY, addParameterAction );
                new ActionInitializer( MessageBundle.class ).initAction( addParameterAction, AddParameterAction.KEY );
            }
            if( detectParametersAction == null )
            {
                detectParametersAction = new DetectParametersAction();
                actionManager.addAction( DetectParametersAction.KEY, detectParametersAction );
                new ActionInitializer( MessageBundle.class ).initAction( detectParametersAction, DetectParametersAction.KEY );
            }
            if( removeSelectedParameterAction == null )
            {
                removeSelectedParameterAction = new RemoveSelectedParameterAction( "parameter" );
                actionManager.addAction( RemoveSelectedParameterAction.KEY, removeSelectedParameterAction );
                new ActionInitializer( MessageBundle.class ).initAction( removeSelectedParameterAction, RemoveSelectedParameterAction.KEY );
            }
            if( addToPlotAction == null )
            {
                addToPlotAction = new AddToPlotAction( "parameter" );
                actionManager.addAction( AddToPlotAction.KEY, addToPlotAction );
                new ActionInitializer( MessageBundle.class ).initAction( addToPlotAction, AddToPlotAction.KEY );
            }
            if( setInitialValuesAction == null )
            {
                setInitialValuesAction = new SetInitialValuesAction( log )
                {
                    @Override
                    protected void setValue(DataElement de, double value)
                    {
                        ( (Variable)de ).setInitialValue( value );
                    }

                    @Override
                    protected Iterator<DataElement> getElementsIterator()
                    {
                        return StreamEx.of( emodel.getVariables().iterator() ).map( p -> (DataElement)p ).iterator();
                    }
                };
                actionManager.addAction( SetInitialValuesAction.KEY, setInitialValuesAction );
                new ActionInitializer( MessageBundle.class ).initAction( setInitialValuesAction, SetInitialValuesAction.KEY );
                setInitialValuesAction.setEnabled( true );
            }
            return new Action[] {addToPlotAction, detectParametersAction, highlightAction, highlightOffAction, setInitialValuesAction,
                    addParameterAction, removeSelectedParameterAction, removeUnusedParametersAction};
        }
        else if( c instanceof MathTab || c instanceof ReactionsTab )
        {
            if( addElementAction == null )
            {
                addElementAction = new AddElementAction();
                actionManager.addAction( AddElementAction.KEY, addElementAction );
                new ActionInitializer( MessageBundle.class ).initAction( addElementAction, AddElementAction.KEY );
            }
            return new Action[] {addElementAction, highlightAction, highlightOffAction};
        }
        else if( c instanceof DiagramTab )
        {
            return new Action[] {highlightAction, highlightOffAction};
        }
        return new Action[0];
    }

    public static class VariablesTab extends TabularPropertiesEditor
    {
        private DataCollection<? extends Variable> dc;
        protected Object template;

        public VariablesTab(DataCollection<? extends Variable> dc, Object template)
        {
            this.dc = dc;
            this.template = template;
            explore( new DataCollectionRowModelAdapter( dc ), template, PropertyInspector.SHOW_USUAL );
            getTable().setAutoResizeMode( JTable.AUTO_RESIZE_NEXT_COLUMN );
            setDefaultNumberFormat( null );
        }

        public void update()
        {
            explore( new DataCollectionRowModelAdapter( dc ), template, PropertyInspector.SHOW_USUAL );
            getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
        }
    }

    public static abstract class DiagramTab<T> extends TabularPropertiesEditor
    {
        protected Diagram diagram;
        private T template;
        public DiagramTab(Diagram diagram, T template)
        {
            this.template = template;
            this.diagram = diagram;
            List<T> list = getObjects();
            explore( new ListRowModel( list, this.template.getClass() ), this.template, PropertyInspector.SHOW_USUAL );
            getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
            setDefaultNumberFormat( null );
        }

        public void update()
        {
            List<T> list = getObjects();
            explore( new ListRowModel( list, template.getClass() ), template, PropertyInspector.SHOW_USUAL );
            getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
        }

        public abstract List<T> getObjects();
    }

    public static class ReactionsTab extends DiagramTab<ReactionSimple>
    {
        public ReactionsTab(Diagram diagram, ReactionSimple template)
        {
            super( diagram, template );
        }

        public List<ReactionSimple> getObjects()
        {
            return StreamEx.of( DiagramUtility.getReactions( diagram ) ).map( r -> new ReactionSimple( r ) ).toList();
        }
    }

    public static class PortsTab extends DiagramTab<PortSimple>
    {
        public PortsTab(Diagram diagram, PortSimple template)
        {
            super( diagram, template );
        }

        public List<PortSimple> getObjects()
        {
            return DiagramUtility.getTopLevelPorts( diagram ).map( n -> new PortSimple( n ) ).toList();
        }
    }

    public static class SubDiagramTab extends DiagramTab<SubDiagramSimple>
    {
        public SubDiagramTab(Diagram diagram, SubDiagramSimple template)
        {
            super( diagram, template );
        }

        public List<SubDiagramSimple> getObjects()
        {
            return DiagramUtility.getSubDiagrams( diagram ).map( n -> new SubDiagramSimple( n ) ).toList();
        }
    }

    public static class ConnectionsTab extends DiagramTab<ConnectionSimple>
    {
        public ConnectionsTab(Diagram diagram, ConnectionSimple template)
        {
            super( diagram, template );
        }

        public List<ConnectionSimple> getObjects()
        {
            return DiagramUtility.getConnections( diagram ).map( n -> new ConnectionSimple( n ) ).toList();
        }
    }

    public static class MathTab extends TabularPropertiesEditor
    {
        private Role template;
        private EModel emodel;
        private Filter<DiagramElement> filter;

        public MathTab(EModel emodel, Role template, Filter<DiagramElement> filter)
        {
            this.template = template;
            this.emodel = emodel;
            this.filter = filter;
            update();
            setDefaultNumberFormat( null );
        }
        
        public Role getTemplate()
        {
            return template;
        }

        public void update()
        {
            List<? extends Role> list = null;
            if( filter == null )
                list = emodel.getChildrenRoles( emodel.getParent(), template.getClass() ).distinct().toList();
            else
                list = emodel.getChildrenRoles( emodel.getParent(), template.getClass(), filter ).distinct().toList();

            explore( new ListRowModel( list, template.getClass() ), template, PropertyInspector.SHOW_USUAL );
            getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
        }
    }

    static class ListRowModel extends AbstractRowModel
    {
        private List<?> roles;
        private Class c;

        public ListRowModel(List<?> roles, Class c)
        {
            this.roles = roles;
            this.c = c;
        }

        @Override
        public int size()
        {
            return roles.size();
        }

        @Override
        public Object getBean(int index)
        {
            return roles.get( index );
        }

        @Override
        public Class<?> getBeanClass()
        {
            return c;
        }
    };

    public class RemoveParametersAction extends AbstractAction
    {
        public static final String KEY = "Remove unused parameters";

        public RemoveParametersAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            emodel.removeNotUsedParameters();
        }
    }

    public class DetectParametersAction extends AbstractAction
    {
        public static final String KEY = "Detect parameter types";

        public DetectParametersAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            emodel.detectVariableTypes();
        }
    }

    public class AddParameterAction extends AbstractAction
    {
        public static final String KEY = "Add parameter";

        public AddParameterAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            String baseName = "parameter";
            int i = 2;
            String name = baseName + "_1";
            while( emodel.containsVariable( name ) )
                name = baseName + "_" + i++;
            emodel.declareVariable( name, Double.valueOf( 0 ) );
        }
    }

    public class HighlightAction extends AbstractAction
    {
        public static final String KEY = "highlight diagram nodes containing selected parameters";
        public HighlightAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            Object component = tabbedPane.getSelectedComponent();
            if( component instanceof TabularPropertyInspector )
            {
                TabularPropertyInspector tab = ( (TabularPropertyInspector)tabbedPane.getSelectedComponent() );
                int[] rowr = tab.getTable().getSelectedRows();
                Set<Object> elements = new HashSet<>();
                if( rowr.length > 0 )
                {
                    for( int i = 0; i < rowr.length; i++ )
                    {
                        Object mod = tab.getModelForRow( rowr[i] );
                        if( mod instanceof Role || mod instanceof Variable || mod instanceof ReactionSimple || mod instanceof PortSimple
                                || mod instanceof ConnectionSimple || mod instanceof SubDiagramSimple || mod instanceof Bus )
                            elements.add( ( mod ) );
                    }
                }
                Diagram diagram = (Diagram)model;
                DiagramUtility.clearHighlight( diagram );
                if( elements.isEmpty() )
                    return;

                List<DiagramElement> des = new ArrayList<DiagramElement>();
                if( tab instanceof VariablesTab )
                {
                    Set<String> names = StreamEx.of( elements ).map( r -> ( (Variable)r ).getName() ).toSet();
                    for( DiagramElement de : diagram.recursiveStream() )
                    {
                        if( DAEModelUtilities.hasVariables( de, names ) )
                            des.add( de );
                    }
                }
                else if( tab instanceof MathTab )
                {
                    if( ( (MathTab)tab ).getTemplate() instanceof Function )
                    {
                        for( DiagramElement de : diagram.recursiveStream() )
                        {
                            if( DAEModelUtilities.hasFunction( de,
                                    StreamEx.of( elements ).select( Function.class ).map( f -> f.getName() ).toSet() ) )
                                des.add( de );
                        }
                    }
                    for( Object role : elements )
                    {
                        if( role instanceof Bus )
                        {
                            for( Node node : ( (Bus)role ).getNodes() )
                            {
                                //                                des.addAll( Arrays.asList( node.getEdges() ) );
                                des.add( node );
                            }
                        }
                        des.add( ( (Role)role ).getDiagramElement() );
                    }
                }
                else if( tab instanceof ReactionsTab )
                {
                    for( Object obj : elements )
                    {
                        Reaction r = ( (ReactionSimple)obj ).getReaction();
                        Object parent = r.getParent();

                        if( parent instanceof Node )
                        {
                            Node n = (Node)parent;
                            des.add(n);
                            //                            for( Edge edge : n.edges() )
                            //                                if( Util.isSpecieReference(edge) )
                            //                                {
                                    //                                    des.add(edge);
                                    //                                    des.add(edge.getOtherEnd(n));
                                    //                                }
                        }
                    }
                }
                else if( tab instanceof ConnectionsTab )
                {
                    for( Object obj : elements )
                        des.add( ( (ConnectionSimple)obj ).connection );
                }
                else if( tab instanceof PortsTab )
                {
                    for( Object obj : elements )
                    {
                        Node node = ( (PortSimple)obj ).getPort();
                        des.add( node );
                        for( Edge edge : node.getEdges() )
                            des.add( edge );
                    }
                }
                else if( tab instanceof SubDiagramTab )
                {
                    for( Object obj : elements )
                        des.add( ( (SubDiagramSimple)obj ).getSubDiagram() );
                }
                DiagramUtility.highlight( des );
                highlightFilter.setEnabled( true );
                if( !DiagramUtility.hasFilter( diagram, highlightFilter ) )
                    DiagramUtility.addFilter( diagram, highlightFilter );
            }
            document.update();
        }
    }

    public class HighlightOffAction extends AbstractAction
    {
        public static final String KEY = "clear diagram highlight";
        public HighlightOffAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            Diagram diagram = (Diagram)model;
            DiagramUtility.clearHighlight( diagram );
            document.update();
        }
    }

    public class RemoveSelectedParameterAction extends AbstractAction
    {
        public static final String KEY = "remove selected";
        private String type = "parameter";

        public RemoveSelectedParameterAction(String type)
        {
            super( KEY );
            this.type = type;
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            VariablesTab tab = ( (VariablesTab)tabbedPane.getSelectedComponent() );
            int[] rowr = tab.getTable().getSelectedRows();
            Set<Variable> vars = new HashSet<>();
            if( rowr.length > 0 )
            {
                for( int i = 0; i < rowr.length; i++ )
                {
                    Object mod = tab.getModelForRow( rowr[i] );
                    if( mod != null && mod instanceof Variable )
                        vars.add( ( (Variable)mod ) );
                }
            }
            if( vars.size() > 0 )
            {
                Diagram diagram = (Diagram)model;
                for( Variable var : vars )
                {
                    if( var instanceof VariableRole )
                    {
                        DiagramElement[] nodes = ( (VariableRole)var ).getAssociatedElements();
                        for( DiagramElement de : nodes )
                        {
                            try
                            {
                                diagram.getType().getSemanticController().remove( de );
                            }
                            catch( Exception e1 )
                            {
                                log.log( Level.INFO, "Can not remove element " + de.getName(), e1 );
                            }
                        }
                    }

                    else
                    {
                        String variableName = var.getName();
                        try
                        {
                            emodel.getVariables().remove( variableName );
                        }
                        catch( Exception ex )
                        {
                            log.info( "Can not remove " + type + " " + variableName );
                        }
                    }
                }
                highlightFilter.setEnabled( false );
                document.update();
            }
        }
    }

    public class AddToPlotAction extends AbstractAction
    {
        public static final String KEY = "add to plot";

        public AddToPlotAction(String type)
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            VariablesTab tab = ( (VariablesTab)tabbedPane.getSelectedComponent() );
            int[] rowr = tab.getTable().getSelectedRows();

            Diagram diagram = (Diagram)model;
            PlotsInfo plotsInfo = DiagramUtility.getPlotsInfo( diagram );
            //try to guess which plot info is desireable
            PlotInfo plotInfo = null;
            if( plotsInfo.getPlots().length >= 1 )
                plotInfo = plotsInfo.getPlots()[0];
            else if( plotsInfo.getPlots().length == 0 )
            {
                plotInfo = new PlotInfo( diagram.getRole( EModel.class ) );
                plotsInfo.setPlots( new PlotInfo[] {plotInfo} );
            }

            Curve[] curves = plotInfo.getYVariables();
            Set<String> alreadyExisting = StreamEx.of( curves ).map( curve -> curve.getName() ).toSet();

            Set<Variable> vars = IntStreamEx.of( rowr ).mapToObj( row -> tab.getModelForRow( row ) ).select( Variable.class ).toSet();

            List<Curve> addCurves = new ArrayList<>();
            for( Variable var : vars )
            {
                if( alreadyExisting.contains( var.getName() ) )
                    continue;

                addCurves.add( new Curve( "", var.getName(), var.getTitle(), diagram.getRole( EModel.class ) ) );
            }
            Curve[] newCurves = new Curve[curves.length + addCurves.size()];
            System.arraycopy( curves, 0, newCurves, 0, curves.length );

            for( int i = 0; i < addCurves.size(); i++ )
                newCurves[curves.length + i] = addCurves.get( i );
            plotInfo.setYVariables( newCurves );
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        Component component = tabbedPane.getSelectedComponent();
        if( component instanceof VariablesTab )
            ( (VariablesTab)component ).update();
        else if (component instanceof MathTab)
            ( (MathTab)component ).update();
        else if( component instanceof ReactionsTab )
            ( (ReactionsTab)component ).update();
    }
    

    public class AddElementAction extends AbstractAction
    {
        public static final String KEY = "Add new element";

        public AddElementAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            Diagram diagram = emodel.getParent();
            Component c = tabbedPane.getSelectedComponent();
            if( c instanceof MathTab )
            {
                MathTab tab = (MathTab)tabbedPane.getSelectedComponent();
                DiagramElementGroup deg = diagram.getType().getSemanticController().createInstance(diagram, tab.getTemplate().getClass(),
                        new Point(), tab.getTemplate());
                Node newNode = deg.nodesStream().findAny().orElse(null);

                if( newNode != null )
                {
                    diagram.put(newNode);
                    layoutElement(diagram, newNode);
                }
                document.update();
                ((MathTab)c).update();
            }
            else if (c instanceof ReactionsTab)
            {
                Object properties = diagram.getType().getSemanticController().getPropertiesByType(diagram, Reaction.class, new Point());
                ViewPane pane = ModelViewPart.this.document.getViewPane();
                ViewEditorPane editorPane = (ViewEditorPane)pane;
                new CreateReactionDialog(Application.getApplicationFrame(), new ReactionPane(diagram, diagram, new Point(), editorPane, (ReactionInitialProperties)properties));
                ( (ReactionsTab)c ).update();
            }
        }
    }

    private void layoutElement(Diagram diagram, Node newNode)
    {
        diagram.getType().getDiagramViewBuilder().createDiagramView( diagram, ApplicationUtils.getGraphics() );
        int maxX = 0;
        int maxY = 0;
        for( Node n : diagram.stream().select( Node.class ) )
        {
            maxX = Math.max( n.getLocation().x + (int)n.getView().getBounds().getWidth(), maxX );
            maxY = Math.max( n.getLocation().y + (int)n.getView().getBounds().getHeight(), maxY );
        }
        if( maxX > maxY )
            newNode.setLocation( 0, maxY + 10 );
        else
            newNode.setLocation( maxX + 10, 0 );
    }
    
    public static class RuleFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            Equation role = de.getRole( Equation.class );
            return ! ( role.getDiagramElement().getKernel() instanceof Reaction ) && ! ( role.getDiagramElement() instanceof Edge );
        }
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        Component component = tabbedPane.getSelectedComponent();
        if (component instanceof MathTab)
            ( (MathTab)component ).update();
        else if( component instanceof ReactionsTab )
            ( (ReactionsTab)component ).update();
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        Component component = tabbedPane.getSelectedComponent();
        if (component instanceof MathTab)
            ( (MathTab)component ).update();
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub
        
    }

}