package biouml.workbench.module.xml;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import com.developmentontheedge.application.Application;

import biouml.workbench.module.xml.editor.XmlModuleDialog;
import ru.biosoft.access.core.CollectionFactory;

@SuppressWarnings ( "serial" )
public class NewCompositeModuleAction extends AbstractAction
{
    public static final String KEY = "New composite database";
    protected static final Logger log = Logger.getLogger("biouml.workbench.module.xml.NewCompositeModuleAction");

    public NewCompositeModuleAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        XmlModuleDialog xmd = new XmlModuleDialog( Application.getApplicationFrame(), "New database properties",
                CollectionFactory.getDataCollection( "databases" ), null );
        if( xmd.doModal() )
        {
            String path = xmd.getModule().getPath().getAbsolutePath();
            try (FileOutputStream fos = new FileOutputStream( path + File.separator + xmd.getModule().getName() + ".xml" ))
            {
                XmlModuleWriter xmw = new XmlModuleWriter(fos);
                xmw.write(xmd.getModule());
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not create composite database", t);
            }

            xmd.getModule().initXmlModule();
        }

    }
}
