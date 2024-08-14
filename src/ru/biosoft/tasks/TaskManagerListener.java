package ru.biosoft.tasks;

/**
 * Task manager events listener interface
 */
public interface TaskManagerListener
{
    /**
     * Indicates task addition
     */
    public void taskAdded(TaskInfo ti);

    /**
     * Indicates task deletion
     */
    public void taskRemoved(TaskInfo ti);
    
    /**
     * Indicates task change
     */
    public void taskChanged(TaskInfo ti);
   
}
