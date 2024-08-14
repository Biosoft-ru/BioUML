package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.Application;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.util.OkCancelDialog;
import biouml.model.Module;
import biouml.model.util.ReferencesHandler;
import biouml.standard.StandardModuleType;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Referrer;
import biouml.standard.type.RelationType;

@SuppressWarnings ( "serial" )
public class DatabaseReferencesEditDialog extends OkCancelDialog
{
    private static Logger log = Logger.getLogger( DatabaseReferencesEditDialog.class.getName() );

    protected TabularPropertyInspector referencesEditor = new TabularPropertyInspector();
    protected TabularPropertyInspector geneHubEditor = new TabularPropertyInspector();

    protected JPanel topPane = new JPanel(new GridBagLayout());
    protected JPanel bottomPane = new JPanel(new GridBagLayout());
    protected JPanel newPane = new JPanel(new GridBagLayout());
    protected JPanel searchOptionsPane = new JPanel(new GridBagLayout());
    protected JPanel referencesPane = new JPanel(new GridBagLayout());
    protected JPanel buttonsPane = new JPanel(new GridBagLayout());
    protected JPanel geneHubPane = new JPanel(new GridBagLayout());

    protected JPanel newButtonsPane = new JPanel(new GridBagLayout());
    protected JButton newPaneAddButton;
    protected JButton newPaneOpenButton = new JButton("Open in browser");
    protected JButton newPaneSearchButton = new JButton("Search in gene hub");

    protected JPanel geneHubButtonsPane = new JPanel(new GridBagLayout());
    protected JButton geneHubPaneOpenButton = new JButton("Open in browser");

    protected JPanel referencesButtonsPane = new JPanel(new GridBagLayout());
    protected JButton referencesPaneRemoveButton = new JButton("Remove");
    protected JButton referencesPaneOpenButton = new JButton("Open in browser");

    protected JButton addButton = new JButton("Add");
    protected JButton searchButton = new JButton("Search");

    protected JComboBox<String> geneHubName = new JComboBox<>();
    protected JComboBox<String> geneHubOrganism = new JComboBox<>();
    protected JComboBox<String> geneHubDatabase = new JComboBox<>();

    protected JComboBox<String> newDatabase = new JComboBox<>();
    protected JTextField newID = new JTextField();
    protected JTextField newAC = new JTextField();
    protected JComboBox<String> newRelation = new JComboBox<>();
    protected JTextField newComment = new JTextField();


    protected Map<String, DatabaseInfo> databaseInfoMap = new HashMap<>();

    public static final String DEFAULT_BINDER = "<not selected>";

    protected Module module;
    protected DatabaseReference[] references;
    protected DatabaseReference[] searchResults = new DatabaseReference[0];
    protected DataCollection<DatabaseInfo> databaseInfo;
    protected DataCollection<?> metadata;

    protected DatabaseReference selectedReferenceLeft;
    protected DatabaseReference selectedReferenceRight;

    protected boolean enabled = true;//indicates if dialog is able for current parameters

    public static final String TITLE = "Database References Editor";

