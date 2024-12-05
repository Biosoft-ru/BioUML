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
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class ProteinDegradationProperties implements InitialElementProperties, DataOwner
{
    private String name;
    private Node node;

    private DataElementPath degradationRates;
    
    public ProteinDegradationProperties(String name)
    {
        this.name = name;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( compartment, new Stub( compartment, name, "ProteinDegradation" ) );
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
        ProteinDegradationProperties result = new ProteinDegradationProperties(name);
        result.setDegradationRates( degradationRates );
        result.setDiagramElement((Node)de);
        return result;
    }

    @Override
    public String[] getNames()
    {
        return null;
    }

    @PropertyName("Degradation rates")
    public DataElementPath getDegradationRates()
    {
        return degradationRates;
    }

    public void setDegradationRates(DataElementPath degradationRates)
    {
        this.degradationRates = degradationRates;
    }
}
