package biouml.standard.type.access;

import java.io.File;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import one.util.streamex.EntryStream;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Key;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.exception.IndexCreateException;
import biouml.standard.type.Base;

/**
 * Base implementation for title index, which can be used by
 * biouml.standard.diagram.CreateDiagramElementDialog.
 *
 * Further extension allows to use specified bean property as a title.
 * For this purpose the property name should be specified in config file,
 * for example:
 * <pre>index.title.property=reference</pre>
 *
 * Constraint concept:
 * allows user to filter index values according to some pattern.
 * Currently title should starts from the specified constraint.
 */
public class TitleIndex extends DefaultComboBoxModel<String> implements DataCollectionListener, Index<String>
{
    /** Data collection property - TitleIndex class. */
    public static final String INDEX_TITLE = "index.title";

    /** Data collection property - TitleIndex class. */
    public static final String INDEX_TITLE_PROPERTY = "index.title.property";

    protected static final Logger log = Logger.getLogger(TitleIndex.class.getName());

    protected String indexName;
    protected Method methodGetTitle;
    protected DataCollection<?> dc;

    //data
    private volatile boolean init;
    protected Map<String, String> id2title = new HashMap<>();
    protected Map<String, String> title2id = new HashMap<>();


    //list of titles, which start with constraint
    private final Vector<String> constraintTitle = new Vector<>();
    private String constraint;
    private boolean valid = false;

    /**
     * Create new title index
     */
    public TitleIndex(DataCollection<?> dc, String indexName) throws Exception
    {
        this.indexName = indexName;
        this.dc = dc;

        String property = dc.getInfo().getProperty(INDEX_TITLE_PROPERTY);
        if( property != null )
        {
            try
            {
                methodGetTitle = dc.getDataElementType().getMethod(
                        "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not init method to get title, property=" + property + ", data element type=" + dc.getDataElementType()
                        + ", error=" + e, e);
            }
        }

        dc.addDataCollectionListener(this);
    }

    public DataCollection<?> getOwner()
    {
        return dc;
    }

    public String getTitle(DataElement de)
    {
        if( methodGetTitle != null )
        {
            try
            {
                return (String)methodGetTitle.invoke(de);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get title for data element: name=" + de.getName() + ", error=" + e, e);
            }
        }

        if( de instanceof Base )
            return ( (Base)de ).getTitle();

        return de.getName();
    }

    protected void doInit() throws Exception
    {
        if( dc == null )
            return;

        for( ru.biosoft.access.core.DataElement de : dc )
        {
            putInternal(de.getName(), getTitle(de));
        }
    }

    /**
     * Init data using data collection dc
     */
    private void init()
    {
        if( !init )
        {
            synchronized(this)
            {
                if(!init)
                {
                    try
                    {
                        doInit();
                    }
                    catch( Throwable t )
                    {
                        ExceptionRegistry.log(new IndexCreateException( t, this ));
                    }
                    init = true;
                }
            }
        }
    }

    /**
     * Get current index name
     */
    @Override
    public String getName()
    {
        return indexName;
    }

    /**
     * Set constraint
     */
    public void setConstraint(String constraint)
    {
        init();

        if( this.constraint != null && !this.constraint.equals(constraint) )
        {
            valid = false;
            this.constraint = constraint;
        }
        else if( this.constraint == null && ( constraint != null && constraint.length() != 0 ) )
        {
            valid = false;
            this.constraint = constraint;
        }
    }

    /** Make composite title for equal titles */
    public static String getCompositeName(Object title, Object id)
    {
        // trim title length
        if( ( (String)title ).length() > 30 )
            title = ( (String)title ).substring(0, 30) + "...";

        return "" + title + " (" + id + ")";
    }

