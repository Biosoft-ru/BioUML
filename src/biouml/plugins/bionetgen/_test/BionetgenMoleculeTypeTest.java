package biouml.plugins.bionetgen._test;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.bionetgen.bnglparser.BNGList;
import biouml.plugins.bionetgen.bnglparser.BNGMoleculeType;
import biouml.plugins.bionetgen.bnglparser.BionetgenParser;
import biouml.plugins.bionetgen.diagram.BionetgenMolecule;
import biouml.plugins.bionetgen.diagram.BionetgenMoleculeType;
import biouml.plugins.bionetgen.diagram.BionetgenSpeciesGraph;

public class BionetgenMoleculeTypeTest extends TestCase
{
    public BionetgenMoleculeTypeTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BionetgenMoleculeTypeTest.class.getName());

        suite.addTest(new BionetgenMoleculeTypeTest("testBionetgenMoleculeType"));
        suite.addTest(new BionetgenMoleculeTypeTest("testAllowability"));

        return suite;
    }

    private String[] types = new String[] {"A(a~0~1,b,c)", "B(a,b~2~1)", "C(a,c~3~2)"};
    private @Nonnull List<BionetgenMoleculeType> typesList = new ArrayList<>();

    public void testBionetgenMoleculeType()
    {
        int size = typesList.size();
        String[] expectedTypes = new String[] {"A(a~0~1,b,c)", "B(a,b~1~2)", "C(a,c~2~3)"};
        for( int i = 0; i < size; i++ )
        {
            assertEquals(expectedTypes[i] + " vs " + typesList.get(i).toString(), expectedTypes[i], typesList.get(i).toString());
        }
    }

    public void testAllowability()
    {
        String[][] tests = new String[][] {new String[] {"A(a~0,b,c)", "true"}, new String[] {"A(a~1,b,c)", "true"},
                new String[] {"A(a~3,b,c)", "false"}, new String[] {"B(a,b~1)", "true"}, new String[] {"B(a,b~2)", "true"},
                new String[] {"B(a,b~0)", "false"}, new String[] {"B(c)", "false"}, new String[] {"A(a,b,c,d)", "true", "false"},
                new String[] {"A(a,b,c)", "true", "true"}, new String[] {"C(c,a)", "true", "true"},
                new String[] {"A(a,b,c)", "false", "false"}, new String[] {"C(c,a)", "false", "false"},};

        boolean isTemplate = false;
        boolean expectedResult;
        for( String[] test : tests )
        {
            BionetgenMolecule mol = new BionetgenMolecule(new BionetgenSpeciesGraph(""), test[0]);
            if( test.length == 3 )
            {
                isTemplate = Boolean.parseBoolean(test[1]);
                expectedResult = Boolean.parseBoolean(test[2]);
            }
            else
                expectedResult = Boolean.parseBoolean(test[1]);

            assertEquals(expectedResult, BionetgenMoleculeType.checkAllowability(typesList, mol, isTemplate));
            isTemplate = false;
        }
    }

    @Override
    public void setUp()
    {
        BNGList moleculeTypes = new BNGList(BionetgenParser.JJTLIST);
        moleculeTypes.setType(BNGList.MOLECULETYPE);
        BionetgenParser parser = new BionetgenParser();
        for( String type : types )
        {
            BNGMoleculeType moleculeType = new BNGMoleculeType(BionetgenParser.JJTMOLECULETYPE);
            moleculeType.addAsLast(parser.parseSpecies(type));
            moleculeTypes.addAsLast(moleculeType);
        }
        typesList = BionetgenMoleculeType.createMoleculeTypesList(moleculeTypes);
    }

}
