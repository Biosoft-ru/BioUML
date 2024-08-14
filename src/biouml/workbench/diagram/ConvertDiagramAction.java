package biouml.workbench.diagram;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import biouml.model.Diagram;
import biouml.workbench.ConvertDiagramDialog;

public class ConvertDiagramAction extends AbstractAction
{
    public static final String KEY = "Convert diagram";
    public static final String DIAGRAM = "Diagram";

    public ConvertDiagramAction()
    {
        this(true);
    }

    public ConvertDiagramAction(boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        DataElement model = Document.getActiveModel();
        if(model instanceof Diagram)
        {
            new ConvertDiagramDialog(( Diagram ) model).doModal();
        }
    }
}

