package biouml.plugins.lucene;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.table.export.TableDPSWrapper;

import biouml.workbench.ExportElementDialog;

import com.developmentontheedge.application.Application;

public class ExportTableAction extends AbstractAction
{
    public static final String KEY = "ExportTable";
    public static final String VIEW_PART = "luceneViewPart";

    public ExportTableAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        LuceneSearchViewPart viewPart = (LuceneSearchViewPart)getValue(VIEW_PART);
        ExportElementDialog dialog = new ExportElementDialog(Application.getApplicationFrame(), new TableDPSWrapper(viewPart.getResults()));
        dialog.doModal();
    }
}
