package ru.biosoft.journal;

import java.util.Iterator;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.tasks.TaskInfo;

/**
 * Base interface for journal
 */
public abstract class Journal implements Iterable<TaskInfo>
{
    /**
     * Add new journal element
     */
    public abstract void addAction(@Nonnull TaskInfo action);
    
    /**
     * Remove journal element
     */
    public abstract void removeAction(@Nonnull TaskInfo action);

    /**
     * Get empty object for next action
     */
    public abstract @Nonnull TaskInfo getEmptyAction();
    
    /**
     * Get iterator for elements
     */
    @Override
    public abstract @Nonnull Iterator<TaskInfo> iterator();
    
    /**
     *  Get path to journal
     */
    public abstract DataElementPath getJournalPath();
}
