package biouml.plugins.virtualcell.diagram;

import java.awt.Dimension;
import java.awt.Point;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.Role;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class MetabolismProperties implements InitialElementProperties, DataOwner, DataElement
{
    private String name;
    private Node node;
    
    private DataElementPath diagramPath;
    private DataElementPath tablePath;
    
    public MetabolismProperties(String name)
    {
        this.name = name;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( compartment, new Stub( compartment, name, "Population" ) );
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

    @Override
    public DiagramElement getDiagramElement()
    {
        return node;
    }
    public void setDiagramElement(Node node)
    {
        this.node = node;
    }
    
    @Override
    public Role clone(DiagramElement de)
    {
        MetabolismProperties result = new MetabolismProperties(name);
        result.diagramPath = diagramPath;
        result.tablePath = tablePath;
        result.setDiagramElement((Node)de);
        return result;
    }
    
    @PropertyName("Model path")
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }

    public void setDiagramPath(DataElementPath diagramPath)
    {
        this.diagramPath = diagramPath;
    }

    @PropertyName("Table path")
    public DataElementPath getTablePath()
    {
        return tablePath;
    }

    public void setTablePath(DataElementPath tablePath)
    {
        this.tablePath = tablePath;
    }

    @Override
    public String[] getNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return null;
    }
}
