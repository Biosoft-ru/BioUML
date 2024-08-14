package biouml.plugins.kegg.type.access._test;

import junit.framework.TestCase;
import ru.biosoft.access.Entry;
import biouml.plugins.kegg.type.access.CompoundTransformer;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Substance;

public class TestCompoundTransformer extends TestCase
{
    public void testTypes()
    {
        CompoundTransformer transformer = new CompoundTransformer();
        assertEquals("Wrong input type", Entry.class, transformer.getInputType());
        assertEquals("Wrong output type", Substance.class, transformer.getOutputType());
    }

    public void testCompoundFromEntry() throws Exception
    {
        CompoundTransformer transformer = new CompoundTransformer();
        Substance compound = transformer.transformInput(defaultEntry);
        checkCompound(compound);
    }

    public void testWriteCompound() throws Exception
    {
        CompoundTransformer transformer = new CompoundTransformer();
        Substance compound = transformer.transformInput(defaultEntry);
        Entry entry = transformer.transformOutput(compound);
    }

    @Override
    protected void setUp()
    {
        String LN = System.getProperty("line.separator");
        StringBuffer data = new StringBuffer();
        
        data.append("ENTRY       C00069                      Compound").append(LN);
        data.append("NAME        Alcohol").append(LN);
        data.append("FORMULA     HOR").append(LN);
        data.append("REACTION    R00626 R00627 R00628 R00629 R00630 R00857 R01478 R01480").append(LN);
        data.append("            R01760 R02588 R03162 R03255 R04070 R04191 R04411 R05788").append(LN);
        data.append("            R07326 R07327 R07328").append(LN);
        data.append("ENZYME      1.1.1.1         1.1.1.2         1.1.1.71        2.3.1.84").append(LN);
        data.append("            2.3.1.152       2.8.2.2         3.1.1.1         3.1.1.6").append(LN);
        data.append("            3.1.1.43        3.1.1.48        3.1.3.1         3.1.3.2").append(LN);
        data.append("            3.1.4.46        3.2.1.31        3.2.1.51        3.2.1.85").append(LN);
        data.append("            3.2.1.88        3.2.1.112       3.2.1.139       3.2.1.149").append(LN);
        data.append("DBLINKS     PubChem: 3369").append(LN);
        data.append("            ChEBI: 30879").append(LN);
        data.append("ATOM        2").append(LN);
        data.append("            1   O1a O     0.3586    0.2034").append(LN);
        data.append("            2   R   R    -0.3586   -0.2034").append(LN);
        data.append("BOND        1").append(LN);
        data.append("            1     1   2 1").append(LN);
        data.append("///").append(LN);

        defaultEntry = new Entry(null, "C00069", data.toString());

        defaultCompound = new Substance(null, "C00069");
        defaultCompound.setTitle("Alcohol");
        defaultCompound.setCompleteName("Alcohol");
        defaultCompound.setFormula("HOR");

        DatabaseReference dbRef = new DatabaseReference("PubChem", "3369");
        DatabaseReference dbRef2 = new DatabaseReference("ChEBI", "30879");

        defaultCompound.setDatabaseReferences(new DatabaseReference[] {dbRef, dbRef2});
    }

    private void checkCompound(Substance compound)
    {
        assertEquals("Wrong name", defaultCompound.getName(), compound.getName());
        assertEquals("Wrong title", defaultCompound.getTitle(), compound.getTitle());
        assertEquals("Wrong complete name", defaultCompound.getCompleteName(), compound.getCompleteName());
        assertEquals("Wrong formula", defaultCompound.getFormula(), compound.getFormula());
        
        DatabaseReference[] defaultRefs = defaultCompound.getDatabaseReferences();
        DatabaseReference[] currRefs = compound.getDatabaseReferences();
        assertNotNull("defaultRef == null", defaultRefs);
        assertTrue("defaultRefs is empty", defaultRefs.length > 0);
        assertNotNull("currRef == null", currRefs);
        assertTrue("currtRefs is empty", currRefs.length > 0);

        assertEquals("Wrong database reference name", defaultRefs[0].getDatabaseName(), currRefs[0].getDatabaseName());
        assertEquals("Wrong database identifier", defaultRefs[0].getId(), currRefs[0].getId());
        assertEquals("Wrong database reference name", defaultRefs[1].getDatabaseName(), currRefs[1].getDatabaseName());
        assertEquals("Wrong database identifier", defaultRefs[1].getId(), currRefs[1].getId());
    }

    private Entry defaultEntry = null;
    private Substance defaultCompound = null;
}