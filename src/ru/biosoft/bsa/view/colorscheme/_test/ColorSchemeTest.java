
package ru.biosoft.bsa.view.colorscheme._test;

import java.awt.Graphics;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import ru.biosoft.bsa.view.colorscheme.SiteColorScheme;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewPane;

import com.developmentontheedge.beans.swing.PropertyInspector;

import biouml.model._test.ViewTestCase;

/**
 * General class to test a SiteColorScheme.
 */
abstract public class ColorSchemeTest extends ViewTestCase
{

    /** The tested ColorScheme. */
    static SiteColorScheme colorScheme;

    protected Logger log = Logger.getLogger( ColorSchemeTest.class.getName() );

    ////////////////////////////////////////
    //
    //

    public ColorSchemeTest(String name)
    {
        super(name);

        File configFile = new File( "./ru/biosoft/bsa/view/colorscheme/_test/test.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
        log.info( "Start test: " + name );
     }


    ////////////////////////////////////////
    // Test methods
    //

    abstract public void testCreateColorScheme() throws Exception;

    public void testViewLegend() throws Exception
    {
        javax.swing.JFrame frame = new javax.swing.JFrame();
        frame.show();
        Graphics g = frame.getGraphics();
        assertNotNull("Can not get graphics", g);
//        frame.hide();

        View v = colorScheme.getLegend(g);
        assertNotNull("Generate legend view", v);

        ViewPane viewPane = new ViewPane();
        viewPane.setView( (CompositeView)v );
        assertView(viewPane, "Color scheme legend");
    }

    public void testViewModel() throws Exception
    {
        PropertyInspector inspector  = new PropertyInspector();
        inspector.explore(colorScheme);
        inspector.expand(2);
        assertView(inspector, "site color scheme");
    }
}



