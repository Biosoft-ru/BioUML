package ru.biosoft.access.history;

import java.beans.PropertyDescriptor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.Date;

import java.util.logging.Logger;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.history.HistoryElement.Type;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.SetPropertyCommand;
import ru.biosoft.access.support.TagEntryTransformer;

import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * BeanInfo transformer for history serialization
 */
public class HistoryBeanInfoTransformer extends BeanInfoEntryTransformer<HistoryElement>
{
    protected Logger log = Logger.getLogger(HistoryBeanInfoTransformer.class.getName());

    @Override
    public Class<HistoryElement> getOutputType()
    {
        return HistoryElement.class;
    }

    @Override
    public void initCommands(Class<HistoryElement> type)
    {
        try
        {
            PropertyDescriptor pd = new PropertyDescriptorEx("dePath", HistoryElement.class);
            addCommand(new SetPropertyCommand("DE", pd, this));
            pd = new PropertyDescriptorEx("type", HistoryElement.class);
            addCommand(new SetTypeCommand("TY", pd, this));
            pd = new PropertyDescriptorEx("timestamp", HistoryElement.class);
            addCommand(new SetDateCommand("TS", pd, this));
            pd = new PropertyDescriptorEx("version", HistoryElement.class);
            addCommand(new SetPropertyCommand("VE", pd, this));
            pd = new PropertyDescriptorEx("author", HistoryElement.class);
            addCommand(new SetPropertyCommand("AU", pd, this));
            pd = new PropertyDescriptorEx("comment", HistoryElement.class);
            addCommand(new SetPropertyCommand("CM", pd, this));
            pd = new PropertyDescriptorEx("data", HistoryElement.class);
            addCommand(new SetPropertyCommand("DT", pd, this));
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot init history transformer", e);
        }
    }

    public static class SetTypeCommand extends SetPropertyCommand
    {
        public SetTypeCommand(String tag, PropertyDescriptor descriptor, TagEntryTransformer transformer)
        {
            super(tag, descriptor, transformer);
        }

        @Override
        protected Object stringToValue(String str)
        {
            if( type == Type.class )
                return Type.fromString(str);

            return null;
        }

        @Override
        protected String valueToString(Object value)
        {
            if( value instanceof Type )
                return ((Type)value).toString();

            return null;
        }
    }

    public static class SetDateCommand extends SetPropertyCommand
    {
        public SetDateCommand(String tag, PropertyDescriptor descriptor, TagEntryTransformer transformer)
        {
            super(tag, descriptor, transformer);
        }

        @Override
        protected Object stringToValue(String str)
        {
            if( type == Date.class )
                try
                {
                    return new SimpleDateFormat(HistoryElement.DATE_FORMAT).parseObject(str);
                }
                catch( ParseException e )
                {
                    throw ExceptionRegistry.translateException( e );
                }

            return null;
        }

        @Override
        protected String valueToString(Object value)
        {
            if( value instanceof Date )
                return new SimpleDateFormat(HistoryElement.DATE_FORMAT).format(value);

            return null;
        }
    }
}
