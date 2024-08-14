package ru.biosoft.analysis;

import ru.biosoft.jobcontrol.JobControl;
/**
 * Emulates JobControl progress according to the 1-2^(-t/T) low
 */
public class FakeProgress
{
    private JobControl jobControl;
    private long millis50;
    private Thread thread;
    
    /**
     * @param jobControl
     * @param millis50 jobControl will reach 50% progress after millis50 milliseconds
     */
    public FakeProgress(JobControl jobControl, long millis50) {
        this.jobControl = jobControl;
        this.millis50 = millis50;
    }
    
    public void start()
    {
        thread = new Thread( () -> {
            long startTime = System.currentTimeMillis();
            jobControl.setPreparedness( 0 );
            while( !Thread.currentThread().isInterrupted() )
            {
                long spentTime = System.currentTimeMillis() - startTime;
                int progress = (int) ( 100*(1 - Math.pow( 2, -(double)spentTime/millis50 )) );
                if(progress == 100)
                    progress = 99;
                if(jobControl.getPreparedness() != progress)
                    jobControl.setPreparedness( progress );
                try
                {
                    Thread.sleep( Math.max( millis50 / 50, 100 ) );
                }
                catch( InterruptedException e )
                {
                    return;
                }
            }
        }, "Fake progress of " + Thread.currentThread().getName());
        thread.start();
    }
    
    public void stop()
    {
        thread.interrupt();
        try
        {
            thread.join();
        }
        catch( InterruptedException e )
        {
        }
        jobControl.setPreparedness( 100 );
    }
}
