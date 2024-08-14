package com.developmentontheedge.print;

import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.JComponent;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PrintManager
{
    protected static final Logger log = Logger.getLogger( PrintManager.class.getName() );

    private PrintManager()
    {
    }

    public static PrintManager getPrintManager()
    {
        if(instance == null)
        {
            instance = new PrintManager();
        }
        return instance;
    }
/*
    public static void init()
    {
        Action printSetupAction = null;
        try
        {
            printSetupAction.addActionLite = Application.getActionManager().getAction(PrintSetupAction.KEY);
        }
        catch(Exception e)
        {
        }
        if(printSetupAction != null)
        {
            printSetupAction.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            PrinterJob job = PrinterJob.getPrinterJob();
                            pageFormat = job.pageDialog(pageFormat);
                        }
                    });
        }
    }
*/
    /**
     * retrun true on succes
     */
    public boolean print(JComponent comp)
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        Pageable pageable = new JPrintableComponent(comp, pageFormat);

        job.setPageable(pageable);
        try
        {
            if(job.printDialog())
            {
                job.print();
                return true;
            }
        }
        catch(PrinterException e)
        {
            log.log(Level.SEVERE, "Printing error", e);
        }
        return false;
    }

    public void preview(JComponent comp)
    {
        PreviewDialog previewDialog = new PreviewDialog(comp);
        previewDialog.doModal();
    }

    public void setPageFormat(PageFormat pageFormat)
    {
        this.pageFormat = pageFormat;
    }

    public PageFormat getPageFormat()
    {
        return pageFormat;
    }

    private PageFormat pageFormat = PrinterJob.getPrinterJob().defaultPage(new FooterFormat());
    private static PrintManager instance = null;
}
