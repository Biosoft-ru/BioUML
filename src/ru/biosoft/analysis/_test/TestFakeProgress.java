package ru.biosoft.analysis._test;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

import junit.framework.TestCase;
import ru.biosoft.analysis.FakeProgress;

public class TestFakeProgress extends TestCase
{
    public void test1() throws Exception
    {
        JobControl jobControl = new TestJobControl( );
        FakeProgress progress = new FakeProgress( jobControl , 1000 );
        progress.start();
        Thread.sleep( 2000 );
        progress.stop();
    }
    
    public static class TestJobControl extends AbstractJobControl
    {
        public TestJobControl()
        {
            super( null );
        }

        @Override
        public void setPreparedness(int percent)
        {
            System.out.println(percent + " %");
        }

        @Override
        protected void doRun() throws JobControlException
        {
        }

    }
}
