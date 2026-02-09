package ru.biosoft.journal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.CheckForNull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * Registry class for journal functionality
 */
public class JournalRegistry
{
    public static final String JOURNAL_NAME_KEY = "journalName";

    protected static final ObjectExtensionRegistry<JournalList> journalLists = new ObjectExtensionRegistry<>(
            "ru.biosoft.workbench.journalList", JournalList.class);
    private static String currentJournalName = null;
    private static boolean useJournal = true;

    /**
     * Special journal with empty functionality
     */
    protected static final Journal journalStub = new Journal()
    {
        @Override
        public @Nonnull TaskInfo getEmptyAction()
        {
            TaskInfo result = new TaskInfo(null, null, null, null, null);
            result.setJournal(this);
            return result;
        }

        @Override
        public void addAction(@Nonnull TaskInfo action)
        {
        }

        @Override
        public void removeAction(@Nonnull TaskInfo action)
        {
        }

        @Override
        public @Nonnull Iterator<TaskInfo> iterator()
        {
            return ListUtil.emptyIterator();
        }

        @Override
        public DataElementPath getJournalPath()
        {
            return null;
        }
    };
    
    public static @CheckForNull DataElementPath getProjectPath(Journal journal)
    {
        return journal == null || journal.getJournalPath() == null ? null : journal.getJournalPath().getParentPath();
    }

    public static @CheckForNull DataElementPath getProjectPath()
    {
        return getProjectPath(getCurrentJournal());
    }

    /**
     * Get current available journal
     */
    public static @CheckForNull Journal getCurrentJournal()
    {
        SessionCache sessionCache = SessionCacheManager.getSessionCache();
        if(sessionCache != null)
        {
            Object journalName = sessionCache.getObject(JOURNAL_NAME_KEY);
            if( journalName != null && !journalName.toString().isEmpty() )
                return getCurrentJournal(journalName.toString());
        }
        return getCurrentJournal(currentJournalName);
    }

    /**
     * Get available journal by name
     */
    public static @CheckForNull Journal getCurrentJournal(String name)
    {
        if( !useJournal )
        {
            //return journal stub
            return journalStub;
        }
        Journal result = null;
        if( name != null )
        {
            for( JournalList jl : journalLists )
            {
                result = jl.getJournal(name);
                if( result != null )
                {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Set journal using flag
     */
    public static void setJournalUse(boolean use)
    {
        useJournal = use;
    }
    
    /**
     * Set name of current journal
     */
    public static void setCurrentJournal(String journalName)
    {
        currentJournalName = journalName;
        JournalProperties.setCurrentJournal(journalName);
    }

    /**
     * Get collection of all journals
     */
    public static String[] getJournalNames()
    {
        List<String> result = new ArrayList<>();
        for( JournalList jl : journalLists )
        {
            result.addAll(jl.getNameList());
        }
        return result.toArray(new String[result.size()]);
    }

    public static Journal getJournalByPath(DataElementPath path)
    {
        DataElementPath targetPath = path.getTargetPath();
        for ( JournalList jl : journalLists )
        {
            List<String> names = jl.getNameList();
            for ( String journalName : names )
            {
                Journal journal = jl.getJournal( journalName );
                if( getProjectPath( journal ) != null )
                {
                    DataElementPath projectPath = getProjectPath( journal ).getTargetPath();
                    if( projectPath.isAncestorOf( targetPath ) )
                        return journal;
                }
            }
        }
        return null;
    }
}
