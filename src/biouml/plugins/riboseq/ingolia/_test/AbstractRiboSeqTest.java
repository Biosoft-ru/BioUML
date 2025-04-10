package biouml.plugins.riboseq.ingolia._test;

import java.io.File;
import java.util.logging.Logger;

import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.importer.BEDTrackImporter;
import ru.biosoft.table.TableCSVImporter;

public class AbstractRiboSeqTest extends AbstractBioUMLTest
{
    private static final Logger log = Logger.getLogger( TestBuildProfileModel.class.getName() );
    private static final String TEST_REPOSITORY_PATH = "../data/test/biouml/plugins/riboseq/data/";

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( TEST_REPOSITORY_PATH );
        CollectionFactory.registerRoot( new VectorDataCollection<>( "live" ) );
    }

    protected File getFile(String name)
    {
        DataElementPath path = DataElementPath.create( "databases/riboseq_data/files" ).getChildPath( name );
        FileDataElement de = path.getDataElement( FileDataElement.class );
        return de.getFile();
    }

    protected void importBEDFile(File file, DataElementPath path) throws Exception
    {
        BEDTrackImporter importer = new BEDTrackImporter();
        importer.doImport( path.getParentCollection(), file, path.getName(), null, log );
    }

    protected void importTable(File file, DataElementPath path) throws Exception
    {
        TableCSVImporter importer = new TableCSVImporter();
        importer.doImport( path.getParentCollection(), file, path.getName(), null, log );
    }
}
