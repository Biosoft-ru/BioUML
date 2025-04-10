package biouml.plugins.server._test;

import java.util.Properties;

import biouml.plugins.server.access.ClientDataCollection;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;

import ru.biosoft.util.ExProperties;

/**
 * These test uses user (currently fkolpakov@gmail.com, it has corresponding privileges) and password.
 * They should be specified as parameters, for example   
 * ../projects/java/BioUML/src/biouml/plugins/server/_test>ant client -Duser=fkolpakov@gmail.com -Dpassword=password
 * 
 * Example of request to BioUML server to get response:
 * https://ict.biouml.org/biouml/web/data?service=access.service&command=21&dc=data/Examples/FluxBalance/Data/Diagrams
 */
public class ClientDataCollectionTest extends TestCase 
{
	//public static String url = "https://ict.biouml.org/biouml/";
	public static String url = "http://192.168.220.191/biouml/";
	//public static String url = "http://localhost:8080/biouml/";
    private static ClientDataCollection data = null;
    
    public ClientDataCollectionTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ClientDataCollectionTest.class.getName());

        //suite.addTest(new ClientDataCollectionTest("testLocalClientCollection"));
        suite.addTest(new ClientDataCollectionTest("testRemoteClientCollection"));
        
        return suite;
    }

    protected void info(String completeName) throws Exception
    {
    	DataCollection dc = CollectionFactory.getDataCollection(completeName);
    	
    	assertNotNull("Can not load data collection, path=" + completeName, dc);
    	
    	System.out.println("\r\n#" + dc.getName() + " - " + dc.getClass().getName() + ": " + dc.getNameList());
    	System.out.println(dc.getInfo().getProperties());
    }

    public void initCollection() throws Exception
    {
        Environment.setClassLoading( new BiosoftClassLoading() );
        //Environment.setIconManager( new BiosoftIconManager() );

        String name 		= "data";    
        String nameOnServer	= "data";    

    	Properties properties = new ExProperties();
    	properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
    	properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());

    	properties.setProperty(ClientDataCollection.SERVER_URL, url); 
    	properties.setProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME, nameOnServer);
        properties.setProperty( DataCollectionConfigConstants.IS_ROOT, "true" );

    	data = new ClientDataCollection(null, properties);

    	String user     = System.getProperty("user");
    	String password = System.getProperty("password");
    	if( user != null && password == null && password.length() < 1 )
    		return;

    	data.login(user, password);
    }

    public void testLocalClientCollection() throws Exception
    {
    	url = "http://localhost:8080/biouml/";
    	initCollection();
   	
//    	info( "data/Examples/FluxBalance/Data/Diagrams/01187-sbml-l3v1" );

    	info( "data/Examples/FluxBalance/Data/Tables" );    	
    	info( "data/Examples/FluxBalance/Data/Tables/01187-sbml-l3v1-fbc-table" );    	
    }

    public void testRemoteClientCollection() throws Exception
    {
    	url = "https://ict.biouml.org/biouml/";
    	initCollection();

//    	info( "data/Examples" );    	
//    	info( "data/Examples/FluxBalance" );    	
//    	info( "data/Examples/FluxBalance/Data" );
//    	info( "data/Examples/FluxBalance/Data/Diagrams" );
    	
    	//info( "data/Examples/FluxBalance/Data/Diagrams/01187-sbml-l3v1" );
    	//info( "data/Examples/FluxBalance/Data/Tables/01187-sbml-l3v1-fbc-table" );    	

    	info( "data/Collaboration/Demo/Data/BPMN" );

    	DataCollection dc = CollectionFactory.getDataCollection( "data/Collaboration/Demo/Data/BPMN" );

    	//System.out.println("\n" + ( ( ru.biosoft.access.TextDataElement )dc.get( "diagram_big.bpmn" ) ).getContent() );
        assertTrue( ((ru.biosoft.access.core.TextDataElement) dc.get( "test_spec.bpmn" )).getContent().
                startsWith( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ) );
    }    
}

