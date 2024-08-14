package ru.biosoft.tasks;

import ru.biosoft.access.core.AbstractTransformer;

/**
 * @author lan
 *
 */
public class TaskWrapperTransformer extends AbstractTransformer<TaskInfo, TaskInfoWrapper>
{
    @Override
    public Class<TaskInfo> getInputType()
    {
        return TaskInfo.class;
    }

    @Override
    public Class<TaskInfoWrapper> getOutputType()
    {
        return TaskInfoWrapper.class;
    }

    @Override
    public TaskInfoWrapper transformInput(TaskInfo input) throws Exception
    {
        return new TaskInfoWrapper(input);
    }

    @Override
    public TaskInfo transformOutput(TaskInfoWrapper output) throws Exception
    {
        throw new UnsupportedOperationException();
    }
}
