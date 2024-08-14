package biouml.workbench.diagram;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub.ConnectionPort;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.ActionsProvider;

import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;

public class PopupActionsProvider implements ActionsProvider
{
    private final ActionManager actionManager;

    public PopupActionsProvider()
    {
        actionManager = Application.getActionManager();

        ActionInitializer initializer = new ActionInitializer(MessageBundle.class);

        Action action = new LineVertexTypeAction();
        actionManager.addAction( LineVertexTypeAction.KEY, action );
        initializer.initAction( action, LineVertexTypeAction.KEY );

        action = new QuadricVertexTypeAction();
        actionManager.addAction( QuadricVertexTypeAction.KEY, action );
        initializer.initAction( action, QuadricVertexTypeAction.KEY );

        action = new CubicVertexTypeAction();
        actionManager.addAction( CubicVertexTypeAction.KEY, action );
        initializer.initAction( action, CubicVertexTypeAction.KEY );

        action = new RemoveVertexAction();
        actionManager.addAction( RemoveVertexAction.KEY, action );
        initializer.initAction( action, RemoveVertexAction.KEY );

        action = new AddVertexAction();
        actionManager.addAction( AddVertexAction.KEY, action );
        initializer.initAction( action, AddVertexAction.KEY );

        action = new StraightenEdgeAction();
        actionManager.addAction( StraightenEdgeAction.KEY, action );
        initializer.initAction( action, StraightenEdgeAction.KEY );

        action = new SnapEdgeAction();
        actionManager.addAction( SnapEdgeAction.KEY, action );
        initializer.initAction( action, SnapEdgeAction.KEY );

        action = new RotateNodeAction();
        actionManager.addAction( RotateNodeAction.KEY, action );
        initializer.initAction( action, RotateNodeAction.KEY );

        action = new EditElementAction();
        actionManager.addAction( EditElementAction.KEY, action );
        initializer.initAction( action, EditElementAction.KEY );

        action = new CopyNodeAction();
        actionManager.addAction( CopyNodeAction.KEY, action );
        initializer.initAction( action, CopyNodeAction.KEY );
        
        action = new PinElementAction();
        actionManager.addAction( PinElementAction.KEY, action );
        initializer.initAction( action, PinElementAction.KEY );
        
        action = new UnpinElementAction();
        actionManager.addAction( UnpinElementAction.KEY, action );
        initializer.initAction( action, UnpinElementAction.KEY );
    }

