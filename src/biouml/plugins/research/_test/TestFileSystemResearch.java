package biouml.plugins.research._test;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Module;
import biouml.plugins.research.FileSystemModuleUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

public class TestFileSystemResearch extends AbstractBioUMLTest
{
    public void test() throws Exception
    {
        File targetDir = null, repositoryDir = null;
        LocalRepository repository = null;
        try
        {
            targetDir = TempFiles.dir( "target" );
            repositoryDir = TempFiles.dir( "repository" );
            ApplicationUtils.writeString( new File(targetDir, "test.txt"), "Test file" );
            ExProperties properties = new ExProperties();
            properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "test" );
            properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
            properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, repositoryDir.getAbsolutePath() );
            repository = new LocalRepository( null, properties );
            CollectionFactory.registerRoot( repository );
            FileSystemModuleUtils.createResearch( targetDir, repository, "TestRepo" );
            assertEquals( "Test file", DataElementPath.create( "test/TestRepo/Data/test.txt" ).getDataElement( TextDataElement.class )
                    .getContent() );
            assertEquals(targetDir, FileSystemModuleUtils.getTargetDirectory( DataElementPath.create("test/TestRepo").getDataElement( Module.class )));
            assertNotNull(FileSystemModuleUtils.getModuleByTarget( targetDir, DataElementPath.create("test").getDataCollection()));
            assertNull(FileSystemModuleUtils.getModuleByTarget( repositoryDir, DataElementPath.create("test").getDataCollection()));
        }
        finally
        {
            if(repository != null)
            {
                repository.close();
            }
            ApplicationUtils.removeDir( repositoryDir );
            ApplicationUtils.removeDir( targetDir );
        }
    }
}
