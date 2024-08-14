package ru.biosoft.access._test;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

/**
 * @author lan
 *
 */
public class TestGenericDataCollection extends AbstractBioUMLTest
{
    private static final DataElementPath PATH = DataElementPath.create("test/folder");
    private File dir;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        dir = TempFiles.dir("testGeneric");
        
        Properties properties = new ExProperties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "test");
        properties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());
        properties.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.toString());
        ExProperties.store(properties, new File(dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE));
        Repository repository = (Repository)CollectionFactory.createCollection(null, properties);
        CollectionFactory.registerRoot(repository);
        repository.getNameList();
        
        DataCollection<?> dc = GenericDataCollection.createGenericCollection(repository, repository, "folder", "folder");
        assertNotNull(dc);
        repository.put(dc);
    }
    
    private long getDiskSize(DataCollection<?> dc)
    {
        return Long.parseLong(dc.getInfo().getProperty(DataCollectionConfigConstants.ELEMENT_SIZE_PROPERTY));
    }
    
    public void testBasicOperations() throws Exception
    {
        GenericDataCollection gdc = PATH.getDataElement(GenericDataCollection.class);
        assertEquals(0, getDiskSize(gdc));
        String content = "Text content";
        TextDataElement test = new TextDataElement("text", gdc, content);
        gdc.put(test);
        assertEquals(content.length(), getDiskSize(gdc));
        assertEquals(content, PATH.getChildPath("text").getDataElement(TextDataElement.class).getContent());
        gdc.remove("text");
        assertFalse(PATH.getChildPath("text").exists());
        assertEquals(0, getDiskSize(gdc));
        gdc.put(test);
        assertEquals(content.length(), getDiskSize(gdc));
        
        GenericDataCollection subdc = (GenericDataCollection)gdc.createSubCollection("subfolder", GenericDataCollection.class);
        assertEquals(content.length(), getDiskSize(gdc));
        String content2 = "Text content2";
        TextDataElement test2 = new TextDataElement("text2", subdc, content2);
        subdc.put(test2);
        assertEquals(content.length()+content2.length(), getDiskSize(gdc));
        assertEquals(content2.length(), getDiskSize(subdc));
        assertEquals(content2, DataElementPath.create("test/folder/subfolder/text2").getDataElement(TextDataElement.class).getContent());
        
        gdc.remove(subdc.getName());
        assertEquals(content.length(), getDiskSize(gdc));
    }

    @Override
    protected void tearDown() throws Exception
    {
        PATH.getDataCollection().close();
        super.tearDown();
        ApplicationUtils.removeDir(dir);
    }
    
    
}
