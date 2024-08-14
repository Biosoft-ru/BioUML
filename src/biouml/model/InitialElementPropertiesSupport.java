package biouml.model;

import java.awt.Dimension;
import java.awt.Point;
import com.developmentontheedge.beans.Option;

import ru.biosoft.graphics.editor.ViewEditorPane;

public abstract class InitialElementPropertiesSupport extends Option implements InitialElementProperties
{
    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        DiagramElementGroup elements = doCreateElements( c, location, viewPane );
        boolean notification = c.isNotificationEnabled();
        c.setNotificationEnabled(true);

        SemanticController controller = Diagram.getDiagram(c).getType().getSemanticController();
        if( viewPane != null )
            viewPane.startTransaction("Add elements");

        for( Node n : elements.nodesStream() )
        {
            n.setLocation(new Point(0,0));
            controller.move(n, c, new Dimension(location.x, location.y), null);
            c.put(n);
        }

        elements.edgesStream().forEach( e -> c.put( e ) );

        if( viewPane != null )
            viewPane.completeTransaction();

        c.setNotificationEnabled(notification);
        return elements;
    }



    public abstract DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception;
}
