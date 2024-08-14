package biouml.plugins.sbml._test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultTestLogger implements TestLogger
{
    public static class TestDescription
    {
        public String categoryName;
        public String name;
        public int status;
        public String messages;
        public long simulationTime;
        public Exception exception;

        public TestDescription(String categoryName, String name, int status, String messages, Exception exception, long simulationTime)
        {
            this.categoryName = categoryName;
            this.name = name;
            this.status = status;
            this.messages = messages;
            this.exception = exception;
            this.simulationTime = simulationTime;
        }
    }

    public static class StatusInfo
    {
        public int status;
        public String columnTitle;
        public boolean showColumn;
        public String statusTitle;
        public int testNumber;
        public String description;

        public StatusInfo(int status, String columnTitle, boolean showColumn, String statusTitle, String description)
        {
            this.status = status;
            this.columnTitle = columnTitle;
            this.showColumn = showColumn;
            this.statusTitle = statusTitle;
            this.description = description;
        }
    }

    protected static final Logger log = Logger.getLogger(DefaultTestLogger.class.getName());

    public int MAX_ERROR_NUMBER = 3;
    public int MAX_WARNING_NUMBER = 5;

    protected int errorNumber = 0;
    protected int warningNumber = 0;

    public DefaultTestLogger(String title)
    {
        log.info("Semantic tests: " + title);
    }

    protected String currentCategory;
    @Override
    public String getCurrentCategory()
    {
        return currentCategory;
    }
    @Override
    public void categoryStarted(String category)
    {
        currentCategory = category;
        log.info("\r\nCATEGORY: " + category);
    }

    protected String currentTest;
    @Override
    public String getCurrentTest()
    {
        return currentTest;
    }

    @Override
    public void testStarted(String testName)
    {
        currentTest = testName;
        status = Status.SUCCESSFULL;
        messages = null;
        exception = null;
    }

    protected int status;
    @Override
    public int getStatus()
    {
        return status;
    }

    protected String messages;
    @Override
    public String getMessages()
    {
        return messages;
    }

    protected Exception exception;
    @Override
    public Exception getException()
    {
        return exception;
    }

    @Override
    public void warn(String message)
    {
        message = "Warnning: " + message;

        if( messages == null )
            messages = message;
        else
        {
            if( warningNumber < MAX_WARNING_NUMBER )
                messages += "\r\n" + message;
            else if( warningNumber == MAX_WARNING_NUMBER )
                messages += "\r\n... there are other warnings...";
        }

        warningNumber++;
    }

    @Override
    public void error(Exception e)
    {
        status = Status.FAILED;
        exception = e;
    }

    @Override
    public void error(int status, String message)
    {
        this.status = status;
        message = "Error: " + message;

        if( messages == null )
            messages = message;
        else
        {
            if( errorNumber < MAX_ERROR_NUMBER )
                messages += "\r\n" + message;
            else if( errorNumber == MAX_ERROR_NUMBER )
                messages += "\r\n... there are other erors...";
        }

        errorNumber++;
    }

    @Override
    public void testCompleted()
    {
        String msg = "  " + currentTest + ": " + status;
        if( status == Status.SUCCESSFULL )
        {
            msg += ", time " + getSimulationTime();
            if( messages != null )
                msg += "\r\n" + messages;
            log.info(msg);
        }
        else
        {
            if( messages != null )
                msg += messages;

            if( exception == null )
                log.log(Level.SEVERE, msg);
            else
                log.log(Level.SEVERE, msg + "\r\n Exception: " + exception, exception);
        }
    }

    protected long simStarted;

    protected List<TestDescription> testDescriptions = new ArrayList<>();
    @Override
    public void simulationStarted()
    {
        simStarted = System.currentTimeMillis();
    }

    protected long simCompleted;

    protected Map<Integer, StatusInfo> statusMap;
    @Override
    public void simulationCompleted()
    {
        simCompleted = System.currentTimeMillis();
    }

    @Override
    public long getSimulationTime()
    {
        if( simCompleted > simStarted )
            return simCompleted - simStarted;

        return 0;
    }

    @Override
    public void complete()
    {
    }

    protected long initStatusMap(Iterator i, Map<String, TestDescription> testMap)
    {
        statusMap = new TreeMap<>();

        put(Status.SUCCESSFULL, "Successfull",  true, "ok", null);
        put(Status.FAILED, "Failed", true, "<font color=\"red\">failed</font>",
            "an exception has occured");

        put(Status.NUMERICALLY_WRONG, "Errors", true, "<font color=\"pink\">error</font>",
            "simulation results significantly differ from the known ones");

        put(Status.NEEDS_TUNING, "Needs tuning", true, "<font color=\"orange\">needs tuning</font>",
            "relative error is not small enough");

        put(Status.RESULT_DIFFER, "Result differs", true, "<font color=\"#800000\">result differs</font>",
            "some variable or time point is missing in simulation engine output." );

        put(Status.CSV_ERROR, "CSV error",    true,   "<font color=\"magenta\">CSV error</font>",
            "original CSV data is missing or can not be parsed");

        if( i == null )
            return 0;

        int totalTime = 0;
        while( i.hasNext() )
        {
            Object obj = i.next();
            TestDescription test = null;
            if( obj instanceof TestDescription )
                test = (TestDescription)obj;
            else
                test = testMap.get(obj);

            StatusInfo status = statusMap.get(test.status);
            if( status != null )
                status.testNumber++;
            else
            {
                log.log(Level.SEVERE, "Unknown status for test " + test.categoryName + "-" + test.name +
                          ", status=" + test.status);
            }

            totalTime += test.simulationTime;
        }

        return totalTime;
    }

    private void put(int status, String columnTitle, boolean showColumn, String statusTitle, String description)
    {
        statusMap.put(status, new StatusInfo(status, columnTitle, showColumn, statusTitle, description));
    }
}
