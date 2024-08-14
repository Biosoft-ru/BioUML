package ru.biosoft.bsa.transformer._test;

import ru.biosoft.access.Entry;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.transformer.EmblTrackTransformer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EmblTrackTransformerTest extends TestCase
{
    public static final String SEQUENCE_PATH = "../data_resources/_test/sequences";

    protected String simpleTestStr = "FH   Key             Location/Qualifiers\n" + "FH\n" + "FT   source          1..1145\n"
            + "FT                   /organism=\"Cairina moschata\"\n" + "FT   prim_transcript 331..1145\n"
            + "FT                   /note=\"alpha-A-globin mRNA\"\n" + "FT   CDS             join(367..461,612..816,921..1049)\n"
            + "FT                   /note=\"alpha-A globin; NCBI gi: 212914\"\n" + "FT                   /codon_start=1\n"
            + "FT   exon            <367..461\n" + "FT                   /note=\"alpha-A globin\"\n" + "FT                   /number=1\n"
            + "FT   intron          462..611\n" + "FT                   /note=\"alpha-A-globin intron A\"\n"
            + "FT   exon            612..816\n" + "FT                   /number=2\n" + "FT   intron          817..920\n"
            + "FT                   /note=\"alpha-A-globin intron B\"\n" + "FT   exon            921..>1049\n"
            + "FT                   /note=\"alpha-A globin\"\n" + "FT                   /number=3\n";

    public void testSimple() throws Exception
    {
        EmblTrackTransformer transformer = new EmblTrackTransformer();
        assertNotNull("Can not create track transformer", transformer);
        Entry entry = new Entry(null, "test", simpleTestStr);
        Track track = transformer.transformInput(entry);
        assertNotNull("Can not transform Entry to Track", track);
        Entry entry2 = transformer.transformOutput(track);
        assertNotNull("Can not transform Track to Entry", entry2);
    }

    public static Test suite()
    {
        return new TestSuite(EmblTrackTransformerTest.class);
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(EmblTrackTransformerTest.class);
        }
    }
}
