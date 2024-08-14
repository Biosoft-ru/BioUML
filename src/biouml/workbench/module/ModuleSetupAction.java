package biouml.workbench.module;

import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.model.util.ModulePackager;
import biouml.workbench.BioUMLApplication;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class ModuleSetupAction extends AbstractAction
{
    public static final String KEY = "Module Setup";
    public static final String PREFERENCES_IMPORT_DIRECTORY = "moduleSetup.importDirectory";

    protected static final Logger log = Logger.getLogger( ModuleSetupAction.class.getName() );

    public ModuleSetupAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_IMPORT_DIRECTORY;
        String importDirectory = Application.getPreferences().getStringValue(key, ".");

        JFileChooser chooser = new JFileChooser(new File(importDirectory));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new BMDFileFilter());

        int res = chooser.showOpenDialog(Application.getApplicationFrame());
        if (res == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();

            // save preferences
            importDirectory = chooser.getCurrentDirectory().getAbsolutePath();
            savePreferences(importDirectory);

            JarFile moduleFile = null;
            try
            {
                moduleFile = new JarFile(file);
            }
            catch (IOException ioe)
            {
                log.log(Level.SEVERE, "Error at creating jar file from " + file.getName(), ioe);
                return;
            }

            if (moduleFile != null)
            {
                boolean showInfoDialog = true;
                String moduleName = ModulePackager.getModuleAttribute(moduleFile, ModulePackager.MF_DATABASE_NAME);
                String moduleParent = ModulePackager.getModuleAttribute( moduleFile, ModulePackager.MF_DATABASE_PARENT );
                DataCollection<?> dc = DataElementPath.create( moduleParent ).getDataCollection( DataCollection.class );
                Repository repository = (Repository)dc;
                File moduleDir = ModulePackager.getModuleDir( repository.getCompletePath(), moduleName );
                File infoFile = new File(moduleDir, ModulePackager.INFO_FILENAME);
                if (infoFile.exists() && infoFile.isFile())
                {
                    Manifest existedVersionManifest = null;
                    Manifest newVersionManifest = null;
                    try
                    {
                        BufferedInputStream in = new BufferedInputStream(new FileInputStream(infoFile));
                        existedVersionManifest = new Manifest(in);
                        newVersionManifest = moduleFile.getManifest();
                    }
                    catch (IOException ex)
                    {
                        log.log(Level.SEVERE, "Reading " + infoFile.getPath() + " error", ex);
                    }

                    // check whether such module already exists
                    if( existedVersionManifest != null && newVersionManifest != null )
                    {
                        String title = BioUMLApplication.getMessageBundle().getResourceString("DATABASE_REPLACE_DIALOG_TITLE");
                        ModuleReplaceDialog moduleReplaceDialog = new ModuleReplaceDialog(Application.getApplicationFrame(), title, existedVersionManifest, newVersionManifest);
                        if( !moduleReplaceDialog.doModal() )
                            return;
                        else
                            showInfoDialog = false;
                    }
                }

                if( showInfoDialog )
                {
                    String title = BioUMLApplication.getMessageBundle().getResourceString("DATABASE_INFO_DIALOG_TITLE");
                    ModuleInfoDialog moduleInfoDialog = new ModuleInfoDialog(Application.getApplicationFrame(), title, moduleFile);
                    if( !moduleInfoDialog.doModal() )
                        return;
                }
                ModulesActivatingDialog.importModule( moduleFile, repository );
            }
        }
    }

    protected void savePreferences(String importDirectory)
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_IMPORT_DIRECTORY;
        if( Application.getPreferences().getProperty(key) != null )
            Application.getPreferences().setValue(key, importDirectory);
        else
        {
            try
            {
                biouml.workbench.resources.MessageBundle messageBundle = BioUMLApplication.getMessageBundle();
                Preferences preferences = Application.getPreferences().getPreferencesValue(Preferences.DIALOGS);
                preferences.add(new DynamicProperty(PREFERENCES_IMPORT_DIRECTORY,
                                        messageBundle.getResourceString("DATABASE_IMPORT_DIRECTORY_PN"),
                                        messageBundle.getResourceString("DATABASE_IMPORT_DIRECTORY_PD"),
                                        String.class, importDirectory));
            }
            catch(Exception ex)
            {
                log.log(Level.SEVERE, "Error saving module directory '" + importDirectory + "' in preferences ", ex);
            }
        }
    }
}
