package biouml.workbench.module.xml.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import ru.biosoft.util.OkCancelDialog;
import biouml.workbench.module.xml.XmlModule.InternalType.IndexDescription;

public class IndexEditorDialog extends OkCancelDialog
{
    protected List<IndexDescription> idList = new ArrayList<>();

    protected RichTableControl rtControl;

    public IndexEditorDialog(Component parent, String title, List<IndexDescription> indexes)
    {
        super(parent, title);
        init(indexes);
    }

    protected void init(List<IndexDescription> indexes)
    {
        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        if( indexes != null )
        {
            for( IndexDescription dt : indexes )
            {
                idList.add(dt.clone());
            }
        }

        rtControl = new RichTableControl(idList, IndexDescription.class, null);
        add(rtControl, BorderLayout.CENTER);
    }

    public List<IndexDescription> getIndexes()
    {
        return idList;
    }
}
