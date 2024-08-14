package ru.biosoft.access.support;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class BeanInfoEntryTransformer<O extends DataElement> extends TagEntryTransformer<O>
{
    public BeanInfoEntryTransformer()
    {
    }

    @Override
    public String getStartTag()
    {
        return "ID";
    }

    @Override
    public String getEndTag()
    {
        return "//";
    }


    /**
     * Initialize transformer.
     *
     * @see #getPrimaryCollection()
     * @see #getTransformedCollection()
     */
    @Override
    public void init(DataCollection primaryCollection, DataCollection transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);
        initCommands(getOutputType());
    }

    public void initCommands(Class<O> type)
    {
        try
        {
            BeanInfo beanInfo = null;
            PropertyDescriptor[] properties = null;
            beanInfo = Introspector.getBeanInfo(type);
            BeanDescriptor bd = beanInfo.getBeanDescriptor();
            try
            {
                properties = (PropertyDescriptor[])bd.getValue(BeanInfoConstants.ORDER);
            }
            catch( Throwable t )
            {
                t.printStackTrace();
            }
            if( properties == null )
                properties = beanInfo.getPropertyDescriptors();

            for( PropertyDescriptor property : properties )
            {
                String tag = (String)property.getValue(HtmlPropertyInspector.DISPLAY_NAME);
                if( tag != null )
                {
                    //axec: in some cases (e.g. reading diagram with species from database), "commandClass" property here is null
                    Class<? extends SetAttributesCommand> commandClass = ( property.getValue( "commandClass" ) == null ) ? null
                            : ( (Class<?>)property.getValue( "commandClass" ) ).asSubclass( SetAttributesCommand.class );
                    if( commandClass != null )
                    {
                        try
                        {
                            SetAttributesCommand command = commandClass.newInstance();
                            command.init(property.getReadMethod(), this);
                            addCommand(command);
                        }
                        catch( Throwable t )
                        {
                            log.log(Level.SEVERE, "Can not load tag command for tag=" + tag + ", command class=" + commandClass, t);
                        }
                    }
                    else
                    {
                        // if set method is missing then we can not write an object
                        if( property.getWriteMethod() == null )
                        {
                            continue;
                        }

                        TagCommand command;
                        Class<?> propertyType = property.getPropertyType();
                        if( propertyType.isArray() )
                            command = new SetArrayPropertyCommand(tag, property, this);
                        else
                            command = new SetPropertyCommand(tag, property, this);

                        addCommand(command);
                    }
                }
            }
        }
        catch( IntrospectionException exc )
        {
            log.log(Level.SEVERE, "BeanInfoTransformer error", exc);
        }
    }

}
