package ensbench;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;

import biouml.plugins.ensembl.biohub.InternalEnsemblHub;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.plugins.ensembl.tabletype.EnsemblProteinTableType;
import biouml.plugins.ensembl.tabletype.EnsemblTranscriptTableType;
import biouml.plugins.riboseq.transcripts.EnsemblTranscriptsProvider;
import biouml.plugins.riboseq.transcripts.Transcript;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Old vs new EnsemblTranscriptsProvider / InternalEnsemblHub against a synthetic Ensembl-shaped MySQL database.
 * Run the same class on the old and the new classes and diff the "verify" output; "bench" prints statements and time.
 *   load   - create the database (charset/collation from -Dens.charset / -Dens.collation) with schema and data
 *   verify - dump every result of every scenario to stdout
 *   bench  - per scenario: SQL statements sent and median wall time of 5 runs (stderr)
 * Connection: -Dens.port (3307), -Dens.db (ens), user root, password bench.
 */
public class EnsBench
{
    static final String DB = System.getProperty( "ens.db", "ens" );
    static final String URL = "jdbc:mysql://127.0.0.1:" + System.getProperty( "ens.port", "3307" ) + "/"
            + "?user=root&password=bench&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    static final int GENES = 20000;

    static Connection con;

    /** InternalEnsemblHub talking to our connection instead of a repository collection. */
    static class TestHub extends InternalEnsemblHub
    {
        TestHub()
        {
            super( new Properties() );
        }
        @Override
        protected DataElementPath getEnsemblPath(Properties properties)
        {
            return DataElementPath.create( "test/ensembl" );
        }
        @Override
        protected synchronized Connection getConnection(DataElementPath path)
        {
            return con;
        }
    }

    /** Job control that reports "terminated by request" from the n-th status query on. */
    static class CancelAfter extends FunctionJobControl
    {
        int calls;
        final int after;
        CancelAfter(int after)
        {
            super( Logger.getLogger( "ensbench" ) );
            this.after = after;
        }
        @Override
        public int getStatus()
        {
            return ++calls >= after ? JobControl.TERMINATED_BY_REQUEST : super.getStatus();
        }
    }

    static String gene(int i)
    {
        return String.format( "ENSG%011d", i );
    }

    public static void main(String[] args) throws Exception
    {
        boolean load = args[0].equals( "load" );
        con = DriverManager.getConnection( URL + ( load ? "&rewriteBatchedStatements=true" : "" ) );
        if( load )
        {
            try( Statement st = con.createStatement() )
            {
                st.execute( "DROP DATABASE IF EXISTS " + DB );
                st.execute( "CREATE DATABASE " + DB + " CHARACTER SET " + System.getProperty( "ens.charset", "latin1" ) + " COLLATE "
                        + System.getProperty( "ens.collation", "latin1_swedish_ci" ) );
            }
        }
        con.setCatalog( DB );
        if( !load )
            registerTypes();
        switch( args[0] )
        {
            case "load" -> load();
            case "verify" -> run( System.out, 1 );
            case "bench" -> run( null, 5 );
            default -> throw new IllegalArgumentException( args[0] );
        }
    }

    /** Registers the Ensembl reference types without OSGi: the hub's constructor looks them up in the registry. */
    static void registerTypes() throws Exception
    {
        java.lang.reflect.Field inst = Class.forName( "ru.biosoft.access.biohub.ReferenceTypeRegistry" ).getDeclaredField( "instance" );
        inst.setAccessible( true );
        Object registry = inst.get( null );
        Class<?> support = Class.forName( "ru.biosoft.util.ExtensionRegistrySupport" );
        List<Object> types = new ArrayList<>( List.of( new EnsemblGeneTableType(), new EnsemblTranscriptTableType(), new EnsemblProteinTableType() ) );
        Map<String, Object> byName = new HashMap<>();
        for( Object t : types )
            byName.put( t.getClass().getName(), t );
        for( String name : new String[] {"extensions", "nameToExtension", "initialized"} )
        {
            java.lang.reflect.Field field = support.getDeclaredField( name );
            field.setAccessible( true );
            field.set( registry, name.equals( "extensions" ) ? types : name.equals( "nameToExtension" ) ? byName : Boolean.TRUE );
        }
    }

