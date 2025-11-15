package biouml.plugins.wdl._test;

import biouml.model.Diagram;
import biouml.plugins.wdl.WDLGenerator;

public class TestWDL //extends //TestCase
{

    private static String[] list = new String[] {"hello", "scatter_range_2_steps", "scatter_simple", "scatter_range", "scatter_range2",
            "two_steps", "four_steps"};

    public static void main(String ... args) throws Exception
    {
        //        test("two_steps");
        //        for (String name: list)
        //            test(name);

        //                test( "scatter_range_2_extra" );
        //                test( "scatter_range_2_steps" );
        //                test( "scatter_simple" );
                test( "double_scatter" );
        //                test( "scatter_range2" );
        //        test( "two_steps" );
        //        test( "four_steps" );
        //        test( "private_declaration_task" );
        //                test( "pbmm2" );
        //        test( "pbsv_1" );
        //        test( "array_input" );
        // test( "array_select");


        //        test( "array_on_the_fly" );
        //        test( "lima" );
//        test( "faidx2" );
        
//        test( "person_struct_task");
//        test( "simple_if");
//        test( "scatter_extra_steps");
        //        test("faidx_import");
        //        test( "fastqc1" );
        //        test( "test_scatter" );
    }

    public static void test() throws Exception
    {
        test( "hello" );
    }

    public static void test(String name) throws Exception
    {
        Diagram diagram = TestUtil.loadDiagram( name );
        WDLGenerator wdlGenerator = new WDLGenerator();

        String wdl = wdlGenerator.generate( diagram );
        
        System.out.println( "Exported WDL: " );
        System.out.println( wdl );
    }
}
