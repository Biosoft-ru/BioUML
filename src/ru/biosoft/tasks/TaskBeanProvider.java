package ru.biosoft.tasks;

import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataCollection;

/**
 * @author lan
 *
 */
public class TaskBeanProvider implements BeanProvider
{
    @Override
    public DataCollection getBean(String path)
    {
        return TaskManager.getInstance().getTasksInfo();
    }
}
