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

public class TranslationProperties implements InitialElementProperties, DataOwner, DataElement
{
    private String name;
    private Node node;

    private DataElementPath translationRates;
    
    public TranslationProperties(String name)
    {
        this.name = name;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( compartment, new Stub( compartment, name, "Translation" ) );
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
        TranslationProperties result = new TranslationProperties(name);
        result.setTranslationRates( translationRates );
        result.node = (Node)de;
        return result;
    }

    @Override
    public String[] getNames()
    {
        return null;
    }

    @PropertyName("Translation rates")
    public DataElementPath getTranslationRates()
    {
        return translationRates;
    }

    public void setTranslationRates(DataElementPath translationRates)
    {
        this.translationRates = translationRates;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
