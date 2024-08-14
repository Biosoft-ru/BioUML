package biouml.plugins.research.workflow.yaml._test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import biouml.model.Diagram;
import biouml.model.util.DiagramXmlReader;
import biouml.plugins.research.workflow.yaml.WorkflowToYamlConverter;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;

public class TestWorkflowToYamlConverter extends AbstractBioUMLTest
{
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( "../data" );
    }
    
    public void testSpec1() throws Exception
    {
        Diagram workflow = readDiagram();
        Map<String, Object> expectedYaml = readYaml();

        WorkflowToYamlConverter converter = new WorkflowToYamlConverter();
        Map<String, Object> yaml = converter.convert( workflow );

        assertMapEquals( expectedYaml, yaml );
    }

    private void assertYamlEquals(Object expected, Object actual)
    {
        if(expected instanceof Map)
        {
            assertTrue(actual instanceof Map);
            assertMapEquals((Map<String, Object>)expected, (Map<String, Object>)actual);
        }else if(expected instanceof List)
        {
            assertTrue(actual instanceof List);
            assertListEquals((List<Object>)expected, (List<Object>)actual );
        }else
            assertEquals( expected, actual );
        
    }
    
    private void assertListEquals(List<Object> expected, List<Object> actual)
    {
        assertNotNull( expected );
        assertNotNull( actual );

        assertEquals( expected.size(), actual.size() );
        for(int i = 0; i < expected.size(); i++)
            assertYamlEquals( expected.get( i ), actual.get(i) );
    }

    private void assertMapEquals(Map<String, Object> expected, Map<String, Object> actual)
    {
        assertNotNull( expected );
        assertNotNull( actual );
        for( Map.Entry<String, Object> e : expected.entrySet() )
        {
            assertTrue( "Absent '" + e.getKey() + "' key", actual.containsKey( e.getKey() ) );
            Object actualValue = actual.get( e.getKey() );
            assertYamlEquals( e.getValue(), actualValue );
        }
        for( String key : actual.keySet() )
            assertTrue( "Extra key '" + key + "'", expected.containsKey( key ) );
    }

    private Diagram readDiagram() throws Exception
    {
        try (InputStream is = new FileInputStream( "../data/test/biouml/plugins/research/workflowSpec1.dml" ))
        {
            return DiagramXmlReader.readDiagram("Mapping to ontologies (Gene table)", is, null, null, null);
        }
    }

    private Map<String, Object> readYaml() throws FileNotFoundException, IOException
    {
        Yaml yaml = new Yaml();
        try (InputStream is = new FileInputStream( "../data/test/biouml/plugins/research/workflowSpec1.yaml" ))
        {
            Map<String, Object> result = (Map<String, Object>)yaml.load( is );
            trimStrings( result );
            return result;
        }
    }
    
    private void trimStrings(Map<String, Object> map)
    {
        for(Map.Entry<String, Object> e : map.entrySet())
            if(e.getValue() instanceof String)
                e.setValue( ((String)e.getValue()).trim() );
            else if(e.getValue() instanceof Map)
                trimStrings((Map<String, Object>)e.getValue());
    }

    public void testYamlNewLines()
    {
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>)yaml.load( "text: >\n" + "  line1\n" + "  line2\n" );
        String actual = (String)map.get( "text" );
        assertEquals( "line1 line2", actual );
        System.out.println( actual );
    }
    
    public void testGenerateYaml() throws Exception
    {
        Diagram workflow = readDiagram();
        WorkflowToYamlConverter converter = new WorkflowToYamlConverter();
        Map<String, Object> yaml = converter.convert( workflow );
        String text = new Yaml().dump( yaml );
        System.out.print( text );
    }
}
