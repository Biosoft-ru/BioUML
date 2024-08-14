package biouml.plugins.optimization.access;

import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.XmlUtil;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.MessageBundle;

public class OptimizationSupport extends OptimizationConstants
{
    protected Logger log;
    protected Optimization optimization;
    protected Map<String, String> newPaths;

    public Element getElement(Element element, String childName)
    {
        Element child = null;
        String elementName = element.getAttribute(NAME_ATTR);
        if( elementName.isEmpty() )
        {
            elementName = element.getTagName();
        }
        try
        {
            Element result = null;
            for( Element e : XmlUtil.elements(element, childName) )
            {
                if( result == null )
                {
                    result = e;
                }
                else
                {
                    log.warning(MessageBundle.format("WARN_MULTIPLE_DECLARATION", new String[] {elementName, childName}));
                }
            }

            return result;
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, MessageBundle.format("ERROR_ELEMENT_PROCESSING", new String[] {elementName, childName, t.getMessage()}));
        }

        return child;
    }

    protected static String getPropertyType(Class<?> _class)
    {
        return StreamEx.ofKeys(typeNameToType, _class::equals).findAny().orElse(_class.getName());
    }

    protected static Class<?> getPropertyType(String type)
    {
        return typeNameToType.get(type);
    }

    private static Map<String, Class<?>> typeNameToType = new HashMap<>();
    static
    {
        typeNameToType.put("String", String.class);
        typeNameToType.put("int", Integer.class);
        typeNameToType.put("double", Double.class);
        typeNameToType.put("boolean", Boolean.class);
        typeNameToType.put("ru.biosoft.access.core.DataElementPath", DataElementPath.class);
    }

    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }

    public Map<String, String> getNewPaths()
    {
        return newPaths;
    }
    public void processDPS(DynamicPropertySet dps)
    {
        if( newPaths == null )
            return;
        for( DynamicProperty dp : dps )
        {
            Object obj = dp.getValue();
            if( obj instanceof DataElementPath )
            {
                if( newPaths.containsKey( obj.toString() ) )
                {
                    dp.setValue( DataElementPath.create( newPaths.get( obj.toString() ) ) );
                }
            }
            else if( obj instanceof String )
            {
                if( newPaths.containsKey( (String)obj ) )
                    dp.setValue( newPaths.get( (String)obj ) );
            }
        }
    }
}
