package ru.biosoft.math._test;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.math.model.Parser;
import ru.biosoft.math.xml.MathMLFormatter;
import ru.biosoft.math.xml.MathMLParser;


public class MathMLFormatterTest extends TestCase
{
    static MathMLParser mlp = null;

    public MathMLFormatterTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(MathMLFormatterTest.class.getName());
//        suite.addTest(new MathMLFormatterTest("test_1"));
//        suite.addTest(new MathMLFormatterTest("test_fromDirectory"));
        return suite;
    }

/*
    public void test_fromDirectory()
    {
        File dir = new File("ru/biosoft/math/_test/MathMLsamples");
        assertTrue(dir.isDirectory());
        File [] files = dir.listFiles();
        for( int i = 0; files[i] != null; i++ )
        {
            FileReader fr = new FileReader(files[i]);
            char[] buf = new char[files[i].length()];
            fr.read(buf, 0, files[i].length());
            String xml = new String(buf);
            test_ParseFormat(xml);
        }
    }
*/
    public void test_1()
    {
//        test_ParseFormat("<math><apply><plus/><ci>a</ci><cn>b</cn></apply></math>");
//        test_ParseFormat("<math><apply><log/><cn>b</cn></apply></math>");
//        test_ParseFormat("<math><lambda><bvar><ci>x</ci></bvar><bvar><ci>y</ci></bvar><apply><log/><degree><ci>x</ci></degree><ci>y</ci></apply></lambda></math>");

//        test_ParseFormatEqual("<math><apply><eq/><apply><diff/><bvar><ci>time</ci></bvar><ci>ACh1</ci></apply><ci>delta_ACh1_rxn0</ci></apply></math>");
//        test_ParseFormatEqual("<math><apply><minus/><apply><times/><ci>Kf_CalciumCalbindin_BoundCytosol</ci><ci>CaBP_C</ci><ci>Ca_C</ci></apply><apply><times/><ci>Kr_CalciumCalbindin_BoundCytosol</ci><ci>CaBPB_C</ci></apply></apply></math>");
//        test_ParseFormatEqual("<math><apply><divide/><apply><times/><ci>Vmax</ci><ci>kP</ci><ci>CaPump_PM</ci><apply><minus/><ci>Ca_C</ci><ci>Ca_Rest</ci></apply></apply><apply><times/><apply><plus/><ci>Ca_C</ci><ci>kP</ci></apply><apply><plus/><ci>Ca_Rest</ci><ci>kP</ci></apply></apply></apply></math>");
//        test_ParseFormatEqual("<math><apply><divide/><apply><minus/><apply><divide/><cn>1.0</cn><apply><plus/><cn>1.0</cn><apply><times/><ci>m</ci><apply><power/><apply><csymbol>delay</csymbol><ci>P</ci><ci>delta_t</ci></apply><ci>q</ci></apply></apply></apply></apply><ci>P</ci></apply><ci>tau</ci></apply></math>");
//        test_ParseFormatEqual("<math><lambda><bvar><ci>x</ci></bvar><apply><times/><ci>x</ci><cn>2.0</cn></apply></lambda></math>");
//        test_ParseFormatEqual("<math><apply><times/><apply><divide/><apply><times/><ci>vm</ci><ci>s2</ci></apply><apply><plus/><ci>km</ci><ci>s2</ci></apply></apply><ci>cell</ci></apply></math>");
    }

    public void test_ParseFormat(String mxml)
    {
            System.out.println("\n*** test_ParseFormat ***");

        mlp = new MathMLParser();
        System.out.println("Initial:\n" + mxml);

        assertEquals(Parser.STATUS_OK, mlp.parse(mxml));
        ru.biosoft.math.model.AstStart start = mlp.getStartNode();

        MathMLFormatter mmlf = new MathMLFormatter();
        String out = mmlf.format(start)[1];
        System.out.println("Result:\n" + out);
    }

    public void test_ParseFormatEqual(String mxml)
    {
            System.out.println("\n*** test_ParseFormat ***");
        System.out.println("Initial:\n" + mxml);

//        assertEquals(Parser.STATUS_OK, mlp.parse(mxml));
        mlp.parse(mxml);
        ru.biosoft.math.model.AstStart start = mlp.getStartNode();


        mlp = new MathMLParser();
        List<String> msgs = mlp.getMessages();
        int size = msgs.size();
        System.out.println(size);
        for(String msg: msgs)
            System.out.println(msg);


        MathMLFormatter mmlf = new MathMLFormatter();
        String out = mmlf.format(start)[1];
        System.out.println("Result:\n" + out);
        assertEquals("output doesn_t match input", mxml, out);
    }
}
