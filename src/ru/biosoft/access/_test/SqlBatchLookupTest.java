package ru.biosoft.access._test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import ru.biosoft.access.sql.SqlBatchLookup;

public class SqlBatchLookupTest extends TestCase
{
    public void testBatchableIdsKeepsOrderAndRemovesDuplicates()
    {
        assertEquals( Arrays.asList( "ENSG2", "ENSG1", "ENSG3" ),
                SqlBatchLookup.batchableIds( Arrays.asList( "ENSG2", "ENSG1", "ENSG2", "ENSG3", "ENSG1" ) ) );
    }

    public void testIdsEqualIgnoringCaseAreNotBatched()
    {
        // a case-insensitive collation matches both to the same rows, FIELD would report only the first
        assertEquals( Arrays.asList( "ENSG2" ), SqlBatchLookup.batchableIds( Arrays.asList( "ENSG1", "ensg1", "ENSG2", "EnSg1" ) ) );
    }

    public void testIdsACollationMayFoldAreNotBatched()
    {
        // trailing/inner space (PAD SPACE), accents, ignorable control characters (utf8mb4_0900_ai_ci), DEL, empty
        List<String> ids = Arrays.asList( "ENSG1 ", " ENSG2", "EN SG3", "ENSG4é", "ENSG5\u0001", "ENSG6\t", "", "ENSG7\u007f", "ENSG8" );
        assertEquals( Arrays.asList( "ENSG8" ), SqlBatchLookup.batchableIds( ids ) );
        assertFalse( SqlBatchLookup.hasPlainCharacters( null ) );
        assertTrue( SqlBatchLookup.hasPlainCharacters( "ENSG00000139618.15" ) );
        assertTrue( SqlBatchLookup.hasPlainCharacters( "!~_-." ) );
    }

    public void testChunks()
    {
        assertTrue( SqlBatchLookup.chunks( Collections.emptyList() ).isEmpty() );
        List<Integer> values = new ArrayList<>();
        for( int i = 0; i < 2 * SqlBatchLookup.BATCH_SIZE + 3; i++ )
            values.add( i );
        List<List<Integer>> chunks = SqlBatchLookup.chunks( values );
        assertEquals( 3, chunks.size() );
        assertEquals( SqlBatchLookup.BATCH_SIZE, chunks.get( 0 ).size() );
        assertEquals( SqlBatchLookup.BATCH_SIZE, chunks.get( 1 ).size() );
        assertEquals( 3, chunks.get( 2 ).size() );
        assertEquals( Integer.valueOf( SqlBatchLookup.BATCH_SIZE ), chunks.get( 1 ).get( 0 ) );
        assertEquals( Integer.valueOf( 2 * SqlBatchLookup.BATCH_SIZE + 2 ), chunks.get( 2 ).get( 2 ) );
    }

    public void testPlaceholders()
    {
        assertEquals( "?", SqlBatchLookup.placeholders( 1 ) );
        assertEquals( "?,?,?", SqlBatchLookup.placeholders( 3 ) );
        try
        {
            SqlBatchLookup.placeholders( 0 );
            fail( "an empty IN list is invalid SQL" );
        }
        catch( IllegalArgumentException e )
        {
            // expected
        }
    }

    public void testFieldIndex()
    {
        assertEquals( 0, SqlBatchLookup.fieldIndex( 1, 3 ) );
        assertEquals( 2, SqlBatchLookup.fieldIndex( 3, 3 ) );
        assertEquals( -1, SqlBatchLookup.fieldIndex( 0, 3 ) ); // FIELD found no equal argument
        assertEquals( -1, SqlBatchLookup.fieldIndex( 4, 3 ) );
    }
}
