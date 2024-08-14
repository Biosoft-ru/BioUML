package ru.biosoft.access.repository;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.repository.RepositoryPane.Status;

public class RepositoryRenderer extends DefaultTreeCellRenderer
{
    protected RepositoryPane repositoryPane;

    public RepositoryRenderer(RepositoryPane repositoryPane)
    {
        this.repositoryPane = repositoryPane;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
            boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        ImageIcon icon = null;
        String text = null;

        DataElementPath path = (DataElementPath)value;

        DataElement parent = repositoryPane.getForName(path.getParentPath());
        DataElement element = null;
        String elementName = path.getName();

        if( parent instanceof DataCollection )
        {
            DataCollectionInfo info = ( (DataCollection)parent ).getInfo();

            if( !info.isLateChildrenInitialization() )
            {
                element = repositoryPane.getForName(path);
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

            if( text == null )
            {
                QuerySystem qs = info.getQuerySystem();
                if( qs != null )
                {
                    Index titleIndex = qs.getIndex("title");
                    if( titleIndex != null )
                    {
                        text = (String)titleIndex.get(elementName);
                    }
                }
            }

            icon = getIcon(path);
        }

        Status status = repositoryPane.getPreloadingStatus(path);
        if( status != null && status.getStatusString().equals(RepositoryPane.STATUS_BUSY) )
            icon = RepositoryPane.BUSY_IMAGE;
        else
        {
            if( status != null && status.getStatusString().equals(RepositoryPane.STATUS_ERROR) )
                icon = RepositoryPane.ERROR_IMAGE;
            else
            {
                if( ( element instanceof DataCollection && ( ! ( (DataCollection)element ).isValid() || ( (DataCollection)element )
                        .getInfo().getError() != null ) ) )
                    icon = RepositoryPane.ERROR_IMAGE;
            }
        }

        if( icon != null )
            setIcon(icon);

        if( text == null || text.length() == 0 )
            text = elementName;

        setText(text);
        setOpaque( !sel);

        return this;
    }

    protected ImageIcon getIcon(DataElementPath path)
    {
        return IconFactory.getIcon(path);
    }
}
