package ru.biosoft.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;


/**
 * Utility class for dialogs with "Ok" and "Cancel" buttons
 *
 * @pending use MessageBundle.
 */
public class OkCancelDialog extends JDialog
{
    public static final int SUCCESS = 0;
    public static final int ERROR = -1;

    /** Result of dialog executing */
    protected boolean result = false;

    private   Component parent;
    private   JPanel  mainPanel;
    protected JPanel  buttonPanel;
    protected JButton okButton;
    protected JButton cancelButton;


    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    /**
     * Constructs dialog with parent component, title and user component.
     *
     * @param dialog a <code>JDialog</code> from which the dialog is displayed.
     * @param title  a dialog title
     * @param userPane user Component in work area of OkCancelDialog.
     */
    public OkCancelDialog(JDialog dialog, String title, Component userPane)
    {
        super(dialog, title, true);
        parent = dialog;
        init(userPane);
    }

    public OkCancelDialog(Component parent, String title)
    {
        this(parent, title, null, true);
    }

    public OkCancelDialog(Component parent, String title, boolean modal)
    {
        this(parent, title, null, modal);
    }

    /**
     * Constructs dialog with parent frame, title and user component.
     *
     * @param parent determines the Frame in which the dialog is displayed.
     *               If null, or if the parent has no Frame, a default Frame is used.
     * @param title  a dialog title
     * @param userPane user component in work area of OkCancelDialog
     */
    public OkCancelDialog(Component parent, String title, Component userPane, boolean modal)
    {
        super(JOptionPane.getFrameForComponent(parent), title, modal);
        this.parent = parent;
        init(userPane);
    }

    public OkCancelDialog(Component parent, String title, Component userPane, String cancelText, String okText)
    {
        this(parent, title, userPane, true);

        if (okText == null)
            okButton.setVisible(false);
        else
        {
            okButton.setText(okText);
            okButton.setDefaultCapable(true);
            getRootPane().setDefaultButton(okButton);
        }

        if (cancelText == null)
            cancelButton.setVisible(false);
        else
            cancelButton.setText(cancelText);
    }

    ////////////////////////////////////////////////////////////////////

    private void init(Component userPane)
    {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        contentPane.add(mainPanel, BorderLayout.CENTER);

        // init buttons
        okButton = new JButton("Ok");
        okButton.setDefaultCapable(true);
        cancelButton = new JButton("Cancel");

        okButton.setPreferredSize(cancelButton.getPreferredSize());

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // init user pane
        if (userPane != null)
            setContent(userPane);
        getRootPane().setDefaultButton(okButton);

        // init listeners
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent ke)
            {
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE)
                        cancelPressed();
            }
        });

        cancelButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                cancelPressed();
            }
        });

        okButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                okPressed();
            }
        });

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (cancelButton.isEnabled())
                    cancelPressed();
            }
        });
    }

    /**
     * Sets user Component in work area.
     *
     * @param comp user <code>Component</code>
     */
    public void setContent(Component comp)
    {
        mainPanel.add(comp, BorderLayout.CENTER);
    }

    /**
     * Displays modal dialog on screen.
     *
     * @return true if "Ok" button is pressed
     */
    public boolean doModal()
    {
        pack();
        setLocationRelativeTo(parent);
        show();
        dispose();
        return result;
    }

    /** "Ok" button is pressed.<br> Override this method when specific processing is needed for "Ok" button. */
    protected void okPressed()
    {
        result = true;
        hide();
    }

    /** "Cancel" button is pressed.<br> Override this method when specific processing is needed for "Cancel" button. */
    protected void cancelPressed()
    {
        result = false;
        hide();
    }

    /**
     * "Close window" button of title bar is pressed.<br> Override this method when specific
     * processing is needed for "Close window" button of title bar.
     */
    protected void exited()
    {
        result = false;
        hide();
    }

    public JButton getCancelButton()
    {
        return cancelButton;
    }

    public JButton getOkButton()
    {
        return okButton;
    }
}
