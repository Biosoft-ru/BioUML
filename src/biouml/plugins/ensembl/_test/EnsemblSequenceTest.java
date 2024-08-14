package biouml.plugins.ensembl._test;

import java.util.Properties;

import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa._test.BSATestUtils;

public class EnsemblSequenceTest extends TestCase
{
    private static DataCollection<AnnotatedSequence> sequenceCollection = null;

    protected synchronized void initSequenceCollection()
    {
        if(sequenceCollection == null)
        {
            Properties properties = new Properties();
            properties.put("name", "chromosomes GRCh37");
            properties.put("data-element-class", "ru.biosoft.bsa.AnnotatedSequence");
            properties.put("class", "ru.biosoft.access.SqlDataCollection");
            properties.put("chromosomes", "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,X,Y,MT");
            properties.put("coordSystemId", "2");
            properties.put("genomeBuild", "hg19");
            properties.put("transformerClass", "biouml.plugins.ensembl.access.EnsemblSequenceTransformer");
            properties.put("jdbcDriverClass", "com.mysql.jdbc.Driver");
            properties.put("jdbcURL", "ensembl_human_64");
            sequenceCollection = CollectionFactory.createCollection(null, properties);
        }
    }
    
    public void testSequenceContent() throws Exception
    {
        initSequenceCollection();

        Sequence seq = sequenceCollection.get("1").getSequence();
        String fragment = new SequenceRegion(seq, 1387760, 12, false, false).toString();
        assertEquals("GTTTGGGGAAGG", fragment.toUpperCase());

        seq = sequenceCollection.get("2").getSequence();
        fragment = new SequenceRegion(seq, 12345678, 13, false, false).toString();
        assertEquals("CATTGGAGCCAGT", fragment.toUpperCase());

        seq = sequenceCollection.get( "X" ).getSequence();
        fragment = new SequenceRegion(seq, 987654, 22, false, false).toString();
        assertEquals("CCCCCTCCTCCTCCTCCTCCCT", fragment.toUpperCase());
    }
    
    public void testSequentialRead() throws Exception
    {
        BSATestUtils.createRepository();
        BSATestUtils.createRepository();
        AnnotatedSequence aseq = DataElementPath.create("databases/Ensembl/Sequences/chromosomes NCBI36/19").getDataElement(AnnotatedSequence.class);
        Sequence seq = aseq.getSequence();
        int start = 7297000;
        int length = 10;
        byte[] result = new byte[] {65, 65, 84, 84, 67, 78, 78, 78, 78, 78};
        for(int i=start; i<start+length; i++)
            assertEquals("Letter at "+i, result[i-start], seq.getLetterAt(i));
        for(int i=start+length-1; i>=start; i--)
            assertEquals("Letter at "+i, result[i-start], seq.getLetterAt(i));
    }

    public void testPARRegions() throws Exception
    {
        initSequenceCollection();
        Sequence chrY = sequenceCollection.get("Y").getSequence();
        String fragment = new SequenceRegion( chrY, 10001, 10, false, false ).toString();
        assertEquals("CTAACCCTAA", fragment.toUpperCase());
    }
}
