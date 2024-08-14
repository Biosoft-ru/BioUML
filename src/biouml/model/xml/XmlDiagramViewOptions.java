package biouml.model.xml;

import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramViewOptions;

/**
 * {@link DiagramViewOptions} for XML diagram type.
 * Options are presented in {@link DynamicPropertySet}
 */
@PropertyName("View options")
public class XmlDiagramViewOptions extends DiagramViewOptions
{
    public XmlDiagramViewOptions(Option parent)
    {
        super(parent);
    }

    DynamicPropertySet options = new DynamicPropertySetSupport();

    public DynamicPropertySet getOptions()
    {
        return options;
    }
    
    public void setOptions(DynamicPropertySet options)
    {
        this.options = options; 
    }

    /**
     * Returns value by property name. It used from JavaScript
     */
    public Object getValue(String name)
    {
        return options.getValue(name);
    }

    @Override
    public ColorFont getNodeTitleFont()
    {
        Object titleFont = options.getValue("nodeTitleFont");
        if( titleFont == null )
        {
            titleFont = super.getNodeTitleFont();
        }
        return (ColorFont)titleFont;
    }
}
