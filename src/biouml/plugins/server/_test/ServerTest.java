package biouml.plugins.server._test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.server.access.AccessClient;
import biouml.plugins.server.access.ClientDataCollection;
import biouml.standard.type.Protein;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.AccessCoreInit;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;
import ru.biosoft.server.Request;


/**
 * Tests client/server communication.
 * ClientConnectionStub is used instead of real server connection.
 */
public class ServerTest extends TestCase
{
    public static final String serverRepositoryPath = "../data/test/ru/biosoft/access/server/server";
    public static final String clientRepositoryPath = "../data/test/ru/biosoft/access/server/client";

    public ServerTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ServerTest.class.getName());

        //prepare server repository
        suite.addTest(new ServerTest("testServerRepository"));
        
        //test access client (directly by AccessClient)
        suite.addTest(new ServerTest("testGetDataCollection"));
        suite.addTest(new ServerTest("testGetDescription"));
        suite.addTest(new ServerTest("testGetNameList"));
        suite.addTest(new ServerTest("testGetEntry"));
        suite.addTest(new ServerTest("testGetEntriesSet"));
        suite.addTest(new ServerTest("testWriteEntry"));
        suite.addTest(new ServerTest("testRemoveEntry"));

        //test client data collection (using client data collection configuration files)
        suite.addTest(new ServerTest("testClientRepository"));
        suite.addTest(new ServerTest("testGetClientDataCollection"));
        suite.addTest(new ServerTest("testTestClientDataCollectionGetSize"));
        suite.addTest(new ServerTest("testTestClientDataCollectionGetNameList"));
        suite.addTest(new ServerTest("testTestClientDataCollectionContains"));
        suite.addTest(new ServerTest("testTestClientDataCollectionModify"));

        return suite;
    }

    ///////////////////////////////////////////////////////////////////
    // Test Server
    //
    
    

    public void testServerRepository() throws Exception
    {
        File file = new File(serverRepositoryPath + "/Module/Data/protein.dat");
        if( file.exists() )
            file.delete();
        file = new File(serverRepositoryPath + "/Module/Data/protein.dat.id");
        if( file.exists() )
            file.delete();
        ApplicationUtils.copyFile(serverRepositoryPath + "/Module/Data/protein.dat", serverRepositoryPath + "/Module/Data/protein.dat.orig");

        String path = serverRepositoryPath + "/" + DataCollectionConfigConstants.DEFAULT_CONFIG_FILE;
        File f = new File(path);
        assertTrue("Can not find repository config path, absolute path=" + f.getAbsolutePath(), f.exists());

        DataCollection serverRepository = CollectionFactory.createRepository(serverRepositoryPath);
        assertNotNull("Can not load repository" + f.getAbsolutePath(), serverRepository);
    }

    ///////////////////////////////////////////////////////////////////
    // Test Access Client
    //

    static DataCollection dc = null;
    static AccessClient connection = null;
    static DataElementPath serverDC = DataElementPath.create("server/Module/Data/protein");

    @Override
    protected void setUp() throws Exception
    {
        AccessCoreInit.init();
        Request request = new Request(new ClientConnectionStub("localhost", 0), Logger.getLogger(ServerTest.class.getName()));
        connection = new AccessClient(request, Logger.getLogger(ServerTest.class.getName()));
        assertNotNull("Cannot connect", connection);
    }

    public void testGetDataCollection() throws Exception
    {
        dc = serverDC.optDataCollection();
        assertNotNull("Invalid test data collection", dc);
    }

    public void testGetDescription() throws Exception
    {
        String result = connection.getDescription(serverDC);
        assertEquals("Wrong description", "Database: protein\nAvailability: public\nDescription: protein description", result);
    }

    public void testGetNameList() throws Exception
    {
        List<String> result = connection.getNameList(serverDC);
        assertEquals("Wrong list size", dc.getSize(), result.size());
    }

    public void testGetEntry() throws Exception
    {
        String result = connection.getEntry(serverDC, "cdc");
        String endl = System.getProperty("line.separator");
        String entry = "ID  cdc" + endl + "TI  cdc" + endl + "FN  inactive" + endl + "AT  " + endl + "//" + endl;
        assertEquals("Wrong entry cdc length", entry.length(), result.length());
        assertEquals("Wrong entry cdc", entry, result);
    }

    public void testGetEntriesSet() throws Exception
    {
        List<String> ids = new ArrayList<>();
        ids.add("cdc-p");
        ids.add("cyclin");
        ids.add("cdc");
        String endl = System.getProperty("line.separator");
        String result = connection.getEntriesSet(serverDC, ids);
        assertEquals("Wrong result length", 122 + 16 * endl.length(), result.length());
    }

    public void testWriteEntry() throws Exception
    {
        String endl = System.getProperty("line.separator");
        String entry = "ID  cdc_test" + endl + "TI  cdc_test" + endl + "FN  inactive" + endl + "//" + endl;
        connection.writeEntry(serverDC, "cdc_test", entry);
        assertNotNull("Cannot write entry", dc.get("cdc_test"));
    }

    public void testRemoveEntry() throws Exception
    {
        connection.removeEntry(serverDC, "cdc_test");
        assertFalse("Cannot remove entry", connection.containsEntry(serverDC, "cdc_test"));
    }

    ///////////////////////////////////////////////////////////////////
    // Test ClientDataCollection
    //

    private static ClientDataCollection cdc = null;

    public void testClientRepository() throws Exception
    {
        String path = clientRepositoryPath + "/" + DataCollectionConfigConstants.DEFAULT_CONFIG_FILE;
        File f = new File(path);
        assertTrue("Can not find client repository config path, absolute path=" + f.getAbsolutePath(), f.exists());

        DataCollection clientRepository = CollectionFactory.createRepository(clientRepositoryPath);
        assertNotNull("Can not load repository" + f.getAbsolutePath(), clientRepository);
    }

    public void testGetClientDataCollection() throws Exception
    {
        cdc = DataElementPath.create("client/Module/Data/protein").getDataElement(ClientDataCollection.class);
    }

    public void testTestClientDataCollectionGetSize() throws Exception
    {
        cdc.releaseCache();
        assertTrue("Invalid size", dc.getSize() == cdc.getSize());
    }

    public void testTestClientDataCollectionGetNameList() throws Exception
    {
        cdc.releaseCache();
        assertTrue("Invalid name list", dc.getNameList().size() == cdc.getNameList().size());
    }

    public void testTestClientDataCollectionContains() throws Exception
    {
        assertTrue("Invalid contains 1", cdc.contains("cdc"));
        assertFalse("Invalid contains 2", cdc.contains("xfdjdfkdgjgdfxtr"));
    }

    public void testTestClientDataCollectionModify() throws Exception
    {
        Protein protein = null;
        dc.put(protein = new Protein(dc, "Hellowean"));
        cdc.releaseCache();
        assertTrue("Invalid obtain new name list", cdc.contains(protein.getName()));
        Protein clientProtein = (Protein)cdc.get(protein.getName());
        assertNotNull("Cannot get element from client data collection", clientProtein);
        dc.remove(protein.getName());
    }
}
