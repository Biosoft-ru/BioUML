package biouml.plugins.antimony.astparser_v2._test;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import biouml.plugins.antimony.AntimonyTextGenerator;
import biouml.plugins.antimony.astparser_v2.AntimonyNotationParser;
import biouml.plugins.antimony.astparser_v2.AstStart;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AstTest extends TestCase
{
    final static String FILE_PATH = "biouml/plugins/antimony/astparser_v2/_test/antimony.txt";
    final static String FILE_PATH_KEYWORDNAMED_OBJECTS = "biouml/plugins/antimony/astparser_v2/_test/antimony_objects_with_keyword_names.txt";

    public AstTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(AstTest.class.getName());
        suite.addTest(new AstTest("testFromFile"));
        suite.addTest(new AstTest("keywordnamedObjectsTest"));
        return suite;
    }

    public void testFromFile() throws Exception
    {
        AntimonyNotationParser parser = new AntimonyNotationParser();
        try (FileInputStream is = new FileInputStream(FILE_PATH);
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
        {
            AstStart astStart = parser.parse(reader);
            StringBuffer dump = new StringBuffer();
            assertTrue(astStart != null);
            astStart.dump(dump, "");
            AntimonyTextGenerator generator = new AntimonyTextGenerator(astStart);
            generator.generateText();
        }
    }


    public void keywordnamedObjectsTest() throws Exception
    {
        AntimonyNotationParser parser = new AntimonyNotationParser();
        try (FileInputStream is = new FileInputStream(FILE_PATH_KEYWORDNAMED_OBJECTS);
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
        {
            AstStart astStart = parser.parse(reader);
            StringBuffer dump = new StringBuffer();
            assertTrue(astStart != null);
            astStart.dump(dump, "");
            AntimonyTextGenerator generator = new AntimonyTextGenerator(astStart);
            generator.generateText();
        }

    }
}
