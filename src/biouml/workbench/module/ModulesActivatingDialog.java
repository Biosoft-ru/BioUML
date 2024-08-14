package biouml.workbench.module;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import com.developmentontheedge.beans.Preferences;

import biouml.model.util.ModulePackager;
import biouml.workbench.BioUMLApplication;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.CreateDataCollectionController;
import ru.biosoft.access.CreateDataCollectionDefaultController;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.jobcontrol.FunctionJobControl;

@SuppressWarnings ( "serial" )
public class ModulesActivatingDialog extends OkCancelDialog
{
    private static final String DEFAULT_DATABASE_DISTRIBS_DIR = "./module distributives";
    protected static final Logger log = Logger.getLogger( ModulesActivatingDialog.class.getName() );

    public static void checkNewModules()
    {
        File rootModuleDistribsDir = new File(DEFAULT_DATABASE_DISTRIBS_DIR);
        if (!rootModuleDistribsDir.exists())
        {
            rootModuleDistribsDir.mkdir();
        }

        FileFilter bmdFilesFilter = pathname -> (!pathname.isDirectory() && pathname.getName().endsWith(".bmd"));

        File[] bmdFiles = rootModuleDistribsDir.listFiles(bmdFilesFilter);
        Set<JarFile> notActiveModules = new HashSet<>();
        for( File bmdFile : bmdFiles )
        {
            String moduleName = null;
            JarFile moduleFile = null;
            try
            {
                moduleFile = new JarFile(bmdFile);
            }
            catch (IOException ioe)
            {
                log.log(Level.SEVERE, "Error at creating jar file from " + bmdFile.getName(), ioe);
                continue;
            }
            moduleName = ModulePackager.getModuleAttribute(moduleFile, ModulePackager.MF_DATABASE_NAME);
            if(moduleName != null)
            {
                DataCollection<DataCollection<?>> root = CollectionFactoryUtils.getDatabases();
                try
                {
                    DataCollection<?> dc = root.get(moduleName);
                    if(dc == null)
                    {
                        notActiveModules.add(moduleFile);
                    }
                }
                catch( Exception e )
                {
                }
            }
        } // for

        if (notActiveModules.size() > 0)
        {
            ModulesActivatingDialog dialog = new ModulesActivatingDialog(Application.getApplicationFrame(), notActiveModules);
            if (dialog.doModal())
            {
                Set<JarFile> modulesToActivate = dialog.getModulesToActivate();
                for(JarFile moduleFile : modulesToActivate)
                {
                    importModule(moduleFile);
                }
            }
        }
    }

    public static void importModule(final JarFile moduleFile)
    {
        Repository repository = (Repository)CollectionFactoryUtils.getDatabases();
        importModule( moduleFile, repository );
    }

