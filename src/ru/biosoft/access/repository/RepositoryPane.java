package ru.biosoft.access.repository;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.ApplicationUtils;

import com.developmentontheedge.application.action.ApplicationAction;
import com.developmentontheedge.beans.ActionsProvider;

@SuppressWarnings ( "serial" )
public class RepositoryPane extends JPanel
{
    protected static final String STATUS_READY = "ready";
    protected static final String STATUS_BUSY = "busy";
    protected static final String STATUS_ERROR = "error";

    public static final ImageIcon BUSY_IMAGE = ApplicationUtils.getImageIcon(RepositoryPane.class.getResource("resources/busy.gif"));
    public static final ImageIcon ERROR_IMAGE = ApplicationUtils.getImageIcon(RepositoryPane.class.getResource("resources/error.gif"));

    private static final int REPOSITORY_PREFERRED_HEIGHT = 300;
    protected static final Logger log = Logger.getLogger(RepositoryPane.class.getName());

    protected TreeModel model = null;
    protected Hashtable<ru.biosoft.access.core.DataElementPath, Status> preloadingMap = new Hashtable<>();


    // //////////////////////////////////////////////////////////////////////////
    // Constructors and initialisation
    //

    public RepositoryPane(DataCollection<?> repository)
    {
        rootDataCollection = repository;
        init(new DataCollectionTreeModelAdapter(repository));
    }

