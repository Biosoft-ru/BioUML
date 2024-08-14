package ru.biosoft.bsa.access;

import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.MessageBundle;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.AbstractDynamicPropertySet;
import com.developmentontheedge.beans.DynamicProperty;

public class TransformedSite extends AbstractDynamicPropertySet implements DataElement
{
    private static final PropertyDescriptor PD_ID = StaticDescriptor.createReadOnly("ID");
    private static final PropertyDescriptor PD_SEQUENCE = StaticDescriptor.createReadOnly(SitesToTableTransformer.PROPERTY_SEQUENCE);
    private static final PropertyDescriptor PD_FROM = StaticDescriptor.createReadOnly(SitesToTableTransformer.PROPERTY_FROM);
    private static final PropertyDescriptor PD_TO = StaticDescriptor.createReadOnly(SitesToTableTransformer.PROPERTY_TO);
    private static final PropertyDescriptor PD_LENGTH = StaticDescriptor.createReadOnly(SitesToTableTransformer.PROPERTY_LENGTH);
    private static final PropertyDescriptor PD_STRAND = StaticDescriptor.createReadOnly(SitesToTableTransformer.PROPERTY_STRAND);
    private static final PropertyDescriptor PD_TYPE = StaticDescriptor.createReadOnly(SitesToTableTransformer.PROPERTY_TYPE);
    private static ResourceBundle bundle = MessageBundle.getBundle(MessageBundle.class.getName());

    private Site s;
    
    private final Map<String, DynamicProperty> properties = new LinkedHashMap<>();

    public TransformedSite(Site s)
    {
        this.s = s;

        properties.put(PD_ID.getName(), new DynamicProperty(PD_ID, String.class, s.getName()));
        properties.put( PD_SEQUENCE.getName(), new DynamicProperty( PD_SEQUENCE, String.class,
                s.getOriginalSequence() == null ? null : s.getOriginalSequence().getName() ) );
        properties.put(PD_FROM.getName(), new DynamicProperty(PD_FROM, Integer.class, s.getFrom()));
        properties.put(PD_TO.getName(), new DynamicProperty(PD_TO, Integer.class, s.getTo()));
        properties.put(PD_LENGTH.getName(), new DynamicProperty(PD_LENGTH, Integer.class, s.getLength()));
        properties.put( PD_STRAND.getName(),
                new DynamicProperty( PD_STRAND, String.class, bundle.getStringArray( "STRAND_TYPES" )[s.getStrand()] ) );
        properties.put(PD_TYPE.getName(), new DynamicProperty(PD_TYPE, String.class, s.getType()));

        Iterator<String> propertyNameIterator = s.getOrigin() instanceof SqlTrack ? ( (SqlTrack)s.getOrigin() ).getAllProperties()
                .iterator() : s.getProperties().nameIterator();
        while( propertyNameIterator.hasNext() )
        {
            String name = propertyNameIterator.next();
            if( name.equals( "profile" ) )
                continue;
            DynamicProperty property = s.getProperties().getProperty( name );
            try
            {
                DynamicProperty newProperty;
                if( property == null )
                    newProperty = new DynamicProperty( SitesToTableTransformer.PROPERTY_PREFIX + name, String.class, "" );
                else
                {
                    if( ru.biosoft.access.core.DataElement.class.isAssignableFrom( property.getType() ) )
                        newProperty = new DynamicProperty( SitesToTableTransformer.PROPERTY_PREFIX + name, DataElementPath.class,
                                DataElementPath.create( (DataElement)property.getValue() ) );
                    else
                        newProperty = new DynamicProperty( SitesToTableTransformer.PROPERTY_PREFIX + name, property.getType(),
                                property.getValue() );
                }
                newProperty.setReadOnly( true );
                properties.put( newProperty.getName(), newProperty );
            }
            catch( Exception e )
            {
            }
        }
    }
    
    public Site getSite()
    {
        return s;
    }
    
    /* Only for ComponentFactory */
    public TransformedSite()
    {
        properties.put(PD_ID.getName(), new DynamicProperty(PD_ID, String.class, ""));
        properties.put(PD_SEQUENCE.getName(), new DynamicProperty(PD_SEQUENCE, String.class, ""));
        properties.put(PD_FROM.getName(), new DynamicProperty(PD_FROM, Integer.class, 0));
        properties.put(PD_TO.getName(), new DynamicProperty(PD_TO, Integer.class, 0));
        properties.put(PD_LENGTH.getName(), new DynamicProperty(PD_LENGTH, Integer.class, 0));
        properties.put(PD_STRAND.getName(), new DynamicProperty(PD_STRAND, String.class, bundle.getStringArray("STRAND_TYPES")[StrandType.STRAND_PLUS]));
        properties.put(PD_TYPE.getName(), new DynamicProperty(PD_TYPE, String.class, ""));
    }

    @Override
    public String getName()
    {
        return s.getName();
    }

    @Override
    public DataCollection getOrigin()
    {
        return s.getOrigin();
    }

    @Override
    public void renameProperty(String from, String to)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(DynamicProperty property)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addBefore(String propName, DynamicProperty property)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAfter(String propName, DynamicProperty property)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(DynamicProperty property)
    {
        return properties.containsKey(property.getName());
    }

    @Override
    public Object remove(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean moveTo(String name, int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replaceWith(String name, DynamicProperty prop)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> nameIterator()
    {
        return properties.keySet().iterator();
    }

    @Override
    public Iterator<DynamicProperty> propertyIterator()
    {
        return properties.values().iterator();
    }

    @Override
    public Map<String, Object> asMap()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size()
    {
        return properties.size();
    }

    @Override
    protected DynamicProperty findProperty(String name)
    {
        return properties.get(name);
    }
}