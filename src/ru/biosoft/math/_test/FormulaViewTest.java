
package ru.biosoft.math._test;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.logging.LogManager;

import biouml.model._test.ViewTestCase;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.parser.Parser;
import ru.biosoft.math.view.FormulaViewBuilder;

public class FormulaViewTest extends ViewTestCase
{
    public FormulaViewTest(String name)
    {
        super(name);
        File configFile = new File( "./ru/biosoft/math/_test/test.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public void test_1()  throws Exception
    {
        testFormulaView("a+b+c+d");
//        testFormulaView("function abs(x_1) = if(x<0) sin(-x_1); if( x==0) 0/(1+x/2); otherwise x_1+e^(x+5)");
    }

    void testFormulaView(String formula) throws Exception
    {
        Parser parser = new Parser(new StringReader(formula));
        AstStart start = parser.Start();
        CompositeView view = (new FormulaViewBuilder()).createView(start, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView(view);
        assertView(pane, formula);
    }
}

