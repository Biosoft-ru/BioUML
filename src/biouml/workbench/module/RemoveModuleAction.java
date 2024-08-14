package biouml.workbench.module;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import biouml.model.Module;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

public class RemoveModuleAction extends AbstractAction
{
    public static final String KEY = "Remove module";
    public static final String DATABASE = "Database";
    protected static final Logger log = Logger.getLogger("biouml.workbench.module.RemoveModuleAction");


    public RemoveModuleAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Module module = (Module)getValue(DATABASE);
        String moduleName = module.getName();

        final String message = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("CONFIRM_REMOVE_DATABASE"),
                new Object[] {moduleName});
        int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message);
        if( res == JOptionPane.YES_OPTION )
        {
            try
            {
                // close all documents from this module
                closeDocumentsFromModule(module);

                DataCollection origin = module.getOrigin();

                origin.remove(moduleName);
                File dir = module.getPath();
                ApplicationUtils.removeDir(dir);

                System.gc();
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Removing the module " + moduleName + " error", ex);
            }
        }
    }

    private void closeDocumentsFromModule(Module module)
    {
        try
        {
            Document oldActiveDocument = GUI.getManager().getCurrentDocument();
            for( Document document : GUI.getManager().getDocuments() )
            {
                Object model = document.getModel();
                if( model instanceof DataElement && ( Module.optModule((DataElement)model) == module ) )
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
            log.log(Level.SEVERE, "Maybe not all diagrams from removed module '" + module.getName() + "'closed. error:", t);
        }
    }
}
