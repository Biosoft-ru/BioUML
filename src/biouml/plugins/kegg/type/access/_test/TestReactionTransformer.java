package biouml.plugins.kegg.type.access._test;

import junit.framework.TestCase;
import ru.biosoft.access.Entry;
import biouml.plugins.kegg.type.access.ReactionTransformer;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class TestReactionTransformer extends TestCase
{
    public TestReactionTransformer(String name)
    {
        super(name);
    }

    @Override
    public void setUp()
    {
        String LN = System.getProperty("line.separator");
        StringBuffer data = new StringBuffer();
        data.append("ENTRY       R00251").append(LN);
        data.append("NAME        5-Oxoproline amidohydrolase (ATP-hydrolysing)").append(LN);
        data.append("DEFINITION  ATP + 5-Oxoproline + 2 H2O <=> ADP + Orthophosphate + L-Glutamate").append(LN);
        data.append("EQUATION    C00002 + C01879 + 2 C00001 <=> C00008 + C00009 + C00025").append(LN);
        data.append("PATHWAY     PATH: RN00480  Glutathione metabolism").append(LN);
        data.append("ENZYME      3.6.3.50        3.6.3.51        3.6.3.52        3.6.3.46").append(LN);
        data.append("            3.6.3.49        3.6.1.3         3.6.1.5").append(LN);
        data.append("///").append(LN);
        defaultEntry = new Entry(null, "R00251", data.toString());

        //Init Reaction
        defaultReaction = new Reaction(null, "R00251");
        defaultReaction.setTitle("5-Oxoproline amidohydrolase (ATP-hydrolysing)");
        defaultReaction.setDescription("Definition: ATP + 5-Oxoproline + 2 H2O <=> ADP + Orthophosphate + L-Glutamate");

        try
        {
            //init reaction reactants and products
            SpecieReference ref = new SpecieReference(defaultReaction, "C00002", SpecieReference.REACTANT);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "C01879", SpecieReference.REACTANT);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "C00001", SpecieReference.REACTANT);
            ref.setStoichiometry("2");
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "C00008", SpecieReference.PRODUCT);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "C00009", SpecieReference.PRODUCT);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "C00025", SpecieReference.PRODUCT);
            defaultReaction.put(ref);

            //init enzymes
            ref = new SpecieReference(defaultReaction, "3.6.3.50", SpecieReference.MODIFIER);
            ref.setModifierAction(SpecieReference.ACTION_CATALYST);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "3.6.3.51", SpecieReference.MODIFIER);
            ref.setModifierAction(SpecieReference.ACTION_CATALYST);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "3.6.3.52", SpecieReference.MODIFIER);
            ref.setModifierAction(SpecieReference.ACTION_CATALYST);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "3.6.3.46", SpecieReference.MODIFIER);
            ref.setModifierAction(SpecieReference.ACTION_CATALYST);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "3.6.3.49", SpecieReference.MODIFIER);
            ref.setModifierAction(SpecieReference.ACTION_CATALYST);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "3.6.1.3", SpecieReference.MODIFIER);
            ref.setModifierAction(SpecieReference.ACTION_CATALYST);
            defaultReaction.put(ref);
            ref = new SpecieReference(defaultReaction, "3.6.1.5", SpecieReference.MODIFIER);
            ref.setModifierAction(SpecieReference.ACTION_CATALYST);
            defaultReaction.put(ref);
        }
        catch (Throwable th)
        {
            fail(th.getMessage());
        }
    }

    public void testTransformInput() throws Exception
    {
        ReactionTransformer transformer = new ReactionTransformer();
        Reaction reaction = transformer.transformInput(defaultEntry);
        assertEquals("wrong name", defaultReaction.getName(), reaction.getName());
        assertEquals("wrong title", defaultReaction.getTitle(), reaction.getTitle());
        assertEquals("wrong description", defaultReaction.getDescription(), reaction.getDescription());

        SpecieReference[] refs = reaction.getSpecieReferences();
        try
        {
            for (int i = 0; i < refs.length; i++)
            {
                checkSpecieReference(refs[i], reaction.get(refs[i].getName()));
            }
        }
        catch (Throwable th)
        {
            fail(th.getMessage());
        }
    }

    private void checkSpecieReference(SpecieReference expected, SpecieReference actual)
    {
        assertNotNull("component with id: " + expected.getName() + " not found", actual);
        assertEquals("name mismatch", expected.getName(), actual.getName());
        assertEquals("role mismatch", expected.getRole(), actual.getRole());

        if (expected.getRole().equals(SpecieReference.MODIFIER))
        {
            assertEquals("modifierAction mismatch", expected.getModifierAction(), actual.getModifierAction());
        }

        if (expected.getRole().equals(SpecieReference.PRODUCT) ||
            expected.getRole().equals(SpecieReference.REACTANT))
        {
            assertEquals("stoichiometry mismatch", expected.getStoichiometry(), actual.getStoichiometry());
        }
    }

    public void testTransformOutput() throws Exception
    {
        ReactionTransformer transformer = new ReactionTransformer();
        Reaction reaction = transformer.transformInput(defaultEntry);
        Entry entry = transformer.transformOutput(reaction);
    }

    private Entry defaultEntry;
    private Reaction defaultReaction;
}


