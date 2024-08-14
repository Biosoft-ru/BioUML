package biouml.standard.state;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.gui.Document;
import ru.biosoft.util.ApplicationUtils;

import com.developmentontheedge.application.action.ActionInitializer;

import biouml.model.Diagram;

public class StatesPane extends JPanel implements StateTabListener
{
    public static final String APPLY = "apply";
    public static final String ADD = "add";
    public static final String REMOVE = "remove";
    public static final String REMOVE_TRANSACTION = "remove_transaction";

    protected ApplyAction applyAction = new ApplyAction(APPLY);
    protected AddAction addAction = new AddAction(ADD);
    protected RemoveAction removeAction = new RemoveAction(REMOVE, this);
    protected RemoveTransactionAction removeTransactionAction = new RemoveTransactionAction(REMOVE_TRANSACTION, this);
    protected Action[] actions;

    protected Diagram diagram;
    protected Document document;

    protected JTabbedPane tabbedPane;
    protected StatesTab statesTab;
    protected ChangesTab changesTab;

    public static final String STATES = "States";
    public static final String CHANGES = "Changes";

    protected MessageBundle messageBundle = new MessageBundle();

    protected State currentState = null;

    public StatesPane()
    {
        setLayout(new BorderLayout());

        statesTab = new StatesTab(this);
        changesTab = new ChangesTab(this);

        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.add(STATES, statesTab);
        tabbedPane.add(CHANGES, changesTab);

        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if( index == 1 )
            {
                changesTab.changeState(currentState);
            }
        });

        initListeners();
    }

    protected void initListeners()
    {
        diagramListener = evt -> {
            if( evt.getPropertyName().equals("currentStateName") )
            {
                if( diagram.getCurrentState() == null )
                {
                    applyAction.setSelected(false);
                }
                else
                {
                    if (!diagram.containState( diagram.getCurrentState() )) //state may be set but is not in the states list
                        return;
         
                    setCurrentState(diagram.getCurrentState());
                    applyAction.setSelected(true);
                }
            }
            else if (evt.getPropertyName().equals( "states" ))
            {
               statesTab.refresh( diagram );
               changesTab.refresh( diagram );
            }
        };
    }

    private PropertyChangeListener diagramListener;
    public void explore(Diagram diagram, Document document)
    {
        this.diagram = diagram;
        this.document = document;
        statesTab.refresh(diagram);
        changesTab.refresh(diagram);

        currentState = diagram.getCurrentState();
        if( currentState == null )
        {
            applyAction.setSelected(false);
        }
        else
        {
            applyAction.setSelected(true);
        }

        diagram.removePropertyChangeListener(diagramListener);
        diagram.addPropertyChangeListener(diagramListener);
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(applyAction, APPLY);
            initializer.initAction(addAction, ADD);
            initializer.initAction(removeAction, REMOVE);
            initializer.initAction(removeTransactionAction, REMOVE_TRANSACTION);

            actions = new Action[] {applyAction, addAction, removeAction, removeTransactionAction};
        }

        return actions;
    }

    public boolean applyCurrentState()
    {
        if( currentState != null )
        {
            diagram.setStateEditingMode(currentState, (ViewEditorPane)document.getViewPane());
            document.update();
            return true;
        }
        return false;
    }

    public void restoreDiagram()
    {
        diagram.restore();
        document.update();
    }

    @Override
    public void setCurrentState(State state)
    {
        this.currentState = state;
        statesTab.changeState(state);
        changesTab.changeState(state);
    }
    @Override
    public State getCurrentState()
    {
        return currentState;
    }

    class ApplyAction extends AbstractAction
    {
        protected boolean selected = false;
        public ApplyAction(String name)
        {
            super(name);
        }

        public void setSelected(boolean selected)
        {
            this.selected = selected;
            refreshButton(selected);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( selected )
            {
                restoreDiagram();
                selected = false;
            }
            else
            {
                if( applyCurrentState() )
                {
                    selected = true;
                }
            }
            refreshButton(selected);
        }

        protected void refreshButton(boolean selected)
        {
            String iconString;
            String descString;
            if( selected )
            {
                iconString = messageBundle.getResourceString(StatesPane.APPLY + Action.SMALL_ICON + "2");
                descString = messageBundle.getResourceString(StatesPane.APPLY + Action.SHORT_DESCRIPTION + "2");
            }
            else
            {
                iconString = messageBundle.getResourceString(StatesPane.APPLY + Action.SMALL_ICON);
                descString = messageBundle.getResourceString(StatesPane.APPLY + Action.SHORT_DESCRIPTION);
            }
            this.putValue(AbstractAction.SHORT_DESCRIPTION, descString);
            URL url = getClass().getResource("resources/" + iconString);
            if( url != null )
            {
                this.putValue(AbstractAction.SMALL_ICON, ApplicationUtils.getImageIcon(url));
            }
        }
    }

    class AddAction extends AbstractAction
    {
        public AddAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            NewStateDialog dialog = new NewStateDialog("New state", diagram);
            if( dialog.doModal() )
            {
                State newState = dialog.getNewDiagramElement();
                diagram.addState(newState);
                statesTab.refresh(diagram);
                changesTab.refresh(diagram);
                if(diagram.getCurrentState() == null)
                {
                    applyAction.setSelected(true);
                    setCurrentState(newState);
                    applyCurrentState();
                }
            }
        }
    }

    class RemoveAction extends AbstractAction
    {
        protected StatesPane statesPane;
        public RemoveAction(String name, StatesPane statesPane)
        {
            super(name);
            this.statesPane = statesPane;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( currentState != null )
            {
                if( JOptionPane.showConfirmDialog(statesPane, "Do you really want to delete state '" + currentState.getName()
                        + "'", "State deleting...", JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION )
                {
                    if( diagram.getCurrentState() == currentState )
                    {
                        restoreDiagram();
                        applyAction.setSelected(false);
                    }
                    diagram.removeState(currentState);
                    currentState = null;
                    statesTab.refresh(diagram);
                    changesTab.refresh(diagram);
                }
            }
        }
    }

    class RemoveTransactionAction extends AbstractAction
    {
        protected StatesPane statesPane;
        public RemoveTransactionAction(String name, StatesPane statesPane)
        {
            super(name);
            this.statesPane = statesPane;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JTable table = changesTab.getChangesTable().getTable();
            if( currentState == null || tabbedPane.getSelectedIndex()!=1 || table.getSelectedRowCount() == 0 ) return;
            if( JOptionPane.showConfirmDialog(statesPane, "Do you really want to delete selected transaction(s)?", "Remove transaction...", JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION )
            {
                if( diagram.getCurrentState() == currentState )
                {
                    restoreDiagram();
                    applyAction.setSelected(false);
                }
                for(int i=table.getRowCount()-1; i>=0; i--)
                {
                    if(table.isRowSelected(i))
                        currentState.getStateUndoManager().removeEdit(i);
                }
                statesTab.refresh(diagram);
                changesTab.refresh(diagram);
            }
        }
    }
}
