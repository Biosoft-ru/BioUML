package biouml.plugins.physicell.document;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;

public class PhysicellResultViewPart extends EditorPartSupport
{
    private final PropertyInspectorEx inspector = new PropertyInspectorEx();
    private PhysicellSimulationResult result;
    private PlayAction playAction;
    private PauseAction pauseAction;
    private StopAction stopAction;

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof PhysicellSimulationResult;
    }

    @Override
    public JComponent getView()
    {
        return inspector;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        result = (PhysicellSimulationResult)model;
        inspector.explore( result.getOptions() );
    }

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( playAction == null )
        {
            playAction = new PlayAction();
            actionManager.addAction( PlayAction.KEY, playAction );
            new ActionInitializer( MessageBundle.class ).initAction( playAction, PlayAction.KEY );
            playAction.setEnabled( true );
        }
        if( pauseAction == null )
        {
            pauseAction = new PauseAction();
            actionManager.addAction( PauseAction.KEY, pauseAction );
            new ActionInitializer( MessageBundle.class ).initAction( pauseAction, PauseAction.KEY );
            pauseAction.setEnabled( true );
        }
        if( stopAction == null )
        {
            stopAction = new StopAction();
            actionManager.addAction( StopAction.KEY, stopAction );
            new ActionInitializer( MessageBundle.class ).initAction( stopAction, StopAction.KEY );
            stopAction.setEnabled( true );
        }
        return new Action[] {playAction, pauseAction, stopAction};
    }

    public class PlayAction extends AbstractAction
    {
        public static final String KEY = "Play";

        public PlayAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            result.play();
        }
    }

    public class PauseAction extends AbstractAction
    {
        public static final String KEY = "Update plot";

        public PauseAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            result.pause();
        }
    }

    public class StopAction extends AbstractAction
    {
        public static final String KEY = "Run simulation";

        public StopAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            result.pause();
        }
    }
}
