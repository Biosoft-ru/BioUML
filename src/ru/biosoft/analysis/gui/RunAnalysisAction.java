package ru.biosoft.analysis.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.ParameterException;
import ru.biosoft.analysis.document.AnalysisDocument;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.util.BeanUtil;

import com.developmentontheedge.application.ApplicationUtils;

@SuppressWarnings ( "serial" )
public class RunAnalysisAction extends AbstractAction
{
    private static final Logger log = Logger.getLogger(RunAnalysisAction.class.getName());

    public static final String KEY = "Run analysis";
    public static final String DOCUMENT = "Analysis document";

    public RunAnalysisAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        AnalysisDocument document = (AnalysisDocument)getValue(DOCUMENT);
        AnalysisParameters params = document.getAnalysisParameters();

        if( !confirmOutputNamesOverwrite(params) )
            return;

        try
        {
            document.startAnalysis();
        }
        catch( IllegalArgumentException ex )
        {
            log.log(Level.SEVERE, ex.getMessage());
            document.stopAnalysis();
        }
        catch( ParameterException ex )
        {
            log.log(Level.SEVERE, ex.getMessage());
            document.stopAnalysis();
        }
    }

    private boolean confirmOutputNamesOverwrite(AnalysisParameters params)
    {
        ru.biosoft.access.core.DataElementPath[] names = params.getExistingOutputNames();

        if( names.length > 0 )
        {
            String message;
            try
            {
                message = "The following elements already exist: \n"+BeanUtil.joinBeanProperties(names, "name", "\n")+"\nDo you want to overwrite them?";
            }
            catch( Exception e )
            {
                throw new InternalException(e);
            }
            if( !ApplicationUtils.dialogAreYouSure(message) )
                return false;
            for( Document document : GUI.getManager().getDocuments() )
            {
                if( document.getModel() instanceof DataElement
                        && StreamEx.of( names ).has( ( (DataElement)document.getModel() ).getCompletePath() ) )
                {
                    GUI.getManager().removeDocument(document);
                }
            }
            return true;
        }
        return true;
    }
}
