package ru.biosoft.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.exception.InternalException;
import ru.biosoft.gui.Document.ActionType;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.ApplicationToolBar;
import com.developmentontheedge.application.action.ActionManager;

@SuppressWarnings ( "serial" )
public class DocumentsPane extends JPanel implements ChangeListener, Iterable<Document>
{
    private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

    private final HashMap<String, Document> tabs = new HashMap<>();

    public DocumentsPane()
    {
        this(CloseDocumentAction.KEY, CloseAllDocumentAction.KEY, null);
    }

    public DocumentsPane(String closeDiagramActionKey, String closeAllDiagramActionKey, String applicationName)
    {
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addChangeListener(this);

        tabbedPane.addMouseListener(new PopupListener(closeDiagramActionKey, closeAllDiagramActionKey, applicationName));

        initListeners();
    }

    /**
     * Listens to roots of all document models.
     */
    private DataCollectionListener rootsListener;

    private void initListeners()
    {
        rootsListener = new DataCollectionListenerSupport()
        {
            @Override
            public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
            {
                for( int i = tabbedPane.getTabCount() - 1; i >= 0; --i )
                {
                    DataElement de = e.getDataElement();
                    Object model = getDocument(i).getModel();

                    if( model instanceof DataElement )
                    {
                        DataElement modelElement = (DataElement)model;
                        if( DataElementPath.create(de).isAncestorOf(DataElementPath.create(modelElement)) )
                        {
                            removeDocument(i, false);
                        }
                    }
                }
            }


            @Override
            public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
            {
                DataCollectionEvent pe = e;
                while( pe.getPrimaryEvent() != null )
                {
                    pe = pe.getPrimaryEvent();
                }

                if( pe.getType() == DataCollectionEvent.ELEMENT_WILL_REMOVE )
                    elementWillRemove(pe);
            }
        };
    }

    public void addChangeListener(ChangeListener l)
    {
        tabbedPane.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l)
    {
        tabbedPane.removeChangeListener(l);
    }

    protected String getSelectedTabName()
    {
        int idx = tabbedPane.getSelectedIndex();
        return idx < 0 ? null : tabbedPane.getTitleAt(idx);
    }

    public int getDocumentCount()
    {
        return tabbedPane.getTabCount();
    }

    public synchronized void addDocument(Document document)
    {
        insertDocument(document, tabbedPane.getTabCount());
    }

    public void insertDocument(Document document, int index)
    {
        if( tabs.keySet().contains(document.getDisplayName()) )
        {
            Document d = tabs.get(document.getDisplayName());
            tabbedPane.setSelectedComponent(d.getViewPane());
        }
        else
        {
            String iconId = IconFactory.getClassIconId( document.getClass() );
            ImageIcon icon = IconFactory.getIconById( iconId );
            tabbedPane.insertTab(document.getDisplayName(), icon, document.getViewPane(), null, index);
            if( !document.isMutable() )
            {
                int index_ = tabbedPane.indexOfComponent(document.getViewPane());
                tabbedPane.setForegroundAt(index_, Color.darkGray);
            }
            tabbedPane.setSelectedComponent(document.getViewPane());
            tabs.put(document.getDisplayName(), document);
            update( document, Document.getActiveDocument() );

            if( document.getModel() instanceof DataElement )
            {
                DataCollection<?> root = getRoot((DataElement)document.getModel());
                if( root != null )
                {
                    root.removeDataCollectionListener(rootsListener);
                    root.addDataCollectionListener(rootsListener);
                }
            }
        }
    }

    private DataCollection<?> getRoot(DataElement de)
    {
        if( de.getOrigin() != null )
        {
            DataCollection<?> root = de.getOrigin();
            while( root.getOrigin() != null )
                root = root.getOrigin();
            return root;
        }
        return null;
    }

    public Document getCurrentDocument()
    {
        return tabs.get(getSelectedTabName());
    }

    public Document getDocument(int n)
    {
        return tabs.get(tabbedPane.getTitleAt(n));
    }

    public void removeCurrentDocument()
    {
        int n = tabbedPane.getSelectedIndex();
        removeDocument(n, false);
    }

    public void removeDocument(int n)
    {
        removeDocument(n, true);
    }

