package biouml.workbench.diagram;

import ru.biosoft.gui.Document;
import ru.biosoft.util.OkCancelDialog;
import biouml.model.Diagram;

import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.application.Application;

public class ViewOptionsDialog extends OkCancelDialog
{
    public ViewOptionsDialog(Diagram diagram)
    {
        super(Application.getApplicationFrame(), "Diagram view options");
        PropertyInspectorEx propertyInspector = new PropertyInspectorEx();
        propertyInspector.setShowToolTip(false);
        propertyInspector.setShowToolTipPane(true);
        propertyInspector.setToolTipPanePreferredHeight(120);
        setContent(propertyInspector);
        propertyInspector.explore(diagram.getViewOptions());
    }

    @Override
    protected void okPressed()
    {
        Document document = Document.getActiveDocument();
        if( document instanceof DiagramDocument )
        {
            ( (DiagramDocument)document ).update();
        }
        super.okPressed();
    }
}
