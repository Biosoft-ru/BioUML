package biouml.plugins.sedml._test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.xml.MathMLParser;
import biouml.plugins.sedml.MathMLUtils;
import junit.framework.TestCase;

public class TestMathMLUtils extends TestCase
{
    public void testEvaluateExpression()
    {
        Object res = Utils.evaluateExpression( "1+5", Collections.emptyMap() );
        assertEquals( 6.0d, res );
    }
    
    public void testEvaluateVectorFunctions()
    {
        Map<String, double[]> scope = new HashMap<>();
        scope.put( "x", new double[] {1,2,3,4,5} );
        AstStart math = Utils.parseExpression( "1 + max(x)" );
        MathMLUtils.evaluateVectorFunctions( math, scope );
        assertEquals( 6.0d, Utils.evaluateExpression( math, Collections.emptyMap() ) );
    }
    
    public void testEvaluateVectorFunctions1()
    {
        Map<String, double[]> vscope = new HashMap<>();
        vscope.put( "x", new double[] {1,2,3,4,5} );
        AstStart math = Utils.parseExpression( "x/max(x)" );
        MathMLUtils.evaluateVectorFunctions( math, vscope );
        Map<String, Object> scope = new HashMap<>();
        scope.put( "x", 3d );
        assertEquals( 3d/5d, Utils.evaluateExpression( math, scope ) );
    }
    
    public void testParseXML()
    {
        String xml =
                "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n"+
                "<apply>\n"+     
                " <divide/>\n"+
                "  <ci>x</ci>\n"+
                "  <apply>\n"+       
                "    <csymbol definitionURL=\"http://sed-ml.org/#max\" encoding=\"text\">max</csymbol>\n"+
                "    <ci>x</ci>\n"+
                "  </apply>\n"+      
                "</apply>\n"+    
                "</math>";
        MathMLParser mathmlParser = new MathMLParser();
        mathmlParser.setContext( new DefaultParserContext() );
        mathmlParser.declareCSymbol( new PredefinedFunction( "max", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1 ) );
        assertEquals(0, mathmlParser.parse( xml ));
        AstStart ast = mathmlParser.getStartNode();
        assertNotNull( ast );
        String expr = MathMLUtils.mathMLToExpression( ast );
        assertEquals("x/max(x)", expr);
    }
    
    public void testPiecewise()
    {
        String xml = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">"+ 
                "  <piecewise>"+ 
                "    <piece>"+ 
                "      <cn> 8 </cn>"+ 
                "      <apply>"+ 
                "        <lt />"+ 
                "        <ci> index </ci>"+ 
                "        <cn> 1 </cn>"+ 
                "      </apply>"+ 
                "    </piece>"+ 
                "    <piece>"+ 
                "      <cn> 0.1 </cn>"+ 
                "      <apply>"+ 
                "        <and />"+ 
                "        <apply>"+ 
                "          <geq />"+ 
                "          <ci> index </ci>"+ 
                "          <cn> 4 </cn>"+ 
                "        </apply>"+ 
                "        <apply>"+ 
                "          <lt />"+ 
                "          <ci> index </ci>"+ 
                "          <cn> 6 </cn>"+ 
                "        </apply>"+ 
                "      </apply>"+ 
                "    </piece>"+ 
                "    <otherwise>"+ 
                "      <cn> 8 </cn>"+ 
                "    </otherwise>"+ 
                "  </piecewise>"+ 
                "</math>";
        MathMLParser mathmlParser = new MathMLParser();
        mathmlParser.setContext( new DefaultParserContext() );
        assertEquals(0, mathmlParser.parse( xml ));
        String expr = MathMLUtils.mathMLToExpression( mathmlParser.getStartNode() );
        assertEquals( "piecewise(index<1.0=>8.0;(index>=4.0)&&(index<6.0)=>0.1;8.0)", expr.replaceAll( " ", "" ) );
    }

}
