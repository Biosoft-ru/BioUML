package biouml.workbench.diagram;

import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.repository.DataElementDroppable;
import ru.biosoft.access.repository.DataElementImportTransferHandler;
import ru.biosoft.access.repository.DataElementPathTransferable;
import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.GUI;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.util.AddElementsUtils;
import biouml.standard.type.Base;
import biouml.workbench.resources.MessageBundle;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.swing.table.ColumnModel;
import com.developmentontheedge.beans.swing.table.DefaultRowModel;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;

@SuppressWarnings ( "serial" )
public class ClipboardPane extends TabularPropertyInspector implements ViewPaneListener, ListSelectionListener
{
    protected static final Logger log = Logger.getLogger(ClipboardPane.class.getName());

    //protected TabularPropertyInspector clipboardTable;// = new TabularPropertyInspector ( );
    protected DefaultRowModel clipboardModel = new DefaultRowModel();

    public static final String COPY = "clipboard-copy";
    public static final String PASTE = "clipboard-paste";
    public static final String DELETE = "clipboard-delete";

    protected Action copyAction = new CopyAction(COPY);
    protected Action pasteAction = new PasteAction(PASTE);
    protected Action deleteAction = new DeleteAction(DELETE);
    protected List<DataElementInfo> items4add = new ArrayList<>();

    protected Diagram diagram;
    protected ViewPane viewPane;
    protected Action[] actions;

    private static ClipboardPane clipboardObject = null;

    public static ClipboardPane getClipboard()
    {
        if( clipboardObject == null )
        {
            clipboardObject = new ClipboardPane();
        }
        return clipboardObject;
    }

    protected ClipboardPane()
    {
        DataElementInfo templateBean = new DataElementInfo(new Node(null, "test", null));
        //clipboardTable = this;
        explore(clipboardModel, new ColumnModel(templateBean));

        copyAction.setEnabled(false);
        pasteAction.setEnabled(false);
        deleteAction.setEnabled(false);

        getTable().setTransferHandler(new ClipboardTransferHandler());
        getTable().setDragEnabled(true);
        setTransferHandler(new DataElementImportTransferHandler(new ClipboardDroppable()));

        getTable().getSelectionModel().addListSelectionListener(this);

        //add ( clipboardTable );
    }

    public void explore(Diagram diagram, ViewPane viewPane)
    {
        this.diagram = diagram;
        this.viewPane = viewPane;
    }

    public void addElement(DataElementInfo dei)
    {
        clipboardModel.add(0, dei);
        explore(clipboardModel, new ColumnModel(dei));
        repaint();
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(copyAction);
            initializer.initAction(pasteAction);
            initializer.initAction(deleteAction);

            actions = new Action[] {copyAction, pasteAction, deleteAction};
        }

