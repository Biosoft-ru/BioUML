package biouml.plugins.expression._test;

import java.util.Collections;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.plugins.expression.ExpressionFilter;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.server.servlets.webservices._test.AbstractProviderTest;

public class TestExpressionFilterProvider extends AbstractProviderTest
{
    private VectorDataCollection<Diagram> vdc;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
    }

    public void testFilterList() throws Exception
    {
        Diagram dgr = new Diagram( vdc, "dgr" );
        ExpressionFilter activeFilter = new ExpressionFilter( "testFilter" );
        dgr.setFilterList( new DiagramFilter[] {activeFilter, new ExpressionFilter( "testFilter2" )} );
        dgr.setDiagramFilter( activeFilter );
        vdc.put( dgr );
        JsonObject response = getResponseJSON( "diagramFilter/list", Collections.singletonMap( "de", dgr.getCompletePath().toString() ) );
        assertOk( response );
        JsonArray array = response.get( "values" ).asArray();
        assertEquals(2, array.size());
        JsonObject first = array.get( 0 ).asObject();
        assertEquals("testFilter", first.get( "name" ).asString());
        assertNotNull( first.get( "active" ) );
        JsonObject second = array.get( 1 ).asObject();
        assertEquals("testFilter2", second.get( "name" ).asString());
        assertNull( second.get( "active" ) );
    }
}
