package biouml.plugins.ensembl._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;

public class TestFruitflyGenome extends TestCase
{
    private DataCollection<?> genomesCollection = null;
    private DataCollection<AnnotatedSequence> sequencesCollection;
    
    private String fastaFile = "/home/ivan/work/2018.03.21-Wed/Drosophila_melanogaster.BDGP6.dna.toplevel.fa.gz";
    private String jdbcURL = "ensembl_fruitfly_91";

    protected synchronized void initSequenceCollection() throws Exception
    {
        if(genomesCollection == null)
        {
            Properties properties = new Properties();
            properties.put("name", "Genomes");
            properties.put("class", "biouml.plugins.ensembl.access.EnsemblSequenceSetsCollection");
            properties.put("chromosomes", "2L,2R,3L,3R,4,X,Y,mitochondrion_genome");
            properties.put("jdbcDriverClass", "com.mysql.jdbc.Driver");
            properties.put("jdbcURL", jdbcURL);
            genomesCollection = CollectionFactory.createCollection(null, properties);
            sequencesCollection = (DataCollection)genomesCollection.get( "chromosomes BDGP6" );
        }
    }
    
    public void testSequenceContent() throws Exception
    {
        initSequenceCollection();
        
        File file = new File(fastaFile);
        try( BufferedReader reader = new BufferedReader( new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ) ) ) )
        {
            String line = reader.readLine();

            while( line != null )
            {
                String header = line;
                String chrName = header.substring( 1 ).split( " " )[0];
                AnnotatedSequence chr = sequencesCollection.get( chrName );
                assertNotNull( "Not found " + chrName, chr );
                Sequence seq = chr.getSequence();
                int pos = seq.getStart();
                while( ( line = reader.readLine() ) != null && !line.startsWith( ">" ) )
                {
                    for( int i = 0; i < line.length(); i++ )
                    {
                        char expected = Character.toUpperCase( line.charAt( i ) );
                        char actual = Character.toUpperCase( (char)seq.getLetterAt( pos + i ) );
                        assertEquals( expected, actual );
                    }
                    pos += line.length();
                }
            }
        }
    }
    
   
}
