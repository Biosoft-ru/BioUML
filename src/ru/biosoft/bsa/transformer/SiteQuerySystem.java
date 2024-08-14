
package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.logging.Level;
import java.util.Properties;
import java.util.AbstractMap.SimpleEntry;

import java.util.logging.Logger;

import ru.biosoft.access.BTreeIndex;
import ru.biosoft.access.BTreeRangeIndex;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Key;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.Index.StringIndexEntry;
import ru.biosoft.bsa.Site;

/**
 *
 * @pending rebuild indexes
 * @pending remove basis index
 */
public class SiteQuerySystem implements QuerySystem
{
    /**
     * Indicates whether site indexes should be created.
     * @see MapFileTransformer.
     */
    final public static String USE_SITE_INDEXES = "useSiteIndexes";

    final public static String BASIS = "basis";
    final public static String FROM  = "from";
    final public static String TO    = "to";
    final public static int DEFAULT_INDEX_BLOCK_SIZE = 4096;

    protected static final Logger log = Logger.getLogger(SiteQuerySystem.class.getName());

    private String indexPath;
    private int    blockSize;

    private Index fromIndex;
    private Index toIndex;

    ////////////////////////////////////////////////////////////////////////////
    //
    //

    public SiteQuerySystem(DataCollection dc)
    {
        if (dc == null)
            return;

        Properties properties = dc.getInfo().getProperties();
        indexPath = properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY) +
                    "/" + dc.getName();

        blockSize = DEFAULT_INDEX_BLOCK_SIZE;
        String blockSizeProperty = properties.getProperty(QuerySystem.INDEX_BLOCK_SIZE);

        if( blockSizeProperty != null )
        {
            try
            {
                blockSize = Integer.parseInt(blockSizeProperty);
            }
            catch (NumberFormatException nfe)
            {
                log.log(Level.SEVERE, "Can not parse index block size '" + blockSizeProperty + "' for " + dc.getCompletePath(), nfe);
            }
        }

        dc.addDataCollectionListener(this);

        try
        {
            openIndexes();

            boolean rebuildFrom  = !fromIndex.isValid();
            boolean rebuildTo    = !toIndex.isValid();

            if( rebuildFrom || rebuildTo )
            {
                if (rebuildFrom)
                    fromIndex.clear();
                if (rebuildTo)
                    toIndex.clear();

/*
                Iterator iter = dc.iterator();
                while (iter.hasNext())
                {
                    Site site = (Site)iter.next();
                    int basis = site.getBasis();
                    int from  = site.getFrom();
                    int to    = site.getTo();
                    if (rebuildBasis)
                        basisIndex.put(
                            new BTreeIndex.IntKey(basis),
                            new StringIndexEntry(site.getName()));
                    if (rebuildFrom)
                        fromIndex.put(
                            new BTreeIndex.IntKey(from),
                            new StringIndexEntry(site.getName()));
                    if (rebuildTo)
                        toIndex.put(
                            new BTreeIndex.IntKey(to),
                            new StringIndexEntry(site.getName()));
                }
*/

            }
            close();
        }
        catch (Throwable t)
        {
            log.log(Level.SEVERE, "Can not initialize SiteQuerySystem for " + dc.getCompletePath(), t);
            close();
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    protected void openIndexes()
    {
        try
        {
            if(fromIndex == null )
                fromIndex = new BTreeRangeIndex(new File(indexPath), "from.id",  indexPath, blockSize);

            if( toIndex == null )
                toIndex = new BTreeRangeIndex(new File(indexPath), "to.id",    indexPath, blockSize);
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, "Can not open SiteQuerySystem indexes, path=" + indexPath, t);
        }
    }

    @Override
    public void close()
    {
        try
        {
            if (fromIndex != null)
                fromIndex.close();
            fromIndex = null;

            if (toIndex != null)
                toIndex.close();
            toIndex = null;
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
        }
    }

    @Override
    protected void finalize()
    {
        close();
    }


    @Override
    public Index getIndex(String name)
    {
        try
        {
            openIndexes();

            if (name.compareTo(FROM) == 0)
                return fromIndex;

            if (name.compareTo(TO) == 0)
                return toIndex;
        }
        catch (Throwable t)
        {
            log.log(Level.SEVERE, "Can not return index " + name + ".", t);
        }

        return null;
    }

    @Override
    public Index[] getIndexes()
    {
        openIndexes();
        Index[] indexes = {getIndex(FROM), getIndex(TO)};
        return indexes;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Interface ru.biosoft.access.core.DataCollectionListener
    //

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        openIndexes();

        Site site = null;
        String name = null;
        try
        {
            name = e.getDataElementName();
            site = ( Site )e.getOwner().get( name );

            fromIndex.put(new BTreeRangeIndex.IntKey( site.getFrom() ), new StringIndexEntry( name ) );
            toIndex.put(new BTreeRangeIndex.IntKey( site.getTo() ), new StringIndexEntry( name ) );
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, "Can not add site index for " + name, t);
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        openIndexes();

        Site site = null;
        String name = null;
        try
        {
            name = e.getDataElementName();
            site = ( Site )e.getOwner().get( name );

            Key key      = null;
            Object entry = null;

            key = new BTreeRangeIndex.IntKey(site.getFrom());
            entry = new SimpleEntry<>(key, new StringIndexEntry(name));
            fromIndex.remove(entry);

            key = new BTreeIndex.IntKey(site.getTo());
            entry = new SimpleEntry<>(key, new StringIndexEntry(name));
            toIndex.remove(entry);
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, "Can not remove site index for " + name, t);
        }
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
    }

    @Override
    public void elementWillChange(DataCollectionEvent e)
        throws DataCollectionVetoException, Exception
    {
    }


    @Override
    public void elementWillRemove(DataCollectionEvent e)
        throws DataCollectionVetoException, Exception
    {
    }
}