    public DatabaseReferencesEditDialog(Component parent, Referrer referrer)
    {
        super(parent, TITLE);

        this.references = referrer.getDatabaseReferences();
        this.module = Module.getModule(referrer);
        try
        {
            this.metadata = (DataCollection<?>)module.get(Module.METADATA);
            if( metadata != null )
            {
                this.databaseInfo = (DataCollection<DatabaseInfo>)metadata.get(StandardModuleType.DATABASE_INFO);
            }
        }
        catch( Throwable t )
        {
            enabled = false;
            String message = "\"database info\" collection is not able for current database";
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), message, "Error", JOptionPane.ERROR_MESSAGE);
            log.log(Level.SEVERE, message);
        }
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        initComboBoxes();
        initButtons();
        newPane.setBorder(new TitledBorder("New database reference:"));
        newPane.setPreferredSize(new Dimension(0, 140));
        newPane.add(new JLabel("Database:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        newPane.add(newDatabase, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        newPane.add(new JLabel("ID:"), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        newPane.add(newID, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(
                10, 0, 0, 0), 0, 0));
        newPane.add(new JLabel("AC:"), new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        newPane.add(newAC, new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(
                10, 0, 0, 0), 0, 0));
        newPane.add(new JLabel("Relation:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        newPane.add(newRelation, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        newPane.add(new JLabel("Comment:"), new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        newPane.add(newComment, new GridBagConstraints(3, 1, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        newButtonsPane.add(newPaneAddButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        newButtonsPane.add(newPaneOpenButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        newButtonsPane.add(newPaneSearchButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        newPane.add(newButtonsPane, new GridBagConstraints(1, 2, 5, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));

        searchOptionsPane.setBorder(new TitledBorder("Gene hub search options:"));
        searchOptionsPane.setPreferredSize(new Dimension(200, 140));
        searchOptionsPane.add(new JLabel("Gene hub:"), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        searchOptionsPane.add(geneHubName, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        searchOptionsPane.add(new JLabel("Organism:"), new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        searchOptionsPane.add(geneHubOrganism, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        searchOptionsPane.add(new JLabel("Database:"), new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        searchOptionsPane.add(geneHubDatabase, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));

        referencesPane.setBorder(new TitledBorder("Current DB references for gene/title:"));
        referencesEditor.explore(references);
        referencesEditor.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        referencesEditor.addListSelectionListener(event -> {
            if( event.getFirstIndex() != -1 )
            {
                Object model = referencesEditor.getModelOfSelectedRow();
                if( model instanceof DatabaseReference )
                {
                    selectedReferenceLeft = (DatabaseReference)model;
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(referencesEditor);
        scrollPane.setPreferredSize(new Dimension(300, 100));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        referencesPane.add(scrollPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        referencesButtonsPane.add(referencesPaneRemoveButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        referencesButtonsPane.add(referencesPaneOpenButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        referencesPane.add(referencesButtonsPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
                GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));

        buttonsPane.setPreferredSize(new Dimension(100, 100));
        buttonsPane.add(addButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        buttonsPane.add(searchButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));

        geneHubPane.setBorder(new TitledBorder("Gene hub references:"));
        geneHubEditor.explore(searchResults);
        geneHubEditor.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        geneHubEditor.addListSelectionListener(event -> {
            if( event.getFirstIndex() != -1 )
            {
                Object model = geneHubEditor.getModelOfSelectedRow();
                if( model instanceof DatabaseReference )
                {
                    selectedReferenceRight = (DatabaseReference)model;
                }
            }
        });
        JScrollPane scrollPane2 = new JScrollPane(geneHubEditor);
        scrollPane2.setPreferredSize(new Dimension(300, 100));
        scrollPane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        geneHubPane.add(scrollPane2, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        geneHubButtonsPane.add(geneHubPaneOpenButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        geneHubPane.add(geneHubButtonsPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));

        topPane.add(newPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        topPane.add(searchOptionsPane, new GridBagConstraints(1, 0, 0, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        bottomPane.add(referencesPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        bottomPane.add(buttonsPane, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        bottomPane.add(geneHubPane, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));

        content.add(topPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        content.add(bottomPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));

        selectedReferenceLeft = null;
        selectedReferenceRight = null;

        updateOKButton();
        setGeneHubSearchActivity();

        setContent(content);
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    private void updateOKButton()
    {
        //TODO: set some function
        okButton.setEnabled(true);
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    public DatabaseReference[] getValue()
    {
        return references;
    }

    protected void initComboBoxes()
    {
        try
        {
            if( databaseInfo != null )
            {
                List<String> dbNames = new ArrayList<>();
                for(DatabaseInfo dbInfo : databaseInfo)
                {
                    dbNames.add(dbInfo.getTitle());
                    databaseInfoMap.put(dbInfo.getTitle(), dbInfo);
                }
                Collections.sort(dbNames);
                for( String dbName : dbNames )
                {
                    newDatabase.addItem(dbName);
                }
            }

            newRelation.addItem("");
            DataCollection<RelationType> relationType = (DataCollection<RelationType>)metadata.get(StandardModuleType.RELATION_TYPE);
            if( relationType != null )
            {
                for(RelationType type : relationType)
                {
                    newRelation.addItem(type.getTitle());
                }
            }
        }
        catch( Exception e )
        {
            //do nothing if can't find metadata
        }

        geneHubName.addItem(DEFAULT_BINDER);
        try
        {
            BioHubRegistry.bioHubs().map( BioHubInfo::getName ).forEach( geneHubName::addItem );
        }
        catch( Throwable t )
        {
            //do nothing if microarray plugin doesn't support
        }
        geneHubName.addItemListener(e -> setGeneHubSearchActivity());
    }

    protected void initButtons()
    {
        //URL url = getClass().getResource("resources/" + "arrow1.gif");
        newPaneAddButton = new JButton();
        newPaneAddButton.setLayout(new BorderLayout());
        newPaneAddButton.add(new JLabel("Add to references"), BorderLayout.CENTER);
        //newPaneAddButton.add(new JLabel(ApplicationUtils.getImageIcon(url)), BorderLayout.WEST);
        newPaneAddButton.addActionListener(event -> addNewReferenceAction());
        referencesPaneRemoveButton.addActionListener(event -> removeReferenceAction());
        searchButton.addActionListener(event -> searchReferencesByCurrentReference());
        newPaneSearchButton.addActionListener(event -> searchReferencesByNewReference());
        addButton.addActionListener(event -> addFoundReference());
        newPaneOpenButton.addActionListener(event -> openNewReference());
        referencesPaneOpenButton.addActionListener(event -> openCurrentReference());
        geneHubPaneOpenButton.addActionListener(event -> openReferenceFromGeneHub());
    }

    protected void addNewReferenceAction()
    {
        DatabaseReference dr = new DatabaseReference();
        String database = databaseInfoMap.get(newDatabase.getSelectedItem()).getName();
        if( database != null && !database.equals("") )
        {
            dr.setDatabaseName(database);
        }
        else
        {
            dr.setDatabaseName("");
        }
        String id = newID.getText();
        if( id != null && !id.equals("") )
        {
            dr.setId(id);
        }
        else
        {
            dr.setId("");
        }
        String ac = newAC.getText();
        if( ac != null && !ac.equals("") )
        {
            dr.setAc(ac);
        }
        else
        {
            dr.setAc("");
        }
        String relation = (String)newRelation.getSelectedItem();
        if( relation != null && !relation.equals("") )
        {
            dr.setRelationshipType(relation);
        }
        else
        {
            dr.setRelationshipType("");
        }
        String comment = newComment.getText();
        if( comment != null && !comment.equals("") )
        {
            dr.setComment(comment);
        }
        else
        {
            dr.setComment("");
        }

        if( references == null )
        {
            references = new DatabaseReference[] {dr};
        }
        else
        {
            DatabaseReference[] databaseReferences = new DatabaseReference[references.length + 1];
            System.arraycopy(references, 0, databaseReferences, 0, references.length);
            databaseReferences[references.length] = dr;
            references = databaseReferences;
        }
        selectedReferenceLeft = null;
        referencesEditor.explore(references);
    }

    protected void removeReferenceAction()
    {
        if( references != null && references.length > 0 && selectedReferenceLeft != null )
        {
            int row = getRowByDatabaseReference(references, selectedReferenceLeft);
            if( row != -1 )
            {
                if( references.length == 1 )
                {
                    references = null;
                }
                else
                {
                    DatabaseReference[] databaseReferences = new DatabaseReference[references.length - 1];
                    if( row > 0 )
                    {
                        System.arraycopy(references, 0, databaseReferences, 0, row);
                    }
                    if( row < references.length - 1 )
                    {
                        System.arraycopy(references, row + 1, databaseReferences, row, references.length - ( row + 1 ));
                    }
                    references = databaseReferences;
                }
                selectedReferenceLeft = null;
                referencesEditor.explore(references);
            }
        }
    }

    protected void searchReferencesByCurrentReference()
    {
        DatabaseReference[] results = null;
        if( selectedReferenceLeft != null )
        {
            int row = getRowByDatabaseReference(references, selectedReferenceLeft);
            if( row != -1 )
            {
                if( !geneHubName.getSelectedItem().equals(DEFAULT_BINDER) )
                {
                    DatabaseReference currentReference = references[row];
                    try
                    {
                        BioHub bioHub = BioHubRegistry.getBioHub(geneHubName.getSelectedItem().toString());
                        Element[] elementResults = bioHub == null ? null : bioHub.getReference(currentReference.convertToElement(), null, null, 1, -1);
                        if( elementResults == null )
                        {
                            results = new DatabaseReference[0];
                        }
                        else
                        {
                            results = convertToDatabaseReferences(elementResults);
                        }
                    }
                    catch( Exception ex )
                    {
                        //do nothing if microarray or genehub plugin doesn't support
                    }
                }
            }
            searchResults = results;
            selectedReferenceRight = null;
            geneHubEditor.explore(searchResults);
        }
    }
    protected void searchReferencesByNewReference()
    {
        DatabaseReference[] results = null;
        if( !geneHubName.getSelectedItem().equals(DEFAULT_BINDER) )
        {
            DatabaseReference newReference = new DatabaseReference();
            newReference.setDatabaseName((String)newDatabase.getSelectedItem());
            newReference.setId(newID.getText());
            newReference.setAc(newAC.getText());
            newReference.setRelationshipType((String)newRelation.getSelectedItem());
            newReference.setComment(newComment.getText());
            try
            {
                BioHub bioHub = BioHubRegistry.getBioHub(geneHubName.getSelectedItem().toString());
                Element[] elementResults = bioHub == null ? null : bioHub.getReference(newReference.convertToElement(), null, null, 1, -1);
                if( elementResults == null )
                {
                    results = new DatabaseReference[0];
                }
                else
                {
                    results = convertToDatabaseReferences(elementResults);
                }
            }
            catch( Exception ex )
            {
                log.info("microarray or genehub plugin doesn't support");
            }
        }
        searchResults = results;
        selectedReferenceRight = null;
        geneHubEditor.explore(searchResults);
    }

    protected void addFoundReference()
    {
        if( searchResults != null && searchResults.length > 0 )
        {
            int row = getRowByDatabaseReference(searchResults, selectedReferenceRight);
            if( row != -1 )
            {
                DatabaseReference newDatabaseReference = searchResults[row];

                if( references == null )
                {
                    references = new DatabaseReference[] {newDatabaseReference};
                }
                else
                {
                    DatabaseReference[] databaseReferences = new DatabaseReference[references.length + 1];
                    System.arraycopy(references, 0, databaseReferences, 0, references.length);
                    databaseReferences[references.length] = newDatabaseReference;
                    references = databaseReferences;
                    selectedReferenceLeft = null;
                    referencesEditor.explore(references);
                }
            }
        }
    }

    protected void openReference(String databaseTitle, String id)
    {
        if( databaseTitle != null && id != null && !id.equals("") && !databaseTitle.equals("") )
        {
            DatabaseInfo di = databaseInfoMap.get(databaseTitle);
            if( di != null )
            {
                String url = di.getQueryById();
                if( null == url )
                {
                    log.log(Level.SEVERE, "ID query not found in database info");
                    return;
                }
                url = url.replace('$', '@').replaceAll("@id@", id).replace('@', '$');
                try
                {
                    Desktop.getDesktop().browse(new URI(url));
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "can't open url link. ", t);
                }
            }
        }
    }

    protected void openNewReference()
    {
        String database = (String)newDatabase.getSelectedItem();
        String id = newID.getText();
        openReference(database, id);
    }

    protected void openCurrentReference()
    {
        int row = getRowByDatabaseReference(references, selectedReferenceLeft);
        if( row != -1 )
        {
            DatabaseReference dr = references[row];
            String databaseTitle = null;
            for(DatabaseInfo di : databaseInfoMap.values())
            {
                if( di.getName().equals(dr.getDatabaseName()) )
                {
                    databaseTitle = di.getTitle();
                    break;
                }
            }
            if( null == databaseTitle )
            {
                for(DatabaseInfo di : databaseInfoMap.values())
                {
                    if( di.getTitle().equals(dr.getDatabaseName()) )
                    {
                        databaseTitle = di.getTitle();
                        break;
                    }
                }
            }
            if( null == databaseTitle )
            {
                databaseTitle = "";
            }
            String id = dr.getId();
            openReference(databaseTitle, id);
        }
    }

    protected void openReferenceFromGeneHub()
    {
        int row = getRowByDatabaseReference(searchResults, selectedReferenceRight);
        if( row != -1 )
        {
            DatabaseReference dr = searchResults[row];
            String databaseTitle = null;
            for(DatabaseInfo di : databaseInfoMap.values())
            {
                if( di.getName().equals(dr.getDatabaseName()) )
                {
                    databaseTitle = di.getTitle();
                    break;
                }
            }
            if( null == databaseTitle )
            {
                for(DatabaseInfo di : databaseInfoMap.values())
                {
                    if( di.getTitle().equals(dr.getDatabaseName()) )
                    {
                        databaseTitle = di.getTitle();
                        break;
                    }
                }
            }
            if( null == databaseTitle )
            {
                databaseTitle = "";
            }
            String id = dr.getId();
            openReference(databaseTitle, id);
        }
    }

    /**
     *  lock gene hub functionality if binder not selected
     */
    protected void setGeneHubSearchActivity()
    {
        if( geneHubName.getSelectedItem().equals(DEFAULT_BINDER) )
        {
            newPaneSearchButton.setEnabled(false);
            searchButton.setEnabled(false);
            geneHubOrganism.setEnabled(false);
            geneHubDatabase.setEnabled(false);
        }
        else
        {
            newPaneSearchButton.setEnabled(true);
            searchButton.setEnabled(true);
            geneHubOrganism.setEnabled(true);
            geneHubDatabase.setEnabled(true);
        }
    }

    protected int getRowByDatabaseReference(DatabaseReference[] collection, DatabaseReference element)
    {
        int i = 0;
        for( DatabaseReference reference : collection )
        {
            if( reference == element )
            {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static DatabaseReference[] convertToDatabaseReferences(Element[] elements)
    {
        return ReferencesHandler.convertToDatabaseReferences( elements );
    }
}