        return actions;
    }

    @Override
    public void mouseClicked(ViewPaneEvent e)
    {
        if( diagram != null && viewPane != null )
        {
            diagramSelectionChanged();
        }
    }

    @Override
    public void mousePressed(ViewPaneEvent event)
    {
        if( diagram != null && viewPane != null )
        {
            diagramSelectionChanged();

            Object model = event.getViewSource().getModel();
            Compartment parent = null;
            if( model instanceof Compartment )
                parent = (Compartment)model;
            else if( model instanceof DiagramElement )
            {
                if( ( (DiagramElement)model ).getOrigin() instanceof Compartment )
                    parent = (Compartment) ( (DiagramElement)model ).getOrigin();
            }
            if( parent == null )
                return;
            for( DataElementInfo deInfo : items4add )
            {
                DataElement de = deInfo.getDataElement();
                try
                {
                    AddElementsUtils.addElements( parent, new Element[] {new Element( DataElementPath.create( de ) )}, event.getPoint() );
                }
                catch( Exception e )
                {
                    String message = "Error pasting "+de.getName()+": "+e.getMessage();
                    log.log(Level.SEVERE, message);
                    ApplicationUtils.errorBox( "Error", message );
                }
            }
            items4add.clear();
        }
    }

    @Override
    public void mouseReleased(ViewPaneEvent e)
    {
        if( diagram != null && viewPane != null )
        {
            diagramSelectionChanged();
        }
    }

    @Override
    public void mouseEntered(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseExited(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseDragged(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseMoved(ViewPaneEvent e)
    {
    }

    protected void diagramSelectionChanged()
    {
        SelectionManager sm = viewPane.getSelectionManager();
        if( sm.getSelectedModels() != null && sm.getSelectedModels().length > 0 && sm.getSelectedModels()[0] instanceof Diagram )
        {
            copyAction.setEnabled(false);
            return;
        }
        copyAction.setEnabled(sm.getSelectedViewCount() > 0);
    }

    /**
     * @pending - explore, setup document for undo/redo
     */
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        ListSelectionModel lsm = getTable().getSelectionModel();
        pasteAction.setEnabled( !lsm.isSelectionEmpty());
        deleteAction.setEnabled( !lsm.isSelectionEmpty());

        if( !lsm.isSelectionEmpty() && lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex() )
        {
            DataElement de = getModelForRow(lsm.getMinSelectionIndex()).getDataElement();
            GUI.getManager().explore( de );
        }
    }

    @Override
    public DataElementInfo getModelForRow(int row)
    {
        return (DataElementInfo)super.getModelForRow( row );
    }

    private class ClipboardDroppable implements DataElementDroppable
    {
        @Override
        public boolean doImport(DataElementPath path, Point point)
        {
            for( int j = 0; j < clipboardModel.size(); j++ )
            {
                DataElementInfo dei = (DataElementInfo)clipboardModel.getBean(j);
                if( DataElementPath.create(dei.getKernel()).equals(path) || DataElementPath.create(dei.getDataElement()).equals(path) )
                    return false;
            }
            addElement(new DataElementInfo(path.optDataElement()));
            return true;
        }
    }

    private class ClipboardTransferHandler extends TransferHandler
    {
        @Override
        public int getSourceActions(final JComponent c)
        {
            return TransferHandler.COPY;
        }

        @Override
        protected Transferable createTransferable(final JComponent c)
        {
            JTable table = getTable();
            if( table.getSelectedRowCount() == 0 )
                return null;
            try
            {
                return new DataElementPathTransferable(DataElementPath.create(((DataElementInfo)clipboardModel.getBean(table.getSelectedRow())).getDataElement()));
            }
            catch(Exception e)
            {
                return null;
            }
        }
    }

    class CopyAction extends AbstractAction
    {
        public CopyAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            items4add.clear();
            try
            {
                SelectionManager sm = viewPane.getSelectionManager();
                Object[] elements = sm.getSelectedModels();
                if( elements == null )
                    return;
                for( DiagramElement de : StreamEx.of( elements ).select( DiagramElement.class )
                        .remove( element -> element instanceof Diagram ) )
                {
                    Base kernel = de.getKernel();
                    if( kernel != null )
                    {
                        //if ( ! ( kernel instanceof Relation ) &&
                        //     ! ( kernel instanceof Reaction ) &&
                        //     ! ( kernel instanceof Stub.NoteLink ) )
                        {
                            for( int j = 0; j < clipboardModel.size(); j++ )
                            {
                                DataElementInfo dei = (DataElementInfo)clipboardModel.getBean(j);
                                if( dei.getKernel() == kernel )
                                    return;
                            }
                            DataElementInfo dei = new DataElementInfo(de);
                            clipboardModel.add(0, dei);
                            explore(clipboardModel, new ColumnModel(dei));
                            repaint();
                        }
                    }
                }
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Error: " + ex.getMessage(), ex);
            }
        }
    }

    class DeleteAction extends AbstractAction
    {
        public DeleteAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            items4add.clear();
            // store selected objects
            ListSelectionModel lsm = getTable().getSelectionModel();
            List<DataElementInfo> selectedItems = IntStreamEx.range( clipboardModel.size() ).filter( lsm::isSelectedIndex )
                    .mapToObj( ClipboardPane.this::getModelForRow ).toList();

            // remove selected items
            while( selectedItems.size() > 0 )
            {
                Object obj = selectedItems.remove(0);
                for( int i = 0; i < clipboardModel.size(); i++ )
                {
                    if( clipboardModel.getBean(i) == obj )
                    {
                        clipboardModel.remove(i);
                        break;
                    }
                }
            }
        }
    }

    class PasteAction extends AbstractAction
    {
        public PasteAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            // store selected objects
            ListSelectionModel lsm = getTable().getSelectionModel();
            if( lsm == null )
                return;

            List<DataElementInfo> selectedItems = IntStreamEx.range( clipboardModel.size() ).filter( lsm::isSelectedIndex )
                    .mapToObj( ClipboardPane.this::getModelForRow ).toList();

            ClipboardPane.this.items4add = selectedItems;
        }
    }

}