    // ---------------------------------------------------------------- scenarios

    /** Ids of the given kind with the edge cases the batching must keep: versions, other case, unknown, duplicates... */
    static String[] inputs(String prefix, int max, int n, Random rnd)
    {
        List<String> ids = new ArrayList<>();
        for( int i = 0; i < n; i++ )
        {
            String id = String.format( "%s%011d", prefix, 1 + rnd.nextInt( max ) );
            int kind = rnd.nextInt( 100 );
            if( kind < 5 )
                ids.add( id + "." + ( 1 + rnd.nextInt( 9 ) ) ); // versioned: needs the retry without version
            else if( kind < 8 )
                ids.add( String.format( "%s%011d", prefix, max + 1000 + rnd.nextInt( 100000 ) ) ); // unknown
            else if( kind < 10 )
                ids.add( id.toLowerCase() ); // matches only through a case-insensitive collation
            else
                ids.add( id );
        }
        String first = String.format( "%s%011d", prefix, 7 );
        ids.add( ids.get( 0 ) ); // duplicate
        ids.addAll( List.of( first, first.toLowerCase(), // two spellings of one id
                first + "é", first + " ", first + "\t", first + "\u0001", first.replace( '0', 'Ｏ' ), "" ) );
        return ids.toArray( new String[0] );
    }

    static void run(PrintStream out, int reps) throws Exception
    {
        ReferenceType genes = new EnsemblGeneTableType(), transcripts = new EnsemblTranscriptTableType(),
                proteins = new EnsemblProteinTableType();
        int nTranscripts = count( "transcript" ), nTranslations = count( "translation" );

        for( int n : new int[] {100, 2000, 10000} )
        {
            String[] input = inputs( "ENSG", GENES, n, new Random( n ) );
            Map<String, String[]> g2t = hub( out, "genes->transcripts n=" + n, reps, input, genes, transcripts );
            List<String> subset = new ArrayList<>();
            for( String[] v : g2t.values() )
                subset.addAll( Arrays.asList( v ) );
            subset.addAll( List.of( "ENST99999999999", "enst00000000001", "ENST00000000001", "ENST00000003000", "" ) );
            subset.add( subset.get( 0 ) ); // duplicate
            for( boolean cds : new boolean[] {false, true} )
                dump( out, "provider subset n=" + subset.size() + " loadCDS=" + cds, reps, subset, cds, false );
        }
        int n = 5000;
        hub( out, "transcripts->genes n=" + n, reps, inputs( "ENST", nTranscripts, n, new Random( 1 ) ), transcripts, genes );
        hub( out, "proteins->genes n=" + n, reps, inputs( "ENSP", nTranslations, n, new Random( 2 ) ), proteins, genes );
        hub( out, "proteins->transcripts n=" + n, reps, inputs( "ENSP", nTranslations, n, new Random( 3 ) ), proteins, transcripts );
        hub( out, "transcripts->proteins n=" + n, reps, inputs( "ENST", nTranscripts, n, new Random( 4 ) ), transcripts, proteins );
        hub( out, "genes->proteins n=" + n, reps, inputs( "ENSG", GENES, n, new Random( 5 ) ), genes, proteins );
        hub( out, "empty input", reps, new String[0], genes, transcripts );
        // one id and a variant a collation may treat as equal to it, in the same batch: ignorable control character,
        // trailing space (PAD SPACE), other case, accent
        String g7 = gene( 7 );
        for( String variant : new String[] {g7 + "\u0001", g7 + " ", g7.toLowerCase(), g7 + "é"} )
            hub( out, "same-batch pair " + escape( variant ), reps, new String[] {g7, variant, gene( 8 )}, genes, transcripts );
        dump( out, "provider empty subset", reps, Collections.emptyList(), true, false );
        for( boolean coding : new boolean[] {true, false} )
            dump( out, "provider all onlyProteinCoding=" + coding + " loadCDS=true", Math.min( reps, 3 ), null, true, coding );

        // cancellation: a job terminated from the start, and one terminated after a few status checks
        for( int after : new int[] {1, 3} )
        {
            String[] input = inputs( "ENSG", GENES, 5000, new Random( 9 ) );
            Object[] res = new Object[1];
            measure( "hub cancelled at status check " + after, 1,
                    () -> res[0] = new TestHub().getReferences( input, genes, transcripts, null, new CancelAfter( after ) ) );
            if( out != null )
                out.println( "# cancelled at " + after + ": " + ( res[0] == null ? "null" : "result" ) );
        }
    }

