package biouml.standard.diagram._test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.diagram.DiagramUtility;
import biouml.workbench.diagram.viewpart.ModelViewPart.SubDiagramTab;
import junit.framework.TestSuite;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * @author Ilya
 * Test for various aspects of composite models flattening
 * TODO: keep exact results in separate files
 */
public class TestCompositePreprocessor extends AbstractBioUMLTest
{
    private static String repositoryPath = "../data";
    private static String databasePath = "databases/Composite Models/";
        
    public TestCompositePreprocessor(String name)
    {
       super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite ( TestCompositePreprocessor.class.getName() );
        suite.addTest(new TestCompositePreprocessor("testSimpleMultilevel"));
        suite.addTest(new TestCompositePreprocessor("testPrivatePort"));
        suite.addTest(new TestCompositePreprocessor("testSimpleBus"));
        suite.addTest(new TestCompositePreprocessor("testEntityTransmitter"));
        suite.addTest(new TestCompositePreprocessor("testSimpleTransmitter"));
        suite.addTest(new TestCompositePreprocessor("testTripleBus"));
        return suite;
    }
   
    private Diagram getDiagram(String folder, String diagramName) throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataCollection collection = CollectionFactory.getDataCollection(databasePath+folder);
        DataElement de = collection.get(diagramName);
        return (Diagram)de;
    }
    
    public void testSimpleMultilevel() throws Exception
    {
        Diagram diagram = getDiagram("C0001","A");
        
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        preprocessor.preprocess(diagram);
        Map<String, String> mapping = preprocessor.getVarPathMapping("");
        
        Map<String, String> correctResult = getSimpleMultiLevelResult();
        String errors = equals(mapping, correctResult);
        assertTrue(errors, errors.isEmpty());     
    }
    
    public void testPrivatePort() throws Exception
    {
        Diagram diagram = getDiagram("C0002","Top");
        
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        preprocessor.preprocess(diagram);
        Map<String, String> mapping = preprocessor.getVarPathMapping("");
        Map<String, String> correctResult = getPrivatePortResult();
        String errors = equals(mapping, correctResult);
        assertTrue(errors, errors.isEmpty());     
    }
    
    public void testSimpleBus() throws Exception
    {
        Diagram diagram = getDiagram("C0003","Top");
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        preprocessor.preprocess(diagram);
        Map<String, String> mapping = preprocessor.getVarPathMapping("");  
        Map<String, String> correctResult = getSimpleBusResult();
        String errors = equals(mapping, correctResult);
        assertTrue(errors, errors.isEmpty());     
    }
    
    public void testEntityTransmitter() throws Exception
    {
        Diagram diagram = getDiagram("C0004","Top");
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        preprocessor.preprocess(diagram);
        Map<String, String> mapping = preprocessor.getVarPathMapping(""); 
        Map<String, String> correctResult = getEntityTransmitterResult();
        String errors = equals(mapping, correctResult);
        assertTrue(errors, errors.isEmpty());     
    }
    
    public void testSimpleTransmitter() throws Exception
    {
        Diagram diagram = getDiagram("C0005","Top");
        
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        preprocessor.preprocess(diagram);
        Map<String, String> mapping = preprocessor.getVarPathMapping("");
        Map<String, String> correctResult = getSimpleTransmitterResult();
        String errors = equals(mapping, correctResult);
        assertTrue(errors, errors.isEmpty());     
    }

    public void testTripleBus() throws Exception
    {
        Diagram diagram = getDiagram("C0006","Top");
        System.out.println( "Composite diagram testing." );
        System.out.println( "Elements of top level." );
        System.out.println( diagram.recursiveStream().map( n -> n.getName() ).joining( "," ) );
        DiagramUtility.getSubDiagrams( diagram ).map( s->s.getDiagram() ).forEach( 
                d -> System.out.println( d.getName() + " INNER: " + d.recursiveStream().map( n -> n.getName() ).joining( "," ) )
                );
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        preprocessor.preprocess( diagram );
        Map<String, String> mapping = preprocessor.getVarPathMapping( "" );
        Map<String, String> correctResult = getTripleBusResult();
        String errors = equals( mapping, correctResult );
        assertTrue( errors, errors.isEmpty() );
    }
    
    private Map<String, String> getSimpleMultiLevelResult()
    {
        Map<String, String> result = new HashMap<>();
        result.put("time", "time");
        result.put("B/time", "time");
        result.put("B/time", "time");
        result.put("B/C/time", "time");
        result.put("B/C/D/time", "time");
        result.put("B/C/D/E/time", "time");
        result.put("B/C/D/E/$S", "$B__C__D__E__S");
        return result;
    }
    
    private Map<String, String> getPrivatePortResult()
    {
        Map<String, String> result = new HashMap<>();
        result.put("time", "time");
        result.put("Sub/time", "time");
        result.put("Sub/A", "Sub__A");
        result.put("Sub/Sub1/time", "time");
        result.put("Sub/Sub1/a", "Sub__A");
        result.put("Sub/Sub1/x", "Sub__Sub1__x");
        return result;
    }
    
    private Map<String, String> getSimpleBusResult()
    {
        Map<String, String> result = new HashMap<>();
        result.put("time", "time");
        result.put("Sub/time", "time");
        result.put("Sub_source/time", "time");
        result.put("Sub/$entity", "$entity_source");
        result.put("Sub_source/$entity_source", "$entity_source");
        return result;
    }
    
 
    private Map<String, String> getEntityTransmitterResult()
    {
        Map<String, String> result = new HashMap<>();
        result.put("time","time");        
        result.put("Sub1/time","time");
        result.put("Sub2/time","time");
        result.put("Sub_Source/time","time");        
        result.put("Sub1/x","Sub1__x");                        
        result.put("Sub1/$entity1","$Sub1__entity1");
        result.put("Sub2/$transmitted","$Sub2__transmitted");
        result.put("Sub2/$entity2","$Sub2__entity2");        
        result.put("Sub_Source/$entity_source","$Sub_Source__entity_source");
        return result;
    }
    
    private Map<String, String> getSimpleTransmitterResult()
    {
        Map<String, String> result = new HashMap<>();
        result.put("time", "time");
        result.put("Source/time", "time");
        result.put("Transmitter/time", "time");
        result.put("Receiver/time", "time");
        result.put("Source/signal", "Source__signal");
        result.put("Transmitter/input", "Source__signal");
        result.put("Transmitter/output", "Transmitter__output");
        result.put("Receiver/signal", "Transmitter__output");
        result.put("Receiver/z", "Receiver__z");
        return result;
    }

    private Map<String, String> getTripleBusResult()
    {
        Map<String, String> result = new HashMap<>();
        result.put("Sub1/$entity1", "$entity3");
        result.put("Sub2/$entity2", "$entity3");
        result.put("Sub3/$entity3", "$entity3");
        result.put("time", "time");
        result.put("Sub1/time", "time");
        result.put("Sub2/time", "time");
        result.put("Sub3/time", "time");
        result.put("Sub1/k", "k");
        result.put("Sub2/k", "k2");
        result.put("Sub3/k", "k3");
        return result;
    }
    
    /**
     * Method compares map m with check and prints detailed messages if there are and differences between them 
     */
    private String equals(Map<String, String> m, Map<String, String> check)
    {
        StringBuffer result = new StringBuffer();
        Set<String> missed = new HashSet<>();
        Set<String> spare = new HashSet<>();
        
        Map<String, String> wrongValue = new HashMap<>();
        
        for( Entry<String, String> e : check.entrySet() )
        {
            if( !m.containsKey(e.getKey()) )
            {
                missed.add(e.getKey());
                continue;
            }
            String value = m.get(e.getKey());
            if( ! ( e.getValue().equals(value) ) )
                wrongValue.put(e.getKey(), value);
        }
        
        for( Entry<String, String> e : m.entrySet() )
        {
            if (!check.containsKey(e.getKey()))
                spare.add(e.getKey());
        }
        
        if (!missed.isEmpty())
        {
           result.append( "\nKeys missed from result: "+StreamEx.of(missed).joining(", ") );
        }
        
        if (!spare.isEmpty())
        {
            result.append("\nKeys should not be in result: "+StreamEx.of(spare).joining(", "));
        }
        
        if (!wrongValue.isEmpty())
        {
            EntryStream.of(wrongValue).forEach(e->result.append( "\n"+e.getKey()+" -> "+e.getValue()+", but should be "+ check.get(e.getKey())));
        }
        
        return result.toString();
    }
    
}