    @Override
    public Action[] getActions(Object obj)
    {
        if( obj instanceof ViewPaneEvent )
        {
            ViewPaneEvent e = (ViewPaneEvent)obj;
            if( e.getViewPane() instanceof ViewEditorPane )
            {
                Point location = new Point( e.getPoint() );
                Object model = e.getViewSource().getModel();
                if( model instanceof DiagramElement )
                {
                    List<Action> actions = new ArrayList<>();
                    Action action = actionManager.getAction(EditElementAction.KEY);
                    action.putValue(EditElementAction.DIAGRAM_ELEMENT, model);
                    action.putValue(EditElementAction.VIEWPANE, e.getViewPane());
                    actions.add(action);

                    action = actionManager.getAction(PinElementAction.KEY);
                    action.putValue(PinElementAction.DIAGRAM_ELEMENT, model);
                    actions.add(action);
                    
                    action = actionManager.getAction(UnpinElementAction.KEY);
                    action.putValue(UnpinElementAction.DIAGRAM_ELEMENT, model);
                    actions.add(action);
                    
                    if( ( model instanceof Edge ) && ( e.getViewSource() instanceof ArrowView ) )
                    {
                        ArrowView view = (ArrowView)e.getViewSource();
                        Point pathOffset = view.getPathOffset();
                        SimplePath path = view.getPath();
                        if( path != null )
                        {
                            int point = -1;
                            for( int i = 1; i < path.npoints - 1; i++ )
                            {
                                if( Math.abs(path.xpoints[i] - location.x + pathOffset.x) < 5
                                        && Math.abs(path.ypoints[i] - location.y + pathOffset.y) < 5 )
                                {
                                    point = i;
                                    break;
                                }
                            }

                            if( point != -1 )
                            {
                                Action action1 = actionManager.getAction(LineVertexTypeAction.KEY);
                                action1.putValue(LineVertexTypeAction.VIEWPANE, e.getViewPane());
                                action1.putValue(LineVertexTypeAction.EDGE, model);
                                action1.putValue(LineVertexTypeAction.POINT, point);
                                action1.putValue(LineVertexTypeAction.TYPE, path.pointTypes[point]);

                                Action action2 = actionManager.getAction(QuadricVertexTypeAction.KEY);
                                action2.putValue(QuadricVertexTypeAction.VIEWPANE, e.getViewPane());
                                action2.putValue(QuadricVertexTypeAction.EDGE, model);
                                action2.putValue(QuadricVertexTypeAction.POINT, point);
                                action2.putValue(QuadricVertexTypeAction.TYPE, path.pointTypes[point]);

                                Action action3 = actionManager.getAction(CubicVertexTypeAction.KEY);
                                action3.putValue(CubicVertexTypeAction.VIEWPANE, e.getViewPane());
                                action3.putValue(CubicVertexTypeAction.EDGE, model);
                                action3.putValue(CubicVertexTypeAction.POINT, point);
                                action3.putValue(CubicVertexTypeAction.TYPE, path.pointTypes[point]);

                                Action removeAction = actionManager.getAction(RemoveVertexAction.KEY);
                                removeAction.putValue(RemoveVertexAction.VIEWPANE, e.getViewPane());
                                removeAction.putValue(RemoveVertexAction.EDGE, model);
                                removeAction.putValue(RemoveVertexAction.POINT, point);

                                actions.add(null);
                                actions.add(action1);
                                actions.add(action2);
                                actions.add(action3);
                                actions.add(null);
                                actions.add(removeAction);
                            }
                            else
                            {
                                action = actionManager.getAction(AddVertexAction.KEY);
                                action.putValue(AddVertexAction.VIEWPANE, e.getViewPane());
                                action.putValue(AddVertexAction.EDGE, model);
                                action.putValue(AddVertexAction.POINT, new Point(location.x - pathOffset.x, location.y - pathOffset.y));
                                actions.add(null);
                                actions.add(action);
                            }

                            if( path.npoints > 2 )
                            {
                                Action straightenAction = actionManager.getAction(StraightenEdgeAction.KEY);
                                straightenAction.putValue(StraightenEdgeAction.VIEWPANE, e.getViewPane());
                                straightenAction.putValue(StraightenEdgeAction.EDGE, model);
                                actions.add(straightenAction);

                                Action alignAction = actionManager.getAction(SnapEdgeAction.KEY);
                                alignAction.putValue(SnapEdgeAction.VIEWPANE, e.getViewPane());
                                alignAction.putValue(SnapEdgeAction.EDGE, model);
                                actions.add(alignAction);
                            }
                        }
                    }
                    else if( model instanceof Node )
                    {
                        if( ( (Node)model ).getAttributes().getProperty(ConnectionPort.PORT_ORIENTATION) != null )
                        {
                            action = actionManager.getAction(RotateNodeAction.KEY);
                            action.putValue(RotateNodeAction.NODE, model);
                            action.putValue(AddVertexAction.VIEWPANE, e.getViewPane());
                            actions.add(null);
                            actions.add(action);
                        }
                        if( ! ( ( (Node)model ).getKernel() instanceof Reaction ) )
                        {
                            action = actionManager.getAction( CopyNodeAction.KEY );
                            action.putValue( CopyNodeAction.NODE, model );
                            action.putValue( CopyNodeAction.VIEWPANE, e.getViewPane() );
                            actions.add( action );
                        }
                    }
                    return actions.toArray(new Action[actions.size()]);
                }
            }
        }
        return null;
    }
}
