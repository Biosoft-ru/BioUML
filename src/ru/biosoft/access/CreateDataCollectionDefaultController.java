package ru.biosoft.access;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Default implementation of creating data collection.
 * Show dialog if some files of data collection already exists.
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */
public class CreateDataCollectionDefaultController
    extends JDialog
    implements CreateDataCollectionController
{
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel pButtons = new JPanel();
    FlowLayout flowLayout1 = new FlowLayout();
    JButton bCancel = new JButton();
    JButton bYes    = new JButton();
    JButton bAll    = new JButton();
    JLabel lMessage = new JLabel();

    protected static final Logger log = Logger.getLogger( CreateDataCollectionDefaultController.class.getName() );

    /**
     * @todo Document it
     */
    public CreateDataCollectionDefaultController( JFrame owner )
    {
        super( owner,true );
        try
        {
            jbInit();
            validate();
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE, "Error at init of data collection default controller", e);
        }
    }

    /**
     * @todo Document it
     */
    public CreateDataCollectionDefaultController( JDialog owner )
    {
        super( owner,true );
        try
        {
            jbInit();
            validate();
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE, "Error at init of data collection default controller", e);
        }
    }

    protected int returnValue = OVERWRITE_ALL;
    @Override
    public int getLastAnswer()
    {
        return returnValue;
    }

    /**
     * @todo  Document it
     */
    private boolean bFirst = true;
    @Override
    public int fileAlreadyExists( File file )
    {
        if(bFirst || returnValue != OVERWRITE_ALL)
        {
            lMessage.setText("<HTML>File already exist. Do you want overwrite it?<BR> "+file+"</HTML>");
            show();
            bFirst = false;
        }
        return returnValue;
    }

    FunctionJobControl jobControl;
    @Override
    public void setJobControl( FunctionJobControl jc )
    {
        jobControl = jc;
    }
    @Override
    public FunctionJobControl getJobControl()
    {
        return jobControl;
    }

    private void jbInit() throws Exception
    {
        this.getContentPane().setLayout(borderLayout1);
        bCancel.setText("Cancel");
        bCancel.addActionListener(new java.awt.event.ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                bCancel_actionPerformed(e);
            }
        });
        pButtons.setLayout(flowLayout1);
        flowLayout1.setAlignment(FlowLayout.RIGHT);
        bYes.setPreferredSize(new Dimension(73, 27));
        bYes.setText("Yes");
        bYes.addActionListener(new java.awt.event.ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                bYes_actionPerformed(e);
            }
        });
        bAll.setMinimumSize(new Dimension(27, 27));
        bAll.setPreferredSize(new Dimension(73, 27));
        bAll.setText("All");
        bAll.addActionListener(new java.awt.event.ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                bAll_actionPerformed(e);
            }
        });
        lMessage.setBorder(BorderFactory.createEtchedBorder());
        lMessage.setMaximumSize(new Dimension(400, 17));
        lMessage.setMinimumSize(new Dimension(200, 17));
        lMessage.setOpaque(true);
        lMessage.setPreferredSize(new Dimension(200, 17));
        lMessage.setHorizontalAlignment(SwingConstants.CENTER);
        lMessage.setText("Custom");
        this.setModal(true);
        this.setTitle("Question");
        this.getContentPane().add(pButtons, BorderLayout.SOUTH);
        pButtons.add(bAll, null);
        pButtons.add(bYes, null);
        pButtons.add(bCancel, null);
        this.getContentPane().add(lMessage, BorderLayout.CENTER);
        this.setSize( 300,200);
//        ApplicationUtils.moveToCenter( this );
    }

    void bAll_actionPerformed(ActionEvent e)
    {
        returnValue = OVERWRITE_ALL;
        hide();
    }

    void bYes_actionPerformed(ActionEvent e)
    {
        returnValue = OVERWRITE_ONE;
        hide();
    }

    /**
     * Handler of 'Cancel' button.
     * @param e Component specific action.
     */
    void bCancel_actionPerformed(ActionEvent e)
    {
        returnValue = CANCEL;
        hide();
    }
}