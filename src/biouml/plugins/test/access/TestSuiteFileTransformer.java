package biouml.plugins.test.access;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import biouml.plugins.test.AcceptanceTestSuite;
import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.support.TagEntryTransformer;

public class TestSuiteFileTransformer extends AbstractFileTransformer<AcceptanceTestSuite>
{    
    private TestSuiteTransformer primaryTransformer = new TestSuiteTransformer();

    @Override
    public Class<AcceptanceTestSuite> getOutputType()
    {
        return AcceptanceTestSuite.class;
    }

    @Override
    public void init(DataCollection<FileDataElement> primaryCollection, DataCollection<AcceptanceTestSuite> transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);
        primaryTransformer.init(primaryCollection, transformedCollection);
    }

    @Override
    public AcceptanceTestSuite load(File input, String name, DataCollection<AcceptanceTestSuite> origin) throws Exception
    {
        AcceptanceTestSuite sr = new AcceptanceTestSuite(origin, name);
        try( FileReader reader = new FileReader( input ) )
        {
            primaryTransformer.readObject( sr, reader );
        }
        return sr;
    }
    
    @Override
    public void save(File output, AcceptanceTestSuite element) throws Exception
    {
        try (FileWriter fw = new FileWriter( output ))
        {
            fw.write(primaryTransformer.getStartTag() + "    " + element.getName() + TagEntryTransformer.endl);
            primaryTransformer.writeObject(element, fw);
        }
    }
}
