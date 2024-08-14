package biouml.workbench.module.xml.editor;

import java.awt.BorderLayout;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.dialog.OkCancelDialog;

import biouml.model.Module;
import biouml.workbench.diagram.NewDiagramDialog;
import biouml.workbench.module.xml.XmlModule;
import biouml.workbench.module.xml.XmlModuleType;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.QuerySystem;

public class XmlModuleDialog extends OkCancelDialog
{
    protected static final Logger log = Logger.getLogger(NewDiagramDialog.class.getName());

    public static final String COMMON = "Common";
    public static final String PROPERTIES = "Properties";
    public static final String GRAPHIC_NOTATIONS = "Diagram types";
    public static final String EXTERNAL_DATABASES = "External types";
    public static final String TYPES = "Types";

    protected DataCollection repository = null;

    protected XmlModule module = null;

    protected JTabbedPane tabbedPane;

    protected CommonTab commonTab;
    protected TypesTab typesTab;
    protected ExternalTab externalTab;
    protected NotationsTab notationsTab;

    ///////////////////////////////////////////////////////////////////
    // Constructor
    //

    public XmlModuleDialog(JDialog dialog, String title, DataCollection repository, XmlModule module)
    {
        super(dialog, title);
        init(repository, module);
    }

    public XmlModuleDialog(JFrame frame, String title, DataCollection repository, XmlModule module)
    {
        super(frame, title);
        init(repository, module);
    }

    protected void init(DataCollection repository, XmlModule module)
    {
        this.module = module;
        this.repository = repository;

        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        commonTab = new CommonTab(module);
        if( module != null )
        {
            typesTab = new TypesTab(module.getInternalTypes());
            externalTab = new ExternalTab(module.getExternalTypes());
            notationsTab = new NotationsTab(module.getDiagramTypes());
        }
        else
        {
            typesTab = new TypesTab(XmlModule.getDefaultInternalTypes());
            externalTab = new ExternalTab(null);
            notationsTab = new NotationsTab(XmlModule.getDefaultDiagramTypes());
        }

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.add(COMMON, commonTab);
        tabbedPane.add(TYPES, typesTab);
        //NOT IMPLEMENTED YET
        //tabbedPane.add(PROPERTIES, propertiesTab);
        tabbedPane.add(EXTERNAL_DATABASES, externalTab);
        tabbedPane.add(GRAPHIC_NOTATIONS, notationsTab);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public XmlModule getModule()
    {
        return module;
    }

    @Override
    protected void okPressed()
    {
        if( validateForms() )
        {
            if( module == null )
            {
                String name = commonTab.getModuleName();
                Properties primary = new Properties();
                primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
                primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());

                String fileName = name + ".xml";

                Properties transformed = new Properties();
                transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, XmlModule.class.getName());
                transformed.setProperty(Module.TYPE_PROPERTY, XmlModuleType.class.getName());
                transformed.setProperty(XmlModule.XML_DATABASE_FILE, fileName);

                //text search support
                transformed.setProperty(QuerySystem.QUERY_SYSTEM_CLASS, "biouml.plugins.lucene.LuceneQuerySystemImpl");
                transformed.setProperty(DataCollectionConfigConstants.DATA_COLLECTION_LISTENER, "biouml.plugins.lucene.LuceneInitListener");
                transformed.setProperty("lucene-directory", "luceneIndex");
                /////////////////////

                try
                {
                    module = (XmlModule)CollectionFactoryUtils.createDerivedCollection((Repository)repository, name, primary, transformed, name);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can not create module", t);
                }
            }
            commonTab.applyChanges(module);
            typesTab.applyChanges(module);
            externalTab.applyChanges(module);
            notationsTab.applyChanges(module);

            super.okPressed();
        }
    }

    protected boolean validateForms()
    {
        String errors = commonTab.validateForm();
        if( errors != null )
        {
            JOptionPane.showMessageDialog(this, errors, "Incorrect values", JOptionPane.INFORMATION_MESSAGE);
            tabbedPane.setSelectedIndex(0);
            return false;
        }
        errors = typesTab.validateForm();
        if( errors != null )
        {
            JOptionPane.showMessageDialog(this, errors, "Incorrect values", JOptionPane.INFORMATION_MESSAGE);
            tabbedPane.setSelectedIndex(1);
            return false;
        }
        errors = externalTab.validateForm();
        if( errors != null )
        {
            JOptionPane.showMessageDialog(this, errors, "Incorrect values", JOptionPane.INFORMATION_MESSAGE);
            tabbedPane.setSelectedIndex(2);
            return false;
        }
        errors = notationsTab.validateForm();
        if( errors != null )
        {
            JOptionPane.showMessageDialog(this, errors, "Incorrect values", JOptionPane.INFORMATION_MESSAGE);
            tabbedPane.setSelectedIndex(3);
            return false;
        }
        return true;
    }
}
