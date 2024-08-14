package ru.biosoft.access.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.SymbolicLinkDataCollection;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * @pending try/catch for listeners notification
 */
public class DataCollectionTreeModelAdapter implements TreeModel, DataCollectionListener
{
    public static final String NONE_ELEMENT = "(none)";

    protected Logger log = Logger.getLogger(DataCollectionTreeModelAdapter.class.getName());

    protected DataCollection<?> root;

    protected EventListenerList listenerList = new EventListenerList();

    private final boolean canBeNull;
    private boolean showLeafNodes = true;

    // 200 = max number of elements likely to be displayed at once
    Cache<ru.biosoft.access.core.DataElementPath, Integer> pathIndexCache = new Cache<>(200);

    public DataCollectionTreeModelAdapter(DataCollection<?> root)
    {
        this(root, false);
    }

    public DataCollectionTreeModelAdapter(DataCollection<?> root, boolean canBeNull)
    {
        this.root = root;
        this.canBeNull = canBeNull;
        if( root != null )
            root.addDataCollectionListener(this);
    }

    private static class Cache<K, V>
    {
        Map<K, V> cache = new HashMap<>();
        Set<K> keyQueue = new LinkedHashSet<>();
        int size;

        public Cache(int size)
        {
            this.size = size;
        }

        public synchronized void put(K key, V value)
        {
            keyQueue.remove(key);
            if(keyQueue.size() >= size)
            {
                K first = keyQueue.iterator().next();
                cache.remove(first);
                keyQueue.remove(first);
            }
            keyQueue.add(key);
            cache.put(key, value);
        }

        public synchronized V get(K key)
        {
            return cache.get(key);
        }

        public synchronized void clear()
        {
            cache.clear();
            keyQueue.clear();
        }
    }

    // //////////////////////////////////////////////////////////////////
    // Implementing TreeModel
    //

    private DataCollection<?> currentDC;

    private List<?> currentNameList;

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        if( ( (DataElementPath)parent ).isEmpty() )
        {
            DataElementPath childName = (DataElementPath)child;
            if( childName.getName().equals(NONE_ELEMENT) )
                return 0;
            if( childName.getName().equals("databases") )
                return 0 + ( canBeNull ? 1 : 0 );
            if( childName.getName().equals("data") )
                return 1 + ( canBeNull ? 1 : 0 );
            return -1;
        }
        Integer cachedIdx = pathIndexCache.get((DataElementPath)child);
        if(cachedIdx != null) return cachedIdx;
        int idx = -1;
        DataElement parentDE = getForName((DataElementPath)parent);
        if( parentDE instanceof DataCollection )
        {
            List<?> names = ( (DataCollection<?>)parentDE ).getNameList();
            String name = ( (DataElementPath)child ).getName();
            idx = names.indexOf( name );
        }
        return idx;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        if( ( (DataElementPath)parent ).isEmpty() )
        {
            if( canBeNull )
                index--;
            if( index == -1 )
                return DataElementPath.create(NONE_ELEMENT);
            if( index == 0 )
                return DataElementPath.create("databases");
            if( index == 1 )
                return DataElementPath.create("data");
        }

        DataElementPath completeChildName = null;
        DataElement parentDE = getForName((DataElementPath)parent);

        if( parentDE instanceof DataCollection )
        {
            DataCollection<?> parentDC = (DataCollection<?>)parentDE;
            if( currentDC != parentDC )
            {
                currentDC = parentDC;
                currentNameList = currentDC.getNameList();
            }
            if( index >= currentNameList.size() )
            {
                index = currentNameList.size() - 1;
            }
            String childName = (String)currentNameList.get(index);
            completeChildName = DataElementPath.create(parentDC, childName);
        }

        pathIndexCache.put(completeChildName, index);