    private void validate()
    {
        init();

        if( !valid )
        {
            constraintTitle.clear();

            Comparator<String> comparator = String.CASE_INSENSITIVE_ORDER;
            for( String title : title2id.keySet() )
            {
                boolean add = true;

                if( constraint != null && constraint.trim().length() > 0 )
                {
                    if( !title.toLowerCase().startsWith(constraint.toLowerCase()) )
                        add = false;
                }

                if( add )
                {
                    int index = Collections.binarySearch(constraintTitle, title, comparator);
                    if(index<0)
                    {
                        constraintTitle.add(-index-1, title);
                    }
                }
            }

            valid = true;
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Dummy Index interface implementation
    //

    /**
     * Not supported
     */
    @Override
    public Iterator nodeIterator(Key key)
    {
        throw new java.lang.UnsupportedOperationException("Method nodeIterator() not yet implemented.");
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public File getIndexFile()
    {
        return null;
    }

    @Override
    public void close() throws Exception
    {
        dc.removeDataCollectionListener(this);
    }

    ////////////////////////////////////////////////////////////////////////////
    // implements ListModel interface
    //

    @Override
    public int getSize()
    {
        validate();

        return constraintTitle.size();
    }

    @Override
    public String getElementAt(int index)
    {
        validate();

        return constraintTitle.get(index);
    }

    ///////////////////////////////////////////////////////////////////
    // DataCollectionListener interface implementation
    //

    private String id;
    private String title;
    private boolean elementWillRemove = false;

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // do nothing
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        try
        {
            DataElement de = e.getDataElement();
            if( de != null )
            {
                id = de.getName();
                title = getTitle(de);
                put(de.getName(), getTitle(de));
            }
        }
        catch( Exception e1 )
        {
        }
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        try
        {
            DataElement de = e.getDataElement();
            if( de != null )
            {
                id = de.getName();
                elementWillRemove = true;
            }
        }
        catch( Exception e1 )
        {
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        if( elementWillRemove )
        {
            elementWillRemove = false;
            remove(id);
        }
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        DataElement de = e.getDataElement();
        if( de != null )
        {
            id = de.getName();
            title = getTitle(de);
        }
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        DataElement de = e.getDataElement();
        if( de != null && de.getName().equals(id) )
        {
            String newTitle;

            newTitle = getTitle(de);

            if( !newTitle.equals(title) )
            {
                remove(id);
                put(id, newTitle);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Map interface implementation
    //

    @Override
    public int size()
    {
        init();
        return id2title.size();
    }

    @Override
    public boolean isEmpty()
    {
        init();
        return id2title.isEmpty();
    }

    @Override
    public boolean containsKey(Object id)
    {
        init();
        return id2title.containsKey(id);
    }

    @Override
    public boolean containsValue(Object title)
    {
        init();
        return id2title.containsValue(title);
    }

    @Override
    public String get(Object id)
    {
        init();
        return id2title.get(id);
    }

    public String getIdByTitle(String title)
    {
        init();
        return title2id.get(title);
    }

    @Override
    public String remove(Object id)
    {
        init();

        String title = id2title.remove(id);
        if( title != null )
        {
            title2id.remove(title);
            valid = false;
        }

        return title;
    }

    @Override
    public String put(String id, String title)
    {
        init();
        return putInternal(id, title);
    }

    protected String putInternal(String id, String title)
    {
        String oldTitle = id2title.remove(id);

        if( title != null )
            title2id.remove(oldTitle);

        if( oldTitle == null && title2id.containsKey(title) )
        {
            String newTitle = getCompositeName(title, id);
            id2title.put(id, newTitle);
            title2id.put(newTitle, id);
        }
        else
        {
            id2title.put(id, title);
            title2id.put(title, id);
        }
        valid = false;

        return oldTitle;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> map)
    {
        init();

        EntryStream.of( map ).forKeyValue( this::putInternal );
    }

    @Override
    public void clear()
    {
        id2title.clear();
        title2id.clear();

        init = false;
        valid = false;
    }

    @Override
    public Set<String> keySet()
    {
        init();
        return id2title.keySet();
    }

    @Override
    public Collection<String> values()
    {
        init();
        return id2title.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet()
    {
        init();
        return id2title.entrySet();
    }
}