    static Map<String, String[]> hub(PrintStream out, String title, int reps, String[] input, ReferenceType from, ReferenceType to)
            throws Exception
    {
        Object[] res = new Object[1];
        String error = attempt( () -> measure( "hub " + title, reps, () -> res[0] = new TestHub().getReferences( input, from, to, null, null ) ) );
        if( error != null )
        {
            if( out != null )
                out.println( "# hub " + title + " ERROR " + error );
            return new HashMap<>();
        }
        @SuppressWarnings ( "unchecked" )
        Map<String, String[]> result = (Map<String, String[]>)res[0];
        if( out != null )
        {
            out.println( "# hub " + title + " size=" + result.size() );
            for( Map.Entry<String, String[]> e : new TreeMap<>( result ).entrySet() )
            {
                String[] sorted = e.getValue().clone(); // SQL returns them in no defined order, neither before nor now
                Arrays.sort( sorted );
                out.println( escape( e.getKey() ) + "\t" + Arrays.toString( sorted ) );
            }
        }
        return result;
    }

    static void dump(PrintStream out, String title, int reps, List<String> subset, boolean cds, boolean coding) throws Exception
    {
        Object[] res = new Object[1];
        String error = attempt( () -> measure( title, reps, () -> {
            EnsemblTranscriptsProvider p = new EnsemblTranscriptsProvider( () -> con );
            p.setLoadCDS( cds );
            p.setOnlyProteinCoding( coding );
            p.setSubset( subset );
            res[0] = p.getTranscripts();
        } ) );
        if( out == null )
            return;
        if( error != null )
        {
            out.println( "# " + title + " ERROR " + error );
            return;
        }
        @SuppressWarnings ( "unchecked" )
        List<Transcript> list = (List<Transcript>)res[0];
        out.println( "# " + title + " size=" + list.size() );
        for( Transcript t : list ) // in result order: the order is part of the behaviour
            out.println( t.getName() + " " + t.getChromosome() + " " + t.getLocation() + " " + t.isOnPositiveStrand() + " exons="
                    + t.getExonLocations() + " cds=" + t.getCDSLocations() );
    }

    static String escape(String s)
    {
        StringBuilder sb = new StringBuilder();
        for( char c : s.toCharArray() )
            sb.append( c < ' ' || c > '~' ? String.format( "\\u%04x", (int)c ) : String.valueOf( c ) );
        return "[" + sb + "]";
    }

    interface Body
    {
        void run() throws Exception;
    }

    /** Runs the body; on failure returns the root cause (class and message), which is compared like a result. */
    static String attempt(Body body)
    {
        try
        {
            body.run();
            return null;
        }
        catch( Throwable t )
        {
            Throwable root = t;
            while( root.getCause() != null && root.getCause() != root )
                root = root.getCause();
            System.err.println( "scenario failed: " + root );
            return root.getClass().getSimpleName() + ": " + root.getMessage();
        }
    }

    static int count(String table) throws Exception
    {
        try( Statement st = con.createStatement(); ResultSet rs = st.executeQuery( "SELECT COUNT(*) FROM " + table ) )
        {
            rs.next();
            return rs.getInt( 1 );
        }
    }

    static long questions() throws Exception
    {
        try( Statement st = con.createStatement(); ResultSet rs = st.executeQuery( "SHOW SESSION STATUS LIKE 'Questions'" ) )
        {
            rs.next();
            return rs.getLong( 2 );
        }
    }

    static void measure(String name, int reps, Body body) throws Exception
    {
        long[] times = new long[reps];
        long statements = 0;
        for( int r = 0; r < reps; r++ )
        {
            long q0 = questions();
            long t0 = System.nanoTime();
            body.run();
            times[r] = System.nanoTime() - t0;
            statements = questions() - q0 - 1; // minus the second SHOW STATUS itself
        }
        Arrays.sort( times );
        System.err.printf( java.util.Locale.ROOT, "%-52s statements=%8d  median=%9.1f ms%n", name, statements, times[reps / 2] / 1e6 );
    }