    public static void importModule(final JarFile moduleFile, Repository repository)
    {
        final Logger cat = Logger.getLogger(ModulePackager.class.getName());
        final FunctionJobControl jobControl = new FunctionJobControl(null);
        final String importingStr = BioUMLApplication.getMessageBundle().getResourceString("IMPORTING");
        final StatusInfoDialog infoDialog = new StatusInfoDialog (
            Application.getApplicationFrame(), importingStr, cat, jobControl);

        Thread thread = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        infoDialog.setInfo(importingStr + "...");
                        CreateDataCollectionController controller = new CreateDataCollectionDefaultController(infoDialog)
                        {
                            @Override
                            public int fileAlreadyExists(File file)
                            {
                                return OVERWRITE_ALL;
                            }
                        };

                        controller.setJobControl(jobControl);
                        ModulePackager.importModule(infoDialog, moduleFile, repository, controller);
                    }
                    catch(Throwable t)
                    {
                        cat.log(Level.SEVERE, t.getMessage());
                        infoDialog.fails();
                        return;
                    }
                    infoDialog.success();
                }
            };

        infoDialog.startProcess(thread);
    }

    public ModulesActivatingDialog(JDialog dialog, Set<JarFile> notActiveModules)
    {

        super(dialog, BioUMLApplication.getMessageBundle().getResourceString("DATABASES_ACTIVATING_DIALOG_TITLE"));
        init(notActiveModules);
    }

    public ModulesActivatingDialog(JFrame frame, Set<JarFile> notActiveModules)
    {
        super(frame, BioUMLApplication.getMessageBundle().getResourceString("DATABASES_ACTIVATING_DIALOG_TITLE"));
        init(notActiveModules);
    }

    public Set<JarFile> getModulesToActivate()
    {
        return StreamEx.ofValues(mapping, JCheckBox::isSelected).toSet();
    }

    private HashMap<JCheckBox, JarFile> mapping = new HashMap<>();

    private void init(Set<JarFile> notActiveModules)
    {
        JPanel content = new JPanel(
            new BorderLayout());

        JPanel upperPanel = new JPanel(new GridLayout(2, 0));
        upperPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        String upperLabel1Text = BioUMLApplication.getMessageBundle().getResourceString("DATABASES_ACTIVATING_DIALOG_NOT_ACTIVE_DATABASES_DETECTED");
        JLabel upperLabel1 = new JLabel(upperLabel1Text);
        String upperLabel2Text = BioUMLApplication.getMessageBundle().getResourceString("DATABASES_ACTIVATING_DIALOG_SELECT_DATABASES");
        JLabel upperLabel2 = new JLabel(upperLabel2Text);
        upperPanel.add(upperLabel1);
        upperPanel.add(upperLabel2);
        content.add(upperPanel, BorderLayout.NORTH);
        String notShowAgainCBText = BioUMLApplication.getMessageBundle().getResourceString("DO_NOT_SHOW_DIALOG_AGAIN");
        final JCheckBox notShowAgainCB = new JCheckBox(notShowAgainCBText);
        notShowAgainCB.setBorder(new EmptyBorder(5, 5, 5, 5));
        content.add(notShowAgainCB, BorderLayout.SOUTH);

        notShowAgainCB.addActionListener(
            e -> {
                Preferences preferences = Application.getPreferences();
                preferences.setValue("checkModulesOnStartup", !notShowAgainCB.isSelected() );
            });

        JPanel centerPanel = new JPanel(
            new GridBagLayout());

        int y = 0;
        for(JarFile jarFile : notActiveModules)
        {
            JCheckBox checkBox = new JCheckBox();
            JPanel infoPanel = createModuleInfoPanel(jarFile);
            mapping.put(checkBox, jarFile);

            centerPanel.add(checkBox,
                            new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                            new Insets(5, 5, 5, 0), 0, 0));
            centerPanel.add(infoPanel,
                            new GridBagConstraints(1, y, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                            new Insets(5, 0, 5, 5), 0, 0));
            y++;
        }

        content.add(
            new JScrollPane(centerPanel), BorderLayout.CENTER);

        setContent(content);
        okButton.setText(BioUMLApplication.getMessageBundle().getResourceString("BUTTON_CONTINUE"));
        okButton.setPreferredSize(null);
        cancelButton.setPreferredSize(okButton.getPreferredSize());
    }

    @Override
    public boolean doModal()
    {
        Preferences preferences = Application.getPreferences();
        boolean checkModulesOnStartup = preferences.getBooleanValue("checkModulesOnStartup", true);
        return checkModulesOnStartup ? super.doModal() : false;
    }

    protected static JPanel createModuleInfoPanel(JarFile jarFile)
    {
        JPanel panel = new JPanel(new GridLayout(3, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                        new EmptyBorder(5, 5, 5, 5)));

        String moduleName = ModulePackager.getModuleAttribute(jarFile, ModulePackager.MF_DATABASE_NAME);
        String moduleVersion = ModulePackager.getModuleAttribute(jarFile, ModulePackager.MF_DATABASE_VERSION);
        String moduleDescription = ModulePackager.getModuleAttribute(jarFile, ModulePackager.MF_DATABASE_DESCRIPTION);

        panel.add(new JLabel(moduleName));
        panel.add(new JLabel(moduleVersion));
        panel.add(new JLabel(moduleDescription));

        return panel;
    }
}
