
package ru.biosoft.math._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import biouml.model._test.ViewTestCase;
import ru.biosoft.math.Expression;
import ru.biosoft.math.ExpressionEditorDialog;

public class ExpressionEditorTest extends ViewTestCase
{
    public ExpressionEditorTest(String name)
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

    public void testExpressionEditor() throws Exception
    {
        ExpressionEditorDialog editor = new ExpressionEditorDialog();
        editor.setVisible(true);
        editor.setExpression(new Expression(null, "0"));
        editor.repaint ( );
    }

    public static void main(String[] args) throws Exception
    {
        ExpressionEditorDialog editor = new ExpressionEditorDialog();
        editor.setVisible(true);
        editor.setExpression(new Expression(null, "0"));
        editor.repaint ( );
        System.in.read();
    }
}


