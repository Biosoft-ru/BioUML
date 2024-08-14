package biouml.workbench.module.xml;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.Application;

import biouml.workbench.module.xml.editor.XmlModuleDialog;
import biouml.workbench.BioUMLApplication;

public class EditModuleAction extends AbstractAction
{
    public static final String KEY = "Edit module";
    public static final String DATABASE = "Module";
    protected static final Logger log = Logger.getLogger("biouml.workbench.module.xml.EditModuleAction");

    public EditModuleAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        XmlModule module = (XmlModule)getValue(DATABASE);
        module.initXmlModule();

        BioUMLApplication app = (BioUMLApplication)Application.getApplicationFrame();
        XmlModuleDialog xmd = new XmlModuleDialog(app, "Edit module properties", module.getOrigin(), module);
        if( xmd.doModal() )
        {
            String path = module.getPath().getAbsolutePath();
            try (FileOutputStream fos = new FileOutputStream( path + File.separator + module.getName() + ".xml" ))
            {
                XmlModuleWriter xmw = new XmlModuleWriter(fos);
                xmw.write(module);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not save changes", t);
            }

            module.initXmlModule();
        }
    }
}