    protected void init(DataCollectionTreeModelAdapter root)
    {
        model = root;
        jTree = new JTree(model);
        RepositoryListenerAdapter listener = new RepositoryListenerAdapter();
        jTree.setExpandsSelectedPaths( true );
        jTree.addTreeSelectionListener(listener);
        jTree.addTreeExpansionListener(listener);
        jTree.addTreeWillExpandListener(listener);
        jTree.setCellRenderer(new RepositoryRenderer(this));

        jTree.addKeyListener(new KeyListener());
        jTree.addMouseListener(new PopupListener());
        jTree.addMouseListener(new MouseListenerAdapter());
        jTree.setDragEnabled(true);
        jTree.setTransferHandler(new TransferHandler()
        {
            @Override
            public int getSourceActions(final JComponent c)
            {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(final JComponent c)
            {
                if( jTree.getSelectionCount() == 0 )
                    return null;
                return new DataElementPathTransferable((DataElementPath)jTree.getSelectionPath().getLastPathComponent());
            }
        });
        jTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        jTree.setLargeModel(true);

        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(jTree);

        scrollPane.setPreferredSize(new Dimension(200, REPOSITORY_PREFERRED_HEIGHT));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setCellRenderer(RepositoryRenderer repositoryRenderer)
    {
        jTree.setCellRenderer(repositoryRenderer);
    }

    // //////////////////////////////////////////////////////////////////////////
    // Properties
    //

    protected JTree jTree = null;
    public JTree getTree()
    {
        return jTree;
    }

    protected DataCollection<?> rootDataCollection = null;
    public DataCollection<?> getRootDataCollection()
    {
        return rootDataCollection;
    }

    protected ActionsProvider actionsProvider;

    public void setActionsProvider(ActionsProvider actionsProvider)
    {
        this.actionsProvider = actionsProvider;
    }

    public DataElement getSelectedNode()
    {
        if(jTree.getLastSelectedPathComponent() == null) return null;

        return getForName((DataElementPath)jTree.getLastSelectedPathComponent());
    }

    /////////////////////////////////////////////////////////////////////////////
    // Miscellaneous
    //

    Vector<RepositoryListener> listeners = new Vector<>();

    public void addListener(RepositoryListener listener)
    {
        listeners.addElement(listener);
    }

    public void removeListener(RepositoryListener listener)
    {
        listeners.removeElement(listener);
    }

    private final Map<ru.biosoft.access.core.DataElementPath, DataElement> dataElementCache = new WeakHashMap<>();

    public DataElement getForName(DataElementPath completeName)
    {
        DataElement de = dataElementCache.get(completeName);
        if( de == null )
        {
            de = completeName.optDataElement();
            dataElementCache.put(completeName, de);
        }
        return de;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Internal classes
    //

    public void selectElement(final DataElementPath elementPath, boolean expand)
    {
        DataElementPath dePath = elementPath;
        List<ru.biosoft.access.core.DataElementPath> paths = new ArrayList<>();
        while(!dePath.equals( rootDataCollection.getCompletePath() ) && !dePath.isEmpty())
        {
            paths.add( dePath );
            dePath = dePath.getParentPath();
        }
        if(dePath.isEmpty())
        {
            return;
        }
        paths.add( dePath );
        Collections.reverse( paths );
        final TreePath path = new TreePath( paths.toArray() );
        new SwingWorker<ru.biosoft.access.core.DataElement, Void>()
        {
            @Override
            protected DataElement doInBackground() throws Exception
            {
                return elementPath.getDataElement();
            }

            @Override
            protected void done()
            {
                jTree.scrollPathToVisible( path );
                jTree.setSelectionPath( path );
                if( expand )
                    jTree.expandPath( path );
            }
        }.execute();
    }

    public void selectElement(final DataElementPath elementPath)
    {
        selectElement( elementPath, false );
    }

    class MouseListenerAdapter extends MouseAdapter
    {
        @Override
        public void mouseClicked(final MouseEvent e)
        {
            if( jTree.getLastSelectedPathComponent() == null )
                return;
            DataElementPath completeNodeName = (DataElementPath)jTree.getLastSelectedPathComponent();

            Task task = de -> {
                if( de == null )
                    return;

                for( RepositoryListener rl : listeners )
                {
                    try
                    {
                        rl.nodeClicked(de, e.getClickCount());
                    }
                    catch( Throwable t )
                    {
                        log.log(Level.SEVERE, "Repository listener error: ", t);
                    }
                }
            };

            TreePath selPath = jTree.getPathForLocation(e.getX(), e.getY());
            if( selPath != null )
            {
                ( new Preloader(completeNodeName, selPath, task) ).execute();
            }
        }
    }

    class KeyListener extends KeyAdapter
    {
        @Override
        public void keyPressed(KeyEvent e)
        {
            if( popup == null )
                return;

            KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
            for( MenuElement me : popup.getSubElements() )
            {
                if( ! ( me instanceof JMenuItem ) )
                    continue;
                JMenuItem jmi = (JMenuItem)me;
                if( ks.equals(jmi.getAccelerator()) )
                    jmi.doClick();
            }

            super.keyPressed(e);
        }
    }

    JPopupMenu popup = null;

    class PopupListener extends MouseAdapter
    {
        @Override
        public void mouseReleased(MouseEvent e)
        {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e)
        {
            // this used instead of  !e.isPopupTrigger() because the latter is not works under Linux
            if( !SwingUtilities.isRightMouseButton(e) )
            {
                return;
            }

            //            if( !e.isPopupTrigger() )
            //                return;

            processPopup(e.getX(), e.getY(), e.getComponent());
        }

        private void processPopup(int x, int y, Component component)
        {
            if( actionsProvider == null )
                return;

            TreePath selPath = jTree.getPathForLocation(x, y);
            if( selPath == null )
                return;

            DataElementPath completeNodeName = (DataElementPath)selPath.getLastPathComponent();

            class PopupTask implements Task
            {
                protected int x, y;
                protected Component component;

                public PopupTask(int x, int y, Component component)
                {
                    this.x = x;
                    this.y = y;
                    this.component = component;
                }

                @Override
                public void run(DataElement de)
                {
                    if( de == null )
                        return;

                    initPopupMenu(de);

                    if( popup != null )
                        popup.show(component, x, y);
                }

                private void initPopupMenu(DataElement de)
                {
                    DataElementPath path = DataElementPath.create(de);

                    Action[] actions = actionsProvider.getActions(de);
                    if( actions == null || actions.length == 0 )
                        return;

                    popup = new JPopupMenu();

                    for( Action a : actions )
                    {
                        if( a == null )
                            continue;
                        a.putValue(ApplicationAction.PARAMETER, path);

                        JMenuItem menuItem = popup.add(a);
                        menuItem.setActionCommand((String)a.getValue(Action.ACTION_COMMAND_KEY));

                        Dimension dim = menuItem.getPreferredSize();
                        dim.height = 23;
                        menuItem.setPreferredSize(dim);
                    }
                }
            }
            PopupTask task = new PopupTask(x, y, component);

            ( new Preloader(completeNodeName, jTree.getSelectionPath(), task) ).execute();
        }
    }

    class RepositoryListenerAdapter implements TreeSelectionListener, TreeExpansionListener, TreeWillExpandListener
    {
        @Override
        public void valueChanged(TreeSelectionEvent e)
        {
            if(jTree.getLastSelectedPathComponent() == null) return;
            DataElementPath selectedNode = (DataElementPath)jTree.getLastSelectedPathComponent();

            Task task = de -> {
                if( de == null )
                    return;

                if( de instanceof DataCollection )
                {
                    DataCollectionInfo info = ( (DataCollection<?>)de ).getInfo();
                    if( info != null && !info.isVisible() )
                        return;
                }

                for( RepositoryListener listener : listeners )
                {
                    listener.selectionChanged(de);
                }
            };

            ( new Preloader(selectedNode, jTree.getSelectionPath(), task) ).execute();
        }

        @Override
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException
        {
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException
        {
        }

        @Override
        public void treeExpanded(TreeExpansionEvent event)
        {
            TreePath path = event.getPath();
            JTree tree = (JTree)event.getSource();
            tree.setSelectionPath(path);
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event)
        {
            TreePath path = event.getPath();
            JTree tree = (JTree)event.getSource();
            tree.setSelectionPath(path);
        }
    }

    class Preloader extends SwingWorker<ru.biosoft.access.core.DataElement, Void>
    {
        DataElement de;

        DataElementPath completeName;
        TreePath path;
        Task task;

        public Preloader(DataElementPath completeName, TreePath path, Task task)
        {
            this.completeName = completeName;
            this.path = path;
            this.task = task;
        }

        @Override
        public DataElement doInBackground()
        {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            DataCollection<?> dc = null;

            Status currentStatus = null;
            synchronized( preloadingMap )
            {
                currentStatus = preloadingMap.get(completeName);
                if( currentStatus == null )
                {
                    currentStatus = new Status(STATUS_BUSY);
                    preloadingMap.put(completeName, currentStatus);
                }
            }
            ( (DataCollectionTreeModelAdapter)model ).treeNodeChanged(model, path);

            synchronized( currentStatus )
            {
                try
                {
                    de = completeName.optDataElement();

                    if( de instanceof DataCollection )
                    {
                        dc = (DataCollection<?>)de;

                        //call getSize to load necessary data to cache
                        dc.getSize();
                    }
                    currentStatus.setStatusString(STATUS_READY);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can not preload data element " + completeName + ": "+ExceptionRegistry.log(t));
                    currentStatus.setStatusString(STATUS_ERROR);

                    if( dc != null )
                        dc.getInfo().setError("Can not preload data, error: " + t);
                }
            }
            return de;
        }
        @Override
        protected void done()
        {
            if( task != null )
            {
                task.run(de);
            }

            preloadingMap.remove(completeName);
            ( (DataCollectionTreeModelAdapter)model ).treeNodeChanged( model, path );
        }
    }

    public Status getPreloadingStatus(DataElementPath completeNodeName)
    {
        return preloadingMap.get(completeNodeName);
    }

    static class Status
    {
        protected String statusString;

        public Status(String statString)
        {
            this.statusString = statString;
        }

        public String getStatusString()
        {
            return statusString;
        }

        public void setStatusString(String statusString)
        {
            this.statusString = statusString;
        }
    }

    public interface Task
    {
        public void run(DataElement de);
    }
}
