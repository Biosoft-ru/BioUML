package biouml.workbench.project;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.GenericFileDataCollection;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

public class RemoveProjectAction extends AbstractAction
{
    public static final String KEY = "Remove project";
    public static final String PROJECT = "Project";
    protected static final Logger log = Logger.getLogger( "biouml.workbench.project.RemoveProjectAction" );


    public RemoveProjectAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataCollection project = (DataCollection) getValue( PROJECT );
        String projectName = project.getName();

        final String message = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("CONFIRM_REMOVE_DATABASE"),
                new Object[] { projectName } );
        int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message);
        if( res == JOptionPane.YES_OPTION )
        {
            try
            {
                project = DataCollectionUtils.fetchPrimaryCollectionPrivileged( project );
                String localPath = project.getInfo().getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
                if( localPath == null && project instanceof GenericFileDataCollection )
                {
                    localPath = project.getInfo().getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
                }

                // close all documents from this module
                closeDocumentsFromProject( project );

                DataCollection origin = project.getOrigin();

                origin.remove( projectName );
                if( localPath != null )
                {
                    File file = new File( localPath );
                    ApplicationUtils.removeDir( file );
                }

                System.gc();
            }
            catch( Exception ex )
            {
                log.log( Level.SEVERE, "Removing the module " + projectName + " error", ex );
            }
        }
    }

    private void closeDocumentsFromProject(DataCollection project)
    {
        DataElementPath projectPath = project.getCompletePath();
        try
        {
            Document oldActiveDocument = GUI.getManager().getCurrentDocument();
            for( Document document : GUI.getManager().getDocuments() )
            {
                Object model = document.getModel();
                if( model instanceof DataElement && (((DataElement) model).getCompletePath().isDescendantOf( projectPath )) )
                {
                    if( document == oldActiveDocument )
                    {
                        oldActiveDocument = null;
                    }
                    GUI.getManager().removeDocument(document);
                }
            }
            if( oldActiveDocument != null )
                GUI.getManager().addDocument(oldActiveDocument);
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Maybe not all diagrams from removed module '" + project.getName() + "'closed. error:", t );
        }
    }
}
