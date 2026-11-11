package biouml.plugins.ensembl._test;

import java.lang.reflect.Field;

import junit.framework.TestCase;
import biouml.plugins.ensembl.biohub.InternalEnsemblHub;

public class InternalEnsemblHubBatchQueryTest extends TestCase
{
    public void testBatchQuery()
    {
        assertEquals( "select distinct FIELD(gs.stable_id,?,?),ts.stable_id from gene_stable_id gs,transcript_stable_id ts,transcript t "
                + "where gs.gene_id=t.gene_id and ts.transcript_id=t.transcript_id and gs.stable_id in (?,?)",
                InternalEnsemblHub.batchQuery( "select distinct ts.stable_id from gene_stable_id gs,transcript_stable_id ts,transcript t "
                        + "where gs.gene_id=t.gene_id and ts.transcript_id=t.transcript_id and gs.stable_id=?", 2 ) );
    }

    /** Every query the hub can run must turn into a batched one keyed by the same column. */
    public void testAllHubQueriesConvert() throws Exception
    {
        Field f = InternalEnsemblHub.class.getDeclaredField( "QUERIES" );
        f.setAccessible( true );
        for( String query : (String[])f.get( null ) )
        {
            String key = query.substring( query.lastIndexOf( ' ' ) + 1, query.length() - "=?".length() );
            String batched = InternalEnsemblHub.batchQuery( query, 3 );
            assertTrue( batched, batched.startsWith( "select distinct FIELD(" + key + ",?,?,?)," ) );
            assertTrue( batched, batched.endsWith( " " + key + " in (?,?,?)" ) );
            assertEquals( batched, 6, batched.chars().filter( c -> c == '?' ).count() );
        }
    }

    public void testRejectsOtherQueries()
    {
        for( String query : new String[] {"select stable_id from gene_stable_id where stable_id=?",
                "select distinct stable_id from gene_stable_id where stable_id like ?"} )
        {
            try
            {
                InternalEnsemblHub.batchQuery( query, 1 );
                fail( query );
            }
            catch( IllegalArgumentException e )
            {
                // expected
            }
        }
    }
}
