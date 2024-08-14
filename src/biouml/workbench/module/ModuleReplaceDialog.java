package biouml.workbench.module;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import biouml.model.util.ModulePackager;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.application.dialog.OkCancelDialog;

public class ModuleReplaceDialog extends OkCancelDialog
{
    protected JPanel content;

    public ModuleReplaceDialog(JDialog dialog, String title, Manifest existedVersionManifest, Manifest newVersionManifest)
    {
        super(dialog, title);
        init(existedVersionManifest, newVersionManifest);
    }

    public ModuleReplaceDialog(JFrame frame, String title, Manifest existedVersionManifest, Manifest newVersionManifest)
    {
        super(frame, title);
        init(existedVersionManifest, newVersionManifest);
    }

    private void init(Manifest existedVersionManifest, Manifest newVersionManifest)
    {
        Attributes existedVersionAttributes = existedVersionManifest.getMainAttributes();
        Attributes newVersionAttributes = newVersionManifest.getMainAttributes();

        String existedModuleName = existedVersionAttributes.getValue(ModulePackager.MF_DATABASE_NAME);
        String message1 = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("MESSAGE_DATABASE_EXIST"),
                new Object[]{existedModuleName});
        String message2 = BioUMLApplication.getMessageBundle().getResourceString("REPLACE_EXISTED_VERSION");

        content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        String existedVersionText = BioUMLApplication.getMessageBundle().getResourceString("EXISTED_VERSION");
        String newVersionText = BioUMLApplication.getMessageBundle().getResourceString("NEW_VERSION");
        JPanel existedModuleInfoPanel = createModuleInfoPanel(existedVersionAttributes, " " + existedVersionText + " ");
        JPanel newModuleInfoPanel = createModuleInfoPanel(newVersionAttributes, " " + newVersionText + " ");

        content.add(new JLabel(message1),
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(0, 0, 0, 0), 0, 0));
        content.add(new JLabel(message2),
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(0, 0, 10, 0), 0, 0));

        content.add(existedModuleInfoPanel,
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(0, 0, 0, 0), 0, 0));
        content.add(newModuleInfoPanel,
                new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(0, 0, 0, 0), 0, 0));

        setContent(content);

        okButton.setText(BioUMLApplication.getMessageBundle().getResourceString("BUTTON_YES"));
        cancelButton.setText(BioUMLApplication.getMessageBundle().getResourceString("BUTTON_CANCEL"));
    }

    private JPanel createModuleInfoPanel(Attributes attributes, String title)
    {
        String moduleName = attributes.getValue(ModulePackager.MF_DATABASE_NAME);
        String moduleVersion = attributes.getValue(ModulePackager.MF_DATABASE_VERSION);
        String moduleDesc = attributes.getValue(ModulePackager.MF_DATABASE_DESCRIPTION);

        JPanel moduleInfoPanel = new JPanel(new GridBagLayout());
        moduleInfoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title));
        moduleInfoPanel.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("NAME")),
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(0, 0, 0, 0), 0, 0));
        moduleInfoPanel.add(new JLabel(moduleName),
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.BOTH,
                                       new Insets(00, 10, 0, 0), 0, 0));
        moduleInfoPanel.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("VERSION")),
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(10, 0, 0, 0), 0, 0));
        moduleInfoPanel.add(new JLabel(moduleVersion),
                new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.BOTH,
                                       new Insets(10, 10, 0, 0), 0, 0));
        moduleInfoPanel.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("DESCRIPTION")),
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(10, 0, 0, 0), 0, 0));
        moduleInfoPanel.add(new JLabel(moduleDesc),
                new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.BOTH,
                                       new Insets(10, 10, 0, 0), 0, 0));
        return moduleInfoPanel;
    }
}
