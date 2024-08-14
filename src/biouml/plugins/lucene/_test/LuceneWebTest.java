package biouml.plugins.lucene._test;

import java.util.Arrays;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.plugins.lucene.LuceneQuerySystem;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.server.servlets.webservices._test.AbstractProviderTest;
import ru.biosoft.util.JsonUtils;

public class LuceneWebTest extends AbstractProviderTest
{
    private static final String repositoryPath = "../data/test/biouml/plugins/lucene/data";

    public void testLuceneWeb() throws Exception
    {
        DataCollection<?> root = CollectionFactory.createRepository( repositoryPath );
        DataCollection<?> module = (DataCollection<?>)root.get( "module2" );
        ( (LuceneQuerySystem)module.getInfo().getQuerySystem() ).createIndex( null, null );

        JsonObject json = getResponseJSON( "lucene", EntryStream.of( "action", "suggest", "de", "test/module2", "query", "cycli" ).toMap() );
        assertOk( json );
        assertEquals( Arrays.asList( "cyclic", "cyclin", "cyclin-p:cdc2", "cyclin:cdc2-p" ), JsonUtils
                .arrayOfStrings( json.get( "values" ) ).toList() );

        json = getResponseJSON( "lucene", EntryStream.of( "action", "search", "de", "test/module2", "query", "cyclin*" ).toMap() );
        assertOk( json );
        JsonObject vals = json.get( "values" ).asObject();
        assertEquals( 9, vals.get( "results" ).asInt() );
        assertEquals( Arrays.asList( "test/module2/Data/protein" ), JsonUtils.arrayOfStrings( vals.get( "collections" ) ).toList() );
        assertEquals( Arrays.asList( "relativeName", "name", "title" ), JsonUtils.arrayOfStrings( vals.get( "fields" ) ).toList() );

        json = getResponseJSON( "bean", EntryStream.of( "action", "get", "de", "properties/luceneSearch/saveTable" ).toMap() );
        assertOk( json );
        JsonArray props = json.get( "values" ).asArray();
        JsonObject fields = StreamEx.of( props.values() ).findFirst( prop -> prop.asObject().getString( "name", "" ).equals( "fields" ) )
                .get().asObject();
        assertTrue( fields.get( "dictionary" ).asArray().values().stream()
                .anyMatch( val -> val.asArray().get( 0 ).asString().equals( "Attributes" ) ) );
    }
}