    // ---------------------------------------------------------------- data

    static void load() throws Exception
    {
        try( Statement st = con.createStatement() )
        {
            st.execute( "CREATE TABLE coord_system (coord_system_id INT PRIMARY KEY, name VARCHAR(40), `rank` INT)" );
            st.execute( "CREATE TABLE seq_region (seq_region_id INT PRIMARY KEY, name VARCHAR(40), coord_system_id INT, KEY(coord_system_id))" );
            st.execute( "CREATE TABLE gene_stable_id (gene_id INT PRIMARY KEY, stable_id VARCHAR(128), KEY(stable_id))" );
            st.execute( "CREATE TABLE transcript (transcript_id INT PRIMARY KEY, gene_id INT, seq_region_id INT, seq_region_start INT, "
                    + "seq_region_end INT, seq_region_strand TINYINT, biotype VARCHAR(40), KEY(gene_id), KEY(seq_region_id))" );
            st.execute( "CREATE TABLE transcript_stable_id (transcript_id INT PRIMARY KEY, stable_id VARCHAR(128), KEY(stable_id))" );
            st.execute( "CREATE TABLE exon (exon_id INT PRIMARY KEY, seq_region_id INT, seq_region_start INT, seq_region_end INT)" );
            st.execute( "CREATE TABLE exon_transcript (exon_id INT, transcript_id INT, `rank` INT, PRIMARY KEY(exon_id, transcript_id, `rank`), KEY(transcript_id))" );
            st.execute( "CREATE TABLE translation (translation_id INT PRIMARY KEY, transcript_id INT, seq_start INT, start_exon_id INT, "
                    + "seq_end INT, end_exon_id INT, KEY(transcript_id))" );
            st.execute( "CREATE TABLE translation_stable_id (translation_id INT PRIMARY KEY, stable_id VARCHAR(128), KEY(stable_id))" );
            st.execute( "INSERT INTO coord_system VALUES (1,'chromosome',1),(2,'scaffold',2)" );
            for( int c = 1; c <= 24; c++ )
                st.execute( "INSERT INTO seq_region VALUES (" + c + ",'" + ( c <= 22 ? "" + c : c == 23 ? "X" : "Y" ) + "',1)" );
            st.execute( "INSERT INTO seq_region VALUES (100,'KI270728.1',2)" ); // not on the rank-1 coordinate system
        }
        Random rnd = new Random( 1 );
        con.setAutoCommit( false );
        try( PreparedStatement gs = con.prepareStatement( "INSERT INTO gene_stable_id VALUES (?,?)" );
                PreparedStatement tr = con.prepareStatement( "INSERT INTO transcript VALUES (?,?,?,?,?,?,?)" );
                PreparedStatement ts = con.prepareStatement( "INSERT INTO transcript_stable_id VALUES (?,?)" );
                PreparedStatement ex = con.prepareStatement( "INSERT INTO exon VALUES (?,?,?,?)" );
                PreparedStatement et = con.prepareStatement( "INSERT INTO exon_transcript VALUES (?,?,?)" );
                PreparedStatement tl = con.prepareStatement( "INSERT INTO translation VALUES (?,?,?,?,?,?)" );
                PreparedStatement tls = con.prepareStatement( "INSERT INTO translation_stable_id VALUES (?,?)" ) )
        {
            int transcriptId = 0, exonId = 0, translationId = 0;
            List<int[]> duplicates = new ArrayList<>(); // {transcriptId, gene, region}
            for( int g = 0; g < GENES; g++ )
            {
                gs.setInt( 1, g + 1 );
                gs.setString( 2, gene( g + 1 ) );
                gs.addBatch();
                int region = g % 997 == 0 ? 100 : 1 + rnd.nextInt( 24 );
                int geneStart = 1 + rnd.nextInt( 200_000_000 );
                int strand = rnd.nextBoolean() ? 1 : -1;
                int poolSize = 3 + rnd.nextInt( 12 );
                int[][] pool = new int[poolSize][];
                int pos = geneStart;
                for( int e = 0; e < poolSize; e++ )
                {
                    pos += 50 + rnd.nextInt( 5000 );
                    int len = 50 + rnd.nextInt( 400 );
                    pool[e] = new int[] {++exonId, pos, pos + len - 1};
                    pos += len;
                    ex.setInt( 1, exonId );
                    ex.setInt( 2, region );
                    ex.setInt( 3, pool[e][1] );
                    ex.setInt( 4, pool[e][2] );
                    ex.addBatch();
                }
                int nTranscripts = 1 + rnd.nextInt( 9 );
                for( int t = 0; t < nTranscripts; t++ )
                {
                    transcriptId++;
                    List<int[]> exons = new ArrayList<>();
                    for( int[] e : pool )
                        if( rnd.nextInt( 3 ) > 0 )
                            exons.add( e );
                    if( transcriptId % 5000 == 0 )
                        exons.clear(); // a transcript without exons
                    if( strand == -1 )
                        Collections.reverse( exons );
                    boolean coding = rnd.nextInt( 10 ) < 6 && !exons.isEmpty();
                    addTranscript( tr, ts, transcriptId, g + 1, region, geneStart, pos, strand, coding, String.format( "ENST%011d", transcriptId ) );
                    if( transcriptId % 3000 == 0 )
                        duplicates.add( new int[] {transcriptId, g + 1, region} );
                    for( int r = 0; r < exons.size(); r++ )
                    {
                        et.setInt( 1, exons.get( r )[0] );
                        et.setInt( 2, transcriptId );
                        et.setInt( 3, r + 1 );
                        et.addBatch();
                    }
                    if( coding )
                        for( int k = 0; k < ( transcriptId % 777 == 0 ? 2 : 1 ); k++ ) // a few transcripts with two translations
                        {
                            int[] first = exons.get( rnd.nextInt( exons.size() ) ), last = exons.get( exons.size() - 1 );
                            tl.setInt( 1, ++translationId );
                            tl.setInt( 2, transcriptId );
                            tl.setInt( 3, 1 + rnd.nextInt( first[2] - first[1] + 1 ) );
                            tl.setInt( 4, first[0] );
                            tl.setInt( 5, 1 + rnd.nextInt( last[2] - last[1] + 1 ) );
                            tl.setInt( 6, last[0] );
                            tl.addBatch();
                            tls.setInt( 1, translationId );
                            tls.setString( 2, String.format( "ENSP%011d", translationId ) );
                            tls.addBatch();
                        }
                }
                if( g % 1000 == 999 )
                    flush( con, gs, tr, ts, ex, et, tl, tls );
            }
            // stable ids that are not unique: a second transcript (higher transcript_id, other region and location)
            for( int[] d : duplicates )
                addTranscript( tr, ts, ++transcriptId, d[1], 1 + ( d[2] % 24 ), 5, 500, 1, false, String.format( "ENST%011d", d[0] ) );
            flush( con, gs, tr, ts, ex, et, tl, tls );
            // Settle the statistics now: InnoDB recalculates them in the background after a bulk load, and a plan
            // change between the old and the new run reorders the rows of queries without ORDER BY.
            try( Statement st = con.createStatement() )
            {
                st.execute( "ANALYZE TABLE coord_system, seq_region, gene_stable_id, transcript, transcript_stable_id, exon, "
                        + "exon_transcript, translation, translation_stable_id" );
            }
            con.commit();
            System.err.println( "loaded into " + DB + ": " + GENES + " genes, " + transcriptId + " transcripts (" + duplicates.size()
                    + " duplicate stable ids), " + exonId + " exons, " + translationId + " translations" );
        }
    }

    static void addTranscript(PreparedStatement tr, PreparedStatement ts, int id, int gene, int region, int start, int end, int strand,
            boolean coding, String stableId) throws Exception
    {
        tr.setInt( 1, id );
        tr.setInt( 2, gene );
        tr.setInt( 3, region );
        tr.setInt( 4, start );
        tr.setInt( 5, end );
        tr.setInt( 6, strand );
        tr.setString( 7, coding ? "protein_coding" : "lncRNA" );
        tr.addBatch();
        ts.setInt( 1, id );
        ts.setString( 2, stableId );
        ts.addBatch();
    }

    static void flush(Connection con, PreparedStatement... statements) throws Exception
    {
        for( PreparedStatement ps : statements )
            ps.executeBatch();
        con.commit();
    }
}
