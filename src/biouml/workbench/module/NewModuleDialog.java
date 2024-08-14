package biouml.workbench.module;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

import biouml.model.ModuleType;
import biouml.workbench.BioUMLApplication;
import biouml.workbench.resources.MessageBundle;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.Repository;

@SuppressWarnings ( "serial" )
public class NewModuleDialog extends OkCancelDialog
{
    // constants for moduleType extensionPoint
    public static final String EXTENSION_POINT = "biouml.workbench.moduleType";
    public static final String CLASS_ATTR = "class";
    public static final String PRIORITY_ATTR = "priority";
    public static final String DISPLAY_NAME_ATTR = "displayName";
    public static final String DESCRIPTION_ATTR = "description";

    protected static final Logger log = Logger.getLogger(NewModuleDialog.class.getName());

    /** Hashmap for extensions: key=displayName, value= configuration element */
    protected Map<String, ModuleInfo> extensionMap;
    protected List<ModuleInfo> extensionsList;
    
    protected MessageBundle resources;
    protected JTextField moduleName;
    protected JComboBox<String> moduleType;
    protected JEditorPane moduleDescription;
    protected JPanel content;

    public NewModuleDialog(JDialog dialog)
    {
        super(dialog, "");
        init();
    }

    public NewModuleDialog(JFrame frame)
    {
        super(frame, "");
        init();
    }

    protected String message(String key, Object[] params)
    {
        String message = resources.getString(key);
        return MessageFormat.format(message, params);
    }

    protected void init()
    {
        resources = BioUMLApplication.getMessageBundle();
        setTitle(resources.getResourceString("NEW_DATABASE_DIALOG_TITLE"));

        content = new JPanel(new BorderLayout());

        moduleName = new JTextField(15);
        moduleType = new JComboBox<>();

        loadExtensions();
        Iterator<ModuleInfo> it = extensionsList.iterator();
        while( it.hasNext() )
            moduleType.addItem(it.next().name);


        JPanel fields = new JPanel(new GridBagLayout());
        content.add(fields, BorderLayout.CENTER);
        fields.setBorder(new EmptyBorder(10, 10, 10, 10));
        fields.add(new JLabel(resources.getResourceString("NEW_DATABASE_DIALOG_NAME")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        fields.add(moduleName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        fields.add(new JLabel(resources.getResourceString("NEW_DATABASE_DIALOG_TYPE")), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        fields.add(moduleType, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        moduleDescription = new JEditorPane("text/html", "");
        moduleDescription.setEditable(false);
        moduleDescription.setPreferredSize(new Dimension(250, 150));
        validateModuleDescription();

        JPanel descriptionPanel = new JPanel(new BorderLayout(10, 10));
        content.add(descriptionPanel, BorderLayout.SOUTH);
        descriptionPanel.add(new JLabel(resources.getResourceString("NEW_DATABASE_DIALOG_DESCRIPTION")), BorderLayout.NORTH);
        descriptionPanel.add(new JScrollPane(moduleDescription), BorderLayout.CENTER);

        moduleName.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                String name = moduleName.getText();
                okButton.setEnabled(name != null && name.length() > 0);
            }
        });

        moduleType.addItemListener(e -> validateModuleDescription());

        setContent(content);
        okButton.setEnabled(false);
    }

    protected void validateModuleDescription()
    {
        Object item = moduleType.getSelectedItem();
        String description =  extensionMap.get( item ).description;
        if( description == null || description.length() == 0 )
            description = message("NEW_DATABASE_DESCRIPTION_ABSENTS", new String[] {item.toString()});

        moduleDescription.setText("<html>" + description + "</html>");
    }

    @Override
    protected void okPressed()
    {
        createNewModule();
        super.okPressed();
    }

    protected void createNewModule()
    {
        String name = null;
        String type = null;
        try
        {
            name = moduleName.getText();
            ModuleType module = extensionMap.get(moduleType.getSelectedItem()).type;
            module.createModule((Repository)CollectionFactoryUtils.getDatabases(), name);
        }
        catch( Throwable t )
        {
            String title = resources.getResourceString("NEW_DATABASE_ERROR_TITLE");
            String message = message("NEW_DATABASE_ERROR", new String[] {name, type, t.getMessage()});
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            log.log(Level.SEVERE, message, t);
        }
    }

    /**
     * Loads all extensions for <code>biouml.workbench.moduleType</code> extension point.
     */
    protected void loadExtensions()
    {
        extensionMap = new HashMap<>();
        extensionsList = new ArrayList<>();

        IExtensionPoint point = Application.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT);

        if( point == null )
        {
            log.log(Level.SEVERE, "NEW_DATABASE_EXTENSION_ABSENTS");
            return;
        }
        IExtension[] extensions = point.getExtensions();
        for( IExtension extension : extensions )
        {
            IConfigurationElement element = extension.getConfigurationElements()[0];
            String elementName = extension.getUniqueIdentifier();
            String pluginId = extension.getNamespaceIdentifier();
            try
            {
                String displayName = element.getAttribute(DISPLAY_NAME_ATTR);

                Class<? extends ModuleType> clazz = ClassLoading.loadSubClass( element.getAttribute(CLASS_ATTR), pluginId, ModuleType.class );
                ModuleType provider = clazz.newInstance();
                if( provider.canCreateEmptyModule() )
                {
                    ModuleInfo info = new ModuleInfo( displayName, provider );
                    info.description = element.getAttribute( DESCRIPTION_ATTR );
                    String priorityAttribute = element.getAttribute( PRIORITY_ATTR );
                    if(priorityAttribute != null )
                    {
                        try
                        {
                            info.priority = Double.parseDouble(priorityAttribute );
                        }
                        catch( Exception ex )
                        {
                            //moduleType may have no priority
                        }
                    }
                    extensionMap.put( displayName, info );
                    extensionsList.add( info );
                }
                else
                    log.warning(message("NEW_DATABASE_EXTENSION_WARN", new String[] {provider.toString(), elementName}));
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, message("NEW_DATABASE_EXTENSION_ERROR", new String[] {elementName, t.getMessage()}), t);
            }
        }
        Collections.sort(extensionsList, new ModuleInfoComparator());
    }
    
    private static class ModuleInfo
    {
        private ModuleInfo(String displayName, ModuleType moduleType)
        {
            this.type = moduleType;
            this.name = displayName;
        }
        public double priority = Double.NEGATIVE_INFINITY;
        public ModuleType type;
        public String description;
        public String name;
    }
    
    public static class ModuleInfoComparator implements Comparator<ModuleInfo>
    {
        @Override
        public int compare(ModuleInfo o1, ModuleInfo o2)
        {
            double p1 = o1.priority;
            double p2 = o2.priority;
            return (int)Math.signum(p2 - p1);
        }
    }
}
