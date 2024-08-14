package com.developmentontheedge.print;

import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;

import javax.swing.AbstractAction;

@SuppressWarnings ( "serial" )
public class PrintSetupAction extends AbstractAction
{
    public static final String KEY = "Print setup";

    public PrintSetupAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pageFormat = job.pageDialog(PrintManager.getPrintManager().getPageFormat());
        PrintManager.getPrintManager().setPageFormat(pageFormat);
    }
}
