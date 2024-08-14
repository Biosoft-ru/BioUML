package ru.biosoft.server.servlets.webservices._test;

import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class TestWebDiagramsProvider extends AbstractProviderTest
{
    private static final String NOTE_1 = "New/note";
    private static final String NOTE_2 = "New note2";
    private static final DataElementPath DIAGRAM_PATH = DataElementPath.create("test/diagram");
    
    public void testDiagramBasics() throws Exception
    {
        FolderVectorCollection fvc = new FolderVectorCollection( "test", null );
        CollectionFactory.registerRoot( fvc );
        Diagram dgr = new Diagram( fvc, new DiagramInfo( fvc, DIAGRAM_PATH.getName()), new PathwayDiagramType() );
        fvc.put( dgr );
        
        checkGetEmptyDiagram();
        checkAddNote();
        checkUndo();
        checkRedo();
        checkAddNote2();
        checkAddEdge();
    }

    protected void checkGetEmptyDiagram() throws Exception
    {
        Map<String, String> params = new HashMap<>();
        params.put( "de", DIAGRAM_PATH.toString() );
        params.put( "type", "json" );
        JsonObject responseJSON = getResponseJSON( "diagram", params );
        assertOk(responseJSON);
        JsonObject vals = responseJSON.get( "values" ).asObject();
        assertNotNull(vals.get( "users" ).asArray());
        assertNotNull(vals.get( "transactions" ).asArray());
        JsonObject view = vals.get( "view" ).asObject();
        assertEquals(DIAGRAM_PATH.toString(), view.get("model").asString());
    }

    protected void checkAddNote() throws Exception
    {
        Map<String,String> params = new HashMap<>();
        params.put( "de", DIAGRAM_PATH.toString() );
        params.put( "resptype", "json" );
        params.put( "action", "add" );
        params.put( "x", "100" );
        params.put( "y", "150" );
        params.put( "dc", "" );
        params.put( "type", Stub.Note.class.getName() );
        params.put( "json", getNameValueBean("name", NOTE_1).toString() );
        JsonObject responseJSON = getResponseJSON( "diagram", params );
        assertOk(responseJSON);
        JsonObject vals = responseJSON.get( "values" ).asObject();
        JsonArray transactions = vals.get( "transactions" ).asArray();
        assertFalse(transactions.isEmpty());
        JsonObject transaction = transactions.get( transactions.size()-1 ).asObject();
        assertEquals("Add "+NOTE_1, transaction.get( "name" ).asString());
        JsonObject view = vals.get( "view" ).asObject().get( "children" ).asArray().get( 0 ).asObject();
        DataElementPath path = DIAGRAM_PATH.getChildPath( NOTE_1 );
        assertEquals(path.toString(), view.get( "model" ).asString());
        path.getDataElement( Node.class );
    }

    protected void checkAddNote2() throws Exception
    {
        Map<String,String> params = new HashMap<>();
        params.put( "de", DIAGRAM_PATH.toString() );
        params.put( "resptype", "json" );
        params.put( "action", "add" );
        params.put( "x", "300" );
        params.put( "y", "350" );
        params.put( "dc", "" );
        params.put( "type", Stub.Note.class.getName() );
        params.put( "json", getNameValueBean("name", NOTE_2).toString() );
        JsonObject responseJSON = getResponseJSON( "diagram", params );
        assertOk(responseJSON);
        JsonObject vals = responseJSON.get( "values" ).asObject();
        JsonArray transactions = vals.get( "transactions" ).asArray();
        assertTrue(transactions.size() >= 2);
        DataElementPath path = DIAGRAM_PATH.getChildPath( NOTE_2 );
        path.getDataElement( Node.class );
    }

    protected void checkAddEdge() throws Exception
    {
        Map<String,String> params = new HashMap<>();
        params.put( "de", "test/diagram" );
        params.put( "action", "add" );
        params.put( "resptype", "json" );
        params.put( "type", Stub.NoteLink.class.getName() );
        params.put( "dc", "" );
        params.put( "input", NOTE_1 );
        params.put( "output", NOTE_2 );
        JsonObject responseJSON = getResponseJSON( "diagram", params );
        assertOk(responseJSON);
        Edge[] edges = DIAGRAM_PATH.getChildPath( NOTE_1 ).getDataElement( Node.class ).getEdges();
        assertEquals(1, edges.length);
        Edge edge = edges[0];
        assertEquals(NOTE_1, edge.getInput().getName());
        assertEquals(NOTE_2, edge.getOutput().getName());
    }

    protected void checkUndo() throws Exception
    {
        Map<String,String> params = new HashMap<>();
        params.put( "de", "test/diagram" );
        params.put( "action", "undo" );
        params.put( "type", "json" );
        JsonObject responseJSON = getResponseJSON( "diagram", params );
        assertOk(responseJSON);
        JsonObject vals = responseJSON.get( "values" ).asObject();
        JsonArray transactions = vals.get( "transactions" ).asArray();
        assertFalse(transactions.isEmpty());
        JsonObject transaction = transactions.get( transactions.size()-1 ).asObject();
        assertEquals("Add "+NOTE_1, transaction.get( "name" ).asString());
        assertTrue(transaction.getBoolean( "next", false ));
        assertTrue(vals.get( "view" ).asObject().get( "children" ).asArray().isEmpty());
        assertFalse(DIAGRAM_PATH.getChildPath( NOTE_1 ).exists());
    }

    protected void checkRedo() throws Exception
    {
        Map<String,String> params = new HashMap<>();
        params.put( "de", "test/diagram" );
        params.put( "action", "redo" );
        params.put( "type", "json" );
        JsonObject responseJSON = getResponseJSON( "diagram", params );
        assertOk(responseJSON);
        JsonObject vals = responseJSON.get( "values" ).asObject();
        JsonArray transactions = vals.get( "transactions" ).asArray();
        assertFalse(transactions.isEmpty());
        JsonObject transaction = transactions.get( transactions.size()-1 ).asObject();
        assertEquals("Add "+NOTE_1, transaction.get( "name" ).asString());
        assertFalse(transaction.getBoolean( "next", false ));
        assertEquals(1, vals.get( "view" ).asObject().get( "children" ).asArray().size());
        assertTrue(DIAGRAM_PATH.getChildPath( NOTE_1 ).exists());
    }
    
    protected JsonArray getNameValueBean(String name, String value)
    {
        return new JsonArray().add( new JsonObject().add( "name", name ).add( "value", value ) );
    }
}
