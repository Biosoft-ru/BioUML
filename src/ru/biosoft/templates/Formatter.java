package ru.biosoft.templates;

import java.util.Date;

import one.util.streamex.StreamEx;

import org.apache.commons.text.StringEscapeUtils;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.j2html.TagCreator;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

/**
 * @author lan
 * Helper class to use in templates
 */
public class Formatter
{
    public String format(String format, Object ... args)
    {
        return String.format(format, args);
    }

    public String formatDate(String date)
    {
        return (new Date(Long.parseLong(date))).toString();
    }

    public String formatSize(String size)
    {
        return formatSize(Long.parseLong(size));
    }

    public String formatSize(long size)
    {
        return TextUtil.formatSize( size );
    }

    public String formatHtml(Object bean)
    {
        if (bean instanceof DataElementPath) //Create link from ru.biosoft.access.core.DataElementPath. TODO: maybe move this to .js?
            return "<a target=_blank href=\"de:"+escapeHtml(bean.toString())+"\">"+((DataElementPath)bean).getName()+"</a>";
        return escapeHtml( bean.toString() );
    }

    public String formatBeanProperty(Object bean, String propertyName)
    {
        ComponentModel model = ComponentFactory.getModel(bean);
        Property property = model.findProperty(propertyName);
        if(property == null) return null;
        Object value = property.getValue();
        if(value == null) return null;
        String result = TextUtil2.toString(value);
        Object referenceTypeObj = property.getDescriptor().getValue("referenceType");
        ReferenceType type = null;
        if(referenceTypeObj != null)
        {
            type = ReferenceTypeRegistry.optReferenceType(referenceTypeObj.toString());
        }
        if(type == null || type == ReferenceTypeRegistry.getDefaultReferenceType()) return result;
        return StreamEx.split( result, ",\\s*" )
            .mapToEntry( type::getURL )
            .mapKeyValue( (val, url) -> url == null ? val
                    : TagCreator.a().withHref( url ).withTarget( "_blank" ).withText( val ).render() )
            .joining( ", " );
    }

    public String escapeHtml(String str)
    {
        return StringEscapeUtils.escapeHtml4(str);
    }

    public String getPath(DataElement de)
    {
        return (de!=null)? DataElementPath.create(de).toString(): null;
    }

    public String getReferenceURL(String type, String id)
    {
        return ReferenceTypeRegistry.getReferenceType( type ).getURL( id );
    }

    public DataElementPath createSRPath(DataElement reactionElement, String specie)
    {
        DataElementPath reactionPath = DataElementPath.create( reactionElement );
        if( reactionPath != null )
        {
            String[] pathComponents = reactionPath.getPathComponents();
            if( pathComponents.length > 4 )
            {
                return ru.biosoft.access.core.DataElementPath
                        .create( String.join( DataElementPath.PATH_SEPARATOR, pathComponents[0], pathComponents[1], specie ) );
            }
        }
        return DataElementPath.create( specie );
    }
}
