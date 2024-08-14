package biouml.plugins.lucene;

import java.awt.BorderLayout;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.SemanticController;
import biouml.model.util.AddElementsUtils;
import biouml.standard.type.Reaction;
import biouml.standard.type.Relation;
import biouml.standard.type.Stub;
import biouml.workbench.diagram.ClipboardPane;
import biouml.workbench.diagram.DataElementInfo;
import biouml.workbench.diagram.DiagramDocument;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathTransferable;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.util.ApplicationUtils;

/**
 * Search view part
 */
@SuppressWarnings ( "serial" )
public class LuceneSearchViewPart extends ViewPartSupport
{

    protected static final Logger log = Logger.getLogger(LuceneSearchViewPart.class.getName());

    protected static final MessageBundle messageBundle = new MessageBundle();

    JPanel mainPanel = null;
    JLabel info = null;
    TabularPropertyInspector tabularInspector = null;

    DataElement selectedElement = null;
    Element selEl = null;
    ViewPaneAdapter adapter = null;
    SemanticController controller = null;
    DynamicPropertySet[] results;

    String searchPath = null;
    String searchQuery = null;
    ViewPane viewEditor = null;

    @Override
    public JComponent getView()
    {
        mainPanel = new JPanel(new BorderLayout());
        info = new JLabel();
        mainPanel.add(info, BorderLayout.NORTH);
        tabularInspector = new TabularPropertyInspector();
        mainPanel.add(tabularInspector, BorderLayout.CENTER);
        tabularInspector.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tabularInspector.getTable().setDragEnabled(true);
        tabularInspector.getTable().setTransferHandler(new TransferHandler()
        {
            @Override
            public int getSourceActions(final JComponent c)
            {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(final JComponent c)
            {
                JTable table = tabularInspector.getTable();
                if( table.getSelectedRowCount() == 0 )
                    return null;
                try
                {
                    return new DataElementPathTransferable(DataElementPath.create(((Property)table.getModel().getValueAt(table.getSelectedRow(),1)).getValue().toString()));
                }
                catch(Exception e)
                {
                    return null;
                }
            }
        });

        tabularInspector.addListSelectionListener(e -> {
            selectedElement = null;
            Object model = tabularInspector.getModelOfSelectedRow();
            if( model == null )
                return;
            if( model instanceof DynamicPropertySet )
            {
                DynamicPropertySet dps = (DynamicPropertySet)model;
                DynamicProperty fullName = dps.getProperty(messageBundle.getResourceString("COLUMN_FULL_NAME"));
                if( fullName == null )
                    return;
                try
                {
                    selectedElement = CollectionFactory.getDataElement(fullName.getValue().toString());
                    selEl = new Element( fullName.getValue().toString() );
                    setAddActive();
                    GUI.getManager().explore( selectedElement );
                }
                catch( Throwable t )
                {
                }
            }

            clipboardAction.setEnabled(selectedElement != null);
        });

        return mainPanel;
    }

    @Override
    public boolean canExplore(Object model)
    {
        return true;
    }

    @Override
    public void explore(Object model, Document document)
    {
        super.explore(model, document);
        selectedElement = null;
        setAddActive();

        clipboardAction.setEnabled(false);
    }

    protected void setAddActive()
    {
        if( ( selectedElement != null ) && document instanceof DiagramDocument )
        {
            addElementAction.setEnabled(true);
        }
        else
        {
            addElementAction.setEnabled(false);
        }
    }

    public void setInfo(String query, String where)
    {
        searchPath = where;
        searchQuery = query;
        info.setText(MessageFormat.format(messageBundle.getResourceString("LUCENE_INFO_TEXT"), new Object[] {query, where}));
    }

    public DynamicPropertySet[] getResults()
    {
        return results;
    }

    public void setResults(DynamicPropertySet[] results)
    {
        String where = searchPath;
        String commonPath = getSearchPath(results);
        if( ( commonPath != null ) && ( searchPath.startsWith(commonPath) ) )
        {
            where = commonPath;
        }
        info.setText(MessageFormat.format(messageBundle.getResourceString("LUCENE_INFO_TEXT"), new Object[] {searchQuery, where}));

        selectedElement = null;
        selEl = null;
        tabularInspector.explore(results);
        setAddActive();
        if( results.length > 0 )
        {
            exportTableAction.setEnabled(true);
            this.results = results;
        }
        else
        {
            exportTableAction.setEnabled(false);
        }
    }

    protected String getSearchPath(DynamicPropertySet[] results)
    {
        if( results != null )
        {
            return StreamEx.of(results)
                .map( r -> r.getProperty(messageBundle.getResourceString("COLUMN_FULL_NAME")) )
                .nonNull()
                .map( dp -> DataElementPath.create(dp.getValue().toString()) )
                .reduce( DataElementPath::getCommonPrefix )
                .map( DataElementPath::toString )
                .orElse( null );
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Actions
    //

    public static final String FULL_MODE = "full_mode";
    public static final String COPY_CLIPBOARD = "copy_clipboard";

    protected AddElementAction addElementAction;
    protected FullModeAction fullModeAction;
    protected ExportTableAction exportTableAction;
    protected CopyToClipboardAction clipboardAction;

    protected boolean addActionPerformed = false;

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( addElementAction == null )
        {
            addElementAction = new AddElementAction();
            addElementAction.setEnabled(false);
            actionManager.addAction(AddElementAction.KEY, addElementAction);

            new ActionInitializer(MessageBundle.class).initAction(addElementAction, AddElementAction.KEY);

            addElementAction.putValue(AddElementAction.VIEW_PART, this);
        }

        if( fullModeAction == null )
        {
            fullModeAction = new FullModeAction(FULL_MODE);
            actionManager.addAction(FULL_MODE, fullModeAction);

            new ActionInitializer(MessageBundle.class).initAction(fullModeAction, FULL_MODE);
        }

        if( exportTableAction == null )
        {
            exportTableAction = new ExportTableAction();
            exportTableAction.setEnabled(false);
            actionManager.addAction(ExportTableAction.KEY, exportTableAction);

            new ActionInitializer(MessageBundle.class).initAction(exportTableAction, ExportTableAction.KEY);

            exportTableAction.putValue(ExportTableAction.VIEW_PART, this);
        }

        if( clipboardAction == null )
        {
            clipboardAction = new CopyToClipboardAction(COPY_CLIPBOARD);
            actionManager.addAction(COPY_CLIPBOARD, clipboardAction);

            new ActionInitializer(MessageBundle.class).initAction(clipboardAction, COPY_CLIPBOARD);

            clipboardAction.setEnabled(false);
        }

        return new Action[] {actionManager.getAction(AddElementAction.KEY), actionManager.getAction(FULL_MODE),
                actionManager.getAction(ExportTableAction.KEY), actionManager.getAction(COPY_CLIPBOARD)};
    }

    public void addElement()
    {
        addActionPerformed = true;

        if( document != null && selectedElement != null )
        {
            if( document instanceof DiagramDocument )
            {
                DiagramDocument doc = (DiagramDocument)document;
                Diagram diagram = doc.getDiagram();
                controller = diagram.getType().getSemanticController();
                //if ( controller.canAccept ( diagram, ( Base ) selectedElement ) )
                {
                    if( ! ( selectedElement instanceof Reaction ) && ! ( selectedElement instanceof Relation )
                            && ! ( selectedElement instanceof Stub.NoteLink ) )
                    {
                        if( viewEditor == null )
                        {
                            viewEditor = doc.getDiagramViewPane();
                            adapter = new NewElementAdapter();
                            viewEditor.addViewPaneListener(adapter);
                        }
                        return;
                    }
                }
            }
        }
        showErrorCreationNewElementDialog();
    }

    protected void showErrorCreationNewElementDialog()
    {
        //  System.out.println("Error");
        JOptionPane.showMessageDialog(Application.getApplicationFrame(), messageBundle
                .getResourceString("ADDING_NEW_DIAGRAM_ELEMENT_ERROR_TITLE"), messageBundle
                .getResourceString("ADDING_NEW_DIAGRAM_ELEMENT_ERROR"), JOptionPane.INFORMATION_MESSAGE);
    }

    protected class NewElementAdapter extends ViewPaneAdapter
    {
        @Override
        public void mousePressed(ViewPaneEvent e)
        {
            if( !addActionPerformed )
                return;
            addActionPerformed = false;
            Object model = e.getViewSource().getModel();
            Compartment parent = null;
            if( model instanceof Compartment )
                parent = (Compartment)model;
            else if( model instanceof DiagramElement )
            {
                if( ( (DiagramElement)model ).getOrigin() instanceof Compartment )
                    parent = (Compartment) ( (DiagramElement)model ).getOrigin();
            }
            if( parent != null && selectedElement != null )
            {
                try
                {
                    AddElementsUtils.addElements( parent, new Element[] {selEl}, e.getPoint() );
                }
                catch( Exception e1 )
                {
                    showErrorCreationNewElementDialog();
                }
            }
        }
    }

    //classes
    class FullModeAction extends AbstractAction
    {
        protected boolean selected = false;
        public FullModeAction(String name)
        {
            super(name);
        }

        public void setSelected(boolean selected)
        {
            this.selected = selected;
            refreshButton(selected);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( selected )
            {
                tabularInspector.setVariableRowHeight(false);
                tabularInspector.explore(results);
                selected = false;
            }
            else
            {
                tabularInspector.setVariableRowHeight(true);
                tabularInspector.repaint();
                selected = true;
            }
            refreshButton(selected);
        }

        protected void refreshButton(boolean selected)
        {
            String iconString;
            String descString;
            if( selected )
            {
                iconString = messageBundle.getResourceString(LuceneSearchViewPart.FULL_MODE + Action.SMALL_ICON + "2");
                descString = messageBundle.getResourceString(LuceneSearchViewPart.FULL_MODE + Action.SHORT_DESCRIPTION + "2");
            }
            else
            {
                iconString = messageBundle.getResourceString(LuceneSearchViewPart.FULL_MODE + Action.SMALL_ICON);
                descString = messageBundle.getResourceString(LuceneSearchViewPart.FULL_MODE + Action.SHORT_DESCRIPTION);
            }
            this.putValue(AbstractAction.SHORT_DESCRIPTION, descString);
            URL url = getClass().getResource("resources/" + iconString);
            if( url != null )
            {
                this.putValue(AbstractAction.SMALL_ICON, ApplicationUtils.getImageIcon(url));
            }
        }
    }

    class CopyToClipboardAction extends AbstractAction
    {
        public CopyToClipboardAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( selectedElement != null )
            {
                DataElementInfo dei = new DataElementInfo(selectedElement);
                ClipboardPane.getClipboard().addElement(dei);
            }
        }
    }
}
