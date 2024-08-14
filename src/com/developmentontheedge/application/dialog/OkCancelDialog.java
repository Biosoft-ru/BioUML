package com.developmentontheedge.application.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.ApplicationUtils;

/**
 *  Use this class for dialog with "Ok" and "Cancel" buttons
 */
@SuppressWarnings ( "serial" )
public class OkCancelDialog extends JDialog
{
    public static final int SUCCESS = 0;
    public static final int ERROR = -1;

    /**Result of dialog executing */
    protected boolean result = false;

    protected JPanel mainPanel;
    protected JPanel buttonPanel;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    /**
     * Constructs dialog with parent frame,title and user
     * component
     *
     * @param frame the <code>JFrame</code> from which the dialog is displayed
     * @param title  the <code>String</code> to display in the dialog's
     *          title bar
     */
    public OkCancelDialog(JFrame frame ,String title)
    {
        this(frame, title, null, true);
    }

    public OkCancelDialog(JFrame frame ,String title, boolean modal)
    {
        this(frame, title, null, modal);
    }

    public OkCancelDialog(JDialog dialog, String title)
    {
        this(dialog, title, null);
    }

    public OkCancelDialog(JFrame frame ,String title, Component userPane,String cancelText, String  okText)
    {
        this(frame, title, userPane);

        if (okText!=null)
        {
            okButton.setText(okText);
            okButton.setDefaultCapable(true);
            getRootPane().setDefaultButton( okButton );
            okButton.setPreferredSize(null);
        } else
            okButton.setVisible(false);

        if (cancelText!=null)
            cancelButton.setText(cancelText);
        else
            cancelButton.setVisible(false);
    }

    public OkCancelDialog(JFrame frame ,String title, Component userPane)
    {
        this(frame, title, userPane, true);
    }

    /**
     * Constructs dialog with parent dialog, title and user
     * component
     *
     * @param dialog the <code>JDialog</code> from which the dialog is displayed
     * @param title  the <code>String</code> to display in the dialog's
     *          title bar
     * @param userPane user Component in work area of OkCancelDialog
     */
    public OkCancelDialog(JDialog dialog, String title, Component userPane)
    {
        super(dialog, title, true);
        init( userPane );
    }


    /**
     * Constructs dialog with parent frame,title and user
     * component
     *
     * @param frame the <code>JFrame</code> from which the dialog is displayed
     * @param title  the <code>String</code> to display in the dialog's
     *          title bar
     * @param userPane user Component in work area of OkCancelDialog
     */
    public OkCancelDialog(JFrame frame ,String title, Component userPane, boolean modal)
    {
        super(frame, title, modal);
        init( userPane );
    }

    private void init( Component userPane )
    {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        //mainPanel.setBorder(  BorderFactory.createRaisedBevelBorder() );

        contentPane.add(mainPanel, BorderLayout.CENTER );


        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        okButton     = new JButton("Ok");
        okButton.setDefaultCapable(true);
        cancelButton = new JButton("Cancel");

        okButton.setPreferredSize(cancelButton.getPreferredSize());

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        buttonPanel.setBorder( new EmptyBorder(5,5,5,5) );
        // new CompoundBorder( new EtchedBorder(EtchedBorder.RAISED), new EmptyBorder(5, 5, 5, 5)));
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        if (userPane != null)
            setContent(userPane);

        getRootPane().setDefaultButton( okButton );
        addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyPressed( KeyEvent ke )
            {
                if( ke.getKeyCode()==KeyEvent.VK_ESCAPE )
                    cancelPressed();
            }
        });

        cancelButton.addActionListener(e -> cancelPressed());

        okButton.addActionListener(e -> okPressed());

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if ( cancelButton.isEnabled() )
                    //exited();
                    cancelPressed();
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    protected JButton okButton;
    public JButton getOkButton()
    {
        return okButton;
    }

    protected JButton cancelButton;
    public JButton getCancelButton()
    {
        return cancelButton;
    }

    /**
     * Sets user Component in work area.
     *
     * @param comp user <code>Component</code>
     */
    public void setContent(Component comp)
    {
        mainPanel.add(comp,BorderLayout.CENTER);
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Displays modal dialog on screen.
     *
     * @return true if "Ok" button is pressed
     */
    public boolean doModal()
    {
        pack();
        ApplicationUtils.moveToCenter(this);
        show();
        dispose();
        return result;
    }

    /**
     * "Ok" button is pressed.<br>
     * Override this method when specific
     * processing is needed for "Ok" button.
     */
    protected void okPressed()
    {
        result = true;
        dispose();
    }

    /**
     * "Cancel" button is pressed.<br>
     * Override this method when specific
     * processing is needed for "Cancel" button.
     */
    protected void cancelPressed()
    {
        result = false;
        dispose();
    }

    /**
     * "Close window" button of title bar is pressed.<br>
     * Override this method when specific
     * processing is needed for "Close window" button of title bar.
     */
    protected void exited()
    {
        result = false;
        dispose();
    }
}
