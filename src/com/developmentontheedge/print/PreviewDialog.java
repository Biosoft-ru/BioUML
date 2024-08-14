package com.developmentontheedge.print;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

@SuppressWarnings ( "serial" )
public class PreviewDialog extends JDialog
{
    public PreviewDialog(JComponent comp)
    {
        super(Application.getApplicationFrame(), "Print Preview", true);
        componentToPreview = comp;
        initContent();
    }

    public void doModal()
    {
        pack();
        ApplicationUtils.moveToCenter(this);
        setVisible(true);
        dispose();
    }

    protected void initContent()
    {
        toolbar = createToolbar();
        previewPanel = createPreviewPanel();

        JPanel content = new JPanel(new BorderLayout());
        content.add(toolbar, BorderLayout.NORTH);
        content.add(previewPanel, BorderLayout.CENTER);

        setContentPane(content);
    }

    protected JComponent createToolbar()
    {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton next = new JButton("Next");
        JButton prev = new JButton("Previous");
        JButton print = new JButton("Print...");
        JButton setup = new JButton("Setup...");
        JButton close = new JButton("Close");

        next.setEnabled(false);
        prev.setEnabled(false);

        print.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    boolean succes = PrintManager.getPrintManager().print(componentToPreview);
                    if(succes)
                    {
                        setVisible(false);
                    }
                }
            });

        setup.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent evt)
                {
                    Action printSetupAction = null;
                    try
                    {
                        printSetupAction = Application.getActionManager().getAction(PrintSetupAction.KEY);
                    }
                    catch(Exception e)
                    {
                    }
                    if(printSetupAction != null)
                    {
                        printSetupAction.actionPerformed(evt);
                        initContent();
                        pack();
                        ApplicationUtils.moveToCenter(PreviewDialog.this);
                    }
                }
            });

        close.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    setVisible(false);
                }
            });

        //toolbar.add(next);
        //toolbar.add(prev);
        toolbar.add(print);
        toolbar.add(setup);
        toolbar.add(close);

        return toolbar;
    }

    protected JComponent createPreviewPanel()
    {
        PageFormat pageFormat = PrintManager.getPrintManager().getPageFormat();
        JPrintableComponent pageable = new JPrintableComponent(componentToPreview, pageFormat);
        JPanel previewPanel = new JPanel();
        GridLayout layout = new GridLayout(pageable.getNumberOfPages(), 0);
        layout.setHgap(10);
        layout.setVgap(10);
        previewPanel.setLayout(layout);
        previewPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for(int i = 0; i < pageable.getNumberOfPages(); i++)
        {
            PageFormat format = pageable.getPageFormat(i);
            PagePreview page = new PagePreview(pageable, format, i);
            previewPanel.add(page);
        }

        JScrollPane scrollPane = new JScrollPane(previewPanel);

        int width = (int)pageFormat.getWidth()+23;
        if(pageable.getNumberOfPages() > 1)
        {
            Integer scrWidth = (Integer)UIManager.get("ScrollBar.width");
            width += scrWidth.intValue();
        }
        int height = (int)pageFormat.getHeight()+23;
        scrollPane.setPreferredSize(new Dimension(width, height));

        return scrollPane;
    }

    private JComponent componentToPreview = null;
    private JComponent toolbar = null;
    private JComponent previewPanel = null;
}
