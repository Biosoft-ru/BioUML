package ru.biosoft.access.task;

/**
 * @author lan
 *
 */
public class RunnableTask extends AbstractTask
{
    private Runnable runnable;

    public RunnableTask(String name, Runnable runnable)
    {
        super(name);
        this.runnable = runnable;
    }

    @Override
    protected void doRun()
    {
        runnable.run();
    }
}
