package biouml.plugins.test.access;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.test.AcceptanceTestSuite;
import biouml.plugins.test.TestModel;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;

class AcceptanceTestsTagCommand implements TagCommand
{
    private static final String LN = System.getProperty("line.separator");

    private final String tag;
    private final TagEntryTransformer<TestModel> transformer;
    private List<String> acPaths;

    public AcceptanceTestsTagCommand(String tag, TagEntryTransformer<TestModel> transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    @Override
    public void start(String tag)
    {
        acPaths = new ArrayList<>();
    }

    @Override
    public void addValue(String value)
    {
        acPaths.add(value);
    }

    @Override
    public void complete(String tag)
    {
        TestModel testModel = transformer.getProcessedObject();
        for( String acPath : acPaths )
        {
            DataElement de = CollectionFactory.getDataElement(acPath);
            if( de instanceof AcceptanceTestSuite )
            {
                testModel.addAcceptanceTestSuite((AcceptanceTestSuite)de);
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
        TestModel testModel = transformer.getProcessedObject();
        AcceptanceTestSuite[] acceptanceTests = testModel.getAcceptanceTests();
        if( acceptanceTests == null || acceptanceTests.length == 0 )
            return null;
        StringBuffer value = new StringBuffer();
        value.append(tag);
        for( int i = 0; i < acceptanceTests.length; i++ )
        {
            AcceptanceTestSuite acceptanceTest = acceptanceTests[i];
            if( acceptanceTest.getOrigin() != null )
            {
                String testPath = DataElementPath.create(acceptanceTest).toString();
                if(i>0)
                    value.append("  ");
                value.append("  ").append(testPath).append(LN);
            }
        }
        return value.toString();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}