package biouml.plugins.research.action;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;

import biouml.model.Module;
import biouml.plugins.research.ResearchBuilder;
import biouml.plugins.server.access.SQLRegistry;
import biouml.plugins.server.access.SQLRegistry.SQLInfo;
import biouml.workbench.BioUMLApplication;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.generic.TableImplementationRecord;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.journal.JournalRegistry;

/**
 * Research project properties pane
 */
@SuppressWarnings ( "serial" )
public class NewProjectPane extends JPanel
{
    protected static final Logger log = Logger.getLogger(NewProjectPane.class.getName());

    protected JTextField researchName;
    protected JComboBox<SQLInfo> sqlList;
    protected String driverName;

    private final MessageBundle resources = new MessageBundle();
    private final ru.biosoft.access.generic.MessageBundle genericResources = new ru.biosoft.access.generic.MessageBundle();

    protected JComboBox<TableImplementationRecord> preferredImplementation;
    protected TabularPropertyInspector collections;
    protected List<CollectionRecord> defaultCollections;

    public NewProjectPane(JButton createButton)
    {
        super();
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        researchName = new JTextField(30);

        JPanel fields = new JPanel(new GridBagLayout());
        add(fields, BorderLayout.CENTER);
        fields.setBorder(new EmptyBorder(10, 10, 10, 10));
        fields.add(new JLabel(resources.getResourceString("NEW_RESEARCH_NAME")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        fields.add(researchName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(
                5, 5, 0, 0), 0, 0));

        final JButton okButton = createButton;
        researchName.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                String name = researchName.getText();
                okButton.setEnabled(name != null && name.length() > 0);
            }
        });

        fields.add(new JLabel(genericResources.getResourceString("PN_TABLE_IMPLEMENTATION")), new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        preferredImplementation = new JComboBox<>(TableImplementationRecord.getTableImplementations());
        fields.add(preferredImplementation, new GridBagConstraints(1, 8, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        driverName = resources.getResourceString("JDBC_DEFAULT_DRIVER");

        fields.add(new JLabel(resources.getResourceString("JDBC_SQL_FIELD")), new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        List<SQLInfo> sqlConnections = SQLRegistry.getSQLServersList();
        sqlList = new JComboBox<>(sqlConnections.toArray(new SQLInfo[sqlConnections.size()]));
        fields.add(sqlList, new GridBagConstraints(1, 9, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5,
                0, 0), 0, 0));

        collections = new TabularPropertyInspector();
        collections.setPreferredSize(new Dimension(450, 100));
        collections.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane collectionsScrollPane = new JScrollPane(collections, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        fields.add(collectionsScrollPane, new GridBagConstraints(0, 10, 2, 1, 1.0, 2.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));
        initDefaultCollections();
        collections.explore(defaultCollections.iterator());
    }

    /**
     * Update SQL connection combo box
     */
    public void updateSQLInfo()
    {
        sqlList.removeAllItems();
        for( SQLInfo sqlInfo : SQLRegistry.getSQLServersList() )
        {
            sqlList.addItem(sqlInfo);
        }
    }

    /**
     * Create new research project with entered parameters
     */
    public void createNewResearch()
    {
        String name = null;
        try
        {
            name = researchName.getText();
            Properties props = new Properties();

            Object sqlConnection = sqlList.getSelectedItem();
            if( sqlConnection instanceof SQLInfo )
            {
                fillSQLProperties(props, (SQLInfo)sqlConnection);
            }
            props.setProperty(GenericDataCollection.PREFERED_TABLE_IMPLEMENTATION_PROPERTY, preferredImplementation.getSelectedItem()
                    .toString());

            ResearchBuilder researchBuilder = new ResearchBuilder(props);
            DataCollection<?> userProjectsParent = CollectionFactoryUtils.getUserProjectsPath().getDataCollection(DataCollection.class);
            ApplicationFrame applicationFrame = Application.getApplicationFrame();
            if( userProjectsParent.contains(name) )
            {
                JOptionPane.showMessageDialog(applicationFrame, resources.getResourceString("NEW_RESEARCH_EXISTS"),
                        "Error", JOptionPane.ERROR_MESSAGE);
                log.log(Level.SEVERE, resources.getResourceString("NEW_RESEARCH_EXISTS"));
                return;
            }
            DataCollection<?> researchDC = researchBuilder.createResearch((Repository)userProjectsParent, name, false);
            if( researchDC != null )
            {
                DataElement dataDC = researchDC.get(Module.DATA);
                if( dataDC instanceof DataCollection )
                {
                    for( String subFolderName : getSelectedCollections() )
                    {
                        DataCollectionUtils.createSubCollection(DataElementPath.create((DataCollection<?>)dataDC, subFolderName));
                    }
                }
            }

            if(applicationFrame instanceof BioUMLApplication)
            {
                // TODO: support Journal Box in Netbeans
                ( (BioUMLApplication)applicationFrame ).updateJournalBox(JournalRegistry.getJournalNames());
            }
        }
        catch( Throwable t )
        {
            String title = resources.getResourceString("NEW_RESEARCH_ERROR_TITLE");
            JOptionPane.showMessageDialog(this, ExceptionRegistry.log(t), title, JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void fillSQLProperties(Properties props, SQLInfo sqlInfo)
    {
        props.setProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY, driverName);
        props.setProperty(SqlDataCollection.JDBC_URL_PROPERTY, sqlInfo.getJdbcUrl());
        props.setProperty(SqlDataCollection.JDBC_USER_PROPERTY, sqlInfo.getUsername());
        props.setProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY, sqlInfo.getPassword());
    }

    //
    // Default collection utils
    //

    private static final String[] defaultCollectionNames = new String[] {"Files", "Tables", "Diagrams", "Tracks", "Scripts"};

    protected void initDefaultCollections()
    {
        defaultCollections = new ArrayList<>();
        for( String cName : defaultCollectionNames )
        {
            defaultCollections.add(new CollectionRecord(cName));
        }
    }

    protected List<String> getSelectedCollections()
    {
        List<String> result = new ArrayList<>();
        for( CollectionRecord cr : defaultCollections )
        {
            if( cr.isUsed() )
            {
                result.add(cr.getName());
            }
        }
        return result;
    }

    /**
     * One line in collections table
     */
    public static class CollectionRecord
    {
        protected String name;
        protected Boolean isUsed;

        public CollectionRecord(String name)
        {
            this.name = name;
            this.isUsed = true;
        }

        public Boolean isUsed()
        {
            return isUsed;
        }

        public void setUsed(Boolean isUsed)
        {
            this.isUsed = isUsed;
        }

        public String getName()
        {
            return name;
        }
    }

    /**
     * BeanInfo for {@link CollectionRecord}
     */
    public static class CollectionRecordBeanInfo extends BeanInfoEx
    {
        public CollectionRecordBeanInfo()
        {
            super(CollectionRecord.class, MessageBundle.class.getName());
            beanDescriptor.setDisplayName(getResourceString("CN_COLLECTIONRECORD"));
            beanDescriptor.setShortDescription(getResourceString("CD_COLLECTIONRECORD"));
        }

        @Override
        public void initProperties() throws Exception
        {
            PropertyDescriptorEx pde;

            pde = new PropertyDescriptorEx("name", beanClass, "getName", null);
            add(pde, getResourceString("PN_COLLECTIONRECORD_NAME"), getResourceString("PD_COLLECTIONRECORD_NAME"));

            pde = new PropertyDescriptorEx("isUsed", beanClass, "isUsed", "setUsed");
            add(pde, getResourceString("PN_COLLECTIONRECORD_USED"), getResourceString("PD_COLLECTIONRECORD_USED"));
        }
    }
}
