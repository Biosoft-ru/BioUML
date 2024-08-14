package ru.biosoft.galaxy.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.galaxy.GalaxyDataCollection;
import ru.biosoft.galaxy.GalaxyDistFiles;
import ru.biosoft.galaxy.GalaxyFactory;
import ru.biosoft.galaxy.javascript.JavaScriptGalaxy;
import ru.biosoft.gui.setupwizard.IncorrectDataException;
import ru.biosoft.gui.setupwizard.WizardPage;
import ru.biosoft.util.ExProperties;

/**
 * @author lan
 *
 */
public class GalaxyWizardPage implements WizardPage
{
    File galaxyRoot;
    JCheckBox useGalaxy;
    JTextField galaxyPath;
    JButton browseButton;

    @Override
    public JPanel getPanel()
    {
        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        galaxyRoot = GalaxyDataCollection.getGalaxyDistFiles().getRootFolder();
        useGalaxy = new JCheckBox("Use Galaxy", galaxyRoot != null && galaxyRoot.exists());
        galaxyPath = new JTextField(galaxyRoot == null ? "" : galaxyRoot.toString());
        contentPane.add(useGalaxy, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(new JLabel("Galaxy path:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(galaxyPath, new GridBagConstraints(1, 1, 1, 1, 10.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
        browseButton = new JButton("...");
        contentPane.add(browseButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(5, 5, 0, 0), 0, 0));

        ActionListener checkBoxListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                update();
            }
        };
        useGalaxy.addActionListener(checkBoxListener);
        checkBoxListener.actionPerformed(null);

        browseButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser((galaxyRoot == null || !galaxyRoot.exists()) ? new File(".") : galaxyRoot);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);
                if( chooser.showOpenDialog(Application.getApplicationFrame()) == JFileChooser.APPROVE_OPTION )
                {
                    File dir = chooser.getSelectedFile();
                    if(new GalaxyDistFiles( dir ).getToolConfFiles().length == 0)
                    {
                        ApplicationUtils.errorBox("Unable to find tool_conf.xml in "+dir+". Please select folder where Galaxy is installed.");
                    } else
                    {
                        galaxyPath.setText(chooser.getSelectedFile().toString());
                        update();
                    }
                }
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(contentPane, BorderLayout.NORTH);
        return topPanel;
    }

    protected void update()
    {
        if(useGalaxy.isSelected())
        {
            galaxyPath.setEnabled(true);
            browseButton.setEnabled(true);
            galaxyRoot = new File(galaxyPath.getText());
        } else
        {
            galaxyPath.setEnabled(false);
            browseButton.setEnabled(false);
            galaxyRoot = null;
        }
    }

    @Override
    public void fireOpenPage()
    {
    }

    @Override
    public void fireClosePage()
    {
    }

    @Override
    public void saveSettings() throws IncorrectDataException
    {
        update();
        try
        {
            if(galaxyRoot == null || !galaxyRoot.equals(GalaxyDataCollection.getGalaxyDistFiles().getRootFolder()))
            {
                JavaScriptGalaxy.GALAXY_COLLECTION.remove();
            }
            if(galaxyRoot != null)
            {
                if(new GalaxyDistFiles( galaxyRoot ).getToolConfFiles().length == 0)
                {
                    throw new IncorrectDataException(this, "Unable to find tool_conf.xml in "+galaxyRoot+". Please select folder where Galaxy is installed.");
                }
                ExProperties properties = new ExProperties();
                properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, JavaScriptGalaxy.GALAXY_COLLECTION.getName());
                properties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, GalaxyDataCollection.class.getName());
                properties.setProperty(GalaxyDataCollection.GALAXY_PATH_ATTR, galaxyRoot.toString());
                ExProperties.addPlugin(properties, GalaxyDataCollection.class);
                DataCollection dc = CollectionFactory.createCollection(JavaScriptGalaxy.GALAXY_COLLECTION.optParentCollection(), properties);
                CollectionFactoryUtils.save(dc);
                GalaxyFactory.reInit();
            }
        }
        catch( IncorrectDataException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new IncorrectDataException(this, ExceptionRegistry.log(e));
        }
    }
}