        return completeChildName;
    }

    @Override
    public Object getRoot()
    {
        return root == null ? DataElementPath.EMPTY_PATH : DataElementPath.create(root);
    }

    @Override
    public int getChildCount(Object parent)
    {
        if( ( (DataElementPath)parent ).isEmpty() )
        {
            return 2 + ( canBeNull ? 1 : 0 ); // "databases" and "data"
        }
        int count = 0;
        DataElement parentDE = getForName((DataElementPath)parent);
        try
        {
            if( parentDE instanceof DataCollection
                    && ( showLeafNodes || parentDE instanceof FolderCollection || ( ! ( (DataCollection<?>)parentDE ).getInfo()
                            .isChildrenLeaf() && ru.biosoft.access.core.DataCollection.class.isAssignableFrom( ( (DataCollection<?>)parentDE ).getDataElementType()) ) ) )
                count = ( (DataCollection<?>)parentDE ).getSize();
        }
        catch( Exception e )
        {
        }

        return count;
    }

    @Override
    public boolean isLeaf(Object node)
    {
        return DataCollectionUtils.isLeaf((DataElementPath)node);
    }

    protected DataElement getForName(DataElementPath parent)
    {
        String parentPath = parent.toString();
        if( root != null )
            root.removeDataCollectionListener(this);
        DataElement de = parent.optDataElement();
        if( de == null && parent.isDescendantOf(DataElementPath.create(root)) )
        {
            //if parentPath is relative
            if( parent.getDepth() == 1 )
                de = root;
            else
            {
                try
                {
                    de = CollectionFactory.getDataElement(parentPath.substring(parentPath.indexOf(DataElementPath.PATH_SEPARATOR) + 1), root, DataElement.class);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, ExceptionRegistry.log(e));
                }
            }
        }
        if( de instanceof SymbolicLinkDataCollection )
        {
            de = ( (SymbolicLinkDataCollection)de ).getPrimaryCollection();
        }
        if( root != null )
            root.addDataCollectionListener(this);
        return de;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        // not editable from GUI yet
    }

    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
        synchronized( listenerList )
        {
            listenerList.add(TreeModelListener.class, l);
        }
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
        synchronized( listenerList )
        {
            listenerList.remove(TreeModelListener.class, l);
        }
    }

    // //////////////////////////////////////////////////////////////////
    //
    // Implementing ru.biosoft.access.core.DataCollectionListener

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        DataCollectionEvent primaryEvent = e;
        while( primaryEvent.getPrimaryEvent() != null )
        {
            primaryEvent = primaryEvent.getPrimaryEvent();
        }
        if( primaryEvent.getType() != DataCollectionEvent.ELEMENT_ADDED )
        {
            return;
        }

        DataElement nodeAdded = primaryEvent.getDataElement();
        DataCollection<?> parent = nodeAdded.getOrigin();
        if( parent != null )
        {
            int idx = 0;
            DataElementPath path = DataElementPath.create(parent);
            if( parent.getOrigin() != null )
                idx = getIndexOfChild(path.getParentPath(), path);
            treeStructureChanged(this, getPathToRoot(e), new int[] {idx}, new Object[] {path.toString()});
        }
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        DataCollectionEvent primaryEvent = e;
        while( primaryEvent.getPrimaryEvent() != null )
        {
            primaryEvent = primaryEvent.getPrimaryEvent();
        }

        DataElement element = primaryEvent.getDataElement();
        if( element == null )
        {
            // it might be that this is an element removal
            elementRemoved(e);
            return;
        }

        DataCollection<?> parent = element.getOrigin();
        if( parent != null && parent.getOrigin() != null )
        {
            DataElementPath path = DataElementPath.create(parent);
            int idx = getIndexOfChild(path.getParentPath(), path);
            treeStructureChanged(this, getPathToRoot(e), new int[] {idx}, new Object[] {path.toString()});
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent e)
    {
        DataCollectionEvent primaryEvent = e;
        while( primaryEvent.getPrimaryEvent() != null )
        {
            primaryEvent = primaryEvent.getPrimaryEvent();
        }

        DataCollection<?> parent = primaryEvent.getOwner();
        if( parent != null && ( parent.getOrigin() != null || ( root != null && parent == root ) ) )
        {
            treeStructureChanged(this, getPathToRoot(e), null, null);
        }
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e)
    {
    }

    @Override
    public void elementWillChange(DataCollectionEvent e)
    {
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e)
    {
    }

    // ////////////////////////////////////////////
    // Helper functions
    //

    protected DataElementPath[] getPathToRoot(DataCollectionEvent e)
    {
        DataElementPath basePath = DataElementPath.create(root);
        List<ru.biosoft.access.core.DataElementPath> result = new ArrayList<>();
        result.add(basePath);
        while( e != null && e.getType() == DataCollectionEvent.ELEMENT_CHANGED )
        {
            List<ru.biosoft.access.core.DataElementPath> subResult = new ArrayList<>();
            DataElementPath currentPath = e.getDataElementPath();
            while( !currentPath.isAncestorOf(basePath) )
            {
                subResult.add(0, currentPath);
                currentPath = currentPath.getParentPath();
            }
            result.addAll(subResult);
            basePath = e.getDataElementPath().getTargetPath();
            e = e.getPrimaryEvent();
        }
        return result.toArray(new ru.biosoft.access.core.DataElementPath[result.size()]);
    }

    protected void treeStructureChanged(Object source, DataElementPath[] path, int[] childIndices, Object[] children)
    {
        currentDC = null;
        synchronized( listenerList )
        {
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            TreeModelEvent e = null;

            // Process the listeners last to first, notifying
            // those that are interested in this event
            for( int i = listeners.length - 2; i >= 0; i -= 2 )
            {
                if( listeners[i] == TreeModelListener.class )
                {
                    // Lazily create the event:
                    if( e == null )
                        e = new TreeModelEvent(source, path, childIndices, children);

                    try
                    {
                        ( (TreeModelListener)listeners[i + 1] ).treeStructureChanged(e);
                    }
                    catch( Throwable t )
                    {
                        //TODO: sometimes ArrayIndexOutOfBoundsException throws for unknown reasons
                        //log.log(Level.SEVERE, "treeStructureChanged failure", t);
                    }
                }
            }
        }
    }

    protected void treeStructureChanged(Object source, TreePath path, int[] childIndices, Object[] children)
    {
        synchronized( listenerList )
        {
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            TreeModelEvent e = null;

            // Process the listeners last to first, notifying
            // those that are interested in this event
            for( int i = listeners.length - 2; i >= 0; i -= 2 )
            {
                if( listeners[i] == TreeModelListener.class )
                {
                    // Lazily create the event:
                    if( e == null )
                        e = new TreeModelEvent(source, path, childIndices, children);

                    try
                    {
                        ( (TreeModelListener)listeners[i + 1] ).treeStructureChanged(e);
                    }
                    catch( Throwable t )
                    {
                        //TODO: sometimes ArrayIndexOutOfBoundsException throws for unknown reasons
                        //log.log(Level.SEVERE, "treeStructureChanged failure", t);
                    }
                }
            }
        }
    }

    void treeNodeChanged(Object source, TreePath path)
    {
        synchronized( listenerList )
        {
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            TreeModelEvent e = null;

            // Process the listeners last to first, notifying
            // those that are interested in this event
            for( int i = listeners.length - 2; i >= 0; i -= 2 )
            {
                if( listeners[i] == TreeModelListener.class )
                {
                    // Lazily create the event:
                    if( e == null )
                        e = new TreeModelEvent(source, path);

                    ( (TreeModelListener)listeners[i + 1] ).treeNodesChanged( e );
                }
            }
        }
    }

    public boolean isShowLeafNodes()
    {
        return showLeafNodes;
    }

    public void setShowLeafNodes(boolean showLeafNodes)
    {
        this.showLeafNodes = showLeafNodes;
    }
}
