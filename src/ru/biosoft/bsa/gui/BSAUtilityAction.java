package ru.biosoft.bsa.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.analysis.document.AnalysisDocument;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.gui.GUI;

@SuppressWarnings ( "serial" )
public class BSAUtilityAction extends AbstractAction
{
    public static final String KEY = "Utility functions";
    public static final String METHOD = "Analysis method";

    public BSAUtilityAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        AnalysisMethodInfo mi = (AnalysisMethodInfo)getValue(METHOD);
        GUI.getManager().addDocument( new AnalysisDocument(mi) );
    }
}
