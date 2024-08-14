
package biouml.workbench.module;

import java.io.File;
import java.text.MessageFormat;
import java.util.jar.JarFile;

import javax.swing.filechooser.FileFilter;

import biouml.model.util.ModulePackager;
import biouml.workbench.BioUMLApplication;

public class BMDFileFilter extends FileFilter
{
    public static final String BMD_EXTENTION = ".bmd";
    public static final String ZIP_EXTENTION = ".zip";
    public static final String JAR_EXTENTION = ".jar";

    private static final String[] exts = new String[] {BMD_EXTENTION, ZIP_EXTENTION, JAR_EXTENTION};
    @Override
    public boolean accept(File f)
    {
        if (f.isDirectory())
        {
            return true;
        }
        else if (f.isFile())
        {
            boolean accept = false;
            for( String ext : exts )
            {
                if (f.getName().toLowerCase().endsWith(ext))
                {
                    try
                    {
                        JarFile jarFile = new JarFile(f);
                        if (ModulePackager.getModuleAttribute(jarFile, ModulePackager.MF_DATABASE_NAME) != null)
                        {
                            accept = true;
                            break;
                        }
                    }
                    catch ( Exception e )
                    {
                        continue;
                    }
                }
            }
            return accept;
        }
        return false;
    }

    @Override
    public String getDescription()
    {
        return MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("BMD_FILE_DESCRIPTION"), (Object[])exts );
    }
}
