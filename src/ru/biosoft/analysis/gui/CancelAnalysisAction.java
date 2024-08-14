package ru.biosoft.analysis.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.analysis.document.AnalysisDocument;

public class CancelAnalysisAction extends AbstractAction
{
    public static final String KEY = "Cancel";
    public static final String DOCUMENT = "Analysis document";

    public CancelAnalysisAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        AnalysisDocument document = (AnalysisDocument)getValue(DOCUMENT);
        document.stopAnalysis();
    }
}
