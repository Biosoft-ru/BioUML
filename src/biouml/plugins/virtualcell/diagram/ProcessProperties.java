package biouml.plugins.virtualcell.diagram;

import java.awt.Dimension;
import java.awt.Point;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.Role;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class ProcessProperties implements InitialElementProperties, DataOwner
{
    private String name;
    private DataElementPath diagramPath;
    private Node node;

    public ProcessProperties(String name)
    {
        this.name = name;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( compartment, new Stub( compartment, name, "Process" ) );
        this.node = result;
        result.setRole( this );
        result.setLocation( location );
        result.setShapeSize( new Dimension( 100, 50 ) );
        compartment.put( result );
        if( viewPane != null )
            viewPane.completeTransaction();
        return new DiagramElementGroup( result );
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }

    public void setDiagramPath(DataElementPath diagramPath)
    {
        this.diagramPath = diagramPath;
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return node;
    }

    @Override
    public Role clone(DiagramElement de)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getNames()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
