package biouml.plugins.test.access;

import java.util.logging.Logger;

import ru.biosoft.access.support.BeanInfoEntryTransformer;
import biouml.plugins.test.AcceptanceTestSuite;

public class TestSuiteTransformer extends BeanInfoEntryTransformer<AcceptanceTestSuite>
{
    protected static final Logger log = Logger.getLogger(TestSuiteTransformer.class.getName());

    @Override
    public void initCommands(Class<AcceptanceTestSuite> type)
    {
        super.initCommands(type);

        addCommand(new TestsTagCommand("TE", this));
    }

    @Override
    public Class<AcceptanceTestSuite> getOutputType()
    {
        return AcceptanceTestSuite.class;
    }
}
