package biouml.workbench.diagram;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.HtmlEditor;
import ru.biosoft.util.TextUtil;
import biouml.model.Diagram;
import biouml.standard.type.DiagramInfo;

public class DiagramDescriptionEditor extends HtmlEditor
{
    public DiagramDescriptionEditor()
    {
        view.setEnabled(false);
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram;
    }

    @Override
    public void explore(Object model, Document document)
    {
        super.explore(model, document);
        view.setEnabled(true);

        Diagram diagram = (Diagram)model;
        String text = " ";
        if( diagram.getKernel() instanceof DiagramInfo )
        {
            DiagramInfo info = (DiagramInfo)diagram.getKernel();

            text = info.getDescription();
            if( text == null )
                text = " ";
        }

        setText(text);
    }

    @Override
    public void save()
    {
        Diagram diagram = (Diagram)getModel();
        if( diagram != null && diagram.getKernel() instanceof DiagramInfo )
        {
            DiagramInfo info = (DiagramInfo)diagram.getKernel();
            String text = getText();
            String body = TextUtil.getSection("body", text);
            if( body != null )
                text = body;

            info.setDescription(text);
        }
    }
}
