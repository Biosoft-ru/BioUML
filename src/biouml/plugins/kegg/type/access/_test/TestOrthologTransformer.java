package biouml.plugins.kegg.type.access._test;

import junit.framework.TestCase;
import ru.biosoft.access.Entry;
import biouml.plugins.kegg.type.access.OrthologTransformer;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Protein;

public class TestOrthologTransformer extends TestCase
{
    public TestOrthologTransformer(String name)
    {
        super(name);
    }

    @Override
    public void setUp()
    {
        String LN = System.getProperty("line.separator");
        StringBuffer data = new StringBuffer();
        
        data.append("ENTRY       K00006                      KO").append(LN);
        data.append("NAME        E1.1.1.8, GPD1").append(LN);
        data.append("DEFINITION  glycerol-3-phosphate dehydrogenase (NAD+) [EC:1.1.1.8]").append(LN);
        data.append("CLASS       Metabolism; Lipid Metabolism; Glycerophospholipid metabolism").append(LN);
        data.append("            [PATH:ko00564]").append(LN);
        data.append("DBLINKS     RN: R00842").append(LN);
        data.append("            COG: COG0240").append(LN);
        data.append("            GO: 0004367").append(LN);
        data.append("GENES       HSA: 23171(GPD1L) 2819(GPD1)").append(LN);
        data.append("            PTR: 741054(GPD1)").append(LN);
        data.append("            MCC: 707723(LOC707723)").append(LN);
        data.append("///").append(LN);

        
        defaultEntry = new Entry(null, "K00006", data.toString());

        defaultOrtholog = new Protein(null, "K00006");
        defaultOrtholog.setTitle("E1.1.1.8, GPD1");
        defaultOrtholog.setCompleteName("glycerol-3-phosphate dehydrogenase (NAD+) [EC:1.1.1.8]");

        DatabaseReference ref1 = new DatabaseReference("RN", "R00842");
        DatabaseReference ref2 = new DatabaseReference("COG", "COG0240");
        DatabaseReference ref3 = new DatabaseReference("GO", "0004367");
        defaultOrtholog.setDatabaseReferences(new DatabaseReference[] {ref1, ref2, ref3});
    }

    public void testTypes()
    {
        OrthologTransformer transformer = new OrthologTransformer();
        assertEquals("wrong input type", Entry.class, transformer.getInputType());
        assertEquals("wrong output type", Protein.class, transformer.getOutputType());
    }

    public void testInput()
    {
        try
        {
            OrthologTransformer transformer = new OrthologTransformer();
            Concept ortholog = transformer.transformInput(defaultEntry);
            checkOrtholog(ortholog);
        }
        catch( Throwable th )
        {
            fail(th.getMessage());
        }
    }

    private void checkOrtholog(Concept ortholog)
    {
        assertNotNull("transformerd ortholog is null", ortholog);
        assertEquals("wrong name", defaultOrtholog.getName(), ortholog.getName());
        assertEquals("wrong title", defaultOrtholog.getTitle(), ortholog.getTitle());
        assertEquals("wrong complete name", defaultOrtholog.getCompleteName(), ortholog.getCompleteName());
        DatabaseReference[] defaultRefs = defaultOrtholog.getDatabaseReferences();
        DatabaseReference[] refs = ortholog.getDatabaseReferences();
        assertNotNull("ortholog db references is null", refs);
        assertEquals("wrong db links array size", defaultRefs.length, refs.length);
        for( int i = 0; i < defaultRefs.length; i++ )
        {
            assertEquals("wrong database reference name", defaultRefs[i].getDatabaseName(), refs[i].getDatabaseName());
            assertEquals("wrong database entry id", defaultRefs[i].getId(), refs[i].getId());
        }
    }

    private Concept defaultOrtholog;
    private Entry defaultEntry;
}
