package ru.biosoft.access.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.history.HistoryElement.Type;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * Facade to work with history functionality
 */
public class HistoryFacade
{
    protected static final Logger log = Logger.getLogger(HistoryFacade.class.getName());

    public static final String HISTORY_COLLECTION = "history-collection";

    /**
     * Check if element has history aspect
     */
    public static boolean hasHistory(DataElement de)
    {
        DataCollection dc = de.getOrigin();
        if( dc == null )
            return false;
        return ( dc.getInfo().getProperty(HISTORY_COLLECTION) != null );
    }

    /**
     * Check if comment is necessary for saving data element
     */
    public static boolean needComment(DataElement de)
    {
        if( hasHistory(de) )
        {
            DataCollection dc = de.getOrigin();
            return dc.contains(de.getName());
        }
        return false;
    }

    /**
     * Get history collection for {@link ru.biosoft.access.core.DataElement}
     */
    public static HistoryDataCollection getHistoryCollection(DataElement de)
    {
        if( de.getOrigin() != null )
        {
            String historyStr = de.getOrigin().getInfo().getProperty(HISTORY_COLLECTION);
            if( historyStr != null )
            {
                return DataElementPath.create(historyStr).optDataElement(HistoryDataCollection.class);
            }
        }
        return null;
    }

    /**
     * Adds history listener for data collection if necessary
     */
    public static void addHistoryListener(DataCollection dc)
    {
        String historyStr = dc.getInfo().getProperty(HISTORY_COLLECTION);
        if( historyStr != null )
        {
            HistoryDataCollection historyDC = DataElementPath.create(historyStr).optDataElement(HistoryDataCollection.class);
            if( historyDC != null )
            {
                dc.addDataCollectionListener(new HistoryListener(historyDC));
            }
        }
    }

    /**
     * Build history element for changes
     */
    public static HistoryElement getDifference(String name, int version, DataElement oldElement, DataElement newElement)
    {
        DiffManager diffManager = getDiffManager(oldElement.getClass());
        if( diffManager != null )
        {
            try
            {
                HistoryElement historyElement = new HistoryElement(newElement.getOrigin(), name);
                DataElementPath path = DataElementPath.create(newElement);
                historyElement.setDePath(path);
                historyElement.setType(Type.CHANGES);
                historyElement.setTimestamp(new Date());
                historyElement.setVersion(version);
                historyElement.setAuthor(SecurityManager.getSessionUser());
                Map<ru.biosoft.access.core.DataElementPath, String> threadComments = comments.get();
                historyElement.setComment(threadComments == null ? null : threadComments.remove(path));
                diffManager.fillDifference(historyElement, oldElement, newElement);
                return historyElement;
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot create history element", e);
            }
        }
        return null;
    }

    protected static DataElement getLatestSavedVersion(DataElementPath path)
    {
        DataCollection parentCollection = path.optParentCollection();
        if( parentCollection == null )
            return null;
        try
        {
            if( parentCollection instanceof AbstractDataCollection )
                return ( (AbstractDataCollection)parentCollection ).getNonCached(path.getName());
            return parentCollection.get(path.getName());
        }
        catch( Exception e )
        {
        }
        return null;
    }

    /**
     * Return old version of the element
     * @param de - element to get old version of (this element is not changed)
     * @param version - version number to get
     */
    public static DataElement getVersion(DataElement de, int version)
    {
        HistoryDataCollection historyDC = HistoryFacade.getHistoryCollection(de);
        if( historyDC == null )
            return null;
        DiffManager diffManager = getDiffManager(de.getClass());
        if( diffManager == null )
            return null;
        DataElementPath path = DataElementPath.create(de);
        de = getLatestSavedVersion(path);
        try
        {
            List<HistoryElement> elements = new ArrayList<>();
            for( String id : historyDC.getHistoryElementNames(path, version) )
            {
                elements.add((HistoryElement)historyDC.get(id));
            }
            de = diffManager.applyDifference(de, elements.toArray(new HistoryElement[elements.size()]));
            return de;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot apply changes to element", e);
        }
        return null;
    }

    /**
     * Returns ru.biosoft.access.core.DataElement representing difference between two elements
     * @param first - older element
     * @param second - newer element
     * @return
     */
    public static DataElement getDiffElement(DataElement first, DataElement second)
    {
        DiffManager diffManager = getDiffManager(first.getClass());
        if( diffManager == null )
            return null;
        // Should have equal DiffManager
        if( getDiffManager(second.getClass()) != diffManager )
            return null;
        try
        {
            return diffManager.getDifferenceElement(first, second);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to build difference element", e);
        }
        return null;
    }

    protected static DiffManager getDiffManager(Class<?> deClass)
    {
        DiffManager bestDiffManager = null;
        int bestPriority = 0;
        for( DiffManager diffManager : providers )
        {
            int priority = diffManager.getPriority(deClass);
            if( priority > bestPriority )
            {
                bestPriority = priority;
                bestDiffManager = diffManager;
            }
        }
        return bestDiffManager;
    }

    protected static final ObjectExtensionRegistry<DiffManager> providers = new ObjectExtensionRegistry<>("ru.biosoft.access.diffManager", DiffManager.class);

    //
    // comments support
    //

    private static ThreadLocal<Map<ru.biosoft.access.core.DataElementPath, String>> comments = new ThreadLocal<>();

    /**
     * Add comment to global map
     */
    public static void addComment(DataElement de, String comment)
    {
        if(comments.get() == null) comments.set(new HashMap<ru.biosoft.access.core.DataElementPath, String>());
        comments.get().put(DataElementPath.create(de), comment);
    }
}
