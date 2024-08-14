package ru.biosoft.tasks;

import com.developmentontheedge.beans.DynamicPropertySet;

public interface TaskExecutor
{
    public static final int EXECUTE_NO = -1;
    public static final int EXECUTE_JVM = 10;
    public static final int EXECUTE_REMOTE  = 20;
    public static final int EXECUTE_SPECIAL = 30;  

    public void init(DynamicPropertySet config);
    
    /**
     * Returns priority how it can execute the task.
     */
    public int canExecute(TaskInfo taskInfo);

   /**
    * Execute task immediately or put it in its own queue.
    * Can replace taskInfo.JobControl by executor specific.
    */
   public void submit(TaskInfo taskInfo);
}
