package biouml.plugins.kegg.type.access._test;

import junit.framework.TestCase;
import ru.biosoft.access.Entry;
import biouml.plugins.kegg.type.access.EnzymeTransformer;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Protein;

public class TestEnzymeTransformer extends TestCase
{
    public TestEnzymeTransformer(String name)
    {
        super(name);
    }

    public void testTypes()
    {
        EnzymeTransformer transformer = new EnzymeTransformer();
        assertEquals("Wrong input type", Entry.class, transformer.getInputType());
        assertEquals("Wrong output type", Protein.class, transformer.getOutputType());
    }

    public void testEnzymeFromEntry() throws Exception
    {
        EnzymeTransformer transformer = new EnzymeTransformer();
        Protein enzyme = transformer.transformInput(defaultEntry);
        checkEnzyme(enzyme);
    }

    public void testEnzymeToEntry() throws Exception
    {
        EnzymeTransformer transformer = new EnzymeTransformer();
        Entry entry = transformer.transformOutput(defaultEnzyme);
        //System.err.println(entry.getData());
        //checkEntry( entry );
    }

    @Override
    protected void setUp()
    {
        String ln = System.getProperty("line.separator");
        StringBuffer data = new StringBuffer();

        data.append("ENTRY       EC 1.1.1.94                 Enzyme").append(ln);
        data.append("NAME        glycerol-3-phosphate dehydrogenase [NAD(P)+];").append(ln);
        data.append("            L-glycerol-3-phosphate:NAD(P)+ oxidoreductase;").append(ln);
        data.append("            glycerol phosphate dehydrogenase (nicotinamide adenine dinucleotide").append(ln);
        data.append("            (phosphate));").append(ln);
        data.append("            glycerol 3-phosphate dehydrogenase (NADP+);").append(ln);
        data.append("            glycerol-3-phosphate dehydrogenase [NAD(P)+]").append(ln);
        data.append("CLASS       Oxidoreductases;").append(ln);
        data.append("            Acting on the CH-OH group of donors;").append(ln);
        data.append("            With NAD+ or NADP+ as acceptor").append(ln);
        data.append("SYSNAME     sn-glycerol-3-phosphate:NAD(P)+ 2-oxidoreductase").append(ln);
        data.append("REACTION    sn-glycerol 3-phosphate + NAD(P)+ = glycerone phosphate + NAD(P)H +").append(ln);
        data.append("            H+ [RN:R00842 R00844]").append(ln);
        data.append("ALL_REAC    R00842 R00844").append(ln);
        data.append("SUBSTRATE   sn-glycerol 3-phosphate [CPD:C00093];").append(ln);
        data.append("            NAD+ [CPD:C00003];").append(ln);
        data.append("            NADP+ [CPD:C00006]").append(ln);
        data.append("PRODUCT     glycerone phosphate [CPD:C00111];").append(ln);
        data.append("            NADH [CPD:C00004];").append(ln);
        data.append("            NADPH [CPD:C00005];").append(ln);
        data.append("            H+ [CPD:C00080]").append(ln);
        data.append("COMMENT     The enzyme from Escherichia coli shows specificity for the B side of").append(ln);
        data.append("            NADPH.").append(ln);
        data.append("REFERENCE   1  [PMID:4389388]").append(ln);
        data.append("  AUTHORS   Kito M, Pizer LI.").append(ln);
        data.append("  TITLE     Purification and regulatory properties of the biosynthetic").append(ln);
        data.append("            L-glycerol 3-phosphate dehydrogenase from Escherichia coli.").append(ln);
        data.append("  JOURNAL   J. Biol. Chem. 244 (1969) 3316-23.").append(ln);
        data.append("  ORGANISM  Escherichia coli [GN:eco]").append(ln);
        data.append("REFERENCE   2  [PMID:355254]").append(ln);
        data.append("  AUTHORS   Edgar JR, Bell RM.").append(ln);
        data.append("  TITLE     Biosynthesis in Escherichia coli fo sn-glycerol 3-phosphate, a").append(ln);
        data.append("            precursor of phospholipid.").append(ln);
        data.append("  JOURNAL   J. Biol. Chem. 253 (1978) 6348-53.").append(ln);
        data.append("  ORGANISM  Escherichia coli [GN:eco]").append(ln);
        data.append("PATHWAY     PATH: map00564  Glycerophospholipid metabolism").append(ln);
        data.append("ORTHOLOGY   KO: K00057  glycerol-3-phosphate dehydrogenase (NAD(P)+)").append(ln);
        data.append("GENES       XLA: 444456(MGC83663)").append(ln);
        data.append("            DTNI: 33849").append(ln);
        data.append("            DOTA: Ot01g03730").append(ln);
        data.append("            DTPS: 31003(e_gw1.1.313.1)").append(ln);
        data.append("STRUCTURES  PDB: 1TXG  ").append(ln);
        data.append("DBLINKS     IUBMB Enzyme Nomenclature: 1.1.1.94").append(ln);
        data.append("            ExPASy - ENZYME nomenclature database: 1.1.1.94").append(ln);
        data.append("            ExplorEnz - The Enzyme Database: 1.1.1.94").append(ln);
        data.append("            ERGO genome analysis and discovery system: 1.1.1.94").append(ln);
        data.append("            BRENDA, the Enzyme Database: 1.1.1.94").append(ln);
        data.append("            CAS: 37250-30-9").append(ln);
        data.append("///").append(ln);


        defaultEntry = new Entry(null, "EC 1.1.1.94", data.toString());
        defaultEnzyme = new Protein(null, "EC 1.1.1.94");
        defaultEnzyme.setTitle("1.1.1.94");
        defaultEnzyme
                .setSynonyms("L-glycerol-3-phosphate:NAD(P)+ oxidoreductase;glycerol phosphate dehydrogenase (nicotinamide adenine dinucleotide(phosphate));glycerol 3-phosphate dehydrogenase (NADP+);glycerol-3-phosphate dehydrogenase [NAD(P)+]");
        defaultEnzyme.setCompleteName("sn-glycerol-3-phosphate:NAD(P)+ 2-oxidoreductase");
        defaultEnzyme.setComment("The enzyme from Escherichia coli shows specificity for the B side of" + ln + "NADPH.");
        defaultEnzyme.setGene("XLA: 444456(MGC83663)" + ln + "DTNI: 33849" + ln + "DOTA: Ot01g03730" + ln + "DTPS: 31003(e_gw1.1.313.1)");
        defaultEnzyme.setStructure("PDB: 1TXG");

        DatabaseReference dbRef1 = new DatabaseReference("IUBMB Enzyme Nomenclature", "1.1.1.94");
        DatabaseReference dbRef2 = new DatabaseReference("ExPASy - ENZYME nomenclature database", "1.1.1.94");
        DatabaseReference dbRef3 = new DatabaseReference("ExplorEnz - The Enzyme Database", "1.1.1.94");
        DatabaseReference dbRef4 = new DatabaseReference("ERGO genome analysis and discovery system", "1.1.1.94");
        DatabaseReference dbRef5 = new DatabaseReference("BRENDA, the Enzyme Database", "1.1.1.94");
        DatabaseReference dbRef6 = new DatabaseReference("CAS", "37250-30-9");
        defaultEnzyme.setDatabaseReferences(new DatabaseReference[] {dbRef1, dbRef2, dbRef3, dbRef4, dbRef5, dbRef6});
    }

