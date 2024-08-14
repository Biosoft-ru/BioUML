package biouml.plugins.test.access;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import biouml.plugins.test.TestModel;
import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.support.TagEntryTransformer;

public class TestModelFileTransformer extends AbstractFileTransformer<TestModel>
{    
    private TestModelTransformer primaryTransformer = new TestModelTransformer();

    @Override
    public Class<TestModel> getOutputType()
    {
        return TestModel.class;
    }

    @Override
    public void init(DataCollection<FileDataElement> primaryCollection, DataCollection<TestModel> transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);
        primaryTransformer.init(primaryCollection, transformedCollection);
    }

    @Override
    public TestModel load(File input, String name, DataCollection<TestModel> origin) throws Exception
    {
        TestModel sr = new TestModel(origin, name);
        try( FileReader reader = new FileReader( input ) )
        {
            primaryTransformer.readObject( sr, reader );
        }
        return sr;
    }
    
    @Override
    public void save(File output, TestModel element) throws Exception
    {
        try (FileWriter fw = new FileWriter( output ))
        {
            fw.write(primaryTransformer.getStartTag() + "    " + element.getName() + TagEntryTransformer.endl);
            primaryTransformer.writeObject(element, fw);
        }
    }
}
