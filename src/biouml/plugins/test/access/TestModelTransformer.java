package biouml.plugins.test.access;

import biouml.plugins.test.TestModel;
import ru.biosoft.access.support.BeanInfoEntryTransformer;

public class TestModelTransformer extends BeanInfoEntryTransformer<TestModel>
{
    @Override
    public void initCommands(Class<TestModel> type)
    {
        super.initCommands(type);

        addCommand(new AcceptanceTestsTagCommand("AT", this));
    }

    @Override
    public Class<TestModel> getOutputType()
    {
        return TestModel.class;
    }
}
