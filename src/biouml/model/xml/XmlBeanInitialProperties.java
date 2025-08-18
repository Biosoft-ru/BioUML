package biouml.model.xml;

import java.awt.Point;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class XmlBeanInitialProperties implements InitialElementProperties
{

    private String name;
    private DynamicPropertySet attributes;

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        return null;
    }

    @PropertyName("Name")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isNameReadOnly()
    {
        return name != null;
    }

    @PropertyName("Attributes")
    public DynamicPropertySet getAttributes()
    {
        return attributes;
    }

    public void setAttributes(DynamicPropertySet attributes)
    {
        this.attributes = attributes;
    }

}
