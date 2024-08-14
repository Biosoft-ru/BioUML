package biouml.workbench.module;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.logging.Logger;

import biouml.model.util.ModulePackager;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.application.dialog.OkCancelDialog;

public class ModuleInfoDialog extends OkCancelDialog
{
    protected JPanel content;

    protected static final Logger log = Logger.getLogger( ModuleInfoDialog.class.getName() );

    public ModuleInfoDialog(JDialog dialog, String title, JarFile jarFile)
    {
        super(dialog, title);
        init(jarFile);
    }

    public ModuleInfoDialog(JFrame frame, String title, JarFile jarFile)
    {
        super(frame, title);
        init(jarFile);
    }

    private void init(JarFile jarFile)
    {
        String moduleName = ModulePackager.getModuleAttribute(jarFile, ModulePackager.MF_DATABASE_NAME);
        String moduleVersion = ModulePackager.getModuleAttribute(jarFile, ModulePackager.MF_DATABASE_VERSION);
        String moduleDesc = ModulePackager.getModuleAttribute(jarFile, ModulePackager.MF_DATABASE_DESCRIPTION);

        if (moduleName != null)
        {
            byte[] buf = new byte[0];
            InputStream stream = null;
            try
            {
                try
                {
                    JarEntry jarEntry = jarFile.getJarEntry(ModulePackager.LICENSE_FILENAME);
                    if ( jarEntry != null )
                    {
                        stream = jarFile.getInputStream(jarEntry);
                        buf = new byte[stream.available()];
                        stream.read(buf);
                    }
                }
                finally
                {
                    if (stream != null)
                    {
                        stream.close();
                    }
                }
            }
            catch (Exception ioe)
            {
                log.log(Level.SEVERE, "Error at reading " + ModulePackager.LICENSE_FILENAME + " file from " + jarFile.getName(), ioe);
            }

            String licenseStr = null;
            if (buf.length > 0)
            {
                licenseStr = new String(buf);
            }

            content = new JPanel(new GridBagLayout());
            content.setBorder(new EmptyBorder(10, 10, 10, 10));

            content.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("NAME")),
                    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(10, 0, 0, 0), 0, 0));
            content.add(new JLabel(moduleName),
                    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                           GridBagConstraints.BOTH,
                                           new Insets(10, 10, 0, 0), 0, 0));
            content.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("VERSION")),
                    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(10, 0, 0, 0), 0, 0));
            content.add(new JLabel(moduleVersion),
                    new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                           GridBagConstraints.BOTH,
                                           new Insets(10, 10, 0, 0), 0, 0));
            content.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("DESCRIPTION")),
                    new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(10, 0, 0, 0), 0, 0));
            content.add(new JLabel(moduleDesc),
                    new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                           GridBagConstraints.BOTH,
                                           new Insets(10, 10, 0, 0), 0, 0));

            if (licenseStr!=null)
            {
                JTextPane textPane = new JTextPane();
                textPane.setEditable(false);
                textPane.setBackground(content.getBackground());
                textPane.setText(licenseStr);

                content.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("LICENSE")),
                        new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                               GridBagConstraints.HORIZONTAL,
                                               new Insets(10, 0, 0, 0), 0, 0));
                content.add(textPane,
                        new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                               GridBagConstraints.BOTH,
                                               new Insets(5, 0, 0, 0), 0, 0));

                JCheckBox acceptCheckBox = new JCheckBox();
                acceptCheckBox.addChangeListener(new ChangeListener()
                {
                    @Override
                    public void stateChanged(ChangeEvent e)
                    {
                        JCheckBox checkBox = (JCheckBox) e.getSource();
                        okButton.setEnabled(checkBox.isSelected());
                    }
                });
                okButton.setEnabled(false);

                content.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("ACCEPT")),
                        new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                               GridBagConstraints.HORIZONTAL,
                                               new Insets(10, 0, 0, 0), 0, 0));
                content.add(acceptCheckBox,
                        new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                               GridBagConstraints.BOTH,
                                               new Insets(10, 10, 0, 0), 0, 0));
            }
        }

        setContent(content);

        okButton.setText(BioUMLApplication.getMessageBundle().getResourceString("BUTTON_CONTINUE"));
        okButton.setPreferredSize(null);
        cancelButton.setPreferredSize(okButton.getPreferredSize());
    }
}
