package biouml.plugins.ensembl._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;

public class TestHumanGenome extends TestCase
{
    private DataCollection<?> genomesCollection = null;
    private DataCollection<AnnotatedSequence> sequencesCollection;

    private String fastaFile = "/home/ivan/work/2018.03.21-Wed/Homo_sapiens.GRCh37.73.dna.primary_assembly.fa.gz";
    //private String fastaFile = "/home/ivan/work/2018.03.21-Wed/Homo_sapiens.GRCh37.73.dna.chromosome.Y.fa.gz";
    private String fastaFileChrY = "/home/ivan/work/2018.03.21-Wed/sequence.fasta.gz";

    private String jdbcURL = "ensembl_human_73";

    protected synchronized void initSequenceCollection() throws Exception
    {
        if( genomesCollection == null )
        {
            Properties properties = new Properties();
            properties.put( "name", "Genomes" );
            properties.put( "class", "biouml.plugins.ensembl.access.EnsemblSequenceSetsCollection" );
            properties.put( "jdbcDriverClass", "com.mysql.jdbc.Driver" );
            properties.put( "jdbcURL", jdbcURL );
            genomesCollection = CollectionFactory.createCollection( null, properties );
            sequencesCollection = (DataCollection)genomesCollection.get( "chromosomes GRCh37" );
        }
    }

    public void testSequenceContent() throws Exception
    {
        initSequenceCollection();

        checkFile( new File( fastaFile ), "Y" );
        checkFile( new File( fastaFileChrY ), "" );
    }

    private void checkFile(File file, String exclude) throws IOException, FileNotFoundException, Exception, RuntimeException
    {
        BufferedReader reader = new BufferedReader( new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ) ) );
        String line = reader.readLine();

        while( line != null )
        {
            String header = line;
            String chrName = header.substring( 1 ).split( " " )[0];
            if( chrName.equals( exclude ) )
            {
                line = skipRecord( reader );
                continue;
            }
            System.out.println( "Processing " + chrName );
            AnnotatedSequence chr = sequencesCollection.get( chrName );
            if( chr == null )
            {
                System.out.println( "Not found " + chrName );
                line = skipRecord( reader );
                continue;
            }
            Sequence seq = chr.getSequence();
            int pos = seq.getStart();
            while( ( line = reader.readLine() ) != null && !line.startsWith( ">" ) )
            {
                for( int i = 0; i < line.length(); i++ )
                {
                    char expected = Character.toUpperCase( line.charAt( i ) );
                    char actual = Character.toUpperCase( (char)seq.getLetterAt( pos + i ) );
                    assertEquals( chrName + ":" + ( pos + i ) + " " + actual + "!=" + expected, expected, actual );
                }
                pos += line.length();
            }
        }
    }

    public String skipRecord(BufferedReader reader) throws IOException
    {
        String line;
        while( ( line = reader.readLine() ) != null && !line.startsWith( ">" ) )
            ;
        return line;
    }


}
