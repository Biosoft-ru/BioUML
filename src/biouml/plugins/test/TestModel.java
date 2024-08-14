package biouml.plugins.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;

import biouml.plugins.test.tests.Test;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.MutableDataElementSupport;

public class TestModel extends MutableDataElementSupport
{
    // path to Diagram
    protected DataElementPath modelPath;
    // acceptance test set
    protected AcceptanceTestSuite[] acceptanceTests;

    public TestModel(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    public TestModel(DataCollection<?> origin, String name, DataElementPath modelPath)
    {
        super(origin, name);
        this.modelPath = modelPath;
    }

    public DataElementPath getModelPath()
    {
        return modelPath;
    }

    public void setModelPath(DataElementPath modelPath)
    {
        DataElementPath old = this.modelPath;
        this.modelPath = modelPath;
        firePropertyChange("modelPath", old, modelPath);
    }

    public AcceptanceTestSuite[] getAcceptanceTests()
    {
        return acceptanceTests;
    }

    // Set of changed test suites
    protected Set<AcceptanceTestSuite> changedTestSuites = new HashSet<>();

    protected void addChangedTestSiute(AcceptanceTestSuite testSuite)
    {
        if( !changedTestSuites.contains(testSuite) )
        {
            changedTestSuites.add(testSuite);
        }
    }

    protected Set<AcceptanceTestSuite> getChangedTestSuites()
    {
        Set<AcceptanceTestSuite> result = changedTestSuites;
        changedTestSuites = new HashSet<>();
        return result;
    }

    /**
     * Add acceptance test suite to document
     */
    public void addAcceptanceTestSuite(AcceptanceTestSuite testSuite)
    {
        if( acceptanceTests == null )
        {
            acceptanceTests = new AcceptanceTestSuite[1];
            acceptanceTests[0] = testSuite;
        }
        else
        {
            AcceptanceTestSuite[] newAcceptanceTests = new AcceptanceTestSuite[acceptanceTests.length + 1];
            System.arraycopy(acceptanceTests, 0, newAcceptanceTests, 0, acceptanceTests.length);
            newAcceptanceTests[acceptanceTests.length] = testSuite;
            acceptanceTests = newAcceptanceTests;
        }

        final AcceptanceTestSuite innerTestSuite = testSuite;
        testSuite.addPropertyChangeListener(arg0 -> {
            addChangedTestSiute(innerTestSuite);
            firePropertyChange("acceptanceTests.suite", null, innerTestSuite);
        });

        firePropertyChange("acceptanceTests", null, acceptanceTests);
    }

    /**
     * Remove acceptance test suite from document
     */
    public void removeAcceptanceTestSuite(AcceptanceTestSuite testSuite)
    {
        if( acceptanceTests != null )
        {
            for( int i = 0; i < acceptanceTests.length; i++ )
            {
                if( acceptanceTests[i] == testSuite )
                {
                    AcceptanceTestSuite[] newTestSuites = new AcceptanceTestSuite[acceptanceTests.length - 1];
                    System.arraycopy(acceptanceTests, 0, newTestSuites, 0, i);
                    System.arraycopy(acceptanceTests, i + 1, newTestSuites, i, acceptanceTests.length - i - 1);
                    AcceptanceTestSuite[] oldValue = acceptanceTests;
                    acceptanceTests = newTestSuites;
                    firePropertyChange("tests", oldValue, acceptanceTests);
                    break;
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Simulation result storage
    //

    protected Map<AcceptanceTestSuite, SimulationResult> results = new HashMap<>();

    public void addSimulationResult(AcceptanceTestSuite testSuite, SimulationResult result)
    {
        results.put(testSuite, result);
    }

    public SimulationResult getSimulationResult(Test test)
    {
        return StreamEx.ofValues(results, suite -> Stream.of(suite.getTests()).anyMatch(test::equals)).findAny()
                .orElse(null);
    }
}
