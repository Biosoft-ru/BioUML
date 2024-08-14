package biouml.standard.type.access;

import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;

import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.standard.type.BaseSupport;

import com.developmentontheedge.beans.DynamicProperty;

public class SetArrayAttributeCommand implements TagCommand
{
    public static final int DEFAULT_IDENT = 12;

    protected static final Logger log = Logger.getLogger(SetAttributeCommand.class.getName());
    protected String tag;
    protected String propertyName;

    protected TagEntryTransformer<? extends BaseSupport> transformer;

    protected boolean duplicateTags = true;

    protected List<String> value;

    private final PropertyDescriptor descriptor;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    public SetArrayAttributeCommand(String tag, String propertyName, TagEntryTransformer<? extends BaseSupport> transformer)
    {
        this.tag = tag;
        this.propertyName = propertyName;
        this.transformer = transformer;
        this.descriptor = StaticDescriptor.create(propertyName, propertyName.substring(0,1).toUpperCase()+propertyName.substring(1));
    }

    ////////////////////////////////////////////////////////////////////////////
    // TagCommand interface implementation
    //

    @Override
    public void start(String tag)
    {
        value = new ArrayList<>();
    }

    @Override
    public void addValue(String appendValue)
    {
        if( appendValue == null || appendValue.length() == 0 )
            return;

        value.add(appendValue);
    }

    @Override
    public void complete(String tag)
    {
        BaseSupport obj = transformer.getProcessedObject();

        try
        {
            String[] valueArray = value.toArray(new String[value.size()]);
            DynamicProperty property = new DynamicProperty(descriptor, String[].class, valueArray);
            obj.getAttributes().add(property);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Property parsing error: " + e + "\n  tag=" + tag + ":\n  object=" + obj + ", class=" + obj.getClass(), e);
        }
    }

    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        return null;
    }

    @Override
    public String getTaggedValue(String value)
    {
        return null;
    }
}
