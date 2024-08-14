package biouml.plugins.test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.test.tests.Test;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElementSupport;

public class AcceptanceTestSuite extends MutableDataElementSupport implements PropertyChangeListener
{
    protected static final Logger log = Logger.getLogger(AcceptanceTestSuite.class.getName());

    ////////////////////////////////////////////////////////
    // properties
    //
    private String stateName;
    private Test[] tests;
    private Status status;
    private long duration;
    private long timeLimit;

    protected SimulationResult simulationResult;

    public AcceptanceTestSuite(DataCollection origin, String name)
    {
        super(origin, name);
        this.tests = new Test[0];
    }

    public String getStateName()
    {
        return stateName;
    }

    public void setStateName(String stateName)
    {
        String oldValue = this.stateName;
        this.stateName = stateName;
        firePropertyChange("stateName", oldValue, stateName);
    }

    public long getTimeLimit()
    {
        return timeLimit;
    }

    public void setTimeLimit(long timeLimit)
    {
        long oldValue = this.timeLimit;
        this.timeLimit = timeLimit;
        firePropertyChange("timeLimit", oldValue, timeLimit);
    }

    private double maxTime = 100;
    public double getMaxTime()
    {
        return maxTime;
    }

    public void setMaxTime(double t)
    {
        double oldValue = this.maxTime;
        this.maxTime = t;
        firePropertyChange("maxTime", oldValue, maxTime);
    }

    
    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public long getDuration()
    {
        return duration;
    }

    public void setDuration(long duration)
    {
        this.duration = duration;
    }

    public Test[] getTests()
    {
        return tests;
    }

    public void addTest(Test test)
    {
        Test[] newTests = new Test[tests.length + 1];
        System.arraycopy(tests, 0, newTests, 0, tests.length);
        newTests[tests.length] = test;

        Test[] oldValue = tests;
        tests = newTests;
        firePropertyChange("tests", oldValue, tests);
        test.addPropertyChangeListener(this);
    }

    public void removeTest(Test test)
    {
        for( int i = 0; i < tests.length; i++ )
        {
            if( tests[i] == test )
            {
                Test[] newTests = new Test[tests.length - 1];
                System.arraycopy(tests, 0, newTests, 0, i);
                System.arraycopy(tests, i + 1, newTests, i, tests.length - i - 1);
                Test[] oldValue = tests;
                tests = newTests;
                firePropertyChange("tests", oldValue, tests);
                break;
            }
        }
        test.removePropertyChangeListener(this);
    }


    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        firePropertyChange("tests.test", null, tests);
    }

    public SimulationResult test(Diagram diagram)
    {
        status = Status.SUCCESS;
        if( tests.length == 0 )//simulation is not necessary
            return null;

        String resultName = diagram.getName();
        if( stateName != null )
            resultName += "_" + stateName;
        SimulationResult simulationResult = new SimulationResult(null, resultName);
        TestThread testThread = new TestThread(diagram, simulationResult);
        testThread.start();

        long watchDog = timeLimit; //time limit in milliseconds
        boolean useWatchDog = ( watchDog != 0 );
        while( testThread.isAlive() )
        {
            if( useWatchDog )
            {
                watchDog -= 1000;
                if( watchDog <= 0 )
                {
                    testThread.stop();
                    status = Status.TIME_LIMIT;
                    return null;
                }
            }
            try
            {
                Thread.sleep(1000);
            }
            catch( Throwable t )
            {
            }
        }
        return simulationResult;
    }

    class TestThread extends Thread
    {
        protected Diagram diagram;
        protected SimulationResult simulationResult;

        public TestThread(Diagram diagram, SimulationResult simulationResult)
        {
            this.diagram = diagram;
            this.simulationResult = simulationResult;
        }

        @Override
        public void run()
        {
            long time = System.currentTimeMillis();
            try
            {
                if( stateName != null && stateName.length() > 0 )
                    diagram.setCurrentStateName(stateName);
                SimulationEngine se = new JavaSimulationEngine();
                se.setDiagram(diagram);
se.setCompletionTime( maxTime );
                try
                {
                    se.simulate(se.createModel(), simulationResult);

                    for( Test test : tests )
                    {
                        String testResult = test.test(simulationResult, se);
                        if( testResult != null )
                        {
                            status = Status.ERROR;
                            test.setError(testResult);
                        }
                    }
                }
                catch( Exception e )
                {
                    status = Status.EXCEPTION;
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Simulation error", t);
            }
            finally
            {
                diagram.restore();
                duration = System.currentTimeMillis() - time;
            }
        }
    }
}
