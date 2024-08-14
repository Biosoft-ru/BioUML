package biouml.plugins.kegg.type.access._test;

import junit.framework.TestCase;
import ru.biosoft.access.Entry;
import biouml.plugins.kegg.type.Glycan;
import biouml.plugins.kegg.type.access.GlycanTransformer;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramReference;

public class TestGlycanTransformer extends TestCase
{
    public TestGlycanTransformer(String name)
    {
        super(name);
    }

    @Override
    public void setUp() throws Exception
    {
        String LN = System.getProperty("line.separator");
        StringBuffer data = new StringBuffer();

        data.append("ENTRY       G00045                      Glycan").append(LN);
        data.append("NAME        IV2Fuc,III4Fuc-Lc4Cer;").append(LN);
        data.append("            IV2-a-Fuc,III4-a-Fuc-Lc4Cer;").append(LN);
        data.append("            Leb glycolipid").append(LN);
        data.append("COMPOSITION (Gal)2 (Glc)1 (GlcNAc)1 (LFuc)2 (Cer)1").append(LN);
        data.append("MASS        981.9 (Cer)").append(LN);
        data.append("CLASS       Glycolipid; Sphingolipid").append(LN);
        data.append("REFERENCE   1  [PMID:3392043]").append(LN);
        data.append("            Larson G, Falk P, Hoskins LC.").append(LN);
        data.append("            Degradation of human intestinal glycosphingolipids by extracellular").append(LN);
        data.append("            glycosidases from mucin-degrading bacteria of the human fecal flora.").append(LN);
        data.append("            J. Biol. Chem. 263 (1988) 10790-8.").append(LN);
        data.append("            2  [PMID:9111138]").append(LN);
        data.append("            Henry S, Jovall PA, Ghardashkhani S, Elmgren A, Martinsson T, Larson").append(LN);
        data.append("            G, Samuelsson B.").append(LN);
        data.append("            Structural and immunochemical identification of Le(a), Le(b), H type").append(LN);
        data.append("            1, and related glycolipids in small intestinal mucosa of a group O").append(LN);
        data.append("            Le(a-b-) nonsecretor.").append(LN);
        data.append("            Glycoconj. J. 14 (1997) 209-23.").append(LN);
        data.append("REACTION    R06163").append(LN);
        data.append("PATHWAY     PATH: map00601  Glycosphingolipid biosynthesis - lactoseries").append(LN);
        data.append("ENZYME      2.4.1.65").append(LN);
        data.append("DBLINKS     CCSD: 1174 1175 1176 2712 8196 8363 8728 13370 14015 14078 14901").append(LN);
        data.append("                  15611 16288 16382 16567 18430 21111 21742 21916 23894 23917").append(LN);
        data.append("                  25515 33041 34832 35055 36050 36052 36115 40778 42843 43739").append(LN);
        data.append("                  48139 48392 50202 50496").append(LN);
        data.append("NODE        7").append(LN);
        data.append("            1   Cer        18     0").append(LN);
        data.append("            2   Glc        11     0").append(LN);
        data.append("            3   Gal         4     0").append(LN);
        data.append("            4   GlcNAc     -4     0").append(LN);
        data.append("            5   LFuc      -12     5").append(LN);
        data.append("            6   Gal       -12    -5").append(LN);
        data.append("            7   LFuc      -19    -5").append(LN);
        data.append("EDGE        6").append(LN);
        data.append("            1     2:b1    1:1").append(LN);
        data.append("            2     3:b1    2:4").append(LN);
        data.append("            3     4:b1    3:3").append(LN);
        data.append("            4     5:a1    4:4").append(LN);
        data.append("            5     6:b1    4:3").append(LN);
        data.append("            6     7:a1    6:2").append(LN);
        data.append("///").append(LN);


        defaultEntry = new Entry(null, "G00045", data.toString());

        defaultGlycan = new Glycan(null, defaultEntry.getName());
        defaultGlycan.setTitle("IV2-a-Fuc,III4-a-Fuc-Lc4Cer");
        defaultGlycan.setCompleteName("IV2Fuc,III4Fuc-Lc4Cer");
        defaultGlycan.setSynonyms("Leb glycolipid");
        defaultGlycan.setComposition("(Gal)2 (Glc)1 (GlcNAc)1 (LFuc)2 (Cer)1");
        defaultGlycan.setMass(981.9f);
        defaultGlycan.setGlycanClass("Glycolipid; Sphingolipid");

        DatabaseReference ref1 = new DatabaseReference("CCSD", "1174");
        DatabaseReference ref2 = new DatabaseReference("CCSD", "1175");
        defaultGlycan.setDatabaseReferences(new DatabaseReference[] {ref1, ref2});
    }

    public void testTypes()
    {
        GlycanTransformer transformer = new GlycanTransformer();
        assertEquals("Wrong input type", Entry.class, transformer.getInputType());
        assertEquals("Wrong output type", Glycan.class, transformer.getOutputType());
    }

    public void testTransformInput()
    {
        GlycanTransformer transformer = new GlycanTransformer();
        try
        {
            Glycan glycan = transformer.transformInput(defaultEntry);
            checkGlycan(glycan);
        }
        catch( Throwable th )
        {
            fail(th.getMessage());
        }
    }

    public void testTransformOutput()
    {
        GlycanTransformer transformer = new GlycanTransformer();
        try
        {
            Entry entry = transformer.transformOutput(defaultGlycan);
        }
        catch( Throwable th )
        {
            fail(th.getMessage());
        }
    }

    private void checkGlycan(Glycan glycan)
    {
        assertNotNull("new parsed glycan is null", glycan);
        assertEquals("Wrong name", defaultGlycan.getName(), glycan.getName());
        assertEquals("Wrong title", defaultGlycan.getTitle(), glycan.getTitle());
        assertEquals("Wrong complete name", defaultGlycan.getCompleteName(), glycan.getCompleteName());
        assertEquals("Wrong synonyms", defaultGlycan.getSynonyms(), glycan.getSynonyms());
        assertEquals("Wrong composition", defaultGlycan.getComposition(), glycan.getComposition());
        assertEquals("Wrong mass", defaultGlycan.getMass(), glycan.getMass(), 0.0f);
        assertEquals("Wrong glycan class", defaultGlycan.getGlycanClass(), glycan.getGlycanClass());

        assertNotNull("!!!!", glycan.getDatabaseReferences());
        DatabaseReference[] defaultRefs = defaultGlycan.getDatabaseReferences();
        DatabaseReference[] refs = glycan.getDatabaseReferences();
        assertEquals("Wrong", 35, refs.length);

        assertEquals("Wrong db name", defaultRefs[0].getDatabaseName(), refs[0].getDatabaseName());
        assertEquals("Wrong db key", defaultRefs[0].getId(), refs[0].getId());

        assertEquals("Wrong db name", defaultRefs[1].getDatabaseName(), refs[1].getDatabaseName());
        assertEquals("Wrong db key", defaultRefs[1].getId(), refs[1].getId());
    }

    private void checkPathways(DiagramReference[] refs)
    {
        DiagramReference[] defaultRefs = defaultGlycan.getPathways();
        assertNotNull("no pathways found", refs);
        assertEquals("diagram references sizes mismatch", defaultRefs.length, refs.length);
        for( int i = 0; i < defaultRefs.length; i++ )
        {
            assertEquals("pathway name mismatch", defaultRefs[i].getName(), refs[i].getName());
            assertEquals("pathway title mismatch", defaultRefs[i].getTitle(), refs[i].getTitle());
        }
    }

    private Entry defaultEntry;
    private Glycan defaultGlycan;
}
