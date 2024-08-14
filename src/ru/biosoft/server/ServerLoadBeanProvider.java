package ru.biosoft.server;

import java.io.BufferedReader;
import java.lang.Thread.State;

import org.apache.commons.lang.SystemUtils;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.BeanProvider;

/**
 * @author lan
 *
 */
public class ServerLoadBeanProvider implements BeanProvider
{
    private static ServerLoad bean = SystemUtils.IS_OS_WINDOWS?new WindowsServerLoad():new LinuxServerLoad();
    
    @Override
    public Object getBean(String path)
    {
        return bean;
    }
    
    public static abstract class ServerLoad
    {
        private static final long DELAY_TIME = 30*1000; // 30 seconds between updates
        private long lastUpdateMillis = 0;
        private int runningThreads;
        
        protected void doUpdate()
        {
            runningThreads = fetchRunningThreads();
        }
        
        protected int fetchRunningThreads()
        {
            Thread[] tarray = new Thread[Thread.activeCount()];
            int nThreads = Thread.enumerate(tarray);
            int runningThreads = 0;
            for(int i=0; i<nThreads; i++)
            {
                Thread thread = tarray[i];
                if(thread.getState() == State.RUNNABLE && !thread.getName().startsWith("http-")
                        && !thread.getName().startsWith("TP-Processor") && !thread.getName().equals("main")
                        && !thread.getName().equals("AWT-Windows")) runningThreads++;
            }
            return runningThreads;
        }
        
        protected final void checkUpdate()
        {
            long currentTime = System.currentTimeMillis();
            if(currentTime-lastUpdateMillis>DELAY_TIME)
            {
                synchronized(this)
                {
                    if(currentTime-lastUpdateMillis>DELAY_TIME)
                    {
                        doUpdate();
                        lastUpdateMillis = System.currentTimeMillis();
                    }
                }
            }
        }
        
        @PropertyName("Running threads")
        @PropertyDescription("Threads which are currently running")
        public int getRunningThreads()
        {
            checkUpdate();
            return runningThreads;
        }
    }
    
    public static class WindowsServerLoad extends ServerLoad
    {
        
    }
    
    public static class WindowsServerLoadBeanInfo extends BeanInfoEx
    {
        public WindowsServerLoadBeanInfo()
        {
            super( WindowsServerLoad.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("runningThreads", beanClass, "getRunningThreads", null));
        }
    }
    
    public static class LinuxServerLoad extends ServerLoad
    {
        private static final String LOAD_AVERAGE_FILE = "/proc/loadavg";
        private float loadAverage;
        
        @Override
        protected void doUpdate()
        {
            super.doUpdate();
            loadAverage = fetchLoadAverage();
        }
        
        protected float fetchLoadAverage()
        {
            try(BufferedReader br = ApplicationUtils.asciiReader( LOAD_AVERAGE_FILE ))
            {
                String line = br.readLine();
                if( line != null )
                    return Float.parseFloat( line.split( "\\s" )[1] ) * 100 / Runtime.getRuntime().availableProcessors();
                return Float.NaN;
            }
            catch(Exception e)
            {
                return Float.NaN;
            }
        }

        @PropertyName("Average load (last 5 min)")
        @PropertyDescription("Average server load during last 5 minutes (%%)")
        public double getLoadAverage()
        {
            checkUpdate();
            return loadAverage;
        }
    }

    public static class LinuxServerLoadBeanInfo extends BeanInfoEx
    {
        public LinuxServerLoadBeanInfo()
        {
            super( LinuxServerLoad.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("runningThreads", beanClass, "getRunningThreads", null));
            add(new PropertyDescriptorEx("loadAverage", beanClass, "getLoadAverage", null));
        }
    }
}
