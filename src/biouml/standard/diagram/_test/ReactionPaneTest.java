package biouml.standard.diagram._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import javax.swing.JFrame;

import junit.framework.TestCase;
import ru.biosoft.graphics.editor.DefaultViewEditorHelper;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.standard.diagram.ReactionPane;


public class ReactionPaneTest extends TestCase
{
    /** Standart JUnit constructor */
    public ReactionPaneTest(String name)
    {
        super(name);

        // Setup log
        File configFile = new File( "./biouml/standard/diagram/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testReactionPanePanel() throws Exception
    {
        ViewEditorPane editorPane = new ViewEditorPane(new DefaultViewEditorHelper());
        ReactionPane reactionPane = new ReactionPane(null, null, null, editorPane);

        JFrame frame = new JFrame("Reaction pane test");
        frame.setContentPane(reactionPane);
        frame.pack();
        frame.setVisible(true);
    }
}
