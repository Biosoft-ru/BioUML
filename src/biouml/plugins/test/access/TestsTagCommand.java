package biouml.plugins.test.access;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import biouml.plugins.test.AcceptanceTestSuite;
import biouml.plugins.test.tests.Test;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;

class TestsTagCommand implements TagCommand
{
    private static final String LN = System.getProperty("line.separator");
    private static final String DELIMITER = ";";

    private final String tag;
    private final TagEntryTransformer<AcceptanceTestSuite> transformer;
    private List<String> testStrings;

    public TestsTagCommand(String tag, TagEntryTransformer<AcceptanceTestSuite> transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    @Override
    public void start(String tag)
    {
        testStrings = new ArrayList<>();
    }

    @Override
    public void addValue(String value)
    {
        testStrings.add(value);
    }

    @Override
    public void complete(String tag)
    {
        AcceptanceTestSuite testSuite = transformer.getProcessedObject();
        for( String testString : testStrings )
        {
            int ind = testString.indexOf(DELIMITER);
            if( ind != -1 )
            {
                String className = testString.substring(0, ind);
                String params = testString.substring(ind + 1);
                try
                {
                    Test test = ClassLoading.loadSubClass( className, Test.class ).newInstance();
                    test.loadFromString(params);
                    testSuite.addTest(test);
                }
                catch( Exception e )
                {
                    TestSuiteTransformer.log.log(Level.SEVERE, "Cannot load test", e);
                }
            }
        }
    }

    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        AcceptanceTestSuite testSuite = transformer.getProcessedObject();
        Test[] tests = testSuite.getTests();
        if( tests.length == 0 )
            return null;
        StringBuffer value = new StringBuffer();
        value.append(tag);
        for( int i = 0; i < tests.length; i++ )
        {
            Test test = tests[i];
            if(i>0)
                value.append("  ");
            value.append("  ").append(test.getClass().getName()).append(DELIMITER).append(test.toString()).append(LN);
        }
        return value.toString();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}