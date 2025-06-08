package biouml.plugins.wdl.texteditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.plugins.wdl.parser.validator.DocumentPrototype;
import biouml.plugins.wdl.parser.validator.TypeChecker;



@SuppressWarnings ( "serial" )
class TextEditor extends JFrame implements ActionListener
{
    protected Logger log = Logger.getLogger(TextEditor.class.getName());

    // Frame
    JFrame f;

    protected Action clearLogAction = new ClearLogAction();
    protected Action validateWDL = new ValidateWDL();

    protected WDLTab wdlTab;
    TypeChecker checker;
    String fileName = "WDLscript";

    // Constructor
    TextEditor()
    {
        // Create a frame
        f = new JFrame("WDL editor");
        wdlTab = new WDLTab();
        try
        {
            // Set metal look and feel
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

            // Set theme to ocean
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        }
        catch( Exception e )
        {
        }
        // Create a menubar
        JMenuBar mb = new JMenuBar();

        // Create a menu 
        JMenu m1 = new JMenu("File");

        // Validate button
        JButton valButton = new JButton("Validate WDL");
        valButton.setPreferredSize(new Dimension(45, 30));
        valButton.addActionListener(new ValidateWDL());

        // Convert WDL to Nextflow button
        JButton convButton = new JButton("Convert WDL to Nextflow");
        convButton.setPreferredSize(new Dimension(45, 30));
        convButton.addActionListener(new ConvertWDL());

        // ClearLog button
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(new ClearLogAction());
        clearLogButton.setPreferredSize(new Dimension(45, 30));
        // Create menu items
        JMenuItem mi1 = new JMenuItem("New");
        JMenuItem mi2 = new JMenuItem("Open");
        JMenuItem mi3 = new JMenuItem("Save");

        // Add action listener
        mi1.addActionListener(this);
        mi2.addActionListener(this);
        mi3.addActionListener(this);

        m1.add(mi1);
        m1.add(mi2);
        m1.add(mi3);


        mb.add(m1);
        mb.add(valButton);
        mb.add(convButton);
        mb.add(clearLogButton);

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        f.setJMenuBar(mb);
        f.setPreferredSize(new Dimension(600, 600));
        f.setLayout(new BorderLayout());
        f.add(wdlTab, BorderLayout.CENTER);
        f.pack();
        f.setVisible(true);
    }

    // If a button is pressed
    public void actionPerformed(ActionEvent e)
    {
        String s = e.getActionCommand();

        if( s.equals("Save") )
        {
            // Create an object of JFileChooser class
            JFileChooser j = new JFileChooser("./test_examples");

            // Invoke the showsSaveDialog function to show the save dialog
            int r = j.showSaveDialog(null);

            if( r == JFileChooser.APPROVE_OPTION )
            {

                // Set the label to the path of the selected directory
                File fi = new File(j.getSelectedFile().getAbsolutePath());

                try
                {
                    // Create a file writer
                    FileWriter wr = new FileWriter(fi, false);

                    // Create buffered writer to write
                    BufferedWriter w = new BufferedWriter(wr);

                    // Write
                    w.write(wdlTab.getText());

                    w.flush();
                    w.close();
                }
                catch( Exception evt )
                {
                    JOptionPane.showMessageDialog(f, evt.getMessage());
                }
            }
        }
        else if( s.equals("Open") )
        {
            // Create an object of JFileChooser class
            JFileChooser j = new JFileChooser();
            j.setCurrentDirectory(new File("./test_examples"));

            // Invoke the showsOpenDialog function to show the save dialog
            int r = j.showOpenDialog(null);

            // If the user selects a file
            if( r == JFileChooser.APPROVE_OPTION )
            {
                // Set the label to the path of the selected directory
                File fi = new File(j.getSelectedFile().getAbsolutePath());

                try
                {
                    // String
                    String s1 = "", sl = "";

                    // File reader
                    FileReader fr = new FileReader(fi);

                    // Buffered reader
                    BufferedReader br = new BufferedReader(fr);

                    // Initialize sl
                    sl = br.readLine();

                    // Take the input from the file
                    while( ( s1 = br.readLine() ) != null )
                    {
                        sl = sl + "\n" + s1;
                    }

                    // Set the text
                    wdlTab.setWDLText(sl);
                    fileName = fi.getName();
                    fr.close();
                    br.close();
                }
                catch( Exception evt )
                {
                    JOptionPane.showMessageDialog(f, evt.getMessage());
                }
            }
        }
        else if( s.equals("New") )
        {
            try
            {
                wdlTab.setWDLText("");
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, ex.getMessage());
            }
        }
    }

    class ValidateWDL extends AbstractAction
    {
        public static final String KEY = "Apply";

        public ValidateWDL()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            WDLParser parser = new WDLParser();
            boolean wasException = false;
            try
            {

                AstStart astStart = parser.parse(new StringReader(wdlTab.getText()));
                checker = new TypeChecker(parser.getVersion());
                checker.check(astStart);
            }
            catch( Exception ex )
            {
                wasException = true;
                log.log(Level.SEVERE, "WDL parsing error: " + ex.getMessage());
            }
            finally
            {
                if( !wasException )
                    log.log(Level.FINE, "Your wdl file is valid!");
            }
        }
    }

    class ConvertWDL extends AbstractAction
    {
        public static final String KEY = "Convert";

        public ConvertWDL()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            WDLParser parser = new WDLParser();
            boolean wasException = false;
            try
            {
                AstStart astStart = parser.parse(new StringReader(wdlTab.getText()));
                checker = new TypeChecker(parser.getVersion());
                DocumentPrototype doc = checker.getPrototype(astStart);

                VelocityEngine velocityEngine = new VelocityEngine();
                velocityEngine.init();

                Template t = velocityEngine.getTemplate("vm_templates/index.vm");

                VelocityContext context = new VelocityContext();
                context.put("tasks", doc.getTasks());
                context.put("workflow", doc.getWorkflow());

                StringWriter writer = new StringWriter();
                t.merge(context, writer);
                wdlTab.setNfText(writer.toString());

            }
            catch( Exception ex )
            {
                wasException = true;
                log.log(Level.SEVERE, "WDL converting error: " + ex.getMessage());
            }
            finally
            {
                if( !wasException )
                    log.log(Level.FINE, "All is fine!");
            }
        }
    }

    class ClearLogAction extends AbstractAction
    {
        public static final String KEY = "Clear log";

        public ClearLogAction()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            wdlTab.appender.getLogTextPanel().setText("");
        }
    }

    // Main class
    public static void main(String args[])
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {

            @Override
            public void run()
            {
                TextEditor editor = new TextEditor();
            }
        });

    }
}

