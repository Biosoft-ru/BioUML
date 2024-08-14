package biouml.plugins.research;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

import java.util.logging.Logger;

import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.journal.Journal;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.TransformedIterator;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Module;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;

/**
 * Journal table view part
 */
public class JournalViewPart extends ViewPartSupport implements ViewPaneListener
{
    protected Logger log = Logger.getLogger(JournalViewPart.class.getName());

    protected TabularPropertyInspector journalTable;
    protected Journal journal;
    protected ArrayList<TaskInfo> items4add = new ArrayList<>();

    public static final String PASTE_ACTION = "journal-paste";
    public static final String REMOVE_ACTION = "journal-remove";
    public static final String REMOVEALL_ACTION = "journal-removeall";

    protected Action[] actions;
    protected Action pasteAction = new PasteAction();
    protected Action removeAction = new RemoveAction();
    protected Action removeAllAction = new RemoveAllAction();

    public JournalViewPart()
    {
        journalTable = new TabularPropertyInspector();
        add(journalTable, BorderLayout.CENTER);
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( this.document != null )
        {
            ViewPane viewPane = ( (DiagramDocument)this.document ).getViewPane();
            viewPane.removeViewPaneListener(this);
        }
        this.model = model;
        this.document = document;
        if( ( model != null ) && ( document != null ) )
        {
            Module module = Module.optModule((Diagram)model);
            if( ( module != null ) && ( module.getType() instanceof ResearchModuleType ) )
            {
                journal = ( (ResearchModuleType)module.getType() ).getResearchJournal(module);
                exploreJournalTable();

                ViewPane viewPane = ( (DiagramDocument)this.document ).getViewPane();
                viewPane.addViewPaneListener(this);
            }
        }
    }

    protected void exploreJournalTable()
    {
        Iterator<JournalInfoWrapper> iter = new TransformedIterator<TaskInfo, JournalInfoWrapper>(journal.iterator())
        {
            @Override
            protected JournalInfoWrapper transform(TaskInfo value)
            {
                return new JournalInfoWrapper(value);
            }
        };
        journalTable.explore(iter);
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof Diagram && ( (Diagram)model ).getType().getSemanticController() instanceof BaseResearchSemanticController )
            return true;
        return false;
    }
    @Override
    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(pasteAction, PASTE_ACTION);
            initializer.initAction(removeAction, REMOVE_ACTION);
            initializer.initAction(removeAllAction, REMOVEALL_ACTION);

            actions = new Action[] {pasteAction, removeAction, removeAllAction};
        }

        return actions;
    }

    //ViewPaneListener
    @Override
    public void mouseClicked(ViewPaneEvent e)
    {
    }

    @Override
    public void mousePressed(ViewPaneEvent event)
    {
        if( model instanceof Diagram && document instanceof DiagramDocument )
        {
            ViewPane viewPane = ( (DiagramDocument)document ).getViewPane();
            Diagram diagram = (Diagram)model;

            Object model = event.getViewSource().getModel();
            Compartment parent = null;
            if( model instanceof Compartment )
                parent = (Compartment)model;
            else if( model instanceof DiagramElement )
            {
                if( ( (DiagramElement)model ).getOrigin() instanceof Compartment )
                    parent = (Compartment) ( (DiagramElement)model ).getOrigin();
            }
            if( parent == null )
                return;

            Iterator<TaskInfo> iter = items4add.iterator();

            try
            {
                ( (BaseResearchSemanticController)diagram.getType().getSemanticController() ).addTaskInfoItemsToDiagram(parent, iter,
                        event.getPoint(), (ViewEditorPane)viewPane);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Paste action error: ", e);
            }
            viewPane.repaint();
            items4add.clear();
        }

    }

    @Override
    public void mouseReleased(ViewPaneEvent e)
    {
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


    //Actions
    private class PasteAction extends AbstractAction
    {
        public PasteAction()
        {
            super(PASTE_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ListSelectionModel lsm = journalTable.getTable().getSelectionModel();
            if( lsm == null )
                return;

            ArrayList<TaskInfo> selectedItems = new ArrayList<>();
            for( int i = 0; i < journalTable.getTable().getModel().getRowCount(); i++ )
            {
                if( lsm.isSelectedIndex(i) )
                    selectedItems.add( ( (JournalInfoWrapper)journalTable.getModelForRow(i) ).getTask());
            }

            JournalViewPart.this.items4add = selectedItems;
        }
    }

    private class RemoveAction extends AbstractAction
    {
        public RemoveAction()
        {
            super(REMOVE_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ListSelectionModel lsm = journalTable.getTable().getSelectionModel();
            if( lsm == null )
                return;

            for( int i = 0; i < journalTable.getTable().getModel().getRowCount(); i++ )
            {
                if( lsm.isSelectedIndex(i) )
                {
                    journal.removeAction( ( (JournalInfoWrapper)journalTable.getModelForRow(i) ).getTask());
                }
            }
            exploreJournalTable();
        }
    }

    private class RemoveAllAction extends AbstractAction
    {
        public RemoveAllAction()
        {
            super(REMOVEALL_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int status = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), "Do you really want to clean journal?");
            if( status == JOptionPane.OK_OPTION )
            {
                for( int i = 0; i < journalTable.getTable().getModel().getRowCount(); i++ )
                {
                    journal.removeAction( ( (JournalInfoWrapper)journalTable.getModelForRow(i) ).getTask());
                }
                exploreJournalTable();
            }
        }
    }
}
