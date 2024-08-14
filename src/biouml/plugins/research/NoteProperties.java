package biouml.plugins.research;

import java.awt.Point;
import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.standard.type.Stub;

/**
 * @author lan
 *
 */
public class NoteProperties implements InitialElementProperties
{
    private String text="";

    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        DiagramElement note = new Node(c, new Stub.Note(null, getText()));
        SemanticController semanticController = Diagram.getDiagram(c).getType().getSemanticController();
        if( semanticController.canAccept(c, note) )
        {
            viewPane.add(note, location);
        }
        return new DiagramElementGroup( note );
    }

    @PropertyName("Note text")
    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}
