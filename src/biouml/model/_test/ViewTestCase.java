
package biouml.model._test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ru.biosoft.access._test.AbstractBioUMLTest;

public abstract class ViewTestCase extends AbstractBioUMLTest
{
    /**
    * Dialog for Test (see below)
    */
    private static TestDialog testDialog = new TestDialog();

    /**
    * flag - holds until user press ok, error or closes dialog
    */
    private static Object flag = new Object();

    /**
    * Constructor for ViewTestCase
    * @param Name of function to test
    */
    public ViewTestCase(String name)
    {
        super(name);
    }

    public static Graphics getGraphics()
    {
        return testDialog.getContentPane().getGraphics();
    }

    /**
    * assert function
    * @param view : item to show
    * @param description : text below
    */
    public static void assertView(JComponent view, String description)
    {
        int result = testDialog.showModal(view, description);
        assertTrue("Error in view", result == JOptionPane.OK_OPTION);
        testDialog = new TestDialog(); //view, description);
    }

    /**
    * class TestDialog
    */
    @SuppressWarnings ( "serial" )
    static class TestDialog extends JFrame
    {
        /**
        * Buttons
        */
        private JButton okButton = new JButton("OK");
        private JButton errorButton = new JButton("Error");
        /**
        * Panel for buttons
        */
        private JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.CENTER));
        /**
        * Result value
        */
        private int result = JOptionPane.CANCEL_OPTION;

        /**
        * Constructor for TestDialog
        * @param view : item to show
        * @param description : text below
        */
        TestDialog()
        {
            super("View Test");
            this.addWindowListener(
                new WindowAdapter()
                {
                    /** Invoked when a window has been closed as the result of calling dispose on the window. */
                    @Override
                    public void windowClosing(WindowEvent e)
                    {
                        synchronized(flag)
                        {
                            flag.notify();
                        }
                    }
                });
            // OK button listener
            okButton.addActionListener(
                new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        //set result value to OK
                        result = JOptionPane.OK_OPTION;
                        dispose();
                        synchronized(flag)
                        {
                            // Drop flag
                            flag.notify();
                        }
                    }
                });
            // Error button listener
            errorButton.addActionListener(
                new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        //set result value to CANCEL
                        result = JOptionPane.CANCEL_OPTION;
                        dispose();
                        synchronized(flag)
                        {
                            // Drop flag
                            flag.notify();
                        }
                    }
                });
            //JPanel buttonPanel1 = new JPanel(new BorderLayout());
            //buttonPanel1.add(buttonPanel, BorderLayout.CENTER);
            // Adding buttons to panel
            buttonPanel.add(okButton);
            buttonPanel.add(errorButton);

            show();
        }

        /**
        * ShowModal function
        */
        protected int showModal(JComponent view, String description)
        {
            //Make container for all elements
            Container content = new Container();
            content.setLayout(new BorderLayout());
            //View area
            JScrollPane compView = new JScrollPane(view);
            //Text area
            JTextArea textArea = new JTextArea(description);
            textArea.setEditable(false);
            //ScrollPane for TextArea
            JScrollPane descView = new JScrollPane(textArea);

            //add all of it to content
            content.add(compView, BorderLayout.CENTER);
            content.add(descView, BorderLayout.NORTH);
            content.add(buttonPanel, BorderLayout.SOUTH);

            setContentPane(content);
            pack();
            show();
            result = JOptionPane.CLOSED_OPTION;
            setSize(getPreferredSize());
            okButton.setPreferredSize(errorButton.getPreferredSize());
            // show dialog
            show();
            try
            {
                synchronized(flag)
                {
                    // wait for flag drops
                    flag.wait();
                }
            }
            catch (Exception e)
            {
                // if interrupted from outside
                System.out.println("Exception=" + e);
            }
            // returns OK, if ok button pressed and CANCEL overwise
            return result;
        }
    }
}
