package ru.biosoft.access.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Support for replacing one {@code key = ?} query per id with batched {@code key IN (?,...)} queries that return the
 * same rows for every id.
 * <p>
 * A batched query selects {@code FIELD(key, ?,...)} next to its other columns to tell which id each row matched.
 * {@code =}, {@code IN} and {@code FIELD} all compare with the collation MySQL derives from the key column and the
 * (coercible) parameters, so a row is attributed to the id that {@code key = ?} would have returned it for.
 * <p>
 * Two things can make that attribution ambiguous, and both are kept out of a batch, so callers look such ids up one
 * by one with their original per-id query:
 * <ul>
 * <li>ids that a collation could treat as equal to a different string: anything outside printable ASCII without
 * space (case folding of non-ASCII letters, accents, ignorable control characters, trailing-space padding);</li>
 * <li>two ids equal ignoring ASCII case: under a case-insensitive collation both match the same rows, but FIELD
 * reports only the first.</li>
 * </ul>
 * Callers must also treat a row whose FIELD value is not in {@code 1..chunk size} as "cannot attribute" and fall
 * back to per-id queries for that chunk (see {@link #fieldIndex}).
 */
public final class SqlBatchLookup
{
    /** Number of ids per batched query (twice as many parameters with FIELD: well below MySQL's limits). */
    public static final int BATCH_SIZE = 500;

    private SqlBatchLookup()
    {
    }

    /** Whether the id consists of printable ASCII characters other than space only. */
    public static boolean hasPlainCharacters(String id)
    {
        if( id == null || id.isEmpty() )
            return false;
        for( int i = 0; i < id.length(); i++ )
        {
            char c = id.charAt( i );
            if( c <= ' ' || c > '~' )
                return false;
        }
        return true;
    }

    /**
     * The ids that can be looked up in batches: distinct, in the order of first occurrence, with plain characters
     * only ({@link #hasPlainCharacters}) and with no other distinct id in {@code ids} equal to them ignoring case.
     */
    public static List<String> batchableIds(Collection<String> ids)
    {
        Map<String, Integer> spellings = new HashMap<>();
        List<String> plain = new ArrayList<>();
        for( String id : new LinkedHashSet<>( ids ) )
        {
            if( !hasPlainCharacters( id ) )
                continue;
            plain.add( id );
            spellings.merge( id.toLowerCase( Locale.ROOT ), 1, Integer::sum );
        }
        List<String> result = new ArrayList<>();
        for( String id : plain )
            if( spellings.get( id.toLowerCase( Locale.ROOT ) ) == 1 )
                result.add( id );
        return result;
    }

    /** The list cut into consecutive chunks of at most {@link #BATCH_SIZE} elements (views of it). */
    public static <T> List<List<T>> chunks(List<T> list)
    {
        if( list.isEmpty() )
            return Collections.emptyList();
        List<List<T>> result = new ArrayList<>();
        for( int from = 0; from < list.size(); from += BATCH_SIZE )
            result.add( list.subList( from, Math.min( from + BATCH_SIZE, list.size() ) ) );
        return result;
    }

    /** "?,?,...,?" with {@code count} placeholders. */
    public static String placeholders(int count)
    {
        if( count <= 0 )
            throw new IllegalArgumentException( "At least one placeholder is needed, got " + count );
        return String.join( ",", Collections.nCopies( count, "?" ) );
    }

    /**
     * The 0-based position in the chunk of the id a row's FIELD value names, or -1 if it names none
     * (0 or out of range): the caller must then not use the batch for that chunk.
     */
    public static int fieldIndex(int fieldValue, int chunkSize)
    {
        return fieldValue >= 1 && fieldValue <= chunkSize ? fieldValue - 1 : -1;
    }
}