    private void checkEnzyme(Protein enzyme)
    {
        assertEquals("Wrong name", defaultEnzyme.getName(), enzyme.getName());
        assertEquals("Wrong title", defaultEnzyme.getTitle(), enzyme.getTitle());
        assertEquals("Wrong synonyms", defaultEnzyme.getSynonyms(), enzyme.getSynonyms());
        assertEquals("Wrong complete name", defaultEnzyme.getCompleteName(), enzyme.getCompleteName());
        assertEquals("Wrong comment", defaultEnzyme.getComment(), enzyme.getComment());
        assertEquals("Wrong gene", defaultEnzyme.getGene(), enzyme.getGene());
        assertEquals("Wrong pdbStructure", defaultEnzyme.getStructure(), enzyme.getStructure());
        checkDatabaseReferences(enzyme.getDatabaseReferences());
    }

    private void checkDatabaseReferences(DatabaseReference[] refs)
    {
        DatabaseReference[] defaultRefs = defaultEnzyme.getDatabaseReferences();
        assertNotNull("No database references found", refs);
        assertEquals("dbRef sizes mismatch", defaultRefs.length, refs.length);
        for( int i = 0; i < defaultRefs.length; i++ )
        {
            assertEquals("wrong database name", defaultRefs[i].getDatabaseName(), refs[i].getDatabaseName());
            assertEquals("wrong database key", defaultRefs[i].getId(), refs[i].getId());
        }
    }

    private Entry defaultEntry = null;
    private Protein defaultEnzyme = null;
}