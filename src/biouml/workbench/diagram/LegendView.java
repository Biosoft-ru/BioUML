package biouml.workbench.diagram;

import java.net.URL;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.HtmlView;
import biouml.model.Diagram;
import biouml.model.DiagramType;

/** Shows legend for graphic notation used by explored diagram. */
public class LegendView extends HtmlView
{
    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof biouml.model.Diagram;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        try
        {
            Diagram diagram = (Diagram)model;
            DiagramType type = diagram.getType();
            URL url = type.getLegend();
            setInitialText(url);
        }
        catch(Exception e)
        {
            setInitialText(e.getMessage());
        }
    }
}
