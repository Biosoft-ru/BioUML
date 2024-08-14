package ru.biosoft.tasks;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.task.TaskPool;

//Execute tasks in the current JVM process
public class JVMExecutor implements TaskExecutor
{
    @Override
    public void init(DynamicPropertySet config)
    {
    }
    
    @Override
    public int canExecute(TaskInfo taskInfo)
    {
        return EXECUTE_JVM;
    }

    @Override
    public void submit(TaskInfo taskInfo)
    {
        TaskPool.getInstance().submit(taskInfo.getTask());
    }

   
}
