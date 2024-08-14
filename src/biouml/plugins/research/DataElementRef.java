package biouml.plugins.research;

import java.awt.Point;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyName;

public class DataElementRef implements InitialElementProperties
{
    private DataElementPath path;

    @PropertyName("Path to data element")
    public DataElementPath getPath()
    {
        return path;
    }

    public void setPath(DataElementPath path)
    {
        this.path = path;
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        BaseResearchSemanticController semanticController = (BaseResearchSemanticController)Diagram.getDiagram(parent).getType().getSemanticController();
        return new DiagramElementGroup( semanticController.addDataElement( parent, getPath(), location, viewPane ) );
    }
}