    public void removeDocument(int n, boolean setSelected)
    {
        if( setSelected )
        {
            tabbedPane.setSelectedIndex(n);
        }

        boolean isUpdated = true;
        if( n == tabbedPane.getSelectedIndex() )
            isUpdated = false;

        Document document = getDocument(n);
        document.close();
        tabs.remove(tabbedPane.getTitleAt(n));
        tabbedPane.removeTabAt(n);

        if( !isUpdated )
        {
            document = getCurrentDocument();
            update( document, Document.getActiveDocument() );
        }
    }

    public void removeDocument(Document document)
    {
        int n = getDocumentCount();
        for( int i = 0; i < n; i++ )
        {
            Document d = getDocument(i);
            if( document == d )
            {
                removeDocument(i);
                break;
            }
        }
    }

    /**
     * Invoked when the target of the listener has changed its state.
     *
     * @param e
     *            a ChangeEvent object
     */
    @Override
    public void stateChanged(ChangeEvent e)
    {
        Object document = tabs.get(getSelectedTabName());
        update( (Document)document, Document.getActiveDocument() );
    }

    public static final int DOCUMENT_TOOLBAR_ACTIONS_GROUP = 3;

    private void update(Document document, Document oldActiveDocument)
    {
        updateActions( document, oldActiveDocument );
        DocumentManager.setActiveDocument( document, this );
    }

    public static void updateActions(Document document, Document oldActiveDocument)
    {
        //disable actions for previous active document
        if( oldActiveDocument != null )
        {
            Action[] actions = oldActiveDocument.getActions(ActionType.MENU_ACTION);
            if( actions != null )
            {
                StreamEx.of(actions).nonNull().forEach( action -> action.setEnabled( false ) );
            }
            actions = oldActiveDocument.getActions(ActionType.ENABLED_ACTIONS);
            if( actions != null )
            {
                StreamEx.of(actions).nonNull().forEach( action -> action.setEnabled( false ) );
            }
        }

        ApplicationToolBar toolbar = Application.getApplicationFrame().getToolBar();
        toolbar.cleanGroup(DOCUMENT_TOOLBAR_ACTIONS_GROUP);
        toolbar.validate();

        //enable specific actions for new active document
        if( document != null )
        {
            Action[] actions = document.getActions(ActionType.MENU_ACTION);
            if( actions != null )
            {
                StreamEx.of(actions).nonNull().forEach( action -> action.setEnabled( true ) );
            }
            actions = document.getActions(ActionType.ENABLED_ACTIONS);
            if( actions != null )
            {
                StreamEx.of(actions).nonNull().forEach( action -> action.setEnabled( true ) );
            }
            actions = document.getActions(ActionType.TOOLBAR_ACTION);
            if( actions != null )
            {
                for( int i = actions.length-1; i >= 0; i-- )
                {
                    if( actions[i] != null )
                    {
                        toolbar.addAction(actions[i], DOCUMENT_TOOLBAR_ACTIONS_GROUP);
                    }
                }
                toolbar.validate();
            }
        }
    }

    static class PopupListener extends MouseAdapter
    {
        JPopupMenu popup = new JPopupMenu();

        PopupListener(String closeDocumentActionKey, String closeAllDocumentsActionKey, String applicationName)
        {
            ActionManager actionManager = null;
            if( applicationName != null )
            {
                actionManager = Application.getActionManager(Application.getApplicationFrame(applicationName));
            }
            else
            {
                actionManager = Application.getActionManager();
            }
            Action action = actionManager.getAction(closeDocumentActionKey);
            popup.add(action);
            action = actionManager.getAction(closeAllDocumentsActionKey);
            popup.add(action);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e)
        {
            if( !e.isPopupTrigger() )
                return;

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * return DocumentsPane with name "document"
     */
    public static @Nonnull DocumentsPane getDocumentsPane()
    {
        ApplicationFrame application = Application.getApplicationFrame();
        if(application == null)
            throw new InternalException("getDocumentsPane called from headless launch");
        JComponent panel = application.getPanelManager().getPanel( ApplicationFrame.DOCUMENT_PANE_NAME );
        if(!(panel instanceof DocumentsPane))
            throw new InternalException("getDocumentsPane: cannot find documents panel");
        return (DocumentsPane)panel;
    }

    @Override
    public Iterator<Document> iterator()
    {
        int count = getDocumentCount();
        List<Document> documents = new ArrayList<>();
        for(int i=0; i<count; i++)
        {
            Document doc = getDocument(i);
            if(doc != null)
                documents.add(doc);
        }
        return documents.iterator();
    }
}
