package ru.biosoft.access.repository;

import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;

/**
 * Popup tree for DataElementPathDialog
 */
public class CollectionSelectPopup extends JPopupMenu
{
    private static final long serialVersionUID = 1L;
    private JTree tree = null;
    private DataCollectionTreeModelAdapter model = null;
    private JScrollPane scrollPane;
    private JButton button;
    private Class<? extends DataElement> childClass = null;
    private Class<? extends DataElement> elementClass = null;
    private CollectionSelectedListener collectionSelectedListener = null;
    // Used to correctly handle click on button, when popup is opened
    // TODO: implement more clean solution
    private static long deactivationTime = 0;
    private boolean internalSelect = false;
    private DataElementSelectorRenderer renderer;

    public CollectionSelectPopup(JButton button, Class<? extends DataElement> childClass, Class<? extends DataElement> elementClass)
    {
        this.button = button;
        this.childClass = childClass;
        this.elementClass = elementClass;
        model = new DataCollectionTreeModelAdapter(null);
        model.setShowLeafNodes(false);
        TreeListener listener = new TreeListener();
        tree = new JTree(model);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        renderer = new DataElementSelectorRenderer();
        tree.setCellRenderer(renderer);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(null);
        scrollPane.setFocusable(false);
        scrollPane.getVerticalScrollBar().setFocusable(false);
        setOpaque(false);
        add(scrollPane);
        setFocusable(false);
        setBorder(new BevelBorder(BevelBorder.LOWERED));
        addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent arg0)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0)
            {
                deactivationTime = System.currentTimeMillis();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent arg0)
            {
            }
        });
        button.addActionListener(ae -> {
            if( isVisible() || System.currentTimeMillis() - deactivationTime < 200 )
                return;
            show();
        });
        selectItem(button.getText());
        tree.addTreeSelectionListener(listener);
    }

    public void selectItem(String path)
    {
        String[] pathElements = DataElementPath.create(path).getPathComponents();
        List<ru.biosoft.access.core.DataElementPath> treePathElements = StreamEx.of( pathElements ).scanLeft( DataElementPath.EMPTY_PATH,
                ru.biosoft.access.core.DataElementPath::getChildPath );
        TreePath treePath = new TreePath(treePathElements.toArray());
        internalSelect = true;
        tree.getSelectionModel().setSelectionPath(treePath);
        internalSelect = false;
        button.setText(path);
    }

    public JTree getTree()
    {
        return tree;
    }
    
    public Icon getItemIcon(DataElementPath path)
    {
        return renderer.getItemIcon(path);
    }

    @Override
    public void show()
    {
        if( tree.getPreferredSize() == null || System.currentTimeMillis() - deactivationTime < 200 )
            return;
        setPreferredSize(new Dimension(button.getWidth(), 350));
        show(button, 0, button.getHeight());
        tree.requestFocus();
    }

    @Override
    public void hide()
    {
        setVisible(false);
    }

    public CollectionSelectedListener getCollectionSelectedListener()
    {
        return collectionSelectedListener;
    }

    public void setCollectionSelectedListener(CollectionSelectedListener collectionSelectedListener)
    {
        Object oldValue = this.collectionSelectedListener;
        this.collectionSelectedListener = collectionSelectedListener;
        firePropertyChange("collectionSelectedListener", oldValue, this.collectionSelectedListener);
    }

    private final Map<String, DataElement> dataElementCache = new WeakHashMap<>();

    private DataElement getForName(String completeName)
    {
        DataElement de = dataElementCache.get(completeName);
        if( de == null )
        {
            de = CollectionFactory.getDataElement(completeName);
            dataElementCache.put(completeName, de);
        }
        return de;
    }

    private class TreeListener implements TreeSelectionListener, TreeExpansionListener, TreeWillExpandListener
    {
        @Override
        public void valueChanged(TreeSelectionEvent event)
        {
            if(internalSelect) return;
            DataElementPath path = (DataElementPath)event.getPath().getLastPathComponent();
            //if(!DataCollectionUtils.isAcceptable(path, childClass, elementClass)) return;
            if( collectionSelectedListener != null )
                collectionSelectedListener.valueChanged(path.getChildPath(""));
            hide();
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event)
        {
        }

        @Override
        public void treeExpanded(TreeExpansionEvent event)
        {
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException
        {
        }

        @Override
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException
        {
        }

    }

    private class DataElementSelectorRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            Icon icon = null;
            String text = null;

            String completeNodeName = value.toString();

            DataElement parent = null;
            DataElement element = null;
            DataElementPath nodePath = DataElementPath.create(completeNodeName);
            DataElementPath parentPath = nodePath.getParentPath();
            if(!parentPath.isEmpty())
            {
                parent = getForName(parentPath.toString());
            }

            if( parent instanceof DataCollection )
            {
                DataCollectionInfo info = ( (DataCollection)parent ).getInfo();

                if( !info.isLateChildrenInitialization() )
                {
                    element = getForName(completeNodeName);
                    if( element instanceof DataCollection )
                    {
                        DataCollectionInfo info1 = ( (DataCollection)element ).getInfo();
                        text = info1.getDisplayName();
                        if( !info1.isVisible() )
                        {
                            return new JLabel();
                        }
                    }
                }

                icon = getItemIcon(nodePath);
            }

            if( ( element instanceof DataCollection && ( (DataCollection)element ).getInfo().getError() != null ) )
                icon = RepositoryPane.ERROR_IMAGE;

            if( icon != null )
                setIcon(icon);

            if( text == null || text.length() == 0 )
                text = nodePath.getName();

            setText(text);
            setOpaque( !sel);

            //setEnabled( DataCollectionUtils.isAcceptable(completeNodeName, childClass, elementClass));

            setVisible( !completeNodeName.equals(""));

            return this;
        }

        public Icon getItemIcon(DataElementPath path)
        {
            ImageIcon icon = IconFactory.getIcon(path);
            if(icon != null) return icon;
            DataCollection<?> parent = path.optParentCollection();
            if( parent != null )
            {
                DataElementDescriptor descriptor = parent.getDescriptor(path.getName());
                return descriptor == null || descriptor.isLeaf()?getLeafIcon():getClosedIcon();
            }
            return getClosedIcon();
        }
    }

    public interface CollectionSelectedListener
    {
        void valueChanged(DataElementPath value);
    }

    public Class<? extends DataElement> getChildClass()
    {
        return childClass;
    }

    public void setChildClass(Class<? extends DataElement> childClass)
    {
        Object oldValue = this.childClass;
        this.childClass = childClass;
        firePropertyChange("childClass", oldValue, this.childClass);
    }

    public Class<? extends DataElement> getElementClass()
    {
        return elementClass;
    }

    public void setElementClass(Class<? extends DataElement> elementClass)
    {
        Object oldValue = this.elementClass;
        this.elementClass = elementClass;
        firePropertyChange("elementClass", oldValue, this.elementClass);
    }
}