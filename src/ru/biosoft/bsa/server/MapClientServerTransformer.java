package ru.biosoft.bsa.server;

import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.Entry;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Sequence;

/**
 * Track transformer for client/server communications
 */
public class MapClientServerTransformer extends BeanInfoEntryTransformer<AnnotatedSequence>
{
    @Override
    public void initCommands(Class<AnnotatedSequence> type)
    {
        super.initCommands(type);

        addCommand(new ServerInfoTagCommand("SI", this));
        addCommand(new SetPropertiesCommand("XX", this));
    }
    @Override
    public Class<AnnotatedSequence> getOutputType()
    {
        return AnnotatedSequence.class;
    }

    @Override
    public AnnotatedSequence transformInput(Entry input) throws Exception
    {
        Sequence sequence = new ClientSequence(getTransformedCollection());
        AnnotatedSequence obj = new MapAsVector(input.getName(), getTransformedCollection(), sequence, null);

        readObject(obj, input.getReader());

        return obj;
    }

    public static class ServerInfoTagCommand implements TagCommand
    {
        public ServerInfoTagCommand(String tag, MapClientServerTransformer transformer)
        {
            this.tag = tag;
            this.transformer = transformer;
        }

        @Override
        public void start(String tag)
        {
            //nothing to do
        }

        @Override
        public void addValue(String value)
        {
            value = value.trim();
            AnnotatedSequence map = transformer.getProcessedObject();
            ( (ClientSequence)map.getSequence() ).setPathOnServer(value);
        }

        @Override
        public void complete(String tag)
        {
            //nothing to do
        }

        @Override
        public String getTag()
        {
            return tag;
        }

        @Override
        public String getTaggedValue()
        {
            AnnotatedSequence map = transformer.getProcessedObject();
            return tag+"  "+map.getCompletePath();
        }

        @Override
        public String getTaggedValue(String value)
        {
            throw new UnsupportedOperationException();
        }

        private final String tag;
        private final MapClientServerTransformer transformer;
    }

    public static class SetPropertiesCommand implements TagCommand
    {
        private final String tag;
        private final MapClientServerTransformer transformer;

        public SetPropertiesCommand(String tag, MapClientServerTransformer transformer)
        {
            this.tag = tag;
            this.transformer = transformer;
        }

        ////////////////////////////////////////////////////////////////////////////
        // TagCommand interface implementation
        //

        protected String name;
        protected String description;
        protected String value;
        protected int status = 0;

        @Override
        public void start(String tag)
        {

        }

        @Override
        public void addValue(String appendValue)
        {
            if( status == 0 && appendValue.length() == 0 )
            {
                status = 1;
            }
            else if( status == 1 )
            {
                name = appendValue;
                status = 2;
            }
            else if( status == 2 )
            {
                description = appendValue;
                status = 3;
            }
            else if( status == 3 )
            {
                value = appendValue;
                status = 4;
            }
            else if( status == 4 && appendValue.length() == 0 )
            {
                try
                {
                    DynamicProperty property = new DynamicProperty(name, description, String.class, value);
                    AnnotatedSequence obj = transformer.getProcessedObject();
                    obj.getProperties().add(property);
                }
                catch( Exception e )
                {
                    //log.log(Level.SEVERE, "Can not create property", e);
                }
                status = 0;
            }
        }

        @Override
        public void complete(String tag)
        {

        }

        @Override
        public String getTaggedValue()
        {
            StringBuffer result = null;
            try
            {
                AnnotatedSequence obj = transformer.getProcessedObject();

                if( obj == null )
                    return null;

                DynamicPropertySet properties = obj.getProperties();
                if( properties.size() == 0 )
                    return null;

                String tagStr = getTag();

                result = new StringBuffer();
                for(DynamicProperty property: properties)
                {
                    result.append(tagStr).append("\n");
                    result.append(tagStr).append("  ").append(property.getName()).append("\n");
                    result.append(tagStr).append("  ").append(property.getDisplayName()).append("\n");
                    result.append(tagStr).append("  ").append(property.getValue()).append("\n");
                    result.append(tagStr).append("\n");
                }
            }
            catch( Exception exc )
            {
                log.log(Level.SEVERE, "Getting tagged value error", exc);
            }

            if( result == null || result.length() == 0 )
                return null;

            return result.toString();
        }

        @Override
        public String getTag()
        {
            return tag;
        }

        @Override
        public String getTaggedValue(String value)
        {
            throw new UnsupportedOperationException();
        }
    }
}
