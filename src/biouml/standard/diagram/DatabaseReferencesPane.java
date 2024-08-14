package biouml.standard.diagram;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import one.util.streamex.StreamEx;

import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Referrer;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.action.ActionInitializer;

public class DatabaseReferencesPane extends JPanel implements ViewPaneListener
{
    public static final String EDIT = "edit";
    public static final String REMOVE = "remove";

    protected Action editAction = new EditAction(EDIT, this);
    protected Action removeAction = new RemoveAction(REMOVE, this);
    protected Action[] actions;

    protected MessageBundle messageBundle = new MessageBundle();

    protected ViewPane viewPane;

    protected TabularPropertyInspector refTable = new TabularPropertyInspector();
    protected Referrer referrer;
    protected DatabaseReference selectedReference;

    public DatabaseReferencesPane()
    {
        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        showStandardStub();
    }

    protected void showStandardStub()
    {
        removeAll();
        JLabel text = new JLabel("Select diagram element with database references");
        add(text, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0),
                0, 0));
        repaint();
        validate();

        editAction.setEnabled(false);
        removeAction.setEnabled(false);
    }

    protected void showDatabaseReferencesTable()
    {
        removeAll();
        JLabel text = new JLabel("Current DB references for DC/title");
        add(text, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(10, 0,
                0, 0), 0, 0));

        refTable.explore(referrer.getDatabaseReferences());
        refTable.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedReference = null;

        refTable.addListSelectionListener(event -> {
            if( event.getFirstIndex() != -1 )
            {
                Object model = refTable.getModelOfSelectedRow();
                if( model instanceof DatabaseReference )
                {
                    selectedReference = (DatabaseReference)model;
                    removeAction.setEnabled(true);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(refTable);
        add(scrollPane, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, 0,
                0, 0), 0, 0));
        repaint();
        validate();

        editAction.setEnabled(true);
        removeAction.setEnabled(false);
    }
    public void explore(ViewPane viewPane)
    {
        viewPane.addViewPaneListener(this);
        this.viewPane = viewPane;
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(editAction, DatabaseReferencesPane.EDIT);
            initializer.initAction(removeAction, DatabaseReferencesPane.REMOVE);

            actions = new Action[] {editAction, removeAction};
        }

        return actions;
    }

    @Override
    public void mouseClicked(ViewPaneEvent e)
    {
    }

    @Override
    public void mousePressed(ViewPaneEvent event)
    {
    }

    @Override
    public void mouseReleased(ViewPaneEvent e)
    {
        diagramSelectionChanged();
    }

    @Override
    public void mouseEntered(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseExited(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseDragged(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseMoved(ViewPaneEvent e)
    {
    }

    protected void diagramSelectionChanged()
    {
        SelectionManager sm = viewPane.getSelectionManager();
        if( sm.getSelectedModels() != null && sm.getSelectedModels().length > 0 && sm.getSelectedModels()[0] instanceof Node )
        {
            Base kernel = ( (Node)sm.getSelectedModels()[0] ).getKernel();
            if( kernel instanceof Referrer )
            {
                referrer = (Referrer)kernel;
                showDatabaseReferencesTable();
                return;
            }
        }
        showStandardStub();
    }

    class EditAction extends AbstractAction
    {
        private final Component parent;
        public EditAction(String name, Component parent)
        {
            super(name);
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            DatabaseReferencesEditDialog dialog;
            dialog = new DatabaseReferencesEditDialog(parent, referrer);

            if( dialog.isEnabled() && dialog.doModal() )
            {
                referrer.setDatabaseReferences(dialog.getValue());
                showDatabaseReferencesTable();
            }
        }
    }

    class RemoveAction extends AbstractAction
    {
        private final Component parent;
        public RemoveAction(String name, Component parent)
        {
            super(name);
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( selectedReference != null )
            {
                if( JOptionPane.showConfirmDialog(parent, "Do you really want to remove selected reference?") == JOptionPane.OK_OPTION )
                {
                    DatabaseReference[] references = referrer.getDatabaseReferences();
                    if( references != null && references.length > 0 )
                    {
                        referrer.setDatabaseReferences( StreamEx.of( references ).without( selectedReference )
                                .toArray( DatabaseReference[]::new ) );
                        showDatabaseReferencesTable();
                    }
                }
            }
            else
            {
                JOptionPane.showMessageDialog(parent, "Select database reference from the table");
            }
        }
    }